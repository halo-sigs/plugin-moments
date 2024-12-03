package run.halo.moments.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MomentUpdatedEvent extends ApplicationEvent {
    private final String momentName;

    public MomentUpdatedEvent(Object source, String momentName) {
        super(source);
        this.momentName = momentName;
    }
}
