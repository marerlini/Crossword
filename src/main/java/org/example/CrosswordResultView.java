package org.example;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.example.model.Crossword;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class CrosswordResultView implements Initializable {

    @FXML private Pane crosswordGridPane;
    @FXML private TextArea hintsArea;
    @FXML private Label titleLabel;

    private Crossword crossword;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        crossword = MainApp.getCurrentCrossword();
        if (crossword == null) {
            hintsArea.setText("Кросворд не згенеровано!");
            return;
        }

        renderGrid();
        renderHints();
    }

    private void renderGrid() {
        double cellSize = 30;
        crosswordGridPane.getChildren().clear();
        ((GridPane) crosswordGridPane).setGridLinesVisible(true); // тепер працює!

        for (int row = 0; row < crossword.getHeight(); row++) {
            for (int col = 0; col < crossword.getWidth(); col++) {
                char ch = crossword.getCell(row, col);

                StackPane cell = new StackPane();
                cell.setPrefSize(cellSize, cellSize);
                cell.setMinSize(cellSize, cellSize);
                cell.setMaxSize(cellSize, cellSize);

                if (ch == '.') {
                    Rectangle black = new Rectangle(cellSize, cellSize);
                    black.setFill(Color.BLACK);
                    cell.getChildren().add(black);
                } else {
                    Rectangle white = new Rectangle(cellSize, cellSize);
                    white.setFill(Color.WHITE);
                    white.setStroke(Color.BLACK);
                    Label letter = new Label(String.valueOf(Character.toUpperCase(ch)));
                    letter.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
                    cell.getChildren().addAll(white, letter);
                }

                crosswordGridPane.getChildren().add(cell);
                GridPane.setRowIndex(cell, row);
                GridPane.setColumnIndex(cell, col);
            }
        }

//        ((GridPane) crosswordGridPane.getParent()).setGridLinesVisible(true);
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