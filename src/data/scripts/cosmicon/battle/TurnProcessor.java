package data.scripts.cosmicon.battle;

import data.scripts.CosmiconConfig;
import data.scripts.cosmicon.util.CosmiconLogger;
import data.scripts.cosmicon.ai.AIPrismaticSelector.PrismaticDecision;
import data.scripts.cosmicon.battle.StatusEffectProcessor.Phase;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.battle.EffectState.ModificationRecord;
import data.scripts.cosmicon.battle.TurnState.TurnType;
import data.scripts.cosmicon.tutorial.TutorialDiceRoller;
import data.scripts.cosmicon.character.PassiveEventSystem;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveResults.PassiveResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;

public class TurnProcessor {
    
    private final BattleState state;
    private AIEngine aiEngine;
    private WeatherController weatherController;
    private DiceRoller diceRoller;
    private DamageResolver damageResolver;
    private DiceRollManager diceRollManager;
    
    private DamageResolver.DamageResult pendingDamageResult;
    private boolean damageAnimationInProgress;
    private boolean impactDamageApplied = false;
    
    private float aiSelectDelay;
    private boolean aiSelectionComplete;
    
    private enum AIVisualPhase {
        NONE,
        REROLL_PLANNING,
        REROLL_REVEALING,
        REROLL_DELAYING,
        REROLL_ANIMATING,
        SELECTION_PLANNING,
        SELECTION_REVEALING,
        SELECTION_DELAYING,
        SELECTION_COMPLETE
    }
    
    private AIVisualPhase aiVisualPhase = AIVisualPhase.NONE;
    private List<Integer> aiPlannedIndices;
    private float aiPhaseTimer;
    private BooleanSupplier opponentAnimationCompleteChecker = () -> true;
    
    public TurnProcessor(BattleState state) {
        this.state = state;
        this.aiSelectDelay = 0f;
        this.aiSelectionComplete = false;
        this.pendingDamageResult = null;
        this.damageAnimationInProgress = false;
    }
    
    public void setAIEngine(AIEngine aiEngine) {
        this.aiEngine = aiEngine;
    }

    public void setWeatherController(WeatherController controller) {
        this.weatherController = controller;
    }
    
    public void setDiceRoller(DiceRoller diceRoller) {
        this.diceRoller = diceRoller;
    }
    
    public DiceRoller getDiceRoller() {
        return diceRoller;
    }
    
    public void setDamageResolver(DamageResolver resolver) {
        this.damageResolver = resolver;
    }

    public void setDiceRollManager(DiceRollManager manager) {
        this.diceRollManager = manager;
    }

    public void setOpponentAnimationCompleteChecker(BooleanSupplier checker) {
        if (checker != null) {
            this.opponentAnimationCompleteChecker = checker;
        }
    }

    public void skipAiAnimation() {
        aiPhaseTimer = 0f;
    }
    
    public void startBattle() {
        weatherController.applyStartOfBattle();
        executeTurn();
    }
    
    public void executeTurn() {
        state.clearConfirmedSelections();
        state.setAttackValue(0);
        state.setDefenseValue(0);
        state.setDefenderRolling(false);
        
        boolean playerIsAttacker = state.isPlayerAttacker();
        state.clearPendingDefLevelBoost(playerIsAttacker);
        
        state.getPlayerEffects().resetTurnState();
        state.getOpponentEffects().resetTurnState();
        
        applyPendingStrength(true);
        applyPendingStrength(false);
        
        aiVisualPhase = AIVisualPhase.NONE;
        aiSelectionComplete = false;
        aiPlannedIndices = null;
        aiPhaseTimer = 0f;
        
        AISelectionVisualizer viz = state.getAiSelectionVisualizer();
        if (viz != null) {
            viz.reset();
        }
        
        weatherController.applyStartOfTurn(state);
        
        StatusEffectProcessor.BattleContext playerContext = createBattleContext(true);
        StatusEffectProcessor.BattleContext opponentContext = createBattleContext(false);
        
        state.getPlayerEffects().processPhase(Phase.START_OF_TURN, 
            playerIsAttacker ? TurnType.ATTACK : TurnType.DEFENSE, playerContext);
        state.getOpponentEffects().processPhase(Phase.START_OF_TURN,
            playerIsAttacker ? TurnType.DEFENSE : TurnType.ATTACK, opponentContext);
        
        state.setPlayerHp(playerContext.getCurrentHp());
        state.setOpponentHp(opponentContext.getCurrentHp());
        
        state.resetRerollsUsedThisTurn();
        
        int baseRerolls = playerIsAttacker ? CosmiconConfig.DEFAULT_REROLLS : 0;
        int opponentBaseRerolls = playerIsAttacker ? 0 : CosmiconConfig.DEFAULT_REROLLS;

        state.setRemainingRerolls(true, baseRerolls);
        state.setRemainingRerolls(false, opponentBaseRerolls);
        
        weatherController.applyRerollPhase(state);
        
        CosmiconLogger.debug("[AI_REROLL_DIAG] After base+weather: playerRerolls=%d, opponentRerolls=%d", 
            state.getRemainingRerolls(true), state.getRemainingRerolls(false));
        
        TurnType playerTurnType = playerIsAttacker ? TurnType.ATTACK : TurnType.DEFENSE;
        TurnType opponentTurnType = playerIsAttacker ? TurnType.DEFENSE : TurnType.ATTACK;
        
        PassiveEventSystem.onStartOfAttackTurn(state, true);
        PassiveEventSystem.onStartOfAttackTurn(state, false);
        
        state.getPlayerEffects().processPhase(Phase.BEFORE_ROLL, playerTurnType, playerContext);
        state.getOpponentEffects().processPhase(Phase.BEFORE_ROLL, opponentTurnType, opponentContext);
        
        state.getPlayerEffects().removeEffect(StatusEffectProcessor.StatusEffect.YAO_GUANG_REROLLS);
        state.getOpponentEffects().removeEffect(StatusEffectProcessor.StatusEffect.YAO_GUANG_REROLLS);
        
        int playerRerollBonus = playerContext.getRerollCount();
        int opponentRerollBonus = opponentContext.getRerollCount();
        
        CosmiconLogger.debug("[AI_REROLL_DIAG] BEFORE_ROLL bonuses: playerRerollBonus=%d, opponentRerollBonus=%d", 
            playerRerollBonus, opponentRerollBonus);
        
        if (playerRerollBonus != 0) {
            state.setRemainingRerolls(true, state.getRemainingRerolls(true) + playerRerollBonus);
        }
        if (opponentRerollBonus != 0) {
            state.setRemainingRerolls(false, state.getRemainingRerolls(false) + opponentRerollBonus);
        }
        
        CosmiconLogger.debug("[AI_REROLL_DIAG] FINAL rerolls after bonuses: playerRerolls=%d, opponentRerolls=%d", 
            state.getRemainingRerolls(true), state.getRemainingRerolls(false));
        
        diceRoller.rollForAttacker(state);
        
        state.setCurrentPhase(TurnState.Phase.ROLLING);
        state.notifyPhaseChange(TurnState.Phase.ROLLING);
        
        StatusEffectProcessor.BattleContext attackerContext = createBattleContext(playerIsAttacker);
        TurnType attackerTurnType = TurnType.ATTACK;
        state.getEffects(playerIsAttacker).processPhase(Phase.AFTER_ROLL, attackerTurnType, attackerContext);
        state.setDiceValues(playerIsAttacker, attackerContext.getDiceValues());
        
        state.clearPrismaticState();
    }
    
