package io.github.trvladislav.terminal.buffer;

import io.github.trvladislav.terminal.cell.CellUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LineTest {

    private static final int WIDTH = 10;
    private Line line;

    @BeforeEach
    void setUp() {
        line = new Line(WIDTH);
    }

    @Test
    void testInitialLineIsFilledWithSpaces() {
        for (int i = 0; i < WIDTH; i++) {
            assertEquals(' ', CellUtils.getCharacter(line.getCell(i)));
        }
    }

    @Test
    void testWriteChangesCellData() {
        long blueA = CellUtils.encode('A', 4, 0, 0);
        line.write(2, blueA);

        assertEquals('A', CellUtils.getCharacter(line.getCell(2)));
        assertEquals(4, CellUtils.getForegroundColor(line.getCell(2)));
    }

    @Test
    void testInsertShiftsCharactersToTheRight() {
        line.write(0, CellUtils.encode('A', 7, 0, 0));
        line.write(1, CellUtils.encode('B', 7, 0, 0));
        line.write(2, CellUtils.encode('C', 7, 0, 0));

        long charX = CellUtils.encode('X', 15, 0, 0);
        line.insert(1, charX);

        assertEquals('A', CellUtils.getCharacter(line.getCell(0)));
        assertEquals('X', CellUtils.getCharacter(line.getCell(1)));
        assertEquals(15, CellUtils.getForegroundColor(line.getCell(1)));
        assertEquals('B', CellUtils.getCharacter(line.getCell(2)));
        assertEquals('C', CellUtils.getCharacter(line.getCell(3)));
    }

    @Test
    void testLastCharacterDropsOffOnInsert() {
        line.write(WIDTH - 1, CellUtils.encode('Z', 7, 0, 0));

        line.insert(0, CellUtils.encode('A', 7, 0, 0));

        assertNotEquals('Z', CellUtils.getCharacter(line.getCell(WIDTH - 1)));
    }

    @Test
    void testDeleteShiftsCharactersToTheLeft() {
        line.write(0, CellUtils.encode('A', 7, 0, 0));
        line.write(1, CellUtils.encode('B', 7, 0, 0));
        line.write(2, CellUtils.encode('C', 7, 0, 0));

        line.delete(1);

        assertEquals('A', CellUtils.getCharacter(line.getCell(0)));
        assertEquals('C', CellUtils.getCharacter(line.getCell(1)));
        assertEquals(' ', CellUtils.getCharacter(line.getCell(WIDTH - 1)));
    }

    @Test
    void testClearResetsAllCells() {
        line.write(0, CellUtils.encode('X', 15, 3, CellUtils.STYLE_BOLD));
        line.write(5, CellUtils.encode('Y', 10, 2, CellUtils.STYLE_ITALIC));

        line.clear();

        for (int i = 0; i < WIDTH; i++) {
            assertEquals(' ', CellUtils.getCharacter(line.getCell(i)));
            assertEquals(7, CellUtils.getForegroundColor(line.getCell(i)));
            assertEquals(0, CellUtils.getBackgroundColor(line.getCell(i)));
        }
    }

    @Test
    void testToStringReturnsCharacters() {
        line.write(0, CellUtils.encode('H', 7, 0, 0));
        line.write(1, CellUtils.encode('i', 7, 0, 0));

        String result = line.toString();
        assertTrue(result.startsWith("Hi"));
        assertEquals(WIDTH, result.length());
    }

    // ==================== Wide Characters ====================

    @Test
    void testWriteWideCharOccupiesTwoCells() {
        long wide = CellUtils.encodeWide(0x4E16, 7, 0, 0); // 世
        line.write(3, wide);

        assertTrue(CellUtils.isWide(line.getCell(3)));
        assertEquals(0x4E16, CellUtils.getCharacter(line.getCell(3)));
        assertTrue(CellUtils.isWideContinuation(line.getCell(4)));
    }

    @Test
    void testWriteWideAtLastColumnDoesNothing() {
        long wide = CellUtils.encodeWide(0x4E16, 7, 0, 0);
        line.write(WIDTH - 1, wide);

        // Should remain empty — no room for continuation
        assertEquals(' ', CellUtils.getCharacter(line.getCell(WIDTH - 1)));
        assertFalse(CellUtils.isWide(line.getCell(WIDTH - 1)));
    }

    @Test
    void testWriteOverWidePairCleansUpOrphan() {
        long wide = CellUtils.encodeWide(0x4E16, 7, 0, 0);
        line.write(3, wide);

        // Overwrite left half with regular char — continuation at 4 should be cleared
        line.write(3, CellUtils.encode('A', 7, 0, 0));

        assertEquals('A', CellUtils.getCharacter(line.getCell(3)));
        assertFalse(CellUtils.isWideContinuation(line.getCell(4)));
        assertEquals(' ', CellUtils.getCharacter(line.getCell(4)));
    }

    @Test
    void testWriteOverContinuationCleansUpOrphan() {
        long wide = CellUtils.encodeWide(0x4E16, 7, 0, 0);
        line.write(3, wide);

        // Overwrite continuation at 4 — left half at 3 should be cleared
        line.write(4, CellUtils.encode('B', 7, 0, 0));

        assertEquals(' ', CellUtils.getCharacter(line.getCell(3)));
        assertFalse(CellUtils.isWide(line.getCell(3)));
        assertEquals('B', CellUtils.getCharacter(line.getCell(4)));
    }

    @Test
    void testInsertWideShiftsByTwo() {
        line.write(0, CellUtils.encode('A', 7, 0, 0));
        line.write(1, CellUtils.encode('B', 7, 0, 0));

        long wide = CellUtils.encodeWide(0x4E16, 7, 0, 0);
        line.insert(0, wide);

        assertTrue(CellUtils.isWide(line.getCell(0)));
        assertTrue(CellUtils.isWideContinuation(line.getCell(1)));
        assertEquals('A', CellUtils.getCharacter(line.getCell(2)));
        assertEquals('B', CellUtils.getCharacter(line.getCell(3)));
    }

    @Test
    void testInsertWideAtLastColumnDoesNothing() {
        line.write(0, CellUtils.encode('A', 7, 0, 0));

        long wide = CellUtils.encodeWide(0x4E16, 7, 0, 0);
        line.insert(WIDTH - 1, wide);

        // Should remain unchanged
        assertEquals('A', CellUtils.getCharacter(line.getCell(0)));
        assertFalse(CellUtils.isWide(line.getCell(WIDTH - 1)));
    }

    @Test
    void testDeleteWideRemovesBothHalves() {
        line.write(0, CellUtils.encode('A', 7, 0, 0));
        long wide = CellUtils.encodeWide(0x4E16, 7, 0, 0);
        line.write(1, wide);
        line.write(3, CellUtils.encode('B', 7, 0, 0));

        // Delete the left half
        line.delete(1);

        assertEquals('A', CellUtils.getCharacter(line.getCell(0)));
        assertEquals('B', CellUtils.getCharacter(line.getCell(1)));
    }

    @Test
    void testDeleteContinuationRemovesBothHalves() {
        line.write(0, CellUtils.encode('A', 7, 0, 0));
        long wide = CellUtils.encodeWide(0x4E16, 7, 0, 0);
        line.write(1, wide);
        line.write(3, CellUtils.encode('B', 7, 0, 0));

        // Delete the continuation half
        line.delete(2);

        assertEquals('A', CellUtils.getCharacter(line.getCell(0)));
        assertEquals('B', CellUtils.getCharacter(line.getCell(1)));
    }

    @Test
    void testToStringSkipsContinuationCells() {
        long wide = CellUtils.encodeWide(0x4E16, 7, 0, 0); // 世
        line.write(0, wide);
        line.write(2, CellUtils.encode('A', 7, 0, 0));

        String result = line.toString();
        assertEquals('\u4E16', result.charAt(0));
        assertEquals('A', result.charAt(1));
        // Width is 10, wide takes 2 cells but 1 char in string, so length = 9
        assertEquals(WIDTH - 1, result.length());
    }

    @Test
    void testOutOfBoundsThrowsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> line.write(-1, CellUtils.encode('!', 7, 0, 0)));
        assertThrows(IndexOutOfBoundsException.class, () -> line.write(WIDTH, CellUtils.encode('!', 7, 0, 0)));
        assertThrows(IndexOutOfBoundsException.class, () -> line.insert(-1, CellUtils.encode('?', 7, 0, 0)));
        assertThrows(IndexOutOfBoundsException.class, () -> line.insert(WIDTH, CellUtils.encode('?', 7, 0, 0)));
        assertThrows(IndexOutOfBoundsException.class, () -> line.getCell(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> line.getCell(WIDTH));
        assertThrows(IndexOutOfBoundsException.class, () -> line.delete(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> line.delete(WIDTH));
    }
}
