package searchengine.services.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder
@Value
@Jacksonized
public class SearchResults {
    boolean result;
    int count;
    List<SearchStatistics> data;

    @Value
    @Builder
    @Jacksonized
    public static class SearchStatistics {
        String site;
        String siteName;
        String uri;
        String title;
        String snippet;
        Float relevance;
    }
}
