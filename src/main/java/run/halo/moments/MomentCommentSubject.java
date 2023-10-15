package run.halo.moments;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.halo.app.content.comment.CommentSubject;
import run.halo.app.extension.GroupVersionKind;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Ref;
import run.halo.app.infra.ExternalLinkProcessor;

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
    private final ExternalLinkProcessor externalLinkProcessor;

    @Override
    public Mono<Moment> get(String name) {
        return client.fetch(Moment.class, name);
    }

    @Override
    public Mono<SubjectDisplay> getSubjectDisplay(String name) {
        return get(name).map(moment -> {
            var content = Optional.ofNullable(moment.getSpec().getContent())
                .map(Moment.MomentContent::getRaw)
                .map(raw -> Jsoup.clean(raw, Safelist.none()))
                .map(raw -> raw.length() > 100 ? raw.substring(0, 100) : raw)
                .orElse(name);
            var momentUrl = externalLinkProcessor.processLink("/moments/" + name);
            return new SubjectDisplay(content, momentUrl, "瞬间");
        });
    }

    @Override
    public boolean supports(Ref ref) {
        Assert.notNull(ref, "Subject ref must not be null.");
        GroupVersionKind groupVersionKind =
            new GroupVersionKind(ref.getGroup(), ref.getVersion(), ref.getKind());
        return GroupVersionKind.fromExtension(Moment.class).equals(groupVersionKind);
    }
}
