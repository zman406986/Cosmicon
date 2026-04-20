# Cosmicon, Roll On! / 银河战力党 - Game Reference

## Overview

**Cosmicon, Roll On!** (Chinese: **银河战力党**) is a card-based dice game introduced in Honkai: Star Rail version 4.0 (February 2026). It's a turn-based battle game where players use character cards and dice to attack and defend against opponents.



## Core Mechanics

### Turn Structure
- Players alternate between **Attack** and **Defense** phases
- Each turn, roll dice and select dice faces to determine Attack Value or Defense Value
- Attack Value > Defense Value = damage dealt
- Cards have passive abilities triggered by dice patterns

### Dice System
* **Attack Level**: The exact number of dice that MUST be selected when attacking.
* **Defense Level**: The exact number of dice that MUST be selected when defending.
* **Rerolling**:
    * On an attacking turn, you can reroll any number of dice a maximum of two times. Defender cannot reroll unless ability allows so.
    * By selecting dice and using the reroll feature, all selected dice will be rerolled. 
    * *(Note: Certain status effects or weather conditions can modify this base reroll count).*
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
