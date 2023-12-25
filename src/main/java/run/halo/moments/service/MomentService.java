package run.halo.moments.service;

import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.moments.ListedMoment;
import run.halo.moments.Moment;
import run.halo.moments.MomentQuery;

/**
 * Service for {@link run.halo.moments.Moment}.
 *
 * @author LIlGG
 * @since 1.0.0
 */
public interface MomentService {
    Mono<ListResult<ListedMoment>> listMoment(MomentQuery query);

    Mono<Moment> create(Moment moment);

    Mono<Moment> getByUsername(String momentName, String username);

    Mono<Moment> update(Moment moment);
}
