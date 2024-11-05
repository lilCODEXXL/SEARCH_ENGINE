package searchengine.controllers.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import searchengine.services.dto.StatisticsData;

@Data
@AllArgsConstructor
public class StatisticsResponse {
    boolean result;
    StatisticsData statistics;
}
