package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.PrismaticDiceProfile;
import java.util.HashMap;
import java.util.Map;

public final class PrismaticDiceRegistry {

    private static final Map<String, PrismaticDiceProfile> REGISTRY = new HashMap<>();

    static {
        registerAllDice();
    }

    private PrismaticDiceRegistry() {}

    private static void registerAllDice() {
        register(new AbsoluteSixProfile());
        
        register(new DefaultPrismaticDiceProfile("evolution", Strings.get("prismatic.evolution.name"), new int[]{3, 3, 4, 4, 6, 2}));
        register(new DefaultPrismaticDiceProfile("destiny", Strings.get("prismatic.destiny.name"), new int[]{1, 3, 3, 6, 6, 12}));
        register(new DefaultPrismaticDiceProfile("revenge", Strings.get("prismatic.revenge.name"), new int[]{6, 6, 8, 8, 12, 12}));
        register(new DefaultPrismaticDiceProfile("doctors_advice", Strings.get("prismatic.doctors_advice.name"), new int[]{1, 2, 3, 4, 6, 6}));
        register(new DefaultPrismaticDiceProfile("last_words", Strings.get("prismatic.last_words.name"), new int[]{4, 5, 5, 1, 2, 4}));
        register(new DefaultPrismaticDiceProfile("repeater", Strings.get("prismatic.repeater.name"), new int[]{1, 1, 1, 1, 4, 4}));
        register(new DefaultPrismaticDiceProfile("cactus", Strings.get("prismatic.cactus.name"), new int[]{4, 5, 6, 7, 8, 9}));
        register(new DefaultPrismaticDiceProfile("miracle", Strings.get("prismatic.miracle.name"), new int[]{99, 99, 99, 99, 99, 99}));
        register(new DefaultPrismaticDiceProfile("loan", Strings.get("prismatic.loan.name"), new int[]{2, 2, 3, 3, 4, 4}));
        register(new DefaultPrismaticDiceProfile("astral_shield", Strings.get("prismatic.astral_shield.name"), new int[]{5, 5, 5, 1, 1, 1}));
        register(new DefaultPrismaticDiceProfile("oath", Strings.get("prismatic.oath.name"), new int[]{6, 6, 6, 4, 4, 6}));
        register(new DefaultPrismaticDiceProfile("prime_number", Strings.get("prismatic.prime_number.name"), new int[]{3, 3, 5, 5, 7, 7}));
        register(new DefaultPrismaticDiceProfile("big_red_button", Strings.get("prismatic.big_red_button.name"), new int[]{6, 6, 6, 8, 8, 8}));
        register(new DefaultPrismaticDiceProfile("sorcerer", Strings.get("prismatic.sorcerer.name"), new int[]{2, 3, 4, 4, 6, 6}));
        register(new DefaultPrismaticDiceProfile("heartbeat", Strings.get("prismatic.heartbeat.name"), new int[]{1, 1, 1, 1, 9, 9}));
        register(new DefaultPrismaticDiceProfile("berserker", Strings.get("prismatic.berserker.name"), new int[]{3, 3, 3, 8, 8, 12}));
        register(new DefaultPrismaticDiceProfile("gambler", Strings.get("prismatic.gambler.name"), new int[]{1, 1, 4, 6, 8, 10}));
        register(new DefaultPrismaticDiceProfile("magic_bullet", Strings.get("prismatic.magic_bullet.name"), new int[]{1, 3, 5, 7, 3, 5}));
    }

    private static void register(PrismaticDiceProfile profile) {
        REGISTRY.put(profile.diceId(), profile);
    }

    public static PrismaticDiceProfile get(String diceId) {
        return REGISTRY.get(diceId);
    }

    public static boolean has(String diceId) {
        return REGISTRY.containsKey(diceId);
    }
}