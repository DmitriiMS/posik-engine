package com.github.dmitriims.posikengine.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
@NoArgsConstructor
public class MorphologyService {

    private String NOT_A_WORD_PATTERN;
    private LuceneMorphology russianLuceneMorph;
    private LuceneMorphology englishLuceneMorph;

    @Autowired
    public MorphologyService(
            @Value("(?:\\.*\\s+\\-\\s+\\.*)|[^\\-а-яА-Яa-zA-Z\\d\\ё\\Ё]+")
            String NOT_A_WORD_PATTERN,

            @Qualifier("russianMorphology")
            LuceneMorphology russianLuceneMorph,

            @Qualifier("englishMorphology")
            LuceneMorphology englishLuceneMorph)
    {
        this.NOT_A_WORD_PATTERN = NOT_A_WORD_PATTERN;
        this.russianLuceneMorph = russianLuceneMorph;
        this.englishLuceneMorph = englishLuceneMorph;
    }

    public Map<String, Integer> getAndCountLemmasInString(String input) {
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

    public List<String> getNormalFormOfAWord(String word) {
        if (russianLuceneMorph.checkString(word) && !isRussianGarbage(russianLuceneMorph.getMorphInfo(word))) {
            return russianLuceneMorph.getNormalForms(word);
        } else if (englishLuceneMorph.checkString(word) && !isEnglishGarbage(englishLuceneMorph.getMorphInfo(word))) {
            return englishLuceneMorph.getNormalForms(word);
        } else if (word.chars().allMatch(Character::isDigit)){
            return Collections.singletonList(word);
        }
        return new ArrayList<>();
    }

    public String readFileToString(String filePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(filePath)));
    }

    public String[] splitStringToLowercaseWords(String input) {
        return Arrays.stream(input.toLowerCase(Locale.ROOT)
                        .replaceAll(NOT_A_WORD_PATTERN, " ")
                        .trim()
                        .split(" "))
                .filter(s -> !s.isBlank()).toArray(String[]::new);
    }


    public boolean isRussianGarbage(List<String> morphInfos) {
        for(String variant : morphInfos) {
            if (variant.contains(" СОЮЗ") || variant.contains(" МЕЖД") ||
                    variant.contains(" ПРЕДЛ") || variant.contains(" ЧАСТ")) {
                return true;
            }
        }
        return false;
    }

    public boolean isEnglishGarbage (List<String> morphInfos) {
        for(String variant : morphInfos) {
            if (variant.contains(" CONJ") || variant.contains(" INT") ||
                    variant.contains(" PREP") || variant.contains(" PART") ||  variant.contains(" ARTICLE")) {
                return true;
            }
        }
        return false;
    }
}