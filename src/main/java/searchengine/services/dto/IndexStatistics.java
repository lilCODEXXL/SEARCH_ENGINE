package searchengine.services.dto;

import lombok.Data;

@Data
public class IndexStatistics {
    private int pageId;
    private int lemmaId;
    private Float rank;

    public IndexStatistics(int pageId, int lemmaId, Float rank) {
        this.pageId = pageId;
        this.lemmaId = lemmaId;
        this.rank = rank;
    }

}
