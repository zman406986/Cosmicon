package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.CharacterAIProfile;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveEvaluator.PassiveResult;
import java.util.List;

public class DanHengPTAIProfile implements CharacterAIProfile {

    @Override
    public String getCharacterId() {
        return "dan_heng_pt";
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.dan_heng_pt.name");
    }

    @Override
    public boolean prefersHighValues(boolean isAttacking) {
        return true;
    }

    @Override
    public int getTargetThreshold(boolean isAttacking) {
        return isAttacking ? 18 : 10;
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
        
        PassiveResult result = PassiveEvaluator.evaluateForCharacter("dan_heng_pt", selectedValues, isAttacking);
        return PassiveEvaluator.toPassiveEvaluation(result, 
            PassiveEvaluator.sumAtLeast(selectedValues, 18) ? Strings.get("character.dan_heng_pt.passive_desc") : "");
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (!isAttacking) return 0f;
        return PassiveEvaluator.sumAtLeast(selectedValues, 18) ? 10f : 0f;
    }
}