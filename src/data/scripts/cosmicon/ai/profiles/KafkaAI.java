package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.AttackRerollAI;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.DiceEvaluator;

import java.util.*;

public class KafkaAI extends AttackRerollAI {

    @Override
    public String getCharacterId() {
        return CharacterIds.KAFKA;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.kafka.name");
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return PassiveEvaluation.notTriggered();
        int distinct = DiceEvaluator.countDistinctValues(selectedValues);
        return PassiveEvaluation.triggered(distinct, Strings.format("character.kafka.passive_desc", distinct));
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return 0f;
        return DiceEvaluator.countDistinctValues(selectedValues);
    }
}
