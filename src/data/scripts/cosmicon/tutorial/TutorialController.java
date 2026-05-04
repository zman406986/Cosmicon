package data.scripts.cosmicon.tutorial;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.BattleState.Phase;
import data.scripts.cosmicon.state.CosmiconEventState;
import data.scripts.cosmicon.state.CosmiconStats;

public class TutorialController {

    public enum TutorialGame {
        GAME_1_SPARXIE,
        GAME_2_ACHERON
    }

    public enum TutorialStep {
        G1_T1_ATTACK_ROLL,
        G1_T1_ATTACK_SELECT,
        G1_T1_ATTACK_CONFIRM,
        G1_T1_ATTACK_WAIT,
        G1_T1_ATTACK_RESOLVE,
        G1_T1_DEFENSE_ROLL,
        G1_T1_DEFENSE_SELECT,
        G1_T1_DEFENSE_CONFIRM,
        G1_T1_DEFENSE_WAIT,
        G1_T1_DEFENSE_RESOLVE,
        G1_T2_ATTACK_ROLL,
        G1_T2_ATTACK_REROLL,
        G1_T2_ATTACK_SELECT,
        G1_T2_ATTACK_CONFIRM,
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
        G2_T3_ATTACK_SELECT,
        G2_T3_ATTACK_CONFIRM,
        G2_VICTORY
    }

    private final TutorialGame game;
    private TutorialStep currentStep;
    private final BattleState battleState;
    private boolean complete;

    public TutorialController(TutorialGame game, BattleState battleState) {
        this.game = game;
        this.battleState = battleState;
        this.complete = false;
        this.currentStep = game == TutorialGame.GAME_1_SPARXIE
            ? TutorialStep.G1_T1_ATTACK_ROLL
            : TutorialStep.G2_T1_DEFENSE_ROLL;
    }

    public static TutorialGame determineTutorialGame() {
        int replayGame = CosmiconEventState.getReplayTutorialGame();
        if (replayGame >= 0) {
            return replayGame == 1 ? TutorialGame.GAME_1_SPARXIE : TutorialGame.GAME_2_ACHERON;
        }
        int gamesPlayed = CosmiconStats.getGamesPlayed();
        return gamesPlayed == 0 ? TutorialGame.GAME_1_SPARXIE : TutorialGame.GAME_2_ACHERON;
    }

    public static boolean shouldActivateTutorial() {
        return CosmiconEventState.isTutorialMode() || CosmiconStats.isInTutorialMode();
    }

    public TutorialGame getGame() {
        return game;
    }

    public boolean isTutorialComplete() {
        return complete;
    }

    public String getTutorialText() {
        String key = "tutorial." + currentStep.name().toLowerCase();
        try {
            return Strings.get(key);
        } catch (Exception e) {
            return "";
        }
    }

    public boolean isDiceClickable() {
        if (complete) return true;

        return switch (currentStep) {
            case G1_T1_ATTACK_SELECT, G1_T1_DEFENSE_SELECT,
                 G1_T2_ATTACK_REROLL, G1_T2_ATTACK_SELECT,
                 G2_T1_DEFENSE_SELECT,
                 G2_T2_ATTACK_SELECT,
                 G2_T2_DEFENSE_SELECT,
                 G2_T3_ATTACK_SELECT -> true;
            default -> false;
        };
    }

    public boolean isConfirmAllowed() {
        if (complete) return true;

        return switch (currentStep) {
            case G1_T1_ATTACK_CONFIRM, G1_T1_DEFENSE_CONFIRM,
                 G1_T2_ATTACK_CONFIRM,
                 G2_T1_DEFENSE_CONFIRM,
                 G2_T2_ATTACK_CONFIRM,
                 G2_T2_DEFENSE_CONFIRM,
                 G2_T3_ATTACK_CONFIRM -> true;
            default -> false;
        };
    }

