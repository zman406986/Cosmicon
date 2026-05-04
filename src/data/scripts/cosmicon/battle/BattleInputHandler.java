package data.scripts.cosmicon.battle;

import java.util.List;

import org.lwjgl.input.Mouse;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleState.Phase;
import data.scripts.cosmicon.tutorial.TutorialController;
import data.scripts.cosmicon.util.UnifiedCoord;

public class BattleInputHandler {
    private static final float DICE_SPACING = 90f;
    private static final float DICE_CLICK_PADDING = 1f;
    private static final float PRISMATIC_BTN_SIZE = 40f;

    private BattleController battleController;
    private BattleState battleState;
    private DiceRollManager diceRollManager;
    private BattleUILabels labels;
    private BattleUIButtons buttons;
    private DamageResolutionAnimator damageAnimator;
    private TutorialController tutorialController;

    private float panelX;
    private float panelY;
    private float diceZoneCenterX;
    private float diceZoneCenterY;

    private int lastMouseButtonState;
    private boolean waitingForClickToRoll;

    private final List<float[]> diceHitboxes;

    public BattleInputHandler(List<float[]> diceHitboxes) {
        this.diceHitboxes = diceHitboxes;
        this.lastMouseButtonState = 0;
        this.waitingForClickToRoll = false;
    }

    public void init(BattleController controller, BattleState state,
            DiceRollManager diceRollManager, BattleUILabels labels, BattleUIButtons buttons,
            float diceZoneCenterX, float diceZoneCenterY) {
        this.battleController = controller;
        this.battleState = state;
        this.diceRollManager = diceRollManager;
        this.labels = labels;
        this.buttons = buttons;
        this.diceZoneCenterX = diceZoneCenterX;
        this.diceZoneCenterY = diceZoneCenterY;
    }

    public void setDamageAnimator(DamageResolutionAnimator animator) {
        this.damageAnimator = animator;
    }

    public void setTutorialController(TutorialController controller) {
        this.tutorialController = controller;
    }

    public void setWaitingForClickToRoll(boolean waiting) {
        this.waitingForClickToRoll = waiting;
    }

    public void setPanelPosition(float x, float y) {
        this.panelX = x;
        this.panelY = y;
    }

    public void setBattleState(BattleState state) {
        this.battleState = state;
    }

    public void cleanup() {
        diceHitboxes.clear();
        battleController = null;
        battleState = null;
        diceRollManager = null;
        labels = null;
        buttons = null;
        damageAnimator = null;
        waitingForClickToRoll = false;
    }

