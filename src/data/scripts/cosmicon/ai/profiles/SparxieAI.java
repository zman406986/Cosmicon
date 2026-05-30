package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.DiceEvaluator;
import java.util.*;

public class SparxieAI extends SimplePassiveAI {

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
    public float getPassiveTriggerScore() {
        return 10f;
    }

    @Override
    protected boolean checkPassiveCondition(List<Integer> selectedValues, boolean isAttacking) {
        return DiceEvaluator.hasIdenticalNumbers(selectedValues);
    }

    @Override
    protected float computePassiveBonus(List<Integer> selectedValues, boolean isAttacking) {
        return 5f;
    }

    @Override
    protected String getPassiveDescription(List<Integer> selectedValues, boolean isAttacking) {
        return Strings.get("character.sparxie.passive_desc");
    }

    @Override
    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        Set<Integer> result = findComboForMostCommonValue(pool, requiredCount);
        return result != null ? Collections.singletonList(result) : Collections.emptyList();
    }
}
