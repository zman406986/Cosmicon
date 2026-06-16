# Cosmicon Dice / 银河战力党

**Version 0.2.0** — A turn-based dice battle card game for Starsector, cloning Honkai: Star Rail's minigame of the same name.

**版本 0.2.0** — 为 Starsector 制作的回合制骰子对战卡牌游戏，复刻自《崩坏：星穹铁道》同名小游戏。

Requires **Console Commands** mod for debugging commands. / 调试命令需要**Console Commands**模组。

Requires **Interastral Peace Casino** mod for stargem draining features. / 星石消耗功能需要**星际和平赌场**模组。

---

## How to Play / 游戏玩法

### Finding Opponents / 寻找对手

Use the comm directory at any market to find a Cosmicon NPC and start a game. Your first two games are tutorial matches that walk you through the basics and unlock your starting characters.

在任何市场的通讯终端中找到战力党NPC即可开始游戏。前两局是教程关卡，会引导你掌握基本操作并解锁初始角色。

If the **Interastral Peace Casino** mod is loaded, a "Cosmicon Dice Lounge" becomes accessible from the Casino main menu after tutorial completion, offering Legend challenges and Tournaments.

如果加载了**星际和平赌场**模组，完成教程后可从赌场主菜单进入"战力党酒馆"，挑战老赌神或参加挑战赛。

### Progression Flow / 游戏进程

```
[Fresh Save]
    |
    v
[Tutorial 1] -- vs Trashcan (using Chimera)
    |   Teaches: rolling, selecting dice, rerolling, character passives
    |   Reward: Unlocks Chimera basic character
    v
[Easy Mode] -- Collect 7 basic characters from bar encounters
    |   No weather, no prismatic dice
    |   Stat bonuses unlock at 3/5/7 collections
    |   Characters: Chimera, Dromas, Automaton Beetle, Trashcan,
    |   |           Furbo Journalist, BananAdvisor, Senior Staff
    v
[Tutorial 2] -- vs Robin (using Acheron)
    |   Teaches: prismatic dice, weather system, advanced strategies
    |   Reward: Unlocks Acheron + Repeater prismatic dice
    v
[Full Game] -- Advanced NPC opponents at bars
    |   Weather & prismatic dice active
    |   Full character selection with stat bonuses
    v
[Casino Collab] -- If Interastral Peace Casino mod loaded
        Challenge the Legend (5,000 stargems)
        Join a Tournament (15,000 stargems)
```

```
[新存档]
    |
    v
[教程1] -- 对战垃圾桶（使用奇美拉）
    |   教学：投骰、选骰、重投、角色被动
    |   奖励：解锁奇美拉基础角色
    v
[简易模式] -- 从酒吧偶遇中收集7个基础角色
    |   无天气、无曜彩骰
    |   收集3/5/7个角色解锁属性加成
    |   角色：奇美拉、巨兽、机甲甲虫、垃圾桶、
    |   |      弗波记者、香蕉顾问、资深员工
    v
[教程2] -- 对战知更鸟（使用黄泉）
    |   教学：曜彩骰、天气系统、进阶策略
    |   奖励：解锁黄泉+复读机曜彩骰
    v
[完整游戏] -- 酒吧高级NPC对手
    |   天气和曜彩骰激活
    |   完整角色选择和属性加成
    v
[赌场联动] -- 若加载星际和平赌场模组
        挑战老赌神（5000星石）
        参加挑战赛（15000星石）
```

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

**Prismatic Dice / 曜彩骰**: Special dice with modified faces and unique effects (e.g. double ATK/DEF, heal HP, gain Combo). Each character comes with a default prismatic dice; some have a "True" upgraded version. Prismatic dice have limited uses per game and may require conditions to be met before they can be rolled (e.g. HP <= 8, after taking 25+ total damage, defense-only, first 4 turns only).

**曜彩骰**：拥有特殊骰面和独特效果的特殊骰子（如攻击值/防御值翻倍、治愈生命值、获得连击等）。每个角色自带默认曜彩骰，部分拥有"真"升级版本。曜彩骰每局使用次数有限，部分需要满足条件才能投掷（如生命值≤8、累计承受25+伤害、仅限防御阶段、仅前4回合等）。

