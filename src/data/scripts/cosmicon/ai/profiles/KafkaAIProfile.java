package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.PassiveEvaluator;
import java.util.List;

public class KafkaAIProfile extends AbstractCharacterAIProfile {

    @Override
    public String getCharacterId() {
        return CharacterIds.KAFKA;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.kafka.name");
    }

    @Override
    protected String getPassiveDescription(List<Integer> selectedValues) {
        int distinct = PassiveEvaluator.countDistinctValues(selectedValues);
        return Strings.format("character.kafka.passive_desc", distinct);
    }

    @Override
    protected float calculatePassiveBonus(List<Integer> selectedValues) {
        return PassiveEvaluator.countDistinctValues(selectedValues);
    }
}