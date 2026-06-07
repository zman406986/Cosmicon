package data.scripts.cosmicon.prismatic;

import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;

public class PrismaticEffect {
    public enum Type { NONE, DOUBLE_VALUE, HEAL_HP, GAIN_PRISMATIC_USE, GRANT_STATUS, INSTANT_DAMAGE }

    public static final PrismaticEffect NONE = new PrismaticEffect(Type.NONE, null, 0, false, 1, 0);
    public static final PrismaticEffect DOUBLE_VALUE = new PrismaticEffect(Type.DOUBLE_VALUE, null, 0, false, 1, 0);
    public static final PrismaticEffect HEAL_HP = new PrismaticEffect(Type.HEAL_HP, null, 0, false, 1, 0);
    public static final PrismaticEffect GAIN_PRISMATIC_USE = new PrismaticEffect(Type.GAIN_PRISMATIC_USE, null, 0, false, 1, 0);

    private final Type type;
    private final StatusEffect grantedEffect;
    private final int fixedLayers;
    private final boolean useFaceValueForLayers;
    private final int layerDivisor;
    private final int instantDamageAmount;

    private PrismaticEffect(Type type, StatusEffect grantedEffect, int fixedLayers,
                            boolean useFaceValueForLayers, int layerDivisor, int instantDamageAmount) {
        this.type = type;
        this.grantedEffect = grantedEffect;
        this.fixedLayers = fixedLayers;
        this.useFaceValueForLayers = useFaceValueForLayers;
        this.layerDivisor = layerDivisor;
        this.instantDamageAmount = instantDamageAmount;
    }

    public static PrismaticEffect grantStatus(StatusEffect status, int layers, boolean useFaceValue) {
        return new PrismaticEffect(Type.GRANT_STATUS, status, layers, useFaceValue, 1, 0);
    }

    public static PrismaticEffect grantStatusWithDivisor(StatusEffect status, int divisor) {
        return new PrismaticEffect(Type.GRANT_STATUS, status, 0, true, divisor, 0);
    }

    public static PrismaticEffect grantStatus(StatusEffect status, int layers) {
        return grantStatus(status, layers, false);
    }

    public static PrismaticEffect instantDamage(int amount) {
        return new PrismaticEffect(Type.INSTANT_DAMAGE, null, 0, false, 1, amount);
    }

    public Type getType() { return type; }
    public boolean isNone() { return type == Type.NONE; }
    public boolean isDoubleValue() { return type == Type.DOUBLE_VALUE; }
    public boolean isGrantStatus() { return type == Type.GRANT_STATUS; }
    public boolean isHealHp() { return type == Type.HEAL_HP; }
    public boolean isGainPrismaticUse() { return type == Type.GAIN_PRISMATIC_USE; }
    public boolean isInstantDamage() { return type == Type.INSTANT_DAMAGE; }

    public StatusEffect getGrantedEffect() { return grantedEffect; }
    public int getInstantDamageAmount() { return instantDamageAmount; }

    public int calculateLayers(int faceValue) {
        if (useFaceValueForLayers) {
            return layerDivisor > 1 ? faceValue / layerDivisor : faceValue;
        }
        return fixedLayers;
    }

    @Override
    public String toString() {
        return type.name();
    }
}
