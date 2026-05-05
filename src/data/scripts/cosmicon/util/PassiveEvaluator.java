package data.scripts.cosmicon.util;

import data.scripts.cosmicon.ai.CharacterAIProfile.PassiveEvaluation;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.StatusEffectProcessor;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.util.PassiveResults.EndOfTurnPassiveResult;
import data.scripts.cosmicon.util.PassiveResults.GrantedEffect;
import data.scripts.cosmicon.util.PassiveResults.PassiveResult;
import data.scripts.cosmicon.util.PassiveResults.PostDamageResult;
import java.util.List;

public class PassiveEvaluator {

    private PassiveEvaluator() {}

    public static boolean allDiceEqualFour(List<Integer> values) {
        return DiceEvaluator.allDiceEqualFour(values);
    }

    public static boolean hasTwoPairs(List<Integer> values) {
        return DiceEvaluator.hasTwoPairs(values);
    }

    public static boolean allEven(List<Integer> values) {
        return DiceEvaluator.allEven(values);
    }

    public static boolean allSame(List<Integer> values) {
        return DiceEvaluator.allSame(values);
    }

    public static boolean allDiceEqualSix(List<Integer> values) {
        return DiceEvaluator.allDiceEqualSix(values);
    }

    public static int countPairs(List<Integer> values) {
        return DiceEvaluator.countPairs(values);
    }

    public static int countOddNumbers(List<Integer> values) {
        return DiceEvaluator.countOddNumbers(values);
    }

    public static int countDistinctValues(List<Integer> values) {
        return DiceEvaluator.countDistinctValues(values);
    }

    public static int sumOfValues(List<Integer> values) {
        return DiceEvaluator.sumOfValues(values);
    }

    public static boolean sumAtLeast(List<Integer> values, int threshold) {
        return DiceEvaluator.sumAtLeast(values, threshold);
    }

    public static boolean hasIdenticalNumbers(List<Integer> values) {
        return DiceEvaluator.hasIdenticalNumbers(values);
    }

    public static PassiveResult evaluateForCharacter(String characterId, List<Integer> diceValues, boolean isAttacking) {
        return evaluateForCharacter(characterId, diceValues, isAttacking, -1, -1, 0);
    }

    public static PassiveResult evaluateForCharacter(String characterId, List<Integer> diceValues, boolean isAttacking, 
                                                       int currentHp, int maxHp, int currentToughnessLayers) {
        PassiveResult result = new PassiveResult();
        CharacterPassives.evaluateForCharacter(characterId, result, diceValues, isAttacking, currentHp, maxHp, currentToughnessLayers);
        return result;
    }

    public static EndOfTurnPassiveResult evaluateEndOfTurnPassive(String characterId, int prismaticTriggerCount, int cumulativeAtkDef) {
        return CharacterPassives.evaluateEndOfTurnPassive(characterId, prismaticTriggerCount, cumulativeAtkDef);
    }

    public static PassiveEvaluation toPassiveEvaluation(PassiveResult result, String description) {
        if (result.isEmpty()) {
            return PassiveEvaluation.notTriggered();
        }
        int totalBonus = result.getAttackBonus() + result.getDefenseBonus();
        for (GrantedEffect ge : result.getGrantedEffects()) {
            totalBonus += ge.layers();
        }
        return PassiveEvaluation.triggered(totalBonus, description);
    }

    public static PostDamageResult evaluatePostDamageForCharacter(String characterId, int damageTaken) {
        return CharacterPassives.evaluatePostDamageForCharacter(characterId, damageTaken);
    }

    public static void applyPassiveEffects(PassiveResult result, BattleState state, boolean forPlayer) {
        if (result == null || result.isEmpty()) return;
        
        StatusEffectProcessor effects = state.getEffects(forPlayer);
        
        for (GrantedEffect ge : result.getGrantedEffects()) {
            if (ge.effect() == StatusEffect.POISON || ge.effect() == StatusEffect.INSTANT_DAMAGE) {
                boolean opponent = !forPlayer;
                state.getEffects(opponent).addEffect(ge.effect(), ge.layers());
            } else if (ge.effect() == StatusEffect.HACK || ge.effect() == StatusEffect.ARISE) {
                state.applyEffect(ge.effect(), ge.layers(), forPlayer);
            } else {
                effects.addEffect(ge.effect(), ge.layers());
            }
        }
        
        for (GrantedEffect ge : result.getSetGrantedEffects()) {
            effects.setEffect(ge.effect(), ge.layers());
        }
        
        if (result.shouldTriggerToughnessInstantDamage()) {
            int currentToughness = effects.getLayers(StatusEffect.TOUGHNESS);
            int newToughness = Math.max(0, currentToughness - result.getToughnessToRemove());
            effects.setEffect(StatusEffect.TOUGHNESS, newToughness);
            
            boolean opponent = !forPlayer;
            state.applyEffect(StatusEffect.INSTANT_DAMAGE, result.getInstantDamageToOpponent(), opponent);
        }
        
        if (result.getHealAmount() > 0) {
            state.applyHealTo(forPlayer, result.getHealAmount());
        }
        
        if (result.hasPerforation()) {
            effects.addEffect(StatusEffect.PERFORATION, 1);
        }
        
        if (result.getPendingDefLevelBoost() > 0) {
            state.setPendingDefLevelBoost(forPlayer, result.getPendingDefLevelBoost());
        }
    }
}