package com.github.dmitriims.posikengine;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
@Slf4j
public class Search {
    private static final double THRESHOLD = 0.9;
    private static final Pattern END_OF_SENTENCE = Pattern.compile("[\\.!?]\\s*");

    private String originalSearchString;
    private String[] searchWords;
    private String[] searchWordsNormalForms;

    private final List<PageDTO> searchResults = new ArrayList<>();

    public Search(String searchString) throws IOException, IllegalArgumentException {
        originalSearchString = searchString;
        searchWords = originalSearchString.split("\\s");
        searchWordsNormalForms = MorphologyUtils.getAndCountLemmasInString(originalSearchString).keySet().toArray(new String[0]);
        if (originalSearchString.isEmpty() || searchWordsNormalForms.length == 0) {
            throw new IllegalArgumentException("empty search string");
        }
    }

    public void performSearch() throws SQLException {
        findPages(getLemmasIds());
    }

    public Set<Lemma> getLemmasIds() throws SQLException {
        Set<Lemma> result = new TreeSet<>(Comparator.comparing(Lemma::getCount));

        PreparedStatement selectStatement = DBConnection.getConnection().prepareStatement(
                "SELECT l.id, l.lemma, l.frequency FROM lemma l " +
                        "INNER JOIN `index` i " +
                        "ON l.id = i.lemma_id " +
                        "WHERE l.lemma IN (" + Arrays.stream(searchWordsNormalForms).collect(Collectors.joining("','","'","'")) + ") " +
                        "GROUP BY l.id " +
                        "HAVING COUNT(i.page_id) < (SELECT COUNT(*) FROM page) * " + THRESHOLD + ";"
        );

        ResultSet resultSet = selectStatement.executeQuery();
        while (resultSet.next()) {
            result.add(new Lemma(resultSet.getInt("id"), resultSet.getString("lemma"),
                    resultSet.getInt("frequency"), 0));
        }
        selectStatement.close();
        return result;
    }

    public void findPages(Set<Lemma> lemmas) throws SQLException {
        List<Integer> pagesIds = new ArrayList<>();

        if (lemmas.size() == 0) {
            return;
        }
        List<Integer> lemmasIds = lemmas.stream().map(Lemma::getId).toList();
        PreparedStatement selectStatement = DBConnection.getConnection().prepareStatement(
                "SELECT i.page_id  FROM `index` i \n" +
                        "WHERE i.lemma_id = " + lemmasIds.get(0) + ";"
        );
        ResultSet resultSet = selectStatement.executeQuery();
        while(resultSet.next()) {
            pagesIds.add(resultSet.getInt("page_id"));
        }
        selectStatement.close();

        for(int i = 1; i < lemmasIds.size(); i++) {
            selectStatement = DBConnection.getConnection().prepareStatement(
                    "SELECT i.page_id FROM `index` i WHERE i.lemma_id = " + lemmasIds.get(i) + " " +
                            "AND i.page_id IN (" + pagesIds.stream().map(pid -> Integer.toString(pid)).collect(Collectors.joining(",")) + ");"
            );
            resultSet = selectStatement.executeQuery();
            pagesIds = new ArrayList<>();
            while(resultSet.next()) {
                pagesIds.add(resultSet.getInt("page_id"));
            }
            selectStatement.close();
            if (pagesIds.size() == 0) {
                return;
            }
        }

        selectStatement = DBConnection.getConnection().prepareStatement(
                "SELECT p.id, p.`path`, p.content, SUM(i.`rank`) AS total_rank  FROM page p " +
                        "INNER JOIN `index` i " +
                        "ON p.id  = i.page_id " +
                        "WHERE i.page_id IN (" + pagesIds.stream().map(pid -> Integer.toString(pid)).collect(Collectors.joining(",")) + ") " +
                        "AND i.lemma_id IN (" + lemmasIds.stream().map(lid -> Integer.toString(lid)).collect(Collectors.joining(",")) + ") " +
                        "GROUP BY p.id;"
        );
        resultSet = selectStatement.executeQuery();

        double maxAbsoluteRank = 0;
        while(resultSet.next()) {
            PageDTO p = getPageFromDBResponse(resultSet);
            maxAbsoluteRank = Math.max(maxAbsoluteRank, p.getRelevance());
            searchResults.add(p);
        }
        for (PageDTO p : searchResults) {
            p.setRelevance(p.getRelevance()/maxAbsoluteRank);
        }
        searchResults.sort(Comparator.comparing(PageDTO::getRelevance).reversed());
    }

