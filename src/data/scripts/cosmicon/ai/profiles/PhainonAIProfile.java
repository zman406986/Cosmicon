package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleState;
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
        return true;
    }

    @Override
    public boolean isAttackPassive() {
        return true;
    }

    @Override
    public boolean isDefensePassive() {
        return true;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) {
            return PassiveEvaluation.notTriggered();
        }
        
        if (isAttacking) {
            return PassiveEvaluation.triggered(15f, Strings.get("character.phainon.passive_desc"));
        }
        
        PassiveResult result = PassiveEvaluator.evaluateForCharacter(CharacterIds.PHAINON, selectedValues, false);
        return PassiveEvaluator.toPassiveEvaluation(result, 
            PassiveEvaluator.allSame(selectedValues) ? Strings.get("character.phainon.passive_desc") : "");
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking, BattleState state, boolean forPlayer) {
        if (!isAttacking && state != null && !state.isPhainonUnyieldingAvailable(forPlayer)) {
            return PassiveEvaluation.notTriggered();
        }
        return evaluatePassiveTrigger(selectedValues, selectedTypes, isAttacking);
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (isAttacking) return 15f;
        return PassiveEvaluator.allSame(selectedValues) ? 50f : 0f;
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking, BattleState state, boolean forPlayer) {
        if (!isAttacking && state != null && !state.isPhainonUnyieldingAvailable(forPlayer)) {
            return 0f;
        }
        return getPassiveBonusValue(selectedValues, isAttacking);
    }

    @Override
    protected float calculatePassiveBonus(List<Integer> selectedValues) {
        return 0f;
    }
}
