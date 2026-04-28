package data.scripts.cosmicon.battle;

import data.scripts.cosmicon.battle.BattleState.TurnType;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.CosmiconLogger;

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
        
        int modifiedAttack = attackValue + attackerEffects.calculateAttackBonus(TurnType.ATTACK);
        int modifiedDefense = defenseValue + defenderEffects.calculateDefenseBonus(TurnType.DEFENSE);
        
        int attackerPrismaticValue = state.getPrismaticDiceTotalValue(state.isPlayerAttacker());
        int defenderPrismaticValue = state.getPrismaticDiceTotalValue(!state.isPlayerAttacker());
        modifiedAttack += attackerPrismaticValue;
        modifiedDefense += defenderPrismaticValue;
        
        int prePerforationDefense = modifiedDefense;
        
        if (attackerEffects.shouldIgnoreDefense()) {
            modifiedDefense = 0;
        }
        
        boolean perforationSuccessful = attackerEffects.shouldIgnoreDefense() && prePerforationDefense > 0;
        
        int damage = Math.max(0, modifiedAttack - modifiedDefense);
        
        boolean forcefieldUsed = false;
        if (defenderEffects.isForcefieldActive() && damage > 0) {
            int forcefieldLayers = defenderEffects.getLayers(StatusEffectProcessor.StatusEffect.FORCEFIELD);
            damage = Math.max(1, damage - forcefieldLayers);
            forcefieldUsed = true;
        }
        
        int thornsDamage = defenderEffects.getLayers(StatusEffectProcessor.StatusEffect.THORNS);
        
        int attackerSelfThorns = 0;
        String attackerCardId = state.isPlayerAttacker() ?
                (state.getPlayerCard() != null ? state.getPlayerCard().getId() : null) :
                (state.getOpponentCard() != null ? state.getOpponentCard().getId() : null);
        if (CharacterIds.YAO_GUANG.equals(attackerCardId)) {
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
        
        DamageResult result = new DamageResult(
            damage,
            thornsDamage,
            attackerSelfThorns,
            counterDamage,
            overloadSelfDamage,
            siphonHeal + weatherSiphon,
            reflectDamage,
            instantDamage,
            perforationSuccessful,
            forcefieldUsed
        );
        
        logDamageResolution(state, attackValue, defenseValue, modifiedAttack, modifiedDefense,
            attackerPrismaticValue, defenderPrismaticValue, damage, result);
        
        return result;
    }

    public record DamageResult(int damageToDefender, int thornsDamage, int selfThornsDamage, int counterDamage, 
                               int overloadSelfDamage, int siphonHeal, int reflectDamage, int instantDamage,
                               boolean perforationSuccessful, boolean forcefieldUsed)
    {
    }
    
    private void logDamageResolution(BattleState state, int baseAttack, int baseDefense,
                                     int modifiedAttack, int modifiedDefense,
                                     int attackerPrismatic, int defenderPrismatic,
                                     int finalDamage, DamageResult result) {
        String attacker = state.isPlayerAttacker() ? "Player" : "Enemy";
        String defender = state.isPlayerAttacker() ? "Enemy" : "Player";
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== Damage Resolution ===\n");
        sb.append("  Attacker: ").append(attacker).append(" | Defender: ").append(defender).append("\n");
        sb.append("  Base: Attack=").append(baseAttack).append(" Defense=").append(baseDefense).append("\n");
        sb.append("  Prismatic: +").append(attackerPrismatic).append(" attack, +").append(defenderPrismatic).append(" defense\n");
        sb.append("  Modified: Attack=").append(modifiedAttack).append(" Defense=").append(modifiedDefense).append("\n");
        sb.append("  Final damage to defender: ").append(finalDamage);
        
        if (result.thornsDamage() > 0 || result.selfThornsDamage() > 0) {
            sb.append("\n  Thorns: ").append(result.thornsDamage());
            if (result.selfThornsDamage() > 0) {
                sb.append(" (self: ").append(result.selfThornsDamage()).append(")");
            }
        }
        if (result.counterDamage() > 0) {
            sb.append("\n  Counter damage: ").append(result.counterDamage());
        }
        if (result.overloadSelfDamage() > 0) {
            sb.append("\n  Overload self-damage: ").append(result.overloadSelfDamage());
        }
        if (result.siphonHeal() > 0) {
            sb.append("\n  Siphon heal: ").append(result.siphonHeal());
        }
        if (result.reflectDamage() > 0) {
            sb.append("\n  Reflect: ").append(result.reflectDamage());
        }
        if (result.instantDamage() > 0) {
            sb.append("\n  Instant damage: ").append(result.instantDamage());
        }
        
        CosmiconLogger.info(sb.toString());
    }
}