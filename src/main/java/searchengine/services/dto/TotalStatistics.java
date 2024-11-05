package searchengine.services.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TotalStatistics {
    private int sites;
    private int pages;
    private int lemmas;
    private boolean indexing;
}
