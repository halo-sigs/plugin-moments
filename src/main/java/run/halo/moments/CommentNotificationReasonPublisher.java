package run.halo.moments;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import com.fasterxml.jackson.core.type.TypeReference;
import io.micrometer.common.util.StringUtils;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import run.halo.app.core.extension.content.Comment;
import run.halo.app.core.extension.notification.Reason;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.MetadataUtil;
import run.halo.app.extension.Ref;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.infra.utils.JsonUtils;
import run.halo.app.notification.NotificationReasonEmitter;
import run.halo.app.notification.UserIdentity;

/**
 * Notification reason publisher for {@link Comment}.
 *
 * @author guqing
 * @since 1.1.0
 */
@Component
@RequiredArgsConstructor
public class CommentNotificationReasonPublisher {
    private static final DateTimeFormatter DEFAULT_DATE_FORMATTER = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    public static final String NEW_COMMENT_ON_MOMENT = "new-comment-on-moment";
    public static final String NOTIFIED_ANNO = "moment.halo.run/notified";

    private final ExtensionClient client;
    private final NotificationReasonEmitter notificationReasonEmitter;
    private final ExternalLinkProcessor externalLinkProcessor;

    /**
     * On new comment.
     */
    @Async
    @EventListener(MomentHasNewCommentEvent.class)
    public void onNewComment(MomentHasNewCommentEvent event) {
        Comment comment = event.getComment();
        var annotations = MetadataUtil.nullSafeAnnotations(comment);
        if (annotations.containsKey(NOTIFIED_ANNO)) {
            return;
        }
        publishReasonBy(comment);
        markAsNotified(comment.getMetadata().getName());
    }

    private void markAsNotified(String commentName) {
        client.fetch(Comment.class, commentName).ifPresent(latestComment -> {
            MetadataUtil.nullSafeAnnotations(latestComment).put(NOTIFIED_ANNO, "true");
            client.update(latestComment);
        });
    }

    public void publishReasonBy(Comment comment) {
        Ref subjectRef = comment.getSpec().getSubjectRef();
        var moment = client.fetch(Moment.class, subjectRef.getName()).orElseThrow();
        if (doNotEmitReason(comment, moment)) {
            return;
        }

        String momentUrl =
            externalLinkProcessor.processLink("/moments/" + moment.getMetadata().getName());
        var reasonSubject = Reason.Subject.builder()
            .apiVersion(moment.getApiVersion())
            .kind(moment.getKind())
            .title("瞬间：" + moment.getMetadata().getName())
            .name(subjectRef.getName())
            .url(momentUrl)
            .build();

        var momentContent =
            defaultIfNull(moment.getSpec().getContent(), new Moment.MomentContent());
        var owner = comment.getSpec().getOwner();
        notificationReasonEmitter.emit(NEW_COMMENT_ON_MOMENT,
            builder -> {
                var attributes = CommentOnMomentReasonData.builder()
                    .momentName(moment.getMetadata().getName())
                    .momentOwner(moment.getSpec().getOwner())
                    .momentCreatedAt(
                        DEFAULT_DATE_FORMATTER.format(moment.getMetadata().getCreationTimestamp()))
                    .momentHtmlContent(cleanHtmlTag(momentContent.getHtml(), Safelist.basic()))
                    .momentRawContent(cleanHtmlTag(momentContent.getRaw(), Safelist.simpleText()))
                    .momentUrl(momentUrl)
                    .commenter(owner.getDisplayName())
                    .content(comment.getSpec().getContent())
                    .commentName(comment.getMetadata().getName())
                    .build();
                builder.attributes(toAttributeMap(attributes))
                    .author(identityFrom(owner))
                    .subject(reasonSubject);
            }).block();
    }

    static String cleanHtmlTag(String html, Safelist safelist) {
        if (StringUtils.isBlank(html)) {
            return "";
        }
        return Jsoup.clean(html, safelist);
    }

    static <T> Map<String, Object> toAttributeMap(T data) {
        Assert.notNull(data, "Reason attributes must not be null");
        return JsonUtils.mapper().convertValue(data, new TypeReference<>() {
        });
    }

    static UserIdentity identityFrom(Comment.CommentOwner owner) {
        if (Comment.CommentOwner.KIND_EMAIL.equals(owner.getKind())) {
            return UserIdentity.anonymousWithEmail(owner.getName());
        }
        return UserIdentity.of(owner.getName());
    }

    boolean doNotEmitReason(Comment comment, Moment moment) {
        Comment.CommentOwner commentOwner = comment.getSpec().getOwner();
        String kind = commentOwner.getKind();
        String name = commentOwner.getName();
        var momentOwner = moment.getSpec().getOwner();
        if (Comment.CommentOwner.KIND_EMAIL.equals(kind)) {
            return false;
        }
        return name.equals(momentOwner);
    }

    @Builder
    record CommentOnMomentReasonData(String momentName, String momentOwner, String momentCreatedAt,
                                     String momentHtmlContent, String momentRawContent,
                                     String momentUrl, String commenter, String content,
                                     String commentName) {
    }
}
