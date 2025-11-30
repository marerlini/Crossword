package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.example.model.Crossword;

public class MainApp extends Application {

    private static Stage primaryStage;
    private static List<MainMenuView.WordEntry> allWordsForGeneration;
    private static int wordsToUseCount;
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

    public static void showCrosswordResult(List<MainMenuView.WordEntry> words, int count) throws IOException {
        allWordsForGeneration = new ArrayList<>(words);
        wordsToUseCount = count;
        loadScene("/fxml/crossword-result-view.fxml");
    }

    public static List<MainMenuView.WordEntry> getWordsForGeneration() {
        return allWordsForGeneration;
    }

    public static int getWordsToUseCount() {
        return wordsToUseCount;
    }

    private static void loadScene(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxml));
        Parent root = loader.load();
        primaryStage.setScene(new Scene(root, 1200, 600));
    }


    public static void main(String[] args) {
        launch();
    }


}