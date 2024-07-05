package run.halo.moments.search;

import static run.halo.moments.search.MomentHaloDocumentsProvider.MOMENT_DOCUMENT_TYPE;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.app.search.HaloDocument;
import run.halo.moments.Moment;

/**
 * @author LIlGG
 */
@Component
@RequiredArgsConstructor
public class DocumentConverter implements Converter<Moment, Mono<HaloDocument>> {

    private final ReactiveExtensionClient client;

    private final ExternalUrlSupplier externalUrlSupplier;

    private final DateTimeFormatter dateFormat =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    @NonNull
    public Mono<HaloDocument> convert(Moment moment) {
        var haloDoc = new HaloDocument();
        var momentContent = moment.getSpec().getContent();
        haloDoc.setMetadataName(moment.getMetadata().getName());
        haloDoc.setType(MOMENT_DOCUMENT_TYPE);
        haloDoc.setId(haloDocId(moment));
        haloDoc.setDescription(momentContent.getHtml());
        haloDoc.setExposed(isExposed(moment));
        haloDoc.setContent(momentContent.getHtml());
        haloDoc.setTags(moment.getSpec().getTags().stream().toList());
        haloDoc.setOwnerName(moment.getSpec().getOwner());
        haloDoc.setUpdateTimestamp(moment.getSpec().getReleaseTime());
        haloDoc.setCreationTimestamp(moment.getMetadata().getCreationTimestamp());
        haloDoc.setPermalink(getPermalink(moment));
        haloDoc.setPublished(true);

        return Mono.when(getTitle(moment).doOnNext(haloDoc::setTitle))
            .then(Mono.fromSupplier(() -> haloDoc));
    }

    String haloDocId(Moment moment) {
        return MOMENT_DOCUMENT_TYPE + '-' + moment.getMetadata().getName();
    }

    private Mono<String> getTitle(Moment moment) {
        return client.fetch(User.class, moment.getSpec().getOwner())
            .map(user -> user.getSpec().getDisplayName())
            .map(displayName -> {
                ZonedDateTime zonedDateTime =
                    moment.getSpec().getReleaseTime().atZone(ZoneId.systemDefault());
                return "发表于：" + dateFormat.format(zonedDateTime)
                    + " by " + displayName;
            });
    }

    private String getPermalink(Moment moment) {
        var externalUrl = externalUrlSupplier.get();
        return externalUrl.resolve("moments/" + moment.getMetadata().getName()).toString();
    }

    private static boolean isExposed(Moment moment) {
        var visible = moment.getSpec().getVisible();
        return Moment.MomentVisible.PUBLIC.equals(visible);
    }
}
