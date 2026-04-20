package data.scripts.cosmicon.prismatic;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.EffectManager;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;

public class PrismaticDiceProcessor {
    
    public void applyEffect(PrismaticDiceInstance dice, BattleState state, EffectManager effectManager, boolean forPlayer) {
        if (!dice.shouldTriggerEffect()) return;
        
        PrismaticEffect effect = dice.getEffect();
        
        if (effect.isNone()) return;
        
        state.incrementPrismaticTriggerCount(forPlayer);
        
        if (effect.isDoubleValue()) {
            state.setDoubleValueActive(forPlayer, true);
            return;
        }
        
        if (effect.isGrantStatus()) {
            StatusEffect statusEffect = effect.getGrantedEffect();
            int layers = effect.calculateLayers(dice.rolledFace);
            effectManager.applyEffect(statusEffect, layers, forPlayer);
            return;
        }
        
        if (effect.isHealHp()) {
            int healAmount = dice.rolledFace;
            state.applyHealTo(forPlayer, healAmount);
            return;
        }
        
        if (effect.isGainPrismaticUse()) {
            state.addPrismaticUse(dice.type, forPlayer);
            return;
        }
        
        if (effect.isInstantDamage()) {
            int damage = effect.getInstantDamageAmount();
            state.applyDamageTo(!forPlayer, damage);
        }
    }
    
    public void checkDestinedDice(PrismaticDiceInstance dice, boolean forPlayer) {
        PrismaticEffect effect = dice.getEffect();
        if (effect.isGrantStatus() && effect.getGrantedEffect() == StatusEffect.DESTINED) {
            dice.setMustSelect(true);
        }
    }
}