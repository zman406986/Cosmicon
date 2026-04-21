package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.util.PassiveEvaluator;
import java.util.List;

public class DanHengAIProfile extends AbstractCharacterAIProfile {

    @Override
    public String getCharacterId() {
        return "dan_heng";
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.dan_heng.name");
    }

    @Override
    public int getTargetThreshold(boolean isAttacking) {
        return isAttacking ? 18 : 10;
    }

    @Override
    protected String getPassiveDescription(List<Integer> selectedValues) {
        return PassiveEvaluator.sumAtLeast(selectedValues, 18) ? Strings.get("character.dan_heng.passive_desc") : "";
    }

    @Override
    protected float calculatePassiveBonus(List<Integer> selectedValues) {
        return PassiveEvaluator.sumAtLeast(selectedValues, 18) ? 10f : 0f;
    }
}