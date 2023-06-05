package run.halo.moments.util;

import lombok.Builder;
import lombok.Data;
import org.springframework.util.PropertyPlaceholderHelper;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Data
@Builder
public class RSS2 {
    private static final PropertyPlaceholderHelper PLACEHOLDER_HELPER = new PropertyPlaceholderHelper("${", "}");
    private String title;
    
    private String link;
    
    private String description;
    
    private List<Item> items;
    
    @Data
    @Builder
    public static class Item {
        private String title;
        
        private String link;
        
        private String description;
        
        private Instant pubDate;
        
        private String guid;
    }
    
    public String toXmlString() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                %s
            </rss>
            """.formatted(channelTag(this));
    }
    
    String channelTag(RSS2 rss) {
        String channelItems = rss2ChannelItemsString(rss.getItems());
        Properties properties = new Properties();
        properties.put("title", rss.getTitle());
        properties.put("link", rss.getLink());
        properties.put("description", rss.getDescription());
        properties.put("lastBuildDate", Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME));
        properties.put("channelItems", channelItems);
        return PLACEHOLDER_HELPER.replacePlaceholders("""
            <channel>
                <title>${title}</title>
                <link>${link}</link>
                <description>${description}</description>
                <lastBuildDate>${lastBuildDate}</lastBuildDate>
                ${channelItems}
            </channel>
            """, properties);
    }
    
    String rss2ChannelItemsString(List<RSS2.Item> itemList) {
        return itemList.stream()
            .map(item -> {
                Properties properties = new Properties();
                properties.put("title", item.getTitle());
                properties.put("link", item.getLink());
                properties.put("description", item.getDescription());
                properties.put("guid", item.guid);
                properties.put("pubDate", item.pubDate.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME));
                return PLACEHOLDER_HELPER.replacePlaceholders("""
                        <item>
                        <title>${title}</title>
                        <link>${link}</link>
                        <description>${description}</description>
                        <guid>${guid}</guid>
                        <pubDate>${pubDate}</pubDate>
                    </item>
                    """, properties);
            })
            .collect(Collectors.joining("\n"));
    }
}