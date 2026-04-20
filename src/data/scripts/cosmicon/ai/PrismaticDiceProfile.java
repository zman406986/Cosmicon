package data.scripts.cosmicon.ai;

public interface PrismaticDiceProfile {

    String diceId();

    String diceName();

    int[] faces();

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

    record AvailabilityContext(int currentHp, int maxHp, int turnNumber, boolean isAttacking, int totalDamageTaken,
                               int prismaticUsesRemaining, int faceCountSelected, int previousFaceFourCount,
                               int previousFaceOneCount)
    {

        public static AvailabilityContext defaultContext(boolean attacking)
        {
                return new AvailabilityContext(30, 30, 1, attacking, 0, 2, 0, 0, 0);
            }

            public float hpPercent()
            {
                return maxHp > 0 ? (float) currentHp / maxHp : 1f;
            }

            public boolean isLowHp(int threshold)
            {
                return currentHp <= threshold;
            }
        }
}