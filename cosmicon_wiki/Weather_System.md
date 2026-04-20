# Cosmicon, Roll On!/Weather System

Weather affects both players and changes at start of turn 2/4/6/8. Effects are predetermined for story battles, random for free co-op matches. NPC duels only use "The Decisive Moment".

## Weather Tags

| Tag | Chinese | Description (EN) | Description (CN) |
|-----|---------|------------------|------------------|
| Safeguard | 坚守 | Benefits defenders | 有利于防守方 |
| Attack | 进攻 | Benefits attackers | 有利于进攻方 |
| Help | 助力 | Neutral benefits | 中立效果 |
| Reversal | 逆转 | Punishes or reverses normal flow | 惩罚或逆转正常流程 |

## Weather Effects

| Name | Chinese | Type | Turn | Description (EN) | Description (CN) |
|------|---------|------|------|------------------|------------------|
| Frost | 霜 | Safeguard | 2 | If attacker's dice have matching numbers, defender's DEF Level +1 next turn | 若攻击方选择的骰子中包含相同点数，则下回合其防御等级 +1 |
| Cyclonic Swarm | 飓风 | Attack | 2 | Both sides gain Combo when attacking | 双方攻击时均获得连击 |
| Blizzard | 暴雪 | Safeguard | 4 | DEF ≤8: gain Forcefield this turn | 防御方选定骰子时，若防御值≤8点，则其在本回合获得力场 |
| Frog Rain | 青蛙雨 | Help | 2 | Neither side rolls minimum dice value | 双方所有骰子均不会掷出最小值 |
| Fish Rain | 雨 | Help | 2 | Both sides gain +1 reroll | 双方在攻击/防御时，都会额外获得1次重投机会 |
| Solar Eclipse | 日食 | Attack | 2 | Different dice values: Attack +4 | 双方攻击时，若选择的骰子中包含不同点数，则攻击值 +4 |
| Storm | 暴雨 | Attack | 4 | Both sides ATK/DEF Level +1 | 双方额外攻击等级+1，防御等级+1 |
| Drizzle | 小雨 | Help | ? | Dice contain 6: remove Poison from self | 选定骰子时，若包含6，则消除身上的中毒状态 |
| Moderate Snow | 中雪 | Help | 4 | 3 identical dice: heal 10 HP | 双方攻击或防御时，若选择的骰子包含3个相同点数，则治愈10点生命值 |
| Heavy Snow | 大雪 | Attack | 4 | Dice contain 7: ATK/DEF +4 | 选定骰子时，若包含7，则攻击值/防御值+4 |
| Dust | 沙尘 | Reversal | 4 | All odd dice: gain 3 Strength | 攻击方选定骰子时，若点数全为奇数，则获得3层力量 |
| Temporal Storm | 时空暴 | Help | 6 | All dice = 6: swap HP of both players | 攻击方选定骰子时，若点数全为6，双方生命值将互换 |
| Sleet | 雨夹雪 | Help | 2 | Non-full HP: gain Counter + DEF Level +2 | 生命值不为满的玩家，获得反击，且防御等级+2 |
| Toxic Fog | 毒雾 | Reversal | 8 | Both sides: 2 Poison | 双方均被附加2层中毒 |
| Venocloud | 毒云 | Attack | ? | Both sides gain Venom | 双方获得猛毒 |
| Sea of Clouds | 云海 | Attack | 6 | On weather change: both gain 1 Prismatic Dice use | 变换至此天气时，双方获得1次曜彩骰使用次数 |
| Sunny | 晴 | Attack | 8 | Both sides: 5 Strength (until weather ends) | 双方获得5层力量，持续到本次天气结束 |
| Rainbow | 彩虹 | Attack | 6 | Attack ≤10: gain Perforation | 攻击方选定骰子时，若攻击值≤10，获得洞穿 |
| Dry Thunderstorm | 晴雷 | Reversal | 8 | Attacker selects dice: deal 3 Instant Damage | 攻击方选定骰子时，直接造成3点瞬伤 |
| Drought | 干旱 | Reversal | 6 | Defender's DEF Level = +3 ATK per level for attacker | 根据对方防御等级，每一级攻击方附加3点攻击值 |
| Scorching Sun | 烈日 | Safeguard | 4 | Siphon 50% HP of damage dealt | 双方攻击时，虹吸造成伤害50%的生命值 |
| The Decisive Moment | 决胜时刻 | Attack | ? | Both sides: 5 Strength | 双方获得5层力量 |
| Acid Rain | 雨 | Safeguard | 4 | Start of turn: player with more HP gets 1 Poison | 每回合开始时：场上生命值更多的一方，会被附加1层中毒 |
| High Temperature | 高温 | Attack | 4 | Start of turn: player with less HP gets 2 Strength (until weather ends) | 回合开始时：场上生命值更少的一方，获得2层力量，持续到本次天气结束 |
| Fine Snow | 细雪 | Safeguard | 2 | No reroll during attack: gain 3 Toughness (next turn only) | 攻击回合若未重投过，则获得3层仅下回合可用的韧性 |
| Sunshower | 晴天雨 | Attack | 6 | Defender's dice can't roll max value | 防御方骰子无法掷出最大值 |
| Lunisolar Luminance | 日月同辉 | Reversal | 6 | Current HP ≤3: Attack doubles | 攻击方选定骰子时，若当前生命值≤3，攻击值翻倍 |
| Crepuscular Rays | 云隙光 | Attack | 6 | Player with less HP: gain Combo when attacking | 生命值更少的玩家，攻击时获得连击 |
| Parhelion | 幻日 | Attack | 2 | Gain 2 rerolls, each reroll = 2 Thorns | 额外多2次重投机会，但重投时会被施加2层荆棘 |
| Thunderstorm | 雷雨 | Help | 2 | Gliding adds +2 ATK/DEF per level | 在此天气下，卡片每经过一次镀闪，攻击值/防御值+2 |