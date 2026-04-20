package data.scripts.cosmicon.prismatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrismaticState {
    private int uses;
    private final Map<PrismaticDiceType, Integer> usesByType;
    private final List<PrismaticDiceInstance> rolledDice;
    private boolean modeActive;
    private PrismaticDiceType selectedType;
    private boolean useTrueVersion;
    private boolean doubleValueActive;
    private int instantDamage;
    private final List<PrismaticDiceInstance> mustSelectDice;
    
    public PrismaticState() {
        this.uses = 2;
        this.usesByType = new HashMap<>();
        this.rolledDice = new ArrayList<>();
        this.modeActive = false;
        this.selectedType = null;
        this.useTrueVersion = false;
        this.doubleValueActive = false;
        this.instantDamage = 0;
        this.mustSelectDice = new ArrayList<>();
    }
    
    public int getUses() { return uses; }
    public void setUses(int uses) { this.uses = uses; }
    public void decrementUses() { this.uses = Math.max(0, uses - 1); }
    public void incrementUses() { this.uses++; }
    
    public int getUsesByType(PrismaticDiceType type) { 
        return usesByType.getOrDefault(type, 0); 
    }
    public void setUsesByType(PrismaticDiceType type, int count) { 
        usesByType.put(type, count); 
    }
    public void decrementUsesByType(PrismaticDiceType type) {
        int current = usesByType.getOrDefault(type, 0);
        if (current > 0) usesByType.put(type, current - 1);
    }
    public void incrementUsesByType(PrismaticDiceType type) {
        usesByType.merge(type, 1, Integer::sum);
    }
    
    public List<PrismaticDiceInstance> getRolledDice() { return rolledDice; }
    public void addRolledDice(PrismaticDiceInstance dice) {
        rolledDice.add(dice);
        if (dice.isMustSelect()) {
            mustSelectDice.add(dice);
        }
    }
    
    public List<PrismaticDiceInstance> getSelectedDice() {
        List<PrismaticDiceInstance> selected = new ArrayList<>();
        for (PrismaticDiceInstance prismaticDiceInstance : rolledDice)
        {
            if (prismaticDiceInstance.isSelected())
            {
                selected.add(prismaticDiceInstance);
            }
        }
        return selected;
    }
    
    public List<PrismaticDiceInstance> getMustSelectDice() { return mustSelectDice; }
    
    public boolean selectDice(int index) {
        if (index < 0 || index >= rolledDice.size()) return false;
        
        PrismaticDiceInstance dice = rolledDice.get(index);
        dice.setSelected(!dice.isSelected());
        
        return true;
    }
    
    public boolean isModeActive() { return modeActive; }
    public void setModeActive(boolean active) { this.modeActive = active; }
    
    public PrismaticDiceType getSelectedType() { return selectedType; }
    public void setSelectedType(PrismaticDiceType type) { this.selectedType = type; }
    
    public boolean isUseTrueVersion() { return useTrueVersion; }
    public void setUseTrueVersion(boolean trueVersion) { this.useTrueVersion = trueVersion; }
    
    public boolean isDoubleValueActive() { return doubleValueActive; }
    public void setDoubleValueActive(boolean active) { this.doubleValueActive = active; }
    
    public int getInstantDamage() { return instantDamage; }
    public void addInstantDamage(int amount) { this.instantDamage += amount; }
    
    public void clear() {
        rolledDice.clear();
        mustSelectDice.clear();
        selectedType = null;
        useTrueVersion = false;
        doubleValueActive = false;
        instantDamage = 0;
    }
    
    public void clearRolledDice() {
        rolledDice.clear();
        mustSelectDice.clear();
    }
    
    public void reset() {
        clear();
        modeActive = false;
    }
}