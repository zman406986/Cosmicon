package data.scripts.cosmicon.battle;

import java.util.List;

import org.lwjgl.input.Mouse;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleState.Phase;
import data.scripts.cosmicon.util.CoordHelper;
import data.scripts.cosmicon.util.CosmiconLogger;

public class BattleInputHandler {
    private static final float DICE_SPACING = 70f;
    private static final float DICE_CLICK_PADDING = 5f;
    private static final float PRISMATIC_BTN_SIZE = 40f;

    private BattleController battleController;
    private BattleState battleState;
    private DiceRollManager diceRollManager;
    private BattleUILabels labels;
    private BattleUIButtons buttons;
    private DamageResolutionAnimator damageAnimator;

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
        int currentButton = Mouse.isButtonDown(0) ? 1 : 0;

        if (currentButton == 1 && lastMouseButtonState == 0) {
            CosmiconLogger.debug("[CLICK] Mouse clicked - Phase: %s, panelX=%.0f, panelY=%.0f",
                battleState.getCurrentPhase().name(), panelX, panelY);

            int mouseX = Mouse.getX();
            int mouseY = Mouse.getY();
            float[] uiPos = CoordHelper.mouseToPanelUi(mouseX, mouseY,
                panelX, panelY, BattleRenderingUtils.PANEL_HEIGHT);
            float panelUiX = uiPos[0];
            float panelUiY = uiPos[1];

            if (!buttons.isPrismaticPopupActive()) {
                boolean playerShouldSelect = (battleState.isAttacker(true) &&
                    battleState.getCurrentPhase() == Phase.SELECTING_ATTACK) ||
                    (battleState.isDefender(true) &&
                    battleState.getCurrentPhase() == Phase.SELECTING_DEFENSE);

                int uses = battleState.getPlayerPrismaticUses();

                if (playerShouldSelect && uses > 0) {
                    boolean insidePrismatic = CoordHelper.isInsideUiRect(panelUiX, panelUiY,
                        buttons.getPlayerPrismaticBtnX(), buttons.getPlayerPrismaticBtnY(), PRISMATIC_BTN_SIZE, PRISMATIC_BTN_SIZE);

                    if (insidePrismatic) {
                        CosmiconLogger.info("Player clicked Prismatic button via processInput");
                        buttons.showPrismaticSelectionPopup();
                        lastMouseButtonState = currentButton;
                        return;
                    }
                }
            }

            if (battleState.getCurrentPhase() == Phase.ROLLING) {
                if (waitingForClickToRoll && diceRollManager.isWaitingForRollTrigger()) {
                    CosmiconLogger.info("Player clicked to trigger dice roll");
                    diceRollManager.triggerRollFromStationary();
                    waitingForClickToRoll = false;
                    labels.hideClickHint();
                } else {
                    CosmiconLogger.debug("Player clicked to skip dice roll animation");
                    diceRollManager.forceCompleteAll();
                    labels.hideClickHint();

                    boolean showPlayerDice = battleState.isDefenderRolling() != battleState.isPlayerAttacker();
                    if (showPlayerDice && diceHitboxes.isEmpty()) {
                        List<DiceType> types = battleState.getPlayerDiceTypes();
                        if (types != null && !types.isEmpty()) {
                            createDiceHitboxes(types);
                        }
                    }

                    labels.updatePrismaticRolledLabel();

                    if (battleState.isDefenderRolling()) {
                        battleController.advanceToDefenderSelectPhase();
                    } else {
                        battleController.advanceToSelectPhase();
                    }

                }
                lastMouseButtonState = currentButton;
                return;
            }

            if (damageAnimator != null) {
                CosmiconLogger.debug("Player clicked to skip damage animation");
                damageAnimator.forceComplete();
                lastMouseButtonState = currentButton;
                return;
            }

            if (battleState.getCurrentPhase() != Phase.SELECTING_ATTACK &&
                battleState.getCurrentPhase() != Phase.SELECTING_DEFENSE) {
                CosmiconLogger.debug("[CLICK] Wrong phase for dice selection: %s", battleState.getCurrentPhase().name());
                lastMouseButtonState = currentButton;
                return;
            }

            boolean playerShouldSelect = (battleState.isAttacker(true) &&
                                           battleState.getCurrentPhase() == Phase.SELECTING_ATTACK) ||
                                          (battleState.isDefender(true) &&
                                           battleState.getCurrentPhase() == Phase.SELECTING_DEFENSE);

            if (!playerShouldSelect) {
                CosmiconLogger.debug("[CLICK] Player should not select");
                lastMouseButtonState = currentButton;
                return;
            }

            CosmiconLogger.debug("[CLICK] Mouse screen: (%d, %d), Panel UI: (%.0f, %.0f), hitboxCount=%d",
                mouseX, mouseY, panelUiX, panelUiY, diceHitboxes.size());

            for (int i = 0; i < diceHitboxes.size(); i++) {
                float[] hb = diceHitboxes.get(i);
                boolean inside = CoordHelper.isInsideUiRect(panelUiX, panelUiY, hb[0], hb[1], hb[2], hb[3]);
                CosmiconLogger.debug("[CLICK] Hitbox %d: UI(%.0f,%.0f) size(%.0f,%.0f) - point (%.0f,%.0f) inside=%s",
                    i, hb[0], hb[1], hb[2], hb[3], panelUiX, panelUiY, inside);
                if (inside) {
                    logDiceSelection(i);
                    battleController.onPlayerSelectDice(i);
                    buttons.updateButtons(battleState.getCurrentPhase());
                    labels.updateSelectionDisplayLabels();
                    break;
                }
            }
        }
        lastMouseButtonState = currentButton;
    }

    private void logDiceSelection(int diceIndex) {
        List<DiceType> types = battleState.getPlayerDiceTypes();
        List<Integer> values = battleState.getPlayerDiceValues();
        List<Boolean> selected = battleState.getPlayerDiceSelected();

        if (types == null || values == null || diceIndex >= types.size()) {
            CosmiconLogger.debug("Player clicked dice %d", diceIndex);
            return;
        }

        DiceType type = types.get(diceIndex);
        int value = values.get(diceIndex);
        boolean wasSelected = selected != null && diceIndex < selected.size() && selected.get(diceIndex);
        String action = wasSelected ? "deselected" : "selected";

        CosmiconLogger.info("Player %s dice %d (%s, value: %d)", action, diceIndex, type.name(), value);
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

    public List<float[]> getDiceHitboxes() {
        return diceHitboxes;
    }

    public void updateClickHintForRoll() {
        if (waitingForClickToRoll && diceRollManager.isWaitingForRollTrigger()) {
            labels.showClickHint(Strings.get("battle.click_to_roll"), 0.8f);
        }
    }
}