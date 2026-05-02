package data.scripts.cosmicon.prismatic;

import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.prismatic.conditions.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PrismaticDiceRegistry {
    
    private static final Map<String, PrismaticDiceType> REGISTRY = new HashMap<>();
    
    static {
        registerAllDice();
    }
    
    private PrismaticDiceRegistry() {}
    
    private static void registerAllDice() {
        register(createEvolution());
        register(createAbsoluteSix());
        register(createDestiny());
        register(createRevenge());
        register(createDoctorsAdvice());
        register(createLastWords());
        register(createRepeater());
        register(createCactus());
        register(createMiracle());
        register(createLoan());
        register(createAstralShield());
        register(createOath());
        register(createPrimeNumber());
        register(createBigRedButton());
        register(createSorcerer());
        register(createHeartbeat());
        register(createBerserker());
        register(createGambler());
        register(createMagicBullet());
        register(createMirror());
        register(createSanctions());
        register(createStrategy());
        register(createToxic());
    }
    
    private static void register(PrismaticDiceType type) {
        REGISTRY.put(type.getId(), type);
    }
    
    public static PrismaticDiceType get(String diceId) {
        return REGISTRY.get(diceId);
    }
    
    public static boolean has(String diceId) {
        return REGISTRY.containsKey(diceId);
    }
    
    public static Map<String, PrismaticDiceType> getAll() {
        return new HashMap<>(REGISTRY);
    }
    
    private static Set<Integer> indices(int... indices) {
        Set<Integer> set = new HashSet<>();
        for (int i : indices) set.add(i);
        return set;
    }
    
    private static Set<Integer> allIndices() {
        Set<Integer> set = new HashSet<>();
        for (int i = 0; i < 6; i++) set.add(i);
        return set;
    }
    
    private static PrismaticDiceType createEvolution() {
        return PrismaticDiceType.createWithSpecialFaces(
            "evolution",
            new int[]{3, 3, 4, 4, 6, 2},
            indices(5),
            PrismaticEffect.doubleValue(),
            AlwaysAvailableCondition.INSTANCE
        );
    }
    
    private static PrismaticDiceType createAbsoluteSix() {
        return PrismaticDiceType.create(
            "absolute_six",
            new int[]{6, 6, 6, 6, 6, 6},
            PrismaticEffect.none(),
            AlwaysAvailableCondition.INSTANCE
        );
    }
    
    private static PrismaticDiceType createDestiny() {
        return PrismaticDiceType.createWithVersions(
            "destiny",
            new int[]{1, 3, 3, 6, 6, 12},
            new int[]{1, 3, 3, 12, 12, 16},
            allIndices(),
            PrismaticEffect.grantStatus(StatusEffect.DESTINED, 1),
            AlwaysAvailableCondition.INSTANCE
        );
    }
    
    private static PrismaticDiceType createRevenge() {
        return PrismaticDiceType.create(
            "revenge",
            new int[]{6, 6, 8, 8, 12, 12},
            PrismaticEffect.none(),
            DamageTakenCondition.atLeast(25)
        );
    }
    
    private static PrismaticDiceType createDoctorsAdvice() {
        return PrismaticDiceType.createWithSpecialFaces(
            "doctors_advice",
            new int[]{1, 2, 3, 4, 6, 6},
            allIndices(),
            PrismaticEffect.healHp(),
            AlwaysAvailableCondition.INSTANCE
        );
    }
    
    private static PrismaticDiceType createLastWords() {
        return PrismaticDiceType.createWithSpecialFaces(
            "last_words",
            new int[]{4, 5, 5, 1, 2, 4},
            indices(3, 4, 5),
            PrismaticEffect.doubleValue(),
            HpThresholdCondition.atOrBelow(8)
        );
    }
    
    private static PrismaticDiceType createRepeater() {
        return PrismaticDiceType.createWithVersions(
            "repeater",
            new int[]{1, 1, 1, 1, 4, 4},
            new int[]{1, 1, 4, 4, 4, 4},
            indices(4, 5),
            PrismaticEffect.grantStatus(StatusEffect.COMBO, 1),
            new CompositeCondition(
                FaceSelectionCountCondition.faceSelected(4, 2),
                ActionTypeCondition.attackOnly()
            )
        );
    }
    
    private static PrismaticDiceType createCactus() {
        return PrismaticDiceType.createWithSpecialFaces(
            "cactus",
            new int[]{4, 5, 6, 7, 8, 9},
            allIndices(),
            PrismaticEffect.grantStatus(StatusEffect.COUNTER, 1),
            ActionTypeCondition.defenseOnly()
        );
    }
    
    private static PrismaticDiceType createMiracle() {
        return PrismaticDiceType.create(
            "miracle",
            new int[]{99, 99, 99, 99, 99, 99},
            PrismaticEffect.none(),
            FaceSelectionCountCondition.faceSelected(1, 9)
        );
    }
    
    private static PrismaticDiceType createLoan() {
        return PrismaticDiceType.createWithSpecialFaces(
            "loan",
            new int[]{2, 2, 3, 3, 4, 4},
            allIndices(),
            PrismaticEffect.grantStatus(StatusEffect.OVERLOAD, 0, true),
            AlwaysAvailableCondition.INSTANCE
        );
    }
    
    private static PrismaticDiceType createAstralShield() {
        return PrismaticDiceType.createWithVersions(
            "astral_shield",
            new int[]{5, 5, 5, 1, 1, 1},
            new int[]{7, 7, 7, 1, 1, 1},
            indices(3, 4, 5),
            PrismaticEffect.grantStatus(StatusEffect.FORCEFIELD, 1),
            ActionTypeCondition.defenseOnly()
        );
    }
    
    private static PrismaticDiceType createOath() {
        return PrismaticDiceType.createWithVersionsAndSpecialFaces(
            "oath",
            new int[]{6, 6, 6, 4, 4, 6},
            new int[]{8, 8, 4, 4, 6, 6},
            indices(3, 4, 5),
            indices(2, 3, 4, 5),
            PrismaticEffect.grantStatus(StatusEffect.UNYIELDING, 1),
            ActionTypeCondition.defenseOnly()
        );
    }
    
    private static PrismaticDiceType createPrimeNumber() {
        return PrismaticDiceType.createWithVersions(
            "prime_number",
            new int[]{3, 3, 5, 5, 7, 7},
            new int[]{5, 5, 5, 7, 7, 7},
            new HashSet<>(),
            PrismaticEffect.none(),
            AlwaysAvailableCondition.INSTANCE
        );
    }
    
    private static PrismaticDiceType createBigRedButton() {
        return PrismaticDiceType.createWithSpecialFaces(
            "big_red_button",
            new int[]{6, 6, 6, 8, 8, 8},
            allIndices(),
            PrismaticEffect.grantStatus(StatusEffect.LAST_STAND, 1),
            new CompositeCondition(
                TurnCountCondition.fromTurn(5),
                ActionTypeCondition.attackOnly()
            )
        );
    }
    
    private static PrismaticDiceType createSorcerer() {
        return PrismaticDiceType.createWithVersions(
            "sorcerer",
            new int[]{2, 3, 4, 4, 6, 6},
            new int[]{4, 4, 4, 4, 6, 6},
            indices(3, 4, 5),
            PrismaticEffect.grantStatus(StatusEffect.HACK, 1),
            AlwaysAvailableCondition.INSTANCE
        );
    }
    
    private static PrismaticDiceType createHeartbeat() {
        return PrismaticDiceType.createWithSpecialFaces(
            "heartbeat",
            new int[]{1, 1, 1, 1, 9, 9},
            indices(4, 5),
            PrismaticEffect.gainPrismaticUse(),
            AlwaysAvailableCondition.INSTANCE
        );
    }
    
    private static PrismaticDiceType createBerserker() {
        return PrismaticDiceType.createWithVersions(
            "berserker",
            new int[]{3, 3, 3, 8, 8, 12},
            new int[]{4, 4, 8, 8, 12, 12},
            indices(3, 4, 5),
            PrismaticEffect.grantStatusWithDivisor(StatusEffect.THORNS, 4),
            AlwaysAvailableCondition.INSTANCE
        );
    }
    
    private static PrismaticDiceType createGambler() {
        return PrismaticDiceType.createWithVersions(
            "gambler",
            new int[]{1, 1, 4, 6, 8, 10},
            new int[]{1, 1, 6, 8, 10, 12},
            Set.of(),
            PrismaticEffect.none(),
            TurnCountCondition.untilTurn(4)
        );
    }
    
    private static PrismaticDiceType createMagicBullet() {
        return PrismaticDiceType.createWithVersionsAndSpecialFaces(
            "magic_bullet",
            new int[]{1, 3, 5, 7, 3, 5},
            new int[]{3, 5, 7, 3, 5, 7},
            indices(4, 5),
            indices(3, 4, 5),
            PrismaticEffect.instantDamage(3),
            AlwaysAvailableCondition.INSTANCE
        );
    }

    private static PrismaticDiceType createMirror() {
        return PrismaticDiceType.createWithVersions(
            "mirror",
            new int[]{2, 3, 4, 4, 5, 6},
            new int[]{3, 4, 5, 5, 6, 7},
            indices(3, 4, 5),
            PrismaticEffect.grantStatus(StatusEffect.REFLECT, 1),
            ActionTypeCondition.defenseOnly()
        );
    }

    private static PrismaticDiceType createSanctions() {
        return PrismaticDiceType.createWithVersions(
            "sanctions",
            new int[]{3, 4, 4, 5, 5, 6},
            new int[]{4, 5, 5, 6, 6, 7},
            indices(3, 4, 5),
            PrismaticEffect.grantStatus(StatusEffect.DETERRENCE, 1),
            AlwaysAvailableCondition.INSTANCE
        );
    }

    private static PrismaticDiceType createStrategy() {
        return PrismaticDiceType.createWithSpecialFaces(
            "strategy",
            new int[]{1, 2, 3, 4, 5, 6},
            allIndices(),
            PrismaticEffect.grantStatus(StatusEffect.TACTICS, 1),
            TurnCountCondition.fromTurn(2)
        );
    }

    private static PrismaticDiceType createToxic() {
        return PrismaticDiceType.createWithVersions(
            "toxic",
            new int[]{2, 3, 3, 4, 4, 5},
            new int[]{3, 4, 4, 5, 5, 6},
            indices(3, 4, 5),
            PrismaticEffect.grantStatus(StatusEffect.POISON, 0, true),
            AlwaysAvailableCondition.INSTANCE
        );
    }
}