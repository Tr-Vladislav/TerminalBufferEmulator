package io.github.trvladislav.terminal.core;

import java.util.Arrays;

public class Line {
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
        if (column >= 0 && column < width) {
            cells[column] = cellData;
        }
    }

    /**
     * Inserts a cell at the given position, shifting others to the right.
     * The last character "falls off" the edge.
     */
    public void insert(int column, long cellData) {
        if (column < 0 || column >= width) return;

        for (int i = width - 1; i > column; i--) {
            cells[i] = cells[i - 1];
        }
        cells[column] = cellData;
    }

    public long getCell(int column) {
        return (column >= 0 && column < width) ? cells[column] : CellUtils.createEmpty();
    }

    public int getWidth() {
        return width;
    }
}

