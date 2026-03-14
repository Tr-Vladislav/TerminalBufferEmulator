package io.github.trvladislav.terminal.core;

import java.util.Arrays;

public class Line implements BufferLine {
    private final long[] cells;
    private final int width;

    public Line(int width) {
        this.width = width;
        this.cells = new long[width];
        Arrays.fill(cells, CellUtils.createEmpty());
    }

    /**
     * Overwrites content at the given position.
     */
    public void write(int column, long cellData) {
        checkBounds(column);
        cells[column] = cellData;
    }

    /**
     * Inserts a cell at the given position, shifting others to the right.
     * The last character "falls off" the edge.
     */
    public void insert(int column, long cellData) {
        checkBounds(column);
        System.arraycopy(cells, column, cells, column + 1, width - 1 - column);
        cells[column] = cellData;
    }

    /**
     * Deletes a cell at the given position, shifting others to the left.
     * An empty cell is placed at the end.
     */
    public void delete(int column) {
        checkBounds(column);
        System.arraycopy(cells, column + 1, cells, column, width - 1 - column);
        cells[width - 1] = CellUtils.createEmpty();
    }

    /**
     * Resets all cells to empty (space with default colors).
     */
    public void clear() {
        Arrays.fill(cells, CellUtils.createEmpty());
    }

    public long getCell(int column) {
        checkBounds(column);
        return cells[column];
    }

    public int getWidth() {
        return width;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(width);
        for (long cell : cells) {
            sb.appendCodePoint(CellUtils.getCharacter(cell));
        }
        return sb.toString();
    }

    private void checkBounds(int column) {
        if (column < 0 || column >= width) {
            throw new IndexOutOfBoundsException("Column: " + column + ", Width: " + width);
        }
    }
}
