package io.github.trvladislav.terminal.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CursorTest {

    private static final int WIDTH = 80;
    private static final int HEIGHT = 24;
    private Cursor cursor;

    @BeforeEach
    void setUp() {
        cursor = new Cursor(WIDTH, HEIGHT);
    }

    @Test
    void testInitialPosition() {
        assertEquals(0, cursor.getColumn());
        assertEquals(0, cursor.getRow());
    }

    @Test
    void testInvalidDimensionsThrow() {
        assertThrows(IllegalArgumentException.class, () -> new Cursor(0, 24));
        assertThrows(IllegalArgumentException.class, () -> new Cursor(80, 0));
        assertThrows(IllegalArgumentException.class, () -> new Cursor(-1, -1));
    }

    // --- setPosition ---

    @Test
    void testSetPosition() {
        cursor.setPosition(10, 5);
        assertEquals(10, cursor.getColumn());
        assertEquals(5, cursor.getRow());
    }

    @Test
    void testSetPositionClampsToUpperBound() {
        cursor.setPosition(200, 100);
        assertEquals(WIDTH - 1, cursor.getColumn());
        assertEquals(HEIGHT - 1, cursor.getRow());
    }

    @Test
    void testSetPositionClampsToLowerBound() {
        cursor.setPosition(-5, -10);
        assertEquals(0, cursor.getColumn());
        assertEquals(0, cursor.getRow());
    }

    // --- moveUp ---

    @Test
    void testMoveUp() {
        cursor.setPosition(0, 10);
        cursor.moveUp(3);
        assertEquals(7, cursor.getRow());
    }

    @Test
    void testMoveUpClampsAtTop() {
        cursor.setPosition(0, 2);
        cursor.moveUp(100);
        assertEquals(0, cursor.getRow());
    }

    // --- moveDown ---

    @Test
    void testMoveDown() {
        cursor.setPosition(0, 10);
        cursor.moveDown(5);
        assertEquals(15, cursor.getRow());
    }

    @Test
    void testMoveDownClampsAtBottom() {
        cursor.setPosition(0, 20);
        cursor.moveDown(100);
        assertEquals(HEIGHT - 1, cursor.getRow());
    }

    // --- moveLeft ---

    @Test
    void testMoveLeft() {
        cursor.setPosition(10, 0);
        cursor.moveLeft(4);
        assertEquals(6, cursor.getColumn());
    }

    @Test
    void testMoveLeftClampsAtLeftEdge() {
        cursor.setPosition(3, 0);
        cursor.moveLeft(100);
        assertEquals(0, cursor.getColumn());
    }

    // --- moveRight ---

    @Test
    void testMoveRight() {
        cursor.setPosition(10, 0);
        cursor.moveRight(5);
        assertEquals(15, cursor.getColumn());
    }

    @Test
    void testMoveRightClampsAtRightEdge() {
        cursor.setPosition(70, 0);
        cursor.moveRight(100);
        assertEquals(WIDTH - 1, cursor.getColumn());
    }

    // --- movement does not affect the other axis ---

    @Test
    void testVerticalMovementPreservesColumn() {
        cursor.setPosition(15, 10);
        cursor.moveUp(3);
        assertEquals(15, cursor.getColumn());
        cursor.moveDown(6);
        assertEquals(15, cursor.getColumn());
    }

    @Test
    void testHorizontalMovementPreservesRow() {
        cursor.setPosition(15, 10);
        cursor.moveLeft(5);
        assertEquals(10, cursor.getRow());
        cursor.moveRight(20);
        assertEquals(10, cursor.getRow());
    }

    // --- edge: 1x1 screen ---

    @Test
    void testMinimalScreen() {
        Cursor tiny = new Cursor(1, 1);
        assertEquals(0, tiny.getColumn());
        assertEquals(0, tiny.getRow());

        tiny.moveRight(1);
        tiny.moveDown(1);
        assertEquals(0, tiny.getColumn());
        assertEquals(0, tiny.getRow());
    }
}