### Weather / 天气

Starting from turn 2, weather effects activate at turns 2/4/6/8, affecting both players. There are 25 weather types across 4 categories:

从第2回合起，天气效果在第2/4/6/8回合激活，影响双方。共25种天气，分为4大类：

| Category | Chinese | Description | 说明 |
|----------|---------|-------------|------|
| Safeguard | 坚守 | Benefits defenders (e.g. Sleet, Blizzard) | 有利防御方（如雨夹雪、暴雪） |
| Attack | 进攻 | Benefits attackers (e.g. Solar Eclipse, Dust) | 有利攻击方（如日食、沙尘） |
| Help | 助力 | Neutral benefits (e.g. Fish Rain, Sea of Clouds) | 中性增益（如鱼雨、云海） |
| Reversal | 逆转 | Punishes or reverses normal flow (e.g. Acid Rain, Temporal Storm) | 惩罚或逆转常规流程（如酸雨、时空暴） |

In free encounters, weather is random; story battles use predetermined weather sequences.

自由对战中天气随机，剧情对战使用预设天气序列。

### Status Effects / 状态效果

| Effect | Chinese | Description | 说明 |
|--------|---------|-------------|------|
| Strength | 力量 | ATK bonus = stack count | 攻击值加成等于层数 |
| Toughness | 韧性 | DEF bonus = stack count | 防御值加成等于层数 |
| Poison | 中毒 | End of turn: DMG = stack count, then -1 stack | 回合结束时受到等于层数的伤害，然后减少1层 |
| Combo | 连击 | Extra attack based on current ATK value | 根据当前攻击值进行额外攻击 |
| Counter | 反击 | If DEF > ATK, deal difference DMG to attacker | 若防御值>攻击值，对攻击方造成差值伤害 |
| Forcefield | 力场 | Negates conventional attack damage | 抵消常规攻击伤害 |
| Perforation | 洞穿 | Ignores DEF and Forcefield | 无视防御值和力场 |
| Thorns | 荆棘 | Before resolution: DMG = stack count | 伤害结算前：造成等于层数的伤害 |
| Unyielding | 不屈 | Always retain 1 HP while active | 激活期间生命值不会低于1 |
| Hack | 骇入 | Turn opponent's highest die into 2 | 将对手最高的骰子变为2 |
| Arise | 跃升 | Transform lowest die to that die's max value | 将最低的骰子变为该骰子的最大值 |
| Siphon | 虹吸 | Recover HP = percentage of DMG dealt | 回复等于造成伤害百分比的HP |
| Overload | 超载 | +ATK per stack but self-damage on defense | 每层增加攻击值，但防御时自伤 |
| Venom | 猛毒 | Poison damage becomes 2x | 毒伤害翻倍 |
| Instant Damage | 瞬伤 | Bypasses damage calculation | 跳过伤害计算直接造成伤害 |
| Awakening | 觉醒 | Double all dice values after selection | 选定骰子后所有骰子点数翻倍 |
| Destined | 命定 | Dice with this effect must be selected | 带此效果的骰子必须被选中 |
| Last Stand | 背水 | Reduce HP to 1, gain bonus = HP reduction | 生命值降为1，获得等于减少量的加成 |

### Bonus System / 加成系统

Collecting basic characters in Easy Mode unlocks permanent stat bonuses:
- **+9 HP** option (unlocked after collecting 3 basic characters)
- **+1 ATK Level** (max 5, unlocked after collecting 5 basic characters)
- **+1 DEF Level** (max 5, unlocked after collecting 7 basic characters)

在简易模式中收集基础角色可解锁永久属性加成：
- **+9 HP** 选项（收集3个基础角色后解锁）
- **+1 攻击等级**（上限5，收集5个基础角色后解锁）
- **+1 防御等级**（上限5，收集7个基础角色后解锁）

### Characters / 角色

#### Basic Characters (7 — Easy Mode) / 基础角色（7个——简易模式）

