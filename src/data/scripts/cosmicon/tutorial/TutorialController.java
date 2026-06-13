package data.scripts.cosmicon.tutorial;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.TurnState.Phase;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.state.CosmiconEventState;
import data.scripts.cosmicon.state.CosmiconStats;

public class TutorialController {

    private static final Map<TutorialStep, TutorialStep> SELECT_TO_CONFIRM = Map.ofEntries(
        Map.entry(TutorialStep.G1_T1_ATTACK_SELECT, TutorialStep.G1_T1_ATTACK_CONFIRM),
        Map.entry(TutorialStep.G1_T1_DEFENSE_SELECT, TutorialStep.G1_T1_DEFENSE_CONFIRM),
        Map.entry(TutorialStep.G1_T2_ATTACK_SELECT, TutorialStep.G1_T2_ATTACK_CONFIRM),
        Map.entry(TutorialStep.G1_T2_DEFENSE_SELECT, TutorialStep.G1_T2_DEFENSE_CONFIRM),
        Map.entry(TutorialStep.G1_T3_ATTACK_SELECT, TutorialStep.G1_T3_ATTACK_CONFIRM),
        Map.entry(TutorialStep.G2_T1_DEFENSE_SELECT, TutorialStep.G2_T1_DEFENSE_CONFIRM),
        Map.entry(TutorialStep.G2_T2_ATTACK_PRISMATIC, TutorialStep.G2_T2_ATTACK_CONFIRM),
        Map.entry(TutorialStep.G2_T2_ATTACK_SELECT, TutorialStep.G2_T2_ATTACK_CONFIRM),
        Map.entry(TutorialStep.G2_T2_DEFENSE_SELECT, TutorialStep.G2_T2_DEFENSE_CONFIRM),
        Map.entry(TutorialStep.G2_T3_ATTACK_SELECT, TutorialStep.G2_T3_ATTACK_CONFIRM),
        Map.entry(TutorialStep.G2_T3_DEFENSE_SELECT, TutorialStep.G2_T3_DEFENSE_CONFIRM),
        Map.entry(TutorialStep.G2_T4_ATTACK_SELECT, TutorialStep.G2_T4_ATTACK_CONFIRM)
    );

    public enum TutorialGame {
        GAME_1_CHIMERA,
        GAME_2_ACHERON
    }

    public enum TutorialStep {
        G1_T1_ATTACK_ROLL,
        G1_T1_ATTACK_SELECT,
        G1_T1_ATTACK_CONFIRM,
        G1_T1_ATTACK_WAIT,
        G1_T1_ATTACK_RESOLVE,
        G1_T1_DEFENSE_ROLL,
        G1_T1_DEFENSE_OPPONENT_REROLL,
        G1_T1_DEFENSE_SELECT,
        G1_T1_DEFENSE_CONFIRM,
        G1_T1_DEFENSE_WAIT,
        G1_T1_DEFENSE_RESOLVE,
        G1_T2_ATTACK_ROLL,
        G1_T2_ATTACK_REROLL,
        G1_T2_ATTACK_SELECT,
        G1_T2_ATTACK_CONFIRM,
        G1_T2_ATTACK_WAIT,
        G1_T2_ATTACK_RESOLVE,
        G1_T2_DEFENSE_ROLL,
        G1_T2_DEFENSE_SELECT,
        G1_T2_DEFENSE_CONFIRM,
        G1_T2_DEFENSE_WAIT,
        G1_T2_DEFENSE_RESOLVE,
        G1_T3_ATTACK_ROLL,
        G1_T3_ATTACK_REROLL,
        G1_T3_ATTACK_SELECT,
        G1_T3_ATTACK_CONFIRM,
        G1_T3_ATTACK_WAIT,
        G1_VICTORY,

