package org.example.view;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import org.example.service.DatabaseService;

public class DataPageView {

    private VBox layout;
    private ListView<String> listView;
    private DatabaseService dbService; // Зберігаємо для оновлення

    public DataPageView(DatabaseService dbService, Runnable showMainPageCallback) {
        this.dbService = dbService;

        layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label dataTitle = new Label("Збережені імена");
        dataTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        listView = new ListView<>();
        listView.setPlaceholder(new Label("Дані відсутні"));

        Button refreshButton = new Button("Оновити");
        refreshButton.setOnAction(e -> refreshData()); // Викликаємо локальний метод

        Button backButton = new Button("На головну");
        backButton.setOnAction(e -> showMainPageCallback.run()); // Викликаємо "дію"

        layout.getChildren().addAll(dataTitle, listView, refreshButton, backButton);
    }

    // Метод для оновлення даних у списку
    public void refreshData() {
        listView.setItems(dbService.loadNames());
    }

    // Метод, щоб MainApp міг отримати готовий UI
    public VBox getLayout() {
        return layout;
    }
}