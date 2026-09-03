package run.halo.moments.mcp;

import static org.springframework.data.domain.Sort.Order.desc;
import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.index.query.Queries.greaterThan;
import static run.halo.app.extension.index.query.Queries.lessThan;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.mcpserver.api.McpToolAnnotations;
import run.halo.mcpserver.api.McpToolDefinition;
import run.halo.mcpserver.api.McpToolException;
import run.halo.mcpserver.api.McpToolInvocation;
import run.halo.mcpserver.api.McpToolProvider;
import run.halo.mcpserver.api.McpToolResult;
import run.halo.moments.Contributor;
import run.halo.moments.ListedMoment;
import run.halo.moments.Moment;
import run.halo.moments.Stats;
import run.halo.moments.exception.NotFoundException;
import run.halo.moments.service.MomentService;

public class MomentMcpToolProvider implements McpToolProvider {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_MEDIA = 9;
    private static final Map<String, Object> MOMENT_OUTPUT_SCHEMA = momentOutputSchema();
    private static final Map<String, Object> LISTED_MOMENT_OUTPUT_SCHEMA =
        listedMomentOutputSchema();

    private final ReactiveExtensionClient client;
    private final MomentService momentService;

    public MomentMcpToolProvider(
        ReactiveExtensionClient client, MomentService momentService) {
        this.client = client;
        this.momentService = momentService;
    }

    @Override
    public Flux<McpToolDefinition> tools() {
        return Flux.just(listMoments(), getMoment(), createMoment(), deleteMoment());
    }

    private McpToolDefinition listMoments() {
        var properties = new LinkedHashMap<String, Object>();
        properties.put("page", Map.of(
            "type", "integer", "minimum", 1, "default", 1));
        properties.put("size", Map.of(
            "type", "integer",
            "minimum", 1,
            "maximum", MAX_PAGE_SIZE,
            "default", DEFAULT_PAGE_SIZE));
        properties.put("owner", Map.of("type", "string", "minLength", 1));
        properties.put("tag", Map.of("type", "string", "minLength", 1));
        properties.put("visible", Map.of(
            "type", "string", "enum", List.of("PUBLIC", "PRIVATE")));
        properties.put("approved", Map.of("type", "boolean"));
        properties.put("startDate", dateTimeSchema());
        properties.put("endDate", dateTimeSchema());
        return McpToolDefinition.builder()
            .name("list_moments")
            .title("List moments")
            .description("List Halo moments, including private or unapproved items when matched "
                + "by filters.")
            .displayTitle("查询瞬间")
            .displayDescription(
                "分页查询瞬间，可按作者、标签、可见性、审核状态和时间"
                    + "筛选。")
            .inputSchema(objectSchema(properties, List.of()))
            .outputSchema(objectSchema(Map.of(
                "page", Map.of("type", "integer", "minimum", 1),
                "size", Map.of("type", "integer", "minimum", 1),
                "total", Map.of("type", "integer", "minimum", 0),
                "items", Map.of(
                    "type", "array", "items", MOMENT_OUTPUT_SCHEMA)),
                List.of("page", "size", "total", "items")))
            .annotations(McpToolAnnotations.readOnly("List moments"))
            .permission(ignored -> Mono.just(true))
            .handler(this::listMoments)
            .build();
    }

    private McpToolDefinition getMoment() {
        return McpToolDefinition.builder()
            .name("get_moment")
            .title("Get a moment")
            .description("Get a Halo moment by metadata name, including its owner and statistics.")
            .displayTitle("获取瞬间详情")
            .displayDescription("按资源名称获取瞬间、作者和统计信息。")
            .inputSchema(objectSchema(
                Map.of("name", Map.of("type", "string", "minLength", 1)),
                List.of("name")))
            .outputSchema(LISTED_MOMENT_OUTPUT_SCHEMA)
            .annotations(McpToolAnnotations.readOnly("Get a moment"))
            .permission(ignored -> Mono.just(true))
            .handler(this::getMoment)
            .build();
    }

