package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.controllers.response.BadRequest;
import searchengine.controllers.response.Response;
import searchengine.controllers.response.StatisticsResponse;
import searchengine.persistence.model.Site;
import searchengine.persistence.repository.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;
import searchengine.services.dto.SearchResults;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SiteRepository siteRepository;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("startIndexing")
    public ResponseEntity<Object> startIndexing() {
        if (indexingService.startIndexing()) {
            return new ResponseEntity<>(new Response(true), HttpStatus.OK);
        }
        return new ResponseEntity<>(new BadRequest(false, "Indexing already started"), HttpStatus.BAD_REQUEST);
    }

    @GetMapping("stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        if (indexingService.stopIndexing()) {
            return new ResponseEntity<>(new Response(true), HttpStatus.OK);
        }
        return new ResponseEntity<>(new BadRequest(false, "Indexing not started"), HttpStatus.BAD_REQUEST);
    }

    @GetMapping("search")
    public ResponseEntity<Object> search(@RequestParam(name = "query", required = false, defaultValue = "") String query,
                                         @RequestParam(name = "site", required = false, defaultValue = "") String site,
                                         @RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                         @RequestParam(name = "pageSize", required = false, defaultValue = "20") int pageSize) {
        if (query.isEmpty()) {
            return new ResponseEntity<>(new BadRequest(false, "Request is empty"), HttpStatus.BAD_REQUEST);
        }

        List<SearchResults.SearchStatistics> searchData;
        Optional<Site> siteByUrl = siteRepository.findByUrl(site);
        if (!site.isBlank() && siteByUrl.isEmpty()) {
            return new ResponseEntity<>(new BadRequest(false, "Required page not found"),
                    HttpStatus.BAD_REQUEST);
        } else if (!site.isBlank() && siteByUrl.isPresent()) {
            searchData = searchService.siteSearch(query, siteByUrl.get()).stream().toList();
        } else {
            searchData = searchService.allSiteSearch(query, pageSize).stream().toList();
        }

        // Начальный индекс и конечный индекс для порции данных
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, searchData.size());

        List<SearchResults.SearchStatistics> paginatedData = searchData.subList(startIndex, endIndex);

        return new ResponseEntity<>(SearchResults.builder()
                .result(true)
                .count(searchData.size())
                .data(paginatedData)
                .build(),
                HttpStatus.OK);
    }

    @PostMapping("indexPage")
    public ResponseEntity<Object> indexPage(@RequestParam String url) {
        if (url.isEmpty()) {
            log.info("Page is empty");
            return new ResponseEntity<>(new BadRequest(false, "Page is empty"), HttpStatus.BAD_REQUEST);
        }
        if (indexingService.urlIndexing(url)) {
            log.info("Page " + url + " added for indexing");
            return new ResponseEntity<>(new Response(true), HttpStatus.OK);
        } else {
            log.info("Required page is out of configuration file");
            return new ResponseEntity<>(new BadRequest(false, "Required page is out of configuration file"),
                    HttpStatus.BAD_REQUEST);
        }
    }
}