package org.example;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Button;
import javafx.geometry.Insets;

import java.util.*;

import org.example.model.Crossword;

import java.io.IOException;
import java.net.URL;
import java.util.stream.Collectors;

public class CrosswordResultView implements Initializable {

    @FXML private ScrollPane crosswordScrollPane;
    @FXML private GridPane crosswordGridPane;
    @FXML private TextArea hintsArea;
    @FXML private Button showAnswerButton;
    private boolean answersVisible = false;

    private Crossword crossword;
    private double zoomLevel = 1.0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        generateAndShowCrossword();  // генеруємо при відкритті
        setupZoomAndPanning();
    }

    private void generateAndShowCrossword() {
        List<MainMenuView.WordEntry> allWords = MainApp.getWordsForGeneration();
        int count = MainApp.getWordsToUseCount();

        if (allWords == null || allWords.isEmpty()) {
            hintsArea.setText("Немає слів для генерації!");
            return;
        }

        List<MainMenuView.WordEntry> selected = new ArrayList<>(allWords);
        Collections.shuffle(selected);
        selected = selected.stream().limit(count).collect(Collectors.toList());

        crossword = CrosswordGenerator.generate(selected, count);

        renderGrid();
        renderHints();

        crosswordGridPane.setScaleX(1.0);
        crosswordGridPane.setScaleY(1.0);
        crosswordScrollPane.setHvalue(0.5);
        crosswordScrollPane.setVvalue(0.5);

        answersVisible = false;
        showAnswerButton.setText("Показати відповіді");
    }

    @FXML
    private void regenerateCrossword() {
        generateAndShowCrossword();
    }

    private void renderGrid() {
        GridPane grid = crosswordGridPane;
        grid.getChildren().clear();
        grid.getRowConstraints().clear();
        grid.getColumnConstraints().clear();

        int rows = crossword.getHeight();
        int cols = crossword.getWidth();
        double cellSize = 40.0;

        for (int i = 0; i < rows; i++) {
            grid.getRowConstraints().add(new RowConstraints(cellSize));
        }
        for (int i = 0; i < cols; i++) {
            grid.getColumnConstraints().add(new ColumnConstraints(cellSize));
        }

        record Cell(int row, int col) {}
        var numberMap = new HashMap<Cell, Integer>();

        for (var clue : crossword.getAcrossClues()) {
            numberMap.put(new Cell(clue.row, clue.col), clue.number);
        }
        for (var clue : crossword.getDownClues()) {
            numberMap.put(new Cell(clue.row, clue.col), clue.number);
        }

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                char ch = crossword.getCell(row, col);

                StackPane cell = new StackPane();
                cell.setMinSize(cellSize, cellSize);
                cell.setPrefSize(cellSize, cellSize);
                cell.setMaxSize(cellSize, cellSize);

                if (ch == '.') {
                    Rectangle black = new Rectangle(cellSize, cellSize, Color.BLACK);
                    cell.getChildren().add(black);
                } else {
                    Rectangle white = new Rectangle(cellSize, cellSize);
                    white.setFill(Color.WHITE);
                    white.setStroke(Color.BLACK);
                    white.setStrokeWidth(2);

                    // Літера — тепер її можна ховати/показувати
                    Label letter = new Label(String.valueOf(Character.toUpperCase(ch)));
                    letter.setStyle("-fx-font-weight: bold; -fx-font-size: 18;");
                    letter.setVisible(answersVisible);  // ключовий рядок!

                    // Номер (якщо є)
                    Integer clueNumber = numberMap.get(new Cell(row, col));
                    if (clueNumber != null) {
                        Label numberLabel = new Label(String.valueOf(clueNumber));
                        numberLabel.setStyle("-fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: #222222;");
                        numberLabel.setMouseTransparent(true);
                        StackPane.setAlignment(numberLabel, Pos.TOP_LEFT);
                        StackPane.setMargin(numberLabel, new Insets(3, 0, 0, 5));
                        cell.getChildren().addAll(white, letter, numberLabel);
                    } else {
                        cell.getChildren().addAll(white, letter);
                    }
                }

                grid.add(cell, col, row);
            }
        }
    }

    @FXML
    private void toggleAnswers() {
        answersVisible = !answersVisible;
        showAnswerButton.setText(answersVisible ? "Приховати відповіді" : "Показати відповіді");
        renderGrid();
    }

    private void setupZoomAndPanning() {
        // Коліщатко — масштабування
        crosswordScrollPane.setOnScroll(event -> {
            if (event.isControlDown()) {
                double delta = event.getDeltaY();
                double scaleFactor = (delta > 0) ? 1.1 : 0.9;
                zoomLevel *= scaleFactor;

                // Обмежуємо масштаб (від 0.3x до 3x)
                zoomLevel = Math.max(0.3, Math.min(zoomLevel, 3.0));

                crosswordGridPane.setScaleX(zoomLevel);
                crosswordGridPane.setScaleY(zoomLevel);

                event.consume();
            }
        });

        // Права кнопка або просто перетягування — рухати (pannable=true вже є)
        crosswordScrollPane.setPannable(true);
    }

    private void renderHints() {
        StringBuilder sb = new StringBuilder();
        sb.append("ПО ГОРИЗОНТАЛІ:\n");
        for (var clue : crossword.getAcrossClues()) {
            sb.append(clue.number).append(". ").append(clue.text)
                    .append(" (").append(clue.answer).append(")\n");
        }
        if (crossword.getDownClues().isEmpty()) {
            sb.append("\nПО ВЕРТИКАЛІ:\n(поки немає)\n");
        } else {
            sb.append("\nПО ВЕРТИКАЛІ:\n");
            for (var clue : crossword.getDownClues()) {
                sb.append(clue.number).append(". ").append(clue.text)
                        .append(" (").append(clue.answer).append(")\n");
            }
        }
        hintsArea.setText(sb.toString());
    }


    @FXML
    private void onBack() throws IOException {
        MainApp.showMainMenu();
    }
}