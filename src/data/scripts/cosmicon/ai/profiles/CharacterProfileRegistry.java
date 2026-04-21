package data.scripts.cosmicon.ai.profiles;

import data.scripts.cosmicon.ai.CharacterAIProfile;
import java.util.HashMap;
import java.util.Map;

public final class CharacterProfileRegistry {

    private static final Map<String, CharacterAIProfile> REGISTRY = new HashMap<>();
    private static final CharacterAIProfile DEFAULT = new DefaultCharacterAIProfile();

    static {
        registerAll();
    }

    private CharacterProfileRegistry() {}

    private static void registerAll() {
        register(new AcheronAIProfile());
        register(new CastoriceAIProfile());
        register(new FireflyAIProfile());
        register(new RobinAIProfile());
        register(new TheHertaAIProfile());
        register(new KafkaAIProfile());
        register(new AventurineAIProfile());
        register(new March7thAIProfile());
        register(new DanHengAIProfile());
        register(new SparxieAIProfile());
        register(new YaoGuangAIProfile());
        register(new CyreneAIProfile());
        register(new PhainonAIProfile());
        register(new HyacineAIProfile());
    }

    private static void register(CharacterAIProfile profile) {
        REGISTRY.put(profile.getCharacterId(), profile);
    }

    public static CharacterAIProfile get(String characterId) {
        return REGISTRY.getOrDefault(characterId, DEFAULT);
    }

    public static boolean has(String characterId) {
        return REGISTRY.containsKey(characterId);
    }
}