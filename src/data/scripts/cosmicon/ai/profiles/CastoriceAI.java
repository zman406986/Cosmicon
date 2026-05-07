package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.AttackRerollAI;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.CharacterIds;
import java.util.*;

public class CastoriceAI extends AttackRerollAI {

    @Override
    public String getCharacterId() {
        return CharacterIds.CASTORICE;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.castorice.name");
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        return !isAttacking;
    }

    @Override
    public float getRiskTolerance() {
        return 0.4f;
    }

    @Override
    public int getTargetThreshold(boolean isAttacking) {
        return isAttacking ? 12 : 15;
    }

    @Override
    public boolean isDefensePassive() {
        return true;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        return PassiveEvaluation.notTriggered();
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (isAttacking) return 0f;
        return 10f;
    }
}
