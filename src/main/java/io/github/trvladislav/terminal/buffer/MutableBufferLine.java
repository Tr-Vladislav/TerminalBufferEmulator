package io.github.trvladislav.terminal.buffer;

/**
 * Mutable extension of {@link BufferLine}.
 * Used internally by {@link TerminalBuffer} for screen lines that can be edited.
 */
public interface MutableBufferLine extends BufferLine {

    void write(int column, long cellData);

    void insert(int column, long cellData);

    void delete(int column);

    void clear();

    void setSoftWrapped(boolean softWrapped);
}
