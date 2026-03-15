package io.github.trvladislav.terminal.cell;

/**
 * Utility class for packing terminal cell data into a single 64-bit long.
 *
 * Memory layout:
 * [0..23]  - Character (Unicode Code Point)
 * [24..31] - Foreground Color (0-255)
 * [32..39] - Background Color (0-255)
 * [40..47] - Style flags (Bold, Italic, etc.)
 * [48]     - Wide flag (1 = this cell is the left half of a wide character)
 * [49]     - Wide continuation flag (1 = this cell is the right half placeholder)
 * [50..63] - Reserved
 */
public final class CellUtils {

    // Bit layout constants
    private static final int CHAR_BITS = 24;
    private static final long CHAR_MASK = (1L << CHAR_BITS) - 1;
    private static final int FG_SHIFT = 24;
    private static final int BG_SHIFT = 32;
    private static final int STYLE_SHIFT = 40;
    private static final long BYTE_MASK = 0xFFL;

    // Default attribute values
    public static final int DEFAULT_FG = 7;
    public static final int DEFAULT_BG = 0;
    public static final int DEFAULT_STYLES = 0;

    // Style bitmasks
    public static final int STYLE_NONE = 0;
    public static final int STYLE_BOLD = 1;
    public static final int STYLE_ITALIC = 2;
    public static final int STYLE_UNDERLINE = 4;

    // Wide character flags (bits 48-49)
    private static final long WIDE_FLAG = 1L << 48;
    private static final long WIDE_CONT_FLAG = 1L << 49;

    private CellUtils() {
    }

    /**
     * Packs cell properties into a single long.
     */
    public static long encode(int character, int fgColor, int bgColor, int styles) {
        return (character & CHAR_MASK) |
                ((fgColor & BYTE_MASK) << FG_SHIFT) |
                ((bgColor & BYTE_MASK) << BG_SHIFT) |
                ((styles & BYTE_MASK) << STYLE_SHIFT);
    }

    /**
     * Returns an empty space cell with default colors (FG: 7, BG: 0).
     */
    public static long createEmpty() {
        return encode(' ', DEFAULT_FG, DEFAULT_BG, DEFAULT_STYLES);
    }

    // --- Decoders ---

    public static int getCharacter(long cell) {
        return (int) (cell & CHAR_MASK);
    }

    public static int getForegroundColor(long cell) {
        return (int) ((cell >>> FG_SHIFT) & BYTE_MASK);
    }

    public static int getBackgroundColor(long cell) {
        return (int) ((cell >>> BG_SHIFT) & BYTE_MASK);
    }

    public static int getStyles(long cell) {
        return (int) ((cell >>> STYLE_SHIFT) & BYTE_MASK);
    }

    /**
     * Packs a wide character into a cell with the wide flag set.
     */
    public static long encodeWide(int character, int fgColor, int bgColor, int styles) {
        return encode(character, fgColor, bgColor, styles) | WIDE_FLAG;
    }

    /**
     * Creates a wide continuation cell (right half placeholder).
     * Carries the same colors and styles as the left half for consistent rendering.
     */
    public static long createWideContinuation(int fgColor, int bgColor, int styles) {
        return encode(' ', fgColor, bgColor, styles) | WIDE_CONT_FLAG;
    }

    // --- Style Checkers ---

    public static boolean isBold(long cell) {
        return (getStyles(cell) & STYLE_BOLD) != 0;
    }

    public static boolean isItalic(long cell) {
        return (getStyles(cell) & STYLE_ITALIC) != 0;
    }

    public static boolean isUnderline(long cell) {
        return (getStyles(cell) & STYLE_UNDERLINE) != 0;
    }

    // --- Wide Character Checkers ---

    public static boolean isWide(long cell) {
        return (cell & WIDE_FLAG) != 0;
    }

    public static boolean isWideContinuation(long cell) {
        return (cell & WIDE_CONT_FLAG) != 0;
    }

    public static int getDisplayWidth(int codePoint) {
        //TODO: check ranges of wide characters
        return 1;
    }

    // --- Modifiers ---

    /**
     * Replaces the character bits while preserving colors and styles.
     */
    public static long setCharacter(long cell, int newCharacter) {
        return (cell & ~CHAR_MASK) | (newCharacter & CHAR_MASK);
    }

    /**
     * Replaces the foreground color while preserving other fields.
     */
    public static long setForegroundColor(long cell, int newFgColor) {
        return (cell & ~(BYTE_MASK << FG_SHIFT)) | ((newFgColor & BYTE_MASK) << FG_SHIFT);
    }

    /**
     * Replaces the background color while preserving other fields.
     */
    public static long setBackgroundColor(long cell, int newBgColor) {
        return (cell & ~(BYTE_MASK << BG_SHIFT)) | ((newBgColor & BYTE_MASK) << BG_SHIFT);
    }

    /**
     * Replaces the style flags while preserving other fields.
     */
    public static long setStyles(long cell, int newStyles) {
        return (cell & ~(BYTE_MASK << STYLE_SHIFT)) | ((newStyles & BYTE_MASK) << STYLE_SHIFT);
    }
}
