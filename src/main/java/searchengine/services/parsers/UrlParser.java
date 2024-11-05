package searchengine.services.parsers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import searchengine.config.AppProp;
import searchengine.services.dto.PageStatistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UrlParser {
    private final AppProp appProp;

    public List<PageStatistics> compute(String address) {
        log.info("Start compute {}", address);
        List<PageStatistics> pageStatisticsList = Collections.synchronizedList(new ArrayList<>());
        List<String> addressList = Collections.synchronizedList(new ArrayList<>());
        handler(address, pageStatisticsList, addressList);
        return pageStatisticsList;
    }

    private void handler(String address, List<PageStatistics> pageStatisticsList, List<String> addressList) {
        List<String> taskList = new ArrayList<>();

        Optional<Document> document = getConnect(address);
        if (document.isEmpty()) {
            pageStatisticsList.add(new PageStatistics(address, "", 500));
            return;
        }

        String html = document.get().outerHtml();
        int statusCode = document.get().connection().response().statusCode();
        pageStatisticsList.add(new PageStatistics(address, html, statusCode));

        Elements elements = document.get().select("body a");
        elements.parallelStream()
                .forEach(el -> handleElement(el, taskList, addressList));
        taskList.parallelStream()
                .forEach(tl -> handler(tl, pageStatisticsList, addressList));
    }

    private Optional<Document> getConnect(String url) {
        try {
            Thread.sleep(150);
            return Optional.of(Jsoup.connect(url)
                    .userAgent(appProp.getUserAgent())
                    .referrer(appProp.getReferrer())
                    .get());
        } catch (Exception e) {
            log.debug("Ошибка подключения к сайту: {}", url, e);
        }
        return Optional.empty();
    }

    private void handleElement(Element el, List<String> taskList, List<String> addressList) {
        String link = el.attr("abs:href");
        if (isCorrect(el, addressList, link)) {
            taskList.add(link);
            addressList.add(link);
        }
    }

    private static boolean isCorrect(Element el, List<String> addressList, String link) {
        return link.startsWith(el.baseUri()) && !link.equals(el.baseUri()) && !link.contains("#")
                && !link.matches(".*\\.(pdf|jpg|png|JPG)$") && !addressList.contains(link);
    }
}
