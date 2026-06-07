package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import data.scripts.cosmicon.ai.AIPrismaticSelector;
import data.scripts.cosmicon.ai.AIPrismaticSelector.PrismaticDecision;
import data.scripts.cosmicon.ai.CosmiconAICore;
import data.scripts.cosmicon.ai.CosmiconAICore.AIDecision;
import data.scripts.cosmicon.util.CosmiconLogger;

public class AIEngine {
    
    public AIEngine() {
    }
    
    public void executeSelection(BattleState state, boolean forPlayer) {
        List<Integer> diceValues = state.getDiceValues(forPlayer);
        List<DiceType> diceTypes = state.getDiceTypes(forPlayer);
        int requiredCount = state.getRequiredDiceCount(forPlayer);
        CharacterCard card = state.getCard(forPlayer);
        
        if (diceValues == null || diceTypes == null || card == null) return;
        
        boolean isAttacking = state.isAttacker(forPlayer);
        int rerolls = state.getRemainingRerolls(forPlayer);
        String charId = card.getId();
        
        CosmiconLogger.info("[AI] Executing selection for %s (%s), dice: %s", 
            charId, isAttacking ? "attack" : "defense", diceValues);
        
        AIDecision decision = CosmiconAICore.makeDecision(
            diceValues, diceTypes, requiredCount,
            charId, isAttacking, rerolls, state, forPlayer);
        
        List<Boolean> selected = state.getDiceSelected(forPlayer);
        if (selected != null && !shouldPreserveForcedSelections(state, forPlayer)) {
            Collections.fill(selected, false);
        }
        
        for (int idx : decision.getSelectedIndicesList()) {
            if (selected != null && idx < selected.size()) {
                selected.set(idx, true);
            }
            state.recordFaceSelection(diceValues.get(idx), forPlayer);
        }
        
        CosmiconLogger.info("[AI] Selection applied: %s selected indices %s, values: %s, sum: %d", 
            charId, decision.getSelectedIndicesList(), decision.selection.selectedValues, decision.selection.sumValue);
    }
    
    /**
     * Compute AI selection decision without applying it yet.
     * Returns the indices the AI would select.
     */
    public List<Integer> planSelection(BattleState state, boolean forPlayer) {
        List<Integer> diceValues = state.getDiceValues(forPlayer);
        List<DiceType> diceTypes = state.getDiceTypes(forPlayer);
        int requiredCount = state.getRequiredDiceCount(forPlayer);
        CharacterCard card = state.getCard(forPlayer);
        
        if (diceValues == null || diceTypes == null || card == null) {
            return Collections.emptyList();
        }
        
        boolean isAttacking = state.isAttacker(forPlayer);
        int rerolls = state.getRemainingRerolls(forPlayer);
        String charId = card.getId();
        
        CosmiconLogger.info("[AI] Planning selection for %s (%s), dice: %s", 
            charId, isAttacking ? "attack" : "defense", diceValues);
        
        AIDecision decision = CosmiconAICore.makeDecision(
            diceValues, diceTypes, requiredCount,
            charId, isAttacking, rerolls, state, forPlayer);
        
        return decision.getSelectedIndicesList();
    }
    
    /**
     * Compute AI reroll decision without applying it yet.
     * Returns the indices the AI would reroll.
     */
    public List<Integer> planReroll(BattleState state, boolean forPlayer) {
        List<Integer> diceValues = state.getDiceValues(forPlayer);
        List<DiceType> diceTypes = state.getDiceTypes(forPlayer);
        int requiredCount = state.getRequiredDiceCount(forPlayer);
        int rerollsAvailable = state.getRemainingRerolls(forPlayer);
        
        if (diceValues == null || diceTypes == null || rerollsAvailable <= 0) {
            return Collections.emptyList();
        }
        
        boolean isAttacking = state.isAttacker(forPlayer);
        CharacterCard card = state.getCard(forPlayer);
        String charId = card != null ? card.getId() : "unknown";
        
        CosmiconLogger.info("[AI] Planning reroll for %s (%s), dice: %s, rerolls left: %d", 
            charId, isAttacking ? "attack" : "defense", diceValues, rerollsAvailable);
        
        Set<Integer> rerollIndices = CosmiconAICore.recommendRerolls(
            diceValues, diceTypes, requiredCount, rerollsAvailable,
            isAttacking, state, forPlayer);
        
        return new ArrayList<>(rerollIndices);
    }
    
    public PrismaticDecision planPrismaticUse(BattleState state, boolean forPlayer) {
        PrismaticDecision decision = AIPrismaticSelector.selectPrismaticDice(state, forPlayer);
        if (decision != null) {
            CosmiconLogger.info("[AI] Prismatic decision: %s, score=%.1f, use=%s (for %s)",
                decision.instance() != null ? decision.instance().type : "none",
                decision.score(), decision.shouldUse(),
                forPlayer ? "player" : "opponent");
        }
        return decision;
    }

    private boolean shouldPreserveForcedSelections(BattleState state, boolean forPlayer) {
        for (int i = 0; i < state.getDiceValues(forPlayer).size(); i++) {
            var pd = state.getPrismaticDiceAt(i, forPlayer);
            if (pd != null && pd.isMustSelect()) {
                return true;
            }
        }
        return false;
    }
}