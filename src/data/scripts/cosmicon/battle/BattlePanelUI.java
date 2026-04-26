package data.scripts.cosmicon.battle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate.DialogCallbacks;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import data.scripts.CosmiconConfig;
import data.scripts.cosmicon.battle.BattleState.BattleEventListener;
import data.scripts.cosmicon.battle.BattleState.Phase;
import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.CoordHelper;
import data.scripts.cosmicon.util.CosmiconLogger;
import data.scripts.cosmicon.util.GLStateUtil;

public class BattlePanelUI extends BaseCustomUIPanelPlugin implements BattleEventListener {

    private static final float PRISMATIC_BTN_SIZE = 40f;
    private static final float OPPONENT_AUTO_ROLL_DELAY = 0.8f;
    private static final float ROLE_TRANSITION_DURATION = 0.4f;
    private static final float PRE_CLASH_DELAY = 1.5f;

    private CustomPanelAPI panel;
    private BattleController battleController;
    private BattleState battleState;
    private DiceRollManager diceRollManager;

    private BattleUILabels labels;
    private BattleUIButtons buttons;
    private BattleInputHandler inputHandler;

    private float panelX;
    private float panelY;
    private float diceZoneCenterX;
    private float diceZoneCenterY;
    private float rollAnimationDelay;
    private boolean diceAnimating;
    private boolean dicePreviewActive;
    private float dicePreviewDelay;
    private float opponentAutoRollDelay;

    private boolean opponentDiceAnimating;
    private float opponentRollDelay;

    private float roleTransitionProgress;
    private float targetRoleTransition;
    private boolean lastPlayerWasAttacker;
    private float cachedRotationAngle;

    private float opponentPrismaticBtnX;
    private float opponentPrismaticBtnY;

    private DamageResolutionAnimator damageAnimator;
    private DamageResolver.DamageResult pendingDamageResult;
    private boolean damageAnimationPending;

    private boolean valueAnimationPending;

    private float preClashTimer;

    public BattlePanelUI() {
        List<float[]> diceHitboxes = new ArrayList<>();
        this.inputHandler = new BattleInputHandler(diceHitboxes);
        this.roleTransitionProgress = 0f;
        this.targetRoleTransition = 0f;
        this.lastPlayerWasAttacker = true;
        this.cachedRotationAngle = 0f;
        this.opponentDiceAnimating = false;
        this.opponentRollDelay = 0f;
        this.dicePreviewActive = false;
        this.dicePreviewDelay = 0f;
        this.opponentAutoRollDelay = 0f;
        this.valueAnimationPending = false;
        this.preClashTimer = 0f;
    }

    public void setBattleController(BattleController controller) {
        this.battleController = controller;
        if (controller != null) {
            this.battleState = controller.getState();
            battleState.addListener(this);
            if (labels != null) labels.setBattleState(battleState);
            if (buttons != null) buttons.setBattleState(battleState);
            if (inputHandler != null) inputHandler.setBattleState(battleState);
        }
    }

    public void updateLabelsFromState() {
        if (labels != null) {
            labels.updateLabelsFromState();
        }
    }

    public void cleanup() {
        CosmiconLogger.info("Battle dialog closed");
        if (battleState != null) {
            battleState.removeListener(this);
        }
        if (labels != null) {
            labels.cleanup();
            labels = null;
        }
        if (buttons != null) {
            buttons.cleanup();
            buttons = null;
        }
        if (inputHandler != null) {
            inputHandler.cleanup();
            inputHandler = null;
        }
        if (damageAnimator != null) {
            damageAnimator.cleanup();
            damageAnimator = null;
        }
        diceRollManager = null;
        damageAnimationPending = false;
        valueAnimationPending = false;
        pendingDamageResult = null;
        battleState = null;
        battleController = null;
        opponentAutoRollDelay = 0f;
        preClashTimer = 0f;
    }

    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        CosmiconLogger.info("Battle dialog opened");
        this.panel = panel;
        callbacks.getPanelFader().setDurationOut(0.5f);

        this.diceRollManager = new DiceRollManager();
        diceRollManager.init();

        PositionAPI pos = panel.getPosition();
        panelX = pos.getX();
        panelY = pos.getY();
        diceZoneCenterX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        diceZoneCenterY = BattleRenderingUtils.PANEL_HEIGHT / 2f - 40f;

