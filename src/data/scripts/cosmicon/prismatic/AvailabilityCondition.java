package data.scripts.cosmicon.prismatic;

import data.scripts.cosmicon.battle.BattleState.TurnType;

import java.util.HashMap;
import java.util.Map;

public interface AvailabilityCondition {
    
    boolean isAvailable(ConditionContext context);
    
    String getDescription();

    record ConditionContext(int currentHp, int maxHp, int turnNumber, TurnType turnType, int totalDamageTaken,
                            Map<Integer, Integer> faceSelectionHistory)
    {
            public ConditionContext(int currentHp, int maxHp, int turnNumber, TurnType turnType,
                                    int totalDamageTaken,
                                    Map<Integer, Integer> faceSelectionHistory)
            {
                this.currentHp = currentHp;
                this.maxHp = maxHp;
                this.turnNumber = turnNumber;
                this.turnType = turnType;
                this.totalDamageTaken = totalDamageTaken;
                this.faceSelectionHistory = faceSelectionHistory != null ? faceSelectionHistory : new HashMap<>();
            }

        public int getFaceSelectionCount(int faceValue)
        {
                return faceSelectionHistory.getOrDefault(faceValue, 0);
            }
        }
}