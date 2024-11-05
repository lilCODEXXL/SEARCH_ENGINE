package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.persistence.model.Status;
import searchengine.persistence.repository.SiteRepository;
import searchengine.services.parsers.SiteIndexingTask;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingService {
    private final DataHandler dataHandler;
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private ForkJoinPool forkJoinPool;

    private boolean checkSite(String url) {
        return sitesList.getSites().stream().anyMatch(site -> site.getUrl().equals(url));
    }

    public boolean isIndexing() {
        return siteRepository.findAll().stream()
                .anyMatch(site -> Objects.equals(site.getStatus(), Status.INDEXING));
    }

    public boolean urlIndexing(String url) {
        if (!checkSite(url)) {
            log.info("Сайт с URL {} не найден в списке сайтов для индексации", url);
            return false;
        }
        log.info("Запуск переиндексации для сайта - {}", url);

        new Thread(() -> {
            try {
                Site site = sitesList.getSites().stream()
                        .filter(s -> s.getUrl().equals(url))
                        .findFirst()
                        .orElseThrow(() -> new NoSuchElementException("Сайт с таким URL не найден: " + url));

                List<Site> sites = new ArrayList<>();
                sites.add(site);

                forkJoinPool = new ForkJoinPool();
                ForkJoinTask<Void> task = new SiteIndexingTask(dataHandler, sites);
                forkJoinPool.invoke(task);
            } catch (Exception e) {
                log.error("Ошибка при переиндексации сайта - {}", url, e);
            }
        }).start();

        return true;
    }

    public boolean startIndexing() {
        if (isIndexing()) {
            log.info("Индексация уже запущена");
            return false;
        }

        log.info("Запуск индексации для всех сайтов из конфигурации");

        new Thread(() -> {
            List<Site> sites = new ArrayList<>(sitesList.getSites());
            forkJoinPool = new ForkJoinPool();
            ForkJoinTask<Void> task = new SiteIndexingTask(dataHandler, sites);
            forkJoinPool.invoke(task);
        }).start();

        return true;
    }

    public boolean stopIndexing() {
        if (!isIndexing()) {
            log.info("Остановка индексации не выполнена, так как индексация не была запущена");
            return false;
        }

        if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
            forkJoinPool.shutdownNow();
            log.info("Индексация была остановлена");
        }

        siteRepository.findAllByStatus(Status.INDEXING).forEach(site -> {
            site.setLastError("Индексация остановлена пользователем");
            site.setStatus(Status.FAILED);
            siteRepository.save(site);
            log.info("Статус сайта {} изменён на FAILED", site.getUrl());
        });

        return true;
    }
}
