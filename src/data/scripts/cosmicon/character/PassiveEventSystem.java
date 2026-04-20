package data.scripts.cosmicon.character;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveEvaluator.EndOfTurnPassiveResult;
import data.scripts.cosmicon.util.PassiveEvaluator.PostDamageResult;

public final class PassiveEventSystem {

    private static final int YAO_GUANG_EXTRA_REROLLS = 4;
    private static final int YAO_GUANG_FREE_REROLL_THRESHOLD = 2;
    private static final int YAO_GUANG_THORNS_PER_EXTRA_REROLL = 2;
    private static final int YAO_GUANG_ATTACK_THRESHOLD = 18;

    private PassiveEventSystem() {}

    public static void onStartOfAttackTurn(BattleState state, boolean forPlayer) {
        String characterId = getCharacterId(state, forPlayer);
        if (characterId == null) return;

        if (isAttacker(state, forPlayer)) return;

        if (characterId.equals("yao_guang"))
        {
            var effects = forPlayer ? state.getPlayerEffects() : state.getOpponentEffects();
            effects.addEffect(StatusEffect.YAO_GUANG_REROLLS, YAO_GUANG_EXTRA_REROLLS);
        }
    }

    public static void onRerollCompleted(BattleState state, boolean forPlayer) {
        String characterId = getCharacterId(state, forPlayer);
        if (characterId == null) return;

        if (isAttacker(state, forPlayer)) return;

        if (characterId.equals("yao_guang"))
        {
            applyYaoGuangRerollThorns(state, forPlayer);
        }
    }

    public static void onAttackResolution(BattleState state, boolean forPlayer) {
        String characterId = getCharacterId(state, forPlayer);
        if (characterId == null) return;

        if (isAttacker(state, forPlayer)) return;

        if (characterId.equals("yao_guang"))
        {
            applyYaoGuangAttackResolution(state, forPlayer);
        }
    }

    public static int onDamageTaken(BattleState state, boolean defenderIsPlayer, int damage) {
        if (damage <= 0) return 0;

        String characterId = getCharacterId(state, defenderIsPlayer);
        if (characterId == null) return 0;

        PostDamageResult result = PassiveEvaluator.evaluatePostDamageForCharacter(characterId, damage);
        
        if (result.hasEffects()) {
            CharacterCard card = state.getCard(defenderIsPlayer);
            if (result.getAtkLevelIncrease() > 0 && card != null) {
                card.setAtkLevel(card.getAtkLevel() + result.getAtkLevelIncrease());
            }
            if (result.getDefLevelIncrease() > 0 && card != null) {
                card.setDefLevel(card.getDefLevel() + result.getDefLevelIncrease());
            }

            var effects = defenderIsPlayer ? state.getPlayerEffects() : state.getOpponentEffects();
            for (PassiveEvaluator.GrantedEffect ge : result.getGrantedEffects()) {
                effects.addEffect(ge.effect(), ge.layers());
            }
        }

        return result.getInstantDamageToAttacker();
    }

    public static int processInstantDamageEffects(BattleState state, boolean forPlayer) {
        var effects = forPlayer ? state.getPlayerEffects() : state.getOpponentEffects();
        int instantDamageLayers = effects.getLayers(StatusEffect.INSTANT_DAMAGE);
        
        if (instantDamageLayers <= 0) return 0;

        effects.removeEffect(StatusEffect.INSTANT_DAMAGE);

        return instantDamageLayers;
    }

