package io.github.trvladislav.terminal.buffer;

import io.github.trvladislav.terminal.cell.CellUtils;
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

    @Test
    void testAttributesAppliedToInsert() {
        buf.setAttributes(14, 6, CellUtils.STYLE_UNDERLINE);
        buf.insertText("Z");

        long cell = buf.getScreenCell(0, 0);
        assertEquals('Z', CellUtils.getCharacter(cell));
        assertEquals(14, CellUtils.getForegroundColor(cell));
        assertEquals(6, CellUtils.getBackgroundColor(cell));
        assertTrue(CellUtils.isUnderline(cell));
    }

    @Test
    void testSetAttributesValidation() {
        // negative values
        assertThrows(IllegalArgumentException.class, () -> buf.setAttributes(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> buf.setAttributes(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> buf.setAttributes(0, 0, -1));
        // over max color
        assertThrows(IllegalArgumentException.class, () -> buf.setAttributes(256, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> buf.setAttributes(0, 256, 0));
        // over max styles
        assertThrows(IllegalArgumentException.class, () -> buf.setAttributes(0, 0, 8));
    }

    @Test
    void testSetAttributesBoundaryValues() {
        // max valid values
        assertDoesNotThrow(() -> buf.setAttributes(255, 255, 7));
        assertEquals(255, buf.getCurrentFg());
        assertEquals(255, buf.getCurrentBg());
        assertEquals(7, buf.getCurrentStyles());
        // min valid values
        assertDoesNotThrow(() -> buf.setAttributes(0, 0, 0));
        assertEquals(0, buf.getCurrentFg());
        assertEquals(0, buf.getCurrentBg());
        assertEquals(0, buf.getCurrentStyles());
    }

    @Test
    void testSetForegroundValidation() {
        assertThrows(IllegalArgumentException.class, () -> buf.setForeground(-1));
        assertThrows(IllegalArgumentException.class, () -> buf.setForeground(256));
        assertDoesNotThrow(() -> buf.setForeground(255));
    }

    @Test
    void testSetBackgroundValidation() {
        assertThrows(IllegalArgumentException.class, () -> buf.setBackground(-1));
        assertThrows(IllegalArgumentException.class, () -> buf.setBackground(256));
        assertDoesNotThrow(() -> buf.setBackground(255));
    }

    @Test
    void testSetStylesValidation() {
        assertThrows(IllegalArgumentException.class, () -> buf.setStyles(-1));
        assertThrows(IllegalArgumentException.class, () -> buf.setStyles(8));
        assertDoesNotThrow(() -> buf.setStyles(7));
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

    // ==================== Wide Characters ====================

    @Test
    void testWriteWideChar() {
        buf.writeText("\u4E16"); // 世 — wide CJK character

        assertTrue(CellUtils.isWide(buf.getScreenCell(0, 0)));
        assertEquals(0x4E16, buf.getCharacterAt(0, 0));
        assertTrue(CellUtils.isWideContinuation(buf.getScreenCell(1, 0)));
        assertEquals(2, buf.getCursorColumn());
    }

    @Test
    void testWriteWideCharAtLastColumnWraps() {
        buf.setCursorPosition(WIDTH - 1, 0);
        buf.writeText("\u4E16"); // 世

        // Last column should get a space, wide char wraps to next row
        assertEquals(' ', buf.getCharacterAt(WIDTH - 1, 0));
        assertTrue(CellUtils.isWide(buf.getScreenCell(0, 1)));
        assertEquals(0x4E16, buf.getCharacterAt(0, 1));
        assertTrue(CellUtils.isWideContinuation(buf.getScreenCell(1, 1)));
    }

    @Test
    void testWriteMixedNarrowAndWide() {
        buf.writeText("A\u4E16B"); // A + 世 + B

        assertEquals('A', buf.getCharacterAt(0, 0));
        assertEquals(0x4E16, buf.getCharacterAt(1, 0));
        assertTrue(CellUtils.isWideContinuation(buf.getScreenCell(2, 0)));
        assertEquals('B', buf.getCharacterAt(3, 0));
        assertEquals(4, buf.getCursorColumn());
    }

    @Test
    void testInsertWideChar() {
        buf.writeText("AB");
        buf.setCursorPosition(1, 0);
        buf.insertText("\u4E16"); // 世

        assertEquals('A', buf.getCharacterAt(0, 0));
        assertTrue(CellUtils.isWide(buf.getScreenCell(1, 0)));
        assertTrue(CellUtils.isWideContinuation(buf.getScreenCell(2, 0)));
        assertEquals('B', buf.getCharacterAt(3, 0));
    }

    @Test
    void testWriteWideCharScrollsAtBottom() {
        buf.setCursorPosition(WIDTH - 1, HEIGHT - 1);
        buf.writeText("\u4E16"); // 世

        // Space at last column triggers wrap, which triggers scroll
        assertEquals(1, buf.getScrollbackSize());
        assertTrue(CellUtils.isWide(buf.getScreenCell(0, HEIGHT - 1)));
    }

    @Test
    void testFillLineWithWideChar() {
        buf.fillLine(0x4E16); // 世

        // WIDTH=10, so 5 wide chars fit (columns 0-1, 2-3, 4-5, 6-7, 8-9)
        for (int col = 0; col < WIDTH; col += 2) {
            assertTrue(CellUtils.isWide(buf.getScreenCell(col, 0)));
            assertTrue(CellUtils.isWideContinuation(buf.getScreenCell(col + 1, 0)));
        }
    }

    @Test
    void testFillLineWithWideCharOddWidth() {
        TerminalBuffer oddBuf = new TerminalBuffer(9, 4, 5);
        oddBuf.fillLine(0x4E16); // 世

        // 4 wide chars (cols 0-7), last column gets a space
        for (int col = 0; col < 8; col += 2) {
            assertTrue(CellUtils.isWide(oddBuf.getScreenCell(col, 0)));
        }
        assertEquals(' ', oddBuf.getCharacterAt(8, 0));
        assertFalse(CellUtils.isWide(oddBuf.getScreenCell(8, 0)));
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
        assertTrue(buf.getScrollbackSize() > 0, "Precondition: scrollback should not be empty");

        buf.clearAll();

        assertEquals(0, buf.getCursorColumn());
        assertEquals(0, buf.getCursorRow());
        assertEquals(0, buf.getScrollbackSize());
        for (int r = 0; r < HEIGHT; r++) {
            for (int c = 0; c < WIDTH; c++) {
                assertEquals(' ', buf.getCharacterAt(c, r));
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

    // ==================== Resize ====================

    @Test
    void testResizeWiderPreservesContent() {
        buf.writeText("Hello");
        buf.resize(20, HEIGHT);

        assertEquals(20, buf.getWidth());
        assertEquals('H', buf.getCharacterAt(0, 0));
        assertEquals('o', buf.getCharacterAt(4, 0));
        assertEquals(' ', buf.getCharacterAt(5, 0));
    }

    @Test
    void testResizeNarrowerTruncatesAndReflows() {
        // Use height 8 so reflow + empty lines don't overflow to scrollback
        // 10-wide, 8 rows → write "ABCDE" on row 0, rows 1-7 empty
        // Resize to 3-wide: "ABC"(soft) + "DE"(hard) + 7 empty = 9 lines → resize height to 10
        TerminalBuffer b = new TerminalBuffer(10, 2, 5);
        b.writeText("ABCDE");
        b.resize(3, 4);

        // "ABCDE" reflows into "ABC" (soft) + "DE" (hard) + 1 empty = 3 lines, fits in 4
        assertEquals('A', b.getCharacterAt(0, 0));
        assertEquals('B', b.getCharacterAt(1, 0));
        assertEquals('C', b.getCharacterAt(2, 0));
        assertEquals('D', b.getCharacterAt(0, 1));
        assertEquals('E', b.getCharacterAt(1, 1));
    }

    @Test
    void testResizeShorterMovesLinesToScrollback() {
        for (int r = 0; r < HEIGHT; r++) {
            buf.setCursorPosition(0, r);
            buf.writeText("R" + r);
        }

        buf.resize(WIDTH, 2);

        assertEquals(2, buf.getHeight());
        assertEquals(2, buf.getScrollbackSize());
        // Last 2 rows should be on screen
        assertEquals('R', buf.getCharacterAt(0, 0));
        assertEquals('R', buf.getCharacterAt(0, 1));
    }

    @Test
    void testResizeTallerAddsEmptyLines() {
        buf.writeText("Hello");
        buf.resize(WIDTH, 8);

        assertEquals(8, buf.getHeight());
        assertEquals('H', buf.getCharacterAt(0, 0));
        // New rows should be empty
        assertEquals(' ', buf.getCharacterAt(0, 7));
    }

    @Test
    void testResizeSameDimensionsDoesNothing() {
        buf.writeText("Test");
        buf.resize(WIDTH, HEIGHT);

        assertEquals('T', buf.getCharacterAt(0, 0));
    }

    @Test
    void testResizeInvalidDimensions() {
        assertThrows(IllegalArgumentException.class, () -> buf.resize(0, 10));
        assertThrows(IllegalArgumentException.class, () -> buf.resize(10, 0));
    }

    @Test
    void testResizeCursorClamped() {
        buf.setCursorPosition(8, 3);
        buf.resize(5, 2);

        assertEquals(4, buf.getCursorColumn());
        assertEquals(1, buf.getCursorRow());
    }

    @Test
    void testResizeReflowsSoftWrappedLines() {
        // Write enough to wrap: 10-wide buffer, write 15 chars
        buf.writeText("ABCDEFGHIJKLMNO");

        // Row 0: "ABCDEFGHIJ" (soft), Row 1: "KLMNO     " (hard)
        // Now resize wider — should merge back
        buf.resize(20, HEIGHT);

        assertEquals('A', buf.getCharacterAt(0, 0));
        assertEquals('O', buf.getCharacterAt(14, 0));
        assertEquals(' ', buf.getCharacterAt(15, 0));
    }

    @Test
    void testResizePreservesHardBreaks() {
        buf.writeText("AB");
        buf.setCursorPosition(0, 1);
        buf.writeText("CD");

        // Two separate lines with hard breaks
        buf.resize(20, HEIGHT);

        // Should stay as 2 lines, not merge
        assertEquals('A', buf.getCharacterAt(0, 0));
        assertEquals('B', buf.getCharacterAt(1, 0));
        assertEquals('C', buf.getCharacterAt(0, 1));
        assertEquals('D', buf.getCharacterAt(1, 1));
    }

    @Test
    void testResizeReflowsScrollbackContent() {
        // Fill screen and push lines to scrollback
        for (int r = 0; r < HEIGHT + 2; r++) {
            buf.setCursorPosition(0, 0);
            buf.writeText("LINE" + r);
            buf.insertLineAtBottom();
        }

        int scrollBefore = buf.getScrollbackSize();
        assertTrue(scrollBefore > 0);

        buf.resize(5, HEIGHT);

        // Scrollback lines should be reflowed to width 5
        String scrollLine = buf.getScrollbackLineAsString(0);
        assertEquals(5, scrollLine.length());
    }

    @Test
    void testResizeNarrowerWithWideChars() {
        // Start with 2 rows to minimize empty lines after reflow
        TerminalBuffer b = new TerminalBuffer(10, 2, 5);
        b.writeText("\u4E16\u754C");  // 世界 — each 2 cells wide, total 4 cells

        // Reflow to width 3: "世 "(soft) + "界 "(hard) + 1 empty = 3 lines → resize to 4
        b.resize(3, 4);

        assertTrue(CellUtils.isWide(b.getScreenCell(0, 0)));
        assertEquals(0x4E16, b.getCharacterAt(0, 0));
        assertTrue(CellUtils.isWide(b.getScreenCell(0, 1)));
        assertEquals(0x754C, b.getCharacterAt(0, 1));
    }

    @Test
    void testResizeWiderMergesWideCharLines() {
        TerminalBuffer narrow = new TerminalBuffer(4, 4, 5);
        narrow.writeText("A\u4E16\u754CB");

        // Width 4: "A世 " (soft, space pad at col 3), "界B  " (hard)
        narrow.resize(10, 4);

        // Merged: A + 世 + 界 + B (soft space stripped during reflow)
        assertEquals('A', narrow.getCharacterAt(0, 0));
        assertEquals(0x4E16, narrow.getCharacterAt(1, 0));
        assertEquals(0x754C, narrow.getCharacterAt(3, 0));
        assertEquals('B', narrow.getCharacterAt(5, 0));
    }

    @Test
    void testResizeOverflowToScrollback() {
        // Fill all 4 rows
        for (int r = 0; r < HEIGHT; r++) {
            buf.setCursorPosition(0, r);
            buf.writeText("ABCDEFGHIJ");
        }

        // Resize to width 5: each 10-char line becomes 2 lines → 8 lines total
        // Screen holds 4, so 4 go to scrollback
        buf.resize(5, HEIGHT);

        assertEquals(4, buf.getScrollbackSize());
        assertEquals('A', buf.getCharacterAt(0, 0));
        assertEquals('F', buf.getCharacterAt(0, 1));
    }

    // ==================== Soft Space ====================

    @Test
    void testSoftSpaceCreatedWhenWideCharWraps() {
        // Width 3: "AB" then 世 → cursor at col 2 (last col), pad + wrap
        TerminalBuffer b = new TerminalBuffer(3, 3, 5);
        b.writeText("AB\u4E16"); // A + B + 世

        // Col 2 of row 0 should be a soft space
        assertTrue(CellUtils.isSoftSpace(b.getScreenCell(2, 0)));
        // Wide char wraps to row 1
        assertTrue(CellUtils.isWide(b.getScreenCell(0, 1)));
        assertEquals(0x4E16, b.getCharacterAt(0, 1));
    }

    @Test
    void testSoftSpaceStrippedDuringReflow() {
        // Width 3: "AB世" → "AB[soft]"(soft) + "世[cont] "(hard)
        TerminalBuffer b = new TerminalBuffer(3, 3, 5);
        b.writeText("AB\u4E16");

        assertTrue(CellUtils.isSoftSpace(b.getScreenCell(2, 0)));

        // Resize wider — soft space should disappear, "AB世" on one line
        b.resize(10, 3);

        assertEquals('A', b.getCharacterAt(0, 0));
        assertEquals('B', b.getCharacterAt(1, 0));
        assertEquals(0x4E16, b.getCharacterAt(2, 0));
        // Col 4 is regular empty, not soft space
        assertFalse(CellUtils.isSoftSpace(b.getScreenCell(4, 0)));
    }

    @Test
    void testSoftSpaceRoundTrip() {
        // Narrow → wide → narrow: soft spaces don't accumulate
        // Use enough height so lines don't overflow to scrollback
        TerminalBuffer b = new TerminalBuffer(3, 5, 5);
        b.writeText("AB\u4E16");

        b.resize(10, 5);  // widen — soft space stripped, "AB世" on one line
        b.resize(3, 5);   // narrow again — new soft space at col 2

        assertEquals('A', b.getCharacterAt(0, 0));
        assertEquals('B', b.getCharacterAt(1, 0));
        assertTrue(CellUtils.isSoftSpace(b.getScreenCell(2, 0)));
        assertEquals(0x4E16, b.getCharacterAt(0, 1));
    }
}
