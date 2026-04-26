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
        
        if (effect.isDoubleValue()) {
            state.setDoubleValueActive(forPlayer, true);
            CosmiconLogger.debug("Prismatic effect applied: DoubleValue to %s", 
                forPlayer ? "Player" : "Opponent");
            return;
        }
        
        if (effect.isGrantStatus()) {
            StatusEffect statusEffect = effect.getGrantedEffect();
            int layers = effect.calculateLayers(dice.rolledFace);
            state.applyEffect(statusEffect, layers, forPlayer);
            CosmiconLogger.debug("Prismatic effect applied: %s x%d to %s", 
                statusEffect.name(), layers, forPlayer ? "Player" : "Opponent");
            return;
        }
        
        if (effect.isHealHp()) {
            int healAmount = dice.rolledFace;
            state.applyHealTo(forPlayer, healAmount);
            CosmiconLogger.debug("Prismatic effect applied: Heal %d HP to %s", 
                healAmount, forPlayer ? "Player" : "Opponent");
            return;
        }
        
        if (effect.isGainPrismaticUse()) {
            state.addPrismaticUse(dice.type, forPlayer);
            CosmiconLogger.debug("Prismatic effect applied: GainPrismaticUse (%s) to %s", 
                dice.type.getId(), forPlayer ? "Player" : "Opponent");
            return;
        }
        
        if (effect.isInstantDamage()) {
            int damage = effect.getInstantDamageAmount();
            state.applyDamageTo(!forPlayer, damage);
            CosmiconLogger.debug("Prismatic effect applied: InstantDamage %d to %s", 
                damage, forPlayer ? "Opponent" : "Player");
        }
    }
}