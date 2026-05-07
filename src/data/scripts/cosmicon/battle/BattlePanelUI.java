package data.scripts.cosmicon.battle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate.DialogCallbacks;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import data.scripts.CosmiconConfig;
import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleEventBus.BattleEventListener;
import data.scripts.cosmicon.battle.TurnState.Phase;
import data.scripts.cosmicon.state.CosmiconStats;
import data.scripts.cosmicon.tutorial.TutorialController;
import data.scripts.cosmicon.tutorial.TutorialUIRenderer;
import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.UnifiedCoord;
import data.scripts.cosmicon.util.CosmiconLogger;
import data.scripts.cosmicon.util.GLStateUtil;

public class BattlePanelUI extends BaseCustomUIPanelPlugin implements BattleEventListener {

    private static final float PRISMATIC_BTN_SIZE = 40f;
    private static final float OPPONENT_AUTO_ROLL_DELAY = 0.8f;
    private static final float ROLE_TRANSITION_DURATION = 0.4f;
    private static final float PRE_CLASH_DELAY = 1.5f;
    private static final float SECONDARY_DAMAGE_FLIGHT_DURATION = 0.6f;
    private static final float SECONDARY_DAMAGE_LIFETIME = 1.5f;
    private static final float HP_ANIM_SPEED = 10f;

    private CustomPanelAPI panel;
    private BattleController battleController;
    private BattleState battleState;
    private DiceRollManager diceRollManager;

    private LabelAPI tutorialLabel;
    private TutorialUIRenderer tutorialRenderer;
    private TutorialController tutorialController;

    private BattleUILabels labels;
    private BattleUIButtons buttons;
    private BattleInputHandler inputHandler;

    private float panelX;
    private float panelY;
    private float diceZoneCenterX;
    private float diceZoneCenterY;
    private float opponentDiceZoneCenterX;
    private float opponentDiceZoneCenterY;
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
    // Refreshed each frame in updateRoleTransition(); do not cache across frames
    private float cachedRotationAngle;

    private float opponentPrismaticBtnX;
    private float opponentPrismaticBtnY;

    private DamageResolutionAnimator damageAnimator;
    private DamageResolver.DamageResult pendingDamageResult;
    private boolean damageAnimationPending = false;
    private boolean damageImpactHandled = false;

    private boolean valueAnimationPending;

    private boolean rerollSelectionClearPending;

    private float rerollAnimSkipGuard;

    private float preClashTimer;
    private float diceDisplayTimer;
    private static final float DICE_DISPLAY_DURATION = 0.8f;

    private static class SecondaryDamageEntry {
        final FlyingNumber number;
        final boolean isPlayer;
        final ImpactEffect impactEffect;
        float timeAlive;
        SecondaryDamageEntry(FlyingNumber number, boolean isPlayer, ImpactEffect impactEffect) {
            this.number = number;
            this.isPlayer = isPlayer;
            this.impactEffect = impactEffect;
            this.timeAlive = 0f;
        }
    }
    private final List<SecondaryDamageEntry> secondaryDamageNumbers = new ArrayList<>();

