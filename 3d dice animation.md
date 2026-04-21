# 3D Dice Animation Implementation Plan

## Goal

Replace current jitter-based dice animation with sprite-based rolling animation that simulates 3D dice rotation from top-down view.

## Overview

- **48 frames per die type** - full rotation cycle
- **Edge views included** - realistic 3D tumble between faces
- **Numbers on faces** - matching current DiceType colors
- **Pre-rendered sprites** - generated via Three.js, loaded at runtime

---

## Architecture

```
┌────────────────────────────────────────────────────────────┐
│  OFFLINE: Three.js Sprite Generation                       │
│  tools/generate_dice_sprites.js                            │
│  → graphics/cosmicon/dice/d{N}_roll_48frames.png           │
└────────────────────────────────────────────────────────────┘
                         ↓
┌────────────────────────────────────────────────────────────┐
│  RUNTIME: Starsector Mod                                   │
│                                                            │
│  DiceSpriteSheet.java                                      │
│  - Loads sprite sheet                                      │
│  - Provides frame-by-frame rendering                       │
│  - Handles texture sub-region cropping                     │
│                                                            │
│  DiceSpriteAnimator.java                                   │
│  - Animation state machine (DROP → ROLL → SETTLE)          │
│  - Frame selection logic                                   │
│  - Transform handling (scale, position)                    │
│                                                            │
│  DiceMotion.java                                           │
│  - Easing curves                                           │
│  - Face-to-frame mapping                                   │
│                                                            │
│  DiceRollManager.java                                      │
│  - Orchestrates multiple dice rolling concurrently         │
│  - Timing/delay management                                 │
└────────────────────────────────────────────────────────────┘
```

---

## Three.js Sprite Generation

### Setup Requirements

- Node.js environment
- Packages: `three`, `canvas` (node-canvas for headless rendering)
- Output: PNG sprite sheets

### Die Geometry Mapping

| Die Type | Three.js Geometry | Notes |
|----------|-------------------|-------|
| D4 | `TetrahedronGeometry` | 4 triangular faces, needs custom face numbering placement |
| D6 | `BoxGeometry` | Standard cube, easiest to texture |
| D8 | `OctahedronGeometry` | 8 triangular faces |
| D12 | `DodecahedronGeometry` | 12 pentagonal faces, most complex |

### Rotation Strategy

**48 frames = one full rotation cycle**

- Rotation axis: Y-axis (vertical spin from top-down)
- Additional tilt: slight X-axis rotation (~15-20°) to show 3D depth
- Frame distribution: Evenly spaced angles across 360°

**Frame angle calculation:**
```
frameAngle = (frameIndex / 48) * 360°
tiltAngle = 15° (constant for depth perception)
```

### Camera Setup

```
OrthographicCamera
- Position: (0, 10, 0) looking down
- View: Top-down projection
- No perspective distortion

Orthographic vs Perspective:
- Orthographic: cleaner, easier sprite alignment
- Perspective: more realistic 3D feel, harder to align frames
→ Use orthographic for predictable sprite dimensions
```

### Lighting

- Ambient: 50% gray - base visibility
- Directional: 100% white from top - face highlight
- Optional: Secondary light from side - edge definition

### Face Numbering Placement

**Challenge:** Each die geometry has different face shapes and orientations.

**Approach:**
1. Calculate face center positions from geometry
2. Create canvas textures with numbers at correct positions
3. Apply as UV-mapped textures to mesh

**Number styling:**
- Font: Bold sans-serif (Arial or similar)
- Color: Match DiceType.numberColor (WHITE for most, GOLD for D12)
- Background: Match DiceType.bodyColor
- Size: Proportional to face size (larger on D4, smaller on D12)

### Edge/Gap Frames

Between major face positions, frames should show:
- Thin edge/side view of the die
- Slight color shift (darker to indicate edge)
- Motion blur optional (can be simulated in runtime instead)

**Edge detection:** If current angle is not aligned to a face normal, render darker.

### Output Format

