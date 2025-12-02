package org.example.model;

import org.example.view.MainMenuView;
import java.util.*;

public class CrosswordGenerator {

    private int GRID_SIZE;
    private static final int MIN_GRID_SIZE = 10;
    private static final int MAX_GRID_SIZE = 100;

    private char[][] grid;
    private final List<MainMenuView.WordEntry> allWords;// весь словник
    private final int targetWordCount;
    private final List<Placement> placements = new ArrayList<>();
    private record Position(int row, int col, boolean horizontal, int intersections) {}
    private record Placement(String word, String hint, int row, int col, boolean horizontal, int number) {}

    public static Crossword generate(List<MainMenuView.WordEntry> wordEntries, int maxWords) {
        return new CrosswordGenerator(wordEntries, maxWords).generate();
    }

    private CrosswordGenerator(List<MainMenuView.WordEntry> allWordsFromDb, int maxWords) {
        this.allWords = new ArrayList<>(allWordsFromDb);
        this.targetWordCount = maxWords;

        // Сітка під найдовше слово + запас
        int maxWordLength = allWords.stream()
                .mapToInt(e -> e.word.length())
                .max()
                .orElse(10);

        this.GRID_SIZE = Math.max(MIN_GRID_SIZE, Math.min(maxWordLength + 5, MAX_GRID_SIZE));

        this.grid = new char[GRID_SIZE][GRID_SIZE];
        for (char[] row : grid) Arrays.fill(row, '.');
    }

    private Crossword generate() {
        // Сортуємо весь словник (найдовші + рідкісні букви — першими)
        List<MainMenuView.WordEntry> sorted = new ArrayList<>(allWords);
        sorted.sort((a, b) -> {
            int lenDiff = b.word.length() - a.word.length();
            if (lenDiff != 0) return lenDiff;
            return Integer.compare(rareLetterScore(b.word), rareLetterScore(a.word));
        });

        int clueNumber = 1;

        // Розміщуємо перше слово по центру
        if (!sorted.isEmpty()) {
            MainMenuView.WordEntry first = sorted.get(0);
            String word = first.word.toUpperCase();

            expandGridIfNeeded(word.length());

            int row = GRID_SIZE / 2;
            int col = (GRID_SIZE - word.length()) / 2;

            placeWord(word, row, col, true);
            placements.add(new Placement(word, first.hint, row, col, true, clueNumber++));

            // Видаляємо використане слово зі списку
            sorted.remove(0);
        }

        // бектрекінг
        Deque<MainMenuView.WordEntry> candidates = new ArrayDeque<>(sorted);
        Deque<MainMenuView.WordEntry> backup = new ArrayDeque<>();

        backtrack(candidates, backup, clueNumber);

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

    private int rareLetterScore(String word) {
        return (int) word.chars()
                .filter(ch -> "QZJXҐЇЄЩЮЯЖЦФХШ".indexOf(ch) >= 0)
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

    private boolean backtrack(Deque<MainMenuView.WordEntry> candidates,
                              Deque<MainMenuView.WordEntry> backup,
                              int clueNumber) {

        if (placements.size() >= targetWordCount) {
            return true;
        }

        // Якщо кандидатів немає — перекидаємо з резерву
        if (candidates.isEmpty() && !backup.isEmpty()) {
            candidates.addAll(backup);
            backup.clear();
        }

        if (candidates.isEmpty()) {
            return placements.size() >= targetWordCount;
        }

        MainMenuView.WordEntry entry = candidates.removeFirst();
        String word = entry.word.toUpperCase();

        expandGridIfNeeded(word.length());

        // Збираємо найкращі позиції (максимумом перетинів)
        List<Position> positions = new ArrayList<>();
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (canPlace(word, r, c, true)) {
                    int inter = countIntersections(word, r, c, true);
                    if (inter > 0 || placements.isEmpty()) {
                        positions.add(new Position(r, c, true, inter));
                    }
                }
                if (canPlace(word, r, c, false)) {
                    int inter = countIntersections(word, r, c, false);
                    if (inter > 0 || placements.isEmpty()) {
                        positions.add(new Position(r, c, false, inter));
                    }
                }
            }
        }

        positions.sort((a, b) -> Integer.compare(b.intersections, a.intersections));

        for (Position pos : positions) {
            placeWord(word, pos.row, pos.col, pos.horizontal);
            placements.add(new Placement(word, entry.hint, pos.row, pos.col, pos.horizontal, clueNumber));

            if (backtrack(candidates, backup, clueNumber + 1)) {
                return true;
            }

            // Бектрек
            removeWord(word, pos.row, pos.col, pos.horizontal);
            placements.remove(placements.size() - 1);
        }

        // Якщо не влізло — кидаємо в резерв
        backup.addLast(entry);

        candidates.addFirst(entry);

        return false;
    }
    // Перевіряє, чи можна розмістити слово в позиції
    private boolean canPlace(String word, int row, int col, boolean horizontal) {
        if (horizontal) {
            if (col + word.length() > GRID_SIZE) return false;

            // Перевірка кожної клітинки слова
            for (int i = 0; i < word.length(); i++) {
                char existing = grid[row][col + i];
                if (existing != '.' && existing != 0 && existing != word.charAt(i)) {
                    return false;
                }
            }

            // Перед першим символом (col-1) — має бути '.' або вихід за межі
            if (col > 0 && grid[row][col - 1] != '.') {
                return false;
            }

            // Після останнього символу (col + len) — має бути '.' або вихід за межі
            int lastCol = col + word.length();
            if (lastCol < GRID_SIZE && grid[row][lastCol] != '.') {
                return false;
            }

        } else { // vertical
            if (row + word.length() > GRID_SIZE) return false;

            for (int i = 0; i < word.length(); i++) {
                char existing = grid[row + i][col];
                if (existing != '.' && existing != 0 && existing != word.charAt(i)) {
                    return false;
                }
            }

            // Перед першим символом (row-1, той самий стовпець)
            if (row > 0 && grid[row - 1][col] != '.') {
                return false;
            }

            // Після останнього символу (row + len, той самий стовпець)
            int lastRow = row + word.length();
            if (lastRow < GRID_SIZE && grid[lastRow][col] != '.') {
                return false;
            }
        }
        return true;
    }

