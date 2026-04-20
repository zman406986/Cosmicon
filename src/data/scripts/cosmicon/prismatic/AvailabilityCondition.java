package data.scripts.cosmicon.prismatic;

import data.scripts.cosmicon.battle.BattleState.TurnType;

public interface AvailabilityCondition {
    
    boolean isAvailable(ConditionContext context);
    
    String getDescription();
    
    class ConditionContext {
        public final int currentHp;
        public final int maxHp;
        public final int turnNumber;
        public final TurnType turnType;
        public final int totalDamageTaken;
        public final java.util.Map<Integer, Integer> faceSelectionHistory;
        
        public ConditionContext(int hp, int maxHp, int turn, TurnType type,
                                int dmgTaken,
                                java.util.Map<Integer, Integer> history) {
            this.currentHp = hp;
            this.maxHp = maxHp;
            this.turnNumber = turn;
            this.turnType = type;
            this.totalDamageTaken = dmgTaken;
            this.faceSelectionHistory = history != null ? history : new java.util.HashMap<>();
        }
        
        public int getFaceSelectionCount(int faceValue) {
            return faceSelectionHistory.getOrDefault(faceValue, 0);
        }
        
        public float getHpPercentage() {
            return maxHp > 0 ? (float) currentHp / maxHp : 1f;
        }
    }
}