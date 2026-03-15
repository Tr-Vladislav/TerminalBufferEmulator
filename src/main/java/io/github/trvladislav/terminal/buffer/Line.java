package io.github.trvladislav.terminal.buffer;

import io.github.trvladislav.terminal.cell.CellUtils;

import java.util.Arrays;

public class Line implements BufferLine {
    private final long[] cells;
    private final int width;
    private boolean softWrapped;

    Line(int width) {
        this.width = width;
        this.cells = new long[width];
        this.softWrapped = false;
        Arrays.fill(cells, CellUtils.createEmpty());
    }

    Line(long[] cells, boolean softWrapped) {
        this.width = cells.length;
        this.cells = cells;
        this.softWrapped = softWrapped;
    }

    /**
     * Overwrites content at the given position.
     * If cellData is wide and there is room for 2 cells, writes both.
     * If cellData is wide and there is no room, does nothing.
     * Cleans up orphaned wide halves at affected positions.
     */
    public void write(int column, long cellData) {
        checkBounds(column);

        if (CellUtils.isWide(cellData)) {
            if (column + 1 >= width) return;

            clearOrphanedWide(column);
            clearOrphanedWide(column + 1);
            cells[column] = cellData;
            cells[column + 1] = CellUtils.createWideContinuation(
                    CellUtils.getForegroundColor(cellData),
                    CellUtils.getBackgroundColor(cellData),
                    CellUtils.getStyles(cellData));
        } else {
            clearOrphanedWide(column);
            cells[column] = cellData;
        }
    }

    /**
     * Inserts a cell at the given position, shifting existing content right.
     * The last cell(s) fall off the edge.
     * If cellData is wide and there is no room for 2 cells, does nothing.
     */
    public void insert(int column, long cellData) {
        checkBounds(column);

        if (CellUtils.isWide(cellData)) {
            if (column + 1 >= width) return;

            shiftRight(column, 2);
            cells[column] = cellData;
            cells[column + 1] = CellUtils.createWideContinuation(
                    CellUtils.getForegroundColor(cellData),
                    CellUtils.getBackgroundColor(cellData),
                    CellUtils.getStyles(cellData));
        } else {
            shiftRight(column, 1);
            cells[column] = cellData;
        }
        cleanupEdge();
    }

    /**
     * Deletes a cell at the given position, shifting others left.
     * If deleting one half of a wide pair, erases the other half too.
     */
    public void delete(int column) {
        checkBounds(column);

        if (CellUtils.isWide(cells[column]) && column + 1 < width) {
            shiftLeft(column, 2);
        } else if (CellUtils.isWideContinuation(cells[column]) && column > 0) {
            shiftLeft(column - 1, 2);
        } else {
            shiftLeft(column, 1);
        }
    }

    /**
     * Resets all cells to empty (space with default colors).
     */
    public void clear() {
        Arrays.fill(cells, CellUtils.createEmpty());
        this.softWrapped = false;
    }

    public long getCell(int column) {
        checkBounds(column);
        return cells[column];
    }

    public long[] getCells() {
        return Arrays.copyOf(cells, width);
    }

    public int getWidth() {
        return width;
    }

    public boolean isSoftWrapped() {
        return softWrapped;
    }

    public void setSoftWrapped(boolean softWrapped) {
        this.softWrapped = softWrapped;
    }

    @Override
    public void appendTo(StringBuilder sb) {
        for (long cell : cells) {
            if (CellUtils.isWideContinuation(cell)) continue;
            sb.appendCodePoint(CellUtils.getCharacter(cell));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(width);
        appendTo(sb);
        return sb.toString();
    }

    /**
     * Shifts cells right by count starting at column. Last cells fall off.
     */
    private void shiftRight(int column, int count) {
        System.arraycopy(cells, column, cells, column + count, width - column - count);
    }

    /**
     * Shifts cells left by count starting at column. Empty cells fill the end.
     */
    private void shiftLeft(int column, int count) {
        System.arraycopy(cells, column + count, cells, column, width - column - count);
        for (int i = width - count; i < width; i++) {
            cells[i] = CellUtils.createEmpty();
        }
    }

    /**
     * If column is part of a wide pair, erases the orphaned half.
     */
    private void clearOrphanedWide(int column) {
        long cell = cells[column];
        if (CellUtils.isWide(cell) && column + 1 < width) {
            cells[column + 1] = CellUtils.createEmpty();
        } else if (CellUtils.isWideContinuation(cell) && column > 0) {
            cells[column - 1] = CellUtils.createEmpty();
        }
    }

    /**
     * Cleans up wide character corruption after a right shift.
     * - If a wide left-half landed in the last column (no room for continuation), erase it.
     * - If an orphaned continuation exists at the last column (its left-half fell off), erase it.
     */
    private void cleanupEdge() {
        if (CellUtils.isWide(cells[width - 1])) {
            cells[width - 1] = CellUtils.createEmpty();
        }
        if (CellUtils.isWideContinuation(cells[width - 1])
                && (width < 2 || !CellUtils.isWide(cells[width - 2]))) {
            cells[width - 1] = CellUtils.createEmpty();
        }
    }

    private void checkBounds(int column) {
        if (column < 0 || column >= width) {
            throw new IndexOutOfBoundsException("Column: " + column + ", Width: " + width);
        }
    }
}
