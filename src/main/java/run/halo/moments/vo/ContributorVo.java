package run.halo.moments.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import run.halo.app.core.extension.User;

/**
 * Listed comment.
 *
 * @author LIlGG
 * @since 2.0.0
 */
@Data
@SuperBuilder
@ToString
@EqualsAndHashCode
public class ContributorVo {
    private String name;
    
    private String avatar;
    
    private String bio;
    
    private String displayName;
    
    public static ContributorVo from(User user) {
        return builder().name(user.getMetadata().getName())
            .displayName(user.getSpec().getDisplayName())
            .avatar(user.getSpec().getAvatar())
            .bio(user.getSpec().getBio())
            .build();
    }
}
