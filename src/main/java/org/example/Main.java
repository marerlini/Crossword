package org.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.*;

public class Main extends Application {

    // Файлова БД у домашній папці (~ = /Users/mariayerliniekova)
    private static final String DB_URL = "jdbc:h2:~/javafx_h2_db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASS = "";

    @Override
    public void start(Stage primaryStage) {
        createTable();

        Label label = new Label("Введи ім'я:");
        TextField textField = new TextField();
        textField.setPromptText("Наприклад: Марія");

        Button saveButton = new Button("Зберегти в БД");
        saveButton.setOnAction(e -> {
            saveToDatabase(textField.getText());
            textField.clear(); // Очистити поле після збереження
        });

        Button loadButton = new Button("Завантажити з БД");
        loadButton.setOnAction(e -> loadFromDatabase());

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(label, textField, saveButton, loadButton);

        Scene scene = new Scene(root, 320, 200);
        primaryStage.setTitle("JavaFX + H2 (файлова БД)");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private static void createTable() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255))";
            stmt.execute(sql);
            System.out.println("Таблиця 'users' готова. Дані зберігаються у файлі: ~/javafx_h2_db.mv.db");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private static void saveToDatabase(String name) {
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

    private static void loadFromDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users ORDER BY id")) {
            System.out.println("=== Дані з БД ===");
            boolean hasData = false;
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + ", Ім'я: " + rs.getString("name"));
                hasData = true;
            }
            if (!hasData) {
                System.out.println("БД порожня.");
            }
            System.out.println("================");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}