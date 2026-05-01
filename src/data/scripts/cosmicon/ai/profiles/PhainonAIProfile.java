package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveResults.PassiveResult;
import java.util.List;

public class PhainonAIProfile extends AbstractCharacterAIProfile {

    @Override
    public String getCharacterId() {
        return CharacterIds.PHAINON;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.phainon.name");
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        return !isAttacking;
    }

    @Override
    public boolean isDefensePassive() {
        return true;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (isAttacking || selectedValues == null || selectedValues.isEmpty()) {
            return PassiveEvaluation.notTriggered();
        }
        
        PassiveResult result = PassiveEvaluator.evaluateForCharacter(CharacterIds.PHAINON, selectedValues, false);
        return PassiveEvaluator.toPassiveEvaluation(result, 
            PassiveEvaluator.allSame(selectedValues) ? Strings.get("character.phainon.passive_desc") : "");
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (isAttacking) return 0f;
        return PassiveEvaluator.allSame(selectedValues) ? 50f : 0f;
    }

    @Override
    protected float calculatePassiveBonus(List<Integer> selectedValues) {
        return 0f;
    }
}