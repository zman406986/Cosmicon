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

public class PhainonAI extends AttackRerollAI {

    @Override
    public String getCharacterId() {
        return CharacterIds.PHAINON;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.phainon.name");
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        return true;
    }

    @Override
    public boolean isAttackPassive() {
        return true;
    }

    @Override
    public boolean isDefensePassive() {
        return true;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return PassiveEvaluation.notTriggered();
        if (isAttacking) {
            return PassiveEvaluation.triggered(15f, Strings.get("character.phainon.passive_desc"));
        }
        PassiveResult result = PassiveEvaluator.evaluateForCharacter(getCharacterId(), selectedValues, false);
        return PassiveEvaluator.toPassiveEvaluation(result,
            DiceEvaluator.allSame(selectedValues) ? Strings.get("character.phainon.passive_desc") : "");
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking, BattleState state, boolean forPlayer) {
        if (!isAttacking && state != null && state.isPhainonUnyieldingAvailable(forPlayer)) {
            return PassiveEvaluation.notTriggered();
        }
        return evaluatePassiveTrigger(selectedValues, selectedTypes, isAttacking);
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (isAttacking) return 15f;
        return DiceEvaluator.allSame(selectedValues) ? 50f : 0f;
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking, BattleState state, boolean forPlayer) {
        if (!isAttacking && state != null && state.isPhainonUnyieldingAvailable(forPlayer)) {
            return 0f;
        }
        return getPassiveBonusValue(selectedValues, isAttacking);
    }

    @Override
    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        if (isAttacking) return Collections.emptyList();

        Map<Integer, List<Integer>> valueToIndices = new HashMap<>();
        for (int i = 0; i < pool.size(); i++) {
            valueToIndices.computeIfAbsent(pool.getValue(i), k -> new ArrayList<>()).add(i);
        }

        int bestValue = -1;
        int bestCount = 0;
        for (Map.Entry<Integer, List<Integer>> entry : valueToIndices.entrySet()) {
            if (entry.getValue().size() > bestCount) {
                bestCount = entry.getValue().size();
                bestValue = entry.getKey();
            }
        }

        if (bestCount >= requiredCount - 1 && bestCount < requiredCount) {
            Set<Integer> nonMatching = new HashSet<>();
            for (int i = 0; i < pool.size(); i++) {
                if (pool.getValue(i) != bestValue) {
                    nonMatching.add(i);
                }
            }
            if (!nonMatching.isEmpty()) {
                return Collections.singletonList(nonMatching);
            }
        }
        return Collections.emptyList();
    }
}
