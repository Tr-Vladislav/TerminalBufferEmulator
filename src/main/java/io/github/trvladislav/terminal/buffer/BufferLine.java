package io.github.trvladislav.terminal.buffer;

/**
 * Abstraction for a single line in a terminal buffer.
 * Allows alternative implementations (e.g., sparse lines, wrapped lines).
 */
public interface BufferLine {

    void write(int column, long cellData);

    void insert(int column, long cellData);

    void delete(int column);

    void clear();

    long getCell(int column);

    int getWidth();

    boolean isSoftWrapped();

    void setSoftWrapped(boolean softWrapped);

    void appendTo(StringBuilder sb);
}
