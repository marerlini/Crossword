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
import org.apache.pdfbox.pdmodel.font.PDFont;
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
        BufferedImage bImg = SwingFXUtils.fromFXImage(snapshot, null);

        FileChooser fc = new FileChooser();
        fc.setTitle("Зберегти кросворд як PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        fc.setInitialFileName("Кросворд_" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".pdf");

        File file = fc.showSaveDialog(crosswordGridPane.getScene().getWindow());
        if (file == null) return;

        try (PDDocument doc = new PDDocument()) {
            PDImageXObject gridImage = PDImageXObject.createFromByteArray(doc, bufferedImageToBytes(bImg), "grid");

            PDType0Font boldFont = loadTtfFont(doc, "/fonts/Roboto-Bold.ttf");
            PDType0Font regularFont = loadTtfFont(doc, "/fonts/Roboto-Regular.ttf");


            // Розбиваємо підказки на два списки
            List<String> across = new ArrayList<>();
            List<String> down = new ArrayList<>();
            boolean isAcross = true;
            for (String line : hintsArea.getText().split("\n")) {
                String t = line.trim();
                if (t.startsWith("ПО ГОРИЗОНТАЛІ")) { isAcross = true; continue; }
                if (t.startsWith("ПО ВЕРТИКАЛІ")) { isAcross = false; continue; }
                if (!t.isEmpty()) {
                    (isAcross ? across : down).add(t);
                }
            }

            // Параметри сторінки
            float margin = 40f;
            float pageWidth = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            float usableWidth = pageWidth - 2 * margin;
            float columnWidth = (usableWidth - 20) / 2;  // 20 — відступ між колонок
            float leftX = margin;
            float rightX = margin + columnWidth + 20;

            boolean firstPage = true;
            List<String> leftColumn = across;
            List<String> rightColumn = down;

            while (!leftColumn.isEmpty() || !rightColumn.isEmpty()) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                    if (firstPage) {
                        // === Сітка тільки на першій сторінці ===
                        float maxSize = 520f;
                        float scale = Math.min(maxSize / gridImage.getWidth(), maxSize / gridImage.getHeight());
                        float imgW = gridImage.getWidth() * scale;
                        float imgH = gridImage.getHeight() * scale;
                        float gridX = (pageWidth - imgW) / 2;
                        float gridY = pageHeight - 120 - imgH;

                        cs.drawImage(gridImage, gridX, gridY, imgW, imgH);

                        // Заголовок
                        cs.beginText();
                        cs.setFont(boldFont, 28);
                        cs.newLineAtOffset(pageWidth / 2 - 110, pageHeight - 70);
                        cs.showText("КРОСВОРД");
                        cs.endText();

                        cs.beginText();
                        cs.setFont(regularFont, 12);
                        cs.newLineAtOffset(pageWidth / 2 - 100, pageHeight - 100);
                        cs.showText("Дата: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                        cs.endText();

                        // Початок підказок — нижче сітки
                        float currentY = gridY - 40;

                        // Заголовки колонок
                        cs.beginText();
                        cs.setFont(boldFont, 14);
                        cs.newLineAtOffset(leftX, currentY);
                        cs.showText("ПО ГОРИЗОНТАЛІ");
                        cs.newLineAtOffset(rightX - leftX, 0);
                        cs.showText("ПО ВЕРТИКАЛІ");
                        cs.endText();

                        currentY -= 30;

                        // Друкуємо, скільки влізе
                        float spaceBelow = currentY - 60; // залишок до низу сторінки
                        float lineHeight = regularFont.getFontDescriptor().getCapHeight() / 1000 * 11 * 1.3f;

                        int linesPerPage = (int) (spaceBelow / lineHeight);

                        printCluesPortion(cs, regularFont, 11, leftX, rightX, currentY,
                                columnWidth, leftColumn, rightColumn, linesPerPage);

                        firstPage = false;
                    } else {
                        // === Наступні сторінки — тільки підказки ===
                        float startY = pageHeight - 60;

                        cs.beginText();
                        cs.setFont(boldFont, 16);
                        cs.newLineAtOffset(margin, startY);
                        cs.showText("Продовження підказок");
                        cs.endText();

                        startY -= 40;

                        int approxLines = (int) ((pageHeight - 100) / (11 * 1.3f));
                        printCluesPortion(cs, regularFont, 11, leftX, rightX, startY,
                                columnWidth, leftColumn, rightColumn, approxLines);
                    }
                }
            }

            doc.save(file);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Кросворд збережено у PDF" + (doc.getNumberOfPages() > 1 ? " (" + doc.getNumberOfPages() + " стор.)" : "") + "!");
            alert.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Помилка збереження PDF:\n" + ex.getMessage());
        }
    }

    private void printCluesPortion(PDPageContentStream cs, PDFont font, float fontSize,
                                   float leftX, float rightX, float startY,
                                   float columnWidth,
                                   List<String> leftList, List<String> rightList,
                                   int maxLinesPerColumn) throws IOException {

        float leading = fontSize * 1.3f;
        cs.setLeading(leading);

        // Ліва колонка
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(leftX, startY);
        int count = 0;
        while (!leftList.isEmpty() && count < maxLinesPerColumn) {
            String line = leftList.remove(0);
            List<String> wrapped = wrapText(line, font, fontSize, columnWidth);
            for (String part : wrapped) {
                if (count >= maxLinesPerColumn) break;
                cs.showText(part);
                cs.newLine();
                count++;
            }
        }
        cs.endText();

        // Права колонка
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(rightX, startY);
        count = 0;
        while (!rightList.isEmpty() && count < maxLinesPerColumn) {
            String line = rightList.remove(0);
            List<String> wrapped = wrapText(line, font, fontSize, columnWidth);
            for (String part : wrapped) {
                if (count >= maxLinesPerColumn) break;
                cs.showText(part);
                cs.newLine();
                count++;
            }
        }
        cs.endText();
    }

    private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (font.getStringWidth(text) / 1000 * fontSize <= maxWidth) {
            lines.add(text);
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            String test = current.length() == 0 ? word : current + " " + word;
            if (font.getStringWidth(test) / 1000 * fontSize > maxWidth) {
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    lines.add(word); // слово довше за колонку
                }
            } else {
                if (current.length() == 0) current.append(word);
                else current.append(" ").append(word);
            }
        }
        if (current.length() > 0) lines.add(current.toString());

        return lines;
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