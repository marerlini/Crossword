package org.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import org.example.service.DatabaseService;

public class ThemeCreatorView implements Initializable {

    @FXML
    private TextField themeNameField;

    @FXML
    private TextField wordField;

    @FXML
    private TextField hintField;

    @FXML
    private ListView<WordEntry> wordList;

    private ObservableList<WordEntry> wordEntries = FXCollections.observableArrayList();

    private DatabaseService databaseService = new DatabaseService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        wordList.setItems(wordEntries);
        wordList.setCellFactory(new Callback<ListView<WordEntry>, ListCell<WordEntry>>() {
            @Override
            public ListCell<WordEntry> call(ListView<WordEntry> param) {
                return new ListCell<WordEntry>() {
                    @Override
                    protected void updateItem(WordEntry item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.word + " - " + (item.hint.isEmpty() ? "без підказки" : item.hint));
                        }
                    }
                };
            }
        });
    }

    @FXML
    private void onAddWord() {
        String word = wordField.getText().trim();
        String hint = hintField.getText().trim();
        if (!word.isEmpty()) {
            wordEntries.add(new WordEntry(word, hint));
            wordField.clear();
            hintField.clear();
        }
    }

    @FXML
    private void onBack() throws IOException {
        MainApp.showMainMenu();
    }

    @FXML
    private void onSave() {
        String themeName = themeNameField.getText().trim();
        if (themeName.isEmpty() || wordEntries.isEmpty()) {
            System.out.println("Помилка: Введіть назву теми та додайте хоча б одне слово!");
            return;
        }
        try {
            databaseService.saveTopic(themeName, wordEntries);
            System.out.println("Тема збережена!");
            // Очистити поля після збереження
            themeNameField.clear();
            wordField.clear();
            hintField.clear();
            wordEntries.clear();
        } catch (Exception e) {
            System.out.println("Помилка збереження: " + e.getMessage());
        }
    }

    // Внутрішній клас для зберігання слова та підказки
    public static class WordEntry {
        public String word;
        public String hint;

        WordEntry(String word, String hint) {
            this.word = word;
            this.hint = hint;
        }
    }
}