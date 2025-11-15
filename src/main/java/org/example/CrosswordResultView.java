package org.example;

import javafx.fxml.FXML;
import java.io.IOException;

public class CrosswordResultView {

    @FXML
    private void onBack() throws IOException {
        MainApp.showMainMenu();
    }
}
