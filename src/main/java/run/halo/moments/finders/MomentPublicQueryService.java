package run.halo.moments.finders;

import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequest;
import run.halo.moments.Moment;
import run.halo.moments.vo.MomentVo;

public interface MomentPublicQueryService {

    /**
     * Lists public moments by the given list options and page request.
     *
     * @param listOptions additional list options
     * @param page page request must not be null
     * @return a list of listed moment vo
     */
    Mono<ListResult<MomentVo>> list(ListOptions listOptions, PageRequest page);

    Mono<MomentVo> getMomentVo(@Nonnull Moment moment);
}
