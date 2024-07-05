package run.halo.moments.search;

import static run.halo.moments.ModelConst.SEARCH_DEFAULT_PAGE_SIZE;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.QueryFactory;
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

    private final ReactiveExtensionClient client;

    private final DocumentConverter converter;

    @Override
    public Flux<HaloDocument> fetchAll() {
        var options = new ListOptions();
        var notDeleted = QueryFactory.isNull("metadata.deletionTimestamp");
        var approved = QueryFactory.equal("spec.approved", "true");
        options.setFieldSelector(FieldSelector.of(notDeleted).andQuery(approved));
        var pageRequest = createPageRequest();
        // make sure the moments are approved and not deleted.
        return client.listBy(Moment.class, options, pageRequest)
            .map(ListResult::getItems)
            .flatMapMany(Flux::fromIterable)
            .flatMap(converter::convert);
    }

    @Override
    public String getType() {
        return MOMENT_DOCUMENT_TYPE;
    }

    private PageRequest createPageRequest() {
        return PageRequestImpl.of(1, SEARCH_DEFAULT_PAGE_SIZE,
            Sort.by("metadata.creationTimestamp", "metadata.name"));
    }
}