    public void advanceToSelectPhase() {
        if (state.getCurrentPhase() != TurnState.Phase.ROLLING) return;
        
        state.clearDiceSelection(true);
        state.clearDiceSelection(false);
        
        state.setCurrentPhase(TurnState.Phase.SELECTING_ATTACK);
        state.notifyPhaseChange(TurnState.Phase.SELECTING_ATTACK);
        
        if (!state.isPlayerAttacker()) {
            startAiSelection();
        }
    }
    
    private void advanceToDefensePhase() {
        state.setDefenderRolling(true);
        
        PassiveEventSystem.onStartOfDefenseTurn(state, true);
        PassiveEventSystem.onStartOfDefenseTurn(state, false);
        
        diceRoller.rollForDefender(state);
        
        state.notifyTransitionToDefenderRoll();
        
        state.setCurrentPhase(TurnState.Phase.ROLLING);
        state.notifyPhaseChange(TurnState.Phase.ROLLING);
        
        boolean defenderIsPlayer = !state.isPlayerAttacker();
        StatusEffectProcessor.BattleContext defenderContext = createBattleContext(defenderIsPlayer);
        TurnType defenderTurnType = TurnType.DEFENSE;
        state.getEffects(defenderIsPlayer).processPhase(Phase.AFTER_ROLL, defenderTurnType, defenderContext);
        state.setDiceValues(defenderIsPlayer, defenderContext.getDiceValues());
    }
    
    public void advanceToDiceDisplayAttack() {
        state.setCurrentPhase(TurnState.Phase.DICE_DISPLAY_ATTACK);
        state.notifyPhaseChange(TurnState.Phase.DICE_DISPLAY_ATTACK);
    }
    
    public void advanceToDiceDisplayDefense() {
        state.setCurrentPhase(TurnState.Phase.DICE_DISPLAY_DEFENSE);
        state.notifyPhaseChange(TurnState.Phase.DICE_DISPLAY_DEFENSE);
    }
    
    public void advanceFromDiceDisplayAttack() {
        advanceToDefensePhase();
    }
    
    public void advanceFromDiceDisplayDefense() {
        resolveDamage();
    }
    
    public void advanceToDefenderSelectPhase() {
        if (!state.isDefenderRolling()) return;
        state.setDefenderRolling(false);

        boolean defenderIsPlayer = !state.isPlayerAttacker();
        state.clearDiceSelection(defenderIsPlayer);

        weatherController.applyDefenderPreSelectionEffects(state, defenderIsPlayer);

        state.setCurrentPhase(TurnState.Phase.SELECTING_DEFENSE);
        state.notifyPhaseChange(TurnState.Phase.SELECTING_DEFENSE);

        if (state.isPlayerAttacker()) {
            startAiSelection();
        }
    }
    
    private void startAiSelection() {
        int aiRerolls = state.getRemainingRerolls(false);
        CosmiconLogger.debug("[AI_REROLL_DIAG] startAiSelection: aiRerolls=%d, isPlayerAttacker=%s, phase=%s", 
            aiRerolls, state.isPlayerAttacker(), state.getCurrentPhase());
        CosmiconLogger.debug("[AI_REROLL_DIAG] opponentDiceValues=%s, opponentDiceTypes=%s", 
            state.getOpponentDiceValues(), state.getOpponentDiceTypes());
        
        TutorialDiceRoller tutorialDiceRoller = state.getTutorialDiceRoller();
        
        PrismaticDecision prismDecision = null;
        if (tutorialDiceRoller != null) {
            Object forced = tutorialDiceRoller.planOpponentPrismatic(state);
            if (forced instanceof PrismaticDecision pd) {
                prismDecision = pd;
            }
        }
        if (prismDecision == null) {
            prismDecision = aiEngine.planPrismaticUse(state, false);
        }
        if (prismDecision != null && prismDecision.shouldUse()) {
            state.addPrismaticDiceToPool(prismDecision.instance(), false);
            if (diceRollManager != null) {
                float opponentCenterX = BattleRenderingUtils.PANEL_WIDTH / 2f;
                float opponentCenterY = BattleRenderingUtils.PANEL_HEIGHT / 2f - 40f;
                diceRollManager.appendOpponentInstantDice(DiceType.PRISMATIC, prismDecision.instance().faceIndex,
                    opponentCenterX, opponentCenterY);
            }
            CosmiconLogger.debug("AI added prismatic dice: %s with score %.1f", 
                prismDecision.instance().type.getId(), prismDecision.score());
        }
        
        if (aiRerolls > 0) {
            List<Integer> rerollIndices = null;
            if (tutorialDiceRoller != null) {
                rerollIndices = tutorialDiceRoller.planOpponentReroll();
            }
            if (rerollIndices == null) {
                rerollIndices = aiEngine.planReroll(state, false);
            }
            CosmiconLogger.debug("[AI_REROLL_DIAG] planReroll returned: %s (size=%d)", rerollIndices, rerollIndices.size());
            aiPlannedIndices = rerollIndices;
            if (!rerollIndices.isEmpty()) {
                aiVisualPhase = AIVisualPhase.REROLL_PLANNING;
                AISelectionVisualizer viz = state.getAiSelectionVisualizer();
                if (viz != null) {
                    viz.planSelection(rerollIndices, true);
                }
                aiPhaseTimer = 0f;
                CosmiconLogger.debug("AI entering REROLL_PLANNING with indices: %s", rerollIndices);
                return;
            }
        }
        aiVisualPhase = AIVisualPhase.SELECTION_PLANNING;
        aiPhaseTimer = 0f;
        aiPlannedIndices = null;
    }
    
    public void advanceAiSelection(float amount) {
        advanceAiVisualPhase(amount);
    }
    
