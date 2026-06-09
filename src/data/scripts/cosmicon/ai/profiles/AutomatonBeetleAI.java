package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.AttackRerollAI;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.DiceEvaluator;
import java.util.*;

public class AutomatonBeetleAI extends AttackRerollAI {

    // Forcefield prevents all general attack damage (~10-15 expected value) + 8 Strength next turn
    private static final float PASSIVE_BONUS_VALUE = 15f;

    @Override
    public String getCharacterId() {
        return CharacterIds.AUTOMATON_BEETLE;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.automaton_beetle.name");
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        return !isAttacking;
    }

    @Override
    public boolean isDefensePassive() {
        return true;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<data.scripts.cosmicon.battle.DiceType> selectedTypes, boolean isAttacking) {
        if (isAttacking || selectedValues.isEmpty()) return PassiveEvaluation.notTriggered();
        if (DiceEvaluator.hasThreeConsecutive(selectedValues)) {
            return PassiveEvaluation.triggered(PASSIVE_BONUS_VALUE, Strings.get("character.automaton_beetle.passive_desc"));
        }
        return PassiveEvaluation.notTriggered();
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (isAttacking || selectedValues.isEmpty()) return 0f;
        return DiceEvaluator.hasThreeConsecutive(selectedValues) ? PASSIVE_BONUS_VALUE : 0f;
    }

    @Override
    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        if (isAttacking) return Collections.emptyList();

        // Try to find dice to reroll to achieve 3 consecutive numbers
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) {
            values.add(pool.getValue(i));
        }
        Collections.sort(values);

        // Check if we're close to having 3 consecutive (have 2 of 3 consecutive)
        Set<Integer> allValues = new HashSet<>(values);
        for (int start = 1; start <= 10; start++) {
            boolean hasStart = allValues.contains(start);
            boolean hasMid = allValues.contains(start + 1);
            boolean hasEnd = allValues.contains(start + 2);

            int present = (hasStart ? 1 : 0) + (hasMid ? 1 : 0) + (hasEnd ? 1 : 0);
            if (present == 2) {
                // Find which value is missing and reroll dice that aren't part of the consecutive run
                Set<Integer> neededValues = new HashSet<>();
                neededValues.add(start);
                neededValues.add(start + 1);
                neededValues.add(start + 2);

                Set<Integer> keepIndices = new HashSet<>();
                int neededFound = 0;
                for (int i = 0; i < pool.size(); i++) {
                    int v = pool.getValue(i);
                    if (neededValues.contains(v) && neededFound < 3) {
                        keepIndices.add(i);
                        neededFound++;
                    }
                }

                Set<Integer> rerollIndices = new HashSet<>();
                for (int i = 0; i < pool.size(); i++) {
                    if (!keepIndices.contains(i)) {
                        rerollIndices.add(i);
                    }
                }
                if (!rerollIndices.isEmpty()) {
                    return Collections.singletonList(rerollIndices);
                }
            }
        }
        return Collections.emptyList();
    }
}