**Sprite sheet layout:**
```
Option A: Horizontal strip (recommended)
┌────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬...┬────┐
│ F0 │ F1 │ F2 │ F3 │ F4 │ F5 │ F6 │ F7 │ F8 │ F9 │F10 │F11 │... │F47 │
└────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴...┴────┘
Width: 48 * frameWidth
Height: frameHeight

Option B: Grid (4x12 or 6x8)
- Harder to index programmatically
- Use Option A
```

**Frame dimensions:**
- Recommended: 60x60 px per frame (matches current DICE_SIZE)
- Total sheet: 2880x60 px
- Transparent background (PNG with alpha)

**Naming convention:**
```
graphics/cosmicon/dice/d4_roll_48frames.png
graphics/cosmicon/dice/d6_roll_48frames.png
graphics/cosmicon/dice/d8_roll_48frames.png
graphics/cosmicon/dice/d12_roll_48frames.png
```

### Three.js Script Structure

```
1. Setup: Scene, camera, renderer (with node-canvas)
2. Create die mesh for each type
   - Geometry selection
   - Texture generation (numbered faces)
   - Material setup
3. Rotation loop (48 iterations)
   - Set rotation angles
   - Render frame
   - Capture to canvas
4. Stitch all frames into single PNG
5. Output to file
```

### Script Output Location

```
tools/generate_dice_sprites.js
tools/node_modules/ (dependencies)
tools/package.json
```

**Run command:** `node tools/generate_dice_sprites.js`

---

## Starsector Runtime Implementation

### DiceSpriteSheet.java

**Purpose:** Load and manage sprite sheet, provide per-frame rendering.

**Key considerations:**

| Challenge | Solution |
|-----------|----------|
| Starsector doesn't support sub-texture rendering directly | Use `SpriteAPI.setRenderSize()` and render offset, or implement custom GL rendering with texture coordinates |
| Frame indexing from sheet | Calculate frameX = frameIndex * frameWidth, render from that offset |
| Multiple die types | Create separate sheet instances per DiceType, or single registry |

**Implementation approach:**

```
Option A: Use SpriteAPI with texture region
- SpriteAPI.renderAtCenter() doesn't support sub-region
- Need to use GL11.glTranslatef() + texture coordinate manipulation

Option B: Load each frame as separate sprite (48 sprites per die)
- Memory heavy (192 sprites total)
- Simple indexing

Option C: Custom GL rendering with texture coordinates
- Most efficient
- Requires GL11.glTexCoord2f() calls
→ Use Option C for memory efficiency
```

**Custom GL rendering pattern:**
```
GL11.glEnable(GL11.GL_TEXTURE_2D);
sheet.bindTexture();  // via Global.getSettings().getSprite()

float frameU0 = frameIndex / 48f;
float frameU1 = (frameIndex + 1) / 48f;
float frameV0 = 0f;
float frameV1 = 1f;

GL11.glBegin(GL11.GL_QUADS);
GL11.glTexCoord2f(frameU0, frameV1); GL11.glVertex2f(x, y);
GL11.glTexCoord2f(frameU1, frameV1); GL11.glVertex2f(x + w, y);
GL11.glTexCoord2f(frameU1, frameV0); GL11.glVertex2f(x + w, y + h);
GL11.glTexCoord2f(frameU0, frameV0); GL11.glVertex2f(x, y + h);
GL11.glEnd();
```

**Gotcha:** Starsector's SpriteAPI texture binding is internal. Use:
```java
// Get internal texture path, then use GL directly
// Or: Create custom Sprite that wraps the sheet
```

### DiceSpriteAnimator.java

**Purpose:** Manage animation state, frame progression, transforms.

**State machine:**

| State | Duration | Behavior |
|-------|----------|----------|
| DROP | 0.15-0.2s | Scale from 0.3→1.0, Y offset descending |
| ROLL | 0.4-0.5s | Play frames, decelerating speed, stop at target frame |
| SETTLE | 0.1-0.15s | Scale bounce (1.0→1.1→0.95→1.0), optional glow |

**Total duration:** ~0.7s per die

**Frame progression during ROLL:**

