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

public class AcheronAI extends AttackRerollAI {

    @Override
    public String getCharacterId() {
        return CharacterIds.ACHERON;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.acheron.name");
    }

    @Override
    public float getRiskTolerance() {
        return 0.4f;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return PassiveEvaluation.notTriggered();
        PassiveResult result = PassiveEvaluator.evaluateForCharacter(getCharacterId(), selectedValues, isAttacking);
        return PassiveEvaluator.toPassiveEvaluation(result,
            DiceEvaluator.allDiceEqualFour(selectedValues) ? Strings.get("character.acheron.passive_desc") : "");
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return 0f;
        return DiceEvaluator.allDiceEqualFour(selectedValues) ? 20f : 0f;
    }

    @Override
    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        if (!isAttacking) return Collections.emptyList();
        List<Integer> nonFourIndices = new ArrayList<>();
        int fourCount = 0;
        for (int i = 0; i < pool.size(); i++) {
            if (pool.getValue(i) == 4) fourCount++;
            else nonFourIndices.add(i);
        }
        if (fourCount >= requiredCount - 1 && fourCount < requiredCount && !nonFourIndices.isEmpty()) {
            return Collections.singletonList(new HashSet<>(nonFourIndices));
        }
        return Collections.emptyList();
    }
}
