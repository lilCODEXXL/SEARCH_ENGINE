package searchengine.services.dto;

import lombok.Value;

@Value
public class LemmaStatistics {
    String lemma;
    int frequency;
}