```
Phase 1: Fast rotation (0-0.2s of ROLL)
  - Frames advance rapidly
  - May loop multiple times (speed > 48 frames / duration)

Phase 2: Deceleration (0.2-0.4s of ROLL)
  - Frame advance slows
  - Approach target frame

Phase 3: Final approach (0.4-0.5s of ROLL)
  - Land exactly on targetFrame
```

**Speed curve:**
```
easeOutQuad or easeOutCubic for deceleration
frameProgress = easeOutQuad(timeProgress)
currentFrame = floor(frameProgress * totalFramesToPlay) % 48
```

**Stopping on correct face:**

```
Challenge: Need to stop on frame that shows finalValue

Solution:
1. Calculate which frame shows which face value
2. During ROLL, ensure we land on targetFrame

Mapping required:
faceToFrame[DiceType][faceValue] = frameIndex

This mapping is geometry-dependent - must match Three.js rotation pattern.
```

**Multiple dice timing:**

```
DiceRollManager handles stagger:
- Each die starts with delay offset (0.05s increments)
- Prevents visual chaos
- Total animation still ~0.7s + stagger
```

### DiceMotion.java

**Purpose:** Utility functions for easing and face-to-frame mapping.

**Easing functions:**

```
// Drop entrance - easeOutBack for bouncy feel
easeOutBack(t):
  const c1 = 1.70158;
  const c3 = c1 + 1;
  return 1 + c3 * pow(t - 1, 3) + c1 * pow(t - 1, 2);

// Roll deceleration - easeOutCubic
easeOutCubic(t):
  return 1 - pow(1 - t, 3);

// Settle bounce - elastic
easeOutElastic(t):
  // Standard elastic ease, overshoots then settles
```

**Face-to-frame mapping:**

```
// CRITICAL: Must match Three.js rotation exactly
// This is determined during sprite generation

// Example structure (will vary by geometry):
int[][] FACE_TO_FRAME = {
  // D4: face 1→frame 0, face 2→frame 12, face 3→frame 24, face 4→frame 36
  {0, 12, 24, 36},
  // D6: faces at frames 0, 8, 16, 24, 32, 40 (8-frame intervals)
  {0, 8, 16, 24, 32, 40},
  // D8: similar pattern
  // D12: 4-frame intervals
};

// Actual mapping requires testing/calibration after sprites generated
```

**Calibration approach:**
```
1. Generate sprites
2. Manually inspect which frame shows which face
3. Document mapping
4. Use in DiceMotion.faceToFrame()
```

### Integration with Existing Code

**Files to modify:**

| File | Changes |
|------|---------|
| `DiceRollManager.java` | Replace DiceAnimator usage with DiceSpriteAnimator |
| `mod_data.json` or similar | Register sprite sheet paths |
| `CosmiconDiceModPlugin.java` | Initialize sprite sheets on mod load |

**Replace pattern:**

```
Current:
DiceAnimator animator = new DiceAnimator();
animator.start(type, value, x, y, delay);

New:
DiceSpriteAnimator animator = new DiceSpriteAnimator(type);
animator.start(value, x, y, delay);
```

**Fallback option:**

Keep DiceAnimator as fallback for:
- Low-spec systems (sprite loading failed)
- Debug mode
- Prismatic dice special effects (may need custom handling)

---

## Key Technical Decisions

### 1. Sprite vs Real-time 3D

| Factor | Pre-rendered Sprites | Real-time 3D |
|--------|---------------------|--------------|
| Memory | ~200KB total (4 PNGs) | Minimal (geometry data) |
| CPU | Very low (just sprite render) | Higher (matrix math per frame) |
| Complexity | Low (once sprites generated) | High (projection, matrices) |
| Flexibility | Fixed frames, no variation | Infinite rotation angles |
| Starsector compatibility | Excellent (uses existing sprite system) | Requires GL state management |

**Decision: Pre-rendered sprites** - Lower runtime complexity, better compatibility.

### 2. Frame Count (48 for all)

**Why 48 for all dice:**

