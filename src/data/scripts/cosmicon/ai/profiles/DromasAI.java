package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.AttackRerollAI;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.DiceEvaluator;
import java.util.*;

public class DromasAI extends AttackRerollAI {

    // 2 stacks of Poison deals 2 damage this turn + 1 next turn = ~3 effective damage
    private static final float POISON_BONUS_VALUE = 3f;

    @Override
    public String getCharacterId() {
        return CharacterIds.DROMAS;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.dromas.name");
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<data.scripts.cosmicon.battle.DiceType> selectedTypes, boolean isAttacking) {
        if (!isAttacking || selectedValues.isEmpty()) return PassiveEvaluation.notTriggered();
        if (DiceEvaluator.allEven(selectedValues)) {
            return PassiveEvaluation.triggered(POISON_BONUS_VALUE, Strings.get("character.dromas.passive_desc"));
        }
        return PassiveEvaluation.notTriggered();
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (!isAttacking || selectedValues.isEmpty()) return 0f;
        return DiceEvaluator.allEven(selectedValues) ? POISON_BONUS_VALUE : 0f;
    }

    @Override
    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        if (!isAttacking) return Collections.emptyList();

        // Try to reroll odd dice to achieve all-even (same pattern as TrashcanAI)
        List<Integer> oddIndices = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) {
            if (pool.getValue(i) % 2 != 0) {
                oddIndices.add(i);
            }
        }
        int evenCount = pool.size() - oddIndices.size();
        if (evenCount >= requiredCount - 1 && !oddIndices.isEmpty()) {
            return Collections.singletonList(new HashSet<>(oddIndices));
        }
        return Collections.emptyList();
    }
}
