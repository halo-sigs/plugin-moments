package run.halo.moments;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Listed moment.
 *
 * @author LIlGG
 * @since 1.0.0
 */
@Data
@Builder
public class ListedMoment {
    
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Moment moment;
    
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Contributor owner;
    
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Stats stats;
}
