package data.scripts.cosmicon.prismatic.conditions;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.TurnState.TurnType;
import data.scripts.cosmicon.prismatic.AvailabilityCondition;

public class ActionTypeCondition implements AvailabilityCondition {

    private static final ActionTypeCondition DEFENSE = new ActionTypeCondition(TurnType.DEFENSE);
    private static final ActionTypeCondition ATTACK = new ActionTypeCondition(TurnType.ATTACK);

    private final TurnType requiredType;

    private ActionTypeCondition(TurnType requiredType) {
        this.requiredType = requiredType;
    }

    public static ActionTypeCondition defenseOnly() {
        return DEFENSE;
    }

    public static ActionTypeCondition attackOnly() {
        return ATTACK;
    }
    
    @Override
    public boolean isAvailable(ConditionContext context) {
        return context.turnType() == requiredType;
    }
    
    @Override
    public String getDescription() {
        if (requiredType == TurnType.DEFENSE) {
            return Strings.get("prismatic.condition.defense_only");
        } else {
            return Strings.get("prismatic.condition.attack_only");
        }
    }
}