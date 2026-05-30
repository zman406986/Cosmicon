package data.scripts.cosmicon.battle;

public enum DiceType {
    BLUE_D4(4, 80f),
    PURPLE_D6(6, 60f),
    ORANGE_D8(8, 80f),
    YELLOW_D12(12, 60f),
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
            case YELLOW_D12 -> "d12";
            case PRISMATIC -> "";
        };
    }

    // d12 is its own type (YELLOW_D12), NOT PRISMATIC.
    // The wiki rule "d12 dice are treated as Prismatic Dice" only means d12 is immune
    // to Arise/Hack effects, which is handled via the diceIsPrismatic flag in
    // StatusEffectProcessor (type == PRISMATIC || type == YELLOW_D12).
    // Do NOT change case 12 to PRISMATIC — that would break maxFace, rendering, and UI.
    public static DiceType fromMaxFace(int maxFace) {
        return switch (maxFace) {
            case 6 -> PURPLE_D6;
            case 8 -> ORANGE_D8;
            case 12 -> YELLOW_D12;
            default -> BLUE_D4;
        };
    }
}