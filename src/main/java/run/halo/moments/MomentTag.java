package run.halo.moments;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

@GVK(group = "moment.halo.run", version = "v1alpha1", kind = "MomentTag",
    plural = "momenttags", singular = "momenttag")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MomentTag extends AbstractExtension {

    @Schema(required = true)
    private MomentTagSpec spec;

    @Data
    public static class MomentTagSpec {

        @Schema(required = true)
        private String displayName;

    }
}
