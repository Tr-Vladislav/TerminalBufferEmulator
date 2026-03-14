package io.github.trvladislav.terminal.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerminalBufferTest {

    private static final int WIDTH = 10;
    private static final int HEIGHT = 4;
    private static final int MAX_SCROLLBACK = 5;
    private TerminalBuffer buf;

    @BeforeEach
    void setUp() {
        buf = new TerminalBuffer(WIDTH, HEIGHT, MAX_SCROLLBACK);
    }

    // ==================== Setup ====================

    @Test
    void testInitialState() {
        assertEquals(WIDTH, buf.getWidth());
        assertEquals(HEIGHT, buf.getHeight());
        assertEquals(0, buf.getCursorColumn());
        assertEquals(0, buf.getCursorRow());
        assertEquals(0, buf.getScrollbackSize());
    }

    @Test
    void testInvalidDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new TerminalBuffer(0, 24, 100));
        assertThrows(IllegalArgumentException.class, () -> new TerminalBuffer(80, 0, 100));
        assertThrows(IllegalArgumentException.class, () -> new TerminalBuffer(80, 24, -1));
    }

    @Test
    void testScreenStartsEmpty() {
        for (int r = 0; r < HEIGHT; r++) {
            for (int c = 0; c < WIDTH; c++) {
                assertEquals(' ', buf.getCharacterAt(c, r));
            }
        }
    }

    // ==================== Attributes ====================

    @Test
    void testSetAttributes() {
        buf.setAttributes(10, 3, CellUtils.STYLE_BOLD);

        assertEquals(10, buf.getCurrentFg());
        assertEquals(3, buf.getCurrentBg());
        assertEquals(CellUtils.STYLE_BOLD, buf.getCurrentStyles());
    }

    @Test
    void testAttributesAppliedToWrite() {
        buf.setAttributes(12, 5, CellUtils.STYLE_ITALIC);
        buf.writeText("A");

        long cell = buf.getScreenCell(0, 0);
        assertEquals('A', CellUtils.getCharacter(cell));
        assertEquals(12, CellUtils.getForegroundColor(cell));
        assertEquals(5, CellUtils.getBackgroundColor(cell));
        assertTrue(CellUtils.isItalic(cell));
    }

    // ==================== Cursor ====================

    @Test
    void testSetCursorPosition() {
        buf.setCursorPosition(5, 2);
        assertEquals(5, buf.getCursorColumn());
        assertEquals(2, buf.getCursorRow());
    }

    @Test
    void testCursorClamping() {
        buf.setCursorPosition(100, 100);
        assertEquals(WIDTH - 1, buf.getCursorColumn());
        assertEquals(HEIGHT - 1, buf.getCursorRow());
    }

    @Test
    void testMoveCursor() {
        buf.setCursorPosition(5, 2);

        buf.moveCursorUp(1);
        assertEquals(1, buf.getCursorRow());

        buf.moveCursorDown(2);
        assertEquals(3, buf.getCursorRow());

        buf.moveCursorLeft(3);
        assertEquals(2, buf.getCursorColumn());

        buf.moveCursorRight(4);
        assertEquals(6, buf.getCursorColumn());
    }

    // ==================== writeText ====================

    @Test
    void testWriteTextBasic() {
        buf.writeText("Hi");

        assertEquals('H', buf.getCharacterAt(0, 0));
        assertEquals('i', buf.getCharacterAt(1, 0));
        assertEquals(2, buf.getCursorColumn());
        assertEquals(0, buf.getCursorRow());
    }

    @Test
    void testWriteTextOverrides() {
        buf.writeText("AAAA");
        buf.setCursorPosition(0, 0);
        buf.writeText("BB");

        assertEquals('B', buf.getCharacterAt(0, 0));
        assertEquals('B', buf.getCharacterAt(1, 0));
        assertEquals('A', buf.getCharacterAt(2, 0));
        assertEquals('A', buf.getCharacterAt(3, 0));
    }

    @Test
    void testWriteTextWrapsToNextLine() {
        buf.setCursorPosition(8, 0);
        buf.writeText("ABCD");

        // A and B on row 0 at columns 8 and 9
        assertEquals('A', buf.getCharacterAt(8, 0));
        assertEquals('B', buf.getCharacterAt(9, 0));
        // C and D wrap to row 1
        assertEquals('C', buf.getCharacterAt(0, 1));
        assertEquals('D', buf.getCharacterAt(1, 1));
    }

    @Test
    void testWriteTextScrollsWhenAtBottom() {
        // Fill all rows
        for (int r = 0; r < HEIGHT; r++) {
            buf.setCursorPosition(0, r);
            buf.writeText("row" + r);
        }

        // Move to end of last row and write to trigger scroll
        buf.setCursorPosition(WIDTH - 1, HEIGHT - 1);
        buf.writeText("XY");

        // 'X' written at last cell of last row, then scroll + 'Y' on new last row
        assertEquals(1, buf.getScrollbackSize());
        assertEquals('Y', buf.getCharacterAt(0, HEIGHT - 1));
    }

    // ==================== insertText ====================

    @Test
    void testInsertTextShiftsRight() {
        buf.writeText("ABCD");
        buf.setCursorPosition(1, 0);
        buf.insertText("X");

        assertEquals('A', buf.getCharacterAt(0, 0));
        assertEquals('X', buf.getCharacterAt(1, 0));
        assertEquals('B', buf.getCharacterAt(2, 0));
        assertEquals('C', buf.getCharacterAt(3, 0));
        assertEquals('D', buf.getCharacterAt(4, 0));
    }

    @Test
    void testInsertTextWraps() {
        buf.setCursorPosition(8, 0);
        buf.insertText("ABCD");

        assertEquals('A', buf.getCharacterAt(8, 0));
        assertEquals('B', buf.getCharacterAt(9, 0));
        assertEquals('C', buf.getCharacterAt(0, 1));
        assertEquals('D', buf.getCharacterAt(1, 1));
    }

    // ==================== fillLine ====================

    @Test
    void testFillLine() {
        buf.setAttributes(2, 3, CellUtils.STYLE_BOLD);
        buf.setCursorPosition(0, 1);
        buf.fillLine('#');

        for (int c = 0; c < WIDTH; c++) {
            long cell = buf.getScreenCell(c, 1);
            assertEquals('#', CellUtils.getCharacter(cell));
            assertEquals(2, CellUtils.getForegroundColor(cell));
            assertEquals(3, CellUtils.getBackgroundColor(cell));
        }
        // Cursor stays put
        assertEquals(0, buf.getCursorColumn());
        assertEquals(1, buf.getCursorRow());
    }

    @Test
    void testFillLineEmpty() {
        buf.writeText("HELLO");
        buf.setCursorPosition(0, 0);
        buf.fillLineEmpty();

        for (int c = 0; c < WIDTH; c++) {
            assertEquals(' ', buf.getCharacterAt(c, 0));
        }
    }

    // ==================== insertLineAtBottom ====================

    @Test
    void testInsertLineAtBottom() {
        buf.setCursorPosition(0, 0);
        buf.writeText("AAA");
        buf.setCursorPosition(0, 1);
        buf.writeText("BBB");

        buf.insertLineAtBottom();

        // "AAA" should be in scrollback
        assertEquals(1, buf.getScrollbackSize());
        assertEquals('A', CellUtils.getCharacter(
                buf.getScrollbackCell(0, 0)));

        // "BBB" shifted up to row 0
        assertEquals('B', buf.getCharacterAt(0, 0));
        // Bottom row is now empty
        assertEquals(' ', buf.getCharacterAt(0, HEIGHT - 1));
    }

    @Test
    void testScrollbackCapacity() {
        // Push more lines than scrollback can hold
        for (int i = 0; i < MAX_SCROLLBACK + 3; i++) {
            buf.setCursorPosition(0, 0);
            buf.writeText(String.valueOf((char) ('A' + i)));
            buf.insertLineAtBottom();
        }

        assertEquals(MAX_SCROLLBACK, buf.getScrollbackSize());
    }

    // ==================== clearScreen ====================

    @Test
    void testClearScreen() {
        buf.writeText("HELLO");
        buf.clearScreen();

        assertEquals(0, buf.getCursorColumn());
        assertEquals(0, buf.getCursorRow());
        for (int r = 0; r < HEIGHT; r++) {
            for (int c = 0; c < WIDTH; c++) {
                assertEquals(' ', buf.getCharacterAt(c, r));
            }
        }
    }

    @Test
    void testClearScreenPreservesScrollback() {
        buf.writeText("TOP");
        buf.insertLineAtBottom();
        int scrollSizeBefore = buf.getScrollbackSize();

        buf.clearScreen();

        assertEquals(scrollSizeBefore, buf.getScrollbackSize());
    }

    // ==================== clearAll ====================

    @Test
    void testClearAll() {
        buf.writeText("DATA");
        buf.insertLineAtBottom();
        buf.insertLineAtBottom();

        buf.clearAll();

        assertEquals(0, buf.getCursorColumn());
        assertEquals(0, buf.getCursorRow());
        for (int r = 0; r < HEIGHT; r++) {
            for (int c = 0; c < WIDTH; c++) {
                assertEquals(' ', buf.getCharacterAt(c, r));
            }
        }
        // Scrollback lines should be cleared (replaced with empty)
        for (int i = 0; i < buf.getScrollbackSize(); i++) {
            for (int c = 0; c < WIDTH; c++) {
                assertEquals(' ', CellUtils.getCharacter(
                        buf.getScrollbackCell(c, i)));
            }
        }
    }

    // ==================== Content Access ====================

    @Test
    void testGetScreenLineAsString() {
        buf.writeText("Hello");
        String line = buf.getScreenLineAsString(0);

        assertTrue(line.startsWith("Hello"));
        assertEquals(WIDTH, line.length());
    }

    @Test
    void testGetScrollbackLineAsString() {
        buf.writeText("OLD");
        buf.insertLineAtBottom();

        String line = buf.getScrollbackLineAsString(0);
        assertTrue(line.startsWith("OLD"));
    }

    @Test
    void testGetScreenContent() {
        buf.setCursorPosition(0, 0);
        buf.writeText("AAA");
        buf.setCursorPosition(0, 1);
        buf.writeText("BBB");

        String content = buf.getScreenContent();
        String[] lines = content.split("\n", -1);

        assertEquals(HEIGHT, lines.length);
        assertTrue(lines[0].startsWith("AAA"));
        assertTrue(lines[1].startsWith("BBB"));
    }

    @Test
    void testGetFullContent() {
        buf.writeText("SCROLL");
        buf.insertLineAtBottom();
        buf.setCursorPosition(0, 0);
        buf.writeText("SCREEN");

        String full = buf.getFullContent();
        String[] lines = full.split("\n", -1);

        // 1 scrollback line + HEIGHT screen lines
        assertEquals(1 + HEIGHT, lines.length);
        assertTrue(lines[0].startsWith("SCROLL"));
        assertTrue(lines[1].startsWith("SCREEN"));
    }

    @Test
    void testGetScreenCellOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> buf.getScreenCell(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> buf.getScreenCell(0, HEIGHT));
        assertThrows(IndexOutOfBoundsException.class, () -> buf.getScreenCell(WIDTH, 0));
    }

    @Test
    void testGetScreenLineOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> buf.getScreenLineAsString(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> buf.getScreenLineAsString(HEIGHT));
    }

    // ==================== Default Attributes ====================

    @Test
    void testInitialAttributesAreDefaults() {
        assertEquals(CellUtils.DEFAULT_FG, buf.getCurrentFg());
        assertEquals(CellUtils.DEFAULT_BG, buf.getCurrentBg());
        assertEquals(CellUtils.DEFAULT_STYLES, buf.getCurrentStyles());
    }

    @Test
    void testResetAttributes() {
        buf.setAttributes(12, 5, CellUtils.STYLE_BOLD);
        buf.resetAttributes();

        assertEquals(CellUtils.DEFAULT_FG, buf.getCurrentFg());
        assertEquals(CellUtils.DEFAULT_BG, buf.getCurrentBg());
        assertEquals(CellUtils.DEFAULT_STYLES, buf.getCurrentStyles());
    }

    @Test
    void testSetAndResetForeground() {
        buf.setForeground(10);
        assertEquals(10, buf.getCurrentFg());

        buf.resetForeground();
        assertEquals(CellUtils.DEFAULT_FG, buf.getCurrentFg());
    }

    @Test
    void testSetAndResetBackground() {
        buf.setBackground(3);
        assertEquals(3, buf.getCurrentBg());

        buf.resetBackground();
        assertEquals(CellUtils.DEFAULT_BG, buf.getCurrentBg());
    }

    @Test
    void testSetAndResetStyles() {
        buf.setStyles(CellUtils.STYLE_BOLD | CellUtils.STYLE_ITALIC);
        assertEquals(CellUtils.STYLE_BOLD | CellUtils.STYLE_ITALIC, buf.getCurrentStyles());

        buf.resetStyles();
        assertEquals(CellUtils.DEFAULT_STYLES, buf.getCurrentStyles());
    }

    @Test
    void testResetForegroundPreservesOtherAttributes() {
        buf.setAttributes(10, 5, CellUtils.STYLE_UNDERLINE);
        buf.resetForeground();

        assertEquals(CellUtils.DEFAULT_FG, buf.getCurrentFg());
        assertEquals(5, buf.getCurrentBg());
        assertEquals(CellUtils.STYLE_UNDERLINE, buf.getCurrentStyles());
    }

    @Test
    void testWriteAfterResetUsesDefaults() {
        buf.setAttributes(12, 5, CellUtils.STYLE_BOLD);
        buf.resetAttributes();
        buf.writeText("A");

        long cell = buf.getScreenCell(0, 0);
        assertEquals(CellUtils.DEFAULT_FG, CellUtils.getForegroundColor(cell));
        assertEquals(CellUtils.DEFAULT_BG, CellUtils.getBackgroundColor(cell));
        assertEquals(CellUtils.DEFAULT_STYLES, CellUtils.getStyles(cell));
    }
}
