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

public class March7thAI extends AttackRerollAI {

    private static final int INSTANT_DAMAGE_PER_PAIR = 3;

    @Override
    public String getCharacterId() {
        return CharacterIds.MARCH_7TH;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.march_7th.name");
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        return true;
    }

    @Override
    public boolean prefersPairs() {
        return true;
    }

    @Override
    public float getRiskTolerance() {
        return 0.6f;
    }

    @Override
    public int getTargetThreshold(boolean isAttacking) {
        return isAttacking ? 12 : 8;
    }

    @Override
    public boolean isDefensePassive() {
        return true;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (selectedValues.isEmpty()) return PassiveEvaluation.notTriggered();
        PassiveResult result = PassiveEvaluator.evaluateForCharacter(getCharacterId(), selectedValues, isAttacking);
        int pairs = DiceEvaluator.countPairs(selectedValues);
        if (pairs >= 1) {
            int instantDamage = pairs * INSTANT_DAMAGE_PER_PAIR;
            return PassiveEvaluator.toPassiveEvaluation(result,
                Strings.format("character.march_7th.passive_desc", pairs, instantDamage));
        }
        return PassiveEvaluation.notTriggered();
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (selectedValues.isEmpty()) return 0f;
        return DiceEvaluator.countPairs(selectedValues) * INSTANT_DAMAGE_PER_PAIR;
    }

    @Override
    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        Map<Integer, List<Integer>> valueToIndices = new HashMap<>();
        for (int i = 0; i < pool.size(); i++) {
            valueToIndices.computeIfAbsent(pool.getValue(i), k -> new ArrayList<>()).add(i);
        }

        for (Map.Entry<Integer, List<Integer>> entry : valueToIndices.entrySet()) {
            if (entry.getValue().size() == 1) {
                List<Integer> nonMatching = new ArrayList<>();
                for (int i = 0; i < pool.size(); i++) {
                    if (pool.getValue(i) != entry.getKey()) {
                        nonMatching.add(i);
                    }
                }
                if (!nonMatching.isEmpty()) {
                    return Collections.singletonList(new HashSet<>(nonMatching.subList(0, 1)));
                }
            }
        }
        return Collections.emptyList();
    }
}
