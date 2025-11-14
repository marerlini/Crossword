package org.example;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.*;

public class Main extends Application {

    private static final String DB_URL = "jdbc:h2:~/javafx_h2_db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASS = "";

    private Stage primaryStage;
    private Scene mainScene, dataScene;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        createTable();

        // === Сторінка 1: Введення даних ===
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));

        Label titleLabel = new Label("Збереження в БД");
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Label inputLabel = new Label("Введи ім'я:");
        TextField nameField = new TextField();
        nameField.setPromptText("Наприклад: Олена");

        Button saveButton = new Button("Зберегти");
        saveButton.setOnAction(e -> {
            saveToDatabase(nameField.getText());
            nameField.clear();
        });

        Button goToDataButton = new Button("Переглянути дані");
        goToDataButton.setOnAction(e -> primaryStage.setScene(dataScene));

        mainLayout.getChildren().addAll(titleLabel, inputLabel, nameField, saveButton, goToDataButton);

        mainScene = new Scene(mainLayout, 350, 250);

        // === Сторінка 2: Перегляд даних ===
        VBox dataLayout = new VBox(15);
        dataLayout.setPadding(new Insets(20));

        Label dataTitle = new Label("Збережені імена");
        dataTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        ListView<String> listView = new ListView<>();
        listView.setPlaceholder(new Label("Дані відсутні"));

        Button refreshButton = new Button("Оновити");
        refreshButton.setOnAction(e -> loadDataToListView(listView));

        Button backButton = new Button("На головну");
        backButton.setOnAction(e -> primaryStage.setScene(mainScene));

        dataLayout.getChildren().addAll(dataTitle, listView, refreshButton, backButton);

        dataScene = new Scene(dataLayout, 350, 400);

        // Початкова сцена
        primaryStage.setTitle("JavaFX + H2 БД (2 сторінки)");
        primaryStage.setScene(mainScene);
        primaryStage.show();

        // Завантажити дані при відкритті другої сторінки
        loadDataToListView(listView);
    }

    // === Створення таблиці ===
    private static void createTable() {
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

    // === Завантаження в ListView ===
    private static void loadDataToListView(ListView<String> listView) {
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
        listView.setItems(items);
    }

    public static void main(String[] args) {
        launch(args);
    }
}