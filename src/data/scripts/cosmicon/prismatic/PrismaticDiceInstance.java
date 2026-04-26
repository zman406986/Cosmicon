package data.scripts.cosmicon.prismatic;

public class PrismaticDiceInstance {
    
    public final PrismaticDiceType type;
    public final boolean isTrueVersion;
    public final int rolledFace;
    public final int faceIndex;
    public final boolean isSpecialFace;
    
    private boolean selected;
    private boolean mustSelect;
    
    public PrismaticDiceInstance(PrismaticDiceType type, boolean isTrueVersion,
                                 int rolledFace, int faceIndex) {
        this.type = type;
        this.isTrueVersion = isTrueVersion;
        this.rolledFace = rolledFace;
        this.faceIndex = faceIndex;
        this.isSpecialFace = type.isSpecialFace(faceIndex, isTrueVersion);
        this.selected = false;
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
        return roll(type, isTrueVersion, new java.util.Random());
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    public boolean isMustSelect() {
        return mustSelect;
    }
    
    public void setMustSelect(boolean mustSelect) {
        this.mustSelect = mustSelect;
    }
    
    public boolean shouldTriggerEffect() {
        return isSpecialFace && selected;
    }
    
    public PrismaticEffect getEffect() {
        return type.getEffect();
    }
    
    @Override
    public String toString() {
        return "PrismaticDice[" + type.getId() + ":" + rolledFace + 
               (isSpecialFace ? "*" : "") + (selected ? " (selected)" : "") + "]";
    }
}