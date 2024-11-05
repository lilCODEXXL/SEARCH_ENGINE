package searchengine.constans;

public class Constants {
    public static final String CYRILLIC_WORD = "[а-яА-Я]*";
    public static final String LATIN_WORD = "[a-zA-Z]*";

    public static final String REGEX = "\\p{Punct}|[0-9]|№|©|◄|«|»|—|-|@|…";

    public static final String COMBINED_REGEX = "(?<![а-яА-Я])[а-яА-Я]{2,}(?![а-яА-Я])|(?<![a-zA-Z])[a-zA-Z]{2,}(?![a-zA-Z])";

}
