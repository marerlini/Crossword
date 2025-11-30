package org.example.view;

import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import org.example.MainApp;
import org.example.model.Crossword;
import org.example.model.CrosswordGenerator;

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
            showError("Немає слів для генерації кросворду!");
            return;
        }

        // Перемішуємо та обрізаємо список до потрібної кількості
        List<MainMenuView.WordEntry> selected = new ArrayList<>(allWords);
        Collections.shuffle(selected);
        selected = selected.stream().limit(count).collect(Collectors.toList());

        try {
            crossword = CrosswordGenerator.generate(selected, count);

            // Якщо згенерувалося менше слів, ніж просили — попереджаємо
            int actualWords = crossword.getAcrossClues().size() + crossword.getDownClues().size();
            if (actualWords < count) {
                String warning = String.format(
                        "Увага!\n" +
                                "Запрошено %d слів, але вдалося розмістити лише %d.\n\n" +
                                "Можливі причини:\n" +
                                "• Дуже довгі або «незручні» слова в списку\n" +
                                "• Недостатньо перетинів між словами\n\n" +
                                "Спробуйте перегенерувати або видалити найдовші слова.",
                        count, actualWords
                );
                showWarning(warning);
            }

            renderGrid();
            renderHints();

            // Центруємо сітку
            crosswordGridPane.setScaleX(1.0);
            crosswordGridPane.setScaleY(1.0);
            crosswordScrollPane.setHvalue(0.5);
            crosswordScrollPane.setVvalue(0.5);

            answersVisible = false;
            showAnswerButton.setText("Показати відповіді");

        } catch (IllegalStateException e) {
            // Це саме та помилка, яку кидає генератор, якщо сітка не влізає
            showError(
                    "Не вдалося згенерувати кросворд!\n\n" +
                            e.getMessage() + "\n\n" +
                            "Рекомендації:\n" +
                            "• Зменшіть кількість слів для генерації\n" +
                            "• Видаліть найдовші слова зі списку\n" +
                            "• Спробуйте перегенерувати ще раз"
            );
        } catch (Exception e) {
            // Будь-які інші неочікувані помилки
            e.printStackTrace();
            showError("Невідома помилка під час генерації:\n" + e.getMessage());
        }
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

                    // Літера
                    Label letter = new Label(String.valueOf(Character.toUpperCase(ch)));
                    letter.setStyle("-fx-font-weight: bold; -fx-font-size: 18;");
                    letter.setVisible(answersVisible);

                    // Номер
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
        // Коліщатко + CTRL масштабування
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

        // Права кнопка або просто перетягування — рухати
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
            sb.append("\nПО ВЕРТИКАЛІ:\n(немає)\n");
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

    @FXML
    private void saveAsPdf() {
        WritableImage snapshot = crosswordGridPane.snapshot(new SnapshotParameters(), null);

        FileChooser fc = new FileChooser();
        fc.setTitle("Зберегти кросворд як PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        fc.setInitialFileName("Кросворд_" + LocalDate.now() + ".pdf");
        File file = fc.showSaveDialog(crosswordGridPane.getScene().getWindow());
        if (file == null) return;

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float width  = page.getMediaBox().getWidth();
            float height = page.getMediaBox().getHeight();

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // Сітка кросворду (максимум 520×520)
                BufferedImage bImg = SwingFXUtils.fromFXImage(snapshot, null);
                PDImageXObject img = PDImageXObject.createFromByteArray(doc, bufferedImageToBytes(bImg), "grid");

                float maxGridSize = 520f;
                float scale = Math.min(maxGridSize / img.getWidth(), maxGridSize / img.getHeight());
                float imgW = img.getWidth()  * scale;
                float imgH = img.getHeight() * scale;

                float gridX = (width - imgW) / 2;
                float gridY = height - 100 - imgH;

                cs.drawImage(img, gridX, gridY, imgW, imgH);

                // Шрифти з українською підтримкою
                PDType0Font boldFont    = loadTtfFont(doc, "/fonts/Roboto-Bold.ttf");
                PDType0Font regularFont = loadTtfFont(doc, "/fonts/Roboto-Regular.ttf");

                //Заголовок
                cs.beginText();
                cs.setFont(boldFont, 28);
                cs.newLineAtOffset(width / 2 - 110, height - 60);
                cs.showText("КРОСВОРД");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 12);
                cs.newLineAtOffset(width / 2 - 80, height - 90);
                cs.showText("Дата: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                cs.endText();

                // Підказки у дві колонки
                String[] lines = hintsArea.getText().split("\n");
                List<String> across = new ArrayList<>();
                List<String> down   = new ArrayList<>();

                boolean isAcross = true;
                for (String line : lines) {
                    if (line.trim().startsWith("ПО ГОРИЗОНТАЛІ")) isAcross = true;
                    else if (line.trim().startsWith("ПО ВЕРТИКАЛІ")) isAcross = false;
                    else if (!line.trim().isEmpty()) {
                        if (isAcross) across.add(line.trim());
                        else          down.add(line.trim());
                    }
                }

                float col1X = 40;
                float col2X = width / 2 + 20;
                float startY = gridY - 40;
                float leading = 14f;

                cs.setFont(boldFont, 14);
                cs.beginText();
                cs.newLineAtOffset(col1X, startY);
                cs.showText("ПО ГОРИЗОНТАЛІ");
                cs.endText();

                cs.beginText();
                cs.newLineAtOffset(col2X, startY);
                cs.showText("ПО ВЕРТИКАЛІ");
                cs.endText();

                cs.setFont(regularFont, 11);
                cs.setLeading(leading);

                // ліва колонка
                cs.beginText();
                cs.newLineAtOffset(col1X, startY - 25);
                for (String s : across) {
                    if (s.length() > 70) s = s.substring(0,67) + "…";
                    cs.showText(s);
                    cs.newLine();
                }
                cs.endText();

                // права колонка
                cs.beginText();
                cs.newLineAtOffset(col2X, startY - 25);
                for (String s : down) {
                    if (s.length() > 70) s = s.substring(0,67) + "…";
                    cs.showText(s);
                    cs.newLine();
                }
                cs.endText();
            }

            doc.save(file);

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Не вдалося зберегти PDF:\n" + ex.getMessage());
        }
    }

    private byte[] bufferedImageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private PDType0Font loadTtfFont(PDDocument doc, String fontPath) {
        try (InputStream fontStream = getClass().getResourceAsStream(fontPath)) {
            if (fontStream != null) {
                return PDType0Font.load(doc, fontStream, true);
            }
        } catch (Exception e) {
            System.err.println("Не вдалося завантажити шрифт " + fontPath + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Помилка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Не всі слова помістилися");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setPrefWidth(480);
        alert.showAndWait();
    }
}