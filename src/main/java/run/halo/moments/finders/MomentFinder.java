package run.halo.moments.finders;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.moments.finders.impl.MomentFinderImpl.MomentQuery;
import run.halo.moments.vo.MomentTagVo;
import run.halo.moments.vo.MomentVo;
import java.util.Map;


/**
 * A finder for {@link run.halo.moments.Moment}.
 *
 * @author LIlGG
 * @since 1.0.0
 */
public interface MomentFinder {

    /**
     * List all moments.
     *
     * @return a flux of moment vo.
     */
    Flux<MomentVo> listAll();

    /**
     * List moments by page.
     *
     * @param page page number.
     * @param size page size.
     * @return a mono of list result.
     */
    Mono<ListResult<MomentVo>> list(Integer page, Integer size);

    /**
     * Lists moments by query params.
     *
     * @param params query params see {@link MomentQuery}
     */
    Mono<ListResult<MomentVo>> list(Map<String, Object> params);

    /**
     * List moments by tag.
     *
     * @param tag tag name.
     * @return a flux of moment vo.
     */
    Flux<MomentVo> listBy(String tag);

    Mono<MomentVo> get(String momentName);

    Flux<MomentTagVo> listAllTags();

    Mono<ListResult<MomentVo>> listByTag(int pageNum, Integer pageSize, String tagName);
}
