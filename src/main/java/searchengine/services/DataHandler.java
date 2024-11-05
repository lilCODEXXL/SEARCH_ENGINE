package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.persistence.model.*;
import searchengine.persistence.repository.IndexRepository;
import searchengine.persistence.repository.LemmaRepository;
import searchengine.persistence.repository.PageRepository;
import searchengine.persistence.repository.SiteRepository;
import searchengine.services.dto.IndexStatistics;
import searchengine.services.dto.LemmaStatistics;
import searchengine.services.dto.PageStatistics;
import searchengine.services.parsers.IndexParser;
import searchengine.services.parsers.LemmaParser;
import searchengine.services.parsers.UrlParser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataHandler {

    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaParser lemmaParser;
    private final IndexParser indexParser;
    private final UrlParser urlParser;

    public void run(searchengine.config.Site site) {
        deleteExistingDataIfPresent(site);
        Site siteTable = initializeSite(site);

        try {
            processPages(siteTable);
            processLemmas(siteTable);
            processIndexes(siteTable);
            finalizeSiteStatus(siteTable, Status.INDEXED, null);
            log.info("Индексация успешно завершена для сайта - {}", site.getUrl());
        } catch (Exception e) {
            log.error("Ошибка при индексации сайта {}: {}", site.getUrl(), e.getMessage());
            finalizeSiteStatus(siteTable, Status.FAILED, e.getMessage());
        }
    }

    private void deleteExistingDataIfPresent(searchengine.config.Site site) {
        siteRepository.findByUrl(site.getUrl()).ifPresent(existingSite -> {
            log.info("Удаление данных для сайта - {}", site.getUrl());
            siteRepository.delete(existingSite);
        });
    }

    private Site initializeSite(searchengine.config.Site site) {
        log.info("Инициализация индексации для сайта - {} {}", site.getUrl(), site.getName());
        Site siteTable = Site.builder()
                .url(site.getUrl())
                .name(site.getName())
                .status(Status.INDEXING)
                .build();
        return siteRepository.save(siteTable);
    }

    private void processPages(Site siteTable) throws Exception {
        List<PageStatistics> pages = collectPageStatistics(siteTable);
        log.info("Сохранение {} страниц для сайта {}", pages.size(), siteTable.getUrl());
        pageRepository.saveAll(mapToPageEntities(siteTable, pages));
    }

    private List<PageStatistics> collectPageStatistics(Site site) {
        log.info("Начало сбора статистики страниц для сайта {}", site.getUrl());
        return urlParser.compute(site.getUrl());
    }

    private List<Page> mapToPageEntities(Site site, List<PageStatistics> pages) {
        return pages.stream()
                .map(page -> Page.builder()
                        .siteId(site)
                        .path(page.getUrl().substring(page.getUrl().indexOf(site.getUrl()) + site.getUrl().length()))
                        .code(page.getCode())
                        .content(page.getContent())
                        .build())
                .collect(Collectors.toList());
    }

    private void processLemmas(Site siteTable) throws Exception {
        List<LemmaStatistics> lemmas = lemmaParser.run();
        log.info("Сохранение {} лемм для сайта {}", lemmas.size(), siteTable.getUrl());
        lemmaRepository.saveAll(lemmas.stream()
                .map(lemmaStat -> Lemma.builder()
                        .lemma(lemmaStat.getLemma())
                        .frequency(lemmaStat.getFrequency())
                        .siteTable(siteTable)
                        .build())
                .collect(Collectors.toList()));
    }

    private void processIndexes(Site siteTable) throws Exception {
        List<IndexStatistics> indexes = indexParser.run(siteTable);
        log.info("Сохранение {} индексов для сайта {}", indexes.size(), siteTable.getUrl());
        indexRepository.saveAll(indexes.stream()
                .map(indexStat -> Index.builder()
                        .page(pageRepository.getReferenceById(indexStat.getPageId()))
                        .lemma(lemmaRepository.getReferenceById(indexStat.getLemmaId()))
                        .rank(indexStat.getRank())
                        .build())
                .collect(Collectors.toList()));
    }

    private void finalizeSiteStatus(Site site, Status status, String errorMessage) {
        site.setStatus(status);
        site.setStatusTime(LocalDateTime.now());

        if (errorMessage != null && !errorMessage.isEmpty()) {
            site.setLastError(errorMessage);
        } else {
            site.setLastError(null);
        }
        siteRepository.save(site);
    }
}
