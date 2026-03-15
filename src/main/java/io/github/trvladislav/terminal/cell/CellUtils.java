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

    /**
     * Pre-computed empty cell: space character, default fg/bg, no styles.
     */
    public static final long EMPTY_CELL =
            (' ' & ((1L << CHAR_BITS) - 1)) |
            ((DEFAULT_FG & 0xFFL) << FG_SHIFT) |
            ((DEFAULT_BG & 0xFFL) << BG_SHIFT) |
            ((DEFAULT_STYLES & 0xFFL) << STYLE_SHIFT);

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
     * @deprecated Use {@link #EMPTY_CELL} constant directly.
     */
    @Deprecated
    public static long createEmpty() {
        return EMPTY_CELL;
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
        // Fast path: ASCII and Latin — vast majority of input
        if (codePoint < 0x1100) return 1;

        // Hangul Jamo
        if (codePoint >= 0x1100 && codePoint <= 0x115F) return 2;

        // Angle brackets
        if (codePoint >= 0x2329 && codePoint <= 0x232A) return 2;

        // CJK and related (0x3000–0xD7AF)
        if (codePoint >= 0x3000 && codePoint <= 0xD7AF) {
            if (codePoint <= 0x303F) return 2;   // CJK Symbols and Punctuation
            if (codePoint <= 0x309F) return 2;   // Hiragana
            if (codePoint <= 0x30FF) return 2;   // Katakana
            if (codePoint <= 0x312F) return 2;   // Bopomofo
            if (codePoint >= 0x31F0 && codePoint <= 0x31FF) return 2; // Katakana Phonetic Extensions
            if (codePoint >= 0x3400 && codePoint <= 0x4DBF) return 2; // CJK Extension A
            if (codePoint >= 0x4E00 && codePoint <= 0x9FFF) return 2; // CJK Unified Ideographs
            if (codePoint >= 0xAC00 && codePoint <= 0xD7AF) return 2; // Hangul Syllables
            return 1;
        }

        // CJK Compatibility Ideographs
        if (codePoint >= 0xF900 && codePoint <= 0xFAFF) return 2;

        // Fullwidth forms
        if (codePoint >= 0xFF01 && codePoint <= 0xFF60) return 2;
        if (codePoint >= 0xFFE0 && codePoint <= 0xFFE6) return 2;

        // Supplementary CJK
        if (codePoint >= 0x20000 && codePoint <= 0x2A6DF) return 2; // CJK Extension B
        if (codePoint >= 0x2A700 && codePoint <= 0x2CEAF) return 2; // CJK Extensions C-F
        if (codePoint >= 0x30000 && codePoint <= 0x323AF) return 2; // CJK Extensions G-I

        // Emoji ranges
        if (codePoint >= 0x1F300 && codePoint <= 0x1F9FF) return 2; // Misc Symbols, Emoticons
        if (codePoint >= 0x1FA00 && codePoint <= 0x1FAFF) return 2; // Extended Symbols and Pictographs
        if (codePoint >= 0x2600 && codePoint <= 0x27BF) return 2;   // Misc Symbols, Dingbats

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
