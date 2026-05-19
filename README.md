# Cosmicon Dice / 银河战力党

A turn-based dice battle card game for Starsector, cloning Honkai: Star Rail's minigame of the same name.

为 Starsector 制作的回合制骰子对战卡牌游戏，复刻自《崩坏：星穹铁道》同名小游戏。

Requires **Console Commands** mod for debugging commands. / 调试命令需要**Console Commands**模组。

Requires **Interastral Peace Casino** mod for stargem draining features. / 星石消耗功能需要**星际和平赌场**模组。

---

## How to Play / 游戏玩法

### Finding Opponents / 寻找对手

Use the comm directory at any market to find a Cosmicon NPC and start a game. Your first two games are tutorial matches that walk you through the basics and unlock your starting characters.

在任何市场的通讯终端中找到战力党NPC即可开始游戏。前两局是教程关卡，会引导你掌握基本操作并解锁初始角色。

If the **Interastral Peace Casino** mod is loaded, a "Cosmicon Dice Lounge" becomes accessible from the Casino main menu after tutorial completion, offering Gatekeeper challenges and Tournaments.

如果加载了**星际和平赌场**模组，完成教程后可从赌场主菜单进入"战力党酒馆"，挑战老赌神或参加挑战赛。

### Battle Basics / 对战基础

Each player picks a **character card** that determines their dice pool, HP, Attack Level, Defense Level, and passive ability. Players alternate between attacking and defending each turn:

每位玩家选择一张**角色卡牌**，决定骰子池、生命值、攻击等级、防御等级和被动技能。双方每回合轮流攻防：

1. **Attack Phase / 攻击阶段** — The attacker rolls all dice, may reroll up to 2 times, then selects exactly **Attack Level** dice. Attack Value = sum of selected dice + bonuses (e.g. Strength stacks).

1. **攻击阶段** — 攻击方投出全部骰子，最多可重投2次，然后必须恰好选定**攻击等级**数量的骰子。攻击值 = 选定骰子点数之和 + 加成（如力量层数）。

2. **Defense Phase / 防御阶段** — The defender rolls and selects exactly **Defense Level** dice (0 rerolls by default). Defense Value = sum of selected dice + bonuses (e.g. Toughness stacks).

2. **防御阶段** — 防御方投骰并选定**防御等级**数量的骰子（默认无重投次数）。防御值 = 选定骰子点数之和 + 加成（如韧性层数）。

3. **Damage / 伤害结算** — If Attack Value > Defense Value, the defender takes damage equal to the difference. If Attack Value <= Defense Value, no damage is dealt.

3. **伤害结算** — 攻击值 > 防御值时，防御方受到差值伤害；攻击值 <= 防御值则不受伤。

Reduce the opponent's HP to 0 to win.

将对手生命值降为0即可获胜。

### Dice Types / 骰子类型

| Type | Faces | Chinese |
|------|-------|---------|
| Blue d4 | 1-4 | 蓝色4面骰 |
| Purple d6 | 1-6 | 紫色6面骰 |
| Orange d8 | 1-8 | 橙色8面骰 |
| Prismatic Dice | Varies | 曜彩骰 |

**Prismatic Dice / 曜彩骰**: Special dice with modified faces and unique effects (e.g. double ATK/DEF, heal HP, gain Combo). Each character comes with a default prismatic dice; some have a "True" upgraded version. Prismatic dice have limited uses per game and may require conditions to be met before they can be rolled.

**曜彩骰**：拥有特殊骰面和独特效果的特殊骰子（如攻击值/防御值翻倍、治愈生命值、获得连击等）。每个角色自带默认曜彩骰，部分拥有"真"升级版本。曜彩骰每局使用次数有限，部分需要满足条件才能投掷。

### Weather / 天气

Starting from turn 2, weather effects activate at turns 2/4/6/8, affecting both players. Weather can boost attackers, defenders, or create chaotic reversals. In free encounters, weather is random; story battles use predetermined weather sequences.

