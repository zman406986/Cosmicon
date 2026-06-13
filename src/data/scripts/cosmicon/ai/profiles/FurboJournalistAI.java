package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.AttackRerollAI;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.util.CharacterIds;
import java.util.*;

public class FurboJournalistAI extends AttackRerollAI {

    @Override
    public String getCharacterId() {
        return CharacterIds.FURBO_JOURNALIST;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.furbo_journalist.name");
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
        boolean allOdd = true;
        for (int v : selectedValues) {
            if (v % 2 == 0) { allOdd = false; break; }
        }
        if (allOdd) {
            return PassiveEvaluation.triggered(4f, Strings.get("character.furbo_journalist.passive_desc_all_odd"));
        }
        return PassiveEvaluation.triggered(2f, Strings.get("character.furbo_journalist.passive_desc_damage"));
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (isAttacking || selectedValues.isEmpty()) return 0f;
        boolean allOdd = true;
        for (int v : selectedValues) {
            if (v % 2 == 0) { allOdd = false; break; }
        }
        return allOdd ? 4f : 2f;
    }

    @Override
    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        if (isAttacking) return Collections.emptyList();

        // Try to reroll even dice to achieve all-odd for the stronger instant damage
        List<Integer> evenIndices = new ArrayList<>();
        int oddCount = 0;
        for (int i = 0; i < pool.size(); i++) {
            if (pool.getValue(i) % 2 == 0) {
                evenIndices.add(i);
            } else {
                oddCount++;
            }
        }
        // If we have enough odd dice that rerolling the even ones could give all-odd
        if (oddCount >= requiredCount - 1 && !evenIndices.isEmpty()) {
            return Collections.singletonList(new HashSet<>(evenIndices));
        }
        return Collections.emptyList();
    }
}
