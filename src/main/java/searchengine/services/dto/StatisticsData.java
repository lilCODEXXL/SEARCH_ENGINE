package searchengine.services.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class StatisticsData {
    private TotalStatistics total;
    private List<DetailedStatistics> detailed;

    @Data
    @Builder
    public static class DetailedStatistics {
        String url;
        String name;
        String status;
        LocalDateTime statusTime;
        String error;
        int pages;
        int lemmas;
    }
}
