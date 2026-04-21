package data.scripts.cosmicon.battle;

import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

public final class CosmiconSprites {
    private static final SettingsAPI settings = Global.getSettings();
    private static final Map<String, SpriteAPI> portraitCache = new HashMap<>();

    private CosmiconSprites() {}

    private static SpriteAPI frameSprite;
    private static SpriteAPI diceD4Sprite;
    private static SpriteAPI diceD6Sprite;
    private static SpriteAPI diceD8Sprite;
    private static SpriteAPI diceD12Sprite;
    private static SpriteAPI prismaticDiceSprite;
    private static SpriteAPI prismaticButtonSprite;
    private static SpriteAPI atkIconSprite;
    private static SpriteAPI defIconSprite;
    private static boolean loaded = false;

    private static final Map<String, String> PORTRAIT_KEYS = new HashMap<>();
    static {
        PORTRAIT_KEYS.put("acheron", "portrait_acheron");
        PORTRAIT_KEYS.put("aventurine", "portrait_aventurine");
        PORTRAIT_KEYS.put("castorice", "portrait_castorice");
        PORTRAIT_KEYS.put("cyrene", "portrait_cyrene");
        PORTRAIT_KEYS.put("dan_heng", "portrait_dan_heng");
        PORTRAIT_KEYS.put("firefly", "portrait_firefly");
        PORTRAIT_KEYS.put("hyacine", "portrait_hyacine");
        PORTRAIT_KEYS.put("kafka", "portrait_kafka");
        PORTRAIT_KEYS.put("march_7th", "portrait_march_7th");
        PORTRAIT_KEYS.put("phainon", "portrait_phainon");
        PORTRAIT_KEYS.put("robin", "portrait_robin");
        PORTRAIT_KEYS.put("sparxie", "portrait_sparxie");
        PORTRAIT_KEYS.put("the_herta", "portrait_the_herta");
        PORTRAIT_KEYS.put("yao_guang", "portrait_yao_guang");
    }

    public static void load() {
        if (loaded) return;
        try {
            frameSprite = settings.getSprite("cosmicon_cards", "frame");
            diceD4Sprite = settings.getSprite("cosmicon_dice", "d4");
            diceD6Sprite = settings.getSprite("cosmicon_dice", "d6");
            diceD8Sprite = settings.getSprite("cosmicon_dice", "d8");
            diceD12Sprite = settings.getSprite("cosmicon_dice", "d12");
            prismaticDiceSprite = settings.getSprite("cosmicon_dice", "prismatic");
            prismaticButtonSprite = settings.getSprite("cosmicon_dice", "prismatic_btn");
            atkIconSprite = settings.getSprite("cosmicon_cards", "atk_icon");
            defIconSprite = settings.getSprite("cosmicon_cards", "def_icon");
            loaded = true;
            Global.getLogger(CosmiconSprites.class).info("Cosmicon sprites loaded successfully");
        } catch (Exception e) {
            Global.getLogger(CosmiconSprites.class).error("Failed to load Cosmicon sprites", e);
        }
    }

    public static SpriteAPI getFrame() {
        if (!loaded) load();
        return frameSprite;
    }

    public static SpriteAPI getAtkIcon() {
        if (!loaded) load();
        return atkIconSprite;
    }

    public static SpriteAPI getDefIcon() {
        if (!loaded) load();
        return defIconSprite;
    }

    public static SpriteAPI getPortrait(String cardId) {
        if (!loaded) load();
        
        if (portraitCache.containsKey(cardId)) {
            return portraitCache.get(cardId);
        }

        String portraitKey = PORTRAIT_KEYS.get(cardId);
        if (portraitKey == null) {
            Global.getLogger(CosmiconSprites.class).warn("No portrait mapping for card: " + cardId);
            portraitCache.put(cardId, null);
            return null;
        }

        try {
            SpriteAPI sprite = settings.getSprite("cosmicon_cards", portraitKey);
            if (sprite != null && sprite.getWidth() > 0) {
                portraitCache.put(cardId, sprite);
                return sprite;
            }
        } catch (Exception e) {
            Global.getLogger(CosmiconSprites.class).warn("Portrait not found for: " + cardId);
        }

        portraitCache.put(cardId, null);
        return null;
    }

    public static SpriteAPI getDiceIcon(DiceType type) {
        if (!loaded) load();
        return switch (type) {
            case BLUE_D4 -> diceD4Sprite;
            case PURPLE_D6 -> diceD6Sprite;
            case ORANGE_D8 -> diceD8Sprite;
            case PRISMATIC_D12 -> prismaticDiceSprite;
        };
    }

    public static SpriteAPI getPrismaticButtonSprite() {
        if (!loaded) load();
        return prismaticButtonSprite;
    }

    public static void clearCache() {
        portraitCache.clear();
        loaded = false;
    }
}