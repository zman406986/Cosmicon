package data.scripts.cosmicon.battle;

import java.awt.Color;

public enum DiceType {
    BLUE_D4(4, new Color(74, 144, 217), Color.WHITE, 3),
    PURPLE_D6(6, new Color(155, 89, 182), Color.WHITE, 4),
    ORANGE_D8(8, new Color(230, 126, 34), Color.WHITE, 4),
    PRISMATIC_D12(12, new Color(255, 215, 0), new Color(255, 215, 0), 5);

    private final int maxFace;
    private final Color bodyColor;
    private final Color numberColor;
    private final int vertices;

    DiceType(int maxFace, Color bodyColor, Color numberColor, int vertices) {
        this.maxFace = maxFace;
        this.bodyColor = bodyColor;
        this.numberColor = numberColor;
        this.vertices = vertices;
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
}