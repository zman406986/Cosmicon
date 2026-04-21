package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.List;

public class AISelectionVisualizer {
    
    private static final float DEFAULT_DELAY_PER_DICE = 0.15f;
    
    private List<Integer> plannedIndices;
    private int currentRevealIndex;
    private float revealTimer;
    private float delayPerDice;
    private boolean isRerollPhase;
    private boolean complete;
    private boolean started;
    
    public AISelectionVisualizer() {
        this.plannedIndices = new ArrayList<>();
        this.delayPerDice = DEFAULT_DELAY_PER_DICE;
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
        List<Integer> visible = new ArrayList<>();
        for (int i = 0; i < currentRevealIndex && i < plannedIndices.size(); i++) {
            visible.add(plannedIndices.get(i));
        }
        return visible;
    }
    
    /**
     * Returns all planned indices (for rendering positions).
     */
    public List<Integer> getAllPlannedIndices() {
        return new ArrayList<>(plannedIndices);
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
    
    public int getTotalCount() {
        return plannedIndices.size();
    }
    
    public int getRevealedCount() {
        return Math.min(currentRevealIndex, plannedIndices.size());
    }
    
    public void setDelayPerDice(float delay) {
        this.delayPerDice = delay;
    }
    
    public void forceComplete() {
        currentRevealIndex = plannedIndices.size();
        complete = true;
    }
}