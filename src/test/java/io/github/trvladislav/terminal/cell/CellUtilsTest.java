package io.github.trvladislav.terminal.cell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CellUtilsTest {

    @Test
    void testEncodeDecodeAllFields() {
        int charCode = 'A';
        int fg = 15;
        int bg = 4;
        int style = CellUtils.STYLE_BOLD | CellUtils.STYLE_UNDERLINE;

        long cell = CellUtils.encode(charCode, fg, bg, style);

        assertEquals(charCode, CellUtils.getCharacter(cell));
        assertEquals(fg, CellUtils.getForegroundColor(cell));
        assertEquals(bg, CellUtils.getBackgroundColor(cell));
        assertEquals(style, CellUtils.getStyles(cell));
    }

    @Test
    void testUnicodeAndEmoji() {
        int rocketEmoji = 0x1F680;
        long cell = CellUtils.encode(rocketEmoji, 7, 0, 0);

        assertEquals(rocketEmoji, CellUtils.getCharacter(cell));
    }

    @Test
    void testMaxUnicodeCodePoint() {
        int maxCodePoint = 0xFFFFFF; // 24-bit max
        long cell = CellUtils.encode(maxCodePoint, 255, 255, 255);

        assertEquals(maxCodePoint, CellUtils.getCharacter(cell));
        assertEquals(255, CellUtils.getForegroundColor(cell));
        assertEquals(255, CellUtils.getBackgroundColor(cell));
        assertEquals(255, CellUtils.getStyles(cell));
    }

    @Test
    void testStyleFlags() {
        long cell = CellUtils.encode('x', 7, 0, CellUtils.STYLE_BOLD | CellUtils.STYLE_ITALIC);

        assertTrue(CellUtils.isBold(cell));
        assertTrue(CellUtils.isItalic(cell));
        assertFalse(CellUtils.isUnderline(cell));
    }

    @Test
    void testEmptyCell() {
        long cell = CellUtils.EMPTY_CELL;

        assertEquals(' ', CellUtils.getCharacter(cell));
        assertEquals(CellUtils.DEFAULT_FG, CellUtils.getForegroundColor(cell));
        assertEquals(CellUtils.DEFAULT_BG, CellUtils.getBackgroundColor(cell));
        assertEquals(CellUtils.STYLE_NONE, CellUtils.getStyles(cell));
        assertFalse(CellUtils.isWide(cell));
        assertFalse(CellUtils.isWideContinuation(cell));
    }

    @Test
    void testSetCharacterPreservesOtherFields() {
        long cell = CellUtils.encode('A', 10, 20, CellUtils.STYLE_BOLD);
        long modified = CellUtils.setCharacter(cell, 'Z');

        assertEquals('Z', CellUtils.getCharacter(modified));
        assertEquals(10, CellUtils.getForegroundColor(modified));
        assertEquals(20, CellUtils.getBackgroundColor(modified));
        assertEquals(CellUtils.STYLE_BOLD, CellUtils.getStyles(modified));
    }

    @Test
    void testSetForegroundColorPreservesOtherFields() {
        long cell = CellUtils.encode('A', 10, 20, CellUtils.STYLE_ITALIC);
        long modified = CellUtils.setForegroundColor(cell, 99);

        assertEquals('A', CellUtils.getCharacter(modified));
        assertEquals(99, CellUtils.getForegroundColor(modified));
        assertEquals(20, CellUtils.getBackgroundColor(modified));
        assertEquals(CellUtils.STYLE_ITALIC, CellUtils.getStyles(modified));
    }

    @Test
    void testSetBackgroundColorPreservesOtherFields() {
        long cell = CellUtils.encode('A', 10, 20, CellUtils.STYLE_UNDERLINE);
        long modified = CellUtils.setBackgroundColor(cell, 55);

        assertEquals('A', CellUtils.getCharacter(modified));
        assertEquals(10, CellUtils.getForegroundColor(modified));
        assertEquals(55, CellUtils.getBackgroundColor(modified));
        assertEquals(CellUtils.STYLE_UNDERLINE, CellUtils.getStyles(modified));
    }

    @Test
    void testSetStylesPreservesOtherFields() {
        long cell = CellUtils.encode('A', 10, 20, CellUtils.STYLE_BOLD);
        long modified = CellUtils.setStyles(cell, CellUtils.STYLE_ITALIC | CellUtils.STYLE_UNDERLINE);

        assertEquals('A', CellUtils.getCharacter(modified));
        assertEquals(10, CellUtils.getForegroundColor(modified));
        assertEquals(20, CellUtils.getBackgroundColor(modified));
        assertEquals(CellUtils.STYLE_ITALIC | CellUtils.STYLE_UNDERLINE, CellUtils.getStyles(modified));
    }

    // ==================== Wide Characters ====================

    @Test
    void testWideAndContinuationFlags() {
        long wide = CellUtils.encodeWide(0x4E16, 7, 0, 0); // 世
        long cont = CellUtils.createWideContinuation(7, 0, 0);
        long regular = CellUtils.encode('A', 7, 0, 0);

        assertTrue(CellUtils.isWide(wide));
        assertFalse(CellUtils.isWideContinuation(wide));

        assertTrue(CellUtils.isWideContinuation(cont));
        assertFalse(CellUtils.isWide(cont));

        assertFalse(CellUtils.isWide(regular));
        assertFalse(CellUtils.isWideContinuation(regular));
    }

    @Test
    void testDisplayWidth() {
        assertEquals(1, CellUtils.getDisplayWidth('A'));
        assertEquals(2, CellUtils.getDisplayWidth(0x4E16));  // 世 (CJK)
        assertEquals(2, CellUtils.getDisplayWidth(0x1F680)); // 🚀 (emoji)
        assertEquals(2, CellUtils.getDisplayWidth(0x3042));  // あ (Hiragana)
        assertEquals(1, CellUtils.getDisplayWidth(0x00E9));  // é (Latin)
    }

    @Test
    void testWideCharPairStorage() {
        long left = CellUtils.encodeWide(0x4E16, 10, 3, CellUtils.STYLE_BOLD);
        long right = CellUtils.createWideContinuation(10, 3, CellUtils.STYLE_BOLD);

        assertEquals(0x4E16, CellUtils.getCharacter(left));
        assertEquals(' ', CellUtils.getCharacter(right));
        assertEquals(CellUtils.getForegroundColor(left), CellUtils.getForegroundColor(right));
        assertEquals(CellUtils.getBackgroundColor(left), CellUtils.getBackgroundColor(right));
    }

    // ==================== Soft Space ====================

    @Test
    void testSoftSpaceFlag() {
        long soft = CellUtils.createSoftSpace(7, 0, 0);
        long regular = CellUtils.encode(' ', 7, 0, 0);

        assertTrue(CellUtils.isSoftSpace(soft));
        assertFalse(CellUtils.isSoftSpace(regular));
        assertEquals(' ', CellUtils.getCharacter(soft));
        assertFalse(CellUtils.isWide(soft));
        assertFalse(CellUtils.isWideContinuation(soft));
    }

    @Test
    void testSoftSpacePreservesColors() {
        long soft = CellUtils.createSoftSpace(10, 3, CellUtils.STYLE_BOLD);

        assertEquals(10, CellUtils.getForegroundColor(soft));
        assertEquals(3, CellUtils.getBackgroundColor(soft));
        assertEquals(CellUtils.STYLE_BOLD, CellUtils.getStyles(soft));
    }
}
