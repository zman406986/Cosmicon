package data.scripts.cosmicon.battle;

import data.scripts.CosmiconConfig;
import data.scripts.cosmicon.util.CosmiconLogger;
import data.scripts.cosmicon.battle.StatusEffectProcessor.Phase;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.battle.BattleState.TurnType;
import data.scripts.cosmicon.character.PassiveEventSystem;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveEvaluator.PassiveResult;
import java.util.ArrayList;
import java.util.List;

public class TurnProcessor {
    
    private final BattleState state;
    private AIEngine aiEngine;
    private WeatherController weatherController;
    private DiceRoller diceRoller;
    private DamageResolver damageResolver;
    
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
    
    public TurnProcessor(BattleState state) {
        this.state = state;
        this.aiSelectDelay = 0f;
        this.aiSelectionComplete = false;
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
    
    public void setDamageResolver(DamageResolver resolver) {
        this.damageResolver = resolver;
    }
    
    public void startBattle() {
        if (weatherController != null) {
            weatherController.applyStartOfBattle(state);
        }
        executeTurn();
    }
    
    public void executeTurn() {
        state.getPlayerEffects().resetTurnState();
        state.getOpponentEffects().resetTurnState();
        
        if (weatherController != null) {
            weatherController.applyStartOfTurn(state);
        }
        
        StatusEffectProcessor.BattleContext playerContext = createBattleContext(true);
        StatusEffectProcessor.BattleContext opponentContext = createBattleContext(false);
        
state.getPlayerEffects().processPhase(Phase.START_OF_TURN, 
            state.isPlayerAttacker() ? TurnType.ATTACK : TurnType.DEFENSE, playerContext);
        state.getOpponentEffects().processPhase(Phase.START_OF_TURN,
            state.isPlayerAttacker() ? TurnType.DEFENSE : TurnType.ATTACK, opponentContext);
        
        state.setPlayerHp(playerContext.getCurrentHp());
        state.setOpponentHp(opponentContext.getCurrentHp());
        
        state.resetRerollsUsedThisTurn();
        
        int baseRerolls = state.isPlayerAttacker() ? CosmiconConfig.DEFAULT_REROLLS : 0;
        int opponentBaseRerolls = state.isPlayerAttacker() ? 0 : CosmiconConfig.DEFAULT_REROLLS;
        
        if (weatherController != null) {
            weatherController.applyRerollPhase(state, state.isPlayerAttacker() ? baseRerolls : opponentBaseRerolls);
        }
        
        if (state.isPlayerAttacker()) {
            state.setRemainingRerolls(true, baseRerolls);
            state.setRemainingRerolls(false, 0);
        } else {
            state.setRemainingRerolls(true, 0);
            state.setRemainingRerolls(false, opponentBaseRerolls);
        }
        
        TurnType playerTurnType = state.isPlayerAttacker() ? TurnType.ATTACK : TurnType.DEFENSE;
        TurnType opponentTurnType = state.isPlayerAttacker() ? TurnType.DEFENSE : TurnType.ATTACK;
        
        PassiveEventSystem.onStartOfAttackTurn(state, true);
        PassiveEventSystem.onStartOfAttackTurn(state, false);
        
        state.getPlayerEffects().processPhase(Phase.BEFORE_ROLL, playerTurnType, playerContext);
        state.getOpponentEffects().processPhase(Phase.BEFORE_ROLL, opponentTurnType, opponentContext);
        
        state.getPlayerEffects().removeEffect(StatusEffectProcessor.StatusEffect.YAO_GUANG_REROLLS);
        state.getOpponentEffects().removeEffect(StatusEffectProcessor.StatusEffect.YAO_GUANG_REROLLS);
        
        int playerRerollBonus = playerContext.getRerollCount();
        int opponentRerollBonus = opponentContext.getRerollCount();
        if (playerRerollBonus != 0) {
            state.setRemainingRerolls(true, state.getRemainingRerolls(true) + playerRerollBonus);
        }
        if (opponentRerollBonus != 0) {
            state.setRemainingRerolls(false, state.getRemainingRerolls(false) + opponentRerollBonus);
        }
        
        state.setCurrentPhase(BattleState.Phase.ROLLING);
        
        if (diceRoller != null) {
            diceRoller.rollForAttacker(state);
        }
        
        state.notifyPhaseChange(BattleState.Phase.ROLLING);
        
        boolean attackerIsPlayer = state.isPlayerAttacker();
        StatusEffectProcessor.BattleContext attackerContext = createBattleContext(attackerIsPlayer);
        TurnType attackerTurnType = TurnType.ATTACK;
        state.getEffects(attackerIsPlayer).processPhase(Phase.AFTER_ROLL, attackerTurnType, attackerContext);
        state.setDiceValues(attackerIsPlayer, attackerContext.getDiceValues());
        
        state.clearPrismaticState();
    }
    
    public void advanceToSelectPhase() {
        if (state.getCurrentPhase() != BattleState.Phase.ROLLING) return;
        
        state.clearDiceSelection(true);
        state.clearDiceSelection(false);
        
        state.setCurrentPhase(BattleState.Phase.SELECTING_ATTACK);
        state.notifyPhaseChange(BattleState.Phase.SELECTING_ATTACK);
        
        if (!state.isPlayerAttacker()) {
            startAiSelection();
        }
    }
    
    private void executeAiAttackerReroll() {
        if (aiEngine == null || state.getRemainingRerolls(false) <= 0) return;
        
        List<Integer> rerollIndices = aiEngine.planReroll(state, false);
        
        if (!rerollIndices.isEmpty()) {
            aiPlannedIndices = rerollIndices;
            aiVisualPhase = AIVisualPhase.REROLL_PLANNING;
            aiPhaseTimer = 0f;
            
            AISelectionVisualizer viz = state.getAiSelectionVisualizer();
            if (viz != null) {
                viz.planSelection(rerollIndices, true);
            }
        } else {
            advanceToAttackPhase();
        }
    }
    
    public void advanceToAttackPhase() {
        if (state.getCurrentPhase() != BattleState.Phase.ROLLING) return;
        
        state.clearDiceSelection(true);
        state.clearDiceSelection(false);
        
        state.setCurrentPhase(BattleState.Phase.SELECTING_ATTACK);
        state.notifyPhaseChange(BattleState.Phase.SELECTING_ATTACK);
        
        if (!state.isPlayerAttacker()) {
            startAiSelection();
        }
    }
    
    private void advanceToDefensePhase() {
        state.setDefenderRolling(true);
        
        if (diceRoller != null) {
            diceRoller.rollForDefender(state);
        }
        
        boolean defenderIsPlayer = !state.isPlayerAttacker();
        StatusEffectProcessor.BattleContext defenderContext = createBattleContext(defenderIsPlayer);
        TurnType defenderTurnType = TurnType.DEFENSE;
        state.getEffects(defenderIsPlayer).processPhase(Phase.AFTER_ROLL, defenderTurnType, defenderContext);
        state.setDiceValues(defenderIsPlayer, defenderContext.getDiceValues());
        
        state.setCurrentPhase(BattleState.Phase.ROLLING);
        state.notifyPhaseChange(BattleState.Phase.ROLLING);
    }
    
    public void advanceToDefenderSelectPhase() {
        if (!state.isDefenderRolling()) return;
        state.setDefenderRolling(false);
        
        state.clearDiceSelection(true);
        state.clearDiceSelection(false);
        
        state.setCurrentPhase(BattleState.Phase.SELECTING_DEFENSE);
        state.notifyPhaseChange(BattleState.Phase.SELECTING_DEFENSE);
        
        if (state.isPlayerAttacker()) {
            startAiSelection();
        }
    }
    
    private void executeAiDefenderReroll() {
        if (aiEngine == null || state.getRemainingRerolls(false) <= 0) return;
        
        List<Integer> rerollIndices = aiEngine.planReroll(state, false);
        
        if (!rerollIndices.isEmpty()) {
            aiPlannedIndices = rerollIndices;
            aiVisualPhase = AIVisualPhase.REROLL_PLANNING;
            aiPhaseTimer = 0f;
            
            AISelectionVisualizer viz = state.getAiSelectionVisualizer();
            if (viz != null) {
                viz.planSelection(rerollIndices, true);
            }
        } else {
            state.setCurrentPhase(BattleState.Phase.SELECTING_DEFENSE);
            state.notifyPhaseChange(BattleState.Phase.SELECTING_DEFENSE);
            
            if (state.isPlayerAttacker()) {
                startAiSelection();
            }
        }
    }
    
    private void startAiSelection() {
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
                if (aiPhaseTimer <= 0f) {
                    aiVisualPhase = AIVisualPhase.NONE;
                    if (viz != null) viz.reset();
                    
                    boolean attackerIsPlayer = state.isPlayerAttacker();
                    if (!attackerIsPlayer) {
                        advanceToAttackPhase();
                    } else {
                        state.setCurrentPhase(BattleState.Phase.SELECTING_DEFENSE);
                        state.notifyPhaseChange(BattleState.Phase.SELECTING_DEFENSE);
                        startAiSelection();
                    }
                }
            }
            
            case SELECTION_PLANNING -> {
                boolean isAttackPhase = state.getCurrentPhase() == BattleState.Phase.SELECTING_ATTACK;
                boolean aiIsAttacker = !state.isPlayerAttacker();
                
                List<Integer> selectIndices = aiEngine.planSelection(state, isAttackPhase != aiIsAttacker);
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
            for (int idx : aiPlannedIndices) {
                if (idx < selected.size()) {
                    selected.set(idx, true);
                }
            }
        }
        
        DiceRoller roller = state.getDiceRoller();
        if (roller != null) {
            roller.rerollSelected(state, false);
        }
        
        CosmiconLogger.debug("AI executed planned reroll for indices: %s", aiPlannedIndices);
    }
    
