package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.controllers.response.StatisticsResponse;
import searchengine.persistence.model.Site;
import searchengine.persistence.repository.LemmaRepository;
import searchengine.persistence.repository.PageRepository;
import searchengine.persistence.repository.SiteRepository;
import searchengine.services.dto.StatisticsData;
import searchengine.services.dto.TotalStatistics;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;

    public StatisticsResponse getStatistics() {
        return new StatisticsResponse(true, StatisticsData.builder()
                .total(totalStatistics())
                .detailed(detailedStatisticsToList())
                .build());
    }

    private TotalStatistics totalStatistics() {
        return TotalStatistics.builder()
                .sites(Integer.parseInt(String.valueOf(siteRepository.count())))
                .pages(Integer.parseInt(String.valueOf(pageRepository.count())))
                .lemmas(Integer.parseInt(String.valueOf(lemmaRepository.count())))
                .indexing(true)
                .build();
    }

    private StatisticsData.DetailedStatistics detailedStatistics(Site site) {
        return StatisticsData.DetailedStatistics.builder()
                .url(site.getUrl())
                .name(site.getName())
                .status(String.valueOf(site.getStatus()))
                .statusTime(site.getStatusTime())
                .error(site.getLastError())
                .pages(pageRepository.countBySiteId(site))
                .lemmas(lemmaRepository.countById(site.getId()))
                .build();
    }

    private List<StatisticsData.DetailedStatistics> detailedStatisticsToList() {
        return siteRepository.findAll().stream()
                .map(this::detailedStatistics)
                .collect(Collectors.toList());
    }
}
