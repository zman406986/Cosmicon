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

    public static AttackRerollAI getRerollAI(String characterId) {
        return CharacterProfileRegistry.getRerollAI(characterId);
    }

    public static AIDecision makeDecision(
            List<Integer> diceValues,
            List<DiceType> diceTypes,
            int requiredSelectCount,
            String characterId,
            boolean isAttacking,
            int rerollsAvailable,
            BattleState state,
            boolean forPlayer) {

        CosmiconLogger.info("[AI] Decision request: character=%s, role=%s, dice=%s, need=%d, rerolls=%d",
            characterId, isAttacking ? "ATTACK" : "DEFEND", diceValues, requiredSelectCount, rerollsAvailable);

        CharacterAIProfile profile = getProfile(characterId);

        SelectionOptimizer.SelectionResult selection = SelectionOptimizer.optimalSelection(
            diceValues, diceTypes, requiredSelectCount, isAttacking, profile, state, forPlayer);

        Set<Integer> rerollIndices = Set.of();
        if (rerollsAvailable > 0) {
            AttackRerollAI rerollAI = getRerollAI(characterId);
            rerollIndices = rerollAI.planReroll(
                diceValues, diceTypes, requiredSelectCount, rerollsAvailable,
                isAttacking, state, forPlayer);
        }

        CosmiconLogger.info("[AI] Decision result: %s selected indices %s (sum=%d), reroll indices %s",
            characterId, selection.selectedIndices, selection.sumValue, rerollIndices);

        return new AIDecision(selection, rerollIndices, profile);
    }

    public static Set<Integer> recommendRerolls(
            List<Integer> diceValues,
            List<DiceType> diceTypes,
            int requiredCount,
            int rerollsAvailable,
            boolean isAttacking,
            BattleState state,
            boolean forPlayer) {

        AttackRerollAI rerollAI = CharacterProfileRegistry.getDefaultRerollAI();
        if (state != null) {
            var card = state.getCard(forPlayer);
            if (card != null) rerollAI = getRerollAI(card.getId());
        }

        Set<Integer> result = rerollAI.planReroll(
            diceValues, diceTypes, requiredCount, rerollsAvailable, isAttacking, state, forPlayer);
        CosmiconLogger.info("[AI] Reroll recommendation: indices %s, role: %s",
            result, isAttacking ? "attacker" : "defender");
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