    private void executePlannedAiSelection() {
        if (aiEngine == null || aiPlannedIndices == null) return;
        
        boolean isAttackPhase = state.getCurrentPhase() == BattleState.Phase.SELECTING_ATTACK;
        boolean aiIsAttacker = !state.isPlayerAttacker();
        boolean forPlayer = isAttackPhase != aiIsAttacker;
        
        List<Boolean> selected = state.getDiceSelected(forPlayer);
        if (selected != null) {
            for (int idx : aiPlannedIndices) {
                if (idx < selected.size()) {
                    selected.set(idx, true);
                }
                state.recordFaceSelection(state.getDiceValues(forPlayer).get(idx), forPlayer);
            }
        }
        
        processPassiveEffects(forPlayer);
        
        StatusEffectProcessor.BattleContext context = createBattleContext(forPlayer);
        TurnType turnType = forPlayer ? 
            (state.isPlayerAttacker() ? TurnType.ATTACK : TurnType.DEFENSE) :
            (state.isPlayerAttacker() ? TurnType.DEFENSE : TurnType.ATTACK);
        state.getEffects(forPlayer).processPhase(Phase.AFTER_SELECT, turnType, context);
        state.setDiceValues(forPlayer, context.getDiceValues());
        
        if (weatherController != null) {
            weatherController.applySelectionPhase(state, forPlayer);
        }
        
        if (aiIsAttacker && state.getCurrentPhase() == BattleState.Phase.SELECTING_ATTACK) {
            state.setAttackValue(state.calculateSelectedSum(false));
            advanceToDefensePhase();
        } else if (!aiIsAttacker && state.getCurrentPhase() == BattleState.Phase.SELECTING_DEFENSE) {
            state.setDefenseValue(state.calculateSelectedSum(false));
            state.setCurrentPhase(BattleState.Phase.RESOLVING);
            state.notifyPhaseChange(BattleState.Phase.RESOLVING);
            resolveDamage();
        }
        
        CosmiconLogger.debug("AI executed planned selection for indices: %s", aiPlannedIndices);
    }
    
