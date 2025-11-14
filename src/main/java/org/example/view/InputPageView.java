package org.example.view;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.example.service.DatabaseService;

// Runnable - це "дія", яку ми передаємо (наприклад, "перейти на іншу сторінку")
public class InputPageView {

    private VBox layout;
    private TextField nameField;

    public InputPageView(DatabaseService dbService, Runnable showDataPageCallback) {
        layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label titleLabel = new Label("Збереження в БД");
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Label inputLabel = new Label("Введи ім'я:");
        nameField = new TextField();
        nameField.setPromptText("Наприклад: Олена");

        Button saveButton = new Button("Зберегти");
        saveButton.setOnAction(e -> {
            dbService.saveName(nameField.getText()); // Використовуємо сервіс
            nameField.clear();
        });

        Button goToDataButton = new Button("Переглянути дані");
        // Викликаємо "дію", яку нам передали з MainApp
        goToDataButton.setOnAction(e -> showDataPageCallback.run());

        layout.getChildren().addAll(titleLabel, inputLabel, nameField, saveButton, goToDataButton);
    }

    // Метод, щоб MainApp міг отримати готовий UI
    public VBox getLayout() {
        return layout;
    }
}