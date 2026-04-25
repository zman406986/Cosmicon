package data.scripts.cosmicon.prismatic.conditions;

import data.scripts.Strings;

public class DamageTakenCondition extends ThresholdCondition {
    
    public DamageTakenCondition(int threshold) {
        super(threshold, true);
    }
    
    public static DamageTakenCondition atLeast(int threshold) {
        return new DamageTakenCondition(threshold);
    }
    
    @Override
    public boolean isAvailable(ConditionContext context) {
        return compare(context.totalDamageTaken());
    }
    
    @Override
    public String getDescription() {
        return Strings.format("prismatic.condition.damage_taken", threshold);
    }
}