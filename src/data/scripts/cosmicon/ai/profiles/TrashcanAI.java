package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.AttackRerollAI;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.DiceEvaluator;
import java.util.*;

public class TrashcanAI extends AttackRerollAI {

    @Override
    public String getCharacterId() {
        return CharacterIds.TRASHCAN;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.trashcan.name");
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (!isAttacking || selectedValues.isEmpty()) return PassiveEvaluation.notTriggered();
        if (DiceEvaluator.allEven(selectedValues)) {
            return PassiveEvaluation.triggered(4f, Strings.get("character.trashcan.passive_desc_even"));
        }
        return PassiveEvaluation.triggered(2f, Strings.get("character.trashcan.passive_desc_odd"));
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (!isAttacking || selectedValues.isEmpty()) return 0f;
        return DiceEvaluator.allEven(selectedValues) ? 4f : 2f;
    }

    @Override
    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        if (!isAttacking) return Collections.emptyList();
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
