package io.github.trvladislav.terminal.buffer;

class RingBuffer {

    private final BufferLine[] buffer;
    private final int capacity;

    // Index of the oldest element in the buffer
    private int head;

    // Current number of elements in the buffer
    private int size;

    RingBuffer(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity must be not negative");
        }
        this.capacity = capacity;
        this.buffer = new BufferLine[capacity];
        this.head = 0;
        this.size = 0;
    }

    /**
     * Adds a new line to the buffer. If the buffer is full,
     * the oldest line is overwritten.
     */
    public void push(BufferLine line) {
        if (capacity == 0) return;

        int tail = (head + size) % capacity;
        buffer[tail] = line;

        if (size < capacity) {
            size++;
        } else {
            head = (head + 1) % capacity;
        }
    }

    /**
     * Retrieves a line by its logical index.
     * Index 0 is always the oldest line currently in the buffer.
     */
    public BufferLine get(int logicalIndex) {
        if (logicalIndex < 0 || logicalIndex >= size) {
            throw new IndexOutOfBoundsException("Index: " + logicalIndex + ", Size: " + size);
        }

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

    /**
     * Removes all elements from the buffer.
     */
    public void clear() {
        for (int i = 0; i < capacity; i++) {
            buffer[i] = null;
        }
        head = 0;
        size = 0;
    }
}
