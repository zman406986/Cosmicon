package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.List;

import data.scripts.CosmiconConfig;

public class AISelectionVisualizer {
    
    private final List<Integer> plannedIndices;
    private int currentRevealIndex;
    private float revealTimer;
    private final float delayPerDice;
    private boolean isRerollPhase;
    private boolean complete;
    private boolean started;
    
    public AISelectionVisualizer() {
        this.plannedIndices = new ArrayList<>();
        this.delayPerDice = CosmiconConfig.AI_REVEAL_PER_DICE_DELAY;
        reset();
    }
    
    public void reset() {
        plannedIndices.clear();
        currentRevealIndex = 0;
        revealTimer = 0f;
        complete = false;
        started = false;
        isRerollPhase = false;
    }
    
    public void planSelection(List<Integer> indices, boolean isRerollPhase) {
        reset();
        if (indices != null) {
            this.plannedIndices.addAll(indices);
        }
        this.isRerollPhase = isRerollPhase;
        this.started = true;
        this.revealTimer = delayPerDice;  // Start timer so first dice shows immediately after first advance
    }
    
    public void advance(float amount) {
        if (!started || complete) return;
        
        revealTimer -= amount;
        
        while (revealTimer <= 0f && currentRevealIndex < plannedIndices.size()) {
            currentRevealIndex++;
            if (currentRevealIndex < plannedIndices.size()) {
                revealTimer += delayPerDice;
            }
        }
        
        if (currentRevealIndex >= plannedIndices.size()) {
            complete = true;
        }
    }
    
    /**
     * Returns indices that have been visually revealed so far.
     */
    public List<Integer> getVisibleIndices() {
        int limit = Math.min(currentRevealIndex, plannedIndices.size());
        return plannedIndices.subList(0, limit);
    }
    
    public boolean isComplete() {
        return complete;
    }
    
    public boolean hasStarted() {
        return started;
    }
    
    public boolean isRerollPhase() {
        return isRerollPhase;
    }
}