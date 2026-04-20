package data.scripts.cosmicon.ai;

import data.scripts.cosmicon.ai.profiles.CharacterProfileRegistry;
import data.scripts.cosmicon.battle.DiceType;
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
        
        CharacterAIProfile profile = getProfile(characterId);

        SelectionOptimizer.SelectionResult selection = SelectionOptimizer.optimalSelection(
            diceValues, diceTypes, requiredSelectCount, isAttacking, profile);

        Set<Integer> rerollIndices = Set.of();
        if (rerollsAvailable > 0) {
            int effectiveTarget = targetSum > 0 ? targetSum : profile.getTargetThreshold(isAttacking);
            rerollIndices = RerollOptimizer.optimalRerolls(
                diceValues, diceTypes, requiredSelectCount, rerollsAvailable, effectiveTarget, isAttacking);
        }

        return new AIDecision(selection, rerollIndices, profile);
    }

    public static Set<Integer> recommendRerolls(
            List<Integer> diceValues,
            List<DiceType> diceTypes,
            int requiredCount,
            int rerollsAvailable,
            boolean isAttacking,
            int targetSum) {
        
        return RerollOptimizer.optimalRerolls(diceValues, diceTypes, requiredCount, rerollsAvailable, targetSum, isAttacking);
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