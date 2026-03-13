package io.github.trvladislav.terminal.core;

public class RingBuffer {

    private final Line[] buffer;
    private final int capacity;

    // Index of the oldest element in the buffer
    private int head;

    // Current number of elements in the buffer
    private int size;

    public RingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        this.capacity = capacity;
        this.buffer = new Line[capacity];
        this.head = 0;
        this.size = 0;
    }

    /**
     * Adds a new line to the buffer. If the buffer is full,
     * the oldest line is overwritten.
     */
    public void push(Line line) {
        // Calculate the physical index for the new element (the "tail")
        int tail = (head + size) % capacity;
        buffer[tail] = line;

        if (size < capacity) {
            size++; // Buffer is not full yet, just increase size
        } else {
            // Buffer is full. The tail just overwrote the head.
            // Move the head forward to point to the new "oldest" element.
            head = (head + 1) % capacity;
        }
    }

    /**
     * Retrieves a line by its logical index.
     * Index 0 is always the oldest line currently in history.
     */
    public Line get(int logicalIndex) {
        if (logicalIndex < 0 || logicalIndex >= size) {
            throw new IndexOutOfBoundsException("Index: " + logicalIndex + ", Size: " + size);
        }

        // Translate the logical index to the actual array index
        int physicalIndex = (head + logicalIndex) % capacity;
        return buffer[physicalIndex];
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return capacity;
    }

    public boolean isFull() {
        return size == capacity;
    }
}