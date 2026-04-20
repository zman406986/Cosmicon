package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.character.PassiveEventSystem;
import data.scripts.cosmicon.util.PassiveEvaluator;
import java.util.List;

public class YaoGuangAIProfile extends AbstractCharacterAIProfile {

    private static final int ATTACK_THRESHOLD = 18;
    private static final int THORNS_DAMAGE_PER_REROLL = 2;
    private static final int FREE_REROLLS = 2;
    private static final float PRISMATIC_USE_VALUE = 8f;

    @Override
    public String getCharacterId() {
        return "yao_guang";
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.yao_guang.name");
    }

    @Override
    public int getTargetThreshold(boolean isAttacking) {
        return isAttacking ? ATTACK_THRESHOLD : 10;
    }

    @Override
    public float getRiskTolerance() {
        return 0.6f;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (!isAttacking || selectedValues == null || selectedValues.isEmpty()) {
            return PassiveEvaluation.notTriggered();
        }
        
        int sum = PassiveEvaluator.sumOfValues(selectedValues);
        if (sum >= ATTACK_THRESHOLD) {
            float bonusValue = PRISMATIC_USE_VALUE + calculateThornsCleansingValue(0);
            return PassiveEvaluation.triggered(bonusValue, Strings.get("character.yao_guang.passive_desc"));
        }
        
        return PassiveEvaluation.notTriggered();
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (!isAttacking || selectedValues == null || selectedValues.isEmpty()) return 0f;
        
        int sum = PassiveEvaluator.sumOfValues(selectedValues);
        if (sum >= ATTACK_THRESHOLD) {
            return PRISMATIC_USE_VALUE;
        }
        return 0f;
    }

    public float evaluateRerollWithThornsCost(int currentRerollsUsed, float improvement, int currentAttackSum) {
        int newRerollsUsed = currentRerollsUsed + 1;
        int thornsCost = PassiveEventSystem.getThornsCostForRerollYaoGuang(currentRerollsUsed);
        
        float thornsPenalty = thornsCost * 1.5f;
        
        float cleansingBonus = 0f;
        if (currentAttackSum + improvement >= ATTACK_THRESHOLD && currentAttackSum < ATTACK_THRESHOLD) {
            cleansingBonus = calculateThornsCleansingValue(newRerollsUsed);
        }
        
        return improvement - thornsPenalty + cleansingBonus;
    }

    private float calculateThornsCleansingValue(int totalRerollsUsed) {
        int totalThorns = PassiveEventSystem.getTotalThornsAfterRerollsYaoGuang(totalRerollsUsed);
        return totalThorns * 1.2f;
    }

    public boolean shouldContinueRerolling(int currentSum, int rerollsRemaining, int rerollsUsedThisTurn) {
        if (currentSum >= ATTACK_THRESHOLD) return false;
        if (rerollsRemaining <= 0) return false;
        
        int potentialThorns = PassiveEventSystem.getTotalThornsAfterRerollsYaoGuang(rerollsUsedThisTurn + 1);
        int currentThorns = PassiveEventSystem.getTotalThornsAfterRerollsYaoGuang(rerollsUsedThisTurn);

        return potentialThorns <= currentThorns || potentialThorns < 6;
    }

    @Override
    protected float calculatePassiveBonus(List<Integer> selectedValues) {
        return 0f;
    }
}