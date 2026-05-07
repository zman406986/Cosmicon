package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.DiceEvaluator;
import java.util.*;

public class AcheronAI extends SimplePassiveAI {

    @Override
    public String getCharacterId() {
        return CharacterIds.ACHERON;
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
    protected boolean checkPassiveCondition(List<Integer> selectedValues, boolean isAttacking) {
        return DiceEvaluator.allDiceEqualFour(selectedValues);
    }

    @Override
    protected float computePassiveBonus(List<Integer> selectedValues, boolean isAttacking) {
        return 20f;
    }

    @Override
    protected String getPassiveDescription(List<Integer> selectedValues, boolean isAttacking) {
        return Strings.get("character.acheron.passive_desc");
    }

    @Override
    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        if (!isAttacking) return Collections.emptyList();
        Set<Integer> result = findComboForSpecificValue(pool, requiredCount, 4);
        return result != null ? Collections.singletonList(result) : Collections.emptyList();
    }
}
