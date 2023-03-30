package run.halo.moments;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import run.halo.app.content.Contributor;

/**
 * Listed moment.
 *
 * @author LIlGG
 * @since 1.0.0
 */
@Data
@Builder
public class ListedMoment {
    
    @Schema(required = true)
    private Moment moment;
    
    @Schema(required = true)
    private Contributor owner;
    
    @Schema(required = true)
    private Stats stats;
}
