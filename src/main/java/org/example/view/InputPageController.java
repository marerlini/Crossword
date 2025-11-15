package org.example.view;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.example.service.DatabaseService;

public class InputPageController {

    @FXML
    private TextField nameField;

    private DatabaseService dbService;
    private Runnable showDataPageCallback;

    public void setDependencies(DatabaseService dbService, Runnable showDataPageCallback) {
        this.dbService = dbService;
        this.showDataPageCallback = showDataPageCallback;
    }

    @FXML
    private void handleSaveButton() {
        if (dbService != null) {
            dbService.saveName(nameField.getText());
            nameField.clear();
        }
    }

    @FXML
    private void handleGoToDataButton() {
        if (showDataPageCallback != null) {
            showDataPageCallback.run();
        }
    }
}