        G2_T1_DEFENSE_ROLL,
        G2_T1_DEFENSE_SELECT,
        G2_T1_DEFENSE_CONFIRM,
        G2_T1_DEFENSE_WAIT,
        G2_T1_DEFENSE_RESOLVE,
        G2_T2_ATTACK_ROLL,
        G2_T2_ATTACK_PRISMATIC,
        G2_T2_ATTACK_SELECT,
        G2_T2_ATTACK_CONFIRM,
        G2_T2_ATTACK_WAIT,
        G2_T2_ATTACK_RESOLVE,
        G2_T2_DEFENSE_ROLL,
        G2_T2_DEFENSE_SELECT,
        G2_T2_DEFENSE_CONFIRM,
        G2_T2_DEFENSE_WAIT,
        G2_T2_DEFENSE_RESOLVE,
        G2_T3_ATTACK_ROLL,
        G2_T3_ATTACK_PRISMATIC,
        G2_T3_ATTACK_REROLL,
        G2_T3_ATTACK_REROLL2,
        G2_T3_ATTACK_SELECT,
        G2_T3_ATTACK_CONFIRM,
        G2_T3_ATTACK_WAIT,
        G2_T3_ATTACK_RESOLVE,
        G2_T3_DEFENSE_ROLL,
        G2_T3_DEFENSE_SELECT,
        G2_T3_DEFENSE_CONFIRM,
        G2_T3_DEFENSE_WAIT,
        G2_T3_DEFENSE_RESOLVE,
        G2_T4_ATTACK_ROLL,
        G2_T4_ATTACK_SELECT,
        G2_T4_ATTACK_CONFIRM,
        G2_T4_ATTACK_WAIT,
        G2_VICTORY
    }

    private final TutorialGame game;
    private TutorialStep currentStep;
    private final BattleState battleState;
    private boolean complete;
    private boolean rerollPending;
    private boolean prismaticUnlocked;

    public TutorialController(TutorialGame game, BattleState battleState) {
        this.game = game;
        this.battleState = battleState;
        this.complete = false;
        this.rerollPending = false;
        this.currentStep = game == TutorialGame.GAME_1_CHIMERA
            ? TutorialStep.G1_T1_ATTACK_ROLL
            : TutorialStep.G2_T1_DEFENSE_ROLL;
    }

    public static TutorialGame determineTutorialGame() {
        int replayGame = CosmiconEventState.getReplayTutorialGame();
        if (replayGame >= 0) {
            return replayGame == 1 ? TutorialGame.GAME_1_CHIMERA : TutorialGame.GAME_2_ACHERON;
        }
        if (!CosmiconStats.isTutorial1Completed()) {
            return TutorialGame.GAME_1_CHIMERA;
        }
        return TutorialGame.GAME_2_ACHERON;
    }

    public static boolean shouldActivateTutorial() {
        return CosmiconEventState.isTutorialMode()
            || CosmiconEventState.isReplayTutorial();
    }

    public TutorialGame getGame() {
        return game;
    }

    public TutorialStep getCurrentStep() {
        return currentStep;
    }

    public boolean isComplete() {
        return complete;
    }

    public String getTutorialText() {
        String key = "tutorial." + currentStep.name().toLowerCase();
        try {
            return Strings.get(key);
        } catch (RuntimeException e) {
            return "";
        }
    }

    public boolean isDiceClickable() {
        if (complete) return true;

        return switch (currentStep) {
            case G1_T1_ATTACK_SELECT, G1_T1_DEFENSE_SELECT,
                 G1_T2_ATTACK_REROLL, G1_T2_ATTACK_SELECT,
                 G1_T2_DEFENSE_SELECT,
                 G1_T3_ATTACK_REROLL, G1_T3_ATTACK_SELECT,
                 G2_T1_DEFENSE_SELECT,
                 G2_T2_ATTACK_PRISMATIC, G2_T2_ATTACK_SELECT,
                 G2_T2_DEFENSE_SELECT,
                 G2_T3_ATTACK_PRISMATIC, G2_T3_ATTACK_REROLL,
                 G2_T3_ATTACK_REROLL2,
                 G2_T3_ATTACK_SELECT,
                 G2_T3_DEFENSE_SELECT,
                 G2_T4_ATTACK_SELECT -> true;
            default -> false;
        };
    }

