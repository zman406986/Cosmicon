package data.scripts.cosmicon.tutorial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.util.CosmiconLogger;

public class TutorialDiceRoller {

    private final TutorialController controller;
    private int playerRollIndex;
    private int opponentRollIndex;
    private final Set<String> completedRerolls;
    private final Set<String> completedOpponentRerollSteps;
    private int opponentRerollCount;
    private int opponentSelectionCount;
    private int playerRerollCount;

    // Tutorial Game 1: Chimera (player) vs Trashcan (opponent)
    // Player dice: 2d6 + 2d4 (4 total), ATK Lv 3, DEF Lv 2
    // Opponent dice: 2d6 + 3d4 (5 total), ATK Lv 3, DEF Lv 2
    //
    // Flow: T1Def → T1Atk → T2Def → T2Atk → T3Def → T3Atk → Victory
    //
    // T1 Defense: Player [5,4,3,1] all unique → DEF 9. Opp attacks [4,3,3,3,2] → ATK 10, DMG 1.
    // T1 Attack: Player [6,5,2,1] all unique → reroll lowest 2 (d4s) → [3,2] → [6,5,3,2] → reroll lowest 2 (d4s) → [4,3] → [6,5,4,3] all unique.
    //   Opp defends [3,2,1,1,1] → DEF 5, DMG 16. Teaches: ATK reroll (2 rerolls)
    // T2 Defense: Player [4,3,2,1] all unique → DEF 7. Opp attacks [4,3,1,1,1] → rerolls 1s → [3,3,2]
    //   → [4,3,3,3,2] ATK 10 + 2 Strength = 12, DMG 5. Teaches: opponent Strength passive (mouse over)
    // T2 Attack: Player [1,3,4,1] pair of 1s → reroll 1s → [6,4] → [6,4,4,3] pair of 4s → +7 ATK
    //   Opp defends [4,3,1,1,1] → DEF 7, DMG 14 (ATK 21-7). Teaches: Chimera passive ability
    // T3 Defense: Player [4,3,2,1] → DEF 7. Opp attacks [5,3,1,1,1] → rerolls 1s → [4,3,2]
    //   → [5,3,4,3,2] ATK 12 + 4 Strength = 16, DMG 9. Teaches: dice types, HP from cards
    // T3 Attack: Player [6,5,4,3] no rerolls left → ATK 18. Opp defends [3,2,1,1,1] → DEF 8, DMG 10.
    //   Total damage: 10+14+10=34 > 25 HP. Victory! Even two 6s can't block this.
    private static final int[][] GAME1_PLAYER_ROLLS = {
        {5, 4, 3, 1},   // T1 Defense (all unique, no matching)
        {6, 5, 2, 1},   // T1 Attack (before reroll, all unique)
        {4, 3, 2, 1},   // T2 Defense (all unique, ability not yet taught)
        {1, 3, 4, 1},   // T2 Attack (before reroll, pair of 1s)
        {4, 3, 2, 1},   // T3 Defense (no new mechanics)
        {6, 5, 4, 3}    // T3 Attack (no rerolls left, even two 6s can't block)
    };

    private static final int[][] GAME1_OPPONENT_ROLLS = {
        {4, 3, 3, 3, 2}, // T1 Attack (no reroll on T1)
        {3, 2, 1, 1, 1}, // T1 Defense
        {4, 3, 1, 1, 1}, // T2 Attack (before reroll, +2 Strength)
        {4, 3, 1, 1, 1}, // T2 Defense
        {5, 3, 1, 1, 1}, // T3 Attack (before reroll, +4 Strength)
        {3, 2, 1, 1, 1}  // T3 Defense
    };

    // Opponent rerolls during T2, T3 attack phases (no reroll on T1).
    // T2: from [4,3,1,1,1] reroll 1s (indices 2,3,4) → [3,3,2] → [4,3,3,3,2]
    // T3: from [5,3,1,1,1] reroll 1s (indices 2,3,4) → [4,3,2] → [5,3,4,3,2]
    private static final int[][] GAME1_OPPONENT_REROLL_INDICES = {
        {2, 3, 4},
        {2, 3, 4}
    };
    private static final int[][] GAME1_OPPONENT_REROLL_RESULTS = {
        {3, 3, 2},
        {4, 3, 2}
    };