    private float displayedPlayerHp;
    private float displayedOpponentHp;

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
        this.rerollSelectionClearPending = false;
        this.rerollAnimSkipGuard = 0f;
        this.preClashTimer = 0f;
        this.diceDisplayTimer = 0f;
        this.displayedPlayerHp = -1f;
        this.displayedOpponentHp = -1f;
    }

    public void setBattleController(BattleController controller) {
        this.battleController = controller;
        if (controller != null) {
            this.battleState = controller.getState();
            battleState.addListener(this);
            if (labels != null) labels.setBattleState(battleState);
            if (buttons != null) buttons.setBattleState(battleState);
            if (inputHandler != null) inputHandler.setBattleState(battleState);
            if (inputHandler != null) inputHandler.setBattlePanelUI(this);
            lastPlayerWasAttacker = battleState.isPlayerAttacker();
            roleTransitionProgress = lastPlayerWasAttacker ? 0f : 1f;
            targetRoleTransition = roleTransitionProgress;

            displayedPlayerHp = battleState.getPlayerHp();
            displayedOpponentHp = battleState.getOpponentHp();

            TutorialController tc = controller.getTutorialController();
            if (tc != null) {
                this.tutorialController = tc;
                if (inputHandler != null) inputHandler.setTutorialController(tc);
                if (buttons != null) buttons.setTutorialController(tc);
                if (labels != null) labels.setTutorialController(tc);
            }

            controller.setOpponentAnimationCompleteChecker(
                () -> diceRollManager != null && diceRollManager.isOpponentComplete());
        }
    }

    public void wireTutorial(TutorialController tc) {
        if (tc == null) return;
        this.tutorialController = tc;
        inputHandler.setTutorialController(tc);
        buttons.setTutorialController(tc);
        labels.setTutorialController(tc);
        if (TutorialController.shouldActivateTutorial()) {
            tutorialRenderer = new TutorialUIRenderer();
            tutorialRenderer.init(tc, panel);
        }
        tc.onPhaseChange(battleState.getCurrentPhase());
    }

    public void updateLabelsFromState() {
        labels.updateLabelsFromState();
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
        if (tutorialRenderer != null) {
            tutorialRenderer.cleanup();
            tutorialRenderer = null;
        }
        if (damageAnimator != null) {
            damageAnimator.cleanup();
            damageAnimator = null;
        }
        for (SecondaryDamageEntry entry : secondaryDamageNumbers) {
            entry.number.cleanup();
        }
        secondaryDamageNumbers.clear();
        diceRollManager = null;
        damageAnimationPending = false;
        damageImpactHandled = false;
        valueAnimationPending = false;
        pendingDamageResult = null;
        battleState = null;
        battleController = null;
        opponentAutoRollDelay = 0f;
        preClashTimer = 0f;
        tutorialLabel = null;
        displayedPlayerHp = -1f;
        displayedOpponentHp = -1f;
    }

    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        CosmiconLogger.info("Battle dialog opened");
        this.panel = panel;
        callbacks.getPanelFader().setDurationOut(0.5f);

        this.diceRollManager = new DiceRollManager();
        diceRollManager.init();
        diceRollManager.setBattleState(battleState);

        if (battleController != null) {
            battleController.setDiceRollManager(diceRollManager);
        }

        PositionAPI pos = panel.getPosition();
        panelX = pos.getX();
        panelY = pos.getY();
        diceZoneCenterX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        diceZoneCenterY = BattleRenderingUtils.PANEL_HEIGHT / 2f + 40f;
        opponentDiceZoneCenterX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        opponentDiceZoneCenterY = BattleRenderingUtils.PANEL_HEIGHT / 2f - 40f;

        labels = new BattleUILabels();
        buttons = new BattleUIButtons();

        opponentPrismaticBtnX = BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.MARGIN - PRISMATIC_BTN_SIZE - 10f;
        opponentPrismaticBtnY = BattleRenderingUtils.MARGIN + 22f;
        labels.init(panel, battleState, diceRollManager,
            opponentPrismaticBtnX, opponentPrismaticBtnY,
            BattleRenderingUtils.MARGIN + 60f, BattleRenderingUtils.PANEL_HEIGHT - 100f);
        buttons.init(panel, callbacks, battleController, battleState, diceRollManager, labels,
            inputHandler, diceZoneCenterX, diceZoneCenterY);
        inputHandler.init(battleController, battleState, diceRollManager, labels, buttons,
            diceZoneCenterX, diceZoneCenterY);

        if (CosmiconStats.isInTutorialMode()) {
            tutorialLabel = Global.getSettings().createLabel(
                Strings.format("tutorial.games_remaining", CosmiconStats.getRemainingTutorialGames()),
                Fonts.INSIGNIA_LARGE);
            tutorialLabel.setAlignment(Alignment.MID);
            tutorialLabel.setColor(new Color(255, 200, 80, 255));
            float labelW = 400f;
            float labelX = (BattleRenderingUtils.PANEL_WIDTH - labelW) / 2f;
            panel.addComponent((UIComponentAPI) tutorialLabel).setSize(labelW, 24f).inTL(labelX, 10f);
        }

        if (battleState != null) {
            labels.updateLabelsFromState();
            lastPlayerWasAttacker = battleState.isPlayerAttacker();
            roleTransitionProgress = lastPlayerWasAttacker ? 0f : 1f;
            targetRoleTransition = roleTransitionProgress;
            displayedPlayerHp = battleState.getPlayerHp();
            displayedOpponentHp = battleState.getOpponentHp();
        }
    }

    public void startDamageResolutionAnimation(BattleState state, DamageResolver.DamageResult result) {
        pendingDamageResult = result;
        damageAnimationPending = true;
        damageImpactHandled = false;
        inputHandler.setDamageAnimator(damageAnimator);

        float playerCardX = BattleRenderingUtils.getPlayerCardX();
        float playerCardY = BattleRenderingUtils.getPlayerCardY();
        float playerCardCenterX = playerCardX + BattleRenderingUtils.CARD_WIDTH / 2f;
        float playerCardCenterY = playerCardY + BattleRenderingUtils.CARD_HEIGHT / 2f;

        float opponentCardX = BattleRenderingUtils.getOpponentCardX();
        float opponentCardY = BattleRenderingUtils.getOpponentCardY();
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
        if (tutorialController != null) {
            tutorialController.onPhaseChange(newPhase);
        }

        if (newPhase == Phase.ROLLING) {
            rollAnimationDelay = 0.3f;
            diceAnimating = true;
            dicePreviewActive = false;
            dicePreviewDelay = 0f;
            preClashTimer = 0f;
            diceDisplayTimer = 0f;
            valueAnimationPending = false;
            if (damageAnimator == null || damageAnimator.isComplete()) {
                damageAnimationPending = false;
                pendingDamageResult = null;
            }

            for (SecondaryDamageEntry entry : secondaryDamageNumbers) {
                entry.number.cleanup();
            }
            secondaryDamageNumbers.clear();

            inputHandler.setWaitingForClickToRoll(false);

            labels.clearPrismaticRolledLabel();
            labels.updatePrismaticButton();

            if (!battleState.isDefenderRolling()) {
                opponentDiceAnimating = false;
                opponentRollDelay = 0f;
                opponentAutoRollDelay = 0f;
            }

            diceRollManager.clear();
            diceRollManager.clearOpponentAnimators();

            if (!battleState.isDefenderRolling()) {
                boolean attackerIsPlayer = battleState.isPlayerAttacker();
                if (diceRollManager.hasRestAnimators(attackerIsPlayer)) {
                    List<DiceType> types = attackerIsPlayer ? battleState.getPlayerDiceTypes() : battleState.getOpponentDiceTypes();
                    List<Integer> values = attackerIsPlayer ? battleState.getPlayerDiceValues() : battleState.getOpponentDiceValues();
                    float centerX = attackerIsPlayer ? diceZoneCenterX : opponentDiceZoneCenterX;
                    float centerY = attackerIsPlayer ? diceZoneCenterY : opponentDiceZoneCenterY;
                    if (types != null && values != null) {
                        diceRollManager.startRollFromRest(attackerIsPlayer, types, values, centerX, centerY);
                        inputHandler.consumeClick();
                    }
                }
            } else {
                boolean defenderIsPlayer = !battleState.isPlayerAttacker();
                if (diceRollManager.hasRestAnimators(defenderIsPlayer)) {
                    List<DiceType> types = defenderIsPlayer ? battleState.getPlayerDiceTypes() : battleState.getOpponentDiceTypes();
                    List<Integer> values = defenderIsPlayer ? battleState.getPlayerDiceValues() : battleState.getOpponentDiceValues();
                    float centerX = defenderIsPlayer ? diceZoneCenterX : opponentDiceZoneCenterX;
                    float centerY = defenderIsPlayer ? diceZoneCenterY : opponentDiceZoneCenterY;
                    if (types != null && values != null) {
                        diceRollManager.startRollFromRest(defenderIsPlayer, types, values, centerX, centerY);
                        inputHandler.consumeClick();
                    }
                }
            }

            if (damageAnimator != null) {
                damageAnimator.cleanup();
                damageAnimator = null;
                inputHandler.setDamageAnimator(null);
            }

            ValueChangeAnimator attackerAnimator = labels.getAttackerValueAnimator();
            ValueChangeAnimator defenderAnimator = labels.getDefenderValueAnimator();
            attackerAnimator.reset();
            defenderAnimator.reset();

            labels.hideClickHint();
        }

        if (newPhase == Phase.DICE_DISPLAY_ATTACK) {
            diceDisplayTimer = 0f;
            boolean attackerIsPlayer = battleState.isPlayerAttacker();
            float gridCenterX = attackerIsPlayer ? BattleRenderingUtils.PLAYER_REST_GRID_CENTER_X : BattleRenderingUtils.OPPONENT_REST_GRID_CENTER_X;
            float gridCenterY = attackerIsPlayer ? BattleRenderingUtils.PLAYER_REST_GRID_CENTER_Y : BattleRenderingUtils.OPPONENT_REST_GRID_CENTER_Y;
            diceRollManager.moveSelectedToRestGrid(attackerIsPlayer, gridCenterX, gridCenterY);
            labels.updateLabelsFromState();
        }

        if (newPhase == Phase.DICE_DISPLAY_DEFENSE) {
            diceDisplayTimer = 0f;
            boolean defenderIsPlayer = !battleState.isPlayerAttacker();
            float gridCenterX = defenderIsPlayer ? BattleRenderingUtils.PLAYER_REST_GRID_CENTER_X : BattleRenderingUtils.OPPONENT_REST_GRID_CENTER_X;
            float gridCenterY = defenderIsPlayer ? BattleRenderingUtils.PLAYER_REST_GRID_CENTER_Y : BattleRenderingUtils.OPPONENT_REST_GRID_CENTER_Y;
            diceRollManager.moveSelectedToRestGrid(defenderIsPlayer, gridCenterX, gridCenterY);
            labels.updateLabelsFromState();
        }

        if (newPhase == Phase.SELECTING_DEFENSE) {
            labels.updateLabelsFromState();
        }

        if (newPhase == Phase.RESOLVING_PRE_CLASH) {
            preClashTimer = 0f;
            
            boolean hasAttackerChanges = !battleState.getPendingValueChanges(battleState.isPlayerAttacker()).isEmpty();
            boolean hasDefenderChanges = !battleState.getPendingValueChanges(!battleState.isPlayerAttacker()).isEmpty();

            if (hasAttackerChanges || hasDefenderChanges) {
                startValueChangeAnimations();
            }
        }

        if (newPhase == Phase.RESOLVING_MODIFICATION) {
            triggerModificationGlowAnimations();
        }

        if (newPhase == Phase.RESOLVING) {
            labels.getStatusEffectAnimator().stopLoopingGlowAnimations();
            labels.hideClickHint();
        }

        if (newPhase == Phase.WAITING_NEXT_TURN || newPhase == Phase.ENDED) {
            labels.getStatusEffectAnimator().stopLoopingGlowAnimations();
            labels.hideClickHint();
            inputHandler.createDiceHitboxes(new ArrayList<>());
            if (newPhase == Phase.ENDED) {
                diceRollManager.clearAllRestAnimators();
            }
        }

        labels.updatePhaseLabel();
        buttons.updateButtons(newPhase);
    }

    @Override
    public void onDiceRerolled(boolean isPlayer, List<Integer> newValues, List<Integer> rerolledIndices) {
        if (rerolledIndices.isEmpty()) return;

        float animDuration = DiceAnimator.getTotalDuration() + 0.1f;

        if (isPlayer) {
            diceRollManager.partialReroll(rerolledIndices, newValues);
            rollAnimationDelay = animDuration;
            diceAnimating = true;
            rerollSelectionClearPending = true;
            rerollAnimSkipGuard = 0.15f;
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
        if (tutorialController != null) {
            tutorialController.onBattleEnd(winner);
        }
        labels.updatePhaseLabel();
    }

    @Override
    public void onDiceRolled(boolean isPlayer, List<DiceType> types, List<Integer> values) {
        labels.updateDiceRolledCounts(isPlayer, types);
        
        if (!isPlayer && types != null && battleState != null 
                && battleState.getCurrentPhase() == TurnState.Phase.ROLLING) {
            triggerOpponentDiceRoll();
        }
    }
    
    @Override
    public void onTransitionToDefenderRoll() {
        diceRollManager.clear();
        diceRollManager.clearOpponentAnimators();
        
        diceAnimating = true;
        rollAnimationDelay = 0f;
        dicePreviewActive = false;
        dicePreviewDelay = 0f;
        opponentDiceAnimating = false;
        opponentRollDelay = 0f;
        opponentAutoRollDelay = 0f;
        diceDisplayTimer = 0f;
        
        inputHandler.setWaitingForClickToRoll(false);
        inputHandler.createDiceHitboxes(new ArrayList<>());
        
        boolean defenderIsPlayer = !battleState.isPlayerAttacker();
        if (!defenderIsPlayer) {
            opponentAutoRollDelay = OPPONENT_AUTO_ROLL_DELAY;
        }
    }

    @Override
    public void onWeatherChange(WeatherType newWeather) {
        if (buttons != null) {
            buttons.updateWeatherLabel();
        }
        if (labels != null) {
            labels.updatePrismaticButton();
        }
    }

    @Override
    public void onDamageAnimationStart(DamageResolver.DamageResult result) {
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

        List<BattleEventBus.ValueChangeRecord> attackerChanges = battleState.getPendingValueChanges(playerIsAttacker);
        List<BattleEventBus.ValueChangeRecord> defenderChanges = battleState.getPendingValueChanges(!playerIsAttacker);

        int attackerDelta = 0;
        for (BattleEventBus.ValueChangeRecord record : attackerChanges) attackerDelta += record.delta();
        int defenderDelta = 0;
        for (BattleEventBus.ValueChangeRecord record : defenderChanges) defenderDelta += record.delta();

        int attackValue = labels.getAttackerTotalDisplayValue() - attackerDelta;
        int defenseValue = labels.getDefenderTotalDisplayValue() - defenderDelta;

        ValueChangeAnimator attackerAnimator = labels.getAttackerValueAnimator();
        ValueChangeAnimator defenderAnimator = labels.getDefenderValueAnimator();

        if (!attackerChanges.isEmpty() && attackerAnimator != null) {
            attackerAnimator.start(attackerCenterX, attackerCenterY, attackValue, panel, true);
        }
        if (!defenderChanges.isEmpty() && defenderAnimator != null) {
            defenderAnimator.start(defenderCenterX, defenderCenterY, defenseValue, panel, false);
        }

        battleState.clearPendingValueChanges();
        battleState.setValueChangeAnimationInProgress(true);
    }

    @Override
    public void onDamageAnimationComplete() {
        labels.updateLabelsFromState();
    }

    @Override
    public void onDamageImpacted() {
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
            targetAnimator.queueChange(displayText, color, delta);
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
    public void onSecondaryDamage(boolean isPlayer, int damage, String damageType) {
        if (damage <= 0) return;

        float cardCenterX;
        float cardCenterY;
        if (isPlayer) {
            float cardX = BattleRenderingUtils.getPlayerCardX();
            float cardY = BattleRenderingUtils.getPlayerCardY();
            cardCenterX = cardX + BattleRenderingUtils.CARD_WIDTH / 2f;
            cardCenterY = cardY + BattleRenderingUtils.CARD_HEIGHT / 2f;
        } else {
            cardCenterX = BattleRenderingUtils.MARGIN + BattleRenderingUtils.CARD_WIDTH / 2f;
            cardCenterY = BattleRenderingUtils.MARGIN + BattleRenderingUtils.CARD_HEIGHT / 2f;
        }

        int sameTargetCount = 0;
        for (SecondaryDamageEntry entry : secondaryDamageNumbers) {
            if (entry.isPlayer == isPlayer) sameTargetCount++;
        }
        float yOffset = sameTargetCount * 20f;

        Color color = getSecondaryDamageColor(damageType);
        FlyingNumber fn = new FlyingNumber();
        fn.setValue(damage);
        fn.setColor(color);
        fn.startFrom(cardCenterX, cardCenterY - yOffset);
        fn.flyTo(cardCenterX, cardCenterY - 30f - yOffset, SECONDARY_DAMAGE_FLIGHT_DURATION);

        ImpactEffect impactEffect = new ImpactEffect();
        impactEffect.triggerFlash(cardCenterX, cardCenterY, 30f, color);
        impactEffect.triggerParticles(cardCenterX, cardCenterY, 5, color);
        if ("COUNTER".equals(damageType)) {
            impactEffect.triggerShockwave(cardCenterX, cardCenterY);
            impactEffect.triggerParticles(cardCenterX, cardCenterY, 6, color);
        }

        secondaryDamageNumbers.add(new SecondaryDamageEntry(fn, isPlayer, impactEffect));

        labels.updateLabelsFromState();
    }

    private Color getSecondaryDamageColor(String damageType) {
        return switch (damageType) {
            case "COUNTER" -> ColorHelper.COUNTER_DAMAGE;
            case "INSTANT_DAMAGE" -> ColorHelper.INSTANT_DAMAGE;
            case "POISON" -> ColorHelper.POISON_DAMAGE;
            case "THORNS" -> ColorHelper.THORNS_DAMAGE;
            case "REFLECT" -> ColorHelper.REFLECT_DAMAGE;
            case "OVERLOAD" -> ColorHelper.OVERLOAD_DAMAGE;
            default -> FlyingNumber.DAMAGE_RESULT;
        };
    }

    private void advanceDisplayedHp(float amount) {
        if (displayedPlayerHp < 0f) displayedPlayerHp = battleState.getPlayerHp();
        if (displayedOpponentHp < 0f) displayedOpponentHp = battleState.getOpponentHp();

        float targetPlayerHp = battleState.getPlayerHp();
        float targetOpponentHp = battleState.getOpponentHp();

        float playerDiff = targetPlayerHp - displayedPlayerHp;
        float opponentDiff = targetOpponentHp - displayedOpponentHp;

        if (Math.abs(playerDiff) < 0.5f) {
            displayedPlayerHp = targetPlayerHp;
        } else {
            displayedPlayerHp += playerDiff * HP_ANIM_SPEED * amount;
        }

        if (Math.abs(opponentDiff) < 0.5f) {
            displayedOpponentHp = targetOpponentHp;
        } else {
            displayedOpponentHp += opponentDiff * HP_ANIM_SPEED * amount;
        }
    }

    private void advanceSecondaryDamageNumbers(float amount) {
        List<SecondaryDamageEntry> toRemove = new ArrayList<>();
        for (SecondaryDamageEntry entry : secondaryDamageNumbers) {
            entry.number.advance(amount);
            entry.impactEffect.advance(amount);
            entry.timeAlive += amount;
            if (entry.timeAlive >= SECONDARY_DAMAGE_LIFETIME) {
                toRemove.add(entry);
            }
        }
        for (SecondaryDamageEntry entry : toRemove) {
            entry.number.cleanup();
            secondaryDamageNumbers.remove(entry);
        }
    }

    @Override
    public void advance(float amount) {
        advanceDisplayedHp(amount);
        advanceSecondaryDamageNumbers(amount);

        updateRoleTransition(amount);
        labels.updateSelectionDisplayLabels();
        labels.updateConfirmedSelectionLabels();
        labels.updateIconValueLabels();
        labels.updateStatusEffectLabels();
        buttons.updateStatusTooltipButtons();
        labels.updatePrismaticButton();
        checkProcessedEffects();
        labels.getStatusEffectAnimator().advance(amount);
        updatePrismaticClickHint();

        battleController.advanceAiSelection(amount);

        buttons.advance(amount);

        if (tutorialRenderer != null) {
            tutorialRenderer.advance(amount);
        }

        Phase currentPhase = battleState.getCurrentPhase();
        if (currentPhase == Phase.RESOLVING || currentPhase == Phase.RESOLVING_MODIFICATION || currentPhase == Phase.WAITING_NEXT_TURN || damageAnimationPending) {
            labels.updateLabelsFromState();
        }

        diceRollManager.advance(amount);

        if (currentPhase == Phase.RESOLVING_PRE_CLASH) {
            preClashTimer += amount;

            ValueChangeAnimator attackerAnimator = labels.getAttackerValueAnimator();
            ValueChangeAnimator defenderAnimator = labels.getDefenderValueAnimator();

            attackerAnimator.advance(amount);
            defenderAnimator.advance(amount);

            if (preClashTimer >= PRE_CLASH_DELAY) {
                boolean attackerComplete = attackerAnimator.isComplete();
                boolean defenderComplete = defenderAnimator.isComplete();

                if (attackerComplete && defenderComplete) {
                    preClashTimer = 0f;
                    battleState.setValueChangeAnimationInProgress(false);
                    battleController.proceedToModificationPause();
                }
            }
            return;
        }

        if (currentPhase == Phase.RESOLVING_MODIFICATION) {
            StatusEffectAnimator effectAnimator = labels.getStatusEffectAnimator();
            effectAnimator.advance(amount);

            ValueChangeAnimator attackerAnimator = labels.getAttackerValueAnimator();
            ValueChangeAnimator defenderAnimator = labels.getDefenderValueAnimator();
            attackerAnimator.advance(amount);
            defenderAnimator.advance(amount);

            labels.showClickHint(Strings.get("battle.click_to_continue"), 0.8f);
        }

        if (currentPhase == Phase.DICE_DISPLAY_ATTACK || currentPhase == Phase.DICE_DISPLAY_DEFENSE) {
            diceDisplayTimer += amount;
            boolean forPlayer = (currentPhase == Phase.DICE_DISPLAY_ATTACK) == battleState.isPlayerAttacker();
            boolean travelComplete = diceRollManager.isRestTravelComplete(forPlayer);
            
            if (diceDisplayTimer >= DICE_DISPLAY_DURATION && travelComplete) {
                diceDisplayTimer = 0f;
                if (currentPhase == Phase.DICE_DISPLAY_ATTACK) {
                    battleController.advanceFromDiceDisplayAttack();
                } else {
                    battleController.advanceFromDiceDisplayDefense();
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

                if (playerComplete && opponentComplete) {
                    rollAnimationDelay = 0f;
                    diceAnimating = false;
                    labels.updatePrismaticRolledLabel();
                    List<DiceType> playerTypes = battleState.getPlayerDiceTypes();
                    if (playerTypes != null && !playerTypes.isEmpty() && diceRollManager.hasAnimators()) {
                        inputHandler.createDiceHitboxes(playerTypes);
                    }
                    if (battleState.getCurrentPhase() == Phase.ROLLING) {
                        dicePreviewActive = true;
                        dicePreviewDelay = CosmiconConfig.DICE_PREVIEW_DELAY;
                    }
                }
            } else if (rollAnimationDelay <= 0f) {
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
                        diceRollManager.startOpponentStationaryPreview(types, values, opponentDiceZoneCenterX, opponentDiceZoneCenterY);
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

        if (battleState.getCurrentPhase() == Phase.ROLLING && !inputHandler.isWaitingForClickToRoll()) {
            boolean isOpponentTurn = battleState.isDefenderRolling() == battleState.isPlayerAttacker();
            if (isOpponentTurn && (diceRollManager.hasOpponentAnimators() || diceRollManager.isOpponentWaitingForRollTrigger())) {
                labels.showClickHint(Strings.get("battle.click_to_continue"), 0.6f);
            }
        }

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
                if (diceRollManager.isOpponentComplete()) {
                    opponentRollDelay = 0f;
                    opponentDiceAnimating = false;
                }
            } else if (opponentRollDelay <= 0f) {
                startOpponentDiceAnimation();
            }
        }

        if (rerollSelectionClearPending) {
            rerollSelectionClearPending = false;
            battleState.clearDiceSelection(true);
        }

        if (currentPhase == Phase.SELECTING_ATTACK || currentPhase == Phase.SELECTING_DEFENSE) {
            if (diceAnimating && diceRollManager.hasAnimators() && !diceRollManager.isComplete()
                    && rerollAnimSkipGuard <= 0f) {
                labels.showClickHint(Strings.get("battle.click_to_continue"), 0.6f);
            } else if (opponentDiceAnimating && diceRollManager.hasOpponentAnimators() && !diceRollManager.isOpponentComplete()) {
                labels.showClickHint(Strings.get("battle.click_to_continue"), 0.6f);
            }
        }

        ValueChangeAnimator attackerAnimator = labels.getAttackerValueAnimator();
        ValueChangeAnimator defenderAnimator = labels.getDefenderValueAnimator();

        attackerAnimator.advance(amount);
        defenderAnimator.advance(amount);

        if (valueAnimationPending) {
            boolean attackerComplete = attackerAnimator.isComplete();
            boolean defenderComplete = defenderAnimator.isComplete();

            if (attackerComplete && defenderComplete) {
                valueAnimationPending = false;
                battleState.setValueChangeAnimationInProgress(false);
                startDamageResolutionAnimation(battleState, pendingDamageResult);
            }
        }

        if (damageAnimator != null) {
            damageAnimator.advance(amount);

            if (damageAnimator.hasDamageImpacted() && damageAnimationPending && !damageImpactHandled) {
                damageImpactHandled = true;
                battleState.notifyDamageImpacted();
                battleController.onDamageImpacted();
            }

            if (!damageAnimator.isComplete()) {
                labels.showClickHint(Strings.get("battle.click_to_continue"), 0.6f);
            } else {
                labels.hideClickHint();
            }

            if (damageAnimator.isComplete() && damageAnimationPending) {
                damageAnimationPending = false;
                damageImpactHandled = false;
                damageAnimator.cleanup();
                damageAnimator = null;
                inputHandler.setDamageAnimator(null);
                labels.hideClickHint();

                battleState.notifyDamageAnimationComplete();
            }
        } else {
            labels.hideClickHint();
        }

        PositionAPI pos = panel.getPosition();
        panelX = pos.getX();
        panelY = pos.getY();
        inputHandler.setPanelPosition(panelX, panelY);

        if (rerollAnimSkipGuard > 0f) rerollAnimSkipGuard -= amount;

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

    private void updatePrismaticClickHint() {
        if (battleState == null) return;
        if (TutorialController.shouldActivateTutorial() && (tutorialController == null || !tutorialController.isPrismaticAllowed())) return;

        int uses = battleState.getPlayerPrismaticUses();
        boolean playerShouldSelect = (battleState.isAttacker(true) &&
            battleState.getCurrentPhase() == Phase.SELECTING_ATTACK) ||
            (battleState.isDefender(true) &&
            battleState.getCurrentPhase() == Phase.SELECTING_DEFENSE);
        boolean prismaticEnabled = uses > 0 && playerShouldSelect && !buttons.isPrismaticPopupActive();

        labels.updatePrismaticClickHint(prismaticEnabled);
    }

    private boolean shouldShowOpponentDice() {
        TurnState.Phase phase = battleState.getCurrentPhase();
        AISelectionVisualizer viz = battleState.getAiSelectionVisualizer();

        boolean aiIsSelecting = (phase == TurnState.Phase.SELECTING_ATTACK && !battleState.isPlayerAttacker()) ||
                                (phase == TurnState.Phase.SELECTING_DEFENSE && battleState.isPlayerAttacker());

        boolean vizActive = viz != null && viz.hasStarted();

        boolean isDefenderRolling = battleState.isDefenderRolling();
        boolean isOpponentTurn = isDefenderRolling == battleState.isPlayerAttacker();

        boolean opponentRolling = phase == TurnState.Phase.ROLLING &&
                                  diceRollManager != null &&
                                  isOpponentTurn &&
                                  (diceRollManager.hasOpponentAnimators() || diceRollManager.isOpponentWaitingForRollTrigger());

        return aiIsSelecting || vizActive || opponentRolling;
    }

    private void startOpponentDiceAnimation() {
        List<DiceType> types = battleState.getOpponentDiceTypes();
        List<Integer> values = battleState.getOpponentDiceValues();

        if (types == null || values == null || types.isEmpty()) {
            opponentDiceAnimating = false;
            return;
        }

        diceRollManager.startOpponentStationaryPreview(types, values, opponentDiceZoneCenterX, opponentDiceZoneCenterY);
        opponentAutoRollDelay = OPPONENT_AUTO_ROLL_DELAY;
    }

    public void triggerOpponentDiceRoll() {
        if (battleState == null || diceRollManager == null) return;
        
        if (diceRollManager.hasOpponentAnimators() || 
            diceRollManager.isOpponentWaitingForRollTrigger()) {
            return;
        }

        List<DiceType> types = battleState.getOpponentDiceTypes();
        List<Integer> values = battleState.getOpponentDiceValues();

        if (types != null && values != null && !types.isEmpty()) {
            diceRollManager.startOpponentStationaryPreview(types, values, opponentDiceZoneCenterX, opponentDiceZoneCenterY);
            opponentAutoRollDelay = OPPONENT_AUTO_ROLL_DELAY;
        }
    }

    public boolean canSkipRerollAnim() {
        return rerollAnimSkipGuard <= 0f;
    }

    @Override
    public void renderBelow(float alphaMult) {
        PositionAPI pos = panel.getPosition();
        float x = pos.getX();
        float y = pos.getY();
        float w = pos.getWidth();
        float h = pos.getHeight();

        UnifiedCoord.setCurrent(new UnifiedCoord.PanelContext(x, y, w, h));

        try {
            GLStateUtil.resetBlendState();

            boolean hideRoleIcons = damageAnimator != null && damageAnimator.isIconClashActive();

            BattleRenderingUtils.renderBattleBackground(x, y, w, h,
                cachedRotationAngle, alphaMult, true, hideRoleIcons);

            renderCard(true, alphaMult);
            renderPlayerHpCircle(alphaMult);
            renderCard(false, alphaMult);
            renderOpponentHpCircle(alphaMult);
            renderStatusEffectBoxes(alphaMult);
            renderWeatherDescBox(alphaMult);

            if (labels.getStatusEffectAnimator().hasActiveAnimations()) {
                labels.getStatusEffectAnimator().render(x, y, w, h, alphaMult);
            }

            ValueChangeAnimator attackerAnimator = labels.getAttackerValueAnimator();
            ValueChangeAnimator defenderAnimator = labels.getDefenderValueAnimator();

            if (!attackerAnimator.isComplete()) {
                attackerAnimator.render(x, y, h, alphaMult);
            }
            if (!defenderAnimator.isComplete()) {
                defenderAnimator.render(x, y, h, alphaMult);
            }

            diceRollManager.render(x, y, w, h, alphaMult);

            diceRollManager.renderRestingDice(x, y, w, h, alphaMult);

            if (damageAnimator != null && !damageAnimator.isComplete()) {
                damageAnimator.render(x, y, w, h, alphaMult);
            }

            for (SecondaryDamageEntry entry : secondaryDamageNumbers) {
                entry.impactEffect.render(x, y, w, h, alphaMult);
                entry.number.render(x, y, h, alphaMult, panel);
            }

            if (shouldShowOpponentDice()) {
                renderOpponentDiceZone(x, y, w, h, alphaMult);
            }

            renderDiceSelectionHighlights(alphaMult);
            PassiveIndicationRenderer.renderIndications(battleState, diceRollManager, alphaMult);
            renderOpponentPrismaticButton(alphaMult);
            renderPrismaticButton(alphaMult);

            if (buttons.isPrismaticPopupActive()) {
                buttons.getPrismaticPopup().renderBelow(alphaMult);
            }

            if (tutorialRenderer != null) {
                tutorialRenderer.render(alphaMult);
            }

            GLStateUtil.resetColor();
        } finally {
            UnifiedCoord.clearCurrent();
        }
    }

    private void renderCard(boolean isPlayer, float alphaMult) {
        float cardUiX, cardUiY;

        if (isPlayer) {
            cardUiX = BattleRenderingUtils.getPlayerCardX();
            cardUiY = BattleRenderingUtils.getPlayerCardY();
        } else {
            cardUiX = BattleRenderingUtils.MARGIN;
            cardUiY = BattleRenderingUtils.MARGIN;
        }

        UnifiedCoord cardPos = new UnifiedCoord(cardUiX, cardUiY);
        float cardX = cardPos.glX();
        float cardY = cardPos.glSpriteY(BattleRenderingUtils.CARD_HEIGHT);

        CharacterCard card = isPlayer ? battleState.getPlayerCard() : battleState.getOpponentCard();
        int effectiveAtk = battleState.getEffectiveAtkLevel(isPlayer);
        int effectiveDef = battleState.getEffectiveDefLevel(isPlayer);
        BattleRenderingUtils.renderCharacterCard(cardX, cardY, card, effectiveAtk, effectiveDef, alphaMult);
    }

    private void renderPlayerHpCircle(float alphaMult) {
        CharacterCard card = battleState.getPlayerCard();
        float cx = BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN + 5f + 17f;
        float cy = BattleRenderingUtils.PANEL_HEIGHT - BattleRenderingUtils.CARD_HEIGHT - BattleRenderingUtils.MARGIN + 14f + 10f;

        UnifiedCoord center = new UnifiedCoord(cx, cy);
        float fillFraction = displayedPlayerHp >= 0f ? displayedPlayerHp / card.getMaxHp() : (float) battleState.getPlayerHp() / card.getMaxHp();
        BattleRenderingUtils.renderHpCircle(center.glX(), center.glY(), BattleRenderingUtils.HP_CIRCLE_RADIUS, fillFraction, alphaMult);
    }

    private void renderOpponentHpCircle(float alphaMult) {
        CharacterCard card = battleState.getOpponentCard();

        float cx = BattleRenderingUtils.MARGIN + 5f + 17f;
        float cy = BattleRenderingUtils.MARGIN + 14f + 10f;

        UnifiedCoord center = new UnifiedCoord(cx, cy);
        float fillFraction = displayedOpponentHp >= 0f ? displayedOpponentHp / card.getMaxHp() : (float) battleState.getOpponentHp() / card.getMaxHp();
        BattleRenderingUtils.renderHpCircle(center.glX(), center.glY(), BattleRenderingUtils.HP_CIRCLE_RADIUS, fillFraction, alphaMult);
    }

    private void renderStatusEffectBoxes(float alphaMult) {
        float playerCardX = BattleRenderingUtils.getPlayerCardX();
        float playerCardY = BattleRenderingUtils.getPlayerCardY();
        float playerBoxX = playerCardX - BattleRenderingUtils.STATUS_BOX_WIDTH - 20f;
        UnifiedCoord playerBoxPos = new UnifiedCoord(playerBoxX, playerCardY);
        float playerBoxGlX = playerBoxPos.glX();
        float playerBoxGlY = playerBoxPos.glSpriteY(BattleRenderingUtils.CARD_HEIGHT);

        BattleRenderingUtils.renderStatusEffectBox(playerBoxGlX, playerBoxGlY,
            BattleRenderingUtils.STATUS_BOX_WIDTH, BattleRenderingUtils.CARD_HEIGHT, alphaMult);

        float opponentCardX = BattleRenderingUtils.getOpponentCardX();
        float opponentCardY = BattleRenderingUtils.getOpponentCardY();
        float opponentBoxX = opponentCardX + BattleRenderingUtils.CARD_WIDTH + 20f;
        UnifiedCoord opponentBoxPos = new UnifiedCoord(opponentBoxX, opponentCardY);
        float opponentBoxGlX = opponentBoxPos.glX();
        float opponentBoxGlY = opponentBoxPos.glSpriteY(BattleRenderingUtils.CARD_HEIGHT);

        BattleRenderingUtils.renderStatusEffectBox(opponentBoxGlX, opponentBoxGlY,
            BattleRenderingUtils.STATUS_BOX_WIDTH, BattleRenderingUtils.CARD_HEIGHT, alphaMult);
    }

    private void renderWeatherDescBox(float alphaMult) {
        float boxW = buttons.getWeatherDescBoxW();
        float boxH = buttons.getWeatherDescBoxH();
        if (boxW <= 0f || boxH <= 0f) return;

        UnifiedCoord boxPos = new UnifiedCoord(buttons.getWeatherDescBoxX(), buttons.getWeatherDescBoxY());
        BattleRenderingUtils.renderWeatherDescBox(boxPos.glX(), boxPos.glSpriteY(boxH), boxW, boxH, alphaMult);
    }

    private void checkProcessedEffects() {
        StatusEffectAnimator animator = labels.getStatusEffectAnimator();

        List<StatusEffectProcessor.ProcessedEffect> playerProcessed =
            battleState.getPlayerEffects().getAndClearProcessedEffects();
        for (StatusEffectProcessor.ProcessedEffect pe : playerProcessed) {
            Integer idx = labels.getEffectDisplayIndex(true, pe.effect());
            if (idx == null) {
                idx = computeDisplayIndex(true, pe.effect());
            }
            if (idx != null) {
                float[] pos = labels.getStatusEffectLabelPosition(true, idx);
                animator.triggerProcessAnimation(pos[0], pos[1], pos[2], pos[3]);
            }
        }

        List<StatusEffectProcessor.ProcessedEffect> opponentProcessed =
            battleState.getOpponentEffects().getAndClearProcessedEffects();
        for (StatusEffectProcessor.ProcessedEffect pe : opponentProcessed) {
            Integer idx = labels.getEffectDisplayIndex(false, pe.effect());
            if (idx == null) {
                idx = computeDisplayIndex(false, pe.effect());
            }
            if (idx != null) {
                float[] pos = labels.getStatusEffectLabelPosition(false, idx);
                animator.triggerProcessAnimation(pos[0], pos[1], pos[2], pos[3]);
            }
        }
    }

    private void triggerModificationGlowAnimations() {
        if (battleState == null || labels == null || labels.getStatusEffectAnimator() == null) return;

        StatusEffectAnimator animator = labels.getStatusEffectAnimator();

        if (battleState.getPlayerEffects().hasEffect(StatusEffectProcessor.StatusEffect.HACK)) {
            Integer idx = labels.getEffectDisplayIndex(true, StatusEffectProcessor.StatusEffect.HACK);
            if (idx == null) idx = computeDisplayIndex(true, StatusEffectProcessor.StatusEffect.HACK);
            if (idx != null) {
                float[] pos = labels.getStatusEffectLabelPosition(true, idx);
                animator.triggerLoopingGlowAnimation(pos[0], pos[1], pos[2], pos[3]);
            }
        }
        if (battleState.getPlayerEffects().hasEffect(StatusEffectProcessor.StatusEffect.ARISE)) {
            Integer idx = labels.getEffectDisplayIndex(true, StatusEffectProcessor.StatusEffect.ARISE);
            if (idx == null) idx = computeDisplayIndex(true, StatusEffectProcessor.StatusEffect.ARISE);
            if (idx != null) {
                float[] pos = labels.getStatusEffectLabelPosition(true, idx);
                animator.triggerLoopingGlowAnimation(pos[0], pos[1], pos[2], pos[3]);
            }
        }
        if (battleState.getOpponentEffects().hasEffect(StatusEffectProcessor.StatusEffect.HACK)) {
            Integer idx = labels.getEffectDisplayIndex(false, StatusEffectProcessor.StatusEffect.HACK);
            if (idx == null) idx = computeDisplayIndex(false, StatusEffectProcessor.StatusEffect.HACK);
            if (idx != null) {
                float[] pos = labels.getStatusEffectLabelPosition(false, idx);
                animator.triggerLoopingGlowAnimation(pos[0], pos[1], pos[2], pos[3]);
            }
        }
        if (battleState.getOpponentEffects().hasEffect(StatusEffectProcessor.StatusEffect.ARISE)) {
            Integer idx = labels.getEffectDisplayIndex(false, StatusEffectProcessor.StatusEffect.ARISE);
            if (idx == null) idx = computeDisplayIndex(false, StatusEffectProcessor.StatusEffect.ARISE);
            if (idx != null) {
                float[] pos = labels.getStatusEffectLabelPosition(false, idx);
                animator.triggerLoopingGlowAnimation(pos[0], pos[1], pos[2], pos[3]);
            }
        }
    }

    private Integer computeDisplayIndex(boolean isPlayer, StatusEffectProcessor.StatusEffect targetEffect) {
        StatusEffectProcessor effects = isPlayer ? battleState.getPlayerEffects() : battleState.getOpponentEffects();
        int idx = 0;
        for (StatusEffectProcessor.StatusEffect effect : StatusEffectProcessor.StatusEffect.values()) {
            if (effect == targetEffect) return idx;
            if (effects.getLayers(effect) > 0) idx++;
        }
        return null;
    }

    private void renderDiceSelectionHighlights(float alphaMult) {
        if (battleState.getCurrentPhase() != Phase.SELECTING_ATTACK &&
            battleState.getCurrentPhase() != Phase.SELECTING_DEFENSE) return;

        List<Boolean> selected = battleState.getPlayerDiceSelected();

        List<DiceAnimator> animators = diceRollManager.getAnimators();

        GLStateUtil.resetBlendState();

        float[] c = ColorHelper.toGLComponents(ColorHelper.SELECTION_HIGHLIGHT, alphaMult);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        GL11.glLineWidth(3f);

        for (int i = 0; i < Math.min(selected.size(), animators.size()); i++) {
            if (selected.get(i)) {
                DiceAnimator animator = animators.get(i);
                if (animator != null) {
                    float visX = animator.getVisualX();
                    float visY = animator.getVisualY();
                    float displaySize = animator.getDisplaySize();
                    float centeringOffset = (AnimationConstants.DICE_SIZE - displaySize) / 2f;
                    float boxX = visX - centeringOffset;
                    float boxY = visY - centeringOffset;
                    UnifiedCoord dicePos = new UnifiedCoord(boxX, boxY).bottomLeft(AnimationConstants.DICE_SIZE);

                    float diceX = dicePos.glX();
                    float diceY = dicePos.glY();

                    GL11.glBegin(GL11.GL_LINE_LOOP);
                    GL11.glVertex2f(diceX, diceY);
                    GL11.glVertex2f(diceX + AnimationConstants.DICE_SIZE, diceY);
                    GL11.glVertex2f(diceX + AnimationConstants.DICE_SIZE, diceY + AnimationConstants.DICE_SIZE);
                    GL11.glVertex2f(diceX, diceY + AnimationConstants.DICE_SIZE);
                    GL11.glEnd();
                }
            }
        }

        GLStateUtil.resetColor();
    }

    private void renderOpponentDiceZone(float panelX, float panelY, float panelWidth, float panelHeight, float alphaMult) {
        diceRollManager.renderOpponentDice(panelX, panelY, panelWidth, panelHeight, alphaMult);

        renderOpponentSelectionHighlights(alphaMult);
    }

    private void renderOpponentSelectionHighlights(float alphaMult) {
        AISelectionVisualizer viz = battleState.getAiSelectionVisualizer();
        if (viz == null || !viz.hasStarted() || viz.isRerollPhase()) return;

        if (diceRollManager != null && !diceRollManager.isOpponentComplete()) return;

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
                    float visX = animator.getVisualX();
                    float visY = animator.getVisualY();
                    float displaySize = animator.getDisplaySize();
                    float centeringOffset = (AnimationConstants.DICE_SIZE - displaySize) / 2f;
                    float boxX = visX - centeringOffset;
                    float boxY = visY - centeringOffset;
                    UnifiedCoord dicePos = new UnifiedCoord(boxX, boxY).bottomLeft(AnimationConstants.DICE_SIZE);

                    float diceX = dicePos.glX();
                    float diceY = dicePos.glY();

                    GL11.glBegin(GL11.GL_LINE_LOOP);
                    GL11.glVertex2f(diceX, diceY);
                    GL11.glVertex2f(diceX + AnimationConstants.DICE_SIZE, diceY);
                    GL11.glVertex2f(diceX + AnimationConstants.DICE_SIZE, diceY + AnimationConstants.DICE_SIZE);
                    GL11.glVertex2f(diceX, diceY + AnimationConstants.DICE_SIZE);
                    GL11.glEnd();
                }
            }
        }

        GLStateUtil.resetColor();
    }

    private void renderOpponentPrismaticButton(float alphaMult) {
        if (TutorialController.shouldActivateTutorial()) return;
        SpriteAPI sprite = CosmiconSprites.getPrismaticButtonSprite();
        if (sprite == null) return;

        GLStateUtil.enableTexturingWithBlend();

        UnifiedCoord btnPos = new UnifiedCoord(opponentPrismaticBtnX, opponentPrismaticBtnY);
        float renderX = btnPos.glX();
        float renderY = btnPos.glSpriteY(PRISMATIC_BTN_SIZE);

        int uses = battleState.getOpponentPrismaticUses();
        float btnAlpha = uses > 0 ? alphaMult : alphaMult * 0.4f;

        sprite.setSize(PRISMATIC_BTN_SIZE, PRISMATIC_BTN_SIZE);
        sprite.setAlphaMult(btnAlpha);
        sprite.render(renderX, renderY);

        GLStateUtil.disableTexturing();
    }

    private void renderPrismaticButton(float alphaMult) {
        if (TutorialController.shouldActivateTutorial() && (tutorialController == null || !tutorialController.isPrismaticAllowed())) return;
        SpriteAPI sprite = CosmiconSprites.getPrismaticButtonSprite();
        if (sprite == null) return;

        GLStateUtil.enableTexturingWithBlend();

        UnifiedCoord btnPos = new UnifiedCoord(buttons.getPlayerPrismaticBtnX(), buttons.getPlayerPrismaticBtnY());
        float renderX = btnPos.glX() + 50f;
        float renderY = btnPos.glSpriteY(PRISMATIC_BTN_SIZE) + 40f;

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