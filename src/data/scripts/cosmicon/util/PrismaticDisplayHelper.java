package data.scripts.cosmicon.util;

import data.scripts.Strings;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.prismatic.PrismaticEffect;

public final class PrismaticDisplayHelper {
    private PrismaticDisplayHelper() {}

    public static String getDiceDisplayName(String diceId) {
        if (diceId == null || diceId.isEmpty()) {
            return Strings.get("prismatic.equip.none");
        }
        String key = "prismatic." + diceId + ".name";
        try {
            return Strings.get(key);
        } catch (Exception e) {
            return diceId;
        }
    }

    public static String getDiceDisplayName(PrismaticDiceType type) {
        if (type == null) {
            return Strings.get("prismatic.equip.none");
        }
        return getDiceDisplayName(type.getId());
    }

    public static String getEffectDescription(PrismaticEffect effect) {
        if (effect == null || effect.isNone()) return Strings.get("prismatic.equip.no_effect");
        if (effect.isDoubleValue()) return Strings.get("prismatic.equip.effect_double");
        if (effect.isHealHp()) return Strings.get("prismatic.equip.effect_heal");
        if (effect.isGainPrismaticUse()) return Strings.get("prismatic.equip.effect_gain_use");
        if (effect.isInstantDamage()) return Strings.format("prismatic.equip.effect_instant_damage", effect.getInstantDamageAmount());
        if (effect.isGrantStatus()) {
            String statusKey = "status." + effect.getGrantedEffect().name().toLowerCase();
            try {
                String statusName = Strings.get(statusKey);
                return Strings.format("prismatic.equip.effect_status", statusName);
            } catch (Exception e) {
                return Strings.format("prismatic.equip.effect_status", effect.getGrantedEffect().name());
            }
        }
        return Strings.get("prismatic.equip.no_effect");
    }

    public static String getEffectDescription(PrismaticDiceType type) {
        if (type == null) {
            return Strings.get("prismatic.equip.no_effect");
        }
        String key = "prismatic." + type.getId() + ".description";
        try {
            return Strings.get(key);
        } catch (Exception e) {
            return getEffectDescription(type.getEffect());
        }
    }

    public static String getEffectDescriptionForDiceId(String diceId) {
        if (diceId == null || diceId.isEmpty()) {
            return Strings.get("prismatic.equip.none_effect");
        }
        String key = "prismatic." + diceId + ".description";
        try {
            return Strings.get(key);
        } catch (Exception e) {
            PrismaticDiceType type = data.scripts.cosmicon.prismatic.PrismaticDiceRegistry.get(diceId);
            if (type != null) {
                return getEffectDescription(type);
            }
            return Strings.get("prismatic.equip.no_effect");
        }
    }
}