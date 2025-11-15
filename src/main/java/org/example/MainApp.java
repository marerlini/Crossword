package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        showMainMenu();
        stage.setTitle("Генератор кросвордів");
        stage.show();
    }

    public static void showMainMenu() throws IOException {
        loadScene("/fxml/main-menu-view.fxml");
    }

    public static void showThemeCreator() throws IOException {
        loadScene("/fxml/theme-creator-view.fxml");
    }

    public static void showCrosswordResult() throws IOException {
        loadScene("/fxml/crossword-result-view.fxml");
    }

    private static void loadScene(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxml));
        Parent root = loader.load();
        primaryStage.setScene(new Scene(root));
    }

    public static void main(String[] args) {
        launch();
    }


}