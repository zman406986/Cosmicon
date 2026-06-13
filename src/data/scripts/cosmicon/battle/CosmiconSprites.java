package data.scripts.cosmicon.battle;

import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

import data.scripts.cosmicon.util.CharacterIds;

public final class CosmiconSprites {
    private static SettingsAPI settings;
    private static final Map<String, SpriteAPI> portraitCache = new HashMap<>();

    private CosmiconSprites() {}

    private static SpriteAPI frameSprite;
    private static SpriteAPI diceD4Sprite;
    private static SpriteAPI diceD6Sprite;
    private static SpriteAPI diceD8Sprite;
    private static SpriteAPI prismaticDiceSprite;
    private static SpriteAPI prismaticButtonSprite;
    private static SpriteAPI atkIconSprite;
    private static SpriteAPI defIconSprite;
    private static boolean loaded = false;

    private static final Map<String, String> PORTRAIT_KEYS = new HashMap<>();
    static {
        PORTRAIT_KEYS.put(CharacterIds.ACHERON, "portrait_acheron");
        PORTRAIT_KEYS.put(CharacterIds.AVENTURINE, "portrait_aventurine");
        PORTRAIT_KEYS.put(CharacterIds.CASTORICE, "portrait_castorice");
        PORTRAIT_KEYS.put(CharacterIds.CYRENE, "portrait_cyrene");
        PORTRAIT_KEYS.put(CharacterIds.DAN_HENG, "portrait_dan_heng");
        PORTRAIT_KEYS.put(CharacterIds.FIREFLY, "portrait_firefly");
        PORTRAIT_KEYS.put(CharacterIds.HYACINE, "portrait_hyacine");
        PORTRAIT_KEYS.put(CharacterIds.KAFKA, "portrait_kafka");
        PORTRAIT_KEYS.put(CharacterIds.MARCH_7TH, "portrait_march_7th");
        PORTRAIT_KEYS.put(CharacterIds.PHAINON, "portrait_phainon");
        PORTRAIT_KEYS.put(CharacterIds.ROBIN, "portrait_robin");
        PORTRAIT_KEYS.put(CharacterIds.SPARXIE, "portrait_sparxie");
        PORTRAIT_KEYS.put(CharacterIds.THE_HERTA, "portrait_the_herta");
        PORTRAIT_KEYS.put(CharacterIds.YAO_GUANG, "portrait_yao_guang");
        PORTRAIT_KEYS.put(CharacterIds.TRASHCAN, "portrait_trashcan");
        PORTRAIT_KEYS.put(CharacterIds.TRASHCAN_2STAR, "portrait_trashcan_2star");
        PORTRAIT_KEYS.put(CharacterIds.CHIMERA, "portrait_chimera");
        PORTRAIT_KEYS.put(CharacterIds.DROMAS, "portrait_dromas");
        PORTRAIT_KEYS.put(CharacterIds.AUTOMATON_BEETLE, "portrait_automaton_beetle");
        PORTRAIT_KEYS.put(CharacterIds.FURBO_JOURNALIST, "portrait_furbo_journalist");
        PORTRAIT_KEYS.put(CharacterIds.BANANADVISOR, "portrait_bananadvisor");
        PORTRAIT_KEYS.put(CharacterIds.SENIOR_STAFF, "portrait_senior_staff");
    }

    public static void load() {
        if (loaded) return;
        settings = Global.getSettings();
        try {
            frameSprite = settings.getSprite("cosmicon_cards", "frame");
            diceD4Sprite = settings.getSprite("cosmicon_dice", "d4");
            diceD6Sprite = settings.getSprite("cosmicon_dice", "d6");
            diceD8Sprite = settings.getSprite("cosmicon_dice", "d8");
            prismaticDiceSprite = settings.getSprite("cosmicon_dice", "prismatic");
            prismaticButtonSprite = settings.getSprite("cosmicon_dice", "prismatic_btn");
            atkIconSprite = settings.getSprite("cosmicon_cards", "atk_icon");
            defIconSprite = settings.getSprite("cosmicon_cards", "def_icon");
            DiceSpriteRegistry.load();
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
            case YELLOW_D12 -> null;
            case PRISMATIC -> prismaticDiceSprite;
        };
    }

    public static SpriteAPI getPrismaticButtonSprite() {
        if (!loaded) load();
        return prismaticButtonSprite;
    }

    public static void clearCache() {
        portraitCache.clear();
        DiceSpriteRegistry.clearCache();
        loaded = false;
    }
}