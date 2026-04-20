package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.CharacterAIProfile;
import data.scripts.cosmicon.battle.DiceType;
import java.util.List;

public class TheHertaAIProfile implements CharacterAIProfile {

    @Override
    public String getCharacterId() {
        return "the_herta";
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.the_herta.name");
    }

    @Override
    public boolean prefersHighValues(boolean isAttacking) {
        return true;
    }
    
    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        return false;
    }
    
    @Override
    public float getPrismaticDiceBonus(DiceType type, int faceValue, boolean isAttacking) {
        float bonus = 0f;
        if (type == DiceType.PRISMATIC_D12) {
            bonus += 5f;
            if (faceValue >= 8) {
                bonus += 3f;
            }
        }
        return bonus;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        return PassiveEvaluation.notTriggered();
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        return 0f;
    }
}