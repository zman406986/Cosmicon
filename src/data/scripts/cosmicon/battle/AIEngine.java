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
        
        CosmiconLogger.debug("AIEngine: executing selection for %s (%s), dice: %s", 
            charId, isAttacking ? "attack" : "defense", diceValues);
        
        AIDecision decision = CosmiconAICore.makeDecision(
            diceValues, diceTypes, requiredCount,
            charId, isAttacking, rerolls, 0);
        
        List<Boolean> selected = state.getDiceSelected(forPlayer);
        if (selected != null) {
            Collections.fill(selected, false);
        }
        
        for (int idx : decision.getSelectedIndicesList()) {
            if (selected != null && idx < selected.size()) {
                selected.set(idx, true);
            }
            state.recordFaceSelection(diceValues.get(idx), forPlayer);
        }
        
        CosmiconLogger.debug("AIEngine: %s selected indices %s, values: %s, sum: %d", 
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
            return new ArrayList<>();
        }
        
        boolean isAttacking = state.isAttacker(forPlayer);
        int rerolls = state.getRemainingRerolls(forPlayer);
        String charId = card.getId();
        
        CosmiconLogger.debug("AIEngine: planning selection for %s (%s), dice: %s", 
            charId, isAttacking ? "attack" : "defense", diceValues);
        
        AIDecision decision = CosmiconAICore.makeDecision(
            diceValues, diceTypes, requiredCount,
            charId, isAttacking, rerolls, 0);
        
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
        
        CosmiconLogger.debug("[AI_REROLL_DIAG] AIEngine.planReroll: forPlayer=%s, diceValues=%s, diceTypes=%s", 
            forPlayer, diceValues, diceTypes);
        CosmiconLogger.debug("[AI_REROLL_DIAG] requiredCount=%d, rerollsAvailable=%d, isAttacking=%s", 
            requiredCount, rerollsAvailable, state.isAttacker(forPlayer));
        
        if (diceValues == null || diceTypes == null || rerollsAvailable <= 0) {
            CosmiconLogger.debug("[AI_REROLL_DIAG] planReroll returning empty: diceValues=%s, diceTypes=%s, rerollsAvailable=%d", 
                diceValues, diceTypes, rerollsAvailable);
            return new ArrayList<>();
        }
        
        boolean isAttacking = state.isAttacker(forPlayer);
        CharacterCard card = state.getCard(forPlayer);
        String charId = card != null ? card.getId() : "unknown";
        
        CosmiconLogger.debug("AIEngine: planning reroll for %s (%s), dice: %s, rerolls left: %d", 
            charId, isAttacking ? "attack" : "defense", diceValues, rerollsAvailable);
        
        Set<Integer> rerollIndices = CosmiconAICore.recommendRerolls(
            diceValues, diceTypes, requiredCount, rerollsAvailable,
            isAttacking, 0, state, forPlayer);
        
        return new ArrayList<>(rerollIndices);
    }
    
    public PrismaticDecision planPrismaticUse(BattleState state, boolean forPlayer) {
        return AIPrismaticSelector.selectPrismaticDice(state, forPlayer);
    }
}