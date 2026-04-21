# Dice Animation Sprite Sheets

## Usage

Each sprite sheet is a horizontal strip of 48 frames (5760×120px, 120×120 per frame).

### Playing the Animation

- **Play direction**: Frame 0 → Frame 47 (forward)
- **Loop**: Frame 47 seamlessly connects back to Frame 0 (full 360° rotation)
- **Result display**: Stop on **Frame 47** to show the final result

### Extracting a Single Frame

```
frameX = result * 120   // where result is 0-47
```

## Files

| Folder | Dice | Files | Color |
|--------|------|-------|-------|
| `d4/` | Tetrahedron (4 faces) | `d4_result_1.png` - `d4_result_4.png` | Blue |
| `d6/` | Cube (6 faces) | `d6_result_1.png` - `d6_result_6.png` | Purple |
| `d8/` | Octahedron (8 faces) | `d8_result_1.png` - `d8_result_8.png` | Orange |
| `d12/` | Dodecahedron (12 faces) | `d12_result_1.png` - `d12_result_12.png` | Gold |

## Perspective

- **View**: Top-down with 15° tilt toward viewer
- **Rotation axis**: Y-axis (spins horizontally)
- **Rotation direction**: Clockwise when viewed from above

## Rolling Direction

As frames advance (0 → 47), the dice **rotates clockwise around its vertical axis** when viewed from above.

When implementing rolling animation:
- Match the sprite's clockwise spin with the dice's physical roll direction
- If the dice rolls **rightward** on screen, the clockwise spin matches naturally
- If the dice rolls **leftward** on screen, the sprite animation would appear to spin backwards (incorrect)

## Number Orientation

- D4/D8 (triangular faces): Numbers point toward the tip vertex
- D6/D12: Numbers centered and upright on each face