    // T1 Attack reroll 1: player rerolls the 2 lowest dice (1,2) → [3,2]
    // After reroll: [6,5,3,2] all unique
    // T1 Attack reroll 2: player rerolls the 2 lowest dice (2,3) → [4,3]
    // After reroll: [6,5,4,3] all unique
    // T2 Attack reroll: player rerolls the pair of 1s → [6,4]
    // After reroll: [6,4,4,3] pair of 4s → +7 ATK
    private static final int[][] GAME1_REROLL_RESULTS = {
        {3, 2},        // T1 reroll 1: d4s get 3,2 (was 5,3 — exceeded d4 max)
        {4, 3},        // T1 reroll 2: d4s get 4,3 (was 6,5 — exceeded d4 max)
        {6, 4}         // T2 reroll: d6 gets 6, d4 gets 4
    };

    // Opponent selections: which dice indices the opponent selects
    // T1 Attack (ATK Lv 3): from [4,3,3,3,2] select [0,1,2] → ATK=10
    // T1 Defense (DEF Lv 2): from [3,2,1,1,1] select [0,1] → DEF=5
    // T2 Attack (ATK Lv 3): from [4,3,1,1,1] reroll 1s → [3,3,2] → [4,3,3,3,2] select [0,1,2] → ATK=10 (+2 Strength=12)
    // T2 Defense (DEF Lv 2): from [4,3,1,1,1] select [0,1] → DEF=7
    // T3 Attack (ATK Lv 3): from [5,3,1,1,1] reroll 1s → [4,3,2] → [5,3,4,3,2] select [0,1,2] → ATK=12 (+4 Strength=16)
    // T3 Defense (DEF Lv 2): from [3,2,1,1,1] select [0,1] → DEF=5
    private static final int[][] GAME1_OPPONENT_SELECTIONS = {
        {0, 1, 2},       // T1 Attack (after reroll)
        {0, 1},          // T1 Defense
        {0, 1, 2},       // T2 Attack (after reroll)
        {0, 1},          // T2 Defense
        {0, 1, 2},       // T3 Attack (after reroll)
        {0, 1}           // T3 Defense
    };

    private static final int[][] GAME2_PLAYER_ROLLS = {
        {4, 4, 3, 2, 1, 1, 1},
        {4, 3, 2, 2, 1, 1, 1},
        {5, 5, 4, 3, 2, 1, 1},
        {3, 2, 2, 1, 1, 1, 1},
        {4, 3, 2, 1, 1, 1, 1},
        {4, 4, 4, 5, 1, 3, 3}
    };

    private static final int[][] GAME2_OPPONENT_ROLLS = {
        {3, 2, 2, 1, 1},
        {6, 5, 4, 3, 2},
        {7, 5, 4, 3, 2},
        {6, 5, 4, 4, 3},
        {3, 2, 2, 1, 1},
        {3, 2, 2, 1, 1}
    };

    private static final int[] GAME2_REROLL_RESULT = {1, 1, 2, 3, 3};
    private static final int[] GAME2_REROLL2_RESULT = {5, 3, 2, 2, 3};

    private static final int[][] GAME2_OPPONENT_REROLL_RESULTS = {
        {5, 4, 3},
        {6, 4, 1}
    };

    private static final int[][] GAME2_OPPONENT_REROLL_INDICES = {
        {0, 3, 4},
        {0, 1, 4}
    };

    private static final int[][] GAME2_OPPONENT_SELECTIONS = {
        {0, 1, 2, 3},
        {0, 1, 2},
        {0, 1, 2}
    };

    private static final int GAME2_PRISMATIC_FACE = 4;
    private static final int GAME2_PRISMATIC_FACE_INDEX = 4;

    public TutorialDiceRoller(TutorialController controller) {
        this.controller = controller;
        this.playerRollIndex = 0;
        this.opponentRollIndex = 0;
        this.completedRerolls = new HashSet<>();
        this.completedOpponentRerollSteps = new HashSet<>();
        this.opponentRerollCount = 0;
        this.opponentSelectionCount = 0;
        this.playerRerollCount = 0;
    }

    public boolean shouldInterceptPrismaticRoll() {
        return controller.getGame() == TutorialController.TutorialGame.GAME_2_ACHERON;
    }

    public PrismaticDiceInstance getFixedPrismaticRoll(PrismaticDiceType type, boolean isTrueVersion) {
        if (controller.getGame() == TutorialController.TutorialGame.GAME_2_ACHERON
                && "repeater".equals(type.getId()) && !isTrueVersion) {
            PrismaticDiceInstance instance = new PrismaticDiceInstance(
                type, false, GAME2_PRISMATIC_FACE, GAME2_PRISMATIC_FACE_INDEX);
            CosmiconLogger.verbose("TutorialDiceRoller: Fixed prismatic roll for %s - face: %d, special: %s",
                type.getId(), instance.rolledFace, instance.isSpecialFace);
            return instance;
        }
        return PrismaticDiceInstance.roll(type, isTrueVersion);
    }

