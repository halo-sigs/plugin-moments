package run.halo.moments.rss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.app.infra.SystemInfo;
import run.halo.app.infra.SystemInfoGetter;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.moments.Moment;
import run.halo.moments.finders.MomentFinder;
import run.halo.moments.vo.ContributorVo;
import run.halo.moments.vo.MomentVo;

@ExtendWith(MockitoExtension.class)
class MomentRssProviderTest {

    @Mock
    ExternalUrlSupplier externalUrlSupplier;

    @Mock
    ExternalLinkProcessor externalLinkProcessor;

    @Mock
    ReactiveExtensionClient client;

    @Mock
    ReactiveSettingFetcher settingFetcher;

    @Mock
    MomentFinder momentFinder;

    @Mock
    SystemInfoGetter systemInfoGetter;

    @InjectMocks
    MomentRssProvider provider;

    @Test
    void shouldExcludePrivateMoments() throws MalformedURLException {
        var request = org.mockito.Mockito.mock(ServerRequest.class);
        var exchange = org.mockito.Mockito.mock(ServerWebExchange.class);
        var httpRequest = org.mockito.Mockito.mock(ServerHttpRequest.class);
        when(request.exchange()).thenReturn(exchange);
        when(exchange.getRequest()).thenReturn(httpRequest);
        when(externalUrlSupplier.getURL(any())).thenReturn(
            URI.create("https://example.com").toURL());
        when(externalLinkProcessor.processLink(anyString()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(systemInfoGetter.get()).thenReturn(Mono.just(new SystemInfo()
            .setTitle("Example")
            .setSubtitle("Example feed")
            .setLogo("/logo.png")));
        when(settingFetcher.get("base")).thenReturn(Mono.empty());
        var privateMoment = moment("private", Moment.MomentVisible.PRIVATE);
        var publicMoment = moment("public", Moment.MomentVisible.PUBLIC);
        when(momentFinder.listAll()).thenReturn(Flux.just(privateMoment, publicMoment));

        var rss = provider.handler(request).block();

        assertThat(rss.getItems())
            .extracting(item -> item.getGuid())
            .containsExactly("moments/public");
    }

    private static MomentVo moment(String name, Moment.MomentVisible visible) {
        var metadata = new Metadata();
        metadata.setName(name);
        var content = new Moment.MomentContent();
        content.setHtml("<p>content</p>");
        var spec = new Moment.MomentSpec();
        spec.setContent(content);
        spec.setReleaseTime(Instant.EPOCH);
        spec.setVisible(visible);
        return MomentVo.builder()
            .metadata(metadata)
            .spec(spec)
            .owner(ContributorVo.builder().displayName("Owner").build())
            .build();
    }
}
