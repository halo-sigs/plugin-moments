package run.halo.moments.rss;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.app.infra.SystemInfoGetter;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.security.AdditionalWebFilter;
import run.halo.feed.CacheClearRule;
import run.halo.feed.RssCacheClearRequested;
import run.halo.moments.event.MomentDeletedEvent;
import run.halo.moments.event.MomentUpdatedEvent;
import run.halo.moments.finders.MomentFinder;

@Configuration
@ConditionalOnClass(name = "run.halo.feed.RssRouteItem")
@RequiredArgsConstructor
public class RssAutoConfiguration {
    private final ExternalUrlSupplier externalUrlSupplier;
    private final ExternalLinkProcessor externalLinkProcessor;
    private final ReactiveExtensionClient client;
    private final ReactiveSettingFetcher settingFetcher;
    private final MomentFinder momentFinder;
    private final SystemInfoGetter systemInfoGetter;
    private final ApplicationEventPublisher eventPublisher;

    @Bean
    MomentRssProvider momentRssProvider() {
        return new MomentRssProvider(externalUrlSupplier, externalLinkProcessor, client,
                settingFetcher, momentFinder, systemInfoGetter);
    }

    @Async
    @EventListener({MomentUpdatedEvent.class, MomentDeletedEvent.class})
    public void onMomentUpdatedOrDeleted() {
        var rule = CacheClearRule.forExact("/feed/moments/rss.xml");
        var event = RssCacheClearRequested.forRule(this, rule);
        eventPublisher.publishEvent(event);
    }

    @Bean
    AdditionalWebFilter oldRssRedirectWebFilter() {
        return new OldRssRouteRedirectionFilter();
    }
}
