package data.scripts.cosmicon.battle;

import java.awt.Color;

public enum DiceType {
    BLUE_D4(4, new Color(74, 144, 217), Color.WHITE, 3, 80f),
    PURPLE_D6(6, new Color(155, 89, 182), Color.WHITE, 4, 60f),
    ORANGE_D8(8, new Color(230, 126, 34), Color.WHITE, 4, 80f),
    PRISMATIC_D12(12, new Color(255, 215, 0), new Color(255, 215, 0), 5, 80f);

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
            case PRISMATIC_D12 -> "d12";
        };
    }
}