    private McpToolDefinition createMoment() {
        var mediaItem = new LinkedHashMap<String, Object>();
        mediaItem.put("type", "object");
        mediaItem.put("properties", Map.of(
            "type", Map.of(
                "type", "string",
                "enum", List.of("PHOTO", "VIDEO", "POST", "AUDIO")),
            "url", Map.of("type", "string", "minLength", 1),
            "originType", Map.of("type", "string")));
        mediaItem.put("required", List.of("type", "url"));
        mediaItem.put("additionalProperties", false);

        var properties = new LinkedHashMap<String, Object>();
        properties.put("content", Map.of(
            "type", "string",
            "description", "Plain-text moment content. May be empty when media is supplied."));
        properties.put("media", Map.of(
            "type", "array", "maxItems", MAX_MEDIA, "items", mediaItem));
        properties.put("tags", Map.of(
            "type", "array",
            "uniqueItems", true,
            "items", Map.of("type", "string", "minLength", 1)));
        properties.put("visible", Map.of(
            "type", "string",
            "enum", List.of("PUBLIC", "PRIVATE"),
            "default", "PUBLIC"));
        return McpToolDefinition.builder()
            .name("create_moment")
            .title("Create a moment")
            .description("Publish an approved Halo moment owned by the current MCP access-key "
                + "owner.")
            .displayTitle("发布瞬间")
            .displayDescription(
                "以当前 MCP 密钥所有者身份发布已审核的图文瞬间。")
            .inputSchema(objectSchema(properties, List.of()))
            .outputSchema(MOMENT_OUTPUT_SCHEMA)
            .annotations(new McpToolAnnotations(
                false, false, false, false, "Create a moment"))
            .permission(ignored -> Mono.just(true))
            .handler(this::createMoment)
            .build();
    }

    private McpToolDefinition deleteMoment() {
        return McpToolDefinition.builder()
            .name("delete_moment")
            .title("Delete a moment")
            .description("Permanently delete a Halo moment by metadata name. You MUST obtain "
                + "the user's explicit confirmation immediately before calling this tool.")
            .displayTitle("删除瞬间")
            .displayDescription("按资源名称永久删除瞬间，调用前必须获得用户明确确认。")
            .inputSchema(objectSchema(
                Map.of("name", Map.of(
                    "type", "string",
                    "minLength", 1,
                    "description", "Metadata name of the moment to delete.")),
                List.of("name")))
            .outputSchema(MOMENT_OUTPUT_SCHEMA)
            .annotations(new McpToolAnnotations(
                false, true, true, false, "Delete a moment"))
            .permission(ignored -> Mono.just(true))
            .handler(this::deleteMoment)
            .build();
    }

