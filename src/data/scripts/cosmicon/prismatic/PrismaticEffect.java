package data.scripts.cosmicon.prismatic;

import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;

public class PrismaticEffect {
    public enum Type { NONE, DOUBLE_VALUE, HEAL_HP, GAIN_PRISMATIC_USE, GRANT_STATUS, INSTANT_DAMAGE }

    public static final PrismaticEffect NONE = new PrismaticEffect(Type.NONE);
    public static final PrismaticEffect DOUBLE_VALUE = new PrismaticEffect(Type.DOUBLE_VALUE);
    public static final PrismaticEffect HEAL_HP = new PrismaticEffect(Type.HEAL_HP);
    public static final PrismaticEffect GAIN_PRISMATIC_USE = new PrismaticEffect(Type.GAIN_PRISMATIC_USE);
    
    private final Type type;
    private StatusEffect grantedEffect;
    private int fixedLayers;
    private boolean useFaceValueForLayers;
    private int layerDivisor;
    private int instantDamageAmount;
    
    private PrismaticEffect(Type type) {
        this.type = type;
    }

    public static PrismaticEffect grantStatus(StatusEffect status, int layers, boolean useFaceValue) {
        PrismaticEffect effect = new PrismaticEffect(Type.GRANT_STATUS);
        effect.grantedEffect = status;
        effect.fixedLayers = layers;
        effect.useFaceValueForLayers = useFaceValue;
        effect.layerDivisor = 1;
        return effect;
    }

    public static PrismaticEffect grantStatusWithDivisor(StatusEffect status, int divisor) {
        PrismaticEffect effect = new PrismaticEffect(Type.GRANT_STATUS);
        effect.grantedEffect = status;
        effect.fixedLayers = 0;
        effect.useFaceValueForLayers = true;
        effect.layerDivisor = divisor;
        return effect;
    }
    
    public static PrismaticEffect grantStatus(StatusEffect status, int layers) {
        return grantStatus(status, layers, false);
    }

    public static PrismaticEffect instantDamage(int amount) {
        PrismaticEffect effect = new PrismaticEffect(Type.INSTANT_DAMAGE);
        effect.instantDamageAmount = amount;
        return effect;
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