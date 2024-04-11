package run.halo.moments.service;

import java.util.Collection;
import reactor.core.publisher.Mono;

public interface RoleService {
    Mono<Boolean> contains(Collection<String> source, Collection<String> candidates);
}
