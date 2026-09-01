package run.halo.moments.mcp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.moments.service.MomentService;

@Configuration
@ConditionalOnClass(name = "run.halo.mcpserver.api.McpToolProvider")
public class McpToolConfiguration {

    @Bean
    MomentMcpToolProvider momentMcpToolProvider(
        ReactiveExtensionClient client, MomentService momentService) {
        return new MomentMcpToolProvider(client, momentService);
    }
}