    private void performAiSelection() {
        if (aiEngine == null) return;
        
        boolean isAttackPhase = state.getCurrentPhase() == BattleState.Phase.SELECTING_ATTACK;
        boolean aiIsAttacker = !state.isPlayerAttacker();
        
        aiEngine.executeSelection(state, isAttackPhase != aiIsAttacker);
        
        processPassiveEffects(false);
        
        StatusEffectProcessor.BattleContext opponentContext = createBattleContext(false);
        TurnType opponentTurnType = state.isPlayerAttacker() ? TurnType.DEFENSE : TurnType.ATTACK;
        state.getOpponentEffects().processPhase(Phase.AFTER_SELECT, opponentTurnType, opponentContext);
        state.setDiceValues(false, opponentContext.getDiceValues());
        
        if (weatherController != null) {
            boolean isPlayer = state.getCurrentPhase() == BattleState.Phase.SELECTING_DEFENSE;
            weatherController.applySelectionPhase(state, isPlayer);
        }
        
        aiSelectionComplete = true;
        
        if (aiIsAttacker && state.getCurrentPhase() == BattleState.Phase.SELECTING_ATTACK) {
            state.setAttackValue(state.calculateSelectedSum(false));
            advanceToDefensePhase();
        } else if (!aiIsAttacker && state.getCurrentPhase() == BattleState.Phase.SELECTING_DEFENSE) {
            state.setDefenseValue(state.calculateSelectedSum(false));
            state.setCurrentPhase(BattleState.Phase.RESOLVING);
            state.notifyPhaseChange(BattleState.Phase.RESOLVING);
            resolveDamage();
        }
    }
    
