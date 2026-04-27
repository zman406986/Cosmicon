package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveResults.PassiveResult;
import java.util.List;

public class March7thAIProfile extends AbstractCharacterAIProfile {

    private static final int INSTANT_DAMAGE_PER_PAIR = 3;

    @Override
    public String getCharacterId() {
        return CharacterIds.MARCH_7TH;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.march_7th.name");
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        return true;
    }

    @Override
    public boolean prefersPairs() {
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
    public boolean isDefensePassive() {
        return true;
    }

    @Override
    public boolean isAttackPassive() {
        return true;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) {
            return PassiveEvaluation.notTriggered();
        }

        PassiveResult result = PassiveEvaluator.evaluateForCharacter(CharacterIds.MARCH_7TH, selectedValues, isAttacking);
        int pairs = PassiveEvaluator.countPairs(selectedValues);
        
        if (pairs >= 1) {
            int instantDamage = pairs * INSTANT_DAMAGE_PER_PAIR;
            return PassiveEvaluator.toPassiveEvaluation(result,
                Strings.format("character.march_7th.passive_desc", pairs, instantDamage));
        }

        return PassiveEvaluation.notTriggered();
    }

    @Override
    protected float calculatePassiveBonus(List<Integer> selectedValues) {
        if (hasValidValues(selectedValues)) return 0f;
        return PassiveEvaluator.countPairs(selectedValues) * INSTANT_DAMAGE_PER_PAIR;
    }
}