import com.google.search.robotstxt.Parser;
import com.google.search.robotstxt.RobotsMatcher;
import com.google.search.robotstxt.RobotsParseHandler;
import com.google.search.robotstxt.RobotsParser;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.concurrent.ForkJoinPool;

@Slf4j
public class Main {
    static String site = "https://www.lutherancathedral.ru/";

    public static void main(String[] args) {
        try {
            String[] splitSite = site.split("//|/");
            String topLevelSite = splitSite[0] + "//" + splitSite[1] + "/";
            Parser robotParser = new RobotsParser(new RobotsParseHandler());
            RobotsMatcher robotsMatcher = (RobotsMatcher) robotParser.parse(getRobotsTxt(topLevelSite));

            DBConnection.dropTables();
            DBConnection.createTables();

            ForkJoinPool pool = new ForkJoinPool();
            Crawler crawler = new Crawler(site, robotsMatcher, Integer.MAX_VALUE, DBConnection.getFields());
            pool.execute(crawler);
            crawler.join();
            DBConnection.flushBufferToDB();

        } catch (IOException | SQLException e) {
            e.printStackTrace();
            log.error(e.toString());
        }

    }

    public static byte[] getRobotsTxt(String site) throws IOException {
        byte[] robotsInBytes;
        URL robots = new URL(site + "robots.txt");

        HttpURLConnection urlConnection = (HttpURLConnection) robots.openConnection();
        urlConnection.setRequestMethod("HEAD");
        if(urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return "User-agent: *\nAllow:".getBytes(StandardCharsets.UTF_8);
        }

        try (InputStream inStream = robots.openStream();
             ByteArrayOutputStream outStream = new ByteArrayOutputStream())
        {
            byte[] buffer = new byte[4096];
            int n = 0;
            while ((n = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, n);
            }
            robotsInBytes = outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return robotsInBytes;
    }
}
