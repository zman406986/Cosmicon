package data.scripts.cosmicon.prismatic;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.util.CosmiconLogger;

public class PrismaticDiceProcessor {

    public void applyEffect(PrismaticDiceInstance dice, BattleState state, boolean forPlayer) {
        if (!dice.isSpecialFace) return;

        PrismaticEffect effect = dice.getEffect();

        if (effect.isNone()) return;

        state.incrementPrismaticTriggerCount(forPlayer);

        switch (effect.getType()) {
            case DOUBLE_VALUE -> {
                state.setDoubleValueActive(forPlayer, true);
                CosmiconLogger.debug("Prismatic effect applied: DoubleValue to %s",
                    forPlayer ? "Player" : "Opponent");
            }
            case GRANT_STATUS -> {
                StatusEffect statusEffect = effect.getGrantedEffect();
                int layers = effect.calculateLayers(dice.rolledFace);
                boolean target = (statusEffect == StatusEffect.POISON) != forPlayer;
                state.applyEffect(statusEffect, layers, target);
                CosmiconLogger.debug("Prismatic effect applied: %s x%d to %s",
                    statusEffect.name(), layers, target ? "Player" : "Opponent");
            }
            case HEAL_HP -> {
                int healAmount = dice.rolledFace;
                state.applyHealTo(forPlayer, healAmount);
                state.notifyHeal(forPlayer, healAmount);
                CosmiconLogger.debug("Prismatic effect applied: Heal %d HP to %s",
                    healAmount, forPlayer ? "Player" : "Opponent");
            }
            case GAIN_PRISMATIC_USE -> {
                state.addPrismaticUse(dice.type, forPlayer);
                CosmiconLogger.debug("Prismatic effect applied: GainPrismaticUse (%s) to %s",
                    dice.type.getId(), forPlayer ? "Player" : "Opponent");
            }
            case INSTANT_DAMAGE -> {
                int damage = effect.getInstantDamageAmount();
                state.applyDamageTo(!forPlayer, damage);
                state.notifySecondaryDamage(!forPlayer, damage, "INSTANT_DAMAGE");
                CosmiconLogger.debug("Prismatic effect applied: InstantDamage %d to %s",
                    damage, forPlayer ? "Opponent" : "Player");
            }
            case NONE -> {}
            default -> CosmiconLogger.warn("Unhandled prismatic effect type: %s for dice %s",
                effect.getType(), dice.type.getId());
        }
    }
}
