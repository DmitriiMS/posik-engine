import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
@Slf4j
public class MorphologyUtils {

    //первая часть регулярок -- для слов написанных через дефис.
    private static final Pattern russianWords = Pattern.compile("(?:\\.*\\s+\\-\\s+\\.*)|[\\-а-яА-Я\\ё\\Ё]+");
    private static final Pattern englishWords = Pattern.compile("(?:\\.*\\s+\\-\\s+\\.*)|[\\-a-zA-Z]+");
    private static final Pattern numbers = Pattern.compile("(?:\\.*\\s+\\-\\s+\\.*)|[\\-\\d]+");

    public static Map<String, Integer> lemmatiseString(String input) throws IOException {
        Map<String, Integer> dictionaryWithCount = new TreeMap<>();

        LuceneMorphology russianLuceneMorph = new RussianLuceneMorphology();
        LuceneMorphology englishLuceneMorph = new EnglishLuceneMorphology();

        String[] words = splitStringToLowercaseWords(input);

        for (String word : words) {
            if (russianWords.matcher(word).matches() && !isRussianGarbage(russianLuceneMorph.getMorphInfo(word))) {
                russianLuceneMorph.getNormalForms(word)
                        .forEach(w -> dictionaryWithCount.put(w, dictionaryWithCount.getOrDefault(w, 0) + 1));
                log.debug(word + " + " +russianLuceneMorph.getNormalForms(word));
            } else if (englishWords.matcher(word).matches() && !isEnglishGarbage(englishLuceneMorph.getMorphInfo(word))) {
                englishLuceneMorph.getNormalForms(word)
                        .forEach(w -> dictionaryWithCount.put(w, dictionaryWithCount.getOrDefault(w, 0) + 1));
                log.debug(word + " + " + englishLuceneMorph.getNormalForms(word));
            } else if (numbers.matcher(word).matches()) {
                dictionaryWithCount.put(word, dictionaryWithCount.getOrDefault(word, 0) + 1);
            }
        }

        return dictionaryWithCount;
    }

    public static String readFileToString(String filePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(filePath)));
    }

    public static String[] splitStringToLowercaseWords(String input) {
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("(?:\\.*\\s+\\-\\s+\\.*)|[^\\-а-яА-Яa-zA-Z\\d_\\ё\\Ё]+", " ")
                .trim()
                .split(" ");
    }


    public static boolean isRussianGarbage(List<String> morphInfos) {
        for(String variant : morphInfos) {
            if (variant.contains(" СОЮЗ") || variant.contains(" МЕЖД") ||
                    variant.contains(" ПРЕДЛ") || variant.contains(" ЧАСТ")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEnglishGarbage (List<String> morphInfos) {
        for(String variant : morphInfos) {
            if (variant.contains(" CONJ") || variant.contains(" INT") ||
                    variant.contains(" PREP") || variant.contains(" PART") ||  variant.contains(" ARTICLE")) {
                return true;
            }
        }
        return false;
    }
}
