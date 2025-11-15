package org.example;

import javafx.fxml.FXML;
import java.io.IOException;

public class ThemeCreatorView {

    @FXML
    private void onBack() throws IOException {
        MainApp.showMainMenu();
    }

    @FXML
    private void onSave() {
        // Логіка збереження теми (поки заглушка)
        System.out.println("Тема збережена!");
    }
}