package io.github.trvladislav.terminal;

import io.github.trvladislav.terminal.buffer.TerminalBuffer;
import io.github.trvladislav.terminal.cell.CellUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Main {

    private static final int COLS = 80;
    private static final int ROWS = 24;
    private static final int MAX_SCROLLBACK = 200;
    private static final int CELL_WIDTH = 10;
    private static final int CELL_HEIGHT = 18;

    private static final Color[] TERMINAL_COLORS = {
            new Color(0, 0, 0),       // 0 - black
            new Color(170, 0, 0),     // 1 - red
            new Color(0, 170, 0),     // 2 - green
            new Color(170, 85, 0),    // 3 - yellow/brown
            new Color(0, 0, 170),     // 4 - blue
            new Color(170, 0, 170),   // 5 - magenta
            new Color(0, 170, 170),   // 6 - cyan
            new Color(170, 170, 170), // 7 - white (default fg)
            new Color(85, 85, 85),    // 8 - bright black
            new Color(255, 85, 85),   // 9 - bright red
            new Color(85, 255, 85),   // 10 - bright green
            new Color(255, 255, 85),  // 11 - bright yellow
            new Color(85, 85, 255),   // 12 - bright blue
            new Color(255, 85, 255),  // 13 - bright magenta
            new Color(85, 255, 255),  // 14 - bright cyan
            new Color(255, 255, 255)  // 15 - bright white
    };

    public static void main(String[] args) {
        TerminalBuffer buffer = new TerminalBuffer(COLS, ROWS, MAX_SCROLLBACK);

        // Demo content
        buffer.setAttributes(10, 0, CellUtils.STYLE_BOLD);
        buffer.writeText("Terminal Buffer Emulator");
        buffer.setCursorPosition(0, 1);
        buffer.setAttributes(7, 0, CellUtils.STYLE_NONE);
        buffer.writeText("Type to write. Enter=newline, Backspace=delete.");
        buffer.setCursorPosition(0, 2);
        buffer.setAttributes(3, 0, CellUtils.STYLE_ITALIC);
        buffer.writeText("Colors: ");
        for (int c = 1; c <= 6; c++) {
            buffer.setAttributes(c, 0, CellUtils.STYLE_NONE);
            buffer.writeText("color" + c + " ");
        }
        buffer.setCursorPosition(0, 4);
        buffer.setAttributes(7, 0, CellUtils.STYLE_NONE);
        buffer.writeText("> ");

        SwingUtilities.invokeLater(() -> createUI(buffer));
    }

    private static void createUI(TerminalBuffer buffer) {
        JFrame frame = new JFrame("Terminal Buffer Emulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

                // Background
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());

                FontMetrics fm = g2.getFontMetrics();

                for (int row = 0; row < ROWS; row++) {
                    for (int col = 0; col < COLS; col++) {
                        long cell = buffer.getScreenCell(col, row);
                        int ch = CellUtils.getCharacter(cell);
                        int fg = CellUtils.getForegroundColor(cell);
                        int bg = CellUtils.getBackgroundColor(cell);
                        int styles = CellUtils.getStyles(cell);

                        int x = col * CELL_WIDTH;
                        int y = row * CELL_HEIGHT;

                        // Draw background if non-default
                        if (bg > 0 && bg < TERMINAL_COLORS.length) {
                            g2.setColor(TERMINAL_COLORS[bg]);
                            g2.fillRect(x, y, CELL_WIDTH, CELL_HEIGHT);
                        }

                        // Draw character
                        Color fgColor = (fg < TERMINAL_COLORS.length) ? TERMINAL_COLORS[fg] : TERMINAL_COLORS[7];
                        g2.setColor(fgColor);

                        int fontStyle = Font.PLAIN;
                        if ((styles & CellUtils.STYLE_BOLD) != 0) fontStyle |= Font.BOLD;
                        if ((styles & CellUtils.STYLE_ITALIC) != 0) fontStyle |= Font.ITALIC;
                        g2.setFont(g2.getFont().deriveFont(fontStyle));

                        if (ch != ' ') {
                            String s = new String(Character.toChars(ch));
                            g2.drawString(s, x + 1, y + fm.getAscent());
                        }

                        // Underline
                        if ((styles & CellUtils.STYLE_UNDERLINE) != 0) {
                            g2.drawLine(x, y + CELL_HEIGHT - 2, x + CELL_WIDTH, y + CELL_HEIGHT - 2);
                        }
                    }
                }

                // Cursor block
                int cx = buffer.getCursorColumn() * CELL_WIDTH;
                int cy = buffer.getCursorRow() * CELL_HEIGHT;
                g2.setColor(new Color(170, 170, 170, 128));
                g2.fillRect(cx, cy, CELL_WIDTH, CELL_HEIGHT);
            }
        };

        panel.setPreferredSize(new Dimension(COLS * CELL_WIDTH, ROWS * CELL_HEIGHT));
        panel.setFocusable(true);

        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ENTER -> {
                        int nextRow = buffer.getCursorRow() + 1;
                        if (nextRow >= ROWS) {
                            buffer.insertLineAtBottom();
                        } else {
                            buffer.setCursorPosition(0, nextRow);
                        }
                    }
                    case KeyEvent.VK_BACK_SPACE -> {
                        if (buffer.getCursorColumn() > 0) {
                            buffer.moveCursorLeft(1);
                            buffer.writeText(" ");
                            buffer.moveCursorLeft(1);
                        }
                    }
                    case KeyEvent.VK_LEFT -> buffer.moveCursorLeft(1);
                    case KeyEvent.VK_RIGHT -> buffer.moveCursorRight(1);
                    case KeyEvent.VK_UP -> buffer.moveCursorUp(1);
                    case KeyEvent.VK_DOWN -> buffer.moveCursorDown(1);
                    default -> {
                        char c = e.getKeyChar();
                        if (c != KeyEvent.CHAR_UNDEFINED && !e.isActionKey() && c >= 32) {
                            buffer.writeText(String.valueOf(c));
                        }
                    }
                }
                panel.repaint();
            }
        });

        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        panel.requestFocusInWindow();
    }
}
