package data.scripts.cosmicon.prismatic;

import data.scripts.cosmicon.util.CosmiconRandom;

public class PrismaticDiceInstance {
    
    public final PrismaticDiceType type;
    public final boolean isTrueVersion;
    public final int rolledFace;
    public final int faceIndex;
    public final boolean isSpecialFace;
    
    private boolean mustSelect;
    
    public PrismaticDiceInstance(PrismaticDiceType type, boolean isTrueVersion,
                                 int rolledFace, int faceIndex) {
        this.type = type;
        this.isTrueVersion = isTrueVersion;
        this.rolledFace = rolledFace;
        this.faceIndex = faceIndex;
        this.isSpecialFace = type.isSpecialFace(faceIndex, isTrueVersion);
        this.mustSelect = false;
    }
    
    public static PrismaticDiceInstance roll(PrismaticDiceType type, boolean isTrueVersion,
                                              java.util.Random random) {
        int[] faces = type.getFaces(isTrueVersion);
        int index = random.nextInt(faces.length);
        int face = faces[index];
        return new PrismaticDiceInstance(type, isTrueVersion, face, index);
    }
    
    public static PrismaticDiceInstance roll(PrismaticDiceType type, boolean isTrueVersion) {
        return roll(type, isTrueVersion, CosmiconRandom.getRandom());
    }
    
    public boolean isMustSelect() {
        return mustSelect;
    }
    
    public void setMustSelect(boolean mustSelect) {
        this.mustSelect = mustSelect;
    }
    
    public PrismaticEffect getEffect() {
        return type.getEffect();
    }
    
    @Override
    public String toString() {
        return "PrismaticDice[" + type.getId() + ":" + rolledFace + 
               (isSpecialFace ? "*" : "") + "]";
    }
}