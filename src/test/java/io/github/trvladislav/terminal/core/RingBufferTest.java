package io.github.trvladislav.terminal.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RingBufferTest {

    private final int CAPACITY = 3;
    private RingBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = new RingBuffer(CAPACITY);
    }

    // Helper method to create a recognizable Line for testing
    private Line createLine(char idChar) {
        Line line = new Line(5); // Arbitrary small width
        line.write(0, CellUtils.encode(idChar, 7, 0, 0));
        return line;
    }

    // Helper method to extract the ID character from a Line
    private char getLineId(Line line) {
        return (char) CellUtils.getCharacter(line.getCell(0));
    }

    @Test
    void testInitialization() {
        assertEquals(0, buffer.size());
        assertEquals(CAPACITY, buffer.capacity());
        assertFalse(buffer.isFull());
    }

    @Test
    void testInvalidCapacityThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new RingBuffer(0));
        assertThrows(IllegalArgumentException.class, () -> new RingBuffer(-5));
    }

    @Test
    void testPushAndGetWithoutWrap() {
        buffer.push(createLine('A'));
        buffer.push(createLine('B'));

        assertEquals(2, buffer.size());
        assertFalse(buffer.isFull());

        // Logical index 0 is the oldest ('A')
        assertEquals('A', getLineId(buffer.get(0)));
        assertEquals('B', getLineId(buffer.get(1)));
    }

    @Test
    void testWrapAroundLogic() {
        // Fill the buffer to capacity
        buffer.push(createLine('A'));
        buffer.push(createLine('B'));
        buffer.push(createLine('C'));

        assertTrue(buffer.isFull());
        assertEquals(CAPACITY, buffer.size());

        // Push one more element to force a wrap-around
        buffer.push(createLine('D'));

        // Size should still be at capacity
        assertEquals(CAPACITY, buffer.size());
        assertTrue(buffer.isFull());

        // 'A' should be overwritten. 'B' is now the oldest (logical index 0)
        assertEquals('B', getLineId(buffer.get(0)));
        assertEquals('C', getLineId(buffer.get(1)));
        assertEquals('D', getLineId(buffer.get(2))); // 'D' is the newest
    }

    @Test
    void testMultipleWrapArounds() {
        // Push 8 elements into a buffer of size 3
        char[] inputs = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'};
        for (char c : inputs) {
            buffer.push(createLine(c));
        }

        assertEquals(CAPACITY, buffer.size());

        // The last 3 elements pushed were F, G, H
        assertEquals('F', getLineId(buffer.get(0)));
        assertEquals('G', getLineId(buffer.get(1)));
        assertEquals('H', getLineId(buffer.get(2)));
    }

    @Test
    void testGetOutOfBoundsThrowsException() {
        buffer.push(createLine('A'));

        assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(1)); // Size is 1, max index is 0
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(10));
    }
}