    private void advanceAiVisualPhase(float amount) {
        if (aiVisualPhase == AIVisualPhase.NONE) {
            if (!aiSelectionComplete && aiSelectDelay > 0f) {
                aiSelectDelay -= amount;
                if (aiSelectDelay <= 0f) {
                    performAiSelection();
                }
            }
            return;
        }
        
        AISelectionVisualizer viz = state.getAiSelectionVisualizer();
        
        switch (aiVisualPhase) {
            case REROLL_PLANNING -> {
                aiVisualPhase = AIVisualPhase.REROLL_REVEALING;
                if (viz != null && viz.hasStarted()) {
                    viz.advance(amount);
                }
            }
            
            case REROLL_REVEALING -> {
                if (viz != null) {
                    viz.advance(amount);
                    if (viz.isComplete()) {
                        aiVisualPhase = AIVisualPhase.REROLL_DELAYING;
                        aiPhaseTimer = CosmiconConfig.AI_REROLL_PREVIEW_DELAY;
                    }
                } else {
                    aiVisualPhase = AIVisualPhase.REROLL_DELAYING;
                    aiPhaseTimer = CosmiconConfig.AI_REROLL_PREVIEW_DELAY;
                }
            }
            
            case REROLL_DELAYING -> {
                aiPhaseTimer -= amount;
                if (aiPhaseTimer <= 0f) {
                    aiVisualPhase = AIVisualPhase.REROLL_ANIMATING;
                    aiPhaseTimer = DiceAnimator.getTotalDuration() + 0.1f;
                    
                    executePlannedAiReroll();
                }
            }
            
            case REROLL_ANIMATING -> {
                aiPhaseTimer -= amount;
                if (aiPhaseTimer <= 0f && opponentAnimationCompleteChecker.getAsBoolean()) {
                    if (viz != null) viz.reset();

                    int remainingRerolls = state.getRemainingRerolls(false);
                    if (remainingRerolls > 0) {
                        TutorialDiceRoller tdr = state.getTutorialDiceRoller();
                        List<Integer> nextRerollIndices = null;
                        if (tdr != null) {
                            nextRerollIndices = tdr.planOpponentReroll();
                        }
                        if (nextRerollIndices == null) {
                            nextRerollIndices = aiEngine.planReroll(state, false);
                        }
                        if (!nextRerollIndices.isEmpty()) {
                            CosmiconLogger.debug("AI planning additional reroll: %s", nextRerollIndices);
                            aiPlannedIndices = nextRerollIndices;
                            aiVisualPhase = AIVisualPhase.REROLL_PLANNING;
                            if (viz != null) {
                                viz.planSelection(nextRerollIndices, true);
                            }
                            break;
                        }
                    }

                    CosmiconLogger.debug("AI reroll complete, transitioning to SELECTION_PLANNING");
                    aiVisualPhase = AIVisualPhase.SELECTION_PLANNING;
                    aiPlannedIndices = null;
                }
            }
            
            case SELECTION_PLANNING -> {
                TutorialDiceRoller tdr = state.getTutorialDiceRoller();
                List<Integer> selectIndices = null;
                if (tdr != null) {
                    selectIndices = tdr.planOpponentSelection(state);
                }
                if (selectIndices == null) {
                    selectIndices = aiEngine.planSelection(state, false);
                }
                aiPlannedIndices = selectIndices;
                
                if (viz != null) {
                    viz.planSelection(selectIndices, false);
                }
                
                aiVisualPhase = AIVisualPhase.SELECTION_REVEALING;
            }
            
            case SELECTION_REVEALING -> {
                if (viz != null) {
                    viz.advance(amount);
                    if (viz.isComplete()) {
                        aiVisualPhase = AIVisualPhase.SELECTION_DELAYING;
                        aiPhaseTimer = CosmiconConfig.AI_CONFIRM_DELAY;
                    }
                } else {
                    aiVisualPhase = AIVisualPhase.SELECTION_DELAYING;
                    aiPhaseTimer = CosmiconConfig.AI_CONFIRM_DELAY;
                }
            }
            
            case SELECTION_DELAYING -> {
                aiPhaseTimer -= amount;
                if (aiPhaseTimer <= 0f) {
                    executePlannedAiSelection();
                    aiVisualPhase = AIVisualPhase.SELECTION_COMPLETE;
                }
            }
            
            case SELECTION_COMPLETE -> {
                aiVisualPhase = AIVisualPhase.NONE;
                if (viz != null) viz.reset();
                aiSelectionComplete = true;
            }
        }
    }
    
    private void executePlannedAiReroll() {
        if (aiPlannedIndices == null || aiPlannedIndices.isEmpty()) return;
        
        List<Boolean> selected = state.getDiceSelected(false);
        if (selected != null) {
            Collections.fill(selected, false);
            for (int idx : aiPlannedIndices) {
                if (idx < selected.size()) {
                    selected.set(idx, true);
                }
            }
        }
        
        diceRoller.rerollSelected(state, false);
        
        CosmiconLogger.debug("AI executed planned reroll for indices: %s", aiPlannedIndices);
    }
    
    private void executePlannedAiSelection() {
        if (aiPlannedIndices == null) return;
        
        boolean isAttackPhase = state.getCurrentPhase() == TurnState.Phase.SELECTING_ATTACK;
        boolean aiIsAttacker = !state.isPlayerAttacker();
        boolean forPlayer = false;
        
        List<Boolean> selected = state.getDiceSelected(forPlayer);
        if (selected != null) {
            Collections.fill(selected, false);
            for (int idx : aiPlannedIndices) {
                if (idx < selected.size()) {
                    selected.set(idx, true);
                }
                state.recordFaceSelection(state.getDiceValues(forPlayer).get(idx), forPlayer);
            }
        }
        
        if (isAttackPhase) {
            state.setAttackValue(state.calculateSelectedSum(forPlayer));
        } else {
            state.setDefenseValue(state.calculateSelectedSum(forPlayer));
        }
        
        applyPostSelectionProcessing(forPlayer);
        
        if (aiIsAttacker && state.getCurrentPhase() == TurnState.Phase.SELECTING_ATTACK) {
            state.setAttackerConfirmedSelection(state.getSelectedDiceValuesFormatted(false));
            state.getAiSelectionVisualizer().reset();
            advanceToDiceDisplayAttack();
        } else if (!aiIsAttacker && state.getCurrentPhase() == TurnState.Phase.SELECTING_DEFENSE) {
            state.setDefenderConfirmedSelection(state.getSelectedDiceValuesFormatted(false));
            weatherController.applyDefenderSelectionPhase(state, false);
            advanceToDiceDisplayDefense();
        }
        
        CosmiconLogger.debug("AI executed planned selection for indices: %s", aiPlannedIndices);
    }
    
