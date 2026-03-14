# Terminal Buffer Emulator — Specification

## Task

Implement a terminal text buffer — the core data structure that terminal emulators use to store and manipulate displayed text.

A terminal buffer is a grid of character cells. Each cell stores a character, foreground color, background color, and style flags. The buffer maintains a cursor position indicating where the next character will be written.

The buffer has two logical regions:
- **Screen** — the visible grid (e.g., 80 columns x 24 rows). This is the editable area.
- **Scrollback** — lines that scrolled off the top of the screen, preserved as read-only history.

## Functional Requirements

### Setup
- Configurable screen width and height
- Configurable maximum scrollback size (number of lines)

### Cell Data
Each cell stores:
- Character: Unicode code point (or empty/space)
- Foreground color: default or one of 256 indexed colors
- Background color: default or one of 256 indexed colors
- Style flags: bold, italic, underline (at minimum)

### Attributes
- Set current text attributes: foreground, background, styles
- Reset individual attributes or all at once to defaults
- Current attributes are applied to all subsequent write and insert operations

### Cursor
- Get and set cursor position (column, row), 0-based
- Move cursor: up, down, left, right by N cells
- Cursor must not move outside screen bounds (clamped)

### Editing — Cursor and Attribute Aware
- **Write text** — overwrite content at cursor position using current attributes; advance cursor; wrap to next line at right edge; scroll screen when wrapping past the last row
- **Insert text** — insert at cursor position, shifting existing content right (last character falls off); advance cursor; wrap and scroll as with write
- **Fill line** — fill the current cursor row with a given character using current attributes (cursor position unchanged)
- **Fill line empty** — reset the current cursor row to default empty cells

### Editing — Cursor Independent
- **Insert empty line at bottom** — scroll screen up by one line; top line moves to scrollback; new empty line appears at the bottom
- **Clear screen** — reset all screen lines to empty; reset cursor to (0, 0); preserve scrollback
- **Clear all** — clear screen and scrollback; reset cursor to (0, 0)

### Content Access
- Get character at screen position
- Get full cell data (character + attributes) at screen or scrollback position
- Get a line as a string (screen or scrollback)
- Get entire screen content as a multi-line string
- Get full content (scrollback + screen) as a multi-line string

## Technical Constraints

| Constraint | Value |
|-----------|-------|
| Language | Java 25 |
| Build tool | Maven |
| External libraries | None (except JUnit 5 for testing) |
| Test framework | JUnit Jupiter 5.10 |

## Design Decisions

### Cell Storage: Primitive `long` Bit-Packing

Each cell is stored as a single `long` (64 bits) rather than a `Cell` object.

**Layout:**
```
[0..23]   Character (24 bits — full Unicode)
[24..31]  Foreground color (8 bits — 256 colors)
[32..39]  Background color (8 bits — 256 colors)
[40..47]  Style flags (8 bits — bold, italic, underline)
[48..63]  Reserved (16 bits)
```

**Rationale:**
- Zero object allocation per cell — no GC pressure
- Cache-friendly contiguous `long[]` arrays
- All encode/decode operations are single-cycle bitwise instructions
- An 80x24 screen = 1,920 longs = 15 KB (vs ~77 KB+ with boxed objects)

### Scrollback: Manual Ring Buffer

A fixed-size circular buffer (`RingBuffer`) stores scrollback history.

**Rationale:**
- O(1) push (no shifting, no reallocation)
- O(1) random access by logical index
- Bounded memory — oldest lines are silently overwritten when capacity is reached
- No per-element allocation after buffer initialization

### Line Abstraction: `BufferLine` Interface

Lines are accessed through the `BufferLine` interface, with `Line` as the dense `long[]` implementation.

**Rationale:**
- Allows future alternative implementations (sparse lines, wrapped lines) without changing `TerminalBuffer`
- `Line` constructor is package-private — only `TerminalBuffer` creates lines
- Consumers interact through the interface, not the implementation

### Package Structure

```
buffer/    Screen grid, scrollback, line storage — the facade
cell/      Bit-level cell encoding — independent of buffer structure
cursor/    Position tracking — independent of buffer and cell
```

**Rationale:**
- One concept per package
- Dependency direction is one-way: `buffer` → `cell`, `cursor`
- `RingBuffer` and `Line` constructor are package-private — minimal public surface

## Test Coverage

| Area | Tests | Coverage |
|------|-------|----------|
| Cell encoding/decoding | 9 | All fields, max values, style flags, setters preserve other fields |
| Cursor | 14 | Positioning, clamping, all 4 directions, axis independence, 1x1 edge case |
| Line | 8 | Init, write, insert shift, delete shift, clear, toString, bounds |
| Ring buffer | 7 | Init, push/get, wrap-around, multiple wraps, capacity 1, bounds |
| Terminal buffer | 34 | Setup, attributes, cursor, write/insert/fill, scroll, clear, content access, defaults |
| **Total** | **72** | |

## Possible Extensions

- **24-bit true color** — expand cell layout using reserved bits for RGB channels
- **Wide characters** — CJK and emoji occupying 2 cells, tracked via a reserved bit flag
- **Screen resize** — reflow or truncate lines when terminal dimensions change
- **Sparse line** — alternative `BufferLine` for mostly-empty screens (stores only non-empty cells)
- **Selection and copy** — range selection across screen and scrollback for clipboard operations
