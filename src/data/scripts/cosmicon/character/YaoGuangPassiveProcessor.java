package data.scripts.cosmicon.character;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.StatusEffectProcessor;
import data.scripts.cosmicon.battle.StatusEffectProcessor.Phase;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;

public final class YaoGuangPassiveProcessor {

    private static final int EXTRA_REROLLS = 4;
    private static final int FREE_REROLL_THRESHOLD = 2;
    private static final int THORNS_PER_EXTRA_REROLL = 2;
    private static final int ATTACK_THRESHOLD = 18;

    private YaoGuangPassiveProcessor() {}

    public static boolean isYaoGuang(String characterId) {
        return "yao_guang".equals(characterId);
    }

public static void applyStartOfAttackTurn(BattleState state, boolean forPlayer) {
        if (isNotCharacterYaoGuang(state, forPlayer)) return;
        if (isDefending(state, forPlayer)) return;
        
        state.setRemainingRerolls(forPlayer, state.getRemainingRerolls(forPlayer) + EXTRA_REROLLS);
    }

    public static void onRerollCompleted(BattleState state, boolean forPlayer) {
        if (isNotCharacterYaoGuang(state, forPlayer)) return;
        if (isDefending(state, forPlayer)) return;
        
        int rerollsUsed = state.getRerollsUsedThisTurn(forPlayer);
        if (rerollsUsed > FREE_REROLL_THRESHOLD) {
            StatusEffectProcessor effects = forPlayer ? state.getPlayerEffects() : state.getOpponentEffects();
            effects.addEffect(StatusEffect.THORNS, THORNS_PER_EXTRA_REROLL);
        }
    }

    public static void onAttackResolution(BattleState state, boolean forPlayer) {
        if (isNotCharacterYaoGuang(state, forPlayer)) return;
        if (isDefending(state, forPlayer)) return;
        
        int attackValue = state.getAttackValue();
        if (attackValue >= ATTACK_THRESHOLD) {
            StatusEffectProcessor effects = forPlayer ? state.getPlayerEffects() : state.getOpponentEffects();
            effects.removeEffect(StatusEffect.THORNS);
            
            state.addPrismaticUse(1);
            for (PrismaticDiceType type : PrismaticDiceRegistry.getAll().values()) {
                state.addPrismaticUseByType(type, forPlayer, 1);
            }
        }
    }

    private static boolean isNotCharacterYaoGuang(BattleState state, boolean forPlayer) {
        var card = state.getCard(forPlayer);
        return card == null || !isYaoGuang(card.getId());
    }

    private static boolean isDefending(BattleState state, boolean forPlayer) {
        return state.isPlayerAttacker() != forPlayer;
    }

    

    public static int getThornsCostForReroll(int currentRerollsUsed) {
        if (currentRerollsUsed <= FREE_REROLL_THRESHOLD) return 0;
        return THORNS_PER_EXTRA_REROLL;
    }

    public static int getTotalThornsAfterRerolls(int totalRerollsUsed) {
        if (totalRerollsUsed <= FREE_REROLL_THRESHOLD) return 0;
        return (totalRerollsUsed - FREE_REROLL_THRESHOLD) * THORNS_PER_EXTRA_REROLL;
    }

    public static boolean wouldCleansThorns(int attackValue) {
        return attackValue >= ATTACK_THRESHOLD;
    }
}