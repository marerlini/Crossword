package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.service.DatabaseService;
import org.example.view.DataPageView;
import org.example.view.InputPageView;

public class Main extends Application {

    private Stage primaryStage;
    private Scene mainScene, dataScene;

    private DatabaseService dbService;
    private DataPageView dataPageView; // Потрібен доступ для оновлення

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        // 1. Створюємо наш сервіс даних
        this.dbService = new DatabaseService();

        // 2. Створюємо класи-глядачі (View)
        // Ми передаємо їм сервіс (щоб вони могли зберігати/завантажувати)
        // і "дію" (щоб вони могли просити переключити сцену)
        InputPageView inputView = new InputPageView(dbService, this::showDataPage);

        this.dataPageView = new DataPageView(dbService, this::showMainPage);

        // 3. Створюємо сцени з розміток, які повернули наші View
        mainScene = new Scene(inputView.getLayout(), 350, 250);
        dataScene = new Scene(dataPageView.getLayout(), 350, 400);

        // 4. Запускаємо
        primaryStage.setTitle("JavaFX + H2 БД (Розділено)");
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    // --- Методи для навігації ---

    private void showMainPage() {
        primaryStage.setScene(mainScene);
    }

    private void showDataPage() {
        // Оновлюємо дані КОЖЕН РАЗ, коли переходимо на сторінку
        dataPageView.refreshData();
        primaryStage.setScene(dataScene);
    }

    // Точка входу
    public static void main(String[] args) {
        launch(args);
    }
}