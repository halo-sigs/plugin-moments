package run.halo.moments;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static run.halo.app.extension.index.query.QueryFactory.all;
import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.contains;
import static run.halo.app.extension.index.query.QueryFactory.equal;
import static run.halo.app.extension.index.query.QueryFactory.greaterThanOrEqual;
import static run.halo.app.extension.index.query.QueryFactory.lessThanOrEqual;
import static run.halo.app.extension.router.QueryParamBuildUtil.sortParameter;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToListOptions;

import java.time.Instant;

import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.router.IListRequest;
import run.halo.app.extension.router.SortableRequest;
import run.halo.app.extension.router.selector.FieldSelector;

/**
 * A query object for {@link Moment} list.
 *
 * @author LIlGG
 * @author guqing
 * @since 1.0.0
 */
public class MomentQuery extends SortableRequest {
    private final MultiValueMap<String, String> queryParams;

    private String username;

    public MomentQuery(ServerWebExchange exchange) {
        super(exchange);
        this.queryParams = exchange.getRequest().getQueryParams();
    }

    public MomentQuery(ServerWebExchange exchange, String username) {
        this(exchange);
        this.username = username;
    }

    @Nullable
    @Schema(description = "Moments filtered by keyword.")
    public String getKeyword() {
        return StringUtils.defaultIfBlank(queryParams.getFirst("keyword"), null);
    }

    @Schema(description = "Owner name.")
    public String getOwnerName() {
        if (StringUtils.isNotBlank(username)) {
            return username;
        }
        String ownerName = queryParams.getFirst("ownerName");
        return StringUtils.isBlank(ownerName) ? null : ownerName;
    }

    @Schema(description = "Moment tag.")
    public String getTag() {
        return StringUtils.defaultIfBlank(queryParams.getFirst("tag"), null);
    }

    @Nullable
    public Moment.MomentVisible getVisible() {
        String visible = queryParams.getFirst("visible");
        return Moment.MomentVisible.from(visible);
    }

    @Schema
    public Instant getStartDate() {
        String startDate = queryParams.getFirst("startDate");
        return convertInstantOrNull(startDate);
    }

    @Schema
    public Instant getEndDate() {
        String endDate = queryParams.getFirst("endDate");
        return convertInstantOrNull(endDate);
    }

    /**
     * Build {@link ListOptions} from query params.
     *
     * @return a list options.
     */
    public ListOptions toListOptions() {
        var listOptions =
            labelAndFieldSelectorToListOptions(getLabelSelector(), getFieldSelector());
        var query = all();
        if (StringUtils.isNotBlank(getOwnerName())) {
            query = and(query, equal("spec.owner", getOwnerName()));
        }
        if (StringUtils.isNotBlank(getTag())) {
            query = and(query, equal("spec.tags", getTag()));
        }
        if (getVisible() != null) {
            query = and(query, equal("spec.visible", getVisible().name()));
        }

        if (getApproved() != null) {
            query = and(query, equal("spec.approved", Boolean.toString(getApproved())));
        }

        if (getStartDate() != null) {
            query = and(query, greaterThanOrEqual("spec.releaseTime", getStartDate().toString()));
        }
        if (getEndDate() != null) {
            query = and(query, lessThanOrEqual("spec.releaseTime", getEndDate().toString()));
        }

        if (listOptions.getFieldSelector() != null) {
            query = and(query, listOptions.getFieldSelector().query());
        }
        if (StringUtils.isNotBlank(getKeyword())) {
            query = and(query, contains("spec.owner", getKeyword()));
        }
        listOptions.setFieldSelector(FieldSelector.of(query));
        return listOptions;
    }

    public PageRequest toPageRequest() {
        var sort = getSort();
        if (sort.isUnsorted()) {
            sort = Sort.by("spec.releaseTime").descending();
        }
        return PageRequestImpl.of(getPage(), getSize(), sort);
    }

    @Schema(description = "moment approved.")
    public Boolean getApproved() {
        return convertBooleanOrNull(queryParams.getFirst("approved"));
    }

    private Boolean convertBooleanOrNull(String value) {
        return StringUtils.isBlank(value) ? null : Boolean.parseBoolean(value);
    }

    private Instant convertInstantOrNull(String timeStr) {
        return StringUtils.isBlank(timeStr) ? null : Instant.parse(timeStr);
    }

    public static void buildParameters(Builder builder) {
        IListRequest.buildParameters(builder);
        builder.parameter(sortParameter())
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("keyword")
                .description("Moments filtered by keyword.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("ownerName")
                .description("Owner name.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("tag")
                .description("Moment tag.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("visible")
                .description("Moment visible.")
                .implementation(Moment.MomentVisible.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("startDate")
                .implementation(Instant.class)
                .description("Moment start date.")
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("endDate")
                .implementation(Instant.class)
                .description("Moment end date.")
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("approved")
                .description("Moment approved.")
                .implementation(Boolean.class)
                .required(false))
        ;
    }

}
