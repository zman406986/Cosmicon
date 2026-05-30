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

    private boolean isUnyieldingUnneeded(BattleState state, boolean forPlayer) {
        if (state == null) return false;
        if (!state.isPhainonUnyieldingAvailable(forPlayer)) return true;
        int currentHp = forPlayer ? state.getPlayerHp() : state.getOpponentHp();
        int attackValue = state.getAttackValue();
        return attackValue < currentHp;
    }

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
    public boolean isDefensePassive() {
        return true;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (selectedValues.isEmpty()) return PassiveEvaluation.notTriggered();
        if (isAttacking) {
            return PassiveEvaluation.triggered(15f, Strings.get("character.phainon.passive_desc"));
        }
        PassiveResult result = PassiveEvaluator.evaluateForCharacter(getCharacterId(), selectedValues, false);
        return PassiveEvaluator.toPassiveEvaluation(result,
            DiceEvaluator.allSame(selectedValues) ? Strings.get("character.phainon.passive_desc") : "");
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking, BattleState state, boolean forPlayer) {
        if (!isAttacking && isUnyieldingUnneeded(state, forPlayer)) {
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
        if (!isAttacking && isUnyieldingUnneeded(state, forPlayer)) {
            return 0f;
        }
        return getPassiveBonusValue(selectedValues, isAttacking);
    }

    @Override
    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        if (isAttacking) return Collections.emptyList();
        if (isUnyieldingUnneeded(state, forPlayer)) return Collections.emptyList();
        Set<Integer> result = findComboForMostCommonValue(pool, requiredCount);
        return result != null ? Collections.singletonList(result) : Collections.emptyList();
    }
}