    public boolean isRerollAllowed() {
        if (complete) return true;

        return currentStep == TutorialStep.G1_T2_ATTACK_REROLL;
    }

    public boolean isPrismaticAllowed() {
        if (complete) return false;

        return currentStep == TutorialStep.G2_T2_ATTACK_PRISMATIC;
    }

    public boolean isClickToRollAllowed() {
        if (complete) return true;

        return switch (currentStep) {
            case G1_T1_ATTACK_ROLL, G1_T1_DEFENSE_ROLL,
                 G1_T2_ATTACK_ROLL,
                 G2_T1_DEFENSE_ROLL,
                 G2_T2_ATTACK_ROLL,
                 G2_T2_DEFENSE_ROLL,
                 G2_T3_ATTACK_ROLL -> true;
            default -> false;
        };
    }

    public boolean isContinueAllowed() {
        if (complete) return false;

        return !switch (currentStep)
        {
            case G1_VICTORY, G2_VICTORY,
                 G1_T1_ATTACK_RESOLVE, G1_T1_DEFENSE_RESOLVE,
                 G2_T1_DEFENSE_RESOLVE,
                 G2_T2_ATTACK_RESOLVE, G2_T2_DEFENSE_RESOLVE -> true;
            default -> false;
        };
    }

    public void onDiceSelected() {
        if (complete) return;

        switch (currentStep) {
            case G1_T1_ATTACK_SELECT -> {
                if (battleState.countSelectedDice(true) == battleState.getRequiredDiceCount(true)) {
                    currentStep = TutorialStep.G1_T1_ATTACK_CONFIRM;
                }
            }
            case G1_T1_DEFENSE_SELECT -> {
                if (battleState.countSelectedDice(true) == battleState.getRequiredDiceCount(true)) {
                    currentStep = TutorialStep.G1_T1_DEFENSE_CONFIRM;
                }
            }
            case G1_T2_ATTACK_SELECT -> {
                if (battleState.countSelectedDice(true) == battleState.getRequiredDiceCount(true)) {
                    currentStep = TutorialStep.G1_T2_ATTACK_CONFIRM;
                }
            }
            case G2_T1_DEFENSE_SELECT -> {
                if (battleState.countSelectedDice(true) == battleState.getRequiredDiceCount(true)) {
                    currentStep = TutorialStep.G2_T1_DEFENSE_CONFIRM;
                }
            }
            case G2_T2_ATTACK_SELECT -> {
                if (battleState.countSelectedDice(true) == battleState.getRequiredDiceCount(true)) {
                    currentStep = TutorialStep.G2_T2_ATTACK_CONFIRM;
                }
            }
            case G2_T2_DEFENSE_SELECT -> {
                if (battleState.countSelectedDice(true) == battleState.getRequiredDiceCount(true)) {
                    currentStep = TutorialStep.G2_T2_DEFENSE_CONFIRM;
                }
            }
            case G2_T3_ATTACK_SELECT -> {
                if (battleState.countSelectedDice(true) == battleState.getRequiredDiceCount(true)) {
                    currentStep = TutorialStep.G2_T3_ATTACK_CONFIRM;
                }
            }
            default -> {}
        }
    }

    public void onConfirmed() {
        if (complete) return;

        switch (currentStep) {
            case G1_T1_ATTACK_CONFIRM -> currentStep = TutorialStep.G1_T1_ATTACK_WAIT;
            case G1_T1_DEFENSE_CONFIRM -> currentStep = TutorialStep.G1_T1_DEFENSE_WAIT;
            case G1_T2_ATTACK_CONFIRM -> currentStep = TutorialStep.G1_VICTORY;
            case G2_T1_DEFENSE_CONFIRM -> currentStep = TutorialStep.G2_T1_DEFENSE_WAIT;
            case G2_T2_ATTACK_CONFIRM -> currentStep = TutorialStep.G2_T2_ATTACK_WAIT;
            case G2_T2_DEFENSE_CONFIRM -> currentStep = TutorialStep.G2_T2_DEFENSE_WAIT;
            case G2_T3_ATTACK_CONFIRM -> currentStep = TutorialStep.G2_VICTORY;
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
        }
    }

