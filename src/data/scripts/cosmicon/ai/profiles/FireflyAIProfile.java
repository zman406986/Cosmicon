package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.util.PassiveEvaluator;
import java.util.List;

public class FireflyAIProfile extends AbstractCharacterAIProfile {

    @Override
    public String getCharacterId() {
        return "firefly";
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.firefly.name");
    }

    @Override
    protected String getPassiveDescription(List<Integer> selectedValues) {
        return PassiveEvaluator.hasTwoPairs(selectedValues) ? Strings.get("character.firefly.passive_desc") : "";
    }

    @Override
    protected float calculatePassiveBonus(List<Integer> selectedValues) {
        float bonus = PassiveEvaluator.hasTwoPairs(selectedValues) ? 15f : 0f;
        bonus += 5f;
        return bonus;
    }
}