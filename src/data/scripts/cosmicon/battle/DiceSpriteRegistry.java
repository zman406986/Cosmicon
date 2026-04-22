package data.scripts.cosmicon.battle;

import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;

public class DiceSpriteRegistry {
    private static final int FRAME_COUNT = 48;
    private static final String[] PRISMATIC_FACE_LETTERS = {"A", "B", "C", "D", "E", "F"};
    
    private static final Map<String, SpriteAPI[]> cache = new HashMap<>();
    private static boolean loaded = false;
    
    public static void load() {
        if (loaded) return;
        
        // Load all frames for each dice type + result combination
        String[] diceTypes = {"d4", "d6", "d8", "d12"};
        int[] maxResults = {4, 6, 8, 12};
        
        for (int i = 0; i < diceTypes.length; i++) {
            String type = diceTypes[i];
            int maxResult = maxResults[i];
            
            for (int result = 1; result <= maxResult; result++) {
                String keyPrefix = type + "_" + result;
                SpriteAPI[] frames = new SpriteAPI[FRAME_COUNT];
                
                for (int frame = 0; frame < FRAME_COUNT; frame++) {
                    String spriteKey = keyPrefix + "_f" + String.format("%02d", frame);
                    frames[frame] = Global.getSettings().getSprite("cosmicon_dice_frames", spriteKey);
                }
                
                cache.put(keyPrefix, frames);
            }
        }
        
        // Load prismatic dice frames (face indices 0-5 → A-F)
        for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
            String letter = PRISMATIC_FACE_LETTERS[faceIndex];
            String keyPrefix = "d6_prismatic_" + letter;
            SpriteAPI[] frames = new SpriteAPI[FRAME_COUNT];
            
            for (int frame = 0; frame < FRAME_COUNT; frame++) {
                String spriteKey = keyPrefix + "_f" + String.format("%02d", frame);
                frames[frame] = Global.getSettings().getSprite("cosmicon_dice_frames", spriteKey);
            }
            
            cache.put(keyPrefix, frames);
        }
        
        loaded = true;
    }
    
    public static void clearCache() {
        cache.clear();
        loaded = false;
    }
    
    public static SpriteAPI getFrame(DiceType type, int result, int frameIndex) {
        String key = type.getSpritePrefix() + "_" + result;
        SpriteAPI[] frames = cache.get(key);
        if (frames == null || frameIndex < 0 || frameIndex >= FRAME_COUNT) {
            return null;
        }
        return frames[frameIndex];
    }
    
    public static SpriteAPI getPrismaticFrame(int faceIndex, int frameIndex) {
        if (faceIndex < 0 || faceIndex >= 6) return null;
        String key = "d6_prismatic_" + PRISMATIC_FACE_LETTERS[faceIndex];
        SpriteAPI[] frames = cache.get(key);
        if (frames == null || frameIndex < 0 || frameIndex >= FRAME_COUNT) {
            return null;
        }
        return frames[frameIndex];
    }
    
    public static SpriteAPI getFinalFrame(DiceType type, int result) {
        return getFrame(type, result, FRAME_COUNT - 1);
    }
    
    public static SpriteAPI getPrismaticFinalFrame(int faceIndex) {
        return getPrismaticFrame(faceIndex, FRAME_COUNT - 1);
    }
}