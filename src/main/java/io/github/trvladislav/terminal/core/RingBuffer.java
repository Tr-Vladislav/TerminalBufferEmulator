package io.github.trvladislav.terminal.core;

public class RingBuffer<T> {

    private final Object[] buffer;
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
        this.buffer = new Object[capacity];
        this.head = 0;
        this.size = 0;
    }

    /**
     * Adds a new element to the buffer. If the buffer is full,
     * the oldest element is overwritten.
     */
    public void push(T element) {
        int tail = (head + size) % capacity;
        buffer[tail] = element;

        if (size < capacity) {
            size++;
        } else {
            head = (head + 1) % capacity;
        }
    }

    /**
     * Retrieves an element by its logical index.
     * Index 0 is always the oldest element currently in the buffer.
     */
    @SuppressWarnings("unchecked")
    public T get(int logicalIndex) {
        if (logicalIndex < 0 || logicalIndex >= size) {
            throw new IndexOutOfBoundsException("Index: " + logicalIndex + ", Size: " + size);
        }

        int physicalIndex = (head + logicalIndex) % capacity;
        return (T) buffer[physicalIndex];
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
