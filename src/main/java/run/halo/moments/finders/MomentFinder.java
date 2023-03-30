package run.halo.moments.finders;

import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.moments.vo.MomentVo;


/**
 * A finder for {@link run.halo.moments.Moment}.
 *
 * @author LIlGG
 * @since 1.0.0
 */
public interface MomentFinder {
    
    Flux<MomentVo> listAll();
    
    Mono<ListResult<MomentVo>> list(@Nullable Integer page, @Nullable Integer size);
}
