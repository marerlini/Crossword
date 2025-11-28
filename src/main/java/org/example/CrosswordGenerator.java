package org.example;

import org.example.model.Crossword;

import java.util.*;
import java.util.stream.Collectors;

public class CrosswordGenerator {

    private static final int GRID_SIZE = 10; // достатньо для більшості кросвордів

    private char[][] grid;
    private final List<MainMenuView.WordEntry> words;
    private final List<Placement> placements = new ArrayList<>();
    private final Set<String> usedWords = new HashSet<>();

    private record Placement(String word, String hint, int row, int col, boolean horizontal, int number) {}

    public static Crossword generate(List<MainMenuView.WordEntry> wordEntries, int maxWords) {
        return new CrosswordGenerator(wordEntries, maxWords).generate();
    }

    private CrosswordGenerator(List<MainMenuView.WordEntry> words, int maxWords) {
        this.words = words.stream()
                .limit(maxWords)
                .collect(Collectors.toList());
        this.grid = new char[GRID_SIZE][GRID_SIZE];
        for (char[] row : grid) Arrays.fill(row, '.');
    }

    private Crossword generate() {
        // Сортуємо слова: спочатку найдовші і найрідші букви — це евристика
        List<MainMenuView.WordEntry> sorted = new ArrayList<>(words);
        sorted.sort((a, b) -> {
            int lenDiff = b.word.length() - a.word.length();
            if (lenDiff != 0) return lenDiff;
            return Integer.compare(rareLetterScore(b.word), rareLetterScore(a.word));
        });

        int clueNumber = 1;
        if (!sorted.isEmpty()) {
            // Перше слово — по центру горизонтально
            MainMenuView.WordEntry first = sorted.get(0);
            int row = GRID_SIZE / 2;
            int col = (GRID_SIZE - first.word.length()) / 2;
            placeWord(first.word, row, col, true);
            placements.add(new Placement(first.word, first.hint, row, col, true, clueNumber++));
            usedWords.add(first.word.toUpperCase());
            sorted.remove(0);
        }

        // Бектрекінг для решти слів
        backtrack(sorted, clueNumber);

        // Обрізаємо порожні краї
        int[] bounds = getBounds();
        int minRow = bounds[0], maxRow = bounds[1], minCol = bounds[2], maxCol = bounds[3];
        int height = maxRow - minRow + 1;
        int width = maxCol - minCol + 1;

        char[][] finalGrid = new char[height][width];
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                char ch = grid[minRow + r][minCol + c];
                finalGrid[r][c] = (ch == 0) ? '.' : ch;
            }
        }

        // Формуємо підказки
        List<Crossword.Clue> across = new ArrayList<>();
        List<Crossword.Clue> down = new ArrayList<>();

        for (Placement p : placements) {
            Crossword.Clue clue = new Crossword.Clue(
                    p.number,
                    p.row - minRow,
                    p.col - minCol,
                    p.hint,
                    p.word,
                    p.horizontal
            );
            if (p.horizontal) across.add(clue);
            else down.add(clue);
        }

        // Сортуємо підказки за номером
        across.sort(Comparator.comparingInt(c -> c.number));
        down.sort(Comparator.comparingInt(c -> c.number));

        return new Crossword(width, height, finalGrid, across, down);
    }

    private boolean backtrack(List<MainMenuView.WordEntry> remaining, int nextNumber) {
        if (remaining.isEmpty()) return true;

        MainMenuView.WordEntry wordEntry = remaining.get(0);
        String word = wordEntry.word.toUpperCase();
        if (usedWords.contains(word)) {
            return backtrack(remaining.subList(1, remaining.size()), nextNumber);
        }

        // Спробуємо всі можливі позиції і напрямки
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                for (boolean horizontal : new boolean[]{true, false}) {
                    if (canPlace(word, row, col, horizontal)) {
                        placeWord(word, row, col, horizontal);
                        placements.add(new Placement(word, wordEntry.hint, row, col, horizontal, nextNumber++));
                        usedWords.add(word);

                        if (backtrack(remaining.subList(1, remaining.size()), nextNumber)) {
                            return true;
                        }

                        // Бектрек
                        removeWord(word, row, col, horizontal);
                        placements.remove(placements.size() - 1);
                        usedWords.remove(word);
                        nextNumber--;
                    }
                }
            }
        }
        return false;
    }

    private boolean canPlace(String word, int row, int col, boolean horizontal) {
        if (horizontal) {
            if (col + word.length() > GRID_SIZE) return false;
            for (int i = 0; i < word.length(); i++) {
                char existing = grid[row][col + i];
                if (existing != '.' && existing != word.charAt(i)) return false;
            }
        } else {
            if (row + word.length() > GRID_SIZE) return false;
            for (int i = 0; i < word.length(); i++) {
                char existing = grid[row + i][col];
                if (existing != '.' && existing != word.charAt(i)) return false;
            }
        }
        return hasIntersectionOrBorder(word, row, col, horizontal);
    }

    private void placeWord(String word, int row, int col, boolean horizontal) {
        if (horizontal) {
            for (int i = 0; i < word.length(); i++) {
                grid[row][col + i] = word.charAt(i);
            }
        } else {
            for (int i = 0; i < word.length(); i++) {
                grid[row + i][col] = word.charAt(i);
            }
        }
    }

    private void removeWord(String word, int row, int col, boolean horizontal) {
        if (horizontal) {
            for (int i = 0; i < word.length(); i++) {
                if (grid[row][col + i] == word.charAt(i)) {
                    grid[row][col + i] = '.';
                }
            }
        } else {
            for (int i = 0; i < word.length(); i++) {
                if (grid[row + i][col] == word.charAt(i)) {
                    grid[row + i][col] = '.';
                }
            }
        }
    }

    // Евристика: слово має перетинатися або бути біля краю/іншого слова
    private boolean hasIntersectionOrBorder(String word, int row, int col, boolean horizontal) {
        int intersections = 0;
        if (horizontal) {
            if (col > 0 && grid[row][col - 1] != '.') intersections++;
            if (col + word.length() < GRID_SIZE && grid[row][col + word.length()] != '.') intersections++;
            for (int i = 0; i < word.length(); i++) {
                if (row > 0 && grid[row - 1][col + i] != '.') intersections++;
                if (row < GRID_SIZE - 1 && grid[row + 1][col + i] != '.') intersections++;
            }
        } else {
            if (row > 0 && grid[row - 1][col] != '.') intersections++;
            if (row + word.length() < GRID_SIZE && grid[row + word.length()][col] != '.') intersections++;
            for (int i = 0; i < word.length(); i++) {
                if (col > 0 && grid[row + i][col - 1] != '.') intersections++;
                if (col < GRID_SIZE - 1 && grid[row + i][col + 1] != '.') intersections++;
            }
        }
        return intersections >= 2 || placements.isEmpty(); // перше слово може бути без пересічень
    }

    private int rareLetterScore(String word) {
        return (int) word.chars()
                .filter(ch -> "QZJX".indexOf(ch) >= 0)
                .count();
    }

    private int[] getBounds() {
        int minRow = GRID_SIZE, maxRow = 0, minCol = GRID_SIZE, maxCol = 0;
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (grid[r][c] != '.' && grid[r][c] != 0) {
                    minRow = Math.min(minRow, r);
                    maxRow = Math.max(maxRow, r);
                    minCol = Math.min(minCol, c);
                    maxCol = Math.max(maxCol, c);
                }
            }
        }
        return new int[]{minRow, maxRow, minCol, maxCol};
    }
}