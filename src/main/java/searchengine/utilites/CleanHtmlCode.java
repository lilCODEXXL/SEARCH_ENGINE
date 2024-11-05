package searchengine.utilites;

import lombok.experimental.UtilityClass;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@UtilityClass
public class CleanHtmlCode {
    public static String clear(String content, String selector) {
        Document doc = Jsoup.parse(content);
        Elements elements = doc.select(selector);

        StringBuilder text = new StringBuilder();
        for (Element el : elements) {
            text.append(el.text());
        }
        return text.toString();
    }
}
