package io.github.trvladislav.terminal.buffer;

import io.github.trvladislav.terminal.cell.CellUtils;
import io.github.trvladislav.terminal.cursor.Cursor;

/**
 * The main facade that ties together the screen grid, scrollback history,
 * cursor, and current text attributes.
 *
 * Screen rows are indexed 0 (top) to height-1 (bottom).
 * Scrollback rows are indexed 0 (oldest) to scrollbackSize-1 (most recent).
 */
public class TerminalBuffer {

    private final int width;
    private final int height;
    private final Cursor cursor;
    private final BufferLine[] screen;
    private final RingBuffer scrollback;

    // Current text attributes applied to every subsequent write/insert
    private int currentFg;
    private int currentBg;
    private int currentStyles;

    public TerminalBuffer(int width, int height, int maxScrollback) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Screen dimensions must be positive: " + width + "x" + height);
        }
        if (maxScrollback < 0) {
            throw new IllegalArgumentException(
                    "Scrollback size must be non-negative: " + maxScrollback);
        }

        this.width = width;
        this.height = height;
        this.cursor = new Cursor(width, height);
        this.screen = new BufferLine[height];
        this.scrollback = new RingBuffer(maxScrollback);

        for (int i = 0; i < height; i++) {
            screen[i] = new Line(width);
        }

        this.currentFg = CellUtils.DEFAULT_FG;
        this.currentBg = CellUtils.DEFAULT_BG;
        this.currentStyles = CellUtils.DEFAULT_STYLES;
    }

    // ==================== Attributes ====================

    public void setAttributes(int fg, int bg, int styles) {
        this.currentFg = fg;
        this.currentBg = bg;
        this.currentStyles = styles;
    }

    /**
     * Resets foreground, background and styles to their defaults.
     */
    public void resetAttributes() {
        this.currentFg = CellUtils.DEFAULT_FG;
        this.currentBg = CellUtils.DEFAULT_BG;
        this.currentStyles = CellUtils.DEFAULT_STYLES;
    }

    public void setForeground(int fg) {
        this.currentFg = fg;
    }

    public void resetForeground() {
        this.currentFg = CellUtils.DEFAULT_FG;
    }

    public void setBackground(int bg) {
        this.currentBg = bg;
    }

    public void resetBackground() {
        this.currentBg = CellUtils.DEFAULT_BG;
    }

    public void setStyles(int styles) {
        this.currentStyles = styles;
    }

    public void resetStyles() {
        this.currentStyles = CellUtils.DEFAULT_STYLES;
    }

    public int getCurrentFg() {
        return currentFg;
    }

    public int getCurrentBg() {
        return currentBg;
    }

    public int getCurrentStyles() {
        return currentStyles;
    }

    // ==================== Cursor ====================

    public int getCursorColumn() {
        return cursor.getColumn();
    }

    public int getCursorRow() {
        return cursor.getRow();
    }

    public void setCursorPosition(int column, int row) {
        cursor.setPosition(column, row);
    }

    public void moveCursorUp(int n) {
        cursor.moveUp(n);
    }

    public void moveCursorDown(int n) {
        cursor.moveDown(n);
    }

    public void moveCursorLeft(int n) {
        cursor.moveLeft(n);
    }

    public void moveCursorRight(int n) {
        cursor.moveRight(n);
    }

    // ==================== Editing (cursor + attributes aware) ====================

    /**
     * Writes text at the current cursor position using current attributes.
     * Overwrites existing content. Advances the cursor.
     * Wraps to the next line when reaching the right edge.
     * Scrolls the screen up when wrapping past the last row.
     */
    public void writeText(String text) {
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            long cell = CellUtils.encode(codePoint, currentFg, currentBg, currentStyles);

            screen[cursor.getRow()].write(cursor.getColumn(), cell);
            advanceCursor();

            i += Character.charCount(codePoint);
        }
    }

    /**
     * Inserts text at the current cursor position using current attributes.
     * Existing content shifts to the right (characters may fall off the edge).
     * Wraps to the next line when reaching the right edge.
     * Scrolls the screen up when wrapping past the last row.
     */
    public void insertText(String text) {
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            long cell = CellUtils.encode(codePoint, currentFg, currentBg, currentStyles);

            screen[cursor.getRow()].insert(cursor.getColumn(), cell);
            advanceCursor();

            i += Character.charCount(codePoint);
        }
    }

    /**
     * Fills the current cursor row with the given character using current attributes.
     * Cursor position is not changed.
     */
    public void fillLine(int codePoint) {
        long cell = CellUtils.encode(codePoint, currentFg, currentBg, currentStyles);
        BufferLine line = screen[cursor.getRow()];
        for (int col = 0; col < width; col++) {
            line.write(col, cell);
        }
    }

    /**
     * Fills the current cursor row with empty cells (space, default colors, no styles).
     * Cursor position is not changed.
     */
    public void fillLineEmpty() {
        screen[cursor.getRow()].clear();
    }

    // ==================== Editing (cursor-independent) ====================

    /**
     * Scrolls the screen up by one line: the top screen line moves into
     * scrollback, all other lines shift up, and a new empty line appears
     * at the bottom.
     */
    public void insertLineAtBottom() {
        scrollUp();
    }

    /**
     * Clears the entire screen (all lines become empty).
     * Cursor position is reset to (0, 0).
     */
    public void clearScreen() {
        for (BufferLine line : screen) {
            line.clear();
        }
        cursor.setPosition(0, 0);
    }

    /**
     * Clears both the screen and the scrollback history.
     * Cursor position is reset to (0, 0).
     */
    public void clearAll() {
        clearScreen();
        scrollback.clear();
    }

    // ==================== Content Access ====================

    /**
     * Returns the character code point at a screen position.
     */
    public int getCharacterAt(int column, int row) {
        return CellUtils.getCharacter(getScreenCell(column, row));
    }

    /**
     * Returns the packed cell (character + attributes) at a screen position.
     */
    public long getScreenCell(int column, int row) {
        checkScreenBounds(column, row);
        return screen[row].getCell(column);
    }

    /**
     * Returns the packed cell from the scrollback at the given position.
     * Row 0 is the oldest scrollback line.
     */
    public long getScrollbackCell(int column, int scrollbackRow) {
        return scrollback.get(scrollbackRow).getCell(column);
    }

    /**
     * Returns a screen line as a string.
     */
    public String getScreenLineAsString(int row) {
        if (row < 0 || row >= height) {
            throw new IndexOutOfBoundsException("Row: " + row + ", Height: " + height);
        }
        return screen[row].toString();
    }

    /**
     * Returns a scrollback line as a string.
     * Row 0 is the oldest line in the scrollback.
     */
    public String getScrollbackLineAsString(int scrollbackRow) {
        return scrollback.get(scrollbackRow).toString();
    }

    /**
     * Returns the entire visible screen as a multi-line string.
     */
    public String getScreenContent() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < height; r++) {
            if (r > 0) sb.append('\n');
            sb.append(screen[r].toString());
        }
        return sb.toString();
    }

    /**
     * Returns the full content (scrollback + screen) as a multi-line string.
     * Scrollback lines appear first (oldest at the top), then screen lines.
     */
    public String getFullContent() {
        StringBuilder sb = new StringBuilder();
        int scrollbackSize = scrollback.size();
        for (int i = 0; i < scrollbackSize; i++) {
            sb.append(scrollback.get(i).toString());
            sb.append('\n');
        }
        for (int r = 0; r < height; r++) {
            if (r > 0) sb.append('\n');
            sb.append(screen[r].toString());
        }
        return sb.toString();
    }

    // ==================== Getters ====================

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getScrollbackSize() {
        return scrollback.size();
    }

    public int getScrollbackCapacity() {
        return scrollback.capacity();
    }

    // ==================== Internal ====================

    /**
     * Advances the cursor by one position: moves right within the line,
     * wraps to the next line at the right edge, or scrolls up at the bottom.
     */
    private void advanceCursor() {
        if (cursor.getColumn() < width - 1) {
            cursor.setPosition(cursor.getColumn() + 1, cursor.getRow());
        } else if (cursor.getRow() < height - 1) {
            cursor.setPosition(0, cursor.getRow() + 1);
        } else {
            scrollUp();
            cursor.setPosition(0, cursor.getRow());
        }
    }

    /**
     * Pushes the top screen line into scrollback, shifts all screen lines up,
     * and places a new empty line at the bottom.
     */
    private void scrollUp() {
        scrollback.push(screen[0]);
        System.arraycopy(screen, 1, screen, 0, height - 1);
        screen[height - 1] = new Line(width);
    }

    private void checkScreenBounds(int column, int row) {
        if (column < 0 || column >= width || row < 0 || row >= height) {
            throw new IndexOutOfBoundsException(
                    "Position (" + column + ", " + row + ") out of screen bounds " + width + "x" + height);
        }
    }
}