    private void resolveDamage() {
        if (weatherController != null) {
            weatherController.applyPreResolution(state);
        }

        state.applyPrismaticDiceEffects();

        StatusEffectProcessor.BattleContext attackerContext = createBattleContext(state.isPlayerAttacker());
        StatusEffectProcessor.BattleContext defenderContext = createBattleContext(!state.isPlayerAttacker());
        
        TurnType attackerTurnType = TurnType.ATTACK;
        TurnType defenderTurnType = TurnType.DEFENSE;
        
        state.getPlayerEffects().processPhase(Phase.BEFORE_RESOLUTION, 
            state.isPlayerAttacker() ? attackerTurnType : defenderTurnType, 
            state.isPlayerAttacker() ? attackerContext : defenderContext);
        state.getOpponentEffects().processPhase(Phase.BEFORE_RESOLUTION,
            state.isPlayerAttacker() ? defenderTurnType : attackerTurnType,
            state.isPlayerAttacker() ? defenderContext : attackerContext);
        
        int attackerInstantDamage = attackerContext.getInstantDamageToOpponent();
        int defenderInstantDamage = defenderContext.getInstantDamageToOpponent();
        
        if (attackerInstantDamage > 0) {
            if (state.isPlayerAttacker()) {
                state.setOpponentHp(Math.max(0, state.getOpponentHp() - attackerInstantDamage));
            } else {
                state.setPlayerHp(Math.max(0, state.getPlayerHp() - attackerInstantDamage));
            }
        }
        
        if (defenderInstantDamage > 0) {
            if (state.isPlayerAttacker()) {
                state.setPlayerHp(Math.max(0, state.getPlayerHp() - defenderInstantDamage));
            } else {
                state.setOpponentHp(Math.max(0, state.getOpponentHp() - defenderInstantDamage));
            }
        }

        PassiveEventSystem.onAttackResolution(state, state.isPlayerAttacker());
        PassiveEventSystem.onAttackResolution(state, !state.isPlayerAttacker());

        if (damageResolver != null) {
            DamageResolver.DamageResult result = damageResolver.resolve(state);
            applyDamageResult(result);
        }
    }
    