        labels = new BattleUILabels();
        buttons = new BattleUIButtons();

        float opponentCardX = BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN;
        float opponentCardY = BattleRenderingUtils.MARGIN;
        opponentPrismaticBtnX = opponentCardX - 60f;
        opponentPrismaticBtnY = opponentCardY + 40f;
        labels.init(panel, battleState, diceRollManager,
            opponentPrismaticBtnX, opponentPrismaticBtnY);
        buttons.init(panel, callbacks, battleController, battleState, diceRollManager, labels,
            inputHandler, diceZoneCenterX, diceZoneCenterY);
        inputHandler.init(battleController, battleState, diceRollManager, labels, buttons,
            diceZoneCenterX, diceZoneCenterY);

        if (battleState != null) {
            labels.updateLabelsFromState();
            lastPlayerWasAttacker = battleState.isPlayerAttacker();
            roleTransitionProgress = lastPlayerWasAttacker ? 0f : 1f;
            targetRoleTransition = roleTransitionProgress;
        }
    }

    public void startDamageResolutionAnimation(BattleState state, DamageResolver.DamageResult result) {
        pendingDamageResult = result;
        damageAnimationPending = true;
        inputHandler.setDamageAnimator(damageAnimator);

        float playerCardX = BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN;
        float playerCardY = BattleRenderingUtils.PANEL_HEIGHT - BattleRenderingUtils.CARD_HEIGHT - BattleRenderingUtils.MARGIN;
        float playerCardCenterX = playerCardX + BattleRenderingUtils.CARD_WIDTH / 2f;
        float playerCardCenterY = playerCardY + BattleRenderingUtils.CARD_HEIGHT / 2f;

        float opponentCardX = BattleRenderingUtils.MARGIN;
        float opponentCardY = BattleRenderingUtils.MARGIN;
        float opponentCardCenterX = opponentCardX + BattleRenderingUtils.CARD_WIDTH / 2f;
        float opponentCardCenterY = opponentCardY + BattleRenderingUtils.CARD_HEIGHT / 2f;

        float defenderTargetX = state.isPlayerAttacker() ? opponentCardCenterX : playerCardCenterX;
        float defenderTargetY = state.isPlayerAttacker() ? opponentCardCenterY : playerCardCenterY;

        damageAnimator = new DamageResolutionAnimator();
        damageAnimator.startResolution(state, defenderTargetX, defenderTargetY, panel);
        inputHandler.setDamageAnimator(damageAnimator);
    }

    @Override
    public void onPhaseChange(Phase newPhase) {
        if (newPhase == Phase.ROLLING) {
            rollAnimationDelay = 0.3f;
            diceAnimating = true;
            labels.clearPrismaticRolledLabel();

            if (diceRollManager != null) {
                diceRollManager.clear();
                diceRollManager.clearOpponentAnimators();
            }
        }

        if (newPhase == Phase.RESOLVING_PRE_CLASH) {
            boolean hasAttackerChanges = !battleState.getPendingValueChanges(battleState.isPlayerAttacker()).isEmpty();
            boolean hasDefenderChanges = !battleState.getPendingValueChanges(!battleState.isPlayerAttacker()).isEmpty();

            if (hasAttackerChanges || hasDefenderChanges) {
                startValueChangeAnimations();
            }
        }

        if (labels.getPhaseLabel() == null) return;
        labels.updatePhaseLabel();
        buttons.updateButtons(newPhase);
    }

    @Override
    public void onDiceRerolled(boolean isPlayer, List<Integer> newValues, List<Integer> rerolledIndices) {
        if (diceRollManager == null || rerolledIndices.isEmpty()) return;

        float animDuration = DiceAnimator.getTotalDuration() + 0.1f;

        if (isPlayer) {
            diceRollManager.partialReroll(rerolledIndices, newValues);
            rollAnimationDelay = animDuration;
            diceAnimating = true;
        } else {
            diceRollManager.rerollOpponentDice(rerolledIndices, newValues);
            opponentRollDelay = animDuration;
            opponentDiceAnimating = true;
        }

        if (labels.getPhaseLabel() == null) return;
        buttons.updateButtons(battleState.getCurrentPhase());
        labels.updatePhaseLabel();
    }

    @Override
    public void onDamageResolved(int damage, int playerHp, int opponentHp) {
        labels.updateLabelsFromState();
    }

    @Override
    public void onBattleEnd(String winner) {
        labels.updatePhaseLabel();
    }

    @Override
    public void onDiceRolled(boolean isPlayer, List<DiceType> types, List<Integer> values) {
        labels.updateDiceRolledCounts(isPlayer, types);
        
        if (!isPlayer && types != null) {
            triggerOpponentDiceRoll();
        }
    }

    @Override
    public void onWeatherChange(WeatherType newWeather) {
    }

    @Override
    public void onDamageAnimationStart(DamageResolver.DamageResult result) {
        if (battleState == null) return;

        pendingDamageResult = result;

        boolean hasAttackerChanges = !battleState.getPendingValueChanges(battleState.isPlayerAttacker()).isEmpty();
        boolean hasDefenderChanges = !battleState.getPendingValueChanges(!battleState.isPlayerAttacker()).isEmpty();

        if (hasAttackerChanges || hasDefenderChanges) {
            startValueChangeAnimations();
            valueAnimationPending = true;
        } else {
            startDamageResolutionAnimation(battleState, result);
        }
    }

    private void startValueChangeAnimations() {
        if (battleState == null || panel == null) return;

        float halfH = BattleRenderingUtils.PANEL_HEIGHT / 2f;
        float iconSize = halfH * BattleRenderingUtils.ROLE_ICON_SIZE_RATIO;

        float topIconCenterX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float topIconCenterY = (halfH - iconSize) / 2f + iconSize / 2f;
        float bottomIconCenterX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float bottomIconCenterY = halfH + (halfH - iconSize) / 2f + iconSize / 2f;

        boolean playerIsAttacker = battleState.isPlayerAttacker();

        float attackerCenterX = playerIsAttacker ? bottomIconCenterX : topIconCenterX;
        float attackerCenterY = playerIsAttacker ? bottomIconCenterY : topIconCenterY;
        float defenderCenterX = playerIsAttacker ? topIconCenterX : bottomIconCenterX;
        float defenderCenterY = playerIsAttacker ? topIconCenterY : bottomIconCenterY;

        int attackValue = battleState.getAttackValue();
        int defenseValue = battleState.getDefenseValue();

        List<BattleState.ValueChangeRecord> attackerChanges = battleState.getPendingValueChanges(playerIsAttacker);
        List<BattleState.ValueChangeRecord> defenderChanges = battleState.getPendingValueChanges(!playerIsAttacker);

        ValueChangeAnimator attackerAnimator = labels.getAttackerValueAnimator();
        ValueChangeAnimator defenderAnimator = labels.getDefenderValueAnimator();

        if (!attackerChanges.isEmpty() && attackerAnimator != null) {
            attackerAnimator.start(attackerCenterX, attackerCenterY, attackValue, panel, true);
        }
        if (!defenderChanges.isEmpty() && defenderAnimator != null) {
            defenderAnimator.start(defenderCenterX, defenderCenterY, defenseValue, panel, false);
        }

        battleState.clearPendingValueChanges();
    }

    @Override
    public void onDamageAnimationComplete() {
        labels.updateLabelsFromState();
    }

    @Override
    public void onValueChange(boolean isPlayer, String changeType, int oldValue, int newValue, int delta) {
        boolean playerIsAttacker = battleState.isPlayerAttacker();
        boolean affectsAttackerValue = (playerIsAttacker == isPlayer &&
            (changeType.equals("ATTACK_LEVEL_UP") || changeType.equals("ATTACK"))) ||
            (playerIsAttacker != isPlayer &&
            (changeType.equals("DEFENSE_LEVEL_UP") || changeType.equals("DEFENSE")));

        ValueChangeAnimator targetAnimator = affectsAttackerValue ?
            labels.getAttackerValueAnimator() : labels.getDefenderValueAnimator();
        Color color = getChangeColor(changeType, delta);
        String displayText = delta >= 0 ? "+" + delta : String.valueOf(delta);
        if (targetAnimator != null) {
            targetAnimator.queueChange(displayText, color);
        }
    }

    private Color getChangeColor(String changeType, int delta) {
        return switch (changeType) {
            case "AWAKENING", "LEVEL_UP", "ATTACK_LEVEL_UP", "DEFENSE_LEVEL_UP" -> ColorHelper.PRISMATIC_GOLD;
            case "WEATHER" -> ColorHelper.WEATHER_BONUS;
            case "PRISMATIC" -> ColorHelper.PRISMATIC_BRIGHT;
            case "PASSIVE" -> delta > 0 ? ColorHelper.ATTACK_VALUE : ColorHelper.DEFENSE_VALUE;
            default -> delta >= 0 ? Color.GREEN : Color.RED;
        };
    }

    @Override
    public void advance(float amount) {
        if (battleState == null) return;

        updateRoleTransition(amount);
        labels.updateSelectionDisplayLabels();
        labels.updateConfirmedSelectionLabels();
        labels.updateIconValueLabels();

        if (battleController != null) {
            battleController.advanceAiSelection(amount);
        }

        buttons.advance(amount);

        if (battleState.getCurrentPhase() == Phase.RESOLVING_PRE_CLASH) {
            preClashTimer += amount;

            ValueChangeAnimator attackerAnimator = labels.getAttackerValueAnimator();
            ValueChangeAnimator defenderAnimator = labels.getDefenderValueAnimator();

            if (attackerAnimator != null) {
                attackerAnimator.advance(amount);
            }
            if (defenderAnimator != null) {
                defenderAnimator.advance(amount);
            }

            if (preClashTimer >= PRE_CLASH_DELAY) {
                boolean attackerComplete = attackerAnimator == null || attackerAnimator.isComplete();
                boolean defenderComplete = defenderAnimator == null || defenderAnimator.isComplete();

                if (attackerComplete && defenderComplete) {
                    preClashTimer = 0f;
                    if (battleController != null) {
                        battleController.proceedToClash();
                    }
                }
            }
            return;
        }

        if (diceAnimating) {
            rollAnimationDelay -= amount;

            boolean hasPlayerAnimators = diceRollManager.hasAnimators();
            boolean hasOpponentAnimators = diceRollManager.hasOpponentAnimators();

            if (hasPlayerAnimators || hasOpponentAnimators) {
                boolean playerComplete = !hasPlayerAnimators || diceRollManager.isComplete();
                boolean opponentComplete = !hasOpponentAnimators || diceRollManager.isOpponentComplete();

                if (playerComplete && opponentComplete && rollAnimationDelay <= 0f) {
                    diceAnimating = false;
                    labels.updatePrismaticRolledLabel();
                    List<DiceType> playerTypes = battleState.getPlayerDiceTypes();
                    if (playerTypes != null && !playerTypes.isEmpty() && hasPlayerAnimators) {
                        inputHandler.createDiceHitboxes(playerTypes);
                    }
                    if (battleState.getCurrentPhase() == Phase.ROLLING) {
                        dicePreviewActive = true;
                        dicePreviewDelay = CosmiconConfig.DICE_PREVIEW_DELAY;
                    }
                }
            } else if (rollAnimationDelay <= 0f) {
                diceAnimating = false;

                boolean isDefenderRolling = battleState.isDefenderRolling();
                boolean showPlayerDice = isDefenderRolling != battleState.isPlayerAttacker();
                boolean anyDiceStarted = false;

                CosmiconLogger.debug("[ANIM] Starting stationary preview - isDefenderRolling=%s, showPlayerDice=%s, playerIsAttacker=%s",
                    isDefenderRolling, showPlayerDice, battleState.isPlayerAttacker());

                if (showPlayerDice) {
                    List<DiceType> types = battleState.getPlayerDiceTypes();
                    List<Integer> values = battleState.getPlayerDiceValues();
                    if (types != null && values != null && !types.isEmpty()) {
                        diceRollManager.startStationaryPreview(types, values, diceZoneCenterX, diceZoneCenterY);
                        inputHandler.setWaitingForClickToRoll(true);
                        anyDiceStarted = true;
                        CosmiconLogger.debug("[ANIM] Player stationary preview started - %d dice, waiting for click", types.size());
                    }
                } else {
                    List<DiceType> types = battleState.getOpponentDiceTypes();
                    List<Integer> values = battleState.getOpponentDiceValues();
                    if (types != null && values != null && !types.isEmpty()) {
                        diceRollManager.startOpponentStationaryPreview(types, values, diceZoneCenterX, diceZoneCenterY);
                        opponentAutoRollDelay = OPPONENT_AUTO_ROLL_DELAY;
                        anyDiceStarted = true;
                        CosmiconLogger.debug("[ANIM] Opponent stationary preview started - %d dice, auto-roll in %.1fs", types.size(), OPPONENT_AUTO_ROLL_DELAY);
                    }
                }

                if (!anyDiceStarted && battleState.getCurrentPhase() == Phase.ROLLING) {
                    if (isDefenderRolling) {
                        battleController.advanceToDefenderSelectPhase();
                    } else {
                        battleController.advanceToSelectPhase();
                    }
                }
            }
        }

        inputHandler.updateClickHintForRoll();

        if (opponentAutoRollDelay > 0f) {
            opponentAutoRollDelay -= amount;
            if (opponentAutoRollDelay <= 0f && diceRollManager.isOpponentWaitingForRollTrigger()) {
                diceRollManager.triggerOpponentRollFromStationary();
                opponentDiceAnimating = true;
                opponentRollDelay = DiceAnimator.getTotalDuration() + 0.1f;
                CosmiconLogger.debug("[ANIM] Opponent auto-roll triggered");
            }
        }

        if (dicePreviewActive) {
            dicePreviewDelay -= amount;
            if (dicePreviewDelay <= 0f) {
                dicePreviewActive = false;
                if (battleState.isDefenderRolling()) {
                    battleController.advanceToDefenderSelectPhase();
                } else {
                    battleController.advanceToSelectPhase();
                }
            }
        }

        if (opponentDiceAnimating) {
            opponentRollDelay -= amount;

            if (diceRollManager.hasOpponentAnimators()) {
                if (diceRollManager.isOpponentComplete() && opponentRollDelay <= 0f) {
                    opponentDiceAnimating = false;
                }
            } else if (opponentRollDelay <= 0f) {
                startOpponentDiceAnimation();
            }
        }

        diceRollManager.advance(amount);

        ValueChangeAnimator attackerAnimator = labels.getAttackerValueAnimator();
        ValueChangeAnimator defenderAnimator = labels.getDefenderValueAnimator();

        if (attackerAnimator != null) {
            attackerAnimator.advance(amount);
        }
        if (defenderAnimator != null) {
            defenderAnimator.advance(amount);
        }

        if (valueAnimationPending) {
            boolean attackerComplete = attackerAnimator == null || attackerAnimator.isComplete();
            boolean defenderComplete = defenderAnimator == null || defenderAnimator.isComplete();

            if (attackerComplete && defenderComplete) {
                valueAnimationPending = false;
                startDamageResolutionAnimation(battleState, pendingDamageResult);
            }
        }

        if (damageAnimator != null) {
            damageAnimator.advance(amount);

            if (!damageAnimator.isComplete()) {
                labels.showClickHint("battle.click_to_continue", 0.6f);
            } else {
                labels.hideClickHint();
            }

            if (damageAnimator.isComplete() && damageAnimationPending) {
                damageAnimationPending = false;
                damageAnimator.cleanup();
                damageAnimator = null;
                inputHandler.setDamageAnimator(null);
                labels.hideClickHint();

                if (battleState != null) {
                    battleState.notifyDamageAnimationComplete();
                }
            }
        } else {
            labels.hideClickHint();
        }

        PositionAPI pos = panel.getPosition();
        panelX = pos.getX();
        panelY = pos.getY();
        inputHandler.setPanelPosition(panelX, panelY);

        inputHandler.handleMouseInput();
    }

    private void updateRoleTransition(float amount) {
        boolean currentPlayerIsAttacker = battleState.isPlayerAttacker();

        if (currentPlayerIsAttacker != lastPlayerWasAttacker) {
            lastPlayerWasAttacker = currentPlayerIsAttacker;
            targetRoleTransition = currentPlayerIsAttacker ? 0f : 1f;
        }

        if (roleTransitionProgress != targetRoleTransition) {
            float diff = targetRoleTransition - roleTransitionProgress;
            float step = Math.abs(diff) / ROLE_TRANSITION_DURATION * amount;

            if (Math.abs(diff) <= step) {
                roleTransitionProgress = targetRoleTransition;
            } else {
                roleTransitionProgress += diff > 0 ? step : -step;
            }
        }

        cachedRotationAngle = getSmoothedTransition() * 180f;
    }

    private float getSmoothedTransition() {
        float t = roleTransitionProgress;
        return t * t * (3f - 2f * t);
    }

    private boolean shouldShowOpponentDice() {
        if (battleState == null) return false;

        BattleState.Phase phase = battleState.getCurrentPhase();
        AISelectionVisualizer viz = battleState.getAiSelectionVisualizer();

        boolean aiIsSelecting = (phase == BattleState.Phase.SELECTING_ATTACK && !battleState.isPlayerAttacker()) ||
                                (phase == BattleState.Phase.SELECTING_DEFENSE && battleState.isPlayerAttacker());

        boolean vizActive = viz != null && viz.hasStarted();

        boolean opponentRolling = phase == BattleState.Phase.ROLLING &&
                                  diceRollManager != null &&
                                  diceRollManager.hasOpponentAnimators();

        return aiIsSelecting || vizActive || opponentRolling;
    }

    private void startOpponentDiceAnimation() {
        if (battleState == null || diceRollManager == null) return;

        List<DiceType> types = battleState.getOpponentDiceTypes();
        List<Integer> values = battleState.getOpponentDiceValues();

        if (types == null || values == null || types.isEmpty()) {
            opponentDiceAnimating = false;
            return;
        }

        diceRollManager.startOpponentStationaryPreview(types, values, diceZoneCenterX, diceZoneCenterY);
        opponentAutoRollDelay = OPPONENT_AUTO_ROLL_DELAY;
    }

    public void triggerOpponentDiceRoll() {
        if (battleState == null || diceRollManager == null) return;

        List<DiceType> types = battleState.getOpponentDiceTypes();
        List<Integer> values = battleState.getOpponentDiceValues();

        if (types != null && values != null && !types.isEmpty()) {
            diceRollManager.startOpponentStationaryPreview(types, values, diceZoneCenterX, diceZoneCenterY);
            opponentAutoRollDelay = OPPONENT_AUTO_ROLL_DELAY;
        }
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (battleState == null || diceRollManager == null) return;

        PositionAPI pos = panel.getPosition();
        float x = pos.getX();
        float y = pos.getY();
        float w = pos.getWidth();
        float h = pos.getHeight();

        GLStateUtil.resetBlendState();

        boolean hideRoleIcons = damageAnimator != null && damageAnimator.isIconClashActive();

        BattleRenderingUtils.renderBattleBackground(x, y, w, h,
            cachedRotationAngle, alphaMult, battleState != null, hideRoleIcons);

        renderPlayerCard(x, y, alphaMult);
        renderOpponentCard(x, y, alphaMult);
        renderDiceZone(x, y, alphaMult);

        ValueChangeAnimator attackerAnimator = labels.getAttackerValueAnimator();
        ValueChangeAnimator defenderAnimator = labels.getDefenderValueAnimator();

        if (attackerAnimator != null && !attackerAnimator.isComplete()) {
            attackerAnimator.render(panelX, panelY, BattleRenderingUtils.PANEL_HEIGHT, alphaMult);
        }
        if (defenderAnimator != null && !defenderAnimator.isComplete()) {
            defenderAnimator.render(panelX, panelY, BattleRenderingUtils.PANEL_HEIGHT, alphaMult);
        }

        diceRollManager.render(panelX, panelY, BattleRenderingUtils.PANEL_HEIGHT, alphaMult);

        if (damageAnimator != null && !damageAnimator.isComplete()) {
            damageAnimator.render(panelX, panelY, BattleRenderingUtils.PANEL_HEIGHT, alphaMult);
        }

        if (shouldShowOpponentDice()) {
            renderOpponentDiceZone(alphaMult);
        }

        renderDiceSelectionHighlights(alphaMult);
        renderOpponentPrismaticButton(alphaMult);
        renderPrismaticButton(alphaMult);

        if (buttons.isPrismaticPopupActive() && buttons.getPrismaticPopup() != null) {
            buttons.getPrismaticPopup().renderBelow(alphaMult);
        }

        GLStateUtil.resetColor();
    }

    private void renderPlayerCard(float panelX, float panelY, float alphaMult) {
        float cardX = panelX + BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN;
        float cardY = CoordHelper.uiTopLeftToGlSpriteY(panelY, BattleRenderingUtils.PANEL_HEIGHT,
            BattleRenderingUtils.PANEL_HEIGHT - BattleRenderingUtils.CARD_HEIGHT - BattleRenderingUtils.MARGIN,
            BattleRenderingUtils.CARD_HEIGHT);

        if (battleState != null) {
            CharacterCard card = battleState.getPlayerCard();
            BattleRenderingUtils.renderCharacterCard(cardX, cardY, card, alphaMult);

            float passiveX = cardX - 20;
            float passiveY = cardY + BattleRenderingUtils.CARD_HEIGHT + 10;
            BattleRenderingUtils.renderPassiveBox(passiveX, passiveY,
                BattleRenderingUtils.CARD_WIDTH + 40, 60, alphaMult);
        } else {
            Color playerCardColor = ColorHelper.PLAYER_CARD_PLACEHOLDER;
            BattleRenderingUtils.renderCardPlaceholder(cardX, cardY, BattleRenderingUtils.CARD_WIDTH,
                BattleRenderingUtils.CARD_HEIGHT, playerCardColor, alphaMult);
        }
    }

    private void renderOpponentCard(float panelX, float panelY, float alphaMult) {
        float cardX = panelX + BattleRenderingUtils.MARGIN;
        float cardY = CoordHelper.uiTopLeftToGlSpriteY(panelY, BattleRenderingUtils.PANEL_HEIGHT,
            BattleRenderingUtils.MARGIN, BattleRenderingUtils.CARD_HEIGHT);

        if (battleState != null) {
            CharacterCard card = battleState.getOpponentCard();
            BattleRenderingUtils.renderCharacterCard(cardX, cardY, card, alphaMult);

            float passiveX = cardX - 20;
            float passiveY = cardY - 70;
            BattleRenderingUtils.renderPassiveBox(passiveX, passiveY,
                BattleRenderingUtils.CARD_WIDTH + 40, 60, alphaMult);
        } else {
            Color opponentCardColor = ColorHelper.OPPONENT_CARD_PLACEHOLDER;
            BattleRenderingUtils.renderCardPlaceholder(cardX, cardY, BattleRenderingUtils.CARD_WIDTH,
                BattleRenderingUtils.CARD_HEIGHT, opponentCardColor, alphaMult);
        }
    }

    private void renderDiceZone(float panelX, float panelY, float alphaMult) {
        float zoneW = 400f;
        float zoneH = 160f;
        float zoneX = panelX + (BattleRenderingUtils.PANEL_WIDTH - zoneW) / 2f;
        float zoneY = panelY + (BattleRenderingUtils.PANEL_HEIGHT - zoneH) / 2f - 40f;

        BattleRenderingUtils.renderDiceZone(zoneX, zoneY, zoneW, zoneH, alphaMult);
    }

    private void renderDiceSelectionHighlights(float alphaMult) {
        if (battleState == null) return;
        if (battleState.getCurrentPhase() != Phase.SELECTING_ATTACK &&
            battleState.getCurrentPhase() != Phase.SELECTING_DEFENSE) return;

        List<Boolean> selected = battleState.getPlayerDiceSelected();
        if (selected == null) return;

        GLStateUtil.resetBlendState();

        float[] c = ColorHelper.toGLComponents(ColorHelper.SELECTION_HIGHLIGHT, alphaMult);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        GL11.glLineWidth(3f);

        List<float[]> diceHitboxes = inputHandler.getDiceHitboxes();
        float diceClickPadding = 5f;

        for (int i = 0; i < Math.min(selected.size(), diceHitboxes.size()); i++) {
            if (selected.get(i)) {
                float[] hb = diceHitboxes.get(i);
                float hx = panelX + hb[0];
                float hy = CoordHelper.uiToGlY(panelY, BattleRenderingUtils.PANEL_HEIGHT, hb[1] + hb[3] - diceClickPadding * 2);
                float hw = hb[2] - diceClickPadding * 2;
                float hh = hb[3] - diceClickPadding * 2;

                GL11.glBegin(GL11.GL_LINE_LOOP);
                GL11.glVertex2f(hx, hy);
                GL11.glVertex2f(hx + hw, hy);
                GL11.glVertex2f(hx + hw, hy + hh);
                GL11.glVertex2f(hx, hy + hh);
                GL11.glEnd();
            }
        }

        GLStateUtil.resetColor();
    }

    private void renderOpponentDiceZone(float alphaMult) {
        float zoneX = panelX + BattleRenderingUtils.MARGIN + BattleRenderingUtils.OPPONENT_DICE_ZONE_OFFSET_X;
        float zoneY = panelY + BattleRenderingUtils.MARGIN + BattleRenderingUtils.OPPONENT_DICE_ZONE_Y_OFFSET;

        BattleRenderingUtils.renderOpponentDiceZone(zoneX, zoneY, alphaMult);

        diceRollManager.renderOpponentDice(panelX, panelY, alphaMult);

        renderOpponentSelectionHighlights(alphaMult);
    }

    private void renderOpponentSelectionHighlights(float alphaMult) {
        if (battleState == null) return;

        AISelectionVisualizer viz = battleState.getAiSelectionVisualizer();
        if (viz == null || !viz.hasStarted() || viz.isRerollPhase()) return;

        List<Integer> visibleIndices = viz.getVisibleIndices();
        if (visibleIndices.isEmpty()) return;

        List<DiceAnimator> opponentAnimators = diceRollManager.getOpponentAnimators();
        if (opponentAnimators == null || opponentAnimators.isEmpty()) return;

        GLStateUtil.resetBlendState();

        Color highlightColor = viz.isRerollPhase() ?
            ColorHelper.OPPONENT_REROLL_HIGHLIGHT :
            ColorHelper.OPPONENT_SELECTION_HIGHLIGHT;

        float[] c = ColorHelper.toGLComponents(highlightColor, alphaMult);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        GL11.glLineWidth(3f);

        for (int idx : visibleIndices) {
            if (idx >= 0 && idx < opponentAnimators.size()) {
                DiceAnimator animator = opponentAnimators.get(idx);
                if (animator != null) {
                    float diceSize = animator.getDisplaySize();
                    float diceX = panelX + animator.getVisualX();
                    float diceY = CoordHelper.uiToGlY(panelY, BattleRenderingUtils.PANEL_HEIGHT,
                        animator.getVisualY() + diceSize);

                    GL11.glBegin(GL11.GL_LINE_LOOP);
                    GL11.glVertex2f(diceX, diceY);
                    GL11.glVertex2f(diceX + diceSize, diceY);
                    GL11.glVertex2f(diceX + diceSize, diceY + diceSize);
                    GL11.glVertex2f(diceX, diceY + diceSize);
                    GL11.glEnd();
                }
            }
        }

        GLStateUtil.resetColor();
    }

    private void renderOpponentPrismaticButton(float alphaMult) {
        SpriteAPI sprite = CosmiconSprites.getPrismaticButtonSprite();
        if (sprite == null) return;

        GLStateUtil.enableTexturingWithBlend();

        float renderX = panelX + opponentPrismaticBtnX;
        float renderY = CoordHelper.uiTopLeftToGlSpriteY(panelY, BattleRenderingUtils.PANEL_HEIGHT,
            opponentPrismaticBtnY, PRISMATIC_BTN_SIZE);

        int uses = battleState.getOpponentPrismaticUses();
        float btnAlpha = uses > 0 ? alphaMult : alphaMult * 0.4f;

        sprite.setSize(PRISMATIC_BTN_SIZE, PRISMATIC_BTN_SIZE);
        sprite.setAlphaMult(btnAlpha);
        sprite.render(renderX, renderY);

        GLStateUtil.disableTexturing();
    }

    private void renderPrismaticButton(float alphaMult) {
        SpriteAPI sprite = CosmiconSprites.getPrismaticButtonSprite();
        if (sprite == null) return;

        GLStateUtil.enableTexturingWithBlend();

        float renderX = panelX + buttons.getPlayerPrismaticBtnX();
        float renderY = CoordHelper.uiTopLeftToGlSpriteY(panelY, BattleRenderingUtils.PANEL_HEIGHT,
            buttons.getPlayerPrismaticBtnY(), PRISMATIC_BTN_SIZE);

        int uses = battleState.getPlayerPrismaticUses();
        boolean playerShouldSelect = (battleState.isAttacker(true) &&
            battleState.getCurrentPhase() == Phase.SELECTING_ATTACK) ||
            (battleState.isDefender(true) &&
            battleState.getCurrentPhase() == Phase.SELECTING_DEFENSE);
        boolean prismaticEnabled = uses > 0 && playerShouldSelect && !buttons.isPrismaticPopupActive();

        float btnAlpha = alphaMult;
        if (!prismaticEnabled) {
            btnAlpha *= 0.4f;
        }

        sprite.setSize(PRISMATIC_BTN_SIZE, PRISMATIC_BTN_SIZE);
        sprite.setAlphaMult(btnAlpha);
        sprite.render(renderX, renderY);

        GLStateUtil.disableTexturing();
    }
}