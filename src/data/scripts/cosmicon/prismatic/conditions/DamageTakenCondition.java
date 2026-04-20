package data.scripts.cosmicon.prismatic.conditions;

import data.scripts.Strings;
import data.scripts.cosmicon.prismatic.AvailabilityCondition;

public class DamageTakenCondition implements AvailabilityCondition {
    
    private final int threshold;
    
    public DamageTakenCondition(int threshold) {
        this.threshold = threshold;
    }
    
    public static DamageTakenCondition atLeast(int threshold) {
        return new DamageTakenCondition(threshold);
    }
    
    @Override
    public boolean isAvailable(ConditionContext context) {
        return context.totalDamageTaken >= threshold;
    }
    
    @Override
    public String getDescription() {
        return Strings.format("prismatic.condition.damage_taken", threshold);
    }
}