    private void performAiSelection() {
        boolean isAttackPhase = state.getCurrentPhase() == TurnState.Phase.SELECTING_ATTACK;
        boolean aiIsAttacker = !state.isPlayerAttacker();
        boolean forPlayer = false;
        
        aiEngine.executeSelection(state, forPlayer);
        
        if (isAttackPhase) {
            state.setAttackValue(state.calculateSelectedSum(forPlayer));
        } else {
            state.setDefenseValue(state.calculateSelectedSum(forPlayer));
        }
        
        applyPostSelectionProcessing(forPlayer);
        
        aiSelectionComplete = true;
        
        if (aiIsAttacker && state.getCurrentPhase() == TurnState.Phase.SELECTING_ATTACK) {
            state.setAttackerConfirmedSelection(state.getSelectedDiceValuesFormatted(false));
            advanceToDiceDisplayAttack();
        } else if (!aiIsAttacker && state.getCurrentPhase() == TurnState.Phase.SELECTING_DEFENSE) {
            state.setDefenderConfirmedSelection(state.getSelectedDiceValuesFormatted(false));
            weatherController.applyDefenderSelectionPhase(state, false);
            advanceToDiceDisplayDefense();
        }
    }
    
    private void resolveDamage() {
        weatherController.applyPreResolution(state);

        int prePrismaticAttack = state.getAttackValue();
        int prePrismaticDefense = state.getDefenseValue();

        state.applyPrismaticDiceEffects();

        int postPrismaticAttack = state.getAttackValue();
        int postPrismaticDefense = state.getDefenseValue();
        
        boolean playerIsAttacker = state.isPlayerAttacker();
        if (postPrismaticAttack != prePrismaticAttack) {
            int delta = postPrismaticAttack - prePrismaticAttack;
            state.queueValueChange(playerIsAttacker, "PRISMATIC", delta);
            state.notifyValueChange(playerIsAttacker, "PRISMATIC", prePrismaticAttack, postPrismaticAttack, delta);
        }
        if (postPrismaticDefense != prePrismaticDefense) {
            int delta = postPrismaticDefense - prePrismaticDefense;
            state.queueValueChange(!playerIsAttacker, "PRISMATIC", delta);
            state.notifyValueChange(!playerIsAttacker, "PRISMATIC", prePrismaticDefense, postPrismaticDefense, delta);
        }

        state.setCurrentPhase(TurnState.Phase.RESOLVING_PRE_CLASH);
        state.notifyPhaseChange(TurnState.Phase.RESOLVING_PRE_CLASH);
    }
    
    public void proceedToModificationPause() {
        if (state.getCurrentPhase() != TurnState.Phase.RESOLVING_PRE_CLASH) return;

        if (state.hasPendingModification()) {
            state.setCurrentPhase(TurnState.Phase.RESOLVING_MODIFICATION);
            state.notifyPhaseChange(TurnState.Phase.RESOLVING_MODIFICATION);
            return;
        }

        continueToClash();
    }

    public void proceedFromModificationPause() {
        if (state.getCurrentPhase() != TurnState.Phase.RESOLVING_MODIFICATION) return;
        applyPendingModifications();
        continueToClash();
    }

    private void applyPendingModifications() {
        List<ModificationRecord> orderedModifications = state.getModificationOrder();
        orderedModifications.sort(Comparator.comparingInt(ModificationRecord::sequence));

        for (ModificationRecord record : orderedModifications) {
            StatusEffect effect = record.effect();
            boolean forPlayer = record.forPlayer();
            StatusEffectProcessor effects = state.getEffects(forPlayer);

            if (effect == StatusEffect.ARISE && effects.hasEffect(StatusEffect.ARISE)) {
                CosmiconLogger.debug("ARISE triggered for %s", forPlayer ? "player" : "opponent");
                StatusEffectProcessor.BattleContext context = createBattleContext(forPlayer);
                List<Integer> preValues = new ArrayList<>(state.getDiceValues(forPlayer));
                int oldSum = state.calculateSelectedSum(forPlayer);

                effects.processedEffectsFromModification(StatusEffect.ARISE);
                int ariseDiceIndex = context.applyArise();
                if (ariseDiceIndex >= 0) {
                    if (diceRollManager != null) {
                        diceRollManager.setRestDiceEffect(ariseDiceIndex, StatusEffect.ARISE, forPlayer);
                    }
                }
                effects.removeLayers(StatusEffect.ARISE, 1);
                state.setDiceValues(forPlayer, context.getDiceValues());

                List<Integer> postValues = state.getDiceValues(forPlayer);
                notifyRestDiceValueChanges(preValues, postValues, forPlayer);

                int newSum = state.calculateSelectedSum(forPlayer);
                if (newSum != oldSum) {
                    int delta = newSum - oldSum;
                    boolean isAttack = state.isAttacker(forPlayer);
                    if (isAttack) state.setAttackValue(newSum);
                    else state.setDefenseValue(newSum);
                    String changeType = isAttack ? "ATTACK_LEVEL_UP" : "DEFENSE_LEVEL_UP";
                    state.queueValueChange(forPlayer, changeType, delta);
                    state.notifyValueChange(forPlayer, "ARISE", oldSum, newSum, delta);
                }
            }

            if (effect == StatusEffect.HACK && effects.hasEffect(StatusEffect.HACK)) {
                CosmiconLogger.debug("HACK triggered for %s", forPlayer ? "player" : "opponent");
                boolean targetIsPlayer = !forPlayer;
                StatusEffectProcessor.BattleContext targetContext = createBattleContext(targetIsPlayer);
                List<Integer> preHackValues = new ArrayList<>(state.getDiceValues(targetIsPlayer));
                int oldTargetSum = state.calculateSelectedSum(targetIsPlayer);

                effects.processedEffectsFromModification(StatusEffect.HACK);
                int hackDiceIndex = targetContext.applyHackToSelectedDice();
                if (hackDiceIndex >= 0) {
                    if (diceRollManager != null) {
                        diceRollManager.setRestDiceEffect(hackDiceIndex, StatusEffect.HACK, targetIsPlayer);
                    }
                }
                state.setDiceValues(targetIsPlayer, targetContext.getDiceValues());
                boolean targetIsAttacker = state.isAttacker(targetIsPlayer);
                if (targetIsAttacker) state.setAttackValue(state.calculateSelectedSum(targetIsPlayer));
                else state.setDefenseValue(state.calculateSelectedSum(targetIsPlayer));

                List<Integer> postHackValues = state.getDiceValues(targetIsPlayer);
                notifyRestDiceValueChanges(preHackValues, postHackValues, targetIsPlayer);

                int newTargetSum = state.calculateSelectedSum(targetIsPlayer);
                if (newTargetSum != oldTargetSum) {
                    int delta = newTargetSum - oldTargetSum;
                    String changeType = targetIsAttacker ? "ATTACK_LEVEL_UP" : "DEFENSE_LEVEL_UP";
                    state.queueValueChange(targetIsPlayer, changeType, delta);
                    state.notifyValueChange(targetIsPlayer, "HACK", oldTargetSum, newTargetSum, delta);
                }

                effects.removeLayers(StatusEffect.HACK, 1);
            }
        }

        state.clearModificationOrder();
    }

