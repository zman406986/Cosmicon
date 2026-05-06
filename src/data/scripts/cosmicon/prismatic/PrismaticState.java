package data.scripts.cosmicon.prismatic;

import data.scripts.cosmicon.util.CosmiconLogger;
import java.util.HashMap;
import java.util.Map;

public class PrismaticState {
    private int uses;
    private final Map<PrismaticDiceType, Integer> usesByType;
    private boolean doubleValueActive;
    private int instantDamage;
    
    public PrismaticState() {
        this.uses = 2;
        this.usesByType = new HashMap<>();
        this.doubleValueActive = false;
        this.instantDamage = 0;
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
    
    public boolean isDoubleValueActive() { return doubleValueActive; }
    public void setDoubleValueActive(boolean active) { 
        this.doubleValueActive = active;
        CosmiconLogger.debug("Prismatic DoubleValue %s", active ? "activated" : "deactivated");
    }
    
    public int getInstantDamage() { return instantDamage; }
    public void addInstantDamage(int amount) { this.instantDamage += amount; }
    
    public void clear() {
        doubleValueActive = false;
        instantDamage = 0;
    }
    
}