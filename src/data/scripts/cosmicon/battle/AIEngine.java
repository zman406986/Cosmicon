package data.scripts.cosmicon.battle;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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
    
    public void executeReroll(BattleState state, boolean forPlayer) {
        List<Integer> diceValues = state.getDiceValues(forPlayer);
        List<DiceType> diceTypes = state.getDiceTypes(forPlayer);
        int requiredCount = state.getRequiredDiceCount(forPlayer);
        int rerollsAvailable = state.getRemainingRerolls(forPlayer);
        
        if (diceValues == null || diceTypes == null || rerollsAvailable <= 0) return;
        
        boolean isAttacking = state.isAttacker(forPlayer);
        CharacterCard card = state.getCard(forPlayer);
        String charId = card != null ? card.getId() : "unknown";
        
        CosmiconLogger.debug("AIEngine: executing reroll for %s (%s), dice: %s, rerolls left: %d", 
            charId, isAttacking ? "attack" : "defense", diceValues, rerollsAvailable);
        
        Set<Integer> rerollIndices = CosmiconAICore.recommendRerolls(
            diceValues, diceTypes, requiredCount, rerollsAvailable,
            isAttacking, 0);
        
        if (!rerollIndices.isEmpty()) {
            CosmiconLogger.debug("AIEngine: %s rerolling indices %s", charId, rerollIndices);
            
            List<Boolean> selected = state.getDiceSelected(forPlayer);
            if (selected != null) {
                Collections.fill(selected, false);
                for (int idx : rerollIndices) {
                    if (idx < selected.size()) {
                        selected.set(idx, true);
                    }
                }
            }
            
            DiceRoller roller = state.getDiceRoller();
            if (roller != null) {
                roller.rerollSelected(state, forPlayer);
            }
            
            CosmiconLogger.debug("AIEngine: %s reroll complete, new dice: %s", charId, state.getDiceValues(forPlayer));
        } else {
            CosmiconLogger.debug("AIEngine: %s chose not to reroll", charId);
        }
    }
}