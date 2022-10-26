package com.github.dmitriims.posikengine.service;

import com.github.dmitriims.posikengine.model.Field;
import com.github.dmitriims.posikengine.model.Lemma;
import com.github.dmitriims.posikengine.service.crawler.CrawlerContext;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class LemmaUtils {

    public static List<Lemma> getAndRankAllLemmas(Document doc, CrawlerContext context, MorphologyService morphologyService) throws IOException {
        List<Lemma> allLemmas = new ArrayList<>();
        for (Field f : context.getFields()) {
            Elements fieldElements = doc.select(f.getSelector());
            for(Element fieldElement : fieldElements) {
                String fieldText = fieldElement.text();
                for (Map.Entry<String, Integer> lemmaCount : getAndCountNormalFormsInString(fieldText, morphologyService).entrySet()) {
                    Lemma tempLemma = new Lemma();
                    tempLemma.setSite(context.getSite());
                    tempLemma.setLemma(lemmaCount.getKey());
                    tempLemma.setFrequency(lemmaCount.getValue());
                    tempLemma.setRank(lemmaCount.getValue() * f.getWeight());

                    int index = allLemmas.indexOf(tempLemma);
                    if (index < 0) {
                        allLemmas.add(tempLemma);
                        continue;
                    }
                    Lemma toUpdate = allLemmas.get(index);
                    toUpdate.setFrequency(toUpdate.getFrequency() + tempLemma.getFrequency());
                    toUpdate.setRank(
                            Math.ceil((toUpdate.getRank() + tempLemma.getRank()) * 10) / 10
                    );
                }
            }
            if(!f.getSelector().equals("title") && !f.getSelector().equals("body")) {
                fieldElements.remove();
            }
        }
        return allLemmas;
    }

    public static int calculateLemmasHash(List<Lemma> lemmas) {
        int hashcode = 0;
        for (Lemma l : lemmas) {
            hashcode += l.getLemma().hashCode() * l.getFrequency();
        }
        return hashcode;
    }

    public static Map<String, Integer> getAndCountNormalFormsInString(String input, MorphologyService morphologyService) {
        Map<String, Integer> dictionaryWithCount = new TreeMap<>();
        String[] words = morphologyService.splitStringToLowercaseWords(input);

        for (String word : words) {
            List<String> normalForms = morphologyService.getNormalFormOfAWord(word);
            if (normalForms.size() == 0) {
                continue;
            }
            normalForms.forEach(w -> dictionaryWithCount.put(w, dictionaryWithCount.getOrDefault(w, 0) + 1));
        }
        return dictionaryWithCount;
    }

}
