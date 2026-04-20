package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.CharacterAIProfile;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveEvaluator.PassiveResult;
import java.util.List;

public class HyacineAIProfile implements CharacterAIProfile {

    @Override
    public String getCharacterId() {
        return "hyacine";
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.hyacine.name");
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
        
        PassiveResult result = PassiveEvaluator.evaluateForCharacter("hyacine", selectedValues, isAttacking);
        if (PassiveEvaluator.allDiceEqualSix(selectedValues)) {
            return PassiveEvaluator.toPassiveEvaluation(result, Strings.get("character.hyacine.passive_desc_full"));
        }
        return PassiveEvaluator.toPassiveEvaluation(result, Strings.get("character.hyacine.passive_desc_half"));
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (!isAttacking) return 0f;
        
        int sum = PassiveEvaluator.sumOfValues(selectedValues);
        if (PassiveEvaluator.allDiceEqualSix(selectedValues)) {
            return sum + 6f;
        }
        return sum * 0.5f;
    }
}