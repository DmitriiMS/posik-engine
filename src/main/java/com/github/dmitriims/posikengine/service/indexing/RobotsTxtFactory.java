package com.github.dmitriims.posikengine.service.indexing;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Component
public class RobotsTxtFactory {
    public byte[] getRobotsTxt(String site) throws IOException {
        byte[] robotsInBytes;
        URL robots = new URL(site + "/robots.txt");

        HttpURLConnection urlConnection = (HttpURLConnection) robots.openConnection();
        urlConnection.setRequestMethod("HEAD");
        if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return "User-agent: *\nAllow:".getBytes(StandardCharsets.UTF_8);
        }

        try (InputStream inStream = robots.openStream();
             ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
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