| ID | Name | HP | Dice Pool | ATK | DEF | Passive | 被动技能 |
|----|------|----|-----------|-----|-----|---------|----------|
| chimera | Chimera | 22 | 2d6, 2d4 | 3 | 2 | Matching numbers: +3 ATK (4s: +7) | 相同点数：攻击值+3（全为4时：+7） |
| dromas | Dromas | 26 | 2d6, 2d4 | 3 | 2 | All even: 2 Poison stacks | 全偶数：2层毒 |
| automaton_beetle | Automaton Beetle | 10 | 1d6, 3d4 | 3 | 3 | 3 consecutives: Forcefield + 8 Strength | 三连：力场+8层力量 |
| trashcan_basic | Trashcan | 25 | 2d6, 3d4 | 3 | 2 | All even: 4 Strength (else 2) | 全偶数：4层力量（否则2层） |
| furbo_journalist | Furbo Journalist | 28 | 1d6, 4d4 | 4 | 3 | All odd: 4 Instant Dmg (else 2) | 全奇数：4点即时伤害（否则2点） |
| bananadvisor | BananAdvisor | 24 | 2d6, 3d4 | 4 | 3 | No DMG: Heal 5; HP<=5: DEF+1 | 未受伤：回复5HP；HP≤5：防御等级+1 |
| senior_staff | Senior Staff | 22 | 1d8, 1d6, 3d4 | 3 | 2 | Distinct numbers: +1 ATK/DEF each | 不同点数：每个+1攻击/防御值 |

#### Advanced Characters (14 — Full Game) / 进阶角色（14个——完整游戏）

| ID | Name | HP | Dice Pool | Prismatic | ATK | DEF | Passive Summary | 被动技能 |
|----|------|----|-----------|-----------|-----|-----|-----------------|----------|
| acheron | Acheron | 33 | 1d8, 1d6, 3d4 | Repeater x2 | 2 | 3 | All 4s = Perforation, ATK+1 | 全为4：贯穿，攻击值+1 |
| castorice | Castorice | 27 | 2d8, 1d6, 2d4 | Doctor's Advice x2 | 3 | 2 | DMG>=8: ATK/DEF+1; DMG<=5: 3 Instant Dmg | 伤害≥8：攻击/防御值+1；伤害≤5：3点即时伤害 |
| firefly | Firefly | 28 | 3d6, 2d4 | Sorcerer x2 | 4 | 3 | 2 pairs: Combo; Full HP: +5 ATK | 两对：连击；满血：攻击值+5 |
| robin | Robin | 30 | 2d6, 3d4 | None | 4 | 3 | All even: Level Up dice (up to d12) | 全偶数：骰子升级（最高d12） |
| the_herta | The Herta | 42 | 2d8, 3d6 | Berserker x2 | 3 | 2 | +1 Prismatic use/turn; 4+ triggers: Arise | 每回合曜彩骰使用次数+1；4个以上触发：兴起 |
| kafka | Kafka | 30 | 2d6, 3d4 | Prime Number x2 | 4 | 3 | Different numbers: Poison stacks | 不同点数：毒层数 |
| aventurine | Aventurine | 33 | 1d8, 3d6, 1d4 | Prime Number x2 | 4 | 2 | Odd numbers: Toughness; 7 stacks: 7 Instant Dmg | 奇数：韧性层数；7层：7点即时伤害 |
| march_7th | March 7th | 25 | 2d6, 3d4 | Magic Bullet x2 | 4 | 3 | Pairs: 3 Instant Damage each | 对子：每对3点即时伤害 |
| dan_heng | Dan Heng | 25 | 2d8, 3d6 | Sorcerer x2 | 3 | 2 | ATK>=18: Counter + DEF+3 next defense | 攻击值≥18：反击+下次防御值+3 |
| sparxie | Sparxie | 22 | 1d8, 2d6, 2d4 | Sorcerer x2 | 4 | 3 | Identical numbers: Hack (highest die -> 2) | 相同点数：骇入（最高骰子变为2） |
| yao_guang | Yao Guang | 35 | 2d8, 3d6 | Destiny x2 | 3 | 2 | 4 rerolls; >2 rerolls: Thorns; ATK>=18: cleanse + Prismatic use | 4次重投；>2次：反伤；攻击值≥18：净化+曜彩骰使用 |
| cyrene | Cyrene | 30 | 1d8, 3d6, 1d4 | Gambler x2 | 3 | 2 | Cumulative ATK+DEF >24: ATK Lv5 + Arise | 累计攻击+防御值>24：攻击等级5+兴起 |
| phainon | Phainon | 20 | 2d8, 3d6 | Astral Shield x2 | 4 | 2 | Siphon 50%; All same: Unyielding (1/game) | 吸血50%；全相同：不屈（每局1次） |
| hyacine | Hyacine | 28 | 1d8, 4d6 | Oath x2 | 2 | 2 | Strength = 50% ATK; All 6s: 100% + Heal 6 | 力量=50%攻击值；全为6：100%吸血+回复6HP |

