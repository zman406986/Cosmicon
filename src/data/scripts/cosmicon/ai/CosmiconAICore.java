package data.scripts.cosmicon.ai;

import data.scripts.cosmicon.ai.profiles.CharacterProfileRegistry;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.CosmiconLogger;
import java.util.*;

public final class CosmiconAICore {

    private CosmiconAICore() {}

    public static CharacterAIProfile getProfile(String characterId) {
        return CharacterProfileRegistry.get(characterId);
    }

    public static AIDecision makeDecision(
            List<Integer> diceValues,
            List<DiceType> diceTypes,
            int requiredSelectCount,
            String characterId,
            boolean isAttacking,
            int rerollsAvailable,
            int targetSum) {
        
        CosmiconLogger.debug("AI Decision: character=%s, role=%s, dice=%s, need=%d, rerolls=%d", 
            characterId, isAttacking ? "ATTACK" : "DEFEND", diceValues, requiredSelectCount, rerollsAvailable);
        
        CharacterAIProfile profile = getProfile(characterId);

        SelectionOptimizer.SelectionResult selection = SelectionOptimizer.optimalSelection(
            diceValues, diceTypes, requiredSelectCount, isAttacking, profile);

        Set<Integer> rerollIndices = Set.of();
        if (rerollsAvailable > 0) {
            int effectiveTarget = targetSum > 0 ? targetSum : profile.getTargetThreshold(isAttacking);
            rerollIndices = RerollOptimizer.optimalRerolls(
                diceValues, diceTypes, requiredSelectCount, rerollsAvailable, effectiveTarget, isAttacking);
        }

        CosmiconLogger.debug("AI Decision result: %s selected indices %s (sum=%d), reroll indices %s", 
            characterId, selection.selectedIndices, selection.sumValue, rerollIndices);
        
        return new AIDecision(selection, rerollIndices, profile);
    }

    public static Set<Integer> recommendRerolls(
            List<Integer> diceValues,
            List<DiceType> diceTypes,
            int requiredCount,
            int rerollsAvailable,
            boolean isAttacking,
            int targetSum) {
        
        Set<Integer> result = RerollOptimizer.optimalRerolls(diceValues, diceTypes, requiredCount, rerollsAvailable, targetSum, isAttacking);
        CosmiconLogger.debug("AI reroll recommendation: indices %s, role: %s, target: %d", 
            result, isAttacking ? "attacker" : "defender", targetSum);
        return result;
    }

    public static Set<Integer> recommendRerolls(
            List<Integer> diceValues,
            List<DiceType> diceTypes,
            int requiredCount,
            int rerollsAvailable,
            boolean isAttacking,
            int targetSum,
            BattleState state,
            boolean forPlayer) {
        
        Set<Integer> result = RerollOptimizer.optimalRerolls(
            diceValues, diceTypes, requiredCount, rerollsAvailable, targetSum, isAttacking, state, forPlayer);
        CosmiconLogger.debug("AI reroll recommendation (with prismatic): indices %s, role: %s, target: %d", 
            result, isAttacking ? "attacker" : "defender", targetSum);
        return result;
    }

    public static final class AIDecision {
        public final SelectionOptimizer.SelectionResult selection;
        public final Set<Integer> rerollIndices;
        public final CharacterAIProfile profile;

        private AIDecision(SelectionOptimizer.SelectionResult selection, Set<Integer> rerollIndices, CharacterAIProfile profile) {
            this.selection = selection;
            this.rerollIndices = rerollIndices;
            this.profile = profile;
        }

        public List<Integer> getSelectedIndicesList() {
            return selection.getSelectedIndicesList();
        }
    }
}