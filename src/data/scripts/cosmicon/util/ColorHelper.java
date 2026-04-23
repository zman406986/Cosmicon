package data.scripts.cosmicon.util;

import java.awt.Color;

public final class ColorHelper {
    private ColorHelper() {}

    public static final Color DICE_ZONE_BG = new Color(55, 60, 80, 180);
    public static final Color SELECTION_HIGHLIGHT = new Color(100, 255, 100, 150);

    public static final Color PRISMATIC_GOLD = new Color(255, 215, 0);
    public static final Color PRISMATIC_BRIGHT = new Color(255, 255, 150);
    public static final Color PRISMATIC_DISABLED = new Color(128, 128, 128);

    public static final Color PHASE_LABEL = new Color(255, 220, 100);

    public static final Color PLAYER_NAME = new Color(100, 150, 255);
    public static final Color OPPONENT_NAME = new Color(255, 100, 100);
    public static final Color PLAYER_CARD_PLACEHOLDER = new Color(100, 120, 180);
    public static final Color OPPONENT_CARD_PLACEHOLDER = new Color(180, 100, 120);

    public static final Color ATTACK_VALUE = new Color(255, 100, 80);
    public static final Color DEFENSE_VALUE = new Color(80, 150, 255);
    public static final Color WEATHER_BONUS = new Color(100, 255, 150);

    public static final Color OPPONENT_SELECTION_HIGHLIGHT = new Color(255, 80, 80, 150);
    public static final Color OPPONENT_REROLL_HIGHLIGHT = new Color(255, 160, 80, 150);
    public static final Color OPPONENT_DICE_ZONE_BG = new Color(70, 50, 50, 180);

    public static float[] toGLComponents(Color color, float alphaMult) {
        return new float[] {
            color.getRed() / 255f,
            color.getGreen() / 255f,
            color.getBlue() / 255f,
            (color.getAlpha() / 255f) * alphaMult
        };
    }
}