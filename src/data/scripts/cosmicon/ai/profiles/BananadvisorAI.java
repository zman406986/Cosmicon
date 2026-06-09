package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.AttackRerollAI;
import data.scripts.cosmicon.util.CharacterIds;
import java.util.*;

public class BananadvisorAI extends AttackRerollAI {

    @Override
    public String getCharacterId() {
        return CharacterIds.BANANADVISOR;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.bananadvisor.name");
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<data.scripts.cosmicon.battle.DiceType> selectedTypes, boolean isAttacking) {
        if (isAttacking || selectedValues.isEmpty()) return PassiveEvaluation.notTriggered();
        return PassiveEvaluation.triggered(5f, Strings.get("character.bananadvisor.passive_desc_heal"));
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        return isAttacking ? 0f : 5f;
    }
}
