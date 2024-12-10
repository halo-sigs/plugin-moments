package run.halo.moments.vo;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import org.springframework.web.util.UriUtils;
import java.nio.charset.StandardCharsets;

@Value
@Builder
public class MomentTagVo {

    String name;

    String permalink;

    Integer momentCount;
}
