package run.halo.moments.service;

import java.util.Collection;
import reactor.core.publisher.Mono;

public interface RoleService {
    /**
     * verify whether the source role contains any role in the candidates.
     *
     * @param source the role to be verified
     * @param candidates the roles to be verified
     * @return <p>true if the source role contains any role in the candidates, otherwise false</p>
     */
    Mono<Boolean> joint(Collection<String> source, Collection<String> candidates);
}
