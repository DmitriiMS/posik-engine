package com.github.dmitriims.posikengine;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DBConnection {
    private static volatile Connection connection;

    private static final int BUFFER_SIZE = 100;
    private static int numberOfIndexedPages = 0;

    private static final Set<Page> pagesBuffer = ConcurrentHashMap.newKeySet(BUFFER_SIZE);
    private static PreparedStatement insertPageStatement;
    private static PreparedStatement insertLemmaStatement;
    private static PreparedStatement insertIndexStatement;


    private static String dbName = "search_engine";
    private static String dbUser = "dms";
    private static String dbPass = "password";


    public static Connection getConnection() throws SQLException {
        if (connection == null) {
            synchronized (DBConnection.class) {
                if (connection == null) {
                    connection = DriverManager.getConnection(
                            "jdbc:mysql://localhost:3306/" + dbName +
                                    "?user=" + dbUser + "&password=" + dbPass + "&useSSL=false");
                }
            }
        }
        return connection;
    }

    public static void dropTables() throws SQLException {
        dropIndexTable();
        dropPageTable();
        dropFieldTable();
        dropLemmaTable();
        dropSiteTable();
    }

    public static void createTables() throws SQLException {
        createSiteTable();
        createPageTable();
        createFieldTable();
        createLemmaTable();
        createIndexTable();
    }

    public static void dropPageTable() throws SQLException {
        getConnection().createStatement().execute("DROP TABLE IF EXISTS page");
    }

    public static void createPageTable() throws SQLException {
        getConnection().createStatement().execute("CREATE TABLE page(" +
                "id INT NOT NULL AUTO_INCREMENT, " +
                "site_id INT NOT NULL DEFAULT 1, " +
                "`path` TEXT NOT NULL, " +
                "code INT NOT NULL, " +
                "content MEDIUMTEXT NOT NULL, " +
                "PRIMARY KEY(id), " +
                "FOREIGN KEY(site_id) REFERENCES site(id), " +
                "UNIQUE KEY (`path`(100)));");
    }

    public static void dropFieldTable() throws SQLException {
        getConnection().createStatement().execute("DROP TABLE IF EXISTS field");
    }

    public static void createFieldTable() throws SQLException {
        getConnection().createStatement().execute("CREATE TABLE field(" +
                "id INT NOT NULL AUTO_INCREMENT, " +
                "`name` VARCHAR(255) NOT NULL, " +
                "selector VARCHAR(255) NOT NULL, " +
                "weight FLOAT NOT NULL, " +
                "PRIMARY KEY(id), " +
                "UNIQUE KEY (`name`(255)));");

        getConnection().createStatement().execute("INSERT INTO field (name, selector, weight) " +
                "VALUES ('title', 'title', 1.0), ('body', 'body', 0.8)");
    }

    public static void dropLemmaTable() throws SQLException {
        getConnection().createStatement().execute("DROP TABLE IF EXISTS lemma");
    }

    public static void createLemmaTable() throws SQLException {
        getConnection().createStatement().execute("CREATE TABLE lemma(" +
                "id INT NOT NULL AUTO_INCREMENT, " +
                "site_id INT NOT NULL DEFAULT 1, " +
                "lemma VARCHAR(255) NOT NULL, " +
                "frequency INT NOT NULL, " +
                "PRIMARY KEY(id), " +
                "FOREIGN KEY(site_id) REFERENCES site(id), " +
                "UNIQUE KEY (lemma(255)));");
    }

    public static void dropIndexTable() throws SQLException {
        getConnection().createStatement().execute("DROP TABLE IF EXISTS `index`");
    }

    public static void createIndexTable() throws SQLException {
        getConnection().createStatement().execute("CREATE TABLE `index`(" +
                "id INT NOT NULL AUTO_INCREMENT, " +
                "page_id INT NOT NULL, " +
                "lemma_id INT NOT NULL, " +
                "`rank` FLOAT NOT NULL, " +
                "PRIMARY KEY(id), " +
                "FOREIGN KEY(page_id) REFERENCES page(id)," +
                "FOREIGN KEY(lemma_id) REFERENCES lemma(id), " +
                "UNIQUE KEY page_lemma(page_id, lemma_id));");
    }

    public static void createSiteTable() throws SQLException {
        getConnection().createStatement().execute("create table site(" +
                "id INT NOT NULL AUTO_INCREMENT," +
                "status ENUM('INDEXING', 'INDEXED', 'FAILED') NOT NULL," +
                "status_time DATETIME NOT NULL," +
                "last_error TEXT," +
                "url VARCHAR(255) NOT NULL," +
                "`name` VARCHAR(255) NOT NULL," +
                "PRIMARY KEY (id)," +
                "UNIQUE KEY (`name`(255)));");

        getConnection().createStatement().execute("INSERT INTO site (status, status_time, last_error, url, `name`) " +
                "VALUES ('INDEXED', CURRENT_TIMESTAMP, '', 'https://www.lutherancathedral.ru/', 'Собор Петра и Павла')");
    }

    public static void dropSiteTable() throws SQLException {
        getConnection().createStatement().execute("DROP TABLE IF EXISTS site");
    }
    public static void addPageToBuffer(AtomicInteger pageCounter, Page page) throws SQLException {
        if (pageCounter.get() <= 0) {return;}
        int sizeBefore = pagesBuffer.size();
        pagesBuffer.add(page);
        pageCounter.addAndGet(sizeBefore - pagesBuffer.size());
        if (pagesBuffer.size() >= BUFFER_SIZE || pageCounter.get() <= 0) {
            flushBufferToDB();
        }
    }

    public static void flushBufferToDB() throws SQLException {
        if (pagesBuffer.size() == 0) {
            return;
        }
        if (insertPageStatement == null) {
            insertPageStatement = getConnection().prepareStatement(
                    "INSERT INTO page (path, code, content) VALUES (?,?,?) ON DUPLICATE KEY UPDATE id=id;",
                    Statement.RETURN_GENERATED_KEYS);
            insertLemmaStatement = getConnection().prepareStatement(
                    "INSERT INTO lemma (lemma, frequency) VALUES (?, ?) ON DUPLICATE KEY UPDATE frequency = frequency + ?;",
                    Statement.RETURN_GENERATED_KEYS);
            insertIndexStatement = getConnection().prepareStatement(
                    "INSERT INTO `index` (page_id, lemma_id, `rank`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `rank` = `rank` + ?;");
        }

        getConnection().setAutoCommit(false);
        for (Page p : pagesBuffer) {
            p.setId(insertPageAndGetId(p));
            for(Lemma l : p.getLemmas()) {
                l.setId(insertLemmaAndGetId(l));
            }
        }
        getConnection().commit();
        insertIndex(pagesBuffer);
        log.info("number of indexed pages: " + numberOfIndexedPages);
    }

    public static int insertPageAndGetId(Page p) throws SQLException {
        insertPageStatement.setString(1, p.getPath());
        insertPageStatement.setInt(2, p.getCode());
        insertPageStatement.setString(3, p.getContent());
        insertPageStatement.addBatch();
        insertPageStatement.executeBatch();
        ResultSet res = insertPageStatement.getGeneratedKeys();
        if (res.next()) {
            return res.getInt(1);
        }
        res = getConnection().createStatement().executeQuery("SELECT p.id as idid FROM page p WHERE p.`path`='" + p.getPath() +"'");
        res.next();
        return res.getInt("idid");
    }

    public static int insertLemmaAndGetId(Lemma l) throws SQLException {
        insertLemmaStatement.setString(1, l.getNormalForm());
        insertLemmaStatement.setInt(2, l.getCount());
        insertLemmaStatement.setInt(3, l.getCount());
        insertLemmaStatement.addBatch();
        insertLemmaStatement.executeBatch();
        ResultSet res = insertLemmaStatement.getGeneratedKeys();
        res.next();
        return res.getInt(1);
    }

    public static void insertIndex(Set<Page> pages) throws SQLException {
        numberOfIndexedPages += pages.size();
        getConnection().setAutoCommit(false);
        for (Page p : pages) {
            for (Lemma l : p.getLemmas()) {
                insertIndexStatement.setInt(1, p.getId());
                insertIndexStatement.setInt(2, l.getId());
                insertIndexStatement.setDouble(3, l.getRank());
                insertIndexStatement.setDouble(4, l.getRank());
                insertIndexStatement.addBatch();
            }
            pages.remove(p);
        }
        insertIndexStatement.executeBatch();
        getConnection().commit();
    }


    public static Set<Field> getFields() throws SQLException {
        Set<Field> fields = new HashSet<>();
        ResultSet results = getConnection().createStatement().executeQuery("Select `name`, `selector`, `weight` FROM field");
        while (results.next()) {
            fields.add(new Field(results.getString("name"),
                    results.getString("selector"),
                    results.getDouble("weight")));
        }
        return fields;
    }
}