    public boolean isDiceSelectionAllowed(int diceIndex) {
        if (complete) return true;

        List<Boolean> selected = battleState.getPlayerDiceSelected();
        if (diceIndex < 0 || diceIndex >= selected.size()) return false;

        if (selected.get(diceIndex)) return true;

        List<Integer> values = battleState.getPlayerDiceValues();
        List<DiceType> types = battleState.getPlayerDiceTypes();
        int value = values.get(diceIndex);
        boolean isPrismatic = types.get(diceIndex) == DiceType.PRISMATIC;

        return switch (currentStep) {
            case G1_T1_ATTACK_SELECT, G1_T1_DEFENSE_SELECT,
                 G1_T2_ATTACK_SELECT, G1_T2_DEFENSE_SELECT,
                 G1_T3_ATTACK_SELECT,
                 G2_T1_DEFENSE_SELECT,
                 G2_T2_DEFENSE_SELECT,
                 G2_T3_DEFENSE_SELECT -> isAmongHighest(diceIndex, values, battleState.getRequiredDiceCount(true));

            case G1_T2_ATTACK_REROLL, G1_T3_ATTACK_REROLL -> !isAmongHighest(diceIndex, values, 2);

            case G2_T3_ATTACK_REROLL, G2_T3_ATTACK_REROLL2 -> !isPrismatic;

            case G2_T2_ATTACK_PRISMATIC, G2_T2_ATTACK_SELECT ->
                isPrismatic || value == 4;

            case G2_T3_ATTACK_PRISMATIC -> isPrismatic;

            case G2_T3_ATTACK_SELECT -> isAmongHighestNonPrismatic(diceIndex, values, types,
                    isPrismaticDiceSelected()
                            ? Math.max(1, battleState.getRequiredDiceCount(true) - 1)
                            : battleState.getRequiredDiceCount(true));

            case G2_T4_ATTACK_SELECT -> value == 4;

            default -> true;
        };
    }

    public boolean canRerollWithCurrentSelection() {
        if (complete) return true;

        if (currentStep == TutorialStep.G1_T2_ATTACK_REROLL
                || currentStep == TutorialStep.G1_T3_ATTACK_REROLL) {
            List<Integer> values = battleState.getPlayerDiceValues();
            List<Boolean> selected = battleState.getPlayerDiceSelected();
            for (int i = 0; i < values.size(); i++) {
                if (!isAmongHighest(i, values, 2) && !selected.get(i)) {
                    return false;
                }
            }
            return true;
        }

        if (currentStep == TutorialStep.G2_T3_ATTACK_REROLL
                || currentStep == TutorialStep.G2_T3_ATTACK_REROLL2) {
            List<DiceType> types = battleState.getPlayerDiceTypes();
            List<Boolean> selected = battleState.getPlayerDiceSelected();
            for (int i = 0; i < types.size(); i++) {
                if (types.get(i) != DiceType.PRISMATIC && !selected.get(i)) {
                    return false;
                }
            }
            return true;
        }

        return true;
    }

    private boolean isAmongHighest(int index, List<Integer> values, int count) {
        if (values == null || values.isEmpty() || index < 0 || index >= values.size()) return false;
        int val = values.get(index);
        int higherCount = 0;
        for (int v : values) {
            if (v > val) higherCount++;
        }
        return higherCount < count;
    }

    private boolean isAmongHighestNonPrismatic(int index, List<Integer> values, List<DiceType> types, int count) {
        if (values == null || values.isEmpty() || index < 0 || index >= values.size()) return false;
        if (types.get(index) == DiceType.PRISMATIC) return false;
        int val = values.get(index);
        int higherCount = 0;
        for (int i = 0; i < values.size(); i++) {
            if (types.get(i) != DiceType.PRISMATIC && values.get(i) > val) {
                higherCount++;
            }
        }
        return higherCount < count;
    }

    private boolean isPrismaticDiceSelected() {
        List<DiceType> types = battleState.getPlayerDiceTypes();
        List<Boolean> selected = battleState.getPlayerDiceSelected();
        for (int i = 0; i < types.size(); i++) {
            if (types.get(i) == DiceType.PRISMATIC && selected.get(i)) {
                return true;
            }
        }
        return false;
    }

    public boolean isConfirmAllowed() {
        if (complete) return true;

        return switch (currentStep) {
            case G1_T1_ATTACK_CONFIRM, G1_T1_DEFENSE_CONFIRM,
                 G1_T2_ATTACK_CONFIRM, G1_T2_DEFENSE_CONFIRM,
                 G1_T3_ATTACK_CONFIRM,
                 G2_T1_DEFENSE_CONFIRM,
                 G2_T2_ATTACK_CONFIRM,
                 G2_T2_DEFENSE_CONFIRM,
                 G2_T3_ATTACK_CONFIRM,
                 G2_T3_DEFENSE_CONFIRM,
                 G2_T4_ATTACK_CONFIRM -> true;
            default -> false;
        };
    }

    public boolean isRerollAllowed() {
        if (complete) return true;

        return currentStep == TutorialStep.G1_T2_ATTACK_REROLL
            || currentStep == TutorialStep.G1_T3_ATTACK_REROLL
            || currentStep == TutorialStep.G2_T3_ATTACK_REROLL
            || currentStep == TutorialStep.G2_T3_ATTACK_REROLL2;
    }

