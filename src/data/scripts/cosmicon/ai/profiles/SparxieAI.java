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

public class SparxieAI extends AttackRerollAI {

    private static final float HACK_BONUS_VALUE = 5f;

    @Override
    public String getCharacterId() {
        return CharacterIds.SPARXIE;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.sparxie.name");
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        return true;
    }

    @Override
    public boolean isDefensePassive() {
        return true;
    }

    @Override
    public boolean isAttackPassive() {
        return true;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return PassiveEvaluation.notTriggered();
        PassiveResult result = PassiveEvaluator.evaluateForCharacter(getCharacterId(), selectedValues, isAttacking);
        if (DiceEvaluator.hasIdenticalNumbers(selectedValues)) {
            return PassiveEvaluator.toPassiveEvaluation(result, Strings.get("character.sparxie.passive_desc"));
        }
        return PassiveEvaluation.notTriggered();
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return 0f;
        return DiceEvaluator.hasIdenticalNumbers(selectedValues) ? HACK_BONUS_VALUE : 0f;
    }

    @Override
    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        Map<Integer, Integer> freq = new HashMap<>();
        for (int i = 0; i < pool.size(); i++) {
            freq.merge(pool.getValue(i), 1, Integer::sum);
        }

        int bestValue = -1;
        int bestCount = 0;
        for (Map.Entry<Integer, Integer> entry : freq.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
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
