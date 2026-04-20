package data.scripts.cosmicon.battle;

import data.scripts.cosmicon.battle.BattleState.TurnType;
import data.scripts.cosmicon.character.PassiveEventSystem;

public class DamageResolver {
    
    public DamageResolver() {
    }
    
    public DamageResult resolve(BattleState state) {
        StatusEffectProcessor attackerEffects = state.isPlayerAttacker() ? 
            state.getPlayerEffects() : state.getOpponentEffects();
        StatusEffectProcessor defenderEffects = state.isPlayerAttacker() ? 
            state.getOpponentEffects() : state.getPlayerEffects();
        
        int attackValue = state.getAttackValue();
        int defenseValue = state.getDefenseValue();
        
        int modifiedAttack = attackValue + attackerEffects.calculateAttackBonus(TurnType.ATTACK, attackValue);
        int modifiedDefense = defenseValue + defenderEffects.calculateDefenseBonus(TurnType.DEFENSE, defenseValue);
        
        int attackerPrismaticValue = state.getPrismaticDiceTotalValue(state.isPlayerAttacker());
        int defenderPrismaticValue = state.getPrismaticDiceTotalValue(!state.isPlayerAttacker());
        modifiedAttack += attackerPrismaticValue;
        modifiedDefense += defenderPrismaticValue;
        
        if (attackerEffects.shouldIgnoreDefense()) {
            modifiedDefense = 0;
        }
        
        int damage = Math.max(0, modifiedAttack - modifiedDefense);
        
        if (defenderEffects.isForcefieldActive() && damage > 0) {
            int forcefieldLayers = defenderEffects.getLayers(StatusEffectProcessor.StatusEffect.FORCEFIELD);
            damage = Math.max(1, damage - forcefieldLayers);
            damage = Math.max(0, damage);
        }
        
        int thornsDamage = defenderEffects.getLayers(StatusEffectProcessor.StatusEffect.THORNS);
        
        int attackerSelfThorns = 0;
        if (PassiveEventSystem.isYaoGuang(state.isPlayerAttacker() ? 
                (state.getPlayerCard() != null ? state.getPlayerCard().getId() : null) :
                (state.getOpponentCard() != null ? state.getOpponentCard().getId() : null))) {
            attackerSelfThorns = attackerEffects.getLayers(StatusEffectProcessor.StatusEffect.THORNS);
        }
        
        int counterDamage = defenderEffects.calculateCounterDamage(attackValue, defenseValue);
        
        int overloadSelfDamage = attackerEffects.calculateOverloadSelfDamage(TurnType.ATTACK);
        
        int siphonHeal = attackerEffects.calculateSiphonHeal(damage);
        
        WeatherController weatherController = state.getWeatherController();
        float siphonMultiplier = weatherController != null ? weatherController.getSiphonMultiplier() : 0f;
        
        int weatherSiphon = 0;
        if (siphonMultiplier > 0f && damage > 0) {
            weatherSiphon = (int)(damage * siphonMultiplier);
        }
        
        int reflectDamage = 0;
        if (damage > 0) {
            reflectDamage = defenderEffects.getLayers(StatusEffectProcessor.StatusEffect.REFLECT);
        }
        
        int instantDamage = state.getPrismaticInstantDamage(!state.isPlayerAttacker());
        
        return new DamageResult(
            damage,
            thornsDamage,
            attackerSelfThorns,
            counterDamage,
            overloadSelfDamage,
            siphonHeal + weatherSiphon,
            reflectDamage,
            instantDamage
        );
    }

    public record DamageResult(int damageToDefender, int thornsDamage, int selfThornsDamage, int counterDamage, 
                               int overloadSelfDamage, int siphonHeal, int reflectDamage, int instantDamage)
    {
    }
}