    public boolean isPrismaticAllowed() {
        if (complete) return false;
        if (game != TutorialGame.GAME_2_ACHERON) return false;

        boolean isAttackPhase = currentStep == TutorialStep.G2_T2_ATTACK_PRISMATIC
            || currentStep == TutorialStep.G2_T2_ATTACK_SELECT
            || currentStep == TutorialStep.G2_T3_ATTACK_PRISMATIC
            || currentStep == TutorialStep.G2_T3_ATTACK_REROLL
            || currentStep == TutorialStep.G2_T3_ATTACK_REROLL2
            || currentStep == TutorialStep.G2_T3_ATTACK_SELECT;

        if (!isAttackPhase) return false;

        if (prismaticUnlocked) return true;

        List<DiceType> types = battleState.getPlayerDiceTypes();
        if (types != null) {
            for (DiceType type : types) {
                if (type == DiceType.PRISMATIC) return false;
            }
        }

        prismaticUnlocked = true;
        return true;
    }

    public boolean isClickToRollAllowed() {
        if (complete) return true;

        return switch (currentStep) {
            case G1_T1_ATTACK_ROLL, G1_T1_DEFENSE_ROLL,
                 G1_T2_ATTACK_ROLL, G1_T2_DEFENSE_ROLL,
                 G1_T3_ATTACK_ROLL,
                 G2_T1_DEFENSE_ROLL,
                 G2_T2_ATTACK_ROLL,
                 G2_T2_DEFENSE_ROLL,
                 G2_T3_ATTACK_ROLL,
                 G2_T3_DEFENSE_ROLL,
                 G2_T4_ATTACK_ROLL -> true;
            default -> false;
        };
    }

    public boolean isContinueAllowed() {
        // Always allow continue during WAITING_NEXT_TURN/ENDED phases.
        // The phase check in BattleUIButtons already gates the button appropriately.
        // RESOLVE steps need Continue to advance the tutorial.
        return true;
    }

    public List<Integer> getIndicatedDiceIndices() {
        List<Integer> indices = new ArrayList<>();
        if (complete) return indices;
        if (!isDiceClickable()) return indices;

        List<Boolean> selected = battleState.getPlayerDiceSelected();
        for (int i = 0; i < selected.size(); i++) {
            if (!selected.get(i) && isDiceSelectionAllowed(i)) {
                indices.add(i);
            }
        }
        return indices;
    }

    public boolean shouldIndicateConfirmButton() {
        if (complete) return false;
        return isConfirmAllowed();
    }

    public boolean shouldIndicateRerollButton() {
        if (complete) return false;
        return isRerollAllowed() && canRerollWithCurrentSelection();
    }

    public boolean shouldIndicatePrismaticButton() {
        if (complete) return false;
        return currentStep == TutorialStep.G2_T2_ATTACK_PRISMATIC
            || currentStep == TutorialStep.G2_T3_ATTACK_PRISMATIC;
    }

    public void onDiceSelected() {
        if (complete) return;

        TutorialStep confirmStep = SELECT_TO_CONFIRM.get(currentStep);
        if (confirmStep != null && battleState.countSelectedDice(true) == battleState.getRequiredDiceCount(true)) {
            currentStep = confirmStep;
        }
    }

    public void onConfirmed() {
        if (complete) return;

        switch (currentStep) {
            case G1_T1_ATTACK_CONFIRM -> currentStep = TutorialStep.G1_T1_ATTACK_WAIT;
            case G1_T1_DEFENSE_CONFIRM -> currentStep = TutorialStep.G1_T1_DEFENSE_WAIT;
            case G1_T2_ATTACK_CONFIRM -> currentStep = TutorialStep.G1_T2_ATTACK_WAIT;
            case G1_T2_DEFENSE_CONFIRM -> currentStep = TutorialStep.G1_T2_DEFENSE_WAIT;
            case G1_T3_ATTACK_CONFIRM -> currentStep = TutorialStep.G1_T3_ATTACK_WAIT;
            case G2_T1_DEFENSE_CONFIRM -> currentStep = TutorialStep.G2_T1_DEFENSE_WAIT;
            case G2_T2_ATTACK_CONFIRM -> currentStep = TutorialStep.G2_T2_ATTACK_WAIT;
            case G2_T2_DEFENSE_CONFIRM -> currentStep = TutorialStep.G2_T2_DEFENSE_WAIT;
            case G2_T3_ATTACK_CONFIRM -> currentStep = TutorialStep.G2_T3_ATTACK_WAIT;
            case G2_T3_DEFENSE_CONFIRM -> currentStep = TutorialStep.G2_T3_DEFENSE_WAIT;
            case G2_T4_ATTACK_CONFIRM -> currentStep = TutorialStep.G2_T4_ATTACK_WAIT;
            default -> {}
        }

        if (currentStep == TutorialStep.G1_VICTORY || currentStep == TutorialStep.G2_VICTORY) {
            complete = true;
        }
    }