    private static List<Integer> toList(int[] arr) {
        List<Integer> list = new ArrayList<>(arr.length);
        for (int v : arr) list.add(v);
        return list;
    }

    public List<Integer> planOpponentReroll() {
        if (controller.getGame() == TutorialController.TutorialGame.GAME_1_CHIMERA) {
            String stepName = controller.getCurrentStep().name();
            if (completedOpponentRerollSteps.contains(stepName)) {
                return new ArrayList<>();
            }
            int phase = getG1OpponentRerollPhase();
            if (phase < 0) return new ArrayList<>();
            int[] indices = GAME1_OPPONENT_REROLL_INDICES[phase];
            completedOpponentRerollSteps.add(stepName);
            CosmiconLogger.verbose("TutorialDiceRoller: predetermined Game 1 opponent reroll phase %d - indices: %s",
                phase, Arrays.toString(indices));
            return toList(indices);
        } else if (controller.getGame() == TutorialController.TutorialGame.GAME_2_ACHERON) {
            if (opponentRerollCount < GAME2_OPPONENT_REROLL_INDICES.length) {
                int[] indices = GAME2_OPPONENT_REROLL_INDICES[opponentRerollCount];
                CosmiconLogger.verbose("TutorialDiceRoller: predetermined opponent reroll #%d - indices: %s",
                    opponentRerollCount + 1, Arrays.toString(indices));
                return toList(indices);
            }
        }
        return new ArrayList<>();
    }

    private int getG1OpponentRerollPhase() {
        TutorialController.TutorialStep step = controller.getCurrentStep();
        return switch (step) {
            case G1_T2_DEFENSE_ROLL -> 0;
            case G1_T3_DEFENSE_ROLL -> 1;
            default -> -1;
        };
    }

    public List<Integer> planOpponentSelection(BattleState state) {
        int idx = opponentSelectionCount;
        int[] selections = null;
        if (controller.getGame() == TutorialController.TutorialGame.GAME_2_ACHERON) {
            if (idx < GAME2_OPPONENT_SELECTIONS.length) {
                selections = GAME2_OPPONENT_SELECTIONS[idx];
            }
        } else {
            if (idx < GAME1_OPPONENT_SELECTIONS.length) {
                selections = GAME1_OPPONENT_SELECTIONS[idx];
            }
        }
        if (selections != null) {
            opponentSelectionCount++;
            CosmiconLogger.verbose("TutorialDiceRoller: predetermined opponent selection #%d - indices: %s",
                idx + 1, Arrays.toString(selections));
            return toList(selections);
        }
        int required = state.getRequiredDiceCount(false);
        List<Integer> fallback = new ArrayList<>();
        for (int i = 0; i < required; i++) {
            fallback.add(i);
        }
        CosmiconLogger.verbose("TutorialDiceRoller: fallback opponent selection - indices: %s", fallback);
        return fallback;
    }

    // Returning null delegates to aiEngine.planPrismaticUse(). This is correct
    // because neither tutorial opponent has prismatic dice: Game 1 opponent
    // (Furbo Journalist) has none, Game 2 opponent (Robin) cannot equip them.
    public Object planOpponentPrismatic() {
        return null;
    }

    public boolean shouldInterceptReroll(boolean forPlayer) {
        if (forPlayer) {
            String key = getPlayerRerollKey();
            return key != null && !completedRerolls.contains(key);
        }
        return shouldInterceptOpponentReroll();
    }

    // Always intercept: ensures all opponent rerolls use predetermined results.
    // In practice this is safe because planOpponentReroll() returns an empty list
    // once predetermined data is exhausted, preventing the reroll from triggering
    // at all (TurnProcessor skips empty lists). The modulo cycling in
    // rerollGame1Opponent/rerollGame2Opponent acts as a safety net for edge cases.
    private boolean shouldInterceptOpponentReroll() {
        return true;
    }

