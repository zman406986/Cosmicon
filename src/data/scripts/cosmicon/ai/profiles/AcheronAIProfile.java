package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.util.PassiveEvaluator;
import java.util.List;

public class AcheronAIProfile extends AbstractCharacterAIProfile {

    @Override
    public String getCharacterId() {
        return "acheron";
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.acheron.name");
    }

    @Override
    public float getRiskTolerance() {
        return 0.4f;
    }

    @Override
    protected String getPassiveDescription(List<Integer> selectedValues) {
        return PassiveEvaluator.allDiceEqualFour(selectedValues) ? Strings.get("character.acheron.passive_desc") : "";
    }

    @Override
    protected float calculatePassiveBonus(List<Integer> selectedValues) {
        return PassiveEvaluator.allDiceEqualFour(selectedValues) ? 20f : 0f;
    }
}