    private Mono<McpToolResult> listMoments(McpToolInvocation invocation) {
        return Mono.defer(() -> {
            var arguments = invocation.arguments();
            var page = integer(arguments, "page", 1, 1, Integer.MAX_VALUE);
            var size = integer(arguments, "size", DEFAULT_PAGE_SIZE, 1, MAX_PAGE_SIZE);
            var options = ListOptions.builder().fieldQuery(ExtensionUtil.notDeleting());
            addEqualFilter(options, arguments, "owner", "spec.owner");
            addEqualFilter(options, arguments, "tag", "spec.tags");
            var visible = optionalString(arguments, "visible");
            if (visible != null) {
                options.andQuery(equal("spec.visible", requireVisible(visible).name()));
            }
            if (arguments.containsKey("approved")) {
                var approved = arguments.get("approved");
                if (!(approved instanceof Boolean)) {
                    throw invalid("approved must be a boolean");
                }
                options.andQuery(equal("spec.approved", approved));
            }
            var startDate = instant(arguments.get("startDate"), "startDate");
            var endDate = instant(arguments.get("endDate"), "endDate");
            if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                throw invalid("startDate must not be after endDate");
            }
            if (startDate != null) {
                options.andQuery(greaterThan("spec.releaseTime", startDate.toString(), true));
            }
            if (endDate != null) {
                options.andQuery(lessThan("spec.releaseTime", endDate.toString(), true));
            }
            var pageRequest = PageRequestImpl.of(page, size, Sort.by(
                desc("spec.releaseTime"), desc("metadata.name")));
            return client.listBy(Moment.class, options.build(), pageRequest)
                .map(result -> McpToolResult.success(Map.of(
                    "page", result.getPage(),
                    "size", result.getSize(),
                    "total", result.getTotal(),
                    "items", result.getItems().stream()
                        .map(MomentMcpToolProvider::momentPayload)
                        .toList())));
        });
    }

    private Mono<McpToolResult> getMoment(McpToolInvocation invocation) {
        return Mono.defer(() -> momentService.findMomentByName(
                requiredString(invocation.arguments(), "name"))
            .map(moment -> McpToolResult.success(listedMomentPayload(moment)))
            .onErrorMap(NotFoundException.class,
                error -> new McpToolException("NOT_FOUND", "Moment not found", error)));
    }

    private Mono<McpToolResult> createMoment(McpToolInvocation invocation) {
        return Mono.defer(() -> momentService.create(toMoment(invocation.arguments()))
            .map(moment -> McpToolResult.success(
                momentPayload(moment), "Moment created")));
    }

    private Mono<McpToolResult> deleteMoment(McpToolInvocation invocation) {
        return Mono.defer(() -> client.fetch(
                Moment.class, requiredString(invocation.arguments(), "name"))
            .switchIfEmpty(Mono.error(new McpToolException(
                "NOT_FOUND", "Moment not found")))
            .flatMap(momentService::deleteBy)
            .map(moment -> McpToolResult.success(
                momentPayload(moment), "Moment deleted")));
    }

    private Moment toMoment(Map<String, Object> arguments) {
        var contentValue = arguments.get("content");
        if (contentValue != null && !(contentValue instanceof String)) {
            throw invalid("content must be a string");
        }
        var plainText = (String) contentValue;
        var media = media(arguments.get("media"));
        if ((plainText == null || plainText.isBlank()) && media.isEmpty()) {
            throw invalid("content or media must be supplied");
        }

        var rendered = plainText == null || plainText.isBlank()
            ? ""
            : renderPlainText(plainText);
        var content = new Moment.MomentContent();
        content.setRaw(rendered);
        content.setHtml(rendered);
        content.setMedium(media);

        var spec = new Moment.MomentSpec();
        spec.setContent(content);
        spec.setTags(strings(arguments.get("tags"), "tags"));
        var visible = optionalString(arguments, "visible");
        spec.setVisible(visible == null
            ? Moment.MomentVisible.PUBLIC
            : requireVisible(visible));
        spec.setApproved(true);
        spec.setApprovedTime(Instant.now());

        var metadata = new Metadata();
        metadata.setGenerateName("moment-");
        var moment = new Moment();
        moment.setMetadata(metadata);
        moment.setSpec(spec);
        return moment;
    }

    private static void addEqualFilter(
        ListOptions.ListOptionsBuilder options,
        Map<String, Object> arguments,
        String argumentName,
        String fieldName) {
        var value = optionalString(arguments, argumentName);
        if (value != null) {
            options.andQuery(equal(fieldName, value));
        }
    }

    private static List<Moment.MomentMedia> media(Object value) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> values) || values.size() > MAX_MEDIA) {
            throw invalid("media must be an array with at most " + MAX_MEDIA + " items");
        }
        var result = new ArrayList<Moment.MomentMedia>(values.size());
        for (var item : values) {
            if (!(item instanceof Map<?, ?> map)) {
                throw invalid("each media item must be an object");
            }
            var type = requiredString(map, "type");
            var medium = new Moment.MomentMedia();
            try {
                medium.setType(Moment.MomentMediaType.valueOf(type));
            } catch (IllegalArgumentException error) {
                throw invalid("unsupported media type: " + type);
            }
            medium.setUrl(requireSafeMediaUrl(requiredString(map, "url")));
            medium.setOriginType(optionalString(map, "originType"));
            result.add(medium);
        }
        return result;
    }

    private static String requireSafeMediaUrl(String value) {
        try {
            var uri = URI.create(value);
            if (!uri.isAbsolute()) {
                if (value.startsWith("/") && !value.startsWith("//")) {
                    return value;
                }
                throw invalid("media url must be an absolute HTTP URL or a root-relative path");
            }
            var scheme = uri.getScheme();
            if (("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                && uri.getHost() != null) {
                return value;
            }
        } catch (IllegalArgumentException ignored) {
        }
        throw invalid("media url must use HTTP or HTTPS");
    }

    private static String renderPlainText(String value) {
        var normalized = value.strip().replace("\r\n", "\n").replace('\r', '\n');
        return "<p>" + HtmlUtils.htmlEscape(normalized).replace("\n", "<br>") + "</p>";
    }

    private static Set<String> strings(Object value, String name) {
        if (value == null) {
            return Set.of();
        }
        if (!(value instanceof List<?> values)) {
            throw invalid(name + " must be an array of strings");
        }
        var result = new LinkedHashSet<String>();
        for (var item : values) {
            if (!(item instanceof String string) || string.isBlank()) {
                throw invalid(name + " must contain only non-empty strings");
            }
            result.add(string.strip());
        }
        return result;
    }

    private static int integer(
        Map<String, Object> arguments,
        String name,
        int defaultValue,
        int minimum,
        int maximum) {
        var value = arguments.get(name);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof Number number)
            || number.doubleValue() != number.intValue()
            || number.intValue() < minimum
            || number.intValue() > maximum) {
            throw invalid(name + " must be an integer between " + minimum + " and " + maximum);
        }
        return number.intValue();
    }

    private static Instant instant(Object value, String name) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String string) || string.isBlank()) {
            throw invalid(name + " must be an ISO-8601 date-time");
        }
        try {
            return Instant.parse(string);
        } catch (DateTimeParseException error) {
            throw invalid(name + " must be an ISO-8601 date-time");
        }
    }

    private static String requiredString(Map<?, ?> arguments, String name) {
        var value = optionalString(arguments, name);
        if (value == null) {
            throw invalid(name + " must be a non-empty string");
        }
        return value;
    }

    private static String optionalString(Map<?, ?> arguments, String name) {
        var value = arguments.get(name);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String string) || string.isBlank()) {
            throw invalid(name + " must be a non-empty string");
        }
        return string;
    }

    private static Moment.MomentVisible requireVisible(String value) {
        var visible = Moment.MomentVisible.from(value);
        if (visible == null) {
            throw invalid("visible must be PUBLIC or PRIVATE");
        }
        return visible;
    }

    private static Map<String, Object> momentPayload(Moment moment) {
        var metadata = moment.getMetadata();
        var spec = moment.getSpec();
        var content = spec == null ? null : spec.getContent();
        var status = moment.getStatus();
        var result = new LinkedHashMap<String, Object>();
        result.put("name", metadata == null ? null : metadata.getName());
        result.put("content", content == null ? null : content.getHtml());
        result.put("media", content == null || content.getMedium() == null
            ? List.of()
            : content.getMedium().stream()
                .map(MomentMcpToolProvider::mediaPayload)
                .toList());
        result.put("tags", spec == null || spec.getTags() == null
            ? List.of()
            : spec.getTags().stream().toList());
        result.put("visible", spec == null || spec.getVisible() == null
            ? null
            : spec.getVisible().name());
        result.put("owner", spec == null ? null : spec.getOwner());
        result.put("approved", spec != null && Boolean.TRUE.equals(spec.getApproved()));
        result.put("releaseTime", spec == null || spec.getReleaseTime() == null
            ? null
            : spec.getReleaseTime().toString());
        result.put("permalink", status == null ? null : status.getPermalink());
        return result;
    }

    private static Map<String, Object> mediaPayload(Moment.MomentMedia media) {
        var result = new LinkedHashMap<String, Object>();
        result.put("type", media.getType() == null ? null : media.getType().name());
        result.put("url", media.getUrl());
        result.put("originType", media.getOriginType());
        return result;
    }

    private static Map<String, Object> listedMomentPayload(ListedMoment listedMoment) {
        var result = new LinkedHashMap<String, Object>();
        result.put("moment", momentPayload(listedMoment.getMoment()));
        result.put("owner", contributorPayload(listedMoment.getOwner()));
        result.put("stats", statsPayload(listedMoment.getStats()));
        return result;
    }

    private static Map<String, Object> contributorPayload(Contributor contributor) {
        if (contributor == null) {
            return null;
        }
        var result = new LinkedHashMap<String, Object>();
        result.put("displayName", contributor.getDisplayName());
        result.put("avatar", contributor.getAvatar());
        result.put("name", contributor.getName());
        return result;
    }

    private static Map<String, Object> statsPayload(Stats stats) {
        if (stats == null) {
            return null;
        }
        var result = new LinkedHashMap<String, Object>();
        result.put("upvote", stats.getUpvote());
        result.put("totalComment", stats.getTotalComment());
        result.put("approvedComment", stats.getApprovedComment());
        return result;
    }

    private static McpToolException invalid(String message) {
        return new McpToolException("INVALID_ARGUMENT", message);
    }

    private static Map<String, Object> dateTimeSchema() {
        return Map.of("type", "string", "format", "date-time");
    }

    private static Map<String, Object> momentOutputSchema() {
        var mediaSchema = objectSchema(Map.of(
            "type", Map.of(
                "type", List.of("string", "null"),
                "enum", Arrays.asList("PHOTO", "VIDEO", "POST", "AUDIO", null)),
            "url", nullableStringSchema(),
            "originType", nullableStringSchema()),
            List.of("type", "url", "originType"));
        return objectSchema(Map.of(
            "name", nullableStringSchema(),
            "content", nullableStringSchema(),
            "media", Map.of("type", "array", "items", mediaSchema),
            "tags", stringArraySchema(),
            "visible", Map.of(
                "type", List.of("string", "null"),
                "enum", Arrays.asList("PUBLIC", "PRIVATE", null)),
            "owner", nullableStringSchema(),
            "approved", Map.of("type", "boolean"),
            "releaseTime", nullableDateTimeSchema(),
            "permalink", nullableStringSchema()),
            List.of(
                "name", "content", "media", "tags", "visible", "owner", "approved",
                "releaseTime", "permalink"));
    }

    private static Map<String, Object> listedMomentOutputSchema() {
        var ownerSchema = nullableObjectSchema(Map.of(
            "displayName", nullableStringSchema(),
            "avatar", nullableStringSchema(),
            "name", nullableStringSchema()),
            List.of("displayName", "avatar", "name"));
        var statsSchema = nullableObjectSchema(Map.of(
            "upvote", nullableIntegerSchema(),
            "totalComment", nullableIntegerSchema(),
            "approvedComment", nullableIntegerSchema()),
            List.of("upvote", "totalComment", "approvedComment"));
        return objectSchema(Map.of(
            "moment", MOMENT_OUTPUT_SCHEMA,
            "owner", ownerSchema,
            "stats", statsSchema),
            List.of("moment", "owner", "stats"));
    }

    private static Map<String, Object> nullableObjectSchema(
        Map<String, Object> properties, List<String> required) {
        return Map.of(
            "type", List.of("object", "null"),
            "properties", properties,
            "required", required,
            "additionalProperties", false);
    }

    private static Map<String, Object> nullableStringSchema() {
        return Map.of("type", List.of("string", "null"));
    }

    private static Map<String, Object> nullableIntegerSchema() {
        return Map.of("type", List.of("integer", "null"));
    }

    private static Map<String, Object> nullableDateTimeSchema() {
        return Map.of("type", List.of("string", "null"), "format", "date-time");
    }

    private static Map<String, Object> stringArraySchema() {
        return Map.of(
            "type", "array",
            "items", Map.of("type", "string"),
            "uniqueItems", true);
    }

    private static Map<String, Object> objectSchema(
        Map<String, Object> properties, List<String> required) {
        return Map.of(
            "type", "object",
            "properties", properties,
            "required", required,
            "additionalProperties", false);
    }
}
