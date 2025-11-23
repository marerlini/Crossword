package org.example.service;

import javafx.collections.ObservableList;
import org.example.ThemeCreatorView;

import java.sql.*;

public class DatabaseService {

    // Константи для підключення
    private static final String DB_URL = "jdbc:h2:~/javafx_h2_db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASS = "";

    // Конструктор: при створенні сервісу одразу перевіряємо таблицю
    public DatabaseService() {
        createTables();
    }

    // === Створення таблиць ===
    private void createTables() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {

            // 1. Створюємо таблицю ТЕМ (topics)
            // id - унікальний номер теми
            // name - назва теми (наприклад, "Фрукти")
            String sqlTopics = "CREATE TABLE IF NOT EXISTS topics (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL UNIQUE)";
            stmt.execute(sqlTopics);

            // 2. Створюємо таблицю СЛІВ (words)
            // word - саме слово
            // hint - підказка
            // topic_id - це зв'язок! Це id з таблиці topics
            // FOREIGN KEY... - це правило, яке каже, що topic_id має існувати в таблиці topics
            String sqlWords = "CREATE TABLE IF NOT EXISTS words (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "word VARCHAR(255) NOT NULL, " +
                    "hint VARCHAR(255), " +
                    "topic_id INT, " +
                    "FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE)";
            stmt.execute(sqlWords);

            System.out.println("База даних оновлена: таблиці topics та words готові.");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // === Збереження теми та слів ===
    public void saveTopic(String name, ObservableList<ThemeCreatorView.WordEntry> entries) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new SQLException("Помилка: Введіть назву теми!");
        }
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            // Вставляємо тему та отримуємо її ID
            PreparedStatement pstmtTopic = conn.prepareStatement(
                    "INSERT INTO topics (name) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            pstmtTopic.setString(1, name.trim());
            pstmtTopic.executeUpdate();

            ResultSet generatedKeys = pstmtTopic.getGeneratedKeys();
            int topicId = -1;
            if (generatedKeys.next()) {
                topicId = generatedKeys.getInt(1);
            }
            if (topicId == -1) {
                throw new SQLException("Помилка: Не вдалося створити тему!");
            }

            // Вставляємо слова
            PreparedStatement pstmtWords = conn.prepareStatement(
                    "INSERT INTO words (word, hint, topic_id) VALUES (?, ?, ?)"
            );
            for (ThemeCreatorView.WordEntry entry : entries) {
                pstmtWords.setString(1, entry.word);
                pstmtWords.setString(2, entry.hint.isEmpty() ? null : entry.hint);
                pstmtWords.setInt(3, topicId);
                pstmtWords.addBatch();
            }
            pstmtWords.executeBatch();
        } catch (SQLException ex) {
            // Обробка унікальності назви теми
            if (ex.getErrorCode() == -104) { // H2 помилка для порушення унікальності
                throw new SQLException("Помилка: Тема з такою назвою вже існує!");
            }
            throw ex;
        }
    }
}