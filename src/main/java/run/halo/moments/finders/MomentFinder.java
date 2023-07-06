package run.halo.moments.finders;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.moments.vo.MomentTagVo;
import run.halo.moments.vo.MomentVo;


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
