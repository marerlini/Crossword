package org.example;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.io.IOException;

public class MainMenuView {

    @FXML private TextField wordField;
    @FXML private TextField hintField;

    @FXML
    private void onCreateTheme() throws IOException {
        MainApp.showThemeCreator();
    }

    @FXML
    private void onGenerate() throws IOException {
        MainApp.showCrosswordResult();
    }
}