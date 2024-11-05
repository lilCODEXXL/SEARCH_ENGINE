package searchengine.utilites;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.IntStream;

import static searchengine.constans.Constants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class Morphology {
    private static EnglishLuceneMorphology englishLuceneMorphology;
    private static RussianLuceneMorphology russianLuceneMorphology;
    private static final Marker INVALID_SYMBOL_MARKER = MarkerManager.getMarker("INVALID_SYMBOL");

    static {
        try {
            russianLuceneMorphology = new RussianLuceneMorphology();
            englishLuceneMorphology = new EnglishLuceneMorphology();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public static HashMap<String, Integer> getLemmaList(String content) {
        content = content.toLowerCase(Locale.ROOT)
                .replaceAll(REGEX, "");
        HashMap<String, Integer> lemmaList = new HashMap<>();
        String[] elements = content.toLowerCase(Locale.ROOT).split("\\s+");
        for (String el : elements) {
            List<String> wordsList = getLemma(el);
            for (String word : wordsList) {
                int count = lemmaList.getOrDefault(word, 0);
                lemmaList.put(word, count + 1);
            }
        }
        return lemmaList;
    }

    public static List<String> getLemma(String word) {
        List<String> lemmaList = Collections.synchronizedList(new ArrayList<>());
        try {
            List<String> baseRusForm = new ArrayList<>();
            if (word.matches(CYRILLIC_WORD)) {
                baseRusForm = russianLuceneMorphology.getNormalForms(word);
            }
            List<String> baseEnForm = new ArrayList<>();
            if (word.matches(LATIN_WORD)) {
                baseEnForm = englishLuceneMorphology.getNormalForms(word);
            }
            List<String> normalForms = new ArrayList<>();
            normalForms.addAll(baseRusForm);
            normalForms.addAll(baseEnForm);

            if (!isServiceWord(word) && !normalForms.isEmpty()) {
                lemmaList.addAll(normalForms);
            }
        } catch (Exception e) {
            log.debug(INVALID_SYMBOL_MARKER.getName(), "Symbol not found - {}", word);
        }
        return lemmaList;
    }

    public static List<Integer> findLemmaIndexInText(String content, String lemma) {
        String[] elements = content.toLowerCase().split("\\p{Punct}|\\s");
        List<Integer> lemmaIndexList = new ArrayList<>();

        IntStream.range(0, elements.length)
                .filter(i -> getLemma(elements[i]).contains(lemma))
                .forEach(i -> lemmaIndexList.add(getIndex(elements, i)));

        return lemmaIndexList;
    }

    private static int getIndex(String[] elements, int i) {
        int index = 0;
        for (int j = 0; j < i; j++) {
            index += elements[j].length() + 1;
        }
        return index;
    }

    private static boolean isServiceWord(String word) {
        if (word.matches(CYRILLIC_WORD)) {
            return isRuWord(word);
        }
        if (word.matches(LATIN_WORD)) {
            return isEnWord(word);
        }
        return false;
    }

    private static boolean isRuWord(String word) {
        if (word.length() <= 3) {
            return false;
        }

        List<String> morphForm = Collections.synchronizedList(new ArrayList<>());
        morphForm.addAll(russianLuceneMorphology.getMorphInfo(word));

        return morphForm.parallelStream()
                .anyMatch(l -> l.contains("ПРЕДЛ")
                        || l.contains("СОЮЗ")
                        || l.contains("МЕЖД")
                        || l.contains("МС")
                        || l.contains("ЧАСТ"));
    }

    private static boolean isEnWord(String word) {
        if (word.length() <= 3) {
            return false;
        }

        List<String> morphForm = Collections.synchronizedList(new ArrayList<>());
        morphForm.addAll(englishLuceneMorphology.getMorphInfo(word));

        return morphForm.parallelStream().anyMatch(l ->
                (l.contains("PREP")
                        || l.contains("CONJ")
                        || l.contains("ADV")
                        || l.contains("NUM")
                        || l.contains("ART")));
    }
}

