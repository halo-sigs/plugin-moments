package run.halo.moments.search;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.Queries;
import run.halo.app.extension.router.selector.FieldSelector;
import run.halo.app.search.HaloDocument;
import run.halo.app.search.HaloDocumentsProvider;
import run.halo.moments.Moment;

/**
 * @author LIlGG
 */
@Component
@RequiredArgsConstructor
public class MomentHaloDocumentsProvider implements HaloDocumentsProvider {

    public static final String MOMENT_DOCUMENT_TYPE = "moment.moment.halo.run";

    private static final int PAGE_SIZE = 200;

    private final ReactiveExtensionClient client;

    private final DocumentConverter converter;

    @Override
    public Flux<HaloDocument> fetchAll() {
        var options = new ListOptions();
        var notDeleted = Queries.isNull("metadata.deletionTimestamp");
        var approved = Queries.equal("spec.approved", Boolean.TRUE);
        options.setFieldSelector(FieldSelector.of(notDeleted).andQuery(approved));
        var pageRequest = createPageRequest();
        // make sure the moments are approved and not deleted.
        return client.listBy(Moment.class, options, pageRequest)
            .expand(result -> result.hasNext()
                ? client.listBy(Moment.class, options, nextPage(result, pageRequest.getSort()))
                : Mono.empty())
            .flatMap(result -> Flux.fromIterable(result.getItems()))
            .flatMap(converter::convert);
    }

    @Override
    public String getType() {
        return MOMENT_DOCUMENT_TYPE;
    }

    private PageRequest createPageRequest() {
        return PageRequestImpl.of(1, PAGE_SIZE,
            Sort.by("metadata.creationTimestamp", "metadata.name"));
    }

    private static PageRequest nextPage(ListResult<Moment> result, Sort sort) {
        return PageRequestImpl.of(result.getPage() + 1, result.getSize(), sort);
    }
}
