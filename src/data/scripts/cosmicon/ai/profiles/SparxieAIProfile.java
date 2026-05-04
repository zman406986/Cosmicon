package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveResults.PassiveResult;
import java.util.List;

public class SparxieAIProfile extends AbstractCharacterAIProfile {

    private static final float HACK_BONUS_VALUE = 5f;

    @Override
    public String getCharacterId() {
        return CharacterIds.SPARXIE;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.sparxie.name");
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
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

        PassiveResult result = PassiveEvaluator.evaluateForCharacter(CharacterIds.SPARXIE, selectedValues, isAttacking);

        if (PassiveEvaluator.hasIdenticalNumbers(selectedValues)) {
            return PassiveEvaluator.toPassiveEvaluation(result,
                Strings.get("character.sparxie.passive_desc"));
        }

        return PassiveEvaluation.notTriggered();
    }

    @Override
    protected float calculatePassiveBonus(List<Integer> selectedValues) {
        if (hasNoValues(selectedValues)) return 0f;
        return PassiveEvaluator.hasIdenticalNumbers(selectedValues) ? HACK_BONUS_VALUE : 0f;
    }
}