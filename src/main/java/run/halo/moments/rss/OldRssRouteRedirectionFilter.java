package run.halo.moments.rss;

import java.net.URI;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import run.halo.app.security.AdditionalWebFilter;

/**
 * <p>The detail page address of the moment is also <code>/moments/{name}</code>, so you need to
 * use a filter for redirection instead of directly using the route.</p>
 */
public class OldRssRouteRedirectionFilter implements AdditionalWebFilter {
    private final DefaultServerRedirectStrategy redirectStrategy =
        new DefaultServerRedirectStrategy();
    private final ServerWebExchangeMatcher requestMatcher = ServerWebExchangeMatchers.pathMatchers(
        HttpMethod.GET, "/moments/rss.xml");

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        return requestMatcher.matches(exchange)
            .flatMap(matchResult -> {
                if (matchResult.isMatch()) {
                    redirectStrategy.setHttpStatus(HttpStatus.PERMANENT_REDIRECT);
                    return redirectStrategy
                        .sendRedirect(exchange, URI.create("/feed/moments/rss.xml"));
                }
                return chain.filter(exchange);
            });
    }
}
