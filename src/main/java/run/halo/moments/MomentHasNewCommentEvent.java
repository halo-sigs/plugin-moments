package run.halo.moments;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import run.halo.app.core.extension.content.Comment;

/**
 * Event for moment has new comment.
 *
 * @author guqing
 * @since 1.1.0
 */
@Getter
public class MomentHasNewCommentEvent extends ApplicationEvent {

    private final Comment comment;

    public MomentHasNewCommentEvent(Object source, Comment comment) {
        super(source);
        this.comment = comment;
    }
}