### Prismatic Dice / 曜彩骰

| ID | Name | Condition | Effect | 触发条件 | 效果 |
|----|------|-----------|--------|----------|------|
| evolution | Evolution | Always | 2 pts, Double ATK/DEF | 始终可用 | 2点，攻击/防御值翻倍 |
| absolute_six | Absolute Six | Always | Always 6 | 始终可用 | 始终为6 |
| destiny | Destiny | Always | Points + Destined (must select) | 始终可用 | 点数+命运（必须选中） |
| revenge | Revenge | After 25+ total DMG taken | High-value faces | 累计承受25+伤害后 | 高点数骰面 |
| doctors_advice | Doctor's Advice | Always | Points + Heal equal to face value | 始终可用 | 点数+回复等于骰面值的HP |
| last_words | Last Words | HP <= 8 | Points + Double ATK/DEF | HP≤8时 | 点数+攻击/防御值翻倍 |
| repeater | Repeater | After selecting face 4 twice | 4 pts + Combo | 选中骰面4两次后 | 4点+连击 |
| cactus | Cactus | Defense only | Points + Counter | 仅防御阶段 | 点数+反击 |
| miracle | Miracle | After selecting 1 nine times | All 99s | 选中骰面1九次后 | 全为99 |
| loan | Loan | Always | Points + Overload stacks | 始终可用 | 点数+超载层数 |
| astral_shield | Astral Shield | Defense only | 1 pt + Forcefield | 仅防御阶段 | 1点+力场 |
| oath | Oath | Defense only | Points + Unyielding | 仅防御阶段 | 点数+不屈 |
| prime_number | Prime Number | Always | Always prime values (3,5,7) | 始终可用 | 始终为质数（3,5,7） |
| big_red_button | Big Red Button | Turn 5+, attacking | Points + Last Stand | 第5回合起，攻击阶段 | 点数+穷途 |
| sorcerer | Sorcerer | Always | Points + Hack | 始终可用 | 点数+骇入 |
| heartbeat | Heartbeat | Always | 9 pts +1 Prismatic use | 始终可用 | 9点+曜彩骰使用次数+1 |
| berserker | Berserker | Always | Points + Thorns stacks | 始终可用 | 点数+反伤层数 |
| gambler | Gambler | First 4 turns | Balanced values | 前4回合 | 均衡数值 |
| magic_bullet | Magic Bullet | Always | Points + 3 Instant Damage | 始终可用 | 点数+3点即时伤害 |

---

## Console Commands / 控制台命令

All commands require the Console Commands mod and must be used in the campaign layer (not in menus).

所有命令需要 Console Commands 模组，且必须在战役层使用（不能在菜单中）。

### Battle Commands / 对战命令

| Command | Syntax | Description |
|---------|--------|-------------|
| `cosmicon_start` | `cosmicon_start [opponentId] [prismaticDiceId] [useTrue] [tutorialGameNum]` | Starts a Cosmicon dice battle. Without arguments, starts a standard random battle. `opponentId`: character to fight (`random` for random). `prismaticDiceId`: prismatic dice for opponent. `useTrue`: true/false. `tutorialGameNum`: 1 or 2 to replay tutorial games. |
| `cosmicon_casino_legend` | `cosmicon_casino_legend [bonusHp]` | Starts a legend battle. `bonusHp`: bonus HP for the opponent (default: 74). |
| `cosmicon_casino_tournament` | `cosmicon_casino_tournament` | Starts an 8-player double-elimination tournament. |
| `cosmicon_win` | `cosmicon_win` | Forces a player victory in the current active battle. |