    public void onPrismaticUsed() {
        if (complete) return;

        if (currentStep == TutorialStep.G2_T2_ATTACK_PRISMATIC) {
            currentStep = TutorialStep.G2_T2_ATTACK_SELECT;
        }
    }

    public void onContinueClicked() {
        if (complete) return;

        switch (currentStep) {
            case G1_T1_ATTACK_RESOLVE -> currentStep = TutorialStep.G1_T1_DEFENSE_ROLL;
            case G1_T1_DEFENSE_RESOLVE -> currentStep = TutorialStep.G1_T2_ATTACK_ROLL;
            case G2_T1_DEFENSE_RESOLVE -> currentStep = TutorialStep.G2_T2_ATTACK_ROLL;
            case G2_T2_ATTACK_RESOLVE -> currentStep = TutorialStep.G2_T2_DEFENSE_ROLL;
            case G2_T2_DEFENSE_RESOLVE -> currentStep = TutorialStep.G2_T3_ATTACK_ROLL;
            default -> {}
        }
    }

    public void onPhaseChange(Phase newPhase) {
        if (complete) return;

        if (game == TutorialGame.GAME_1_SPARXIE) {
            onGame1PhaseChange(newPhase);
        } else {
            onGame2PhaseChange(newPhase);
        }
    }

    private void onGame1PhaseChange(Phase newPhase) {
        boolean playerIsAttacker = battleState.isPlayerAttacker();

        if (newPhase == Phase.SELECTING_ATTACK && playerIsAttacker) {
            if (currentStep == TutorialStep.G1_T1_ATTACK_ROLL) {
                currentStep = TutorialStep.G1_T1_ATTACK_SELECT;
            } else if (currentStep == TutorialStep.G1_T2_ATTACK_ROLL) {
                currentStep = TutorialStep.G1_T2_ATTACK_REROLL;
            }
        } else if (newPhase == Phase.SELECTING_DEFENSE && !playerIsAttacker) {
            if (currentStep == TutorialStep.G1_T1_DEFENSE_ROLL) {
                currentStep = TutorialStep.G1_T1_DEFENSE_SELECT;
            }
        } else if (newPhase == Phase.WAITING_NEXT_TURN) {
            if (currentStep == TutorialStep.G1_T1_ATTACK_WAIT) {
                currentStep = TutorialStep.G1_T1_ATTACK_RESOLVE;
            } else if (currentStep == TutorialStep.G1_T1_DEFENSE_WAIT) {
                currentStep = TutorialStep.G1_T1_DEFENSE_RESOLVE;
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
            }
        } else if (newPhase == Phase.SELECTING_ATTACK && playerIsAttacker) {
            if (currentStep == TutorialStep.G2_T2_ATTACK_ROLL) {
                currentStep = TutorialStep.G2_T2_ATTACK_PRISMATIC;
            } else if (currentStep == TutorialStep.G2_T3_ATTACK_ROLL) {
                currentStep = TutorialStep.G2_T3_ATTACK_SELECT;
            }
        } else if (newPhase == Phase.WAITING_NEXT_TURN) {
            if (currentStep == TutorialStep.G2_T1_DEFENSE_WAIT) {
                currentStep = TutorialStep.G2_T1_DEFENSE_RESOLVE;
            } else if (currentStep == TutorialStep.G2_T2_ATTACK_WAIT) {
                currentStep = TutorialStep.G2_T2_ATTACK_RESOLVE;
            } else if (currentStep == TutorialStep.G2_T2_DEFENSE_WAIT) {
                currentStep = TutorialStep.G2_T2_DEFENSE_RESOLVE;
            }
        }
    }
}