    private String getPlayerRerollKey() {
        if (controller.getGame() == TutorialController.TutorialGame.GAME_1_CHIMERA) {
            if (controller.getCurrentStep() == TutorialController.TutorialStep.G1_T1_ATTACK_REROLL) {
                return "G1_T1_PLAYER";
            }
            if (controller.getCurrentStep() == TutorialController.TutorialStep.G1_T1_ATTACK_REROLL2) {
                return "G1_T1_PLAYER_2";
            }
            if (controller.getCurrentStep() == TutorialController.TutorialStep.G1_T2_ATTACK_REROLL) {
                return "G1_T2_PLAYER";
            }
        }
        if (controller.getGame() == TutorialController.TutorialGame.GAME_2_ACHERON) {
            if (controller.getCurrentStep() == TutorialController.TutorialStep.G2_T3_ATTACK_REROLL) {
                return "G2_T3_PLAYER_1";
            }
            if (controller.getCurrentStep() == TutorialController.TutorialStep.G2_T3_ATTACK_REROLL2) {
                return "G2_T3_PLAYER_2";
            }
        }
        return null;
    }

    public void rollForParticipant(BattleState state, boolean forPlayer) {
        if (controller.getGame() == TutorialController.TutorialGame.GAME_1_CHIMERA) {
            rollGame1(state, forPlayer);
        } else {
            rollGame2(state, forPlayer);
        }
    }

    public void rerollSelected(BattleState state, boolean forPlayer) {
        if (forPlayer) {
            String key = getPlayerRerollKey();
            if (key == null || completedRerolls.contains(key)) return;

            switch (key)
            {
                case "G1_T1_PLAYER", "G1_T1_PLAYER_2", "G1_T2_PLAYER" -> rerollGame1Player(state);
                case "G2_T3_PLAYER_1" -> rerollGame2T3(state);
                case "G2_T3_PLAYER_2" -> rerollGame2T3Second(state);
            }
            completedRerolls.add(key);
        } else {
            if (controller.getGame() == TutorialController.TutorialGame.GAME_1_CHIMERA) {
                rerollGame1Opponent(state);
            } else if (controller.getGame() == TutorialController.TutorialGame.GAME_2_ACHERON) {
                rerollGame2Opponent(state);
                opponentRerollCount++;
            }
        }
    }

    private void rollGame1(BattleState state, boolean forPlayer) {
        if (forPlayer) {
            int idx = Math.min(playerRollIndex, GAME1_PLAYER_ROLLS.length - 1);
            setFixedRoll(state, true, GAME1_PLAYER_ROLLS[idx]);
            playerRollIndex++;
        } else {
            int idx = Math.min(opponentRollIndex, GAME1_OPPONENT_ROLLS.length - 1);
            setFixedRoll(state, false, GAME1_OPPONENT_ROLLS[idx]);
            opponentRollIndex++;
        }
    }

    private void rerollGame1Player(BattleState state) {
        List<Integer> values = state.getDiceValues(true);
        List<Boolean> selected = state.getDiceSelected(true);
        List<Integer> rerolledIndices = new ArrayList<>();

        int[] results = GAME1_REROLL_RESULTS[playerRerollCount % GAME1_REROLL_RESULTS.length];
        int resultIdx = 0;
        for (int i = 0; i < selected.size(); i++) {
            if (selected.get(i)) {
                if (resultIdx < results.length) {
                    values.set(i, results[resultIdx++]);
                }
                rerolledIndices.add(i);
            }
        }

        state.setDiceValues(true, values);
        state.decrementRerolls(true);
        state.incrementRerollsUsed(true);
        playerRerollCount++;

        CosmiconLogger.verbose("TutorialDiceRoller: Game 1 player reroll #%d - indices: %s",
            playerRerollCount, rerolledIndices);
    }

    private void rerollGame1Opponent(BattleState state) {
        List<Integer> values = state.getDiceValues(false);
        List<Boolean> selected = state.getDiceSelected(false);
        List<Integer> rerolledIndices = new ArrayList<>();

        int phase = getG1OpponentRerollPhase();
        int[] results = phase >= 0 && phase < GAME1_OPPONENT_REROLL_RESULTS.length
            ? GAME1_OPPONENT_REROLL_RESULTS[phase] : new int[0];
        int resultIdx = 0;
        for (int i = 0; i < selected.size(); i++) {
            if (selected.get(i)) {
                if (resultIdx < results.length) {
                    values.set(i, results[resultIdx++]);
                }
                rerolledIndices.add(i);
            }
        }

        state.setDiceValues(false, values);
        state.decrementRerolls(false);
        state.incrementRerollsUsed(false);

        CosmiconLogger.verbose("TutorialDiceRoller: Game 1 opponent reroll phase %d - indices: %s",
            phase, rerolledIndices);
    }

