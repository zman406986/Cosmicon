package data.scripts.cosmicon.prismatic.conditions;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleState.TurnType;
import data.scripts.cosmicon.prismatic.AvailabilityCondition;

public class ActionTypeCondition implements AvailabilityCondition {
    
    private final TurnType requiredType;
    
    public ActionTypeCondition(TurnType requiredType) {
        this.requiredType = requiredType;
    }
    
    public static ActionTypeCondition defenseOnly() {
        return new ActionTypeCondition(TurnType.DEFENSE);
    }
    
    public static ActionTypeCondition attackOnly() {
        return new ActionTypeCondition(TurnType.ATTACK);
    }
    
    @Override
    public boolean isAvailable(ConditionContext context) {
        return context.turnType == requiredType;
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