- Uniform animation speed across dice types
- D4 (4 faces): Each face appears ~12 frames apart
- D6 (6 faces): Each face appears ~8 frames apart
- D8 (8 faces): Each face appears ~6 frames apart
- D12 (12 faces): Each face appears ~4 frames apart

**D12 speed concern:** D12 rotates visually faster because faces change every 4 frames instead of 12. This is acceptable - reflects real physics (more faces = faster visual rotation).

### 3. Sprite Sheet vs Individual Frames

| Approach | Memory | Complexity | Flexibility |
|----------|--------|------------|-------------|
| Sprite sheet (1 per die) | Optimal | Medium (texture coords) | Good |
| Individual frames (48 files) | High (192 files) | Low (direct load) | Poor |
| Grid sheet (4x12) | Optimal | High (grid indexing) | Good |

**Decision: Horizontal strip sprite sheet** - Best balance of memory and indexing simplicity.

### 4. Drop/Settle Animation

**Drop entrance:**

- Purpose: Visual "arrival" cue
- Duration: Short (0.15s) - doesn't interrupt gameplay
- Scale: easeOutBack (overshoots slightly, feels physical)

**Settle bounce:**

- Purpose: Final value emphasis
- Duration: Very short (0.1s)
- Scale: Small bounce (1.0→1.08→0.96→1.0)
- Optional: Glow/brightness pulse

### 5. Face-to-Frame Mapping Calibration

**This is the most critical calibration step.**

The Three.js script must produce predictable face positions, and Starsector must know exactly which frame to stop on for each face value.

**Two approaches:**

| Approach | Pros | Cons |
|----------|------|------|
| Manual calibration (generate, inspect, document) | Accurate, simple | Requires manual work |
| Programmatic (script outputs mapping JSON alongside PNG) | Automated, reproducible | More script complexity |

**Recommendation:** Script outputs mapping JSON:
```json
{
  "d4": { "1": 0, "2": 12, "3": 24, "4": 36 },
  "d6": { "1": 0, "2": 8, "3": 16, "4": 24, "5": 32, "6": 40 },
  ...
}
```

Starsector loads this JSON for accurate frame mapping.

---

## Gotchas and Edge Cases

### 1. GL State Management

**Current code pattern (from DiceAnimator):**
```
GLStateUtil.resetBlendState() at start of render
GL11.glDisable(GL11.GL_TEXTURE_2D) for quads
GL11.glEnable(GL11.GL_TEXTURE_2D) before sprites
```

**New requirement:** Sprite rendering always needs textures enabled. Don't forget:
```
GL11.glEnable(GL11.GL_TEXTURE_2D);
// Then render sprite frame
GL11.glDisable(GL11.GL_TEXTURE_2D); // Reset for subsequent non-textured draws
```

### 2. Coordinate System

**UI panel Y=0 at TOP, OpenGL Y=0 at BOTTOM.**

Use CoordHelper for all conversions:
```
CoordHelper.uiToGlY(panelY, panelHeight, uiY)
```

**Sprite render Y must use GL coordinates.**

### 3. Transparency

PNG sprites will have transparent background. Ensure:
```
GL11.glEnable(GL11.GL_BLEND);
GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
```

Before any sprite render.

### 4. Memory Management

Sprite sheets loaded once at mod init, not per-battle:
```
// In CosmiconDiceModPlugin.onApplicationLoad()
DiceSpriteSheetRegistry.init();
```

Don't load sprites repeatedly - memory leak risk.

### 5. D12 Prismatic Dice

Prismatic dice may have special visual effects:
- Glow/halo
- Color shift
- Particle effects

**Decision:** Handle prismatic as overlay effect on base sprite, not separate sprite sheet.

### 6. Multiple Dice Overlap

If dice positions overlap during rolling:
- Z-order matters
- Render in reverse order (last die on top)
- Or: ensure positions don't overlap

### 7. Animation Interruption

If battle state changes mid-animation (e.g., player clicks button):
- Allow animation to complete or force-complete
- Don't leave animation state hanging
- Implement `forceComplete()` that jumps to final frame instantly

---

## Testing Strategy