    public void onRerolled() {
        if (complete) return;

        if (currentStep == TutorialStep.G1_T2_ATTACK_REROLL) {
            currentStep = TutorialStep.G1_T2_ATTACK_SELECT;
        } else if (currentStep == TutorialStep.G1_T3_ATTACK_REROLL) {
            currentStep = TutorialStep.G1_T3_ATTACK_SELECT;
        } else if (currentStep == TutorialStep.G2_T3_ATTACK_REROLL) {
            currentStep = TutorialStep.G2_T3_ATTACK_REROLL2;
        } else if (currentStep == TutorialStep.G2_T3_ATTACK_REROLL2) {
            currentStep = TutorialStep.G2_T3_ATTACK_SELECT;
        }
    }

    public void setRerollPending() {
        this.rerollPending = true;
    }

    public boolean hasRerollPending() {
        return rerollPending;
    }

    public void processPendingReroll() {
        if (rerollPending) {
            rerollPending = false;
            onRerolled();
        }
    }

    public void onPrismaticUsed() {
        if (complete) return;

        if (currentStep == TutorialStep.G2_T2_ATTACK_PRISMATIC) {
            currentStep = TutorialStep.G2_T2_ATTACK_SELECT;
        } else if (currentStep == TutorialStep.G2_T3_ATTACK_PRISMATIC) {
            currentStep = TutorialStep.G2_T3_ATTACK_REROLL;
        }
    }

    public void onPrismaticPopupClosed() {
        onPrismaticUsed();
    }

    public void onContinueClicked() {
        if (complete) return;

        switch (currentStep) {
            case G1_T1_ATTACK_RESOLVE -> currentStep = TutorialStep.G1_T1_DEFENSE_ROLL;
            case G1_T1_DEFENSE_RESOLVE -> currentStep = TutorialStep.G1_T2_ATTACK_ROLL;
            case G1_T2_ATTACK_RESOLVE -> currentStep = TutorialStep.G1_T2_DEFENSE_ROLL;
            case G1_T2_DEFENSE_RESOLVE -> currentStep = TutorialStep.G1_T3_ATTACK_ROLL;
            case G2_T1_DEFENSE_RESOLVE -> currentStep = TutorialStep.G2_T2_ATTACK_ROLL;
            case G2_T2_ATTACK_RESOLVE -> currentStep = TutorialStep.G2_T2_DEFENSE_ROLL;
            case G2_T2_DEFENSE_RESOLVE -> currentStep = TutorialStep.G2_T3_ATTACK_ROLL;
            case G2_T3_ATTACK_RESOLVE -> currentStep = TutorialStep.G2_T3_DEFENSE_ROLL;
            case G2_T3_DEFENSE_RESOLVE -> currentStep = TutorialStep.G2_T4_ATTACK_ROLL;
            default -> {}
        }
    }

    public void onPhaseChange(Phase newPhase) {
        if (complete) return;

        if (game == TutorialGame.GAME_1_CHIMERA) {
            onGame1PhaseChange(newPhase);
        } else {
            onGame2PhaseChange(newPhase);
        }
    }

    public void onBattleEnd(String winner) {
        if (complete) return;

        if (game == TutorialGame.GAME_1_CHIMERA
                && currentStep == TutorialStep.G1_T3_ATTACK_WAIT
                && "player".equals(winner)) {
            currentStep = TutorialStep.G1_VICTORY;
            complete = true;
        } else if (game == TutorialGame.GAME_2_ACHERON
                && currentStep == TutorialStep.G2_T4_ATTACK_WAIT
                && "player".equals(winner)) {
            currentStep = TutorialStep.G2_VICTORY;
            complete = true;
        }
    }

