package data.scripts.cosmicon.prismatic;

import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;

public class PrismaticEffect {
    public static final PrismaticEffect NONE = new PrismaticEffect("NONE");
    public static final PrismaticEffect DOUBLE_VALUE = new PrismaticEffect("DOUBLE_VALUE");
    public static final PrismaticEffect HEAL_HP = new PrismaticEffect("HEAL_HP");
    public static final PrismaticEffect GAIN_PRISMATIC_USE = new PrismaticEffect("GAIN_PRISMATIC_USE");
    
    private final String name;
    private StatusEffect grantedEffect;
    private int fixedLayers;
    private boolean useFaceValueForLayers;
    private int layerDivisor;
    private int instantDamageAmount;
    
    private PrismaticEffect(String name) {
        this.name = name;
    }
    
    public static PrismaticEffect none() {
        return NONE;
    }
    
    public static PrismaticEffect doubleValue() {
        return DOUBLE_VALUE;
    }
    
    public static PrismaticEffect grantStatus(StatusEffect status, int layers, boolean useFaceValue) {
        PrismaticEffect effect = new PrismaticEffect("GRANT_STATUS");
        effect.grantedEffect = status;
        effect.fixedLayers = layers;
        effect.useFaceValueForLayers = useFaceValue;
        effect.layerDivisor = 1;
        return effect;
    }

    public static PrismaticEffect grantStatusWithDivisor(StatusEffect status, int divisor) {
        PrismaticEffect effect = new PrismaticEffect("GRANT_STATUS");
        effect.grantedEffect = status;
        effect.fixedLayers = 0;
        effect.useFaceValueForLayers = true;
        effect.layerDivisor = divisor;
        return effect;
    }
    
    public static PrismaticEffect grantStatus(StatusEffect status, int layers) {
        return grantStatus(status, layers, false);
    }
    
    public static PrismaticEffect healHp() {
        return HEAL_HP;
    }
    
    public static PrismaticEffect gainPrismaticUse() {
        return GAIN_PRISMATIC_USE;
    }
    
    public static PrismaticEffect instantDamage(int amount) {
        PrismaticEffect effect = new PrismaticEffect("INSTANT_DAMAGE");
        effect.instantDamageAmount = amount;
        return effect;
    }
    
    public boolean isNone() { return this == NONE; }
    public boolean isDoubleValue() { return this == DOUBLE_VALUE || "DOUBLE_VALUE".equals(name); }
    public boolean isGrantStatus() { return "GRANT_STATUS".equals(name); }
    public boolean isHealHp() { return this == HEAL_HP || "HEAL_HP".equals(name); }
    public boolean isGainPrismaticUse() { return this == GAIN_PRISMATIC_USE || "GAIN_PRISMATIC_USE".equals(name); }
    public boolean isInstantDamage() { return "INSTANT_DAMAGE".equals(name); }
    
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
        return name;
    }
}