    public void continueToClash() {
        if (state.getCurrentPhase() != TurnState.Phase.RESOLVING_PRE_CLASH &&
            state.getCurrentPhase() != TurnState.Phase.RESOLVING_MODIFICATION) return;

        StatusEffectProcessor.BattleContext attackerContext = createBattleContext(state.isPlayerAttacker());
        StatusEffectProcessor.BattleContext defenderContext = createBattleContext(!state.isPlayerAttacker());
        
        TurnType attackerTurnType = TurnType.ATTACK;
        TurnType defenderTurnType = TurnType.DEFENSE;
        
        int playerThornsDamage = state.getPlayerEffects().processPhase(Phase.BEFORE_RESOLUTION, 
            state.isPlayerAttacker() ? attackerTurnType : defenderTurnType, 
            state.isPlayerAttacker() ? attackerContext : defenderContext);
        int opponentThornsDamage = state.getOpponentEffects().processPhase(Phase.BEFORE_RESOLUTION,
            state.isPlayerAttacker() ? defenderTurnType : attackerTurnType,
            state.isPlayerAttacker() ? defenderContext : attackerContext);
        
        int attackerInstantDamage = attackerContext.getInstantDamageToOpponent();
        int defenderInstantDamage = defenderContext.getInstantDamageToOpponent();
        
        int attackerHolderDamage = attackerContext.getInstantDamageFromHolder();
        int defenderHolderDamage = defenderContext.getInstantDamageFromHolder();
        
        if (attackerInstantDamage > 0) {
            if (state.isPlayerAttacker()) {
                state.setOpponentHp(Math.max(0, state.getOpponentHp() - attackerInstantDamage));
                state.notifySecondaryDamage(false, attackerInstantDamage, "INSTANT_DAMAGE");
            } else {
                state.setPlayerHp(Math.max(0, state.getPlayerHp() - attackerInstantDamage));
                state.notifySecondaryDamage(true, attackerInstantDamage, "INSTANT_DAMAGE");
            }
        }
        
        if (defenderInstantDamage > 0) {
            if (state.isPlayerAttacker()) {
                state.setPlayerHp(Math.max(0, state.getPlayerHp() - defenderInstantDamage));
                state.notifySecondaryDamage(true, defenderInstantDamage, "INSTANT_DAMAGE");
            } else {
                state.setOpponentHp(Math.max(0, state.getOpponentHp() - defenderInstantDamage));
                state.notifySecondaryDamage(false, defenderInstantDamage, "INSTANT_DAMAGE");
            }
        }
        
        if (attackerHolderDamage > 0) {
            if (state.isPlayerAttacker()) {
                state.applyDamageTo(false, attackerHolderDamage);
                state.notifySecondaryDamage(false, attackerHolderDamage, "INSTANT_DAMAGE");
            } else {
                state.applyDamageTo(true, attackerHolderDamage);
                state.notifySecondaryDamage(true, attackerHolderDamage, "INSTANT_DAMAGE");
            }
        }
        
        if (defenderHolderDamage > 0) {
            if (state.isPlayerAttacker()) {
                state.applyDamageTo(true, defenderHolderDamage);
                state.notifySecondaryDamage(true, defenderHolderDamage, "INSTANT_DAMAGE");
            } else {
                state.applyDamageTo(false, defenderHolderDamage);
                state.notifySecondaryDamage(false, defenderHolderDamage, "INSTANT_DAMAGE");
            }
        }

        PassiveEventSystem.onAttackResolution(state, state.isPlayerAttacker());
        PassiveEventSystem.onAttackResolution(state, !state.isPlayerAttacker());

        if (playerThornsDamage > 0 && state.getPlayerEffects().hasEffect(StatusEffectProcessor.StatusEffect.THORNS)) {
            state.applyDamageTo(true, playerThornsDamage);
            state.notifySecondaryDamage(true, playerThornsDamage, "THORNS");
        }
        if (opponentThornsDamage > 0 && state.getOpponentEffects().hasEffect(StatusEffectProcessor.StatusEffect.THORNS)) {
            state.applyDamageTo(false, opponentThornsDamage);
            state.notifySecondaryDamage(false, opponentThornsDamage, "THORNS");
        }

        state.setCurrentPhase(TurnState.Phase.RESOLVING);
        state.notifyPhaseChange(TurnState.Phase.RESOLVING);

        DamageResolver.DamageResult result = damageResolver.resolve(state);
        pendingDamageResult = result;
        damageAnimationInProgress = true;
        impactDamageApplied = false;
        state.notifyDamageAnimationStart(result);
    }
    
    public void onDamageImpacted() {
        if (pendingDamageResult == null || !damageAnimationInProgress) return;

        applyImpactDamage(pendingDamageResult);
        impactDamageApplied = true;
    }

