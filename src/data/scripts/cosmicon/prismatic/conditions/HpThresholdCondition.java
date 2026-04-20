package data.scripts.cosmicon.prismatic.conditions;

import data.scripts.Strings;
import data.scripts.cosmicon.prismatic.AvailabilityCondition;

public class HpThresholdCondition implements AvailabilityCondition {
    
    private final int threshold;
    private final boolean lessOrEqual;
    
    public HpThresholdCondition(int threshold, boolean lessOrEqual) {
        this.threshold = threshold;
        this.lessOrEqual = lessOrEqual;
    }
    
    public static HpThresholdCondition atOrBelow(int threshold) {
        return new HpThresholdCondition(threshold, true);
    }
    
    public static HpThresholdCondition atOrAbove(int threshold) {
        return new HpThresholdCondition(threshold, false);
    }
    
    @Override
    public boolean isAvailable(ConditionContext context) {
        if (lessOrEqual) {
            return context.currentHp <= threshold;
        } else {
            return context.currentHp >= threshold;
        }
    }
    
    @Override
    public String getDescription() {
        if (lessOrEqual) {
            return Strings.format("prismatic.condition.hp_threshold_low", threshold);
        } else {
            return Strings.format("prismatic.condition.hp_threshold_high", threshold);
        }
    }
}