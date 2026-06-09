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

    private static final String[] DICE_ROLL_IDS = {"cos_dice_roll_1", "cos_dice_roll_2", "cos_dice_roll_3", "cos_dice_roll_4"};

    public static void playDiceRoll(int diceCount) {
        int toPlay = Math.min(diceCount, DICE_ROLL_IDS.length);
        for (int i = 0; i < toPlay; i++) {
            float pitch = 0.9f + (float) Math.random() * 0.2f;
            Global.getSoundPlayer().playUISound(DICE_ROLL_IDS[i], pitch, 1f);
        }
    }
}
