package run.halo.moments.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MomentTagVo {

    String name;

    Integer momentCount;
}
