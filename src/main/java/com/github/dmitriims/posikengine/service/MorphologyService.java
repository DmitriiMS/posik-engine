package com.github.dmitriims.posikengine.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class MorphologyService {

    private String NOT_A_WORD_PATTERN;
    private LuceneMorphology russianLuceneMorph;
    private LuceneMorphology englishLuceneMorph;

    public MorphologyService(
            @Qualifier("notAWord")
            String NOT_A_WORD_PATTERN,
            @Qualifier("russianMorphology")
            LuceneMorphology russianLuceneMorph,
            @Qualifier("englishMorphology")
            LuceneMorphology englishLuceneMorph) {
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
        word = word.replaceAll("ё", "е");
        if (russianLuceneMorph.checkString(word) && !isRussianGarbage(russianLuceneMorph.getMorphInfo(word))) {
            return russianLuceneMorph.getNormalForms(word);
        } else if (englishLuceneMorph.checkString(word) && !isEnglishGarbage(englishLuceneMorph.getMorphInfo(word))) {
            return englishLuceneMorph.getNormalForms(word);
        } else if (word.chars().allMatch(Character::isDigit)){
            return Collections.singletonList(word);
        }
        return new ArrayList<>();
    }

    public String[] splitStringToLowercaseWords(String input) {
        return Arrays.stream(input.toLowerCase(Locale.ROOT)
                        .replaceAll(NOT_A_WORD_PATTERN, " ")
                        .trim()
                        .split(" "))
                .filter(s -> !s.isBlank()).toArray(String[]::new);
    }

    public String[] splitStringToWords(String sentence) {
        return Arrays.stream(sentence.replaceAll(NOT_A_WORD_PATTERN, " ")
                        .trim()
                        .split(" "))
                .filter(s -> !s.isBlank()).toArray(String[]::new);
    }


    boolean isRussianGarbage(List<String> morphInfos) {
        for(String variant : morphInfos) {
            if (variant.contains(" СОЮЗ") || variant.contains(" МЕЖД") ||
                    variant.contains(" ПРЕДЛ") || variant.contains(" ЧАСТ")) {
                return true;
            }
        }
        return false;
    }

    boolean isEnglishGarbage (List<String> morphInfos) {
        for(String variant : morphInfos) {
            if (variant.contains(" CONJ") || variant.contains(" INT") ||
                    variant.contains(" PREP") || variant.contains(" PART") ||  variant.contains(" ARTICLE")) {
                return true;
            }
        }
        return false;
    }
}