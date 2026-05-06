package data.scripts.cosmicon.battle;

public enum DiceType {
    BLUE_D4(4, 80f),
    PURPLE_D6(6, 60f),
    ORANGE_D8(8, 80f),
    RED_D12(12, 60f),
    PRISMATIC(6, 60f);

    private final int maxFace;
    private final float displaySize;

    DiceType(int maxFace, float displaySize) {
        this.maxFace = maxFace;
        this.displaySize = displaySize;
    }

    public float getDisplaySize() {
        return displaySize;
    }

    public int getMaxFace() {
        return maxFace;
    }

    public String getSpritePrefix() {
        return switch (this) {
            case BLUE_D4 -> "d4";
            case PURPLE_D6 -> "d6";
            case ORANGE_D8 -> "d8";
            case RED_D12 -> "d12";
            case PRISMATIC -> "";
        };
    }

    public static DiceType fromMaxFace(int maxFace) {
        return switch (maxFace) {
            case 6 -> PURPLE_D6;
            case 8 -> ORANGE_D8;
            case 12 -> RED_D12;
            default -> BLUE_D4;
        };
    }
}