    public void onDamageAnimationComplete() {
        if (pendingDamageResult == null || !damageAnimationInProgress) return;

        if (!impactDamageApplied) {
            applyImpactDamage(pendingDamageResult);
        }
        applyPostAnimationEffects(pendingDamageResult);
        pendingDamageResult = null;
        damageAnimationInProgress = false;
        impactDamageApplied = false;
        state.notifyDamageAnimationComplete();
    }

private void applyImpactDamage(DamageResolver.DamageResult result) {
        int damage = result.damageToDefender();
        boolean playerIsAttacker = state.isPlayerAttacker();
        boolean defenderIsPlayer = !playerIsAttacker;

        if (damage > 0) {
            if (playerIsAttacker) {
                state.applyDamageTo(false, damage);

                if (result.siphonHeal() > 0) {
                    state.applyHealTo(true, result.siphonHeal());
                }
                state.getEffects(true).removeEffect(StatusEffect.SIPHON);
            } else {
                state.applyDamageTo(true, damage);

                if (result.siphonHeal() > 0) {
                    state.applyHealTo(false, result.siphonHeal());
                }
                state.getEffects(false).removeEffect(StatusEffect.SIPHON);
            }

            int instantDamageToAttacker = PassiveEventSystem.onDamageTaken(state, defenderIsPlayer, damage);
            if (instantDamageToAttacker > 0) {
                state.applyDamageTo(playerIsAttacker, instantDamageToAttacker);
                state.notifySecondaryDamage(playerIsAttacker, instantDamageToAttacker, "INSTANT_DAMAGE");
            }

            PassiveEventSystem.onDefenseFail(state, defenderIsPlayer);
        }
    }

private void applyPostAnimationEffects(DamageResolver.DamageResult result) {
        boolean playerIsAttacker = state.isPlayerAttacker();
        boolean defenderIsPlayer = !playerIsAttacker;

        if (result.thornsDamage() > 0) {
            state.getEffects(defenderIsPlayer).removeEffect(StatusEffectProcessor.StatusEffect.THORNS);
        }

        if (result.counterDamage() > 0) {
            state.applyDamageTo(playerIsAttacker, result.counterDamage());
            state.notifySecondaryDamage(playerIsAttacker, result.counterDamage(), "COUNTER");

            if (state.getEffects(playerIsAttacker).hasEffect(StatusEffectProcessor.StatusEffect.COMBO)) {
                state.applyDamageTo(playerIsAttacker, result.counterDamage());
                state.notifySecondaryDamage(playerIsAttacker, result.counterDamage(), "COUNTER");
            }
        }

        if (result.selfThornsDamage() > 0) {
            state.getEffects(playerIsAttacker).removeEffect(StatusEffectProcessor.StatusEffect.THORNS);
        }

        if (result.overloadSelfDamage() > 0) {
            state.applyDamageTo(playerIsAttacker, result.overloadSelfDamage());
            state.notifySecondaryDamage(playerIsAttacker, result.overloadSelfDamage(), "OVERLOAD");
        }

        if (result.instantDamage() > 0) {
            state.applyDamageTo(defenderIsPlayer, result.instantDamage());
            state.notifySecondaryDamage(defenderIsPlayer, result.instantDamage(), "INSTANT_DAMAGE");
        }

        if (result.reflectDamage() > 0) {
            state.applyDamageTo(playerIsAttacker, result.reflectDamage());
            state.notifySecondaryDamage(playerIsAttacker, result.reflectDamage(), "REFLECT");
            state.getEffects(defenderIsPlayer).removeEffect(StatusEffectProcessor.StatusEffect.REFLECT);
        }

        applyComboAttack(playerIsAttacker, defenderIsPlayer);

        if (result.perforationSuccessful()) {
            state.getEffects(playerIsAttacker).removeEffect(StatusEffectProcessor.StatusEffect.PERFORATION);
        }

        if (state.getEffects(defenderIsPlayer).isForcefieldActive()) {
            state.getEffects(defenderIsPlayer).removeEffect(StatusEffectProcessor.StatusEffect.FORCEFIELD);
        }

        PassiveEventSystem.onAttackResolved(state, playerIsAttacker, result.perforationSuccessful(), result.damageToDefender());

        state.notifyDamageResolved(result.damageToDefender(), state.getPlayerHp(), state.getOpponentHp());

        if (state.getPlayerHp() <= 0 || state.getOpponentHp() <= 0) {
            endBattle();
        } else {
            state.setCurrentPhase(TurnState.Phase.WAITING_NEXT_TURN);
            state.notifyPhaseChange(TurnState.Phase.WAITING_NEXT_TURN);
        }
    }
    
    private void applyComboAttack(boolean playerIsAttacker, boolean defenderIsPlayer) {
        StatusEffectProcessor attackerEffects = state.getEffects(playerIsAttacker);
        
        if (!attackerEffects.hasEffect(StatusEffect.COMBO)) {
            return;
        }
        
        int attackValue = state.getAttackValue();
        int modifiedAttack = attackValue + attackerEffects.calculateAttackBonus(TurnType.ATTACK);
        int attackerPrismaticValue = state.getPrismaticDiceTotalValue(playerIsAttacker);
        modifiedAttack += attackerPrismaticValue;

        StatusEffectProcessor defenderEffects = state.getEffects(defenderIsPlayer);
        int defenseValue = state.getDefenseValue();
        int modifiedDefense = defenseValue + defenderEffects.calculateDefenseBonus(TurnType.DEFENSE);
        int defenderPrismaticValue = state.getPrismaticDiceTotalValue(defenderIsPlayer);
        modifiedDefense += defenderPrismaticValue;

        if (attackerEffects.shouldIgnoreDefense()) {
            modifiedDefense = 0;
        }

        int comboDamage = Math.max(0, modifiedAttack - modifiedDefense);
        
        if (defenderEffects.isForcefieldActive() && comboDamage > 0) {
            comboDamage = 0;
        }
        
        attackerEffects.removeEffect(StatusEffect.COMBO);
        
        if (comboDamage > 0) {
            state.applyDamageTo(defenderIsPlayer, comboDamage);
            
            CosmiconLogger.info("COMBO attack: %d damage (Attack=%d, Defense=%d, modifiedAtk=%d, modifiedDef=%d)",
                comboDamage, attackValue, defenseValue, modifiedAttack, modifiedDefense);
            
            int reflectDamage = defenderEffects.getLayers(StatusEffectProcessor.StatusEffect.REFLECT);
            if (reflectDamage > 0) {
                state.applyDamageTo(playerIsAttacker, reflectDamage);
                state.notifySecondaryDamage(playerIsAttacker, reflectDamage, "REFLECT");
                defenderEffects.removeEffect(StatusEffectProcessor.StatusEffect.REFLECT);
                CosmiconLogger.debug("COMBO reflect: %d damage to attacker", reflectDamage);
            }
            
            int instantDamageToAttacker = PassiveEventSystem.onDamageTaken(state, defenderIsPlayer, comboDamage);
            if (instantDamageToAttacker > 0) {
                state.applyDamageTo(playerIsAttacker, instantDamageToAttacker);
                state.notifySecondaryDamage(playerIsAttacker, instantDamageToAttacker, "INSTANT_DAMAGE");
            }
            
            PassiveEventSystem.onDefenseFail(state, defenderIsPlayer);
        }
}
    
