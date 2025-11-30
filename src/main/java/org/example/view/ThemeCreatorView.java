package org.example.view;

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

import org.example.MainApp;
import org.example.service.DatabaseService;

public class ThemeCreatorView implements Initializable {

    @FXML private TextField themeNameField;
    @FXML private TextField wordField;
    @FXML private TextField hintField;
    @FXML private Button addButton;
    @FXML private ListView<MainMenuView.WordEntry> wordList;

    private final ObservableList<MainMenuView.WordEntry> wordEntries = FXCollections.observableArrayList();
    private final DatabaseService databaseService = new DatabaseService();
    private MainMenuView.WordEntry currentlyEditing = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        wordList.setItems(wordEntries);

        wordList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(MainMenuView.WordEntry item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.word + (item.hint.isEmpty() ? "" : " — " + item.hint));
            }
        });

        // Обмеження та автодоповнення для поля слова
        wordField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null) {
                // Обмеження 50 символів
                if (newText.length() > 50) {
                    wordField.setText(oldText);
                    showError("Максимум 50 символів для слова!");
                    return;
                }
                // Авто-верхній регістр
                String upper = newText.toUpperCase();
                if (!newText.equals(upper)) {
                    wordField.setText(upper);
                    wordField.positionCaret(upper.length());
                }
            }
        });

        wordField.setPromptText("Слово (макс. 50 символів)");
        hintField.setPromptText("Підказка (обов’язково!)");

        // Подвійний клік — редагування
        wordList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                MainMenuView.WordEntry selected = wordList.getSelectionModel().getSelectedItem();
                if (selected != null) startEditing(selected);
            }
        });

        // Delete — видалення
        wordList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                MainMenuView.WordEntry selected = wordList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    wordEntries.remove(selected);
                    clearEditingState();
                }
            }
        });

        // Enter у полях = додати/оновити
        wordField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) applyCurrentAction(); });
        hintField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) applyCurrentAction(); });
    }

    @FXML private void onAddWord() { applyCurrentAction(); }

    // ІМПОРТ ФАЙЛУ
    @FXML
    private void onImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Імпортувати слова з файлу");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Текстові файли (*.txt)", "*.txt")
        );
        File file = fileChooser.showOpenDialog(wordList.getScene().getWindow());
        if (file == null) return;

        int imported = 0;
        int skipped = 0;
        int invalid = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue; // коментарі

                String word, hint = "";

                int separatorPos = line.indexOf("—");
                if (separatorPos == -1) separatorPos = line.indexOf("–");
                if (separatorPos == -1) separatorPos = line.indexOf("-");
                if (separatorPos == -1) separatorPos = line.indexOf(":");

                if (separatorPos > 0) {
                    word = line.substring(0, separatorPos).trim().toUpperCase();
                    hint = line.substring(separatorPos + 1).trim();
                } else {
                    word = line.trim().toUpperCase();
                }

                // Валідація при імпорті
                if (word.isEmpty() || word.isBlank()) {
                    invalid++;
                    continue;
                }
                if (word.length() > 50) {
                    invalid++;
                    continue;
                }
                if (hint.isEmpty()) {
                    invalid++;
                    continue;
                }

                boolean exists = wordEntries.stream().anyMatch(e -> e.word.equalsIgnoreCase(word));
                if (!exists) {
                    wordEntries.add(new MainMenuView.WordEntry(word, hint));
                    imported++;
                } else {
                    skipped++;
                }
            }

            String message = "Імпорт завершено!\n" +
                    "Додано нових: " + imported + "\n" +
                    "Пропущено дублікатів: " + skipped;
            if (invalid > 0) {
                message += "\nПропущено некоректних рядків: " + invalid;
            }

            showInfo(message);

        } catch (IOException ex) {
            showError("Не вдалося прочитати файл:\n" + ex.getMessage());
        }
    }

    private void applyCurrentAction() {
        String rawWord = wordField.getText();
        String hint = hintField.getText().trim();

        if (rawWord == null || rawWord.trim().isEmpty()) {
            showError("Введіть слово!");
            return;
        }

        String word = rawWord.trim().toUpperCase();

        // Перевірки
        if (word.length() > 50) {
            showError("Слово занадто довге! Максимум 50 символів.");
            return;
        }

        if (word.isBlank()) {
            showError("Слово не може складатися лише з пробілів!");
            return;
        }

        if (hint.isEmpty()) {
            showError("Введіть підказку до слова!");
            return;
        }

        if (currentlyEditing != null) {
            // === РЕДАГУВАННЯ ===
            // Дозволяємо змінити навіть на таке саме слово (бо це те саме)
            currentlyEditing.word = word;
            currentlyEditing.hint = hint;
            wordList.refresh();
            clearEditingState();
        } else {
            // === ДОДАВАННЯ НОВОГО ===
            boolean exists = wordEntries.stream()
                    .anyMatch(e -> e.word.equalsIgnoreCase(word));

            if (exists) {
                showError("Слово \"" + word + "\" вже є у списку теми!");
                return;
            }

            wordEntries.add(new MainMenuView.WordEntry(word, hint));
        }

        // Очищення полів
        wordField.clear();
        hintField.clear();
        wordField.requestFocus();
    }

    private void startEditing(MainMenuView.WordEntry entry) {
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
}