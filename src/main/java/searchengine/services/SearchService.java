package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.persistence.model.Index;
import searchengine.persistence.model.Lemma;
import searchengine.persistence.model.Page;
import searchengine.persistence.model.Site;
import searchengine.persistence.repository.IndexRepository;
import searchengine.persistence.repository.LemmaRepository;
import searchengine.persistence.repository.PageRepository;
import searchengine.persistence.repository.SiteRepository;
import searchengine.services.dto.SearchResults;
import searchengine.utilites.CleanHtmlCode;
import searchengine.utilites.Morphology;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;

    public List<SearchResults.SearchStatistics> allSiteSearch(String searchText, int limit) {

        try {
            log.info("Getting results of the search \"{}\"", searchText);
            List<Site> siteList = siteRepository.findAll();
            List<String> lemmasFromRequest = getLemmaFromSearchText(searchText);

            List<Lemma> foundLemmaList = siteList.stream()
                    .flatMap(site -> getLemmaListFromSite(lemmasFromRequest, site).stream())
                    .collect(Collectors.toList());

            List<SearchResults.SearchStatistics> searchData = getFilteredSearchStatistics(foundLemmaList, lemmasFromRequest);
            searchData.sort(Comparator.comparing(SearchResults.SearchStatistics::getRelevance));
            log.info("Search done. Got results.");

            return searchData.stream().limit(limit).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("", e);
            return null;
        }
    }

    public List<SearchResults.SearchStatistics> siteSearch(String searchText, Site site) {
        log.info("Searching for \"{}\" in - {}", searchText, site.getUrl());
        List<String> textLemmaList = getLemmaFromSearchText(searchText);
        List<Lemma> foundLemmaList = getLemmaListFromSite(textLemmaList, site);

        List<SearchResults.SearchStatistics> filteredStatistics = getFilteredSearchStatistics(foundLemmaList, textLemmaList);

        log.info("Search done. Got results.");

        return filteredStatistics;
    }

    private List<String> getLemmaFromSearchText(String searchText) {
        String[] words = searchText.toLowerCase().split(" ");
        return Arrays.stream(words)
                .map(Morphology::getLemma)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<Lemma> getLemmaListFromSite(List<String> lemmas, Site site) {
        return lemmaRepository.findLemmaListBySiteTable(lemmas, site.getId())
                .stream()
                .sorted(Comparator.comparing(Lemma::getFrequency))
                .toList();
    }

    private List<SearchResults.SearchStatistics> getSearchStatisticsList(Map<Page, Float> pageList, List<String> lemmasFromRequest) {
        return pageList.entrySet().stream()
                .map(entry -> {
                    Page page = entry.getKey();
                    Float relevance = entry.getValue();
                    String content = page.getContent();
                    Site pageSite = page.getSiteId();
                    return getSearchStatistics(lemmasFromRequest, page, relevance, content, pageSite);
                }).collect(Collectors.toList());
    }

    private SearchResults.SearchStatistics getSearchStatistics(List<String> textLemmaList, Page page, Float relevance, String content, Site pageSite) {
        return SearchResults.SearchStatistics.builder()
                .site(pageSite.getUrl())
                .siteName(pageSite.getName())
                .uri(page.getPath())
                .title(CleanHtmlCode.clear(content, "title"))
                .snippet(getSnippet(CleanHtmlCode.clear(content, "body"), textLemmaList))
                .relevance(relevance)
                .build();
    }

    private String getSnippet(String content, List<String> lemmaList) {
        List<Integer> lemmaIndex = lemmaList.stream()
                .flatMap(lemma -> Morphology.findLemmaIndexInText(content, lemma).stream())
                .collect(Collectors.toList());

        return generateSnippetFromContent(content, lemmaIndex);
    }

    private String generateSnippetFromContent(String content, List<Integer> lemmaIndex) {
        Collections.sort(lemmaIndex);
        List<String> wordsList = getWordsFromContent(content, lemmaIndex);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < wordsList.size() && i <= 3; i++) {
            result.append(wordsList.get(i)).append("... ");
        }

        return result.toString();
    }

    private List<String> getWordsFromContent(String content, List<Integer> lemmaIndex) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lemmaIndex.size(); i++) {
            int start = lemmaIndex.get(i);
            int finish = content.indexOf(" ", start);
            int nextPoint = i + 1;
            while (nextPoint < lemmaIndex.size() && lemmaIndex.get(nextPoint) - finish > 0 && lemmaIndex.get(nextPoint) - finish < 5) {
                finish = content.indexOf(" ", lemmaIndex.get(nextPoint));
                nextPoint += 1;
            }
            i = nextPoint - 1;
            String text = getWordsFromIndex(start, finish, content);
            result.add(text);
        }
        result.sort(Comparator.comparingInt(String::length));
        return result;
    }

    private String getWordsFromIndex(int startPoint, int endPoint, String content) {
        String word = content.substring(startPoint, endPoint).replaceAll("\\)", "");
        int prevPoint = Math.max(0, content.lastIndexOf(" ", startPoint));
        int lastPoint = Math.min(content.length(), content.indexOf(" ", endPoint + 30));
        String text = content.substring(prevPoint, lastPoint);
        try {
            text = text.replaceAll(word, "<b>" + word + "</b>");
        } catch (Exception e) {
            log.error("Error with parsing in method getWordsFromIndex word {}", word, e);
        }
        return text;
    }

    private List<SearchResults.SearchStatistics> getFilteredSearchStatistics(List<Lemma> lemmaList, List<String> lemmasFromRequest) {
        List<Page> foundPageList = pageRepository.findIdsByLemmaList(lemmaList.stream().map(Lemma::getId).toList());
        List<Index> foundIndexList = indexRepository.findByPageTableAndLemmaTable(lemmaList.stream().map(Lemma::getId).toList(),
                foundPageList.stream().map(Page::getId).toList());

        Map<Page, Float> sortedPageByAbsRelevance = getPageAbsRelevance(foundPageList, foundIndexList);
        return getSearchStatisticsList(sortedPageByAbsRelevance, lemmasFromRequest);
    }

    private Map<Page, Float> getPageAbsRelevance(List<Page> pageList, List<Index> indexList) {
        Map<Page, Float> pageWithRelevance = new HashMap<>();
        if (!pageList.isEmpty()) {
            pageList.forEach(page -> {
                double sum = indexList.stream()
                        .filter(index -> index.getPage().equals(page))
                        .mapToDouble(Index::getRank)
                        .sum();
                pageWithRelevance.put(page, (float) sum);
            });
        }

        Map<Page, Float> pageWithAbsRelevance = new HashMap<>();
        for (Page page : pageWithRelevance.keySet()) {
            float relativeRelevance = pageWithRelevance.get(page) / Collections.max(pageWithRelevance.values());
            pageWithAbsRelevance.put(page, relativeRelevance);
        }
        return pageWithAbsRelevance.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, Hashtable::new));
    }
}