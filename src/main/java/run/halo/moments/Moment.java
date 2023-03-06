package run.halo.moments;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
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

    @Schema(required = true)
    private MomentSpec spec;

    @Data
    public static class MomentSpec {

        @Schema(required = true)
        private MomentContent content;

        @Schema(description = "Release timestamp. This field can be customized by owner")
        private Instant releaseTime;

        @Schema(description = "Visible indicates when to show publicly. Default is public",
                defaultValue = "PUBLIC")
        private MomentVisible visible;

        @Schema(required = true, description = "Owner of the moment")
        private String owner;

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
