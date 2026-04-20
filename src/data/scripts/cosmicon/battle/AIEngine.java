package data.scripts.cosmicon.battle;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import data.scripts.cosmicon.ai.CosmiconAICore;
import data.scripts.cosmicon.ai.CosmiconAICore.AIDecision;

public class AIEngine {
    
    public AIEngine() {
    }
    
    public void executeSelection(BattleState state, boolean forPlayer) {
        List<Integer> diceValues = state.getDiceValues(forPlayer);
        List<DiceType> diceTypes = state.getDiceTypes(forPlayer);
        int requiredCount = state.getRequiredDiceCount(forPlayer);
        CharacterCard card = state.getCard(forPlayer);
        
        if (diceValues == null || diceTypes == null || card == null) return;
        
        boolean isAttacking = (forPlayer && state.isPlayerAttacker()) || (!forPlayer && !state.isPlayerAttacker());
        int rerolls = state.getRemainingRerolls(forPlayer);
        
        AIDecision decision = CosmiconAICore.makeDecision(
            diceValues, diceTypes, requiredCount,
            card.getId(), isAttacking, rerolls, 0);
        
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
    }
    
    public void executeReroll(BattleState state, boolean forPlayer) {
        List<Integer> diceValues = state.getDiceValues(forPlayer);
        List<DiceType> diceTypes = state.getDiceTypes(forPlayer);
        int requiredCount = state.getRequiredDiceCount(forPlayer);
        CharacterCard card = state.getCard(forPlayer);
        int rerollsAvailable = state.getRemainingRerolls(forPlayer);
        
        if (diceValues == null || diceTypes == null || card == null || rerollsAvailable <= 0) return;
        
        boolean isAttacking = (forPlayer && state.isPlayerAttacker()) || (!forPlayer && !state.isPlayerAttacker());
        
        Set<Integer> rerollIndices = CosmiconAICore.recommendRerolls(
            diceValues, diceTypes, requiredCount, rerollsAvailable,
            card.getId(), isAttacking, 0);
        
        if (!rerollIndices.isEmpty()) {
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
        }
    }
}