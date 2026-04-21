# Cosmicon, Roll On! / 银河战力党 - Game Reference

## Overview

**Cosmicon, Roll On!** (Chinese: **银河战力党**) is a card-based dice game introduced in Honkai: Star Rail version 4.0 (February 2026). It's a turn-based battle game where players use character cards and dice to attack and defend against opponents.



## Core Mechanics

### Turn Structure

Each turn follows a strict sequential flow:

1. **Attacker Phase**:
   - Attacker rolls all dice from their dice pool
   - Attacker **Reroll Phase**: Can select any number of dice to reroll (up to 2 times by default, unless abilities/weather grant more)
   - After rerolls (or skipping reroll), attacker must select exactly **Attack Level** dice
   - Attacker confirms selection (cannot confirm unless exact Attack Level dice are selected)
   - Attack Value is calculated from selected dice + bonuses

2. **Defender Phase**:
   - Defender rolls all dice from their dice pool (happens AFTER attacker confirms)
   - Defender **Reroll Phase**: 0 rerolls by default (unless abilities/weather grant more)
   - Defender must select exactly **Defense Level** dice
   - Defender confirms selection (cannot confirm unless exact Defense Level dice are selected)
   - Defense Value is calculated from selected dice + bonuses

3. **Damage Resolution**:
   - If Attack Value > Defense Value: Damage = Attack Value - Defense Value
   - If Attack Value ≤ Defense Value: No damage dealt
   - Turn ends, roles swap for next turn (attacker becomes defender)

- Cards have passive abilities triggered by dice patterns
- Players alternate between Attack and Defense roles each turn

### Dice System
* **Attack Level**: The exact number of dice that MUST be selected when attacking.
* **Defense Level**: The exact number of dice that MUST be selected when defending.
* **Rerolling**:
    * **Attacker**: Can reroll any number of dice up to 2 times per turn (default). Select dice, then use reroll - all selected dice will be rerolled.
    * **Defender**: 0 rerolls by default (unless abilities/weather grant reroll attempts).
    * After using all rerolls or skipping reroll phase, proceed to dice selection.
    * *(Note: Certain status effects or weather conditions can modify base reroll count for either side).*
* **Dice Types**:
    * **Blue d4**: 4-sided dice (faces 1-4)
    * **Purple d6**: 6-sided dice (faces 1-6)
    * **Orange d8**: 8-sided dice (faces 1-8)
    * **Prismatic Dice**: Special dice with modified faces and custom effects (see [Prismatic_Dice_Index.md](Prismatic_Dice_Index.md))

### Key Stats
- **HP**: Start with base HP (typically 20-30)
- **Attack Value (攻击值)**: Sum of selected dice faces + bonuses (Strength stacks, card effects, weather)
- **Defense Value (防御值)**: Sum of selected dice faces + bonuses (Toughness stacks, card effects)
- **Attack Level (攻击等级)**: Number of dice to select when attacking
- **Defense Level (防御等级)**: Number of dice to select when defending

### Damage Calculation

**Core formula:**
- **Damage** = Attack Value - Defense Value (when attack > defense)
- **Instant Damage (瞬伤)**: Immediate damage, bypasses damage resolution phase

**Special modifiers:**
- **Perforation (洞穿)**: Ignores opponent's defense value and Forcefield effect
- **Forcefield (力场)**: Does not receive conventional attack damage while active

### Status Effects

See [Status_Effects.md](Status_Effects.md) for full status effect details.



## Character Cards

See [Card_Index.md](Card_Index.md) for full character card details.

## Prismatic Dice

See [Prismatic_Dice_Index.md](Prismatic_Dice_Index.md) for full prismatic dice details.

## Weather System

See [Weather_System.md](Weather_System.md) for full weather system details.

## Story Battles

See [Story_Battles.md](Story_Battles.md) for full story battle details.
