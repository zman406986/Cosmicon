package data.scripts.cosmicon.prismatic.conditions;

import data.scripts.Strings;
import data.scripts.cosmicon.prismatic.AvailabilityCondition;

public class TurnCountCondition implements AvailabilityCondition {
    
    private final int threshold;
    private final boolean greaterOrEqual;
    
    public TurnCountCondition(int threshold, boolean greaterOrEqual) {
        this.threshold = threshold;
        this.greaterOrEqual = greaterOrEqual;
    }
    
    public static TurnCountCondition fromTurn(int threshold) {
        return new TurnCountCondition(threshold, true);
    }
    
    public static TurnCountCondition untilTurn(int threshold) {
        return new TurnCountCondition(threshold, false);
    }
    
    @Override
    public boolean isAvailable(ConditionContext context) {
        if (greaterOrEqual) {
            return context.turnNumber() >= threshold;
        } else {
            return context.turnNumber() <= threshold;
        }
    }
    
    @Override
    public String getDescription() {
        if (greaterOrEqual) {
            return Strings.format("prismatic.condition.turn_threshold_high", threshold);
        } else {
            return Strings.format("prismatic.condition.turn_threshold_low", threshold);
        }
    }
}