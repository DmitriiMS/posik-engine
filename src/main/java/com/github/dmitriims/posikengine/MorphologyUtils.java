package com.github.dmitriims.posikengine;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
public class MorphologyUtils {

    private static final String NOT_A_WORD_PATTERN = "(?:\\.*\\s+\\-\\s+\\.*)|[^\\-а-яА-Яa-zA-Z\\d\\ё\\Ё]+";
    private static final LuceneMorphology russianLuceneMorph;
    private static final LuceneMorphology englishLuceneMorph;

    static {
        try {
            russianLuceneMorph = new RussianLuceneMorphology();
            englishLuceneMorph = new EnglishLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    public static Map<String, Integer> getAndCountLemmasInString(String input) {
        Map<String, Integer> dictionaryWithCount = new TreeMap<>();
        String[] words = splitStringToLowercaseWords(input);

        for (String word : words) {
            List<String> normalForms = getNormalFormOfAWord(word);
            if (normalForms.size() == 0) {
                continue;
            }
            normalForms.forEach(w -> dictionaryWithCount.put(w, dictionaryWithCount.getOrDefault(w, 0) + 1));
        }
        return dictionaryWithCount;
    }

    public static List<String> getNormalFormOfAWord(String word) {
        if (russianLuceneMorph.checkString(word) && !isRussianGarbage(russianLuceneMorph.getMorphInfo(word))) {
            return russianLuceneMorph.getNormalForms(word);
        } else if (englishLuceneMorph.checkString(word) && !isEnglishGarbage(englishLuceneMorph.getMorphInfo(word))) {
            return englishLuceneMorph.getNormalForms(word);
        } else if (word.chars().allMatch(Character::isDigit)){
            return Collections.singletonList(word);
        }
        return new ArrayList<>();
    }

    public static String readFileToString(String filePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(filePath)));
    }

    public static String[] splitStringToLowercaseWords(String input) {
        return Arrays.stream(input.toLowerCase(Locale.ROOT)
                .replaceAll(NOT_A_WORD_PATTERN, " ")
                .trim()
                .split(" "))
                .filter(s -> !s.isBlank()).toArray(String[]::new);
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
