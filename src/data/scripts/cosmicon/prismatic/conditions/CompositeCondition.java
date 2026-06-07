package data.scripts.cosmicon.prismatic.conditions;

import data.scripts.Strings;
import data.scripts.cosmicon.prismatic.AvailabilityCondition;

public class CompositeCondition implements AvailabilityCondition {

    private final AvailabilityCondition[] conditions;
    private final boolean requireAll;

    public CompositeCondition(boolean requireAll, AvailabilityCondition... conditions) {
        this.conditions = conditions;
        this.requireAll = requireAll;
    }

    public CompositeCondition(AvailabilityCondition... conditions) {
        this(true, conditions);
    }

    @Override
    public boolean isAvailable(ConditionContext context) {
        if (requireAll) {
            for (AvailabilityCondition condition : conditions) {
                if (!condition.isAvailable(context)) {
                    return false;
                }
            }
            return true;
        } else {
            for (AvailabilityCondition condition : conditions) {
                if (condition.isAvailable(context)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < conditions.length; i++) {
            if (i > 0) {
                sb.append(requireAll
                    ? Strings.get("prismatic.condition.composite_and_separator")
                    : Strings.get("prismatic.condition.composite_or_separator"));
            }
            sb.append(conditions[i].getDescription());
        }
        return sb.toString();
    }
}
