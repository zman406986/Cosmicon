package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.CharacterAIProfile;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveEvaluator.PassiveResult;
import java.util.List;

public class March7thAIProfile implements CharacterAIProfile {

    private static final int INSTANT_DAMAGE_PER_PAIR = 3;

    @Override
    public String getCharacterId() {
        return "march_7th";
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.march_7th.name");
    }

    @Override
    public boolean prefersHighValues(boolean isAttacking) {
        return true;
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        return true;
    }

    @Override
    public float getRiskTolerance() {
        return 0.6f;
    }

    @Override
    public int getTargetThreshold(boolean isAttacking) {
        return isAttacking ? 12 : 8;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) {
            return PassiveEvaluation.notTriggered();
        }

        PassiveResult result = PassiveEvaluator.evaluateForCharacter("march_7th", selectedValues, isAttacking);
        int pairs = PassiveEvaluator.countPairs(selectedValues);
        
        if (pairs >= 1) {
            int instantDamage = pairs * INSTANT_DAMAGE_PER_PAIR;
            return PassiveEvaluator.toPassiveEvaluation(result,
                Strings.format("character.march_7th.passive_desc", pairs, instantDamage));
        }

        return PassiveEvaluation.notTriggered();
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return 0f;
        return PassiveEvaluator.countPairs(selectedValues) * INSTANT_DAMAGE_PER_PAIR;
    }

    @Override
    public boolean isDefensePassive() {
        return true;
    }

    @Override
    public boolean isAttackPassive() {
        return true;
    }

    public int calculateTotalDamageOutput(int baseAttackDamage, List<Integer> selectedValues) {
        int pairs = PassiveEvaluator.countPairs(selectedValues);
        return baseAttackDamage + pairs * INSTANT_DAMAGE_PER_PAIR;
    }

    public float estimatePairPotential(List<Integer> allDiceValues, int requiredSelectCount) {
        if (allDiceValues == null || allDiceValues.isEmpty() || requiredSelectCount < 2) {
            return 0f;
        }

        return Math.min(PassiveEvaluator.countPairs(allDiceValues), requiredSelectCount / 2);
    }
}