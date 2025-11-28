// src/main/java/org/example/generator/CrosswordGenerator.java
package org.example;

import org.example.model.Crossword;
import java.util.ArrayList;
import java.util.List;

public class CrosswordGenerator {

    public static Crossword generateTestCrossword(List<MainMenuView.WordEntry> words, int wordCount) {
        // Поки що ігноруємо вхідні слова і робимо жорстко зафіксований тестовий кросворд
        int w = 15, h = 15;
        char[][] grid = new char[h][w];
        for (char[] row : grid) java.util.Arrays.fill(row, '.');

        // Простий тестовий кросворд 5×5 в центрі
        String[] testWords = {"JAVA", "KOTLIN", "GROK", "FXML", "MAVEN"};
        String[] hints = {"Мова програмування", "Альтернатива Java", "AI від xAI", "UI в JavaFX", "Збірка проєктів"};

        // По горизонталі
        placeWord(grid, 5, 5, "JAVA", true);
        placeWord(grid, 6, 4, "KOTLIN", true);
        placeWord(grid, 7, 5, "GROK", true);
        placeWord(grid, 8, 5, "FXML", true);
        placeWord(grid, 9, 5, "MAVEN", true);

        List<Crossword.Clue> across = new ArrayList<>();
        List<Crossword.Clue> down = new ArrayList<>();

        int clueNum = 1;
        across.add(new Crossword.Clue(clueNum++, 5, 5, hints[0], testWords[0], true));
        across.add(new Crossword.Clue(clueNum++, 6, 4, hints[1], testWords[1], true));
        across.add(new Crossword.Clue(clueNum++, 7, 5, hints[2], testWords[2], true));
        across.add(new Crossword.Clue(clueNum++, 8, 5, hints[3], testWords[3], true));
        across.add(new Crossword.Clue(clueNum++, 9, 5, hints[4], testWords[4], true));

        return new Crossword(w, h, grid, across, down);
    }

    private static void placeWord(char[][] grid, int row, int col, String word, boolean horizontal) {
        for (int i = 0; i < word.length(); i++) {
            if (horizontal) {
                grid[row][col + i] = word.charAt(i);
            } else {
                grid[row + i][col] = word.charAt(i);
            }
        }
    }
}