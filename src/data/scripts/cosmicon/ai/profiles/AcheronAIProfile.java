package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.CharacterAIProfile;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveEvaluator.PassiveResult;
import java.util.List;

public class AcheronAIProfile implements CharacterAIProfile {

    @Override
    public String getCharacterId() {
        return "acheron";
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.acheron.name");
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
    public float getRiskTolerance() {
        return 0.4f;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (!isAttacking || selectedValues == null || selectedValues.isEmpty()) {
            return PassiveEvaluation.notTriggered();
        }
        
        PassiveResult result = PassiveEvaluator.evaluateForCharacter("acheron", selectedValues, isAttacking);
        return PassiveEvaluator.toPassiveEvaluation(result, 
            PassiveEvaluator.allDiceEqualFour(selectedValues) ? Strings.get("character.acheron.passive_desc") : "");
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (!isAttacking) return 0f;
        return PassiveEvaluator.allDiceEqualFour(selectedValues) ? 20f : 0f;
    }
}