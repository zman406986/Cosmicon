# Cosmicon Dice

A turn-based dice battle card game for Starsector, cloning Honkai: Star Rail's minigame of the same name.

Requires **Console Commands** mod for debugging commands.
Requires **Interastral Peace Casino** mod for stargem draining features.

## Console Commands

All commands require the Console Commands mod and must be used in the campaign layer (not in menus).

### Battle Commands

| Command                     | Syntax                                                                      | Description                                                                                                                                                                                                                                                           |
|-----------------------------|-----------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `cosmicon_start`            | `cosmicon_start [opponentId] [prismaticDiceId] [useTrue] [tutorialGameNum]` | Starts a Cosmicon dice battle. Without arguments, starts a standard random battle. `opponentId`: character to fight (`random` for random). `prismaticDiceId`: prismatic dice for opponent. `useTrue`: true/false. `tutorialGameNum`: 1 or 2 to replay tutorial games. |
| `cosmicon_casino_boss`      | `cosmicon_casino_boss [opponentId] [bonusHp]`                               | Starts a casino boss battle simulation. `opponentId`: specific opponent (default: random). `bonusHp`: bonus HP for opponent (default: 15).                                                                                                                            |
| `cosmicon_casino_challenge` | `cosmicon_casino_challenge [bonusHp]`                                       | Starts a casino challenge battle vs Trashcan. `bonusHp`: bonus HP for Trashcan (default: 74).                                                                                                                                                                         |

### Progress Commands

| Command                  | Syntax                                         | Description                                                                                                                                                                                         |
|--------------------------|------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `cosmicon_skip_tutorial` | `cosmicon_skip_tutorial`                       | Completes the tutorial and unlocks all characters and prismatic dice.                                                                                                                               |
| `cosmicon_unlock`        | `cosmicon_unlock char\|prismatic <id>\|all`    | Unlocks characters or prismatic dice. `cosmicon_unlock char <id>` to unlock a specific character. `cosmicon_unlock prismatic <id>` to unlock specific dice. Use `all` to unlock everything at once. |
| `cosmicon_reset`         | `cosmicon_reset [all\|stats\|unlocks\|player]` | Resets Cosmicon progress. `all`: reset everything (default). `stats`: reset games played/won. `unlocks`: reset unlocked chars/dice. `player`: reset selected character/dice.                        |
| `cosmicon_casino_reset`  | `cosmicon_casino_reset [all\|hunter\|battle]`  | Resets casino collab state. `all`: reset everything (default). `hunter`: reset Trashcan Hunter Level. `battle`: clear casino battle state.                                                          |

### Info Commands

| Command                  | Syntax                                      | Description                                                                                                                                                                    |
|--------------------------|---------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `cosmicon_status`        | `cosmicon_status [verbose]`                 | Shows current Cosmicon stats: games played/won, unlocks, selected character, tutorial status. `verbose`: also lists all unlocks, config, and memory keys.                      |
| `cosmicon_config`        | `cosmicon_config [show\|set <key> <value>]` | Shows or modifies config values at runtime. `set` keys: `cosmiconDiceEnabled`, `marketSizeMin`, `defaultHP`, `defaultRerolls`, `debugEnabled`. Changes do not persist to file. |
| `cosmicon_casino_status` | `cosmicon_casino_status`                    | Shows casino collab status: Trashcan Hunter Level, locked reward pools, current battle state, and potential boss rewards.                                                      |

### Example Usage

```
# Start a battle vs Firefly with their default prismatic dice
cosmicon_start firefly

# Start a casino boss battle with +20 bonus HP
cosmicon_casino_boss random 20

# Start a casino challenge (Trashcan +74 HP)
cosmicon_casino_challenge

# Unlock all characters
cosmicon_unlock all

# Check casino collab status
cosmicon_casino_status

# Show full debug status
cosmicon_status verbose
```