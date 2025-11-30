package org.example.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.example.MainApp;

public class MainMenuView implements Initializable {

    @FXML private ComboBox<String> themeComboBox;
    @FXML private TextField wordField;
    @FXML private TextField hintField;
    @FXML private Button addButton;
    @FXML private ListView<WordEntry> wordList;
    @FXML private TextField wordCountField;

    // Тимчасовий список слів (не зберігається в БД)
    private ObservableList<WordEntry> currentWords = FXCollections.observableArrayList();

    private WordEntry currentlyEditing = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        wordList.setItems(currentWords);
        updateWordCount();

        // Форматування списку
        wordList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(WordEntry item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.word + (item.hint.isEmpty() ? "" : " — " + item.hint));
            }
        });

        // Завантажуємо теми з бази
        loadThemes();

        // При виборі теми — завантажуємо її слова
        themeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, old, newTheme) -> {
            if (newTheme != null && !newTheme.equals("Обрати тему")) {
                loadWordsFromTheme(newTheme);
            } else {
                currentWords.clear();
                updateWordCount();
            }
        });

        // Подвійний клік = редагування
        wordList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                WordEntry selected = wordList.getSelectionModel().getSelectedItem();
                if (selected != null) startEditing(selected);
            }
        });

        // Delete = видалити
        wordList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                WordEntry selected = wordList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    currentWords.remove(selected);
                    clearEditingState();
                    updateWordCount();
                }
            }
        });

        // Enter у полях = додати/оновити
        wordField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) applyWordAction(); });
        hintField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) applyWordAction(); });
    }

    private void loadThemes() {
        themeComboBox.getItems().clear();
        themeComboBox.getItems().add("Обрати тему");

        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:~/javafx_h2_db;AUTO_SERVER=TRUE", "sa", "");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM topics ORDER BY name")) {

            while (rs.next()) {
                themeComboBox.getItems().add(rs.getString("name"));
            }
        } catch (SQLException e) {
            showError("Не вдалося завантажити теми: " + e.getMessage());
        }

        themeComboBox.getSelectionModel().selectFirst(); // Обрати тему
    }

    private void loadWordsFromTheme(String themeName) {
        currentWords.clear();
        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:~/javafx_h2_db;AUTO_SERVER=TRUE", "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT word, hint FROM words WHERE topic_id = (SELECT id FROM topics WHERE name = ?)")) {

            pstmt.setString(1, themeName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String word = rs.getString("word");
                String hint = rs.getString("hint");
                if (hint == null) hint = "";
                currentWords.add(new WordEntry(word, hint));
            }
        } catch (SQLException e) {
            showError("Не вдалося завантажити слова: " + e.getMessage());
        }
        updateWordCount();
        clearEditingState();
    }

    @FXML
    private void onAddWord() {
        applyWordAction();
    }

    private void applyWordAction() {
        String word = wordField.getText().trim();
        String hint = hintField.getText().trim();
        if (word.isEmpty()) return;

        if (currentlyEditing != null) {
            // Редагуємо
            currentlyEditing.word = word;
            currentlyEditing.hint = hint;
            wordList.refresh();
            clearEditingState();
            // Не оновлюємо лічильник при редагуванні (кількість не змінюється)
        } else {
            // Додаємо нове (уникаємо дублікатів за словом)
            if (currentWords.stream().noneMatch(w -> w.word.equalsIgnoreCase(word))) {
                currentWords.add(new WordEntry(word, hint));
                updateWordCount(); // Оновлюємо тільки при додаванні нового слова
            }
        }
        wordField.clear();
        hintField.clear();
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
        addButton.setText("Додати слово");
        wordField.clear();
        hintField.clear();
    }

    private void updateWordCount() {
        wordCountField.setText(String.valueOf(currentWords.size()));
    }

    @FXML
    private void onCreateTheme() throws IOException {
        MainApp.showThemeCreator();
    }

    @FXML
    private void onGenerate() throws IOException {
        if (currentWords.isEmpty()) {
            showError("Додайте хоча б одне слово перед генерацією!");
            return;
        }

        String countText = wordCountField.getText().trim();
        int count;
        try {
            count = Integer.parseInt(countText);
            if (count <= 0) count = currentWords.size();
            if (count > currentWords.size()) count = currentWords.size();
        } catch (NumberFormatException e) {
            count = currentWords.size();
        }

        // передаємо дані
        MainApp.showCrosswordResult(new ArrayList<>(currentWords), count);
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    // Внутрішній клас
    public static class WordEntry {
        public String word;
        public String hint = "";

        public WordEntry(String word, String hint) {
            this.word = word;
            this.hint = hint != null ? hint : "";
        }
    }
}