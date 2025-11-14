package org.example.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

public class DatabaseService {

    // Константи для підключення
    private static final String DB_URL = "jdbc:h2:~/javafx_h2_db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASS = "";

    // Конструктор: при створенні сервісу одразу перевіряємо таблицю
    public DatabaseService() {
        createTable();
    }

    // === Створення таблиці ===
    private void createTable() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255))";
            stmt.execute(sql);
            System.out.println("Таблиця готова. Файл: ~/javafx_h2_db.mv.db");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // === Збереження ===
    public void saveName(String name) {
        if (name == null || name.trim().isEmpty()) {
            System.out.println("Помилка: Введи ім'я!");
            return;
        }
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users (name) VALUES (?)")) {
            pstmt.setString(1, name.trim());
            pstmt.executeUpdate();
            System.out.println("Збережено: " + name);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // === Завантаження даних ===
    // Цей метод тепер повертає список, а не змінює UI напряму
    public ObservableList<String> loadNames() {
        ObservableList<String> items = FXCollections.observableArrayList();
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM users ORDER BY id")) {

            while (rs.next()) {
                items.add(rs.getInt("id") + ": " + rs.getString("name"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            items.add("Помилка завантаження");
        }
        return items;
    }
}