    public static void onEndOfTurn(BattleState state, boolean forPlayer) {
        String characterId = getCharacterId(state, forPlayer);
        if (characterId == null) return;

        int triggerCount = state.getPrismaticTriggerCount(forPlayer);
        state.recordTurnAtkDef(forPlayer);
        int cumulativeAtkDef = state.getCumulativeAtkDef(forPlayer);

        EndOfTurnPassiveResult result = PassiveEvaluator.evaluateEndOfTurnPassive(characterId, triggerCount, cumulativeAtkDef);

        CharacterCard card = state.getCard(forPlayer);
        if ("cyrene".equals(characterId)) {
            boolean thresholdAlreadyMet = state.isCyreneThresholdMet(forPlayer);
            if (PassiveEvaluator.shouldApplyCyreneAtkBoost(thresholdAlreadyMet, cumulativeAtkDef)) {
                if (card != null) {
                    card.setAtkLevel(5);
                }
                state.setCyreneThresholdMet(forPlayer, true);
            }
        }

        if (result.hasEffects()) {
            if (result.getPrismaticUseBonus() > 0) {
                for (int i = 0; i < result.getPrismaticUseBonus(); i++) {
                    state.addPrismaticUseByType(null, forPlayer, 1);
                }
            }

            if (result.shouldGrantArise()) {
                state.getEffectManager().applyEffect(StatusEffect.ARISE, 1, forPlayer);
            }

            if (result.getAtkLevelBoost() > 0 && card != null) {
                card.setAtkLevel(card.getAtkLevel() + result.getAtkLevelBoost());
            }
        }
    }

    public static int getThornsCostForReroll(BattleState state, boolean forPlayer, int currentRerollsUsed) {
        String characterId = getCharacterId(state, forPlayer);
        if (!"yao_guang".equals(characterId)) return 0;
        if (currentRerollsUsed <= YAO_GUANG_FREE_REROLL_THRESHOLD) return 0;
        return YAO_GUANG_THORNS_PER_EXTRA_REROLL;
    }

    public static int getTotalThornsAfterRerolls(BattleState state, boolean forPlayer, int totalRerollsUsed) {
        String characterId = getCharacterId(state, forPlayer);
        if (!"yao_guang".equals(characterId)) return 0;
        if (totalRerollsUsed <= YAO_GUANG_FREE_REROLL_THRESHOLD) return 0;
        return (totalRerollsUsed - YAO_GUANG_FREE_REROLL_THRESHOLD) * YAO_GUANG_THORNS_PER_EXTRA_REROLL;
    }

    public static int getThornsCostForRerollYaoGuang(int currentRerollsUsed) {
        if (currentRerollsUsed <= YAO_GUANG_FREE_REROLL_THRESHOLD) return 0;
        return YAO_GUANG_THORNS_PER_EXTRA_REROLL;
    }

    public static int getTotalThornsAfterRerollsYaoGuang(int totalRerollsUsed) {
        if (totalRerollsUsed <= YAO_GUANG_FREE_REROLL_THRESHOLD) return 0;
        return (totalRerollsUsed - YAO_GUANG_FREE_REROLL_THRESHOLD) * YAO_GUANG_THORNS_PER_EXTRA_REROLL;
    }

    public static boolean wouldCleanseThorns(BattleState state, boolean forPlayer, int attackValue) {
        String characterId = getCharacterId(state, forPlayer);
        if (!"yao_guang".equals(characterId)) return false;
        return attackValue >= YAO_GUANG_ATTACK_THRESHOLD;
    }

    private static String getCharacterId(BattleState state, boolean forPlayer) {
        CharacterCard card = state.getCard(forPlayer);
        return card != null ? card.getId() : null;
    }

    private static boolean isAttacker(BattleState state, boolean forPlayer) {
        return state.isPlayerAttacker() != forPlayer;
    }

    public static boolean isYaoGuang(String characterId) {
        return "yao_guang".equals(characterId);
    }

    private static void applyYaoGuangRerollThorns(BattleState state, boolean forPlayer) {
        int rerollsUsed = state.getRerollsUsedThisTurn(forPlayer);
        if (rerollsUsed > YAO_GUANG_FREE_REROLL_THRESHOLD) {
            var effects = forPlayer ? state.getPlayerEffects() : state.getOpponentEffects();
            effects.addEffect(StatusEffect.THORNS, YAO_GUANG_THORNS_PER_EXTRA_REROLL);
        }
    }

    private static void applyYaoGuangAttackResolution(BattleState state, boolean forPlayer) {
        int attackValue = state.getAttackValue();
        if (attackValue >= YAO_GUANG_ATTACK_THRESHOLD) {
            var effects = forPlayer ? state.getPlayerEffects() : state.getOpponentEffects();
            effects.removeEffect(StatusEffect.THORNS);

            state.addPrismaticUse(1);
            for (PrismaticDiceType type : PrismaticDiceRegistry.getAll().values()) {
                state.addPrismaticUseByType(type, forPlayer, 1);
            }
        }
    }
}