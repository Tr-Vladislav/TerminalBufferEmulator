package io.github.trvladislav.terminal.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LineTest {

    private Line line;
    private final int WIDTH = 10;

    @BeforeEach
    void setUp() {
        line = new Line(WIDTH);
    }

    @Test
    void testInitialLineIsFilledWithSpaces() {
        for (int i = 0; i < WIDTH; i++) {
            // Check if every cell is initialized as a space character
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
        // Prepare: [A, B, C, ...]
        line.write(0, CellUtils.encode('A', 7, 0, 0));
        line.write(1, CellUtils.encode('B', 7, 0, 0));
        line.write(2, CellUtils.encode('C', 7, 0, 0));

        // Insert 'X' at index 1
        long charX = CellUtils.encode('X', 7, 0, 0);
        line.insert(1, charX);

        // Result should be: [A, X, B, C, ...]
        assertEquals('A', CellUtils.getCharacter(line.getCell(0)));
        assertEquals('X', CellUtils.getCharacter(line.getCell(1)));
        assertEquals('B', CellUtils.getCharacter(line.getCell(2)));
        assertEquals('C', CellUtils.getCharacter(line.getCell(3)));
    }

    @Test
    void testLastCharacterDropsOffOnInsert() {
        // Set 'Z' at the very last position
        line.write(WIDTH - 1, CellUtils.encode('Z', 7, 0, 0));

        // Push everything by inserting at the start
        line.insert(0, CellUtils.encode('A', 7, 0, 0));

        // 'Z' should be pushed out of array bounds
        assertNotEquals('Z', CellUtils.getCharacter(line.getCell(WIDTH - 1)));
    }

    @Test
    void testBoundaryProtection() {
        // Ensure methods do not throw exceptions for out-of-bounds indices
        assertDoesNotThrow(() -> {
            line.write(-1, CellUtils.encode('!', 7, 0, 0));
            line.write(WIDTH + 5, CellUtils.encode('!', 7, 0, 0));
            line.insert(WIDTH, CellUtils.encode('?', 7, 0, 0));
        });
    }
}