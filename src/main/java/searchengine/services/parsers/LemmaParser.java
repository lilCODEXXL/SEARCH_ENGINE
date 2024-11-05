package searchengine.services.parsers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.persistence.model.Page;
import searchengine.persistence.repository.PageRepository;
import searchengine.services.dto.LemmaStatistics;
import searchengine.utilites.CleanHtmlCode;
import searchengine.utilites.Morphology;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static searchengine.constans.Constants.COMBINED_REGEX;

@Component
@RequiredArgsConstructor
public class LemmaParser {

    private final PageRepository pageRepository;

    public List<LemmaStatistics> run() {
        List<Page> pages = pageRepository.findAll();
        Map<String, Integer> lemmaMap = new ConcurrentHashMap<>();

        pages.parallelStream().forEach(page -> {
            Map<String, Integer> titleLemmas = extractLemmas(page.getContent(), "title");
            Map<String, Integer> bodyLemmas = extractLemmas(page.getContent(), "body");

            titleLemmas.keySet().forEach(word -> addLemma(lemmaMap, word));
            bodyLemmas.keySet().forEach(word -> addLemma(lemmaMap, word));
        });

        return lemmaMap.entrySet().stream()
                .map(entry -> new LemmaStatistics(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private Map<String, Integer> extractLemmas(String content, String tag) {
        String text = CleanHtmlCode.clear(content, tag);
        return Morphology.getLemmaList(text).entrySet().stream()
                .filter(entry -> entry.getKey().matches(COMBINED_REGEX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void addLemma(Map<String, Integer> lemmaMap, String word) {
        lemmaMap.merge(word, 1, Integer::sum);
    }
}
