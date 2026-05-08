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
    private int opponentRerollCount;
    private int opponentSelectionCount;

    private static final int[][] GAME1_PLAYER_ROLLS = {
        {7, 6, 4, 3, 2},
        {4, 4, 3, 2, 1},
        {1, 1, 2, 4, 3}
    };

    private static final int[][] GAME1_OPPONENT_ROLLS = {
        {3, 2, 2, 1, 1},
        {6, 5, 3, 2, 1},
        {3, 2, 2, 1, 1}
    };

    private static final int[] GAME1_REROLL_RESULT = {4, 4, 4, 0, 0};

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

    private static final int[][] GAME1_OPPONENT_REROLL_INDICES = {
        {3, 4},
        {}
    };

    private static final int[][] GAME1_OPPONENT_REROLL_RESULTS = {
        {4, 4},
        {}
    };

    private static final int[][] GAME2_OPPONENT_SELECTIONS = {
        {0, 1, 2, 3},
        {0, 1, 2},
        {0, 1, 2}
    };

    private static final int[][] GAME1_OPPONENT_SELECTIONS = {
        {0, 1},
        {0, 1, 3},
        {0, 1}
    };

    private static final int GAME2_PRISMATIC_FACE = 4;
    private static final int GAME2_PRISMATIC_FACE_INDEX = 4;

    public TutorialDiceRoller(TutorialController controller) {
        this.controller = controller;
        this.playerRollIndex = 0;
        this.opponentRollIndex = 0;
        this.completedRerolls = new HashSet<>();
        this.opponentRerollCount = 0;
        this.opponentSelectionCount = 0;
    }

    public boolean shouldInterceptPrismaticRoll() {
        return controller.getGame() == TutorialController.TutorialGame.GAME_2_ACHERON;
    }

    public PrismaticDiceInstance getFixedPrismaticRoll(PrismaticDiceType type, boolean isTrueVersion) {
        if (controller.getGame() == TutorialController.TutorialGame.GAME_2_ACHERON
                && "repeater".equals(type.getId()) && !isTrueVersion) {
            PrismaticDiceInstance instance = new PrismaticDiceInstance(
                type, false, GAME2_PRISMATIC_FACE, GAME2_PRISMATIC_FACE_INDEX);
            CosmiconLogger.debug("TutorialDiceRoller: Fixed prismatic roll for %s - face: %d, special: %s",
                type.getId(), instance.rolledFace, instance.isSpecialFace);
            return instance;
        }
        return PrismaticDiceInstance.roll(type, isTrueVersion);
    }

    public List<Integer> planOpponentReroll() {
        if (controller.getGame() == TutorialController.TutorialGame.GAME_2_ACHERON) {
            if (opponentRerollCount < GAME2_OPPONENT_REROLL_INDICES.length) {
                int[] indices = GAME2_OPPONENT_REROLL_INDICES[opponentRerollCount];
                CosmiconLogger.debug("TutorialDiceRoller: predetermined opponent reroll #%d - indices: %s",
                    opponentRerollCount + 1, Arrays.toString(indices));
                return new ArrayList<>(Arrays.stream(indices).boxed().toList());
            }
        } else {
            if (opponentRerollCount < GAME1_OPPONENT_REROLL_INDICES.length) {
                int[] indices = GAME1_OPPONENT_REROLL_INDICES[opponentRerollCount];
                CosmiconLogger.debug("TutorialDiceRoller: predetermined Game 1 opponent reroll #%d - indices: %s",
                    opponentRerollCount + 1, Arrays.toString(indices));
                return new ArrayList<>(Arrays.stream(indices).boxed().toList());
            }
        }
        return new ArrayList<>();
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
            CosmiconLogger.debug("TutorialDiceRoller: predetermined opponent selection #%d - indices: %s",
                idx + 1, Arrays.toString(selections));
            return new ArrayList<>(Arrays.stream(selections).boxed().toList());
        }
        int required = state.getRequiredDiceCount(false);
        List<Integer> fallback = new ArrayList<>();
        for (int i = 0; i < required; i++) {
            fallback.add(i);
        }
        CosmiconLogger.debug("TutorialDiceRoller: fallback opponent selection - indices: %s", fallback);
        return fallback;
    }

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

    private boolean shouldInterceptOpponentReroll() {
        return true;
    }

    private String getPlayerRerollKey() {
        if (controller.getGame() == TutorialController.TutorialGame.GAME_1_SPARXIE) {
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
        if (controller.getGame() == TutorialController.TutorialGame.GAME_1_SPARXIE) {
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
                case "G1_T2_PLAYER" -> rerollGame1Turn3(state);
                case "G2_T3_PLAYER_1" -> rerollGame2T3(state);
                case "G2_T3_PLAYER_2" -> rerollGame2T3Second(state);
            }
            completedRerolls.add(key);
        } else {
            if (controller.getGame() == TutorialController.TutorialGame.GAME_2_ACHERON) {
                rerollGame2Opponent(state);
            } else {
                rerollGame1Opponent(state);
            }
            opponentRerollCount++;
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

    private void rerollGame1Turn3(BattleState state) {
        List<Integer> values = state.getDiceValues(true);
        List<Boolean> selected = state.getDiceSelected(true);
        List<Integer> rerolledIndices = new ArrayList<>();

        int resultIdx = 0;
        for (int i = 0; i < selected.size(); i++) {
            if (selected.get(i)) {
                if (resultIdx < GAME1_REROLL_RESULT.length) {
                    values.set(i, GAME1_REROLL_RESULT[resultIdx++]);
                }
                rerolledIndices.add(i);
            }
        }

        state.setDiceValues(true, values);
        state.decrementRerolls(true);
        state.incrementRerollsUsed(true);

        CosmiconLogger.debug("TutorialDiceRoller: Game 1 Turn 3 reroll - indices: %s", rerolledIndices);
    }

    private void rerollGame1Opponent(BattleState state) {
        List<Integer> values = state.getDiceValues(false);
        List<Boolean> selected = state.getDiceSelected(false);
        List<Integer> rerolledIndices = new ArrayList<>();

        int[] results = GAME1_OPPONENT_REROLL_RESULTS[opponentRerollCount % GAME1_OPPONENT_REROLL_RESULTS.length];
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

        CosmiconLogger.debug("TutorialDiceRoller: Game 1 opponent reroll #%d - indices: %s",
            opponentRerollCount + 1, rerolledIndices);
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

        CosmiconLogger.debug("TutorialDiceRoller: Game 2 T3 reroll (non-prismatic) - indices: %s", rerolledIndices);
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

        CosmiconLogger.debug("TutorialDiceRoller: Game 2 T3 second reroll (non-prismatic) - indices: %s", rerolledIndices);
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

        CosmiconLogger.debug("TutorialDiceRoller: opponent reroll #%d - indices: %s",
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

        for (int i = 0; i < cardTypes.size(); i++) {
            typeList.add(cardTypes.get(i));
            int maxFace = cardTypes.get(i).getMaxFace();
            valueList.add(i < values.length ? Math.min(values[i], maxFace) : 1);
            selectedList.add(false);
        }

        state.setDiceValues(forPlayer, valueList);
        state.setDiceTypes(forPlayer, typeList);
        state.setDiceSelected(forPlayer, selectedList);

        state.notifyDiceRolled(forPlayer, typeList, valueList);

        CosmiconLogger.debug("TutorialDiceRoller: Fixed roll for %s - values: %s",
            forPlayer ? "Player" : "Opponent", valueList);
    }
}
