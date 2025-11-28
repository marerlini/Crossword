// src/main/java/org/example/model/Crossword.java
package org.example.model;

import java.util.List;

public class Crossword {
    private final int width;
    private final int height;
    private final char[][] grid; // '.' = чорна клітинка, літера = біла
    private final List<Clue> acrossClues;
    private final List<Clue> downClues;

    public Crossword(int width, int height, char[][] grid,
                     List<Clue> acrossClues, List<Clue> downClues) {
        this.width = width;
        this.height = height;
        this.grid = grid;
        this.acrossClues = acrossClues;
        this.downClues = downClues;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public char getCell(int row, int col) { return grid[row][col]; }
    public List<Clue> getAcrossClues() { return acrossClues; }
    public List<Clue> getDownClues() { return downClues; }

    public static class Clue {
        public final int number;
        public final int row;
        public final int col;
        public final String text;
        public final String answer;
        public final boolean isAcross; // true = по горизонталі, false = по вертикалі

        public Clue(int number, int row, int col, String text, String answer, boolean isAcross) {
            this.number = number;
            this.row = row;
            this.col = col;
            this.text = text;
            this.answer = answer;
            this.isAcross = isAcross;
        }
    }
}