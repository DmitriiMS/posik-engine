package com.github.dmitriims.posikengine;

import com.google.search.robotstxt.Parser;
import com.google.search.robotstxt.RobotsMatcher;
import com.google.search.robotstxt.RobotsParseHandler;
import com.google.search.robotstxt.RobotsParser;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Slf4j
public class Main {
    static String site = "https://dombulgakova.ru/";

    public static void main(String[] args) {
        try {
            //TODO: переделать, такая примитивная проверка параметра чисто для отладки.
            if (args.length == 1 && args[0].equals("index")) {
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
                log.info(site + " indexed");
            }


            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.println("Введите поисковый запрос:");
                String searchTerm = reader.readLine();
                if(searchTerm.equals("000")) {
                    break;
                }
                Search search = new Search(searchTerm);
                search.performSearch();
                List<PageDTO> foundPages = search.getSearchResults();
                foundPages.forEach(System.out::println);
            }

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
