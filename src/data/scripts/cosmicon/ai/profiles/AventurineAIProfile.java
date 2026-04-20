package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.util.PassiveEvaluator;
import java.util.List;

public class AventurineAIProfile extends AbstractCharacterAIProfile {

    @Override
    public String getCharacterId() {
        return "aventurine";
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.aventurine.name");
    }

    @Override
    protected String getPassiveDescription(List<Integer> selectedValues) {
        int oddCount = PassiveEvaluator.countOddNumbers(selectedValues);
        return Strings.format("character.aventurine.passive_desc", oddCount);
    }

    @Override
    protected float calculatePassiveBonus(List<Integer> selectedValues) {
        float bonus = PassiveEvaluator.countOddNumbers(selectedValues);
        bonus += 7f;
        return bonus;
    }
}