    public void advanceToNextTurn() {
        if (state.getCurrentPhase() != TurnState.Phase.WAITING_NEXT_TURN) return;
        
        StatusEffectProcessor.BattleContext playerContext = createBattleContext(true);
        StatusEffectProcessor.BattleContext opponentContext = createBattleContext(false);
        
        int playerPoisonDamage = state.getPlayerEffects().processPhase(
            Phase.END_OF_TURN,
            state.isPlayerAttacker() ? TurnType.ATTACK : TurnType.DEFENSE,
            playerContext);
        int opponentPoisonDamage = state.getOpponentEffects().processPhase(
            Phase.END_OF_TURN,
            state.isPlayerAttacker() ? TurnType.DEFENSE : TurnType.ATTACK,
            opponentContext);
        
        state.applyDamageTo(true, playerPoisonDamage);
        state.applyDamageTo(false, opponentPoisonDamage);
        
        if (playerPoisonDamage > 0) {
            state.notifySecondaryDamage(true, playerPoisonDamage, "POISON");
        }
        if (opponentPoisonDamage > 0) {
            state.notifySecondaryDamage(false, opponentPoisonDamage, "POISON");
        }
        
        if (state.getPlayerHp() <= 0 || state.getOpponentHp() <= 0) {
            endBattle();
            return;
        }
        
        state.getPlayerEffects().clearTemporaryEffects();
        state.getOpponentEffects().clearTemporaryEffects();
        
        processEndOfTurnPassives(true);
        processEndOfTurnPassives(false);
        
        if (weatherController.shouldApplyFineSnowEffect(state, true)) {
            state.getPlayerEffects().addEffect(StatusEffectProcessor.StatusEffect.TOUGHNESS, 3);
        }
        if (weatherController.shouldApplyFineSnowEffect(state, false)) {
            state.getOpponentEffects().addEffect(StatusEffectProcessor.StatusEffect.TOUGHNESS, 3);
        }
        
        PassiveEventSystem.onEndOfDefenseTurn(state, true);
        PassiveEventSystem.onEndOfDefenseTurn(state, false);
        
        state.incrementTurnNumber();
        state.swapAttackerDefender();
        
        CosmiconLogger.debug("Turn transition: Turn %d, Player now %s", 
            state.getTurnNumber(), state.isPlayerAttacker() ? "attacker" : "defender");
        
        int playerHpLost = state.getPlayerCard().getMaxHp() - state.getPlayerHp();
        int opponentHpLost = state.getOpponentCard().getMaxHp() - state.getOpponentHp();
        boolean attackerIsPlayer = state.isPlayerAttacker();
        int attackerHpLost = attackerIsPlayer ? playerHpLost : opponentHpLost;
        int defenderHpLost = attackerIsPlayer ? opponentHpLost : playerHpLost;
        boolean allowSafeguard = defenderHpLost >= attackerHpLost;
        boolean allowAttack = attackerHpLost >= defenderHpLost;
        
        WeatherType oldWeather = weatherController.getCurrentWeather();
        weatherController.advanceTurn(allowSafeguard, allowAttack);
        WeatherType newWeather = weatherController.getCurrentWeather();
        
        state.clearWeatherMods();
        if (newWeather != null) {
            if (newWeather != oldWeather) {
                state.notifyWeatherChange(newWeather);
                weatherController.applyWeatherAppearanceEffects(state);
            }
            weatherController.applyPersistentWeatherMods(state);
        }
        
        state.clearDiceSelection(true);
        state.clearDiceSelection(false);
        executeTurn();
    }
    
    private void endBattle() {
        state.setCurrentPhase(TurnState.Phase.ENDED);
        String winner = state.getPlayerHp() <= 0 ? "opponent" : "player";
        state.setWinner(winner);
        state.notifyBattleEnd(winner);
    }
    
    public void executePlayerReroll() {
        if (!state.canReroll(true)) return;
        
        int selectedCount = state.countSelectedDice(true);
        if (selectedCount == 0) return;
        
        diceRoller.rerollSelected(state, true);
        
        StatusEffectProcessor.BattleContext playerContext = createBattleContext(true);
        TurnType playerTurnType = state.isPlayerAttacker() ? TurnType.ATTACK : TurnType.DEFENSE;
        state.getPlayerEffects().processPhase(Phase.AFTER_ROLL, playerTurnType, playerContext);
        state.setDiceValues(true, playerContext.getDiceValues());
    }
    
    public void confirmPlayerSelection() {
        if (state.getCurrentPhase() != TurnState.Phase.SELECTING_ATTACK && 
            state.getCurrentPhase() != TurnState.Phase.SELECTING_DEFENSE) return;
        
        int requiredCount = state.getRequiredDiceCount(true);
        int selectedCount = state.countSelectedDice(true);
        
        if (selectedCount != requiredCount) {
            CosmiconLogger.debug("Cannot confirm: need %d dice selected, have %d", requiredCount, selectedCount);
            return;
        }
        
        if (!state.canConfirmPrismaticSelection(true)) return;
        
        state.recordSelectedFaces(true);
        
        boolean isAttackPhase = state.getCurrentPhase() == TurnState.Phase.SELECTING_ATTACK;
        
        if (isAttackPhase) {
            state.setAttackValue(state.calculateSelectedSum(true));
        } else {
            state.setDefenseValue(state.calculateSelectedSum(true));
        }
        
        applyPostSelectionProcessing(true);
        
        if (state.isPlayerAttacker() && state.getCurrentPhase() == TurnState.Phase.SELECTING_ATTACK) {
            state.setAttackerConfirmedSelection(state.getSelectedDiceValuesFormatted(true));
            advanceToDiceDisplayAttack();
        } else if (!state.isPlayerAttacker() && state.getCurrentPhase() == TurnState.Phase.SELECTING_DEFENSE) {
            state.setDefenderConfirmedSelection(state.getSelectedDiceValuesFormatted(true));
            weatherController.applyDefenderSelectionPhase(state, true);
            advanceToDiceDisplayDefense();
        }
    }
    
    private void applyPostSelectionProcessing(boolean forPlayer) {
        processPassiveEffects(forPlayer);
        
        boolean isAttackPhase = state.getCurrentPhase() == TurnState.Phase.SELECTING_ATTACK;
        boolean isAttackerSide = forPlayer == state.isPlayerAttacker();
        
        int passiveBonus;
        if (isAttackPhase) {
            passiveBonus = state.getAttackValue() - state.calculateSelectedSum(forPlayer);
        } else {
            passiveBonus = state.getDefenseValue() - state.calculateSelectedSum(forPlayer);
        }
        
        StatusEffectProcessor.BattleContext context = createBattleContext(forPlayer);
        TurnType turnType = isAttackerSide ? TurnType.ATTACK : TurnType.DEFENSE;
        
        int oldValue = isAttackPhase ? state.getAttackValue() : state.getDefenseValue();
        List<Integer> preSelectValues = new ArrayList<>(state.getDiceValues(forPlayer));
        List<DiceType> preSelectTypes = state.getDiceTypes(forPlayer) != null ? new ArrayList<>(state.getDiceTypes(forPlayer)) : null;
        state.getEffects(forPlayer).processPhase(Phase.AFTER_SELECT, turnType, context);
        state.setDiceValues(forPlayer, context.getDiceValues());
        state.setDiceTypes(forPlayer, context.getDiceTypes());
        updateUpgradedDicePool(forPlayer);
        List<Integer> postSelectValues = state.getDiceValues(forPlayer);
        notifyRestDiceValueChanges(preSelectValues, postSelectValues, forPlayer);
        notifyRestDiceTypeChanges(preSelectTypes, state.getDiceTypes(forPlayer), forPlayer);
        
        notifyLevelUpValueChange(forPlayer, isAttackPhase, oldValue);
        
        if (isAttackPhase) {
            state.setAttackValue(state.calculateSelectedSum(forPlayer) + passiveBonus);
        } else {
            state.setDefenseValue(state.calculateSelectedSum(forPlayer) + passiveBonus);
        }
        
        applyWeatherAndNotifyValueChanges(forPlayer);
    }
    
