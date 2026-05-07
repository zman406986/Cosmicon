package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.DiceEvaluator;
import java.util.*;

public class RobinAI extends SimplePassiveAI {

    @Override
    public String getCharacterId() {
        return CharacterIds.ROBIN;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.robin.name");
    }

    @Override
    protected boolean checkPassiveCondition(List<Integer> selectedValues, boolean isAttacking) {
        return DiceEvaluator.allEven(selectedValues);
    }

    @Override
    protected float computePassiveBonus(List<Integer> selectedValues, boolean isAttacking) {
        return 10f;
    }

    @Override
    protected String getPassiveDescription(List<Integer> selectedValues, boolean isAttacking) {
        return Strings.get("character.robin.passive_desc");
    }

    @Override
    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        if (!isAttacking) return Collections.emptyList();
        List<Integer> oddIndices = new ArrayList<>();
        int evenCount = 0;
        for (int i = 0; i < pool.size(); i++) {
            if (pool.getValue(i) % 2 != 0) {
                oddIndices.add(i);
            } else {
                evenCount++;
            }
        }
        if (evenCount >= requiredCount - 1 && oddIndices.size() <= 2 && !oddIndices.isEmpty()) {
            return Collections.singletonList(new HashSet<>(oddIndices));
        }
        return Collections.emptyList();
    }
}
