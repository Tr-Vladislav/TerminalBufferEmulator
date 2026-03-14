package io.github.trvladislav.terminal.core;

/**
 * Tracks the cursor position within the visible screen area.
 * The cursor is clamped to screen bounds on every operation.
 * Column and row are 0-based.
 */
public class Cursor {

    private int column;
    private int row;
    private final int screenWidth;
    private final int screenHeight;

    public Cursor(int screenWidth, int screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            throw new IllegalArgumentException(
                    "Screen dimensions must be positive: " + screenWidth + "x" + screenHeight);
        }
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.column = 0;
        this.row = 0;
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    /**
     * Sets the cursor to an absolute position, clamped to screen bounds.
     */
    public void setPosition(int column, int row) {
        this.column = clampColumn(column);
        this.row = clampRow(row);
    }

    public void moveUp(int n) {
        row = clampRow(row - n);
    }

    public void moveDown(int n) {
        row = clampRow(row + n);
    }

    public void moveLeft(int n) {
        column = clampColumn(column - n);
    }

    public void moveRight(int n) {
        column = clampColumn(column + n);
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    private int clampColumn(int col) {
        return Math.max(0, Math.min(col, screenWidth - 1));
    }

    private int clampRow(int r) {
        return Math.max(0, Math.min(r, screenHeight - 1));
    }
}
