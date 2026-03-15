package io.github.trvladislav.terminal.buffer;

import io.github.trvladislav.terminal.cell.CellUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RingBufferTest {

    private static final int CAPACITY = 3;
    private RingBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = new RingBuffer(CAPACITY);
    }

    private Line createLine(char idChar) {
        Line line = new Line(5);
        line.write(0, CellUtils.encode(idChar, 7, 0, 0));
        return line;
    }

    private char getLineId(BufferLine line) {
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
        assertThrows(IllegalArgumentException.class, () -> new RingBuffer(-5));
    }

    @Test
    void testZeroCapacity() {
        RingBuffer zero = new RingBuffer(0);

        assertEquals(0, zero.capacity());
        assertEquals(0, zero.size());
        assertTrue(zero.isFull());

        // push is silently ignored
        zero.push(createLine('A'));
        zero.push(createLine('B'));
        assertEquals(0, zero.size());

        // get throws for any index
        assertThrows(IndexOutOfBoundsException.class, () -> zero.get(0));

        // clear is safe
        assertDoesNotThrow(zero::clear);
        assertEquals(0, zero.size());
    }

    @Test
    void testPushAndGetWithoutWrap() {
        buffer.push(createLine('A'));
        buffer.push(createLine('B'));

        assertEquals(2, buffer.size());
        assertFalse(buffer.isFull());

        assertEquals('A', getLineId(buffer.get(0)));
        assertEquals('B', getLineId(buffer.get(1)));
    }

    @Test
    void testWrapAroundLogic() {
        buffer.push(createLine('A'));
        buffer.push(createLine('B'));
        buffer.push(createLine('C'));

        assertTrue(buffer.isFull());
        assertEquals(CAPACITY, buffer.size());

        buffer.push(createLine('D'));

        assertEquals(CAPACITY, buffer.size());
        assertTrue(buffer.isFull());

        assertEquals('B', getLineId(buffer.get(0)));
        assertEquals('C', getLineId(buffer.get(1)));
        assertEquals('D', getLineId(buffer.get(2)));
    }

    @Test
    void testMultipleWrapArounds() {
        char[] inputs = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'};
        for (char c : inputs) {
            buffer.push(createLine(c));
        }

        assertEquals(CAPACITY, buffer.size());

        assertEquals('F', getLineId(buffer.get(0)));
        assertEquals('G', getLineId(buffer.get(1)));
        assertEquals('H', getLineId(buffer.get(2)));
    }

    @Test
    void testGetOutOfBoundsThrowsException() {
        buffer.push(createLine('A'));

        assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(10));
    }

    @Test
    void testCapacityOne() {
        RingBuffer single = new RingBuffer(1);

        single.push(createLine('A'));
        assertEquals(1, single.size());
        assertEquals('A', getLineId(single.get(0)));

        single.push(createLine('B'));
        assertEquals(1, single.size());
        assertEquals('B', getLineId(single.get(0)));
    }
}
