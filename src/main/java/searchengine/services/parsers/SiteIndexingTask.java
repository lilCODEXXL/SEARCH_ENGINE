package searchengine.services.parsers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.services.DataHandler;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.RecursiveAction;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteIndexingTask extends RecursiveAction {


    private final DataHandler dataHandler;
    private final List<Site> sites;

    @Override
    protected void compute() {
        Collections.synchronizedList(sites).parallelStream().forEach(site -> {
            log.info("Парсим сайт: {}", site.getName());
            dataHandler.run(site);
        });
    }
}
