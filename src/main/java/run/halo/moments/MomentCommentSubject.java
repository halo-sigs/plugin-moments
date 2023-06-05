package run.halo.moments;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.halo.app.content.comment.CommentSubject;
import run.halo.app.extension.GroupVersionKind;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Ref;

/**
 * <p>Comment subject for moment.</p>
 * This class helps to get comment subject by name when comment list query.
 *
 * @author guqing
 * @since 1.0.2
 */
@Component
@RequiredArgsConstructor
public class MomentCommentSubject implements CommentSubject<Moment> {

    private final ReactiveExtensionClient client;

    @Override
    public Mono<Moment> get(String name) {
        return client.fetch(Moment.class, name);
    }

    @Override
    public boolean supports(Ref ref) {
        Assert.notNull(ref, "Subject ref must not be null.");
        GroupVersionKind groupVersionKind =
            new GroupVersionKind(ref.getGroup(), ref.getVersion(), ref.getKind());
        return GroupVersionKind.fromExtension(Moment.class).equals(groupVersionKind);
    }
}
