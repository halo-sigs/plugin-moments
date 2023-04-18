package run.halo.moments.vo;

import lombok.Builder;
import lombok.Value;
import run.halo.app.extension.MetadataOperator;
import run.halo.moments.MomentTag;

/**
 * @author LIlGG
 */
@Value
@Builder
public class MomentTagVo {
    MetadataOperator metadata;

    MomentTag.MomentTagSpec spec;

    public static MomentTagVo from(MomentTag momentTag) {
        return MomentTagVo.builder()
            .metadata(momentTag.getMetadata())
            .spec(momentTag.getSpec())
            .build();
    }
}
