package data.scripts.cosmicon.battle;

import data.scripts.CosmiconConfig;
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
        
        state.setRemainingRerolls(true, playerContext.getRerollCount());
        state.setRemainingRerolls(false, opponentContext.getRerollCount());
        
        state.setCurrentPhase(BattleState.Phase.ROLLING);
        state.notifyPhaseChange(BattleState.Phase.ROLLING);
        
        if (diceRoller != null) {
            diceRoller.rollAll(state);
        }
        
        state.getPlayerEffects().processPhase(Phase.AFTER_ROLL, playerTurnType, playerContext);
        state.getOpponentEffects().processPhase(Phase.AFTER_ROLL, opponentTurnType, opponentContext);
        
        state.setDiceValues(true, playerContext.getDiceValues());
        state.setDiceValues(false, opponentContext.getDiceValues());
        
        state.clearPrismaticState();
    }
    
    public void advanceToSelectPhase() {
        if (state.getCurrentPhase() != BattleState.Phase.ROLLING) return;
        
        boolean attackerHasRerolls = state.isPlayerAttacker() ? 
            state.getRemainingRerolls(true) > 0 : 
            state.getRemainingRerolls(false) > 0;
        
        boolean defenderHasRerolls = state.isPlayerAttacker() ? 
            state.getRemainingRerolls(false) > 0 : 
            state.getRemainingRerolls(true) > 0;
        
        if (attackerHasRerolls || defenderHasRerolls) {
            state.setCurrentPhase(BattleState.Phase.REROLL_PHASE);
            state.notifyPhaseChange(BattleState.Phase.REROLL_PHASE);
            
            if (!state.isPlayerAttacker()) {
                executeOpponentReroll();
            }
        } else {
            advanceToAttackPhase();
        }
    }
    
    private void executeOpponentReroll() {
        if (aiEngine != null && state.getRemainingRerolls(false) > 0) {
            aiEngine.executeReroll(state, false);
        }
    }
    
    public void advanceToAttackPhase() {
        if (state.getCurrentPhase() != BattleState.Phase.REROLL_PHASE && 
            state.getCurrentPhase() != BattleState.Phase.ROLLING) return;
        
        state.clearDiceSelection(true);
        state.clearDiceSelection(false);
        
        state.setCurrentPhase(BattleState.Phase.SELECTING_ATTACK);
        state.notifyPhaseChange(BattleState.Phase.SELECTING_ATTACK);
        
        if (!state.isPlayerAttacker()) {
            startAiSelection();
        }
    }
    
    private void advanceToDefensePhase() {
        state.setCurrentPhase(BattleState.Phase.SELECTING_DEFENSE);
        state.notifyPhaseChange(BattleState.Phase.SELECTING_DEFENSE);
        
        if (state.isPlayerAttacker()) {
            startAiSelection();
        }
    }
    
    private void startAiSelection() {
        aiSelectDelay = 0.8f;
        aiSelectionComplete = false;
    }
    
    public void advanceAiSelection(float amount) {
        if (!aiSelectionComplete && aiSelectDelay > 0f) {
            aiSelectDelay -= amount;
            if (aiSelectDelay <= 0f) {
                performAiSelection();
            }
        }
    }
    
    private void performAiSelection() {
        if (aiEngine == null) return;
        
        boolean isAttackPhase = state.getCurrentPhase() == BattleState.Phase.SELECTING_ATTACK;
        boolean aiIsAttacker = !state.isPlayerAttacker();
        
        if (isAttackPhase && aiIsAttacker) {
            aiEngine.executeSelection(state, false);
        } else if (!isAttackPhase && !aiIsAttacker) {
            aiEngine.executeSelection(state, false);
        } else if (isAttackPhase && !aiIsAttacker) {
            aiEngine.executeSelection(state, true);
        } else {
            aiEngine.executeSelection(state, true);
        }
        
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
        
        if (state.isPlayerAttacker() && state.getCurrentPhase() == BattleState.Phase.SELECTING_ATTACK) {
            state.setDefenseValue(state.calculateSelectedSum(false));
            if (weatherController != null) {
                weatherController.applyDefenderSelectionPhase(state);
            }
            state.setCurrentPhase(BattleState.Phase.RESOLVING);
            state.notifyPhaseChange(BattleState.Phase.RESOLVING);
            resolveDamage();
        } else if (!state.isPlayerAttacker() && state.getCurrentPhase() == BattleState.Phase.SELECTING_ATTACK) {
            state.setAttackValue(state.calculateSelectedSum(false));
            state.setCurrentPhase(BattleState.Phase.SELECTING_DEFENSE);
            state.notifyPhaseChange(BattleState.Phase.SELECTING_DEFENSE);
        } else if (!state.isPlayerAttacker() && state.getCurrentPhase() == BattleState.Phase.SELECTING_DEFENSE) {
            state.setDefenseValue(state.calculateSelectedSum(true));
            state.setCurrentPhase(BattleState.Phase.RESOLVING);
            state.notifyPhaseChange(BattleState.Phase.RESOLVING);
            resolveDamage();
        } else if (state.isPlayerAttacker() && state.getCurrentPhase() == BattleState.Phase.SELECTING_DEFENSE) {
            state.setDefenseValue(state.calculateSelectedSum(true));
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
        
        state.setLastDamageDealt(damage);
        
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
    
    public boolean confirmPlayerSelection() {
        if (state.getCurrentPhase() == BattleState.Phase.REROLL_PHASE) {
            return executePlayerReroll();
        }
        
        if (state.getCurrentPhase() != BattleState.Phase.SELECTING_ATTACK && 
            state.getCurrentPhase() != BattleState.Phase.SELECTING_DEFENSE) return false;
        
        int requiredCount = state.getRequiredDiceCount(true);
        int selectedCount = state.countSelectedDice(true);
        
        if (selectedCount != requiredCount) return false;
        
        if (!state.canConfirmPrismaticSelection(true)) return false;
        
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
        
        return true;
    }
    
    private boolean executePlayerReroll() {
        if (diceRoller == null) return false;
        
        diceRoller.rerollSelected(state, true);
        StatusEffectProcessor.BattleContext playerContext = createBattleContext(true);
        TurnType playerTurnType = state.isPlayerAttacker() ? TurnType.ATTACK : TurnType.DEFENSE;
        state.getPlayerEffects().processPhase(Phase.AFTER_ROLL, playerTurnType, playerContext);
        state.setDiceValues(true, playerContext.getDiceValues());
        
        if (!state.isPlayerAttacker() && state.getRemainingRerolls(false) > 0) {
            executeOpponentReroll();
        }
        return true;
    }
    
public void skipRerollPhase() {
        if (state.getCurrentPhase() != BattleState.Phase.REROLL_PHASE) return;

        state.clearDiceSelection(true);
        state.clearDiceSelection(false);

        if (!state.isPlayerAttacker() && state.getRemainingRerolls(false) > 0) {
            executeOpponentReroll();
        }

        advanceToAttackPhase();
    }

    private StatusEffectProcessor.BattleContext createBattleContext(boolean forPlayer) {
        return state.createBattleContext(forPlayer);
    }
    
    private List<Boolean> getPrismaticFlagsForDice(boolean forPlayer) {
        return state.getPrismaticFlagsForDice(forPlayer);
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