package searchengine.services.dto;

import lombok.Value;

@Value
public class PageStatistics {
    String url;
    String content;
    int code;
}
