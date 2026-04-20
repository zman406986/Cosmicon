package data.scripts.cosmicon.ai.profiles;

import data.scripts.cosmicon.ai.PrismaticDiceProfile;

public class DefaultPrismaticDiceProfile implements PrismaticDiceProfile {

    protected final String diceId;
    protected final String diceName;
    protected final int[] faces;

    public DefaultPrismaticDiceProfile(String id, String name, int[] faces) {
        this.diceId = id;
        this.diceName = name;
        this.faces = faces;
    }

    @Override
    public String getDiceId() {
        return diceId;
    }

    @Override
    public String getDiceName() {
        return diceName;
    }

    @Override
    public int[] getFaces() {
        return faces.clone();
    }

    @Override
    public int[] getTrueFaces() {
        return faces.clone();
    }

    @Override
    public boolean isTrueVersion() {
        return false;
    }

    @Override
    public int getMaxFace() {
        int max = 0;
        for (int f : faces) max = Math.max(max, f);
        return max;
    }

    @Override
    public float getExpectedValue() {
        float sum = 0;
        for (int f : faces) sum += f;
        return sum / faces.length;
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
}