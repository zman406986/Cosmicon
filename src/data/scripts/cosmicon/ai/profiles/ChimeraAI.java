package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.AttackRerollAI;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.DiceEvaluator;
import java.util.*;

public class ChimeraAI extends AttackRerollAI {

    @Override
    public String getCharacterId() {
        return CharacterIds.CHIMERA;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.chimera.name");
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<data.scripts.cosmicon.battle.DiceType> selectedTypes, boolean isAttacking) {
        if (!isAttacking || selectedValues.isEmpty()) return PassiveEvaluation.notTriggered();
        if (DiceEvaluator.hasIdenticalNumbers(selectedValues)) {
            int bonus = 3;
            int[] freq = DiceEvaluator.frequencyArray(selectedValues);
            for (int v = 1; v <= 12; v++) {
                if (freq[v] >= 2 && v == 4) {
                    bonus = 7;
                    break;
                }
            }
            return PassiveEvaluation.triggered(bonus, Strings.get("character.chimera.passive_desc"));
        }
        return PassiveEvaluation.notTriggered();
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (!isAttacking || selectedValues.isEmpty()) return 0f;
        if (!DiceEvaluator.hasIdenticalNumbers(selectedValues)) return 0f;
        int[] freq = DiceEvaluator.frequencyArray(selectedValues);
        for (int v = 1; v <= 12; v++) {
            if (freq[v] >= 2 && v == 4) return 7f;
        }
        return 3f;
    }

    @Override
    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        if (!isAttacking) return Collections.emptyList();

        // Try to reroll non-matching dice to achieve identical numbers
        Set<Integer> result = findComboForMostCommonValue(pool, requiredCount);
        return result != null ? Collections.singletonList(result) : Collections.emptyList();
    }
}