    // Рахує кількість перетинів
    private int countIntersections(String word, int row, int col, boolean horizontal) {
        int count = 0;
        if (horizontal) {
            for (int i = 0; i < word.length(); i++) {
                char ch = grid[row][col + i];
                if (ch != '.' && ch != 0 && ch == word.charAt(i)) {
                    count++;
                }
            }
        } else {
            for (int i = 0; i < word.length(); i++) {
                char ch = grid[row + i][col];
                if (ch != '.' && ch != 0 && ch == word.charAt(i)) {
                    count++;
                }
            }
        }
        return count;
    }

    // Видаляємо слово
    private void removeWord(String word, int row, int col, boolean horizontal) {
        if (horizontal) {
            for (int i = 0; i < word.length(); i++) {
                char current = grid[row][col + i];
                // Якщо ця буква належить тільки цьому слову — стираємо
                if (current == word.charAt(i)) {
                    // Перевіряємо, чи ця клітинка не є частиною іншого слова
                    if (NotPartOfOtherWord(row, col + i, word)) {
                        grid[row][col + i] = '.';
                    }
                }
            }
        } else {
            for (int i = 0; i < word.length(); i++) {
                char current = grid[row + i][col];
                if (current == word.charAt(i)) {
                    if (NotPartOfOtherWord(row + i, col, word)) {
                        grid[row + i][col] = '.';
                    }
                }
            }
        }
    }

    private boolean NotPartOfOtherWord(int r, int c, String excludedWord) {
        char ch = grid[r][c];
        if (ch == '.' || ch == 0) return true;

        for (Placement p : placements) {
            if (p.word.equals(excludedWord)) continue;

            if (p.horizontal) {
                if (p.row == r && c >= p.col && c < p.col + p.word.length()) {
                    if (p.word.charAt(c - p.col) == ch) return false;
                }
            } else {
                if (p.col == c && r >= p.row && r < p.row + p.word.length()) {
                    if (p.word.charAt(r - p.row) == ch) return false;
                }
            }
        }
        return true;
    }

    private void expandGridIfNeeded(int requiredSize) {
        if (requiredSize <= GRID_SIZE) return;

        int desired = requiredSize + 15;
        int newSize = Math.min(MAX_GRID_SIZE, Math.max(GRID_SIZE + 20, desired));

        if (newSize >= MAX_GRID_SIZE) {
            // Якщо дійсно не влізає — кидаємо зрозумілу помилку
            throw new IllegalStateException("Максимальний розмір сітки: " + MAX_GRID_SIZE + "×" + MAX_GRID_SIZE +
                    ". При генерації кросворд перевищив ці значення, спробуйте ввести меншу кількість слів та/або використати коротші слова");
        }
        char[][] newGrid = new char[newSize][newSize];
        for (char[] row : newGrid) Arrays.fill(row, '.');

        int offsetR = (newSize - GRID_SIZE) / 2;
        int offsetC = (newSize - GRID_SIZE) / 2;

        // Копіюємо стару сітку
        for (int r = 0; r < GRID_SIZE; r++) {
            System.arraycopy(grid[r], 0, newGrid[offsetR + r], offsetC, GRID_SIZE);
        }

        // Оновлюємо placements
        List<Placement> updated = new ArrayList<>();
        for (Placement p : placements) {
            updated.add(new Placement(
                    p.word, p.hint,
                    p.row + offsetR,
                    p.col + offsetC,
                    p.horizontal,
                    p.number
            ));
        }
        placements.clear();
        placements.addAll(updated);

        this.grid = newGrid;
        this.GRID_SIZE = newSize;
    }
}