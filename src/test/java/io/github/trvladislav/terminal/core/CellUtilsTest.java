package io.github.trvladislav.terminal.core;

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
    void testStyleFlags() {
        long cell = CellUtils.encode('x', 7, 0, CellUtils.STYLE_BOLD | CellUtils.STYLE_ITALIC);

        assertTrue(CellUtils.isBold(cell));
        assertTrue(CellUtils.isItalic(cell));
        assertFalse(CellUtils.isUnderline(cell));
    }
}
