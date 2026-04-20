package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.CharacterAIProfile;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveEvaluator.PassiveResult;
import java.util.List;

public class AventurineAIProfile implements CharacterAIProfile {

    @Override
    public String getCharacterId() {
        return "aventurine";
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.aventurine.name");
    }

    @Override
    public boolean prefersHighValues(boolean isAttacking) {
        return true;
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        return isAttacking;
    }

    @Override
    public boolean isAttackPassive() {
        return true;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (!isAttacking || selectedValues == null || selectedValues.isEmpty()) {
            return PassiveEvaluation.notTriggered();
        }
        
        PassiveResult result = PassiveEvaluator.evaluateForCharacter("aventurine", selectedValues, isAttacking);
        int oddCount = PassiveEvaluator.countOddNumbers(selectedValues);
        return PassiveEvaluator.toPassiveEvaluation(result, Strings.format("character.aventurine.passive_desc", oddCount));
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (!isAttacking) return 0f;
        float bonus = PassiveEvaluator.countOddNumbers(selectedValues);
        bonus += 7f;
        return bonus;
    }
}