### Phase 1: Placeholder Sprites

Before Three.js script is ready:
- Use simple colored circles/squares as placeholder
- Validate DiceSpriteAnimator state machine
- Test timing, drop/settle curves
- Test multi-dice stagger

### Phase 2: Single Die Type

Generate D6 first (easiest geometry):
- Validate frame-to-face mapping
- Check rotation speed feels natural
- Test edge frame appearance

### Phase 3: All Die Types

Generate remaining dice:
- Validate all face mappings
- Check D12 doesn't look too fast
- Verify D4 triangle faces render correctly

### Phase 4: Integration

- Replace in actual battle UI
- Test with player interaction
- Verify no performance impact
- Check on different screen scales (Starsector's `getScreenScaleMult()`)

### Phase 5: Polish

- Adjust timing curves if needed
- Add settle bounce/glow if desired
- Verify prismatic dice overlay effects work

---

## Alternative Approaches (If Issues Arise)

### Alternative A: Blender Python Script

If Three.js setup is problematic:
- Blender has built-in Python API
- Can render from scripts
- More robust 3D geometry handling
- Export as image sequence

### Alternative B: Manual 2D Drawing

If all 3D generation fails:
- Draw 48 frames manually for each die
- Use vector art tool (Inkscape, Illustrator)
- Time-consuming but guaranteed to work

### Alternative C: Simplified Rotation

If full 48-frame rotation is too complex:
- Reduce to 12-16 frames
- Skip edge frames
- Simpler "spinning" instead of tumbling

### Alternative D: Hybrid Procedural

If sprite generation fails:
- Pre-render face textures only (4-12 per die)
- Runtime: Composite face onto rotating polygon
- Less convincing 3D, but simpler assets

---

## File Structure Summary

```
Cosmicon Dice/
├── tools/
│   ├── generate_dice_sprites.js     # Three.js sprite generator
│   ├── package.json                 # Node dependencies
│   └── dice_mapping.json            # Output: frame-to-face mapping
│
├── graphics/cosmicon/dice/
│   ├── d4_roll_48frames.png         # 2880x60 sprite sheet
│   ├── d6_roll_48frames.png
│   ├── d8_roll_48frames.png
│   └── d12_roll_48frames.png
│
├── src/data/scripts/cosmicon/
│   ├── battle/
│   │   ├── DiceSpriteSheet.java     # NEW: Sprite management
│   │   ├── DiceSpriteAnimator.java  # NEW: Animation state machine
│   │   ├── DiceMotion.java          # NEW: Easing/mapping utils
│   │   ├── DiceRollManager.java     # MODIFY: Use new animator
│   │   └── DiceAnimator.java        # KEEP: Fallback/legacy
│   │
│   └── util/
│       ├── DiceSpriteSheetRegistry.java  # NEW: Global sheet registry
│       └── CoordHelper.java              # EXISTING: Use for coords
│       └── GLStateUtil.java              # EXISTING: Use for GL state
```

---

## Time Estimates (For Experienced Coder)

| Task | Estimated Time |
|------|----------------|
| Three.js script setup & basic rendering | 2-3 hours |
| Face numbering texture generation | 1-2 hours |
| Frame mapping calibration | 1 hour |
| DiceSpriteSheet.java (GL texture rendering) | 1-2 hours |
| DiceSpriteAnimator.java (state machine) | 2-3 hours |
| DiceMotion.java (easing, mapping) | 1 hour |
| Integration & testing | 2-3 hours |
| Polish & adjustments | 1-2 hours |
| **Total** | **10-15 hours** |

---

## Dependencies

**Three.js generation:**
- Node.js (v14+)
- npm packages: `three`, `canvas`

**Starsector runtime:**
- No new dependencies
- Uses existing GL11, SpriteAPI

---

## Success Criteria

1. All 4 die types have 48-frame sprite sheets
2. Animation plays smoothly at 60fps in Starsector
3. Final face value always matches stopped frame
4. Drop/settle animation feels physical
5. No memory leaks or performance issues
6. Works on different screen scales
7. Prismatic dice overlay effects work correctly