package run.halo.moments;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.router.selector.FieldSelector;
import run.halo.app.infra.AnonymousUserConst;

import java.security.Principal;
import java.util.function.Predicate;

import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.equal;
import static run.halo.app.extension.index.query.QueryFactory.isNull;
import static run.halo.app.extension.index.query.QueryFactory.or;


/**
 * The default implementation of {@link ReactiveQueryMomentPredicateResolver}.
 *
 */
@Component
public class DefaultQueryMomentPredicateResolver implements ReactiveQueryMomentPredicateResolver {

    @Override
    public Mono<Predicate<Moment>> getPredicate() {
        Predicate<Moment> predicate = moment -> moment.isApproved()
            && !ExtensionUtil.isDeleted(moment);
        Predicate<Moment> visiblePredicate = Moment::isPubliclyVisible;
        return currentUserName()
            .map(username -> predicate.and(
                visiblePredicate.or(moment -> username.equals(moment.getSpec().getOwner())))
            )
            .defaultIfEmpty(predicate.and(visiblePredicate));
    }

    @Override
    public Mono<ListOptions> getListOptions() {
        var listOptions = new ListOptions();
        var fieldQuery = and(
            isNull("metadata.deletionTimestamp"),
            equal("spec.approved", Boolean.TRUE.toString())
        );
        var visibleQuery = equal("spec.visible", Moment.MomentVisible.PUBLIC.name());
        return currentUserName()
            .map(username -> and(fieldQuery,
                or(visibleQuery, equal("spec.owner", username)))
            )
            .defaultIfEmpty(and(fieldQuery, visibleQuery))
            .map(query -> {
                listOptions.setFieldSelector(FieldSelector.of(query));
                return listOptions;
            });
    }

    Mono<String> currentUserName() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Principal::getName)
            .filter(name -> !AnonymousUserConst.isAnonymousUser(name));
    }
}