| 命令 | 语法 | 说明 |
|------|------|------|
| `cosmicon_start` | `cosmicon_start [对手ID] [曜彩骰ID] [使用真版本] [教程关卡]` | 开始一局对战。不带参数则随机匹配对手。`对手ID`：对手角色（`random`为随机）。`曜彩骰ID`：对手的曜彩骰。`使用真版本`：true/false。`教程关卡`：1或2可重玩教程。 |
| `cosmicon_casino_legend` | `cosmicon_casino_legend [额外生命值]` | 开始赌神对战。`额外生命值`：对手的额外HP（默认：74）。 |
| `cosmicon_casino_tournament` | `cosmicon_casino_tournament` | 开始8人双败淘汰挑战赛。 |
| `cosmicon_win` | `cosmicon_win` | 强制当前对局玩家获胜。 |

### Progress Commands / 进度命令

| Command | Syntax | Description |
|---------|--------|-------------|
| `cosmicon_skip_tutorial` | `cosmicon_skip_tutorial` | Completes the tutorial and unlocks all characters and prismatic dice. |
| `cosmicon_unlock` | `cosmicon_unlock char\|prismatic <id>\|all` | Unlocks characters or prismatic dice. `cosmicon_unlock char <id>` to unlock a specific character. `cosmicon_unlock prismatic <id>` to unlock specific dice. `cosmicon_unlock prismatic true <id>` to unlock the true version. Use `all` to unlock everything at once. |
| `cosmicon_reset` | `cosmicon_reset [all\|stats\|unlocks\|player]` | Resets Cosmicon progress. `all`: reset everything (default). `stats`: reset games played/won. `unlocks`: reset unlocked chars/dice. `player`: reset selected character/dice. |
| `cosmicon_casino_reset` | `cosmicon_casino_reset [all\|legend\|battle\|tournament]` | Resets casino collab state. `all`: reset everything (default). `legend`: reset Master Dicer Level. `battle`: clear casino battle state. `tournament`: clear tournament state and lock. |

| 命令 | 语法 | 说明 |
|------|------|------|
| `cosmicon_skip_tutorial` | `cosmicon_skip_tutorial` | 跳过教程，解锁所有角色和曜彩骰。 |
| `cosmicon_unlock` | `cosmicon_unlock char\|prismatic <ID>\|all` | 解锁角色或曜彩骰。`cosmicon_unlock char <ID>` 解锁指定角色。`cosmicon_unlock prismatic <ID>` 解锁指定曜彩骰。`cosmicon_unlock prismatic true <ID>` 解锁真版本。使用 `all` 一键解锁全部。 |
| `cosmicon_reset` | `cosmicon_reset [all\|stats\|unlocks\|player]` | 重置进度。`all`：全部重置（默认）。`stats`：重置胜负场次。`unlocks`：重置已解锁角色/骰子。`player`：重置已选角色/骰子。 |
| `cosmicon_casino_reset` | `cosmicon_casino_reset [all\|legend\|battle\|tournament]` | 重置赌场联动状态。`all`：全部重置（默认）。`legend`：重置赌神等级。`battle`：清除赌场对战状态。`tournament`：清除挑战赛状态并锁定。 |

### Info & Debug Commands / 信息与调试命令