    public void handleMouseInput() {
        UnifiedCoord.setCurrent(new UnifiedCoord.PanelContext(
            panelX, panelY, 0, BattleRenderingUtils.PANEL_HEIGHT));

        int currentButton = Mouse.isButtonDown(0) ? 1 : 0;

        if (currentButton == 1 && lastMouseButtonState == 0) {
            UnifiedCoord mousePos = UnifiedCoord.fromMouse();

            if (!buttons.isPrismaticPopupActive()) {
                boolean playerShouldSelect = (battleState.isAttacker(true) &&
                    battleState.getCurrentPhase() == Phase.SELECTING_ATTACK) ||
                    (battleState.isDefender(true) &&
                    battleState.getCurrentPhase() == Phase.SELECTING_DEFENSE);

                int uses = battleState.getPlayerPrismaticUses();

                if (playerShouldSelect && uses > 0) {
                    boolean prismaticAllowed = tutorialController == null || tutorialController.isPrismaticAllowed();
                    if (prismaticAllowed) {
                        boolean insidePrismatic = mousePos.isInsideRect(
                            buttons.getPlayerPrismaticBtnX(), buttons.getPlayerPrismaticBtnY(),
                            PRISMATIC_BTN_SIZE, PRISMATIC_BTN_SIZE);

                        if (insidePrismatic) {
                            buttons.showPrismaticSelectionPopup();
                            lastMouseButtonState = currentButton;
                            UnifiedCoord.clearCurrent();
                            return;
                        }
                    }
                }
            }

            if (buttons.isPrismaticPopupActive()) {
                lastMouseButtonState = currentButton;
                UnifiedCoord.clearCurrent();
                return;
            }

            if (battleState.getCurrentPhase() == Phase.ROLLING) {
                if (tutorialController != null && !tutorialController.isClickToRollAllowed()) {
                    lastMouseButtonState = currentButton;
                    UnifiedCoord.clearCurrent();
                    return;
                }

                if (waitingForClickToRoll && diceRollManager.isWaitingForRollTrigger()) {
                    diceRollManager.triggerRollFromStationary();
                    waitingForClickToRoll = false;
                    labels.hideClickHint();
                } else if (diceRollManager.hasAnimators() || diceRollManager.isWaitingForRollTrigger()) {
                    boolean showPlayerDice = battleState.isDefenderRolling() != battleState.isPlayerAttacker();
                    if (showPlayerDice && diceHitboxes.isEmpty()) {
                        List<DiceType> types = battleState.getPlayerDiceTypes();
                        if (types != null && !types.isEmpty()) {
                            createDiceHitboxes(types);
                        }
                    }

                    diceRollManager.forceCompleteAll();
                    labels.hideClickHint();
                    labels.updatePrismaticRolledLabel();

                    if (battleState.isDefenderRolling()) {
                        battleController.advanceToDefenderSelectPhase();
                    } else {
                        battleController.advanceToSelectPhase();
                    }

                }
                lastMouseButtonState = currentButton;
                UnifiedCoord.clearCurrent();
                return;
            }

            if (damageAnimator != null) {
                damageAnimator.forceComplete();
                lastMouseButtonState = currentButton;
                UnifiedCoord.clearCurrent();
                return;
            }

            if (battleState.getCurrentPhase() != Phase.SELECTING_ATTACK &&
                battleState.getCurrentPhase() != Phase.SELECTING_DEFENSE) {
                lastMouseButtonState = currentButton;
                UnifiedCoord.clearCurrent();
                return;
            }

            boolean playerShouldSelect = (battleState.isAttacker(true) &&
                                           battleState.getCurrentPhase() == Phase.SELECTING_ATTACK) ||
                                          (battleState.isDefender(true) &&
                                           battleState.getCurrentPhase() == Phase.SELECTING_DEFENSE);

            if (!playerShouldSelect) {
                lastMouseButtonState = currentButton;
                UnifiedCoord.clearCurrent();
                return;
            }

            for (int i = 0; i < diceHitboxes.size(); i++) {
                float[] hb = diceHitboxes.get(i);
                boolean inside = mousePos.isInsideRect(hb[0], hb[1], hb[2], hb[3]);
                if (inside) {
                    if (tutorialController != null && !tutorialController.isDiceClickable()) {
                        lastMouseButtonState = currentButton;
                        UnifiedCoord.clearCurrent();
                        return;
                    }
                    battleController.onPlayerSelectDice(i);
                    if (tutorialController != null) {
                        tutorialController.onDiceSelected();
                    }
                    buttons.updateButtons(battleState.getCurrentPhase());
                    labels.updateSelectionDisplayLabels();
                    break;
                }
            }
        }
        lastMouseButtonState = currentButton;
        UnifiedCoord.clearCurrent();
    }

    public void createDiceHitboxes(List<DiceType> types) {
        diceHitboxes.clear();
        int count = types.size();
        float maxDiceSize = AnimationConstants.DICE_SIZE;
        float hbSize = maxDiceSize + DICE_CLICK_PADDING * 2;

        int animatorCount = diceRollManager.getAnimatorCount();

        for (int i = 0; i < count; i++) {
            float slotX, slotY;

            if (i < animatorCount) {
                slotX = diceRollManager.getAnimatorTargetSlotX(i);
                slotY = diceRollManager.getAnimatorTargetSlotY(i);
            } else {
                float totalWidth = DICE_SPACING * (count - 1) + maxDiceSize;
                slotX = diceZoneCenterX - totalWidth / 2f + i * DICE_SPACING;
                slotY = diceZoneCenterY - maxDiceSize / 2f;
            }

            if (slotX < 0 || slotY < 0) {
                float totalWidth = DICE_SPACING * (count - 1) + maxDiceSize;
                slotX = diceZoneCenterX - totalWidth / 2f + i * DICE_SPACING;
                slotY = diceZoneCenterY - maxDiceSize / 2f;
            }

            float hbX = slotX - DICE_CLICK_PADDING;
            float hbY = slotY - DICE_CLICK_PADDING;
            diceHitboxes.add(new float[]{hbX, hbY, hbSize, hbSize});
        }
    }

    

    public void updateClickHintForRoll() {
        if (waitingForClickToRoll && diceRollManager.isWaitingForRollTrigger()) {
            labels.showClickHint(Strings.get("battle.click_to_roll"), 0.8f);
        }
    }
}