    private void applyDamageResult(DamageResolver.DamageResult result) {
        int damage = result.damageToDefender();
        boolean playerIsAttacker = state.isPlayerAttacker();
        boolean defenderIsPlayer = !playerIsAttacker;
        
        if (damage > 0) {
            if (playerIsAttacker) {
                state.applyDamageTo(false, damage);
                
                if (result.siphonHeal() > 0) {
                    state.applyHealTo(true, result.siphonHeal());
                }
            } else {
                state.applyDamageTo(true, damage);
                
                if (result.siphonHeal() > 0) {
                    state.applyHealTo(false, result.siphonHeal());
                }
            }
            
            int instantDamageToAttacker = PassiveEventSystem.onDamageTaken(state, defenderIsPlayer, damage);
            if (instantDamageToAttacker > 0) {
                state.applyDamageTo(playerIsAttacker, instantDamageToAttacker);
            }
        }
        
        if (result.thornsDamage() > 0) {
            state.applyDamageTo(playerIsAttacker, result.thornsDamage());
        }
        
        if (result.counterDamage() > 0) {
            state.applyDamageTo(playerIsAttacker, result.counterDamage());
        }
        
        if (result.selfThornsDamage() > 0) {
            state.applyDamageTo(playerIsAttacker, result.selfThornsDamage());
            StatusEffectProcessor attackerEffects = playerIsAttacker ? state.getPlayerEffects() : state.getOpponentEffects();
            attackerEffects.removeEffect(StatusEffectProcessor.StatusEffect.THORNS);
        }
        
        if (result.overloadSelfDamage() > 0) {
            state.applyDamageTo(playerIsAttacker, result.overloadSelfDamage());
        }
        
        if (result.instantDamage() > 0) {
            state.applyDamageTo(defenderIsPlayer, result.instantDamage());
        }
        
        state.notifyDamageResolved(damage, state.getPlayerHp(), state.getOpponentHp());
        
        if (state.getPlayerHp() <= 0 || state.getOpponentHp() <= 0) {
            endBattle();
        } else {
            state.setCurrentPhase(BattleState.Phase.WAITING_NEXT_TURN);
            state.notifyPhaseChange(BattleState.Phase.WAITING_NEXT_TURN);
        }
    }
    
    public void advanceToNextTurn() {
        if (state.getCurrentPhase() != BattleState.Phase.WAITING_NEXT_TURN) return;
        
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
        
        if (state.getPlayerHp() <= 0 || state.getOpponentHp() <= 0) {
            endBattle();
            return;
        }
        
        state.getPlayerEffects().clearTemporaryEffects();
        state.getOpponentEffects().clearTemporaryEffects();
        
        processEndOfTurnPassives(true);
        processEndOfTurnPassives(false);
        
        if (weatherController != null) {
            if (weatherController.shouldApplyFineSnowEffect(state, true)) {
                state.getPlayerEffects().addEffect(StatusEffectProcessor.StatusEffect.TOUGHNESS, 3);
            }
            if (weatherController.shouldApplyFineSnowEffect(state, false)) {
                state.getOpponentEffects().addEffect(StatusEffectProcessor.StatusEffect.TOUGHNESS, 3);
            }
        }
        
        state.incrementTurnNumber();
        state.swapAttackerDefender();
        
        CosmiconLogger.debug("Turn transition: Turn %d, Player now %s", 
            state.getTurnNumber(), state.isPlayerAttacker() ? "attacker" : "defender");
        
        if (weatherController != null) {
            WeatherType oldWeather = weatherController.getCurrentWeather();
            weatherController.advanceTurn();
            WeatherType newWeather = weatherController.getCurrentWeather();
            
            if (newWeather != oldWeather && newWeather != null) {
                state.notifyWeatherChange(newWeather);
                weatherController.applyWeatherTransitionEffect(state, oldWeather, newWeather);
            }
        }
        
        state.clearDiceSelection(true);
        state.clearDiceSelection(false);
        executeTurn();
    }
    
