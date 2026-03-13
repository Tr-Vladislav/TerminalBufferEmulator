package io.github.trvladislav.terminal.core;
/**
 * Utility class for packing terminal cell data into a single 64-bit long.
 * * Memory layout:
 * [0..23]  - Character (Unicode Code Point)
 * [24..31] - Foreground Color (0-255)
 * [32..39] - Background Color (0-255)
 * [40..47] - Style flags (Bold, Italic, etc.)
 * [48..63] - Reserved (e.g., for wide character flags)
 */
public final class CellUtils {

    // Style bitmasks
    public static final int STYLE_NONE = 0;
    public static final int STYLE_BOLD = 1;      // 0001
    public static final int STYLE_ITALIC = 2;    // 0010
    public static final int STYLE_UNDERLINE = 4; // 0100

    private CellUtils() {
    }

    /**
     * Packs cell properties into a single long.
     */
    public static long encode(int character, int fgColor, int bgColor, int styles) {
        return (character & 0xFFFFFFL) |
                ((fgColor & 0xFFL) << 24) |
                ((bgColor & 0xFFL) << 32) |
                ((styles & 0xFFL) << 40);
    }

    /**
     * Returns an empty space cell with default colors (e.g., FG: 7, BG: 0).
     */
    public static long createEmpty() {
        return encode(' ', 7, 0, STYLE_NONE);
    }

    // --- Decoders ---

    public static int getCharacter(long cell) {
        return (int) (cell & 0xFFFFFFL);
    }

    public static int getForegroundColor(long cell) {
        return (int) ((cell >>> 24) & 0xFFL);
    }

    public static int getBackgroundColor(long cell) {
        return (int) ((cell >>> 32) & 0xFFL);
    }

    public static int getStyles(long cell) {
        return (int) ((cell >>> 40) & 0xFFL);
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

    // --- Modifiers ---

    /**
     * Replaces the character bits while preserving colors and styles.
     */
    public static long setCharacter(long cell, int newCharacter) {
        // ~0xFFFFFFL masks out the old character bits
        return (cell & ~0xFFFFFFL) | (newCharacter & 0xFFFFFFL);
    }
}
