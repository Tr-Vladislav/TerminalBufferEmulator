# Terminal Buffer Emulator

A terminal text buffer implementation in Java — the core data structure that terminal emulators use to store and manipulate displayed text.

When a shell sends output, the terminal emulator updates this buffer, and a UI layer renders it. This project implements the buffer layer with no external dependencies (JUnit 5 for testing only).

## Architecture

The project is organized into three packages with a clear one-way dependency flow:

```
io.github.trvladislav.terminal
├── buffer    TerminalBuffer (facade), BufferLine, Line, RingBuffer
├── cell      CellUtils (bit-packed cell encoding/decoding)
└── cursor    Cursor (position tracking with bounds clamping)
```

`buffer` depends on `cell` and `cursor`. `cell` and `cursor` are independent of each other. No circular dependencies.

### Key Components

| Class | Visibility | Role |
|-------|-----------|------|
| `TerminalBuffer` | public | Main facade — the only class most consumers need |
| `BufferLine` | public (interface) | Abstraction for a single line, returned from accessors |
| `Line` | public (pkg-private constructor) | Dense `long[]` implementation of `BufferLine` |
| `RingBuffer` | package-private | Internal circular buffer for scrollback history |
| `CellUtils` | public | Bit-level cell encoding: character + colors + styles in a single `long` |
| `Cursor` | public | Cursor position tracker, clamped to screen bounds |

## Cell Encoding: Packing into a Primitive `long`

Each terminal cell (character + foreground color + background color + style flags) is packed into a single **64-bit `long`** value. This avoids object allocation per cell — an 80x24 screen is just a flat `long[1920]` array with zero GC pressure.

### Memory Layout

```
Bit:  63        48 47      40 39      32 31      24 23             0
      ┌──────────┬──────────┬──────────┬──────────┬─────────────────┐
      │ Reserved │  Styles  │ BG Color │ FG Color │   Character     │
      │ (16 bit) │  (8 bit) │  (8 bit) │  (8 bit) │  (24 bit)      │
      └──────────┴──────────┴──────────┴──────────┴─────────────────┘
```

| Field | Bits | Range | Description |
|-------|------|-------|-------------|
| Character | 0..23 | 0 — 16,777,215 | Unicode code point (covers full BMP + supplementary planes, including emoji) |
| Foreground | 24..31 | 0 — 255 | 8-bit color index (16 standard + 240 extended) |
| Background | 32..39 | 0 — 255 | 8-bit color index |
| Styles | 40..47 | bitmask | Bold (bit 0), Italic (bit 1), Underline (bit 2) |
| Reserved | 48..63 | — | Available for wide-char flags, hyperlinks, etc. |

### Why a Primitive `long`?

- **No heap allocation per cell.** A `Cell` object would cost 16+ bytes of object header alone. A `long` is 8 bytes, stored inline in the array.
- **Cache-friendly.** A `long[]` is a contiguous block of memory. Iterating over cells to render a screen line hits sequential memory — ideal for CPU caches.
- **Fast encoding/decoding.** All operations are bitwise AND, OR, and shifts — single-cycle CPU instructions. No method dispatch, no field access overhead.

### Trade-offs

- 8-bit color limits to 256 colors. Modern terminals support 24-bit true color (RGB). The 16 reserved bits (48..63) could be repurposed for this in a future version.
- Reading individual fields requires calling `CellUtils.getCharacter(cell)` etc., which is less readable than `cell.character`. This is the cost of avoiding object allocation.

## Manual Ring Buffer for Scrollback

Scrollback history is stored in a **manually implemented circular buffer** (`RingBuffer`). When the screen scrolls, the top line is pushed into the ring buffer. Once the buffer reaches its configured capacity, the oldest line is silently overwritten.

### How It Works

```
Capacity = 4, after pushing lines A, B, C, D, E:

Physical array:  [ E | B | C | D ]
                   ^
                   head (oldest = B)

Logical view:    [0]=B  [1]=C  [2]=D  [3]=E
```

- `head` points to the oldest element
- `size` tracks how many slots are occupied (up to `capacity`)
- `push()` writes to `(head + size) % capacity` and advances `head` when full
- `get(i)` translates logical index to physical: `(head + i) % capacity`

### Why Not `ArrayDeque` or `LinkedList`?

- **`ArrayDeque`** doesn't support indexed access (`get(i)`). Scrollback requires random access to render any visible portion of history.
- **`LinkedList`** has O(n) indexed access and heavy per-node allocation.
- **`ArrayList`** with manual removal at index 0 is O(n) per scroll — the ring buffer is O(1).
- A manual ring buffer gives O(1) push and O(1) random access with zero allocation after initialization.

## Features

### Attributes
- Set foreground color, background color, and style flags (bold, italic, underline)
- Individual setters and reset-to-default for each attribute
- Attributes are applied to all subsequent write/insert operations

### Cursor
- Absolute positioning with `setCursorPosition(column, row)`
- Relative movement: `moveCursorUp/Down/Left/Right(n)`
- All positions are clamped to screen bounds — cursor never leaves the visible area

### Editing
- **Write text** — overwrites at cursor position, wraps at line end, scrolls at screen bottom
- **Insert text** — shifts existing content right (last character falls off), wraps and scrolls
- **Fill line** — fills the current row with a character using current attributes
- **Insert line at bottom** — scrolls screen up, top line moves to scrollback
- **Clear screen** — resets all screen lines, preserves scrollback
- **Clear all** — resets screen and scrollback

### Content Access
- Get character or full cell data at any screen/scrollback position
- Get any line as a string
- Get entire screen or full content (scrollback + screen) as a multi-line string

## Building and Running

### Prerequisites
- Java 25
- Maven wrapper is included — no separate Maven installation required

### Build and Test
```bash
./mvnw clean test
```

### Run Demo UI
```bash
./mvnw compile exec:java -Dexec.mainClass="io.github.trvladislav.terminal.Main"
```

The demo opens a Swing window with an 80x24 terminal grid. Type to write, use arrow keys to move, Enter for new lines, Backspace to delete.

## Project Structure

```
src/
├── main/java/io/github/trvladislav/terminal/
│   ├── Main.java                          Swing demo application
│   ├── buffer/
│   │   ├── TerminalBuffer.java            Main facade
│   │   ├── BufferLine.java                Line interface
│   │   ├── Line.java                      Dense long[] line implementation
│   │   └── RingBuffer.java                Circular buffer (package-private)
│   ├── cell/
│   │   └── CellUtils.java                 Cell bit-packing utilities
│   └── cursor/
│       └── Cursor.java                    Cursor position tracker
└── test/java/io/github/trvladislav/terminal/
    ├── buffer/
    │   ├── TerminalBufferTest.java         34 tests
    │   ├── LineTest.java                   8 tests
    │   └── RingBufferTest.java             7 tests
    ├── cell/
    │   └── CellUtilsTest.java             9 tests
    └── cursor/
        └── CursorTest.java                14 tests
```

## Possible Improvements

- **24-bit true color** — expand the cell layout to support RGB foreground/background using the 16 reserved bits
- **Wide character support** — CJK characters and emoji that occupy 2 cells, using a reserved bit as a wide-char flag
- **Screen resize** — reflow or truncate lines when dimensions change
- **Sparse line implementation** — alternative `BufferLine` that stores only non-empty cells, efficient for mostly-blank screens
- **`Cell` record** — a read-only `record Cell(int character, int fg, int bg, int styles)` for the public API, keeping the raw `long` for internal performance paths

