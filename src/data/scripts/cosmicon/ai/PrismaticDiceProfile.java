package data.scripts.cosmicon.ai;

import data.scripts.cosmicon.battle.DiceType;
import java.util.List;

public interface PrismaticDiceProfile {

    String getDiceId();

    String getDiceName();

    int[] getFaces();

    int[] getTrueFaces();

    boolean isTrueVersion();

    int getMaxFace();

    float getExpectedValue();

    boolean isAvailable(AvailabilityContext context);

    boolean mustBeSelected();

    default float getPriorityScore(int faceValue, boolean isAttacking) {
        return faceValue;
    }

    float getEffectValue(int faceValue, boolean isAttacking);

    boolean isAttackOnly();

    boolean isDefenseOnly();

    default boolean hasSpecialEffect() {
        return false;
    }

    default String getEffectDescription() {
        return "";
    }

    class AvailabilityContext {
        public final int currentHp;
        public final int maxHp;
        public final int turnNumber;
        public final boolean isAttacking;
        public final int totalDamageTaken;
        public final int prismaticUsesRemaining;
        public final int faceCountSelected;
        public final int previousFaceFourCount;
        public final int previousFaceOneCount;

        public AvailabilityContext(int hp, int maxHp, int turn, boolean attacking, 
                                    int dmgTaken, int uses, int faceCount, int fourCount, int oneCount) {
            this.currentHp = hp;
            this.maxHp = maxHp;
            this.turnNumber = turn;
            this.isAttacking = attacking;
            this.totalDamageTaken = dmgTaken;
            this.prismaticUsesRemaining = uses;
            this.faceCountSelected = faceCount;
            this.previousFaceFourCount = fourCount;
            this.previousFaceOneCount = oneCount;
        }

        public static AvailabilityContext defaultContext(boolean attacking) {
            return new AvailabilityContext(30, 30, 1, attacking, 0, 2, 0, 0, 0);
        }

        public float hpPercent() {
            return maxHp > 0 ? (float) currentHp / maxHp : 1f;
        }

        public boolean isLowHp(int threshold) {
            return currentHp <= threshold;
        }
    }
}