package run.halo.moments;

import lombok.Builder;
import lombok.Value;

/**
 * Stats of moment.
 *
 * @author LIlGG
 * @since 0.0.1
 */
@Value
@Builder
public class Stats {

    Integer upvote;

    Integer totalComment;

    Integer approvedComment;

    public static Stats empty() {
        return Stats.builder()
                .upvote(0)
                .totalComment(0)
                .approvedComment(0)
                .build();
    }
}
