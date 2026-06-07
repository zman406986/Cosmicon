package data.scripts.cosmicon.prismatic.conditions;

import data.scripts.Strings;

public class HpThresholdCondition extends ThresholdCondition {

    public HpThresholdCondition(int threshold, boolean lessOrEqual) {
        super(threshold, !lessOrEqual);
    }

    public static HpThresholdCondition atOrBelow(int threshold) {
        return new HpThresholdCondition(threshold, true);
    }

    @Override
    public boolean isAvailable(ConditionContext context) {
        return compare(context.currentHp());
    }

    @Override
    public String getDescription() {
        if (!greaterOrEqual) {
            return Strings.format("prismatic.condition.hp_threshold_low", threshold);
        } else {
            return Strings.format("prismatic.condition.hp_threshold_high", threshold);
        }
    }
}