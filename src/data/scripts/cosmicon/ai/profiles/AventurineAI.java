package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.AttackRerollAI;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.DiceEvaluator;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveResults.PassiveResult;
import java.util.*;

public class AventurineAI extends AttackRerollAI {

    private static final int TOUGHNESS_BURST_THRESHOLD = 7;
    private static final float BURST_DAMAGE_VALUE = 7f;

    @Override
    public String getCharacterId() {
        return CharacterIds.AVENTURINE;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.aventurine.name");
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        return true;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return PassiveEvaluation.notTriggered();
        int oddCount = DiceEvaluator.countOddNumbers(selectedValues);
        return PassiveEvaluation.triggered(oddCount + 7f, Strings.format("character.aventurine.passive_desc", oddCount));
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return 0f;
        return DiceEvaluator.countOddNumbers(selectedValues) + 7f;
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking, BattleState state, boolean forPlayer) {
        if (state == null) return getPassiveBonusValue(selectedValues, isAttacking);

        int currentToughness = state.getEffects(forPlayer).getLayers(StatusEffect.TOUGHNESS);
        int oddCount = DiceEvaluator.countOddNumbers(selectedValues);
        int newTotal = currentToughness + oddCount;

        if (newTotal >= TOUGHNESS_BURST_THRESHOLD) {
            int remainder = newTotal - TOUGHNESS_BURST_THRESHOLD;
            return BURST_DAMAGE_VALUE + remainder;
        }
        return oddCount;
    }

    @Override
    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        List<Integer> evenIndices = new ArrayList<>();
        int oddCount = 0;
        for (int i = 0; i < pool.size(); i++) {
            if (pool.getValue(i) % 2 == 0) {
                evenIndices.add(i);
            } else {
                oddCount++;
            }
        }
        if (oddCount >= requiredCount - 1 && !evenIndices.isEmpty()) {
            return Collections.singletonList(new HashSet<>(evenIndices));
        }
        return Collections.emptyList();
    }
}
