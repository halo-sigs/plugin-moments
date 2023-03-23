package run.halo.moments.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.util.Assert;
import run.halo.app.extension.MetadataOperator;
import run.halo.moments.Moment;
import run.halo.moments.Stats;

/**
 * Listed moment.
 *
 * @author LIlGG
 * @since 1.0.0
 */
@Data
@SuperBuilder
@ToString
@EqualsAndHashCode
public class MomentVo {

    private MetadataOperator metadata;

    private Moment.MomentSpec spec;

    private ContributorVo owner;

    private Stats stats;

    public static MomentVo from(Moment moment) {
        Assert.notNull(moment, "The moment must not be null.");
        return MomentVo.builder()
                .metadata(moment.getMetadata())
                .spec(moment.getSpec())
                .build();
    }
}