| Command | Syntax | Description |
|---------|--------|-------------|
| `cosmicon_status` | `cosmicon_status [verbose]` | Shows current Cosmicon stats: games played/won, mode, unlocks, bonuses, selected character, tutorial status. `verbose`: also lists all unlocks, config, and `$cos_*` memory keys. |
| `cosmicon_config` | `cosmicon_config [show\|set <key> <value>]` | Shows or modifies config values at runtime. Settable keys: `cosmiconDiceEnabled`, `marketSizeMin`, `defaultHP`, `defaultRerolls`, `debugEnabled`, `verboseEnabled`, `rerollLogEnabled`. Changes do not persist to file. |
| `cosmicon_debug` | `cosmicon_debug [off\|debug\|verbose\|reroll]` | Unified debug logging control. `off`: disable all. `debug`: enable tier 2 (battle flow, damage, status effects). `verbose`: enable tier 2 + tier 3 (AI internals, dice rest, animations). `reroll`: toggle AI reroll decision logging. Without arguments, shows current state. |
| `cosmicon_reroll_log` | `cosmicon_reroll_log [on\|off\|toggle]` | Toggles AI reroll decision logging. Without arguments, shows current state. |
| `cosmicon_casino_status` | `cosmicon_casino_status` | Shows casino collab status: Master Dicer Level, locked reward pools, current battle state, tournament status, and potential boss rewards. |

| 命令 | 语法 | 说明 |
|------|------|------|
| `cosmicon_status` | `cosmicon_status [verbose]` | 显示当前状态：胜负场次、游戏模式、解锁情况、加成、已选角色、教程进度。`verbose`：额外列出所有解锁项、配置和 `$cos_*` 内存键。 |
| `cosmicon_config` | `cosmicon_config [show\|set <键> <值>]` | 显示或修改运行时配置。可设置的键：`cosmiconDiceEnabled`、`marketSizeMin`、`defaultHP`、`defaultRerolls`、`debugEnabled`、`verboseEnabled`、`rerollLogEnabled`。修改不会写入文件。 |
| `cosmicon_debug` | `cosmicon_debug [off\|debug\|verbose\|reroll]` | 统一调试日志控制。`off`：关闭所有。`debug`：启用二级日志（战斗流程、伤害、状态效果）。`verbose`：启用二级+三级日志（AI内部、骰子重置、动画）。`reroll`：切换AI重投日志。不带参数显示当前状态。 |
| `cosmicon_reroll_log` | `cosmicon_reroll_log [on\|off\|toggle]` | 切换AI重投决策日志。不带参数显示当前状态。 |
| `cosmicon_casino_status` | `cosmicon_casino_status` | 显示赌场联动状态：赌神等级、未领取奖励池、当前对战状态、挑战赛进度及可能的Boss奖励。 |

### Example Usage / 使用示例

```
# Start a battle vs Firefly with their default prismatic dice
cosmicon_start firefly

# Start a legend battle (opponent +74 HP)
cosmicon_casino_legend

# Start a legend battle with +20 bonus HP
cosmicon_casino_legend 20

# Start a tournament
cosmicon_casino_tournament

# Unlock all characters and prismatic dice
cosmicon_unlock all

# Unlock the true version of a specific prismatic dice
cosmicon_unlock prismatic true repeater

# Set debug level to verbose
cosmicon_debug verbose

# Toggle AI reroll logging (via unified command)
cosmicon_debug reroll

# Toggle AI reroll logging (legacy command)
cosmicon_reroll_log toggle

# Show full debug status
cosmicon_status verbose

# Check casino collab status
cosmicon_casino_status

# Show runtime config
cosmicon_config show
```

```
# 与流萤对战，使用其默认曜彩骰
cosmicon_start firefly

# 开始赌神对战（对手+74HP）
cosmicon_casino_legend

# 赌神对战，对手额外+20HP
cosmicon_casino_legend 20

# 开始挑战赛
cosmicon_casino_tournament

# 一键解锁全部角色和曜彩骰
cosmicon_unlock all

# 解锁指定曜彩骰的真版本
cosmicon_unlock prismatic true repeater

# 设置调试级别为详细
cosmicon_debug verbose

# 切换AI重投日志（统一命令）
cosmicon_debug reroll

# 切换AI重投日志（旧命令）
cosmicon_reroll_log toggle

# 显示完整调试信息
cosmicon_status verbose

# 查看赌场联动状态
cosmicon_casino_status

# 显示运行时配置
cosmicon_config show
```
