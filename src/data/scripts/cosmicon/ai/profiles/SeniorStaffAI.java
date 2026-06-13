package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.AttackRerollAI;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.DiceEvaluator;
import java.util.*;

public class SeniorStaffAI extends AttackRerollAI {

    @Override
    public String getCharacterId() {
        return CharacterIds.SENIOR_STAFF;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.senior_staff.name");
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        // Passive works on both attack and defense
        return true;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<data.scripts.cosmicon.battle.DiceType> selectedTypes, boolean isAttacking) {
        if (selectedValues.isEmpty()) return PassiveEvaluation.notTriggered();
        int distinct = DiceEvaluator.countDistinctValues(selectedValues);
        return PassiveEvaluation.triggered(distinct, Strings.get("character.senior_staff.passive_desc"));
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (selectedValues.isEmpty()) return 0f;
        return DiceEvaluator.countDistinctValues(selectedValues);
    }

    @Override
    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        // Try to reroll duplicate-value dice to increase distinct count
        Map<Integer, List<Integer>> valueToIndices = new HashMap<>();
        for (int i = 0; i < pool.size(); i++) {
            valueToIndices.computeIfAbsent(pool.getValue(i), k -> new ArrayList<>()).add(i);
        }

        // Find values that appear more than once - reroll extras to try for distinct values
        List<Integer> duplicateIndices = new ArrayList<>();
        int distinctCount = 0;
        for (Map.Entry<Integer, List<Integer>> entry : valueToIndices.entrySet()) {
            List<Integer> indices = entry.getValue();
            distinctCount++;
            // Keep the first occurrence, mark extras for reroll
            for (int i = 1; i < indices.size(); i++) {
                duplicateIndices.add(indices.get(i));
            }
        }

        // Only suggest rerolling duplicates if we have room to improve distinct count
        if (!duplicateIndices.isEmpty() && distinctCount < requiredCount) {
            return Collections.singletonList(new HashSet<>(duplicateIndices));
        }
        return Collections.emptyList();
    }
}