    private StatusEffectProcessor.BattleContext createBattleContext(boolean forPlayer) {
        return state.createBattleContext(forPlayer);
    }
    
    private void notifyLevelUpValueChange(boolean forPlayer, boolean isAttackPhase, int oldValue) {
        int newValue = isAttackPhase ? state.getAttackValue() : state.getDefenseValue();
        if (newValue != oldValue) {
            int delta = newValue - oldValue;
            String changeType = isAttackPhase ? "ATTACK_LEVEL_UP" : "DEFENSE_LEVEL_UP";
            state.queueValueChange(forPlayer, changeType, delta);
            state.notifyValueChange(forPlayer, "LEVEL_UP", oldValue, newValue, delta);
        }
    }
    
    private void applyWeatherAndNotifyValueChanges(boolean forPlayer) {
        int oldAtk = state.getAttackValue();
        int oldDef = state.getDefenseValue();
        weatherController.applySelectionPhase(state, forPlayer);
        int newAtk = state.getAttackValue();
        int newDef = state.getDefenseValue();
        if (newAtk != oldAtk) {
            int delta = newAtk - oldAtk;
            state.queueValueChange(forPlayer, "WEATHER", delta);
            state.notifyValueChange(forPlayer, "WEATHER", oldAtk, newAtk, delta);
        }
        if (newDef != oldDef) {
            int delta = newDef - oldDef;
            state.queueValueChange(forPlayer, "WEATHER", delta);
            state.notifyValueChange(forPlayer, "WEATHER", oldDef, newDef, delta);
        }
    }
    
    private void processPassiveEffects(boolean forPlayer) {
        CharacterCard card = state.getCard(forPlayer);
        if (card == null) return;
        
        String characterId = card.getId();
        List<Integer> allValues = state.getDiceValues(forPlayer);
        List<Boolean> selectedFlags = state.getDiceSelected(forPlayer);
        
        List<Integer> selectedValues = new ArrayList<>();
        if (allValues != null && selectedFlags != null) {
            for (int i = 0; i < allValues.size(); i++) {
                if (selectedFlags.get(i)) {
                    selectedValues.add(allValues.get(i));
                }
            }
        }
        
        boolean isAttacking = state.isAttacker(forPlayer);
        
        int currentHp = forPlayer ? state.getPlayerHp() : state.getOpponentHp();
        int maxHp = forPlayer ? 
            (state.getPlayerCard() != null ? state.getPlayerCard().getMaxHp() : currentHp) :
            (state.getOpponentCard() != null ? state.getOpponentCard().getMaxHp() : currentHp);
        
        StatusEffectProcessor effects = state.getEffects(forPlayer);
        int currentToughness = effects.getLayers(StatusEffect.TOUGHNESS);
        int currentStrengthLayers = effects.getLayers(StatusEffect.STRENGTH);
        
        PassiveResult result = PassiveEvaluator.evaluateForCharacter(
            characterId, selectedValues, isAttacking, currentHp, maxHp, currentToughness, currentStrengthLayers);
        
        PassiveEvaluator.applyPassiveEffects(result, state, forPlayer);
        
        if (result.getAttackBonus() > 0) {
            int bonus = result.getAttackBonus();
            int currentAttack = state.getAttackValue();
            if (isAttacking) {
                state.setAttackValue(currentAttack + bonus);
                state.queueValueChange(forPlayer, "PASSIVE", bonus);
                state.notifyValueChange(forPlayer, "PASSIVE", currentAttack, currentAttack + bonus, bonus);
            }
        }
    }
    
    private void processEndOfTurnPassives(boolean forPlayer) {
        PassiveEventSystem.onEndOfTurn(state, forPlayer);
    }
    
    private void applyPendingStrength(boolean forPlayer) {
        int pending = state.consumePendingStrength(forPlayer);
        if (pending > 0) {
            state.getEffects(forPlayer).setEffect(StatusEffectProcessor.StatusEffect.STRENGTH, pending);
        }
    }
    
    private void notifyRestDiceValueChanges(List<Integer> oldValues, List<Integer> newValues, boolean forPlayer) {
        if (diceRollManager == null || oldValues == null || newValues == null) return;
        if (!diceRollManager.hasRestAnimators(forPlayer)) return;
        
        int minSize = Math.min(oldValues.size(), newValues.size());
        for (int i = 0; i < minSize; i++) {
            if (!oldValues.get(i).equals(newValues.get(i))) {
                diceRollManager.updateRestDiceValue(i, newValues.get(i), forPlayer);
            }
        }
    }

    private void notifyRestDiceTypeChanges(List<DiceType> oldTypes, List<DiceType> newTypes, boolean forPlayer) {
        if (diceRollManager == null || oldTypes == null || newTypes == null) return;
        if (!diceRollManager.hasRestAnimators(forPlayer)) return;

        int minSize = Math.min(oldTypes.size(), newTypes.size());
        for (int i = 0; i < minSize; i++) {
            if (oldTypes.get(i) != newTypes.get(i)) {
                diceRollManager.updateRestDiceType(i, newTypes.get(i), forPlayer);
            }
        }
    }

    private void updateUpgradedDicePool(boolean forPlayer) {
        List<DiceType> currentTypes = state.getDiceTypes(forPlayer);
        if (currentTypes == null) return;

        List<DiceType> basePool = state.getCard(forPlayer).getDicePool();
        boolean hasUpgrade = false;
        for (int i = 0; i < currentTypes.size(); i++) {
            if (i < basePool.size() && currentTypes.get(i) != basePool.get(i)) {
                hasUpgrade = true;
                break;
            }
        }

        if (hasUpgrade) {
            state.setUpgradedDicePool(forPlayer, new ArrayList<>(currentTypes));
        }
    }
}
