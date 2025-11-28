package org.example;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.input.ScrollEvent;
import javafx.scene.control.ScrollPane;
import org.example.model.Crossword;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class CrosswordResultView implements Initializable {

    @FXML private ScrollPane crosswordScrollPane;   // Обов'язково має бути!
    @FXML private GridPane crosswordGridPane;       // Це тепер GridPane, а не Pane!
    @FXML private TextArea hintsArea;

    private Crossword crossword;
    private double zoomLevel = 1.0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        crossword = MainApp.getCurrentCrossword();
        if (crossword == null) {
            hintsArea.setText("Кросворд не згенеровано!");
            return;
        }

        renderGrid();
        renderHints();
        setupZoomAndPanning();
    }

    private void renderGrid() {
        GridPane grid = crosswordGridPane;
        grid.getChildren().clear();
        grid.getRowConstraints().clear();
        grid.getColumnConstraints().clear();

        int rows = crossword.getHeight();
        int cols = crossword.getWidth();

        // Фіксований розмір клітинки (наприклад 40px) — це важливо!
        double cellSize = 40;

        for (int i = 0; i < rows; i++) {
            RowConstraints rc = new RowConstraints(cellSize);
            grid.getRowConstraints().add(rc);
        }
        for (int i = 0; i < cols; i++) {
            ColumnConstraints cc = new ColumnConstraints(cellSize);
            grid.getColumnConstraints().add(cc);
        }

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                char ch = crossword.getCell(row, col);

                StackPane cell = new StackPane();
                cell.setMinSize(cellSize, cellSize);
                cell.setPrefSize(cellSize, cellSize);
                cell.setMaxSize(cellSize, cellSize);
                cell.setAlignment(Pos.CENTER);

                if (ch == '.') {
                    Rectangle black = new Rectangle(cellSize, cellSize, Color.BLACK);
                    cell.getChildren().add(black);
                } else {
                    Rectangle white = new Rectangle(cellSize, cellSize);
                    white.setFill(Color.WHITE);
                    white.setStroke(Color.BLACK);
                    white.setStrokeWidth(2);

                    Label letter = new Label(String.valueOf(Character.toUpperCase(ch)));
                    letter.setStyle("-fx-font-weight: bold; -fx-font-size: 18;");

                    cell.getChildren().addAll(white, letter);
                }

                grid.add(cell, col, row);
            }
        }
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