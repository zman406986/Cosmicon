package data.scripts.cosmicon.util;

import data.scripts.cosmicon.ai.CharacterAIProfile.PassiveEvaluation;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.StatusEffectProcessor;
import data.scripts.cosmicon.battle.StatusEffectProcessor.DurationType;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.util.PassiveResults.EndOfTurnPassiveResult;
import data.scripts.cosmicon.util.PassiveResults.GrantedEffect;
import data.scripts.cosmicon.util.PassiveResults.PassiveResult;
import data.scripts.cosmicon.util.PassiveResults.PostDamageResult;
import java.util.List;

public class PassiveEvaluator {

    private PassiveEvaluator() {}

    public static PassiveResult evaluateForCharacter(String characterId, List<Integer> diceValues, boolean isAttacking) {
        return evaluateForCharacter(characterId, diceValues, isAttacking, -1, -1, 0, 0);
    }

    public static PassiveResult evaluateForCharacter(String characterId, List<Integer> diceValues, boolean isAttacking, 
                                                       int currentHp, int maxHp, int currentToughnessLayers) {
        return evaluateForCharacter(characterId, diceValues, isAttacking, currentHp, maxHp, currentToughnessLayers, 0);
    }

    public static PassiveResult evaluateForCharacter(String characterId, List<Integer> diceValues, boolean isAttacking, 
                                                       int currentHp, int maxHp, int currentToughnessLayers,
                                                       int currentStrengthLayers) {
        PassiveResult result = new PassiveResult();
        CharacterPassives.evaluateForCharacter(characterId, result, diceValues, isAttacking, currentHp, maxHp, currentToughnessLayers, currentStrengthLayers);
        return result;
    }

    public static EndOfTurnPassiveResult evaluateEndOfTurnPassive(String characterId, int prismaticTriggerCount, int cumulativeAtkDef) {
        return CharacterPassives.evaluateEndOfTurnPassive(characterId, prismaticTriggerCount, cumulativeAtkDef);
    }

    public static PassiveEvaluation toPassiveEvaluation(PassiveResult result, String description) {
        if (result.isEmpty()) {
            return PassiveEvaluation.notTriggered();
        }
        int totalBonus = result.getAttackBonus();
        for (GrantedEffect ge : result.getGrantedEffects()) {
            totalBonus += ge.layers();
        }
        return PassiveEvaluation.triggered(totalBonus, description);
    }

    public static PostDamageResult evaluatePostDamageForCharacter(String characterId, int damageTaken) {
        return CharacterPassives.evaluatePostDamageForCharacter(characterId, damageTaken);
    }

    public static void applyPassiveEffects(PassiveResult result, BattleState state, boolean forPlayer, String characterId) {
        if (result == null || result.isEmpty()) return;
        
        StatusEffectProcessor effects = state.getEffects(forPlayer);
        String source = "passive." + characterId;
        
        for (GrantedEffect ge : result.getGrantedEffects()) {
            if (ge.effect() == StatusEffect.POISON) {
                boolean opponent = !forPlayer;
                state.getEffects(opponent).addEffect(ge.effect(), source, ge.layers(), DurationType.PERMANENT);
            } else if (ge.effect() == StatusEffect.INSTANT_DAMAGE) {
                effects.addEffect(ge.effect(), source, ge.layers(), DurationType.PERMANENT);
            } else if (ge.effect() == StatusEffect.HACK || ge.effect() == StatusEffect.ARISE) {
                state.applyEffect(ge.effect(), ge.layers(), forPlayer);
            } else if (ge.effect() == StatusEffect.UNYIELDING) {
                effects.addEffect(ge.effect(), source, ge.layers(), DurationType.TURN_BASED);
            } else {
                effects.addEffect(ge.effect(), source, ge.layers(), DurationType.PERMANENT);
            }
        }
        
        if (result.shouldTriggerToughnessInstantDamage()) {
            int currentToughness = effects.getLayers(StatusEffect.TOUGHNESS);
            int newToughness = Math.max(0, currentToughness - result.getToughnessToRemove());
            effects.setEffectFromSource(StatusEffect.TOUGHNESS, source, newToughness);
            
            effects.addEffect(StatusEffect.INSTANT_DAMAGE, source, result.getInstantDamageToOpponent(), DurationType.PERMANENT);
        }
        
        if (result.getHealAmount() > 0) {
            state.applyHealTo(forPlayer, result.getHealAmount());
            state.notifyHeal(forPlayer, result.getHealAmount());
        }
        
        if (result.hasPerforation()) {
            effects.addEffect(StatusEffect.PERFORATION, source, 1, DurationType.PERMANENT);
        }
        
        if (result.getPendingDefLevelBoost() > 0) {
            state.setPendingDefLevelBoost(forPlayer, result.getPendingDefLevelBoost());
        }
        
        if (result.getPendingStrength() > 0) {
            state.setPendingStrength(forPlayer, result.getPendingStrength());
        }
    }
}