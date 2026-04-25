package data.scripts.cosmicon.prismatic.conditions;

import data.scripts.cosmicon.prismatic.AvailabilityCondition;

public abstract class ThresholdCondition implements AvailabilityCondition {
    
    protected final int threshold;
    protected final boolean greaterOrEqual;
    
    protected ThresholdCondition(int threshold, boolean greaterOrEqual) {
        this.threshold = threshold;
        this.greaterOrEqual = greaterOrEqual;
    }
    
    protected boolean compare(int value) {
        return greaterOrEqual ? value >= threshold : value <= threshold;
    }
}