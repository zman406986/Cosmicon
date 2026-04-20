package data.scripts.cosmicon.prismatic.conditions;

import data.scripts.Strings;
import data.scripts.cosmicon.prismatic.AvailabilityCondition;

public class AlwaysAvailableCondition implements AvailabilityCondition {
    
    public static final AlwaysAvailableCondition INSTANCE = new AlwaysAvailableCondition();
    
    private AlwaysAvailableCondition() {}
    
    @Override
    public boolean isAvailable(ConditionContext context) {
        return true;
    }
    
    @Override
    public String getDescription() {
        return Strings.get("prismatic.condition.always");
    }
}