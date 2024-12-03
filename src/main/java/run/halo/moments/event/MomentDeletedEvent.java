package run.halo.moments.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MomentDeletedEvent extends ApplicationEvent {
    private final String momentName;

    public MomentDeletedEvent(Object source, String momentName) {
        super(source);
        this.momentName = momentName;
    }
}
