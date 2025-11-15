package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.service.DatabaseService;
import org.example.view.DataPageView;
import org.example.view.InputPageController;

import java.io.IOException;

public class Main extends Application {

    private Stage primaryStage;
    private Scene mainScene, dataScene;
    private DatabaseService dbService;
    private DataPageView dataPageView;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.dbService = new DatabaseService();

        try {
            // Завантажуємо FXML
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/org.example.view/input-page-view.fxml"));
            Parent inputPageRoot = loader.load();

            // Отримуємо контролер
            InputPageController inputController = loader.getController();
            inputController.setDependencies(dbService, this::showDataPage);

            mainScene = new Scene(inputPageRoot, 350, 250);

            // Сторінка 2
            this.dataPageView = new DataPageView(dbService, this::showMainPage);
            dataScene = new Scene(dataPageView.getLayout(), 350, 400);

            primaryStage.setTitle("JavaFX + FXML + H2");
            primaryStage.setScene(mainScene);
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Не вдалося знайти FXML-файл. Перевірте шлях: /org/example/view/input-page-view.fxml");
        }
    }

    private void showMainPage() {
        primaryStage.setScene(mainScene);
    }

    private void showDataPage() {
        dataPageView = new DataPageView(dbService, this::showMainPage); // оновлюємо
        dataScene = new Scene(dataPageView.getLayout(), 350, 400);
        primaryStage.setScene(dataScene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}