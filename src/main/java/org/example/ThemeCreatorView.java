package org.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

import org.example.service.DatabaseService;

public class ThemeCreatorView implements Initializable {

    @FXML private TextField themeNameField;
    @FXML private TextField wordField;
    @FXML private TextField hintField;
    @FXML private Button addButton;
    @FXML private Button importButton;
    @FXML private ListView<WordEntry> wordList;

    private ObservableList<WordEntry> wordEntries = FXCollections.observableArrayList();
    private final DatabaseService databaseService = new DatabaseService();

    private WordEntry currentlyEditing = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        wordList.setItems(wordEntries);

        wordList.setCellFactory(lv -> new ListCell<WordEntry>() {
            @Override
            protected void updateItem(WordEntry item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.word + (item.hint.isEmpty() ? "" : " — " + item.hint));
            }
        });

        // Подвійний клік — редагування
        wordList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                WordEntry selected = wordList.getSelectionModel().getSelectedItem();
                if (selected != null) startEditing(selected);
            }
        });

        // Delete — видалення
        wordList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                WordEntry selected = wordList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    wordEntries.remove(selected);
                    clearEditingState();
                }
            }
        });

        // Enter у полях
        wordField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) applyCurrentAction(); });
        hintField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) applyCurrentAction(); });
    }

    @FXML private void onAddWord() { applyCurrentAction(); }

    // === ІМПОРТ ФАЙЛУ ===
    @FXML
    private void onImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Імпортувати слова з файлу");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Текстові файли", "*.txt")
        );
        File file = fileChooser.showOpenDialog(wordList.getScene().getWindow());
        if (file == null) return;

        int imported = 0;
        int skipped = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String word, hint = "";

                // Розділяємо по " — " або " - " або просто по першому тире
                int dashIndex = line.indexOf("—");   // ем-деш
                if (dashIndex == -1) dashIndex = line.indexOf("–"); // ен-деш
                if (dashIndex == -1) dashIndex = line.indexOf('-');

                if (dashIndex > 0) {
                    word = line.substring(0, dashIndex).trim();
                    hint = line.substring(dashIndex + 1).trim();
                } else {
                    word = line;
                }

                if (word.isEmpty()) continue;

                // Перевіряємо на дублікат (за словом)
                boolean exists = wordEntries.stream().anyMatch(e -> e.word.equalsIgnoreCase(word));
                if (!exists) {
                    wordEntries.add(new WordEntry(word, hint));
                    imported++;
                } else {
                    skipped++;
                }
            }

            showInfo("Імпорт завершено!\nДодано: " + imported + "\nПропущено дублікатів: " + skipped);

        } catch (IOException ex) {
            showError("Не вдалося прочитати файл:\n" + ex.getMessage());
        }
    }

    private void applyCurrentAction() {
        String word = wordField.getText().trim();
        String hint = hintField.getText().trim();

        if (word.isEmpty()) return;

        if (currentlyEditing != null) {
            currentlyEditing.word = word;
            currentlyEditing.hint = hint;
            wordList.refresh();
            clearEditingState();
        } else {
            // Додаємо тільки якщо такого слова ще немає
            if (wordEntries.stream().noneMatch(e -> e.word.equalsIgnoreCase(word))) {
                wordEntries.add(new WordEntry(word, hint));
            }
            wordField.clear();
            hintField.clear();
        }
    }

    private void startEditing(WordEntry entry) {
        currentlyEditing = entry;
        wordField.setText(entry.word);
        hintField.setText(entry.hint);
        wordField.requestFocus();
        wordField.selectAll();
        addButton.setText("Оновити");
    }

    private void clearEditingState() {
        currentlyEditing = null;
        addButton.setText("Додати");
        wordField.clear();
        hintField.clear();
    }

    @FXML
    private void onBack() throws IOException {
        if (!wordEntries.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Вийти без збереження?", ButtonType.YES, ButtonType.NO);
            a.setTitle("Незбережені зміни");
            if (a.showAndWait().orElse(ButtonType.NO) == ButtonType.NO) return;
        }
        MainApp.showMainMenu();
    }

    @FXML
    private void onSave() {
        String themeName = themeNameField.getText().trim();
        if (themeName.isEmpty()) { showError("Введіть назву теми!"); return; }
        if (wordEntries.isEmpty()) { showError("Додайте хоча б одне слово!"); return; }

        try {
            databaseService.saveTopic(themeName, wordEntries);
            showInfo("Тему «" + themeName + "» збережено!\nСлів: " + wordEntries.size());
            themeNameField.clear();
            wordEntries.clear();
            clearEditingState();
        } catch (Exception e) {
            showError("Помилка: " + e.getMessage());
        }
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public static class WordEntry {
        public String word;
        public String hint = "";

        public WordEntry(String word, String hint) {
            this.word = word;
            this.hint = hint != null ? hint : "";
        }
    }
}