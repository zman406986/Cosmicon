package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.PassiveEvaluator;
import java.util.List;

public class RobinAIProfile extends AbstractCharacterAIProfile {

    @Override
    public String getCharacterId() {
        return CharacterIds.ROBIN;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.robin.name");
    }

    @Override
    protected String getPassiveDescription(List<Integer> selectedValues) {
        return PassiveEvaluator.allEven(selectedValues) ? Strings.get("character.robin.passive_desc") : "";
    }

    @Override
    protected float calculatePassiveBonus(List<Integer> selectedValues) {
        return PassiveEvaluator.allEven(selectedValues) ? 10f : 0f;
    }
}