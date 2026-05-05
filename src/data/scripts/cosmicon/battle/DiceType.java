package data.scripts.cosmicon.battle;

import java.awt.Color;

public enum DiceType {
    BLUE_D4(4, new Color(74, 144, 217), Color.WHITE, 3, 80f),
    PURPLE_D6(6, new Color(155, 89, 182), Color.WHITE, 4, 60f),
    ORANGE_D8(8, new Color(230, 126, 34), Color.WHITE, 4, 80f),
    RED_D12(12, new Color(192, 57, 43), Color.WHITE, 5, 60f),
    PRISMATIC(6, new Color(255, 215, 0), new Color(255, 215, 0), 5, 60f);

    private final int maxFace;
    private final Color bodyColor;
    private final Color numberColor;
    private final int vertices;
    private final float displaySize;

    DiceType(int maxFace, Color bodyColor, Color numberColor, int vertices, float displaySize) {
        this.maxFace = maxFace;
        this.bodyColor = bodyColor;
        this.numberColor = numberColor;
        this.vertices = vertices;
        this.displaySize = displaySize;
    }

    public float getDisplaySize() {
        return displaySize;
    }

    public int getMaxFace() {
        return maxFace;
    }

    public Color getBodyColor() {
        return bodyColor;
    }

    public Color getNumberColor() {
        return numberColor;
    }

    public int getVertices() {
        return vertices;
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
            case 4 -> BLUE_D4;
            case 6 -> PURPLE_D6;
            case 8 -> ORANGE_D8;
            case 12 -> RED_D12;
            default -> BLUE_D4;
        };
    }
}