    private void endBattle() {
        state.setCurrentPhase(BattleState.Phase.ENDED);
        String winner = state.getPlayerHp() <= 0 ? "opponent" : "player";
        state.setWinner(winner);
        state.notifyBattleEnd(winner);
    }
    
    public void executePlayerReroll() {
        if (diceRoller == null) return;
        if (!state.canReroll(true)) return;
        
        int selectedCount = state.countSelectedDice(true);
        if (selectedCount == 0) return;
        
        diceRoller.rerollSelected(state, true);
        state.clearDiceSelection(true);
        
        StatusEffectProcessor.BattleContext playerContext = createBattleContext(true);
        TurnType playerTurnType = state.isPlayerAttacker() ? TurnType.ATTACK : TurnType.DEFENSE;
        state.getPlayerEffects().processPhase(Phase.AFTER_ROLL, playerTurnType, playerContext);
        state.setDiceValues(true, playerContext.getDiceValues());
    }
    
    public void confirmPlayerSelection() {
        if (state.getCurrentPhase() != BattleState.Phase.SELECTING_ATTACK && 
            state.getCurrentPhase() != BattleState.Phase.SELECTING_DEFENSE) return;
        
        int requiredCount = state.getRequiredDiceCount(true);
        int selectedCount = state.countSelectedDice(true);
        
        if (selectedCount != requiredCount) {
            CosmiconLogger.debug("Cannot confirm: need %d dice selected, have %d", requiredCount, selectedCount);
            return;
        }
        
        if (!state.canConfirmPrismaticSelection(true)) return;
        
        state.recordSelectedFaces(true);
        
        processPassiveEffects(true);
        
        StatusEffectProcessor.BattleContext playerContext = createBattleContext(true);
        TurnType playerTurnType = state.isPlayerAttacker() ? TurnType.ATTACK : TurnType.DEFENSE;
        state.getPlayerEffects().processPhase(Phase.AFTER_SELECT, playerTurnType, playerContext);
        state.setDiceValues(true, playerContext.getDiceValues());
        
        if (weatherController != null) {
            weatherController.applySelectionPhase(state, true);
        }
        
        if (state.isPlayerAttacker() && state.getCurrentPhase() == BattleState.Phase.SELECTING_ATTACK) {
            state.setAttackValue(state.calculateSelectedSum(true));
            advanceToDefensePhase();
        } else if (!state.isPlayerAttacker() && state.getCurrentPhase() == BattleState.Phase.SELECTING_DEFENSE) {
            state.setDefenseValue(state.calculateSelectedSum(true));
            state.setCurrentPhase(BattleState.Phase.RESOLVING);
            state.notifyPhaseChange(BattleState.Phase.RESOLVING);
            resolveDamage();
        }
    }
    
    private StatusEffectProcessor.BattleContext createBattleContext(boolean forPlayer) {
        return state.createBattleContext(forPlayer);
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
        
        StatusEffectProcessor effects = (forPlayer ? state.getPlayerEffects() : state.getOpponentEffects());
        int currentToughness = effects.getLayers(StatusEffect.TOUGHNESS);
        
        PassiveResult result = PassiveEvaluator.evaluateForCharacter(
            characterId, selectedValues, isAttacking, currentHp, maxHp, currentToughness);
        
        PassiveEvaluator.applyPassiveEffects(result, state, forPlayer);
        
        if (result.getAttackBonus() > 0) {
            int bonus = result.getAttackBonus();
            int currentAttack = state.getAttackValue();
            if (isAttacking) {
                state.setAttackValue(currentAttack + bonus);
            }
        }
    }
    
    private void processEndOfTurnPassives(boolean forPlayer) {
        PassiveEventSystem.onEndOfTurn(state, forPlayer);
    }
}
