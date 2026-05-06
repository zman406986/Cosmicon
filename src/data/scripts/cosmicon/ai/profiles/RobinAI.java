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

public class RobinAI extends AttackRerollAI {

    @Override
    public String getCharacterId() {
        return CharacterIds.ROBIN;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.robin.name");
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return PassiveEvaluation.notTriggered();
        PassiveResult result = PassiveEvaluator.evaluateForCharacter(getCharacterId(), selectedValues, isAttacking);
        return PassiveEvaluator.toPassiveEvaluation(result,
            DiceEvaluator.allEven(selectedValues) ? Strings.get("character.robin.passive_desc") : "");
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return 0f;
        return DiceEvaluator.allEven(selectedValues) ? 10f : 0f;
    }

    @Override
    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        if (!isAttacking) return Collections.emptyList();
        List<Integer> oddIndices = new ArrayList<>();
        int evenCount = 0;
        for (int i = 0; i < pool.size(); i++) {
            if (pool.getValue(i) % 2 != 0) {
                oddIndices.add(i);
            } else {
                evenCount++;
            }
        }
        if (evenCount >= requiredCount - 1 && oddIndices.size() <= 2 && !oddIndices.isEmpty()) {
            return Collections.singletonList(new HashSet<>(oddIndices));
        }
        return Collections.emptyList();
    }
}
