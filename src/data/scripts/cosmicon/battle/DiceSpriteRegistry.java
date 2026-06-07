package data.scripts.cosmicon.battle;

import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;

public class DiceSpriteRegistry {
    private static final int FRAME_COUNT = AnimationConstants.FRAME_COUNT;

    private static final Map<String, SpriteAPI[]> cache = new HashMap<>();
    private static boolean loaded = false;

    private record CycleMapping(String cycleKey, int startFrame)
    {
    }

    private static final Map<String, CycleMapping> resultToCycle = new HashMap<>();
    private static final Map<String, CycleMapping> prismaticToCycle = new HashMap<>();

    public static void load() {
        if (loaded) return;

        loadD4PerResult();
        loadD6Cycles();
        loadD8Cycles();
        loadD12Cycles();
        loadPrismaticCycles();

        loaded = true;
    }

    private static void loadCycleSheet(String cycleKey) {
        SpriteAPI[] frames = new SpriteAPI[FRAME_COUNT];
        for (int frame = 0; frame < FRAME_COUNT; frame++) {
            String spriteKey = cycleKey + "_f" + String.format("%02d", frame);
            frames[frame] = Global.getSettings().getSprite("cosmicon_dice_frames", spriteKey);
        }
        cache.put(cycleKey, frames);
    }

    private static void loadD4PerResult() {
        for (int result = 1; result <= 4; result++) {
            String cycleKey = "d4_" + result;
            loadCycleSheet(cycleKey);
            resultToCycle.put(cycleKey, new CycleMapping(cycleKey, 0));
        }
    }

    private static void loadD6Cycles() {
        loadCycleSheet("d6_cycle_1_6");
        resultToCycle.put("d6_1", new CycleMapping("d6_cycle_1_6", 0));
        resultToCycle.put("d6_6", new CycleMapping("d6_cycle_1_6", 24));

        loadCycleSheet("d6_cycle_2_5");
        resultToCycle.put("d6_2", new CycleMapping("d6_cycle_2_5", 0));
        resultToCycle.put("d6_3", new CycleMapping("d6_cycle_2_5", 12));
        resultToCycle.put("d6_5", new CycleMapping("d6_cycle_2_5", 24));
        resultToCycle.put("d6_4", new CycleMapping("d6_cycle_2_5", 36));
    }

    private static void loadD8Cycles() {
        loadCycleSheet("d8_cycle_1_4");
        resultToCycle.put("d8_1", new CycleMapping("d8_cycle_1_4", 0));
        resultToCycle.put("d8_2", new CycleMapping("d8_cycle_1_4", 12));
        resultToCycle.put("d8_3", new CycleMapping("d8_cycle_1_4", 24));
        resultToCycle.put("d8_4", new CycleMapping("d8_cycle_1_4", 36));

        loadCycleSheet("d8_cycle_5_8");
        resultToCycle.put("d8_5", new CycleMapping("d8_cycle_5_8", 0));
        resultToCycle.put("d8_6", new CycleMapping("d8_cycle_5_8", 12));
        resultToCycle.put("d8_7", new CycleMapping("d8_cycle_5_8", 24));
        resultToCycle.put("d8_8", new CycleMapping("d8_cycle_5_8", 36));
    }

    private static void loadD12Cycles() {
        loadCycleSheet("d12_cycle_A");
        resultToCycle.put("d12_1", new CycleMapping("d12_cycle_A", 0));
        resultToCycle.put("d12_2", new CycleMapping("d12_cycle_A", 12));
        resultToCycle.put("d12_3", new CycleMapping("d12_cycle_A", 24));
        resultToCycle.put("d12_4", new CycleMapping("d12_cycle_A", 36));

        loadCycleSheet("d12_cycle_B");
        resultToCycle.put("d12_5", new CycleMapping("d12_cycle_B", 0));
        resultToCycle.put("d12_6", new CycleMapping("d12_cycle_B", 12));
        resultToCycle.put("d12_7", new CycleMapping("d12_cycle_B", 24));
        resultToCycle.put("d12_8", new CycleMapping("d12_cycle_B", 36));

        loadCycleSheet("d12_cycle_C");
        resultToCycle.put("d12_9", new CycleMapping("d12_cycle_C", 0));
        resultToCycle.put("d12_10", new CycleMapping("d12_cycle_C", 12));
        resultToCycle.put("d12_11", new CycleMapping("d12_cycle_C", 24));
        resultToCycle.put("d12_12", new CycleMapping("d12_cycle_C", 36));
    }

    private static void loadPrismaticCycles() {
        loadCycleSheet("d6_prismatic_cycle_A_F");
        prismaticToCycle.put("prismatic_0", new CycleMapping("d6_prismatic_cycle_A_F", 0));
        prismaticToCycle.put("prismatic_5", new CycleMapping("d6_prismatic_cycle_A_F", 24));

        loadCycleSheet("d6_prismatic_cycle_B_E");
        prismaticToCycle.put("prismatic_1", new CycleMapping("d6_prismatic_cycle_B_E", 0));
        prismaticToCycle.put("prismatic_2", new CycleMapping("d6_prismatic_cycle_B_E", 12));
        prismaticToCycle.put("prismatic_4", new CycleMapping("d6_prismatic_cycle_B_E", 24));
        prismaticToCycle.put("prismatic_3", new CycleMapping("d6_prismatic_cycle_B_E", 36));
    }

    public static void clearCache() {
        cache.clear();
        resultToCycle.clear();
        prismaticToCycle.clear();
        loaded = false;
    }

    private static SpriteAPI lookupFrame(Map<String, CycleMapping> map, String key, int frameIndex) {
        CycleMapping mapping = map.get(key);
        if (mapping == null) return null;

        SpriteAPI[] frames = cache.get(mapping.cycleKey);
        if (frames == null || frameIndex < 0 || frameIndex >= FRAME_COUNT) return null;

        return frames[(mapping.startFrame + 1 + frameIndex) % FRAME_COUNT];
    }

    public static SpriteAPI getFrame(DiceType type, int result, int frameIndex) {
        return lookupFrame(resultToCycle, type.getSpritePrefix() + "_" + result, frameIndex);
    }

    public static SpriteAPI getPrismaticFrame(int faceIndex, int frameIndex) {
        if (faceIndex < 0 || faceIndex >= 6) return null;
        return lookupFrame(prismaticToCycle, "prismatic_" + faceIndex, frameIndex);
    }
}
