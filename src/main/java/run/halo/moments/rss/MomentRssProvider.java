package run.halo.moments.rss;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.halo.app.core.attachment.ThumbnailSize;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.app.infra.SystemInfoGetter;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.feed.RSS2;
import run.halo.feed.RssRouteItem;
import run.halo.moments.Moment;
import run.halo.moments.finders.MomentFinder;
import run.halo.moments.vo.MomentVo;

@RequiredArgsConstructor
public class MomentRssProvider implements RssRouteItem {
    private final ExternalUrlSupplier externalUrlSupplier;
    private final ExternalLinkProcessor externalLinkProcessor;
    private final ReactiveExtensionClient client;
    private final ReactiveSettingFetcher settingFetcher;
    private final MomentFinder momentFinder;
    private final SystemInfoGetter systemInfoGetter;

    @Override
    public Mono<String> pathPattern() {
        return Mono.fromSupplier(() -> "/moments/rss.xml");
    }

    @Override
    @NonNull
    public String displayName() {
        return "瞬间";
    }

    @Override
    public String description() {
        return "瞬间 RSS";
    }

    @Override
    public String example() {
        return "https://example.com/feed/moments/rss.xml";
    }

    @Override
    public Mono<RSS2> handler(ServerRequest request) {
        return buildRss(request);
    }

    private Mono<RSS2> buildRss(ServerRequest request) {
        var externalUrl = externalUrlSupplier.getURL(request.exchange().getRequest());

        var builder = RSS2.builder();
        var rssMono = systemInfoGetter.get()
            .flatMap(info -> getMomentPageTitle()
                .doOnNext(momentTitle -> info.setTitle(
                    String.join(" | ", info.getTitle(), momentTitle))
                )
                .thenReturn(info)
            )
            .map(basic -> builder
                .title(basic.getTitle())
                .image(externalLinkProcessor.processLink(basic.getLogo()))
                .description(StringUtils.defaultIfBlank(basic.getSubtitle(),
                    basic.getTitle()))
                .link(externalUrl.toString())
            )
            .subscribeOn(Schedulers.boundedElastic());

        var rssItemMono = momentFinder.listAll()
            .map(moment -> {
                var permalink = getMomentPermalink(moment);
                var medium = moment.getSpec().getContent().getMedium();
                var mediumHtml = generateMediaHtmlList(medium);
                var htmlContent = processHtml(moment.getSpec().getContent().getHtml());
                return RSS2.Item.builder()
                    .title(buildMomentTitle(moment))
                    .link(externalLinkProcessor.processLink(permalink))
                    .pubDate(moment.getSpec().getReleaseTime())
                    .guid(permalink)
                    .description(htmlContent + mediumHtml)
                    .build();
            })
            .collectList()
            .doOnNext(builder::items)
            .subscribeOn(Schedulers.boundedElastic());
        return Mono.when(rssMono, rssItemMono)
            .then(Mono.fromSupplier(builder::build));
    }

    private String processHtml(String html) {
        var document = Jsoup.parse(html);

        // Process all links
        var links = document.select("a[href]");
        for (Element link : links) {
            var isTag = link.hasClass("tag");
            String href = link.attr("href");
            if (isTag && href.startsWith("?")) {
                // 兼容旧版标签链接
                href = "/moments" + href;
            }
            var absoluteUrl = externalLinkProcessor.processLink(href);
            link.attr("href", absoluteUrl);
        }
        // process all images
        var images = document.select("img[src]");
        for (Element image : images) {
            String src = image.attr("src");
            var thumb = genThumbUrl(src, ThumbnailSize.M);
            var absoluteUrl = externalLinkProcessor.processLink(thumb);
            image.attr("src", absoluteUrl);
        }
        return document.body().html();
    }

    private String genThumbUrl(String url, ThumbnailSize size) {
        return externalLinkProcessor.processLink(
            "/apis/api.storage.halo.run/v1alpha1/thumbnails/-/via-uri?uri=" + url + "&size="
                + size.name().toLowerCase()
        );
    }

    private String generateMediaHtmlList(List<Moment.MomentMedia> medium) {
        if (CollectionUtils.isEmpty(medium)) {
            return "";
        }
        return medium.stream()
            .map(this::generateSingleMediaHtml)
            .collect(Collectors.joining());
    }

    private String generateSingleMediaHtml(Moment.MomentMedia media) {
        var url = media.getUrl();
        return switch (media.getType()) {
            case PHOTO -> generatePhotoHtml(media);
            case VIDEO ->
                String.format("<video controls><source src=\"%s\" type=\"video/mp4\" /></video>",
                    url);
            case AUDIO ->
                String.format("<audio controls><source src=\"%s\" type=\"audio/mpeg\" /></audio>",
                    url);
            case POST -> String.format("<a href=\"%s\">%s</a>", url, url);
        };
    }

    private String generatePhotoHtml(Moment.MomentMedia media) {
        // the best practice is to use the thumbnail for src
        var mSrc = genThumbUrl(media.getUrl(), ThumbnailSize.M);
        // If the reader does not support srcset, then only src,
        var srcSet = """
            %s 400w,
            %s 800w,
            %s 1200w,
            """.formatted(
            genThumbUrl(media.getUrl(), ThumbnailSize.S),
            mSrc,
            genThumbUrl(media.getUrl(), ThumbnailSize.L)
        );
        return String.format(
            "<img src=\"%s\"%s alt=\"moment photo\" />",
            mSrc,
            srcSet
        );
    }

    private static <T> List<T> nullSafeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static String getMomentPermalink(MomentVo moment) {
        return "moments/" + moment.getMetadata().getName();
    }

    private static String buildMomentTitle(MomentVo momentVo) {
        return momentVo.getOwner().getDisplayName() + " published on "
            + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(momentVo.getSpec().getReleaseTime());
    }

    Mono<String> getMomentPageTitle() {
        return this.settingFetcher.get("base")
            .map(setting -> setting.get("title").asText("瞬间"))
            .defaultIfEmpty("瞬间");
    }
}
