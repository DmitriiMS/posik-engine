import com.google.search.robotstxt.RobotsMatcher;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Crawler extends RecursiveAction {

    private static String site;
    private static final Set<String> visitedPages;
    private static RobotsMatcher robotsMatcher;
    private static final AtomicInteger numberOfPagesToCrawl;
    private static final List<String> FORBIDDEN_COMPONENTS;
    private static Random delayGenerator;
    private static Set<Field> fields;

    static {
        visitedPages = ConcurrentHashMap.newKeySet();
        numberOfPagesToCrawl = new AtomicInteger();
        FORBIDDEN_COMPONENTS = new ArrayList<>() {{
            add("#");
            add("mailto:");
            add("tel:");
            add("javascript:");
        }};
        Crawler.delayGenerator = new Random(System.currentTimeMillis());
    }

    private final String link;

    public Crawler(String link) {
        this.link = link;
        Crawler.visitedPages.add(link);
    }

    public Crawler(String site, RobotsMatcher robotsMatcher, int limit, Set<Field> fields) {
        Crawler.site = site;
        Crawler.robotsMatcher = robotsMatcher;
        Crawler.numberOfPagesToCrawl.set(limit);
        Crawler.fields = fields;

        this.link = site;
    }

    @Override
    protected void compute() {
        try {
            if (numberOfPagesToCrawl.get() <= 0) {
                return;
            }
            Thread.sleep(delayGenerator.ints(500, 5000).findFirst().getAsInt());

            Connection.Response response = Jsoup.connect(link)
                    .userAgent("PosikEngineSearchBot")
                    .referrer("http://www.google.com")
                    .timeout(60 * 1000)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute();
            String type = response.contentType();
            if (!type.startsWith("text")) {
                return;
            }
            int code = response.statusCode();
            String content = response.body();


            Document document = response.parse();
            List<Lemma> allLemmas = getAndRankAllLemmas(document);

            Page currentPage = new Page(link.replaceFirst(site, "/"), code, content, allLemmas);
            synchronized (DBConnection.class) {
                DBConnection.addPageToBuffer(numberOfPagesToCrawl, currentPage);
            }



            Set<String> filteredLinks = filterLinks(
                    document.select("a[href]").stream().map(e -> e.attr("abs:href")).toList()
            );
            if (filteredLinks.size() != 0) {
                invokeAll(filteredLinks.stream().map(Crawler::new).toList());
            }

        } catch (InterruptedException | IOException e) {
            log.error(e.toString());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Lemma> getAndRankAllLemmas(Document doc) throws IOException {
        List<Lemma> allLemmas = new ArrayList<>();
        for (Field f : fields) {
            String fieldText = doc.select(f.getSelector()).text();
            for (Map.Entry<String, Integer> lemmaCount : MorphologyUtils.lemmatiseString(fieldText).entrySet()){
                Lemma tempLemma = new Lemma(lemmaCount.getKey(), lemmaCount.getValue(), lemmaCount.getValue() * f.getWeight());
                int index = allLemmas.indexOf(tempLemma);
                if (index < 0) {
                    allLemmas.add(tempLemma);
                    continue;
                }
                Lemma toUpdate = allLemmas.get(index);
                toUpdate.setCount(toUpdate.getCount() + tempLemma.getCount());
                toUpdate.setRank(toUpdate.getRank() + tempLemma.getRank());
            }
        }
        return allLemmas;
    }

    public Set<String> filterLinks(List<String> links) {
        Set<String> filtered = new HashSet<>();
        for (String l : links) {
            if (visitedPages.contains(l) || !l.startsWith(site) || containsForbiddenComponents(l) ||
                    !robotsMatcher.singleAgentAllowedByRobots("PosikEngineSearchBot", l)) {
                visitedPages.add(l);
                continue;
            }
            filtered.add(l);
        }
        return filtered;
    }

    private boolean containsForbiddenComponents(String link) {
        for (String component : FORBIDDEN_COMPONENTS) {
            if (link.contains(component)) {
                return true;
            }
        }
        return false;
    }
}
