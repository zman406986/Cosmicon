package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveEvaluator.PassiveResult;
import java.util.List;

public class HyacineAIProfile extends AbstractCharacterAIProfile {

    @Override
    public String getCharacterId() {
        return "hyacine";
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.hyacine.name");
    }

    @Override
    protected String getPassiveDescription(List<Integer> selectedValues) {
        if (PassiveEvaluator.allDiceEqualSix(selectedValues)) {
            return Strings.get("character.hyacine.passive_desc_full");
        }
        return Strings.get("character.hyacine.passive_desc_half");
    }

    @Override
    protected float calculatePassiveBonus(List<Integer> selectedValues) {
        int sum = PassiveEvaluator.sumOfValues(selectedValues);
        if (PassiveEvaluator.allDiceEqualSix(selectedValues)) {
            return sum + 6f;
        }
        return sum * 0.5f;
    }
}