package data.scripts.cosmicon.prismatic.conditions;

import data.scripts.Strings;

public class TurnCountCondition extends ThresholdCondition {
    
    public TurnCountCondition(int threshold, boolean greaterOrEqual) {
        super(threshold, greaterOrEqual);
    }
    
    public static TurnCountCondition fromTurn(int threshold) {
        return new TurnCountCondition(threshold, true);
    }
    
    public static TurnCountCondition untilTurn(int threshold) {
        return new TurnCountCondition(threshold, false);
    }
    
    @Override
    public boolean isAvailable(ConditionContext context) {
        return compare(context.turnNumber());
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