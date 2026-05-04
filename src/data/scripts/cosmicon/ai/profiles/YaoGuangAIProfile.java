package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.PassiveEvaluator;
import java.util.List;

public class YaoGuangAIProfile extends AbstractCharacterAIProfile {

    private static final int ATTACK_THRESHOLD = 18;
    private static final float PRISMATIC_USE_VALUE = 8f;

    @Override
    public String getCharacterId() {
        return CharacterIds.YAO_GUANG;
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
            return PassiveEvaluation.triggered(PRISMATIC_USE_VALUE, Strings.get("character.yao_guang.passive_desc"));
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

    @Override
    public float getThornsPenaltyPerReroll() {
        return 2f;
    }

    @Override
    protected float calculatePassiveBonus(List<Integer> selectedValues) {
        return 0f;
    }
}