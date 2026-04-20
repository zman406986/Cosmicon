package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.PrismaticDiceProfile;

public class AbsoluteSixProfile implements PrismaticDiceProfile {

    private static final int[] FACES = {6, 6, 6, 6, 6, 6};
    private static final int MAX_FACE = 6;

    @Override
    public String diceId() {
        return "absolute_six";
    }

    @Override
    public String diceName() {
        return Strings.get("prismatic.absolute_six.name");
    }

    @Override
    public int[] faces() {
        return FACES.clone();
    }

    @Override
    public int[] getTrueFaces() {
        return FACES.clone();
    }

    @Override
    public boolean isTrueVersion() {
        return true;
    }

    @Override
    public int getMaxFace() {
        return MAX_FACE;
    }

    @Override
    public float getExpectedValue() {
        return 6.0f;
    }

    @Override
    public boolean isAvailable(AvailabilityContext context) {
        return true;
    }

    @Override
    public boolean mustBeSelected() {
        return false;
    }

    @Override
    public float getPriorityScore(int faceValue, 
                                   boolean isAttacking) {
        return 6.0f;
    }

    @Override
    public float getEffectValue(int faceValue, 
                                boolean isAttacking) {
        return 0f;
    }

    @Override
    public boolean isAttackOnly() {
        return false;
    }

    @Override
    public boolean isDefenseOnly() {
        return false;
    }

    @Override
    public boolean hasSpecialEffect() {
        return false;
    }

    @Override
    public String getEffectDescription() {
        return Strings.get("prismatic.absolute_six.effect_desc");
    }
}