    private void rollGame2(BattleState state, boolean forPlayer) {
        if (forPlayer) {
            int idx = Math.min(playerRollIndex, GAME2_PLAYER_ROLLS.length - 1);
            setFixedRoll(state, true, GAME2_PLAYER_ROLLS[idx]);
            playerRollIndex++;
        } else {
            int idx = Math.min(opponentRollIndex, GAME2_OPPONENT_ROLLS.length - 1);
            setFixedRoll(state, false, GAME2_OPPONENT_ROLLS[idx]);
            opponentRollIndex++;
        }
    }

    private void rerollGame2T3(BattleState state) {
        List<Integer> values = state.getDiceValues(true);
        List<Boolean> selected = state.getDiceSelected(true);
        List<DiceType> types = state.getDiceTypes(true);
        List<Integer> rerolledIndices = new ArrayList<>();

        int resultIdx = 0;
        for (int i = 0; i < selected.size(); i++) {
            if (selected.get(i) && types.get(i) != DiceType.PRISMATIC) {
                if (resultIdx < GAME2_REROLL_RESULT.length) {
                    values.set(i, GAME2_REROLL_RESULT[resultIdx++]);
                }
                rerolledIndices.add(i);
            }
        }

        state.setDiceValues(true, values);
        state.decrementRerolls(true);
        state.incrementRerollsUsed(true);

        CosmiconLogger.verbose("TutorialDiceRoller: Game 2 T3 reroll (non-prismatic) - indices: %s", rerolledIndices);
    }

    private void rerollGame2T3Second(BattleState state) {
        List<Integer> values = state.getDiceValues(true);
        List<Boolean> selected = state.getDiceSelected(true);
        List<DiceType> types = state.getDiceTypes(true);
        List<Integer> rerolledIndices = new ArrayList<>();

        int resultIdx = 0;
        for (int i = 0; i < selected.size(); i++) {
            if (selected.get(i) && types.get(i) != DiceType.PRISMATIC) {
                if (resultIdx < GAME2_REROLL2_RESULT.length) {
                    values.set(i, GAME2_REROLL2_RESULT[resultIdx++]);
                }
                rerolledIndices.add(i);
            }
        }

        state.setDiceValues(true, values);
        state.decrementRerolls(true);
        state.incrementRerollsUsed(true);

        CosmiconLogger.verbose("TutorialDiceRoller: Game 2 T3 second reroll (non-prismatic) - indices: %s", rerolledIndices);
    }

    private void rerollGame2Opponent(BattleState state) {
        List<Integer> values = state.getDiceValues(false);
        List<Boolean> selected = state.getDiceSelected(false);
        List<Integer> rerolledIndices = new ArrayList<>();

        int[] results = GAME2_OPPONENT_REROLL_RESULTS[opponentRerollCount % GAME2_OPPONENT_REROLL_RESULTS.length];
        int resultIdx = 0;
        for (int i = 0; i < selected.size(); i++) {
            if (selected.get(i)) {
                if (resultIdx < results.length) {
                    values.set(i, results[resultIdx++]);
                }
                rerolledIndices.add(i);
            }
        }

        state.setDiceValues(false, values);
        state.decrementRerolls(false);
        state.incrementRerollsUsed(false);

        CosmiconLogger.verbose("TutorialDiceRoller: opponent reroll #%d - indices: %s",
            opponentRerollCount + 1, rerolledIndices);
    }

    private void setFixedRoll(BattleState state, boolean forPlayer, int[] values) {
        List<DiceType> cardTypes = state.getUpgradedDicePool(forPlayer);
        if (cardTypes == null) {
            CharacterCard card = state.getCard(forPlayer);
            cardTypes = card.getDicePool();
        }
        List<DiceType> typeList = new ArrayList<>();
        List<Integer> valueList = new ArrayList<>();
        List<Boolean> selectedList = new ArrayList<>();
        int valueIdx = 0;
        for (DiceType cardType : cardTypes)
        {
            if (cardType == DiceType.PRISMATIC) continue;
            typeList.add(cardType);
            int maxFace = cardType.getMaxFace();
            valueList.add(valueIdx < values.length ? Math.min(values[valueIdx], maxFace) : 1);
            selectedList.add(false);
            valueIdx++;
        }

        state.setDiceValues(forPlayer, valueList);
        state.setDiceTypes(forPlayer, typeList);
        state.setDiceSelected(forPlayer, selectedList);

        state.notifyDiceRolled(forPlayer, typeList, valueList);

        CosmiconLogger.verbose("TutorialDiceRoller: Fixed roll for %s - values: %s",
            forPlayer ? "Player" : "Opponent", valueList);
    }
}
