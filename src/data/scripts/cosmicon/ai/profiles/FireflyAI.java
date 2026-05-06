package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.AttackRerollAI;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.DiceEvaluator;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveResults.PassiveResult;
import java.util.*;

public class FireflyAI extends AttackRerollAI {

    @Override
    public String getCharacterId() {
        return CharacterIds.FIREFLY;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.firefly.name");
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return PassiveEvaluation.notTriggered();
        PassiveResult result = PassiveEvaluator.evaluateForCharacter(getCharacterId(), selectedValues, isAttacking);
        return PassiveEvaluator.toPassiveEvaluation(result,
            DiceEvaluator.hasTwoPairs(selectedValues) ? Strings.get("character.firefly.passive_desc") : "");
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return 0f;
        return DiceEvaluator.hasTwoPairs(selectedValues) ? 15f : 0f;
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
        List<Integer> pairedIndices = new ArrayList<>();
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