    public PageDTO getPageFromDBResponse(ResultSet rs) throws SQLException {
        PageDTO pageDTO = new PageDTO();
        Document contents = Jsoup.parse(rs.getString("content"));

        pageDTO.setUri(rs.getString("path"));
        pageDTO.setTitle(contents.select("title").text());
        pageDTO.setSnippet(getSnippetFromPage(contents.select("body").text()));
        pageDTO.setRelevance(rs.getDouble("total_rank"));

        return pageDTO;
    }

    //TODO: посмотреть как можно упростить.
    public String getSnippetFromPage(String text) {
        List<String> sentences = new ArrayList<>();
        Map<String, Integer> wordsWithPositions = getIndexMap(text);
        List<String> words = new ArrayList<>(wordsWithPositions.keySet());
        for (Map.Entry<String, Integer> p: wordsWithPositions.entrySet()) {
            if (!words.contains(p.getKey())) {
                continue;
            }
            String sentence = getSentenceWithWordFromText(text, p.getValue(), p.getKey().length());
            ListIterator<String> iterator = words.listIterator();
            while (iterator.hasNext()) {
                String word = iterator.next();
                int index = findIndexOfWord(sentence, word);
                if (index >= 0 && !word.equals(p.getKey())) {
                    sentence = getSentenceWithWordFromText(sentence, index, word.length());
                    iterator.remove();
                }
            }
            sentences.add(sentence);
        }
        return String.join(" <...> ", sentences);
    }

    public Map<String, Integer> getIndexMap(String text) {
        Map<String, Integer> result = new TreeMap<>();
        Set<String> normalizedSearchTerms = new HashSet<>(List.of(searchWordsNormalForms));
        String[] words = MorphologyUtils.splitStringToLowercaseWords(text);
        for (String word : words) {
            if(word.isBlank()) {
                continue;
            }
            List<String> normWords = MorphologyUtils.getNormalFormOfAWord(word);
            for (String normWord : normWords) {
                if (normalizedSearchTerms.contains(normWord)) {
                    int index = findIndexOfWord(text, word);
                    if (index < 0) { break; }
                    result.putIfAbsent(word, index);
                    normalizedSearchTerms.remove(normWord);
                    break;
                }
            }
        }
        return result;
    }

    public int findIndexOfWord(String text, String word) {
        Pattern wordPattern = Pattern.compile("\\b" + word + "\\b");
        Matcher wordMatcher = wordPattern.matcher(text.toLowerCase(Locale.ROOT));
        if (!wordMatcher.find()) {
            return -1;
        }
        return wordMatcher.start();
    }

    public String getSentenceWithWordFromText(String text, int wordStartIndex, int wordLength) {
        StringBuilder builder = new StringBuilder();

        String textChunk = text.substring(0, wordStartIndex);
        Matcher matcher = END_OF_SENTENCE.matcher(textChunk);
        int tempIndex = 0;
        while(matcher.find()){
            tempIndex = matcher.end();
        }
        builder.append(text, tempIndex, wordStartIndex);
        textChunk = text.substring(wordStartIndex+wordLength);
        matcher = END_OF_SENTENCE.matcher(textChunk);
        matcher.find();
        tempIndex = matcher.start() + wordStartIndex + wordLength + 1;
        builder.append("<b>").append(text, wordStartIndex, wordStartIndex+wordLength).append("</b>");
        builder.append(text, wordStartIndex+wordLength, tempIndex);
        return builder.toString();
    }

}
