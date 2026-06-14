package data.scripts.cosmicon;

import com.fs.starfarer.api.Global;

public class CosmiconSFX {

    public static void playUIBeep() {
        Global.getSoundPlayer().playUISound("cos_ui_beep", 1f, 1f);
    }

    public static void playImpact() {
        float pitch = 0.9f + (float) Math.random() * 0.2f;
        Global.getSoundPlayer().playUISound("cos_impact", pitch, 1f);
    }

    public static void playDiceSelect() {
        Global.getSoundPlayer().playUISound("cos_dice_select", 1f, 1f);
    }

    public static void playDiceUnselect() {
        Global.getSoundPlayer().playUISound("cos_dice_unselect", 1f, 1f);
    }

    private static final String ROLL_SHORT = "cos_dice_roll_short";
    private static final String ROLL_MEDIUM = "cos_dice_roll_medium";
    private static final String ROLL_LONG = "cos_dice_roll_long";

    public static void playDiceRoll(float[] distances) {
        if (distances == null || distances.length == 0) return;

        float pitch = 0.9f + (float) Math.random() * 0.2f;

        if (distances.length == 1) {
            String soundId = distances[0] >= 350f ? ROLL_LONG : distances[0] >= 250f ? ROLL_MEDIUM : ROLL_SHORT;
            Global.getSoundPlayer().playUISound(soundId, pitch, 1f);
        } else if (distances.length == 2) {
            boolean hasLong = false;
            for (float d : distances) {
                if (d >= 350f) { hasLong = true; break; }
            }
            if (hasLong) {
                Global.getSoundPlayer().playUISound(ROLL_LONG, pitch, 1f);
                Global.getSoundPlayer().playUISound(ROLL_MEDIUM, pitch, 1f);
            } else {
                Global.getSoundPlayer().playUISound(ROLL_MEDIUM, pitch * 1.05f, 1f);
                Global.getSoundPlayer().playUISound(ROLL_SHORT, pitch, 1f);
            }
        } else {
            Global.getSoundPlayer().playUISound(ROLL_SHORT, pitch * 0.95f, 1f);
            Global.getSoundPlayer().playUISound(ROLL_MEDIUM, pitch, 1f);
            Global.getSoundPlayer().playUISound(ROLL_LONG, pitch * 1.05f, 1f);
        }
    }

    public static void playFlyingSwoosh() {
        Global.getSoundPlayer().playUISound("cos_flying_swoosh", 1f, 1f);
    }

    public static void playClashAtkWin() {
        Global.getSoundPlayer().playUISound("cos_clash_atk_win", 1f, 1f);
    }

    public static void playClashDefWin() {
        Global.getSoundPlayer().playUISound("cos_clash_def_win", 1f, 1f);
    }
}
