package data.scripts.cosmicon.ai.profiles;

import data.scripts.cosmicon.ai.AttackRerollAI;
import data.scripts.cosmicon.ai.CharacterAIProfile;
import java.util.HashMap;
import java.util.Map;

public final class CharacterProfileRegistry {

    private static final Map<String, CharacterAIProfile> REGISTRY = new HashMap<>();
    private static final DefaultAI DEFAULT = new DefaultAI();

    static {
        registerAll();
    }

    private CharacterProfileRegistry() {}

    private static void registerAll() {
        register(new AcheronAI());
        register(new CastoriceAI());
        register(new FireflyAI());
        register(new RobinAI());
        register(new TheHertaAI());
        register(new KafkaAI());
        register(new AventurineAI());
        register(new March7thAI());
        register(new DanHengAI());
        register(new SparxieAI());
        register(new YaoGuangAI());
        register(new CyreneAI());
        register(new PhainonAI());
        register(new HyacineAI());
        register(new TrashcanAI());
    }

    private static void register(CharacterAIProfile profile) {
        REGISTRY.put(profile.getCharacterId(), profile);
    }

    public static CharacterAIProfile get(String characterId) {
        return REGISTRY.getOrDefault(characterId, DEFAULT);
    }

    public static AttackRerollAI getRerollAI(String characterId) {
        CharacterAIProfile profile = get(characterId);
        if (profile instanceof AttackRerollAI) return (AttackRerollAI) profile;
        return DEFAULT;
    }

    public static AttackRerollAI getDefaultRerollAI() {
        return DEFAULT;
    }

    public static boolean has(String characterId) {
        return REGISTRY.containsKey(characterId);
    }
}