从第2回合起，天气效果在第2/4/6/8回合激活，影响双方。天气可能增强攻击方、防守方，或制造混乱的逆转效果。自由对战中天气随机，剧情对战使用预设天气序列。

---

## Console Commands / 控制台命令

All commands require the Console Commands mod and must be used in the campaign layer (not in menus).

所有命令需要 Console Commands 模组，且必须在战役层使用（不能在菜单中）。

### Battle Commands / 对战命令

| Command | Syntax | Description |
|---------|--------|-------------|
| `cosmicon_start` | `cosmicon_start [opponentId] [prismaticDiceId] [useTrue] [tutorialGameNum]` | Starts a Cosmicon dice battle. Without arguments, starts a standard random battle. `opponentId`: character to fight (`random` for random). `prismaticDiceId`: prismatic dice for opponent. `useTrue`: true/false. `tutorialGameNum`: 1 or 2 to replay tutorial games. |
| `cosmicon_casino_gatekeeper` | `cosmicon_casino_gatekeeper [bonusHp]` | Starts a gatekeeper battle vs Trashcan. `bonusHp`: bonus HP for Trashcan (default: 74). |
| `cosmicon_casino_tournament` | `cosmicon_casino_tournament` | Starts an 8-player double-elimination tournament. |
| `cosmicon_win` | `cosmicon_win` | Forces a player victory in the current active battle. |

| 命令 | 语法 | 说明 |
|------|------|------|
| `cosmicon_start` | `cosmicon_start [对手ID] [曜彩骰ID] [使用真版本] [教程关卡]` | 开始一局对战。不带参数则随机匹配对手。`对手ID`：对手角色（`random`为随机）。`曜彩骰ID`：对手的曜彩骰。`使用真版本`：true/false。`教程关卡`：1或2可重玩教程。 |
| `cosmicon_casino_gatekeeper` | `cosmicon_casino_gatekeeper [额外生命值]` | 开始与垃圾桶的老赌神对战。`额外生命值`：垃圾桶的额外HP（默认：74）。 |
| `cosmicon_casino_tournament` | `cosmicon_casino_tournament` | 开始8人双败淘汰挑战赛。 |
| `cosmicon_win` | `cosmicon_win` | 强制当前对局玩家获胜。 |

### Progress Commands / 进度命令

| Command | Syntax | Description |
|---------|--------|-------------|
| `cosmicon_skip_tutorial` | `cosmicon_skip_tutorial` | Completes the tutorial and unlocks all characters and prismatic dice. |
| `cosmicon_unlock` | `cosmicon_unlock char\|prismatic <id>\|all` | Unlocks characters or prismatic dice. `cosmicon_unlock char <id>` to unlock a specific character. `cosmicon_unlock prismatic <id>` to unlock specific dice. `cosmicon_unlock prismatic true <id>` to unlock the true version. Use `all` to unlock everything at once. |
| `cosmicon_reset` | `cosmicon_reset [all\|stats\|unlocks\|player]` | Resets Cosmicon progress. `all`: reset everything (default). `stats`: reset games played/won. `unlocks`: reset unlocked chars/dice. `player`: reset selected character/dice. |
| `cosmicon_casino_reset` | `cosmicon_casino_reset [all\|hunter\|battle\|tournament]` | Resets casino collab state. `all`: reset everything (default). `hunter`: reset Master Dicer Level. `battle`: clear casino battle state. `tournament`: clear tournament state and lock. |

| 命令 | 语法 | 说明 |
|------|------|------|
| `cosmicon_skip_tutorial` | `cosmicon_skip_tutorial` | 跳过教程，解锁所有角色和曜彩骰。 |
| `cosmicon_unlock` | `cosmicon_unlock char\|prismatic <ID>\|all` | 解锁角色或曜彩骰。`cosmicon_unlock char <ID>` 解锁指定角色。`cosmicon_unlock prismatic <ID>` 解锁指定曜彩骰。`cosmicon_unlock prismatic true <ID>` 解锁真版本。使用 `all` 一键解锁全部。 |
| `cosmicon_reset` | `cosmicon_reset [all\|stats\|unlocks\|player]` | 重置进度。`all`：全部重置（默认）。`stats`：重置胜负场次。`unlocks`：重置已解锁角色/骰子。`player`：重置已选角色/骰子。 |
| `cosmicon_casino_reset` | `cosmicon_casino_reset [all\|hunter\|battle\|tournament]` | 重置赌场联动状态。`all`：全部重置（默认）。`hunter`：重置赌神等级。`battle`：清除赌场对战状态。`tournament`：清除挑战赛状态并锁定。 |

### Info & Debug Commands / 信息与调试命令

| Command | Syntax | Description |
|---------|--------|-------------|
| `cosmicon_status` | `cosmicon_status [verbose]` | Shows current Cosmicon stats: games played/won, unlocks, selected character, tutorial status. `verbose`: also lists all unlocks, config, and memory keys. |
| `cosmicon_config` | `cosmicon_config [show\|set <key> <value>]` | Shows or modifies config values at runtime. `set` keys: `cosmiconDiceEnabled`, `marketSizeMin`, `defaultHP`, `defaultRerolls`, `debugEnabled`, `rerollLogEnabled`. Changes do not persist to file. |
| `cosmicon_casino_status` | `cosmicon_casino_status` | Shows casino collab status: Master Dicer Level, locked reward pools, current battle state, tournament status, and potential boss rewards. |
| `cosmicon_reroll_log` | `cosmicon_reroll_log [on\|off\|toggle]` | Toggles AI reroll decision logging. Without arguments, shows current state. |

| 命令 | 语法 | 说明 |
|------|------|------|
| `cosmicon_status` | `cosmicon_status [verbose]` | 显示当前状态：胜负场次、解锁情况、已选角色、教程进度。`verbose`：额外列出所有解锁项、配置和内存键。 |
| `cosmicon_config` | `cosmicon_config [show\|set <键> <值>]` | 显示或修改运行时配置。可设置的键：`cosmiconDiceEnabled`、`marketSizeMin`、`defaultHP`、`defaultRerolls`、`debugEnabled`、`rerollLogEnabled`。修改不会写入文件。 |
| `cosmicon_casino_status` | `cosmicon_casino_status` | 显示赌场联动状态：赌神等级、未领取奖励池、当前对战状态、挑战赛进度及可能的Boss奖励。 |
| `cosmicon_reroll_log` | `cosmicon_reroll_log [on\|off\|toggle]` | 切换AI重投决策日志。不带参数显示当前状态。 |

### Example Usage / 使用示例

```
# Start a battle vs Firefly with their default prismatic dice
cosmicon_start firefly

# Start a gatekeeper battle (Trashcan +74 HP)
cosmicon_casino_gatekeeper

# Start a gatekeeper battle with +20 bonus HP
cosmicon_casino_gatekeeper 20

# Start a tournament
cosmicon_casino_tournament

# Unlock all characters and prismatic dice
cosmicon_unlock all

# Unlock the true version of a specific prismatic dice
cosmicon_unlock prismatic true repeater

# Check casino collab status
cosmicon_casino_status

# Show full debug status
cosmicon_status verbose

# Toggle AI reroll logging
cosmicon_reroll_log toggle
```

```
# 与流萤对战，使用其默认曜彩骰
cosmicon_start firefly

# 开始老赌神对战（垃圾桶+74HP）
cosmicon_casino_gatekeeper

# 老赌神对战，垃圾桶额外+20HP
cosmicon_casino_gatekeeper 20

# 开始挑战赛
cosmicon_casino_tournament

# 一键解锁全部角色和曜彩骰
cosmicon_unlock all

# 解锁指定曜彩骰的真版本
cosmicon_unlock prismatic true repeater

# 查看赌场联动状态
cosmicon_casino_status

# 显示完整调试信息
cosmicon_status verbose

# 切换AI重投日志
cosmicon_reroll_log toggle
```
