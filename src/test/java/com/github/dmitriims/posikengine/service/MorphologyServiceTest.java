package com.github.dmitriims.posikengine.service;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class MorphologyServiceTest {
    LuceneMorphology russianLuceneMorphology = new RussianLuceneMorphology();
    LuceneMorphology englishLuceneMorphology = new EnglishLuceneMorphology();
    String notAWord = "(?:\\.*\\s+\\-\\s+\\.*)|[^\\-а-яА-Яa-zA-Z\\d\\ё\\Ё]+";//TODO: посмотреть как можно подтягивать автоматом

    MorphologyService morphologyService = new MorphologyService(notAWord, russianLuceneMorphology, englishLuceneMorphology);

    public MorphologyServiceTest() throws IOException {
    }

    @Test
    @DisplayName("Тест splitStringToLowerCaseWords с простой строкой")
    public void testSplitStringToLowercaseWordsSimpleExample() {
        String input = "This is a simple input string, really SIMPLE one!";
        String[] correct = new String[]{"this", "is", "a", "simple", "input", "string", "really", "simple", "one"};
        String[] response = morphologyService.splitStringToLowercaseWords(input);
        assertArrayEquals(correct, response);
    }

    @Test
    @DisplayName("splitStringToLowerCaseWords тест с пустой строкой")
    public void testSplitStringToLowercaseWordsEmptyString() {
        String input = "";
        String[] correct = new String[]{};
        String[] response = morphologyService.splitStringToLowercaseWords(input);
        assertArrayEquals(correct, response);
    }

    @Test
    @DisplayName("splitStringToLowerCaseWords тест с пробельными символами")
    public void testSplitStringToLowercaseWordsWhiteSpaceString() {
        String input = " \n\t  ";
        String[] correct = new String[]{};
        String[] response = morphologyService.splitStringToLowercaseWords(input);
        assertArrayEquals(correct, response);
    }

    @Test
    @DisplayName("splitStringToLowerCaseWords тест с числами")
    public void testSplitStringToLowercaseWordsWDigitsAllowed() {
        String input = "31 июня";
        String[] correct = new String[]{"31", "июня"};
        String[] response = morphologyService.splitStringToLowercaseWords(input);
        assertArrayEquals(correct, response);
    }

    @Test
    @DisplayName("splitStringToLowerCaseWords тест с мусором")
    public void testSplitStringToLowercaseOnlyGarbage() {
        String input = "*/.!@#$%^&*()+\"'`[]{}<|>\\";
        String[] correct = new String[]{};
        String[] response = morphologyService.splitStringToLowercaseWords(input);
        assertArrayEquals(correct, response);
    }

    @Test
    @DisplayName("splitStringToLowerCaseWords тест со словом через дефис")
    public void testSplitStringToLowercaseHyphenatedAllowed() {
        String input = "Из-за острого изжога!";
        String[] correct = new String[]{"из-за", "острого", "изжога"};
        String[] response = morphologyService.splitStringToLowercaseWords(input);
        assertArrayEquals(correct, response);
    }

    @Test
    @DisplayName("splitStringToLowerCaseWords тест со сложным предложением")
    public void testSplitStringToLowercaseComplex() {
        String input = "21/04/1998 в 11:00 произошло интереснейшее событие - вазбегульт, более известное как <redacted>.";
        String[] correct = new String[]{"21", "04", "1998", "в", "11", "00", "произошло", "интереснейшее", "событие",
                 "вазбегульт", "более", "известное", "как", "redacted"};
        String[] response = morphologyService.splitStringToLowercaseWords(input);
        assertArrayEquals(correct, response);
    }

    @Test
    @DisplayName("isRussianGarbage - союз")
    public void testIsRussianGarbageConj() {
        assertTrue(morphologyService.isRussianGarbage(russianLuceneMorphology.getMorphInfo("или")));
    }

    @Test
    @DisplayName("isRussianGarbage - междометие")
    public void testIsRussianGarbageInterjection() {
        assertTrue(morphologyService.isRussianGarbage(russianLuceneMorphology.getMorphInfo("тьфу")));
    }

    @Test
    @DisplayName("isRussianGarbage - предлог")
    public void testIsRussianGarbagePrep() {
        assertTrue(morphologyService.isRussianGarbage(russianLuceneMorphology.getMorphInfo("на")));
    }

    @Test
    @DisplayName("isRussianGarbage - частица")
    public void testIsRussianGarbageParticle() {
        assertTrue(morphologyService.isRussianGarbage(russianLuceneMorphology.getMorphInfo("не")));
    }

    @Test
    @DisplayName("isRussianGarbage - просто слово")
    public void testIsRussianGarbageWord() {
        assertFalse(morphologyService.isRussianGarbage(russianLuceneMorphology.getMorphInfo("слово")));
    }

    @Test
    @DisplayName("isEnglishGarbage - союз")
    public void testIsEnglishGarbageConj() {
        assertTrue(morphologyService.isEnglishGarbage(englishLuceneMorphology.getMorphInfo("and")));
    }

    @Test
    @DisplayName("isEnglishGarbage - междометие")
    public void testIsEnglishGarbageInterjection() {
        assertTrue(morphologyService.isEnglishGarbage(englishLuceneMorphology.getMorphInfo("drat")));
    }

    @Test
    @DisplayName("isEnglishGarbage - предлог")
    public void testIsEnglishGarbagePrep() {
        assertTrue(morphologyService.isEnglishGarbage(englishLuceneMorphology.getMorphInfo("with")));
    }

    @Test
    @DisplayName("isEnglishGarbage - частица")
    public void testIsEnglishGarbageParticle() {
        assertTrue(morphologyService.isEnglishGarbage(englishLuceneMorphology.getMorphInfo("to")));
    }

    @Test
    @DisplayName("isEnglishGarbage - артикль")
    public void testIsEnglishGarbageArticle() {
        assertTrue(morphologyService.isEnglishGarbage(englishLuceneMorphology.getMorphInfo("the")));
    }

    @Test
    @DisplayName("isEnglishGarbage - просто слово")
    public void testIsEnglishGarbageWord() {
        assertFalse(morphologyService.isEnglishGarbage(englishLuceneMorphology.getMorphInfo("word")));
    }

    @Test
    @DisplayName("getNormalFormOfAWord - русский")
    public void testGetNormalFormOfAWordRussian() {
        List<String> correct = new ArrayList<>() {{
            add("тест");
            add("тесто");
        }};
        List<String> response = morphologyService.getNormalFormOfAWord("тест");
        assertIterableEquals(correct, response);
    }

    @Test
    @DisplayName("getNormalFormOfAWord - английский")
    public void testGetNormalFormOfAWordEnglish() {
        List<String> correct = new ArrayList<>() {{
            add("test");
        }};
        List<String> response = morphologyService.getNormalFormOfAWord("test");
        assertIterableEquals(correct, response);
    }

    @Test
    @DisplayName("getNormalFormOfAWord - число")
    public void testGetNormalFormOfAWordDigit() {
        List<String> correct = new ArrayList<>() {{
            add("42");
        }};
        List<String> response = morphologyService.getNormalFormOfAWord("42");
        assertIterableEquals(correct, response);
    }

    @Test
    @DisplayName("getNormalFormOfAWord - мусор")
    public void testGetNormalFormOfAWordGarbage() {
        List<String> correct = new ArrayList<>();
        List<String> response = morphologyService.getNormalFormOfAWord("или");
        assertIterableEquals(correct, response);
    }

    @Test
    @DisplayName("getNormalFormOfAWord - другой язык")
    public void testGetNormalFormOfAWordAnotherLanguage() {
        List<String> correct = new ArrayList<>();
        List<String> response = morphologyService.getNormalFormOfAWord("猫");
        assertIterableEquals(correct, response);
    }

    @Test
    @DisplayName("getAndCountLemmasInString - подсчёт лемм")
    public void testGetAndCountLemmasInStringNormal() {
        Map<String, Integer> correct  = new TreeMap<>(){{ //TODO: проверить в последствии замену на обычную хэшмапу
            put("42", 1);
            put("be", 1);
            put("first", 3);
            put("parse", 2);
            put("we", 1);
            put("which", 1);
            put("word", 1);
        }};
        Map<String, Integer> response = morphologyService.
                getAndCountLemmasInString("First we parsed the first word which was the first to parse. Но не 42!");
        assertIterableEquals(correct.entrySet(), response.entrySet());
    }
    @Test
    @DisplayName("getAndCountLemmasInString - если мусор, то ничего не вернёт")
    public void testGetAndCountLemmasInStringGarbage() {
        Map<String, Integer> correct  = new TreeMap<>();
        Map<String, Integer> response = morphologyService.getAndCountLemmasInString("猫 или 鯨? Тьфу drat!");
        assertIterableEquals(correct.entrySet(), response.entrySet());
    }
}
