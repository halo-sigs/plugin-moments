package run.halo.moments;

import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;

import java.util.function.Predicate;

/**
 * The reactive query moment predicate resolver.
 *
 */
public interface ReactiveQueryMomentPredicateResolver {

    Mono<Predicate<Moment>> getPredicate();

    Mono<ListOptions> getListOptions();
}
