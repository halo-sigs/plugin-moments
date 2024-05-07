package run.halo.moments.service.impl;

import static run.halo.app.extension.Comparators.compareCreationTimestamp;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Role;
import run.halo.app.extension.MetadataUtil;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.utils.JsonUtils;
import run.halo.moments.service.RoleService;
import run.halo.moments.util.AuthorityUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultRoleService implements RoleService {

    private final ReactiveExtensionClient client;

    @Override
    public Mono<Boolean> joint(Collection<String> source, Collection<String> candidates) {
        if (source.contains(AuthorityUtils.SUPER_ROLE_NAME)) {
            return Mono.just(true);
        }
        return listDependencies(new HashSet<>(source))
            .map(role -> role.getMetadata().getName())
            .collect(Collectors.toSet())
            .map(roleNames -> !Collections.disjoint(roleNames, candidates));
    }

    private Flux<Role> listDependencies(Set<String> names) {
        var visited = new HashSet<String>();
        return listRoles(names)
            .expand(role -> {
                var name = role.getMetadata().getName();
                if (visited.contains(name)) {
                    return Flux.empty();
                }
                if (log.isTraceEnabled()) {
                    log.trace("Expand role: {}", role.getMetadata().getName());
                }
                visited.add(name);
                var annotations = MetadataUtil.nullSafeAnnotations(role);
                var dependenciesJson = annotations.get(Role.ROLE_DEPENDENCIES_ANNO);
                var dependencies = stringToList(dependenciesJson);

                return Flux.fromIterable(dependencies)
                    .filter(dep -> !visited.contains(dep))
                    .collect(Collectors.<String>toSet())
                    .flatMapMany(this::listRoles);
            })
            .concatWith(Flux.defer(() -> listAggregatedRoles(visited)));
    }

    private Flux<Role> listAggregatedRoles(Set<String> roleNames) {
        var aggregatedLabelNames = roleNames.stream()
            .map(roleName -> Role.ROLE_AGGREGATE_LABEL_PREFIX + roleName)
            .collect(Collectors.toSet());
        Predicate<Role> predicate = role -> {
            var labels = role.getMetadata().getLabels();
            if (labels == null) {
                return false;
            }
            return aggregatedLabelNames.stream()
                .anyMatch(aggregatedLabel -> Boolean.parseBoolean(labels.get(aggregatedLabel)));
        };
        return client.list(Role.class, predicate, compareCreationTimestamp(true));
    }

    private Flux<Role> listRoles(Set<String> names) {
        if (CollectionUtils.isEmpty(names)) {
            return Flux.empty();
        }

        Predicate<Role> predicate = role -> names.contains(role.getMetadata().getName());
        return client.list(Role.class, predicate, compareCreationTimestamp(true));
    }

    @NonNull
    private List<String> stringToList(String str) {
        if (StringUtils.isBlank(str)) {
            return Collections.emptyList();
        }
        return JsonUtils.jsonToObject(str,
            new TypeReference<>() {
            });
    }
}
