package run.halo.moments;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

@GVK(group = "moment.halo.run", version = "v1alpha1", kind = "Moment",
    plural = "moments", singular = "moment")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Moment extends AbstractExtension {
    public static final String REQUIRE_SYNC_ON_STARTUP_INDEX_NAME = "requireSyncOnStartup";

    @Schema(requiredMode = REQUIRED)
    private MomentSpec spec;

    private Status status;

    @Data
    public static class MomentSpec {

        @Schema(requiredMode = REQUIRED)
        private MomentContent content;

        @Schema(description = "Release timestamp. This field can be customized by owner")
        private Instant releaseTime;

        @Schema(description = "Visible indicates when to show publicly. Default is public",
            defaultValue = "PUBLIC")
        private MomentVisible visible;

        @Schema(requiredMode = REQUIRED, description = "Owner of the moment")
        private String owner;

        @Schema(description = "Tags of the moment")
        private Set<String> tags;

        @Schema(defaultValue = "false")
        private Boolean approved;

        private Instant approvedTime;
    }

    @Data
    @Schema(name = "MomentStatus")
    public static class Status {
        private long observedVersion;

        private String permalink;
    }

    @Data
    public static class MomentContent {

        @Schema(description = "Raw of content")
        private String raw;

        @Schema(description = "Rendered result with HTML format")
        private String html;

        @ArraySchema(
            uniqueItems = true,
            arraySchema = @Schema(description = "Medium of moment"),
            schema = @Schema(description = "Media item of moment"))
        private List<MomentMedia> medium;
    }

    @Data
    public static class MomentMedia {

        @Schema(description = "Type of media")
        private MomentMediaType type;

        @Schema(description = "External URL of media")
        private String url;

        @Schema(description = "Origin type of media.")
        private String originType;
    }

    public enum MomentMediaType {
        PHOTO,
        VIDEO,
        POST,
        AUDIO,
        // TODO Might add more types here in the future
    }

    public enum MomentVisible {
        /**
         * Public is default visible of moment.
         */
        PUBLIC,

        /**
         * Private visible is only for view for self.
         */
        PRIVATE;
        // TODO Might add more visibles here in the future.

        /**
         * Convert value string to {@link MomentVisible}.
         *
         * @param value enum value string
         * @return {@link MomentVisible} if found, otherwise null
         */
        public static MomentVisible from(String value) {
            for (MomentVisible visible : MomentVisible.values()) {
                if (visible.name().equalsIgnoreCase(value)) {
                    return visible;
                }
            }
            return null;
        }
    }
}
