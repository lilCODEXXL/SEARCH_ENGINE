package searchengine.services.parsers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.persistence.model.Lemma;
import searchengine.persistence.model.Page;
import searchengine.persistence.model.Site;
import searchengine.persistence.repository.LemmaRepository;
import searchengine.persistence.repository.PageRepository;
import searchengine.services.dto.IndexStatistics;
import searchengine.utilites.CleanHtmlCode;
import searchengine.utilites.Morphology;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexParser {

    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    public List<IndexStatistics> run(Site site) {
        List<Page> pages = pageRepository.findAllBySiteId(site.getId());
        List<Lemma> lemmas = lemmaRepository.findBySiteTableId(site.getId());

        return pages.parallelStream()
                .filter(page -> page.getCode() < 400)
                .flatMap(page -> processPage(page, lemmas).stream())
                .collect(Collectors.toList());
    }

    private List<IndexStatistics> processPage(Page page, List<Lemma> lemmas) {
        Map<String, Integer> titleLemmas = extractLemmas(page.getContent(), "title");
        Map<String, Integer> bodyLemmas = extractLemmas(page.getContent(), "body");

        return titleLemmas.keySet().stream()
                .map(lemma -> createIndexStatistics(lemma, titleLemmas, bodyLemmas, page, lemmas))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private Map<String, Integer> extractLemmas(String content, String tag) {
        String text = CleanHtmlCode.clear(content, tag);
        return Morphology.getLemmaList(text);
    }

    private Optional<IndexStatistics> createIndexStatistics(
            String lemmaWord, Map<String, Integer> titleLemmas, Map<String, Integer> bodyLemmas, Page page, List<Lemma> lemmas) {
        return lemmas.stream()
                .filter(lemma -> lemma.getLemma().equals(lemmaWord))
                .findFirst()
                .map(lemma -> {
                    float rank = calculateRank(titleLemmas, bodyLemmas, lemma.getLemma());
                    return rank > 0.0 ? new IndexStatistics(page.getId(), lemma.getId(), rank) : null;
                });
    }

    private float calculateRank(Map<String, Integer> titleLemmas, Map<String, Integer> bodyLemmas, String lemma) {
        float rank = 0.0F;
        rank += titleLemmas.getOrDefault(lemma, 0);
        rank += bodyLemmas.getOrDefault(lemma, 0) * 0.8F;
        return rank;
    }
}
