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
Bit:  63    50 49  48 47      40 39      32 31      24 23             0
      ┌───────┬───┬──┬──────────┬──────────┬──────────┬─────────────────┐
      │ Rsrvd │WC │W │  Styles  │ BG Color │ FG Color │   Character     │
      │(14bit)│(1)│(1)│  (8 bit) │  (8 bit) │  (8 bit) │  (24 bit)      │
      └───────┴───┴──┴──────────┴──────────┴──────────┴─────────────────┘
```

| Field | Bits | Range | Description |
|-------|------|-------|-------------|
| Character | 0..23 | 0 — 16,777,215 | Unicode code point (covers full BMP + supplementary planes, including emoji) |
| Foreground | 24..31 | 0 — 255 | 8-bit color index (16 standard + 240 extended) |
| Background | 32..39 | 0 — 255 | 8-bit color index |
| Styles | 40..47 | bitmask | Bold (bit 0), Italic (bit 1), Underline (bit 2) |
| Wide | 48 | 0 or 1 | Wide character flag — marks the left half of a 2-cell character |
| Wide Cont | 49 | 0 or 1 | Wide continuation flag — marks the right half placeholder |
| Reserved | 50..63 | — | Available for future use |

### Why a Primitive `long`?

- **No heap allocation per cell.** A `Cell` object would cost 16+ bytes of object header alone. A `long` is 8 bytes, stored inline in the array.
- **Cache-friendly.** A `long[]` is a contiguous block of memory. Iterating over cells to render a screen line hits sequential memory — ideal for CPU caches.
- **Fast encoding/decoding.** All operations are bitwise AND, OR, and shifts — single-cycle CPU instructions. No method dispatch, no field access overhead.

### Trade-offs

- 8-bit color limits to 256 colors. Modern terminals support 24-bit true color (RGB). The reserved bits (50..63) could be repurposed for this in a future version.
- Reading individual fields requires calling `CellUtils.getCharacter(cell)` etc., which is less readable than `cell.character`. This is the cost of avoiding object allocation.

## Wide Character Support

CJK ideographs, Hiragana, Katakana, Hangul, fullwidth forms, and emoji occupy 2 terminal cells. The buffer handles this at three layers:

### Cell Layer (`CellUtils`)

- `getDisplayWidth(codePoint)` returns 1 or 2 based on Unicode block ranges. Fast-exits for ASCII (`< 0x1100`).
- `encodeWide()` packs a cell with the wide flag (bit 48) set — marks the left half.
- `createWideContinuation()` creates a placeholder cell with the continuation flag (bit 49) — the right half.

### Line Layer (`Line`)

- **Write**: a wide cell writes both the left half and its continuation. If there's no room (last column), does nothing. Overwriting either half of an existing wide pair cleans up the orphaned half.
- **Insert**: shifts content right by 2, writes the pair. Edge cleanup handles wide pairs split by the shift.
- **Delete**: deleting either half removes both and shifts left by 2.
- **toString / appendTo**: skips continuation cells so wide characters appear once in string output.

### Buffer Layer (`TerminalBuffer`)

- **writeText / insertText**: checks display width. If a wide character would start at the last column, a space is placed there and the cursor wraps to the next line before writing the wide pair.
- **fillLine**: steps by 2 for wide characters. If the line has odd width, the last column gets a space.
- **advanceCursor**: advances by 2 after writing a wide character.

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
- **Write text** — overwrites at cursor position, wraps at line end, scrolls at screen bottom. Wide characters occupy 2 cells and wrap if at the last column.
- **Insert text** — shifts existing content right (last character falls off), wraps and scrolls. Wide characters shift by 2.
- **Fill line** — fills the current row with a character using current attributes. Wide characters step by 2; odd-width lines get a trailing space.
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

- **24-bit true color** — expand the cell layout to support RGB foreground/background using the reserved bits
- **Screen resize** — reflow lines when dimensions change, using soft/hard line break tracking
- **Sparse line implementation** — alternative `BufferLine` that stores only non-empty cells, efficient for mostly-blank screens
- **`Cell` record** — a read-only `record Cell(int character, int fg, int bg, int styles)` for the public API, keeping the raw `long` for internal performance paths

