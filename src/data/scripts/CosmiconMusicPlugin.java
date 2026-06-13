package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

public class CosmiconMusicPlugin {

    private static final String COSMICON_MUSIC_ID = "cosmicon_theme";
    private static final String COSMICON_MUSIC_MEMORY_KEY = "$cosMusicPlaying";

    private static String originalMusicId = null;
    private static boolean wasMusicPlaying = false;

    private static boolean isValidMusicId(String id) {
        return id != null && !id.equals("nothing") && !id.equals("null");
    }

    public static void startMusic() {
        originalMusicId = Global.getSoundPlayer().getCurrentMusicId();
        wasMusicPlaying = isValidMusicId(originalMusicId);

        Global.getSoundPlayer().setSuspendDefaultMusicPlayback(true);
        Global.getSoundPlayer().playCustomMusic(1, 1, COSMICON_MUSIC_ID, true);

        MemoryAPI globalMemory = Global.getSector().getMemory();
        globalMemory.set(COSMICON_MUSIC_MEMORY_KEY, true);
    }

    public static void stopMusic() {
        Global.getSoundPlayer().pauseCustomMusic();
        Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false);

        if (wasMusicPlaying && originalMusicId != null) {
            try {
                Global.getSoundPlayer().playCustomMusic(1, 1, originalMusicId, true);
            } catch (Exception e) {
                Global.getSoundPlayer().restartCurrentMusic();
            }
        } else {
            Global.getSoundPlayer().restartCurrentMusic();
        }

        MemoryAPI globalMemory = Global.getSector().getMemory();
        globalMemory.unset(COSMICON_MUSIC_MEMORY_KEY);
    }
    
    public static boolean isMusicPlaying() {
        return Global.getSector().getMemory().getBoolean(COSMICON_MUSIC_MEMORY_KEY);
    }

    public static void resetStaleState() {
        MemoryAPI globalMemory = Global.getSector().getMemory();
        if (globalMemory.getBoolean(COSMICON_MUSIC_MEMORY_KEY)) {
            globalMemory.unset(COSMICON_MUSIC_MEMORY_KEY);
            originalMusicId = null;
            wasMusicPlaying = false;
        }
    }
}