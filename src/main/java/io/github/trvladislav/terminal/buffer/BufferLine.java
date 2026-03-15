package io.github.trvladislav.terminal.buffer;

/**
 * Read-only view of a single line in a terminal buffer.
 * Returned from content-access APIs (screen and scrollback).
 */
public interface BufferLine {

    long getCell(int column);

    long[] getCells();

    int getWidth();

    boolean isSoftWrapped();

    void appendTo(StringBuilder sb);
}