    private void onGame1PhaseChange(Phase newPhase) {
        boolean playerIsAttacker = battleState.isPlayerAttacker();

        if (newPhase == Phase.SELECTING_ATTACK && playerIsAttacker) {
            if (currentStep == TutorialStep.G1_T1_ATTACK_ROLL) {
                currentStep = TutorialStep.G1_T1_ATTACK_SELECT;
            } else if (currentStep == TutorialStep.G1_T2_ATTACK_ROLL) {
                currentStep = TutorialStep.G1_T2_ATTACK_REROLL;
            } else if (currentStep == TutorialStep.G1_T3_ATTACK_ROLL) {
                currentStep = TutorialStep.G1_T3_ATTACK_REROLL;
            }
        } else if (newPhase == Phase.SELECTING_ATTACK) {
            // playerIsAttacker is false here (the true case was handled above)
            if (currentStep == TutorialStep.G1_T1_DEFENSE_ROLL) {
                currentStep = TutorialStep.G1_T1_DEFENSE_OPPONENT_REROLL;
            } else if (currentStep == TutorialStep.G1_T2_DEFENSE_ROLL) {
                currentStep = TutorialStep.G1_T2_DEFENSE_SELECT;
            }
        } else if (newPhase == Phase.SELECTING_DEFENSE && !playerIsAttacker) {
            if (currentStep == TutorialStep.G1_T1_DEFENSE_OPPONENT_REROLL) {
                currentStep = TutorialStep.G1_T1_DEFENSE_SELECT;
            }
        } else if (newPhase == Phase.WAITING_NEXT_TURN) {
            if (currentStep == TutorialStep.G1_T1_ATTACK_WAIT) {
                currentStep = TutorialStep.G1_T1_ATTACK_RESOLVE;
            } else if (currentStep == TutorialStep.G1_T1_DEFENSE_WAIT) {
                currentStep = TutorialStep.G1_T1_DEFENSE_RESOLVE;
            } else if (currentStep == TutorialStep.G1_T2_ATTACK_WAIT) {
                currentStep = TutorialStep.G1_T2_ATTACK_RESOLVE;
            } else if (currentStep == TutorialStep.G1_T2_DEFENSE_WAIT) {
                currentStep = TutorialStep.G1_T2_DEFENSE_RESOLVE;
            }
        }
    }

    private void onGame2PhaseChange(Phase newPhase) {
        boolean playerIsAttacker = battleState.isPlayerAttacker();

        if (newPhase == Phase.SELECTING_DEFENSE && !playerIsAttacker) {
            if (currentStep == TutorialStep.G2_T1_DEFENSE_ROLL) {
                currentStep = TutorialStep.G2_T1_DEFENSE_SELECT;
            } else if (currentStep == TutorialStep.G2_T2_DEFENSE_ROLL) {
                currentStep = TutorialStep.G2_T2_DEFENSE_SELECT;
            } else if (currentStep == TutorialStep.G2_T3_DEFENSE_ROLL) {
                currentStep = TutorialStep.G2_T3_DEFENSE_SELECT;
            }
        } else if (newPhase == Phase.SELECTING_ATTACK && playerIsAttacker) {
            if (currentStep == TutorialStep.G2_T2_ATTACK_ROLL) {
                currentStep = TutorialStep.G2_T2_ATTACK_PRISMATIC;
            } else if (currentStep == TutorialStep.G2_T3_ATTACK_ROLL) {
                currentStep = TutorialStep.G2_T3_ATTACK_PRISMATIC;
            } else if (currentStep == TutorialStep.G2_T4_ATTACK_ROLL) {
                currentStep = TutorialStep.G2_T4_ATTACK_SELECT;
            }
        } else if (newPhase == Phase.WAITING_NEXT_TURN) {
            if (currentStep == TutorialStep.G2_T1_DEFENSE_WAIT) {
                currentStep = TutorialStep.G2_T1_DEFENSE_RESOLVE;
            } else if (currentStep == TutorialStep.G2_T2_ATTACK_WAIT) {
                currentStep = TutorialStep.G2_T2_ATTACK_RESOLVE;
            } else if (currentStep == TutorialStep.G2_T2_DEFENSE_WAIT) {
                currentStep = TutorialStep.G2_T2_DEFENSE_RESOLVE;
            } else if (currentStep == TutorialStep.G2_T3_ATTACK_WAIT) {
                currentStep = TutorialStep.G2_T3_ATTACK_RESOLVE;
            } else if (currentStep == TutorialStep.G2_T3_DEFENSE_WAIT) {
                currentStep = TutorialStep.G2_T3_DEFENSE_RESOLVE;
            }
        }
    }
}
