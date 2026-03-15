package io.github.trvladislav.terminal.buffer;

import io.github.trvladislav.terminal.cell.CellUtils;
import io.github.trvladislav.terminal.cursor.Cursor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The main facade that ties together the screen grid, scrollback history,
 * cursor, and current text attributes.
 *
 * Screen rows are indexed 0 (top) to height-1 (bottom).
 * Scrollback rows are indexed 0 (oldest) to scrollbackSize-1 (most recent).
 */
public class TerminalBuffer {

    private int width;
    private int height;
    private final Cursor cursor;
    private BufferLine[] screen;
    private RingBuffer scrollback;

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
        validateColorRange(fg);
        validateColorRange(bg);
        validateStyleRange(styles);
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
        validateColorRange(fg);
        this.currentFg = fg;
    }

    public void resetForeground() {
        this.currentFg = CellUtils.DEFAULT_FG;
    }

    public void setBackground(int bg) {
        validateColorRange(bg);
        this.currentBg = bg;
    }

    public void resetBackground() {
        this.currentBg = CellUtils.DEFAULT_BG;
    }

    public void setStyles(int styles) {
        validateStyleRange(styles);
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
        processCharacters(text, CellOperation.WRITE);
    }

    /**
     * Inserts text at the current cursor position using current attributes.
     * Existing content shifts to the right (characters may fall off the edge).
     * Wraps to the next line when reaching the right edge.
     * Scrolls the screen up when wrapping past the last row.
     */
    public void insertText(String text) {
        processCharacters(text, CellOperation.INSERT);
    }

    /**
     * Fills the current cursor row with the given character using current attributes.
     * Cursor position is not changed.
     * Wide characters step by 2 columns; if the last column has no room, it gets a space.
     */
    public void fillLine(int codePoint) {
        int displayWidth = CellUtils.getDisplayWidth(codePoint);
        BufferLine line = screen[cursor.getRow()];

        if (displayWidth == 2) {
            long cell = CellUtils.encodeWide(codePoint, currentFg, currentBg, currentStyles);
            int col = 0;
            while (col + 1 < width) {
                line.write(col, cell);
                col += 2;
            }
            if (col < width) {
                line.write(col, CellUtils.encode(' ', currentFg, currentBg, currentStyles));
            }
        } else {
            long cell = CellUtils.encode(codePoint, currentFg, currentBg, currentStyles);
            for (int col = 0; col < width; col++) {
                line.write(col, cell);
            }
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
        StringBuilder sb = new StringBuilder(width * height + height);
        for (int r = 0; r < height; r++) {
            if (r > 0) sb.append('\n');
            screen[r].appendTo(sb);
        }
        return sb.toString();
    }

    /**
     * Returns the full content (scrollback + screen) as a multi-line string.
     * Scrollback lines appear first (oldest at the top), then screen lines.
     */
    public String getFullContent() {
        int scrollbackSize = scrollback.size();
        StringBuilder sb = new StringBuilder(width * (scrollbackSize + height) + scrollbackSize + height);
        for (int i = 0; i < scrollbackSize; i++) {
            scrollback.get(i).appendTo(sb);
            sb.append('\n');
        }
        for (int r = 0; r < height; r++) {
            if (r > 0) sb.append('\n');
            screen[r].appendTo(sb);
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

    // ==================== Resize ====================

    /**
     * Resizes the terminal to new dimensions with content reflow.
     *
     * Width change: soft-wrapped lines are merged into logical lines and
     * re-wrapped to the new width. Hard breaks are preserved.
     *
     * Height change: if shorter, top screen lines move to scrollback.
     * If taller, empty lines are added at the bottom.
     *
     * Scrollback content is also reflowed to the new width.
     */
    public void resize(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) {
            throw new IllegalArgumentException(
                    "Screen dimensions must be positive: " + newWidth + "x" + newHeight);
        }
        if (newWidth == width && newHeight == height) return;

        // 1. Collect all physical lines: scrollback + screen
        List<BufferLine> allLines = collectAllLines();

        // 2. Group into logical lines and re-wrap to new width
        List<BufferLine> reflowed = reflow(allLines, newWidth);

        // 3. Distribute into new scrollback and screen
        int scrollbackCapacity = scrollback.capacity();
        RingBuffer newScrollback = new RingBuffer(scrollbackCapacity);
        BufferLine[] newScreen = new BufferLine[newHeight];

        int totalLines = reflowed.size();

        if (totalLines <= newHeight) {
            // All content fits on screen
            int screenStart = 0;
            for (int i = 0; i < totalLines; i++) {
                newScreen[screenStart + i] = reflowed.get(i);
            }
            // Fill remaining with empty lines
            for (int i = totalLines; i < newHeight; i++) {
                newScreen[i] = new Line(newWidth);
            }
        } else {
            // Excess goes to scrollback, last newHeight lines go to screen
            int screenStart = totalLines - newHeight;
            for (int i = 0; i < screenStart; i++) {
                newScrollback.push(reflowed.get(i));
            }
            for (int i = 0; i < newHeight; i++) {
                newScreen[i] = reflowed.get(screenStart + i);
            }
        }

        // 4. Update state
        this.width = newWidth;
        this.height = newHeight;
        this.screen = newScreen;
        this.scrollback = newScrollback;
        cursor.resize(newWidth, newHeight);
    }

    /**
     * Collects all lines in order: scrollback (oldest first), then screen (top to bottom).
     */
    private List<BufferLine> collectAllLines() {
        List<BufferLine> all = new ArrayList<>(scrollback.size() + height);
        all.addAll(scrollback.toList());
        for (int i = 0; i < height; i++) {
            all.add(screen[i]);
        }
        return all;
    }

    /**
     * Groups physical lines into logical lines using softWrapped flag,
     * then re-wraps each logical line to the given width.
     */
    private List<BufferLine> reflow(List<BufferLine> physicalLines, int newWidth) {
        List<BufferLine> result = new ArrayList<>();
        List<long[]> logicalLines = groupLogicalLines(physicalLines);

        for (long[] logicalCells : logicalLines) {
            int contentLength = trimTrailingEmpty(logicalCells);
            result.addAll(wrapLogicalLine(logicalCells, contentLength, newWidth));
        }

        return result;
    }

    /**
     * Merges consecutive soft-wrapped physical lines into logical lines.
     * Each logical line is a single long[] of all cells concatenated.
     */
    private List<long[]> groupLogicalLines(List<BufferLine> physicalLines) {
        List<long[]> logicalLines = new ArrayList<>();
        int i = 0;

        while (i < physicalLines.size()) {
            List<long[]> segments = new ArrayList<>();
            int totalCells = 0;
            boolean merging = true;

            while (merging && i < physicalLines.size()) {
                long[] cells = physicalLines.get(i).getCells();
                segments.add(cells);
                totalCells += cells.length;
                merging = physicalLines.get(i).isSoftWrapped();
                i++;
            }

            long[] merged = new long[totalCells];
            int offset = 0;
            for (long[] segment : segments) {
                System.arraycopy(segment, 0, merged, offset, segment.length);
                offset += segment.length;
            }
            logicalLines.add(merged);
        }

        return logicalLines;
    }

    /**
     * Returns the index of the last non-empty cell + 1.
     */
    private int trimTrailingEmpty(long[] cells) {
        long emptyCell = CellUtils.EMPTY_CELL;
        int length = cells.length;
        while (length > 0 && cells[length - 1] == emptyCell) {
            length--;
        }
        return length;
    }

    /**
     * Wraps a logical line (flat cell array) into physical lines of the given width.
     * Skips continuation cells and rebuilds wide char pairs at new positions.
     * Returns at least one line (empty logical lines produce one empty physical line).
     */
    private List<BufferLine> wrapLogicalLine(long[] logicalCells, int contentLength, int newWidth) {
        List<BufferLine> lines = new ArrayList<>();

        if (contentLength == 0) {
            lines.add(new Line(newWidth));
            return lines;
        }

        long emptyCell = CellUtils.EMPTY_CELL;
        long[] lineCells = createEmptyCells(newWidth);
        int col = 0;

        for (int c = 0; c < contentLength; c++) {
            long cell = logicalCells[c];
            if (CellUtils.isWideContinuation(cell)) continue;

            int cellWidth = CellUtils.isWide(cell) ? 2 : 1;

            // Wide char at last column — pad with space and soft-wrap
            if (cellWidth == 2 && col == newWidth - 1) {
                lineCells[col] = CellUtils.encode(' ',
                        CellUtils.getForegroundColor(cell),
                        CellUtils.getBackgroundColor(cell),
                        CellUtils.getStyles(cell));
                lines.add(new Line(lineCells, true));
                lineCells = createEmptyCells(newWidth);
                col = 0;
            }

            // Place the cell
            lineCells[col] = cell;
            if (cellWidth == 2 && col + 1 < newWidth) {
                lineCells[col + 1] = CellUtils.createWideContinuation(
                        CellUtils.getForegroundColor(cell),
                        CellUtils.getBackgroundColor(cell),
                        CellUtils.getStyles(cell));
            }
            col += cellWidth;

            // Line full and more content ahead — soft-wrap
            if (col >= newWidth && hasMoreContent(logicalCells, c + 1, contentLength)) {
                lines.add(new Line(lineCells, true));
                lineCells = createEmptyCells(newWidth);
                col = 0;
            }
        }

        // Last line — hard break
        lines.add(new Line(lineCells, false));
        return lines;
    }

    /**
     * Returns true if there are non-continuation cells remaining after fromIndex.
     */
    private boolean hasMoreContent(long[] cells, int fromIndex, int contentLength) {
        for (int i = fromIndex; i < contentLength; i++) {
            if (!CellUtils.isWideContinuation(cells[i])) return true;
        }
        return false;
    }

    private long[] createEmptyCells(int width) {
        long[] cells = new long[width];
        Arrays.fill(cells, CellUtils.EMPTY_CELL);
        return cells;
    }

    // ==================== Internal ====================

    private enum CellOperation { WRITE, INSERT }

    /**
     * Processes each character in the text, encoding it with current attributes
     * and applying the given operation (write or insert) to the current line.
     * Handles wide characters, wrapping, and scrolling.
     */
    private void processCharacters(String text, CellOperation operation) {
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            int displayWidth = CellUtils.getDisplayWidth(codePoint);

            if (displayWidth == 2) {
                // Wide char needs 2 cells — if at last column, pad and wrap first
                if (cursor.getColumn() == width - 1) {
                    screen[cursor.getRow()].write(cursor.getColumn(),
                            CellUtils.encode(' ', currentFg, currentBg, currentStyles));
                    advanceCursor(1);
                }
                long cell = CellUtils.encodeWide(codePoint, currentFg, currentBg, currentStyles);
                applyCell(cell, operation);
                advanceCursor(2);
            } else {
                long cell = CellUtils.encode(codePoint, currentFg, currentBg, currentStyles);
                applyCell(cell, operation);
                advanceCursor(1);
            }

            i += Character.charCount(codePoint);
        }
    }

    private void applyCell(long cell, CellOperation operation) {
        BufferLine line = screen[cursor.getRow()];
        int col = cursor.getColumn();
        switch (operation) {
            case WRITE -> line.write(col, cell);
            case INSERT -> line.insert(col, cell);
        }
    }

    /**
     * Advances the cursor by n positions: moves right within the line,
     * wraps to the next line at the right edge, scrolls up at the bottom.
     * Uses arithmetic instead of per-step looping.
     */
    private void advanceCursor(int n) {
        int col = cursor.getColumn();
        int row = cursor.getRow();
        int total = col + n;

        if (total < width) {
            // Stays on the same line — no wrapping
            cursor.setPosition(total, row);
            return;
        }

        // Mark current line as soft-wrapped
        screen[row].setSoftWrapped(true);

        // How many full lines we advance past the current one
        // (subtract remaining cells on current line, then divide)
        int remaining = total - width;
        int lineWraps = 1 + remaining / width;
        int newCol = remaining % width;

        // Mark intermediate lines as soft-wrapped and scroll if needed
        for (int w = 1; w < lineWraps; w++) {
            if (row < height - 1) {
                row++;
            } else {
                scrollUp();
            }
            screen[row].setSoftWrapped(true);
        }

        // Final wrap — move to the next line
        if (row < height - 1) {
            row++;
        } else {
            scrollUp();
        }

        cursor.setPosition(newCol, row);
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

    private void validateColorRange(int color) {
        if (color < 0 || color > 255) {
            throw new IllegalArgumentException(
                    "Color must be 0-255, got: " + color);
        }
    }

    private void validateStyleRange(int styles) {
        if (styles < 0 || styles > 7) {
            throw new IllegalArgumentException(
                    "Styles must be 0-7, got: " + styles);
        }
    }

    private void checkScreenBounds(int column, int row) {
        if (column < 0 || column >= width || row < 0 || row >= height) {
            throw new IndexOutOfBoundsException(
                    "Position (" + column + ", " + row + ") out of screen bounds " + width + "x" + height);
        }
    }
}
