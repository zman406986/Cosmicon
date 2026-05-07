package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.DiceEvaluator;

import java.util.*;

public class KafkaAI extends SimplePassiveAI {

    @Override
    public String getCharacterId() {
        return CharacterIds.KAFKA;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.kafka.name");
    }

    @Override
    protected boolean checkPassiveCondition(List<Integer> selectedValues, boolean isAttacking) {
        return true;
    }

    @Override
    protected float computePassiveBonus(List<Integer> selectedValues, boolean isAttacking) {
        return DiceEvaluator.countDistinctValues(selectedValues);
    }

    @Override
    protected String getPassiveDescription(List<Integer> selectedValues, boolean isAttacking) {
        return Strings.format("character.kafka.passive_desc", DiceEvaluator.countDistinctValues(selectedValues));
    }
}
