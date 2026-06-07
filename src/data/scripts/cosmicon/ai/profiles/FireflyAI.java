package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.DiceEvaluator;
import java.util.*;

public class FireflyAI extends SimplePassiveAI {

    @Override
    public String getCharacterId() {
        return CharacterIds.FIREFLY;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.firefly.name");
    }

    @Override
    protected boolean checkPassiveCondition(List<Integer> selectedValues, boolean isAttacking) {
        return DiceEvaluator.hasTwoPairs(selectedValues);
    }

    @Override
    protected float computePassiveBonus(List<Integer> selectedValues, boolean isAttacking) {
        return 15f;
    }

    @Override
    protected String getPassiveDescription(List<Integer> selectedValues, boolean isAttacking) {
        return Strings.get("character.firefly.passive_desc");
    }

    @Override
    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        if (!isAttacking) return Collections.emptyList();
        Map<Integer, List<Integer>> valueToIndices = new HashMap<>();
        for (int i = 0; i < pool.size(); i++) {
            valueToIndices.computeIfAbsent(pool.getValue(i), k -> new ArrayList<>()).add(i);
        }

        int pairCount = 0;
        Set<Integer> pairedIndices = new HashSet<>();
        for (List<Integer> indices : valueToIndices.values()) {
            if (indices.size() >= 2) {
                pairCount++;
                pairedIndices.add(indices.get(0));
                pairedIndices.add(indices.get(1));
            }
        }

        if (pairCount == 1 && requiredCount >= 4) {
            Set<Integer> nonPaired = new HashSet<>();
            for (int i = 0; i < pool.size(); i++) {
                if (!pairedIndices.contains(i)) {
                    nonPaired.add(i);
                }
            }
            if (!nonPaired.isEmpty()) {
                return Collections.singletonList(nonPaired);
            }
        }
        return Collections.emptyList();
    }
}
