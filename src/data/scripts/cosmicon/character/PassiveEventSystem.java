package data.scripts.cosmicon.character;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.util.CharacterPassives;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveResults.EndOfTurnPassiveResult;
import data.scripts.cosmicon.util.PassiveResults.PostDamageResult;

public final class PassiveEventSystem {

    private PassiveEventSystem() {}

    public static void onStartOfAttackTurn(BattleState state, boolean forPlayer) {
        String characterId = state.getCardId(forPlayer);
        if (characterId == null || isAttacker(state, forPlayer)) return;

        CharacterPassives.onStartOfAttackTurn(characterId, state, forPlayer);
    }

    public static void onStartOfDefenseTurn(BattleState state, boolean forPlayer) {
        String characterId = state.getCardId(forPlayer);
        if (characterId == null || isAttacker(state, forPlayer)) return;

        CharacterPassives.onStartOfDefenseTurn(characterId, state, forPlayer);
    }

    public static void onEndOfDefenseTurn(BattleState state, boolean forPlayer) {
        String characterId = state.getCardId(forPlayer);
        if (characterId == null) return;

        CharacterPassives.onEndOfDefenseTurn(characterId, state, forPlayer);
    }

    public static void onRerollCompleted(BattleState state, boolean forPlayer) {
        String characterId = state.getCardId(forPlayer);
        if (characterId == null || isAttacker(state, forPlayer)) return;

        CharacterPassives.onRerollCompleted(characterId, state, forPlayer);
    }

    public static void onAttackResolution(BattleState state, boolean forPlayer) {
        String characterId = state.getCardId(forPlayer);
        if (characterId == null || isAttacker(state, forPlayer)) return;

        CharacterPassives.onAttackResolution(characterId, state, forPlayer);
    }

    public static void onAttackResolved(BattleState state, boolean attackerIsPlayer, boolean perforationSuccessful, int damage) {
        if (!perforationSuccessful || damage <= 0) return;
        
        String characterId = state.getCardId(attackerIsPlayer);
        if (characterId == null) return;
        
        CharacterPassives.onPerforationSuccess(characterId, state, attackerIsPlayer);
    }

    public static int onDamageTaken(BattleState state, boolean defenderIsPlayer, int damage) {
        if (damage <= 0) return 0;

        String characterId = state.getCardId(defenderIsPlayer);
        if (characterId == null) return 0;

        PostDamageResult result = PassiveEvaluator.evaluatePostDamageForCharacter(characterId, damage);
        
        if (result.hasEffects()) {
            if (result.getAtkLevelIncrease() > 0) {
                state.modifyCardAtkLevel(defenderIsPlayer, result.getAtkLevelIncrease());
            }
            if (result.getDefLevelIncrease() > 0) {
                CharacterCard card = state.getCard(defenderIsPlayer);
                if (card != null) {
                    card.setDefLevel(card.getDefLevel() + result.getDefLevelIncrease());
                }
            }
        }

        return result.getInstantDamageToAttacker();
    }

    public static void onEndOfTurn(BattleState state, boolean forPlayer) {
        String characterId = state.getCardId(forPlayer);
        if (characterId == null) return;

        int triggerCount = state.getPrismaticTriggerCount(forPlayer);
        state.recordTurnAtkDef(forPlayer);
        int cumulativeAtkDef = state.getCumulativeAtkDef(forPlayer);

        EndOfTurnPassiveResult result = PassiveEvaluator.evaluateEndOfTurnPassive(characterId, triggerCount, cumulativeAtkDef);

        boolean cyreneThresholdMet = state.isCyreneThresholdMet(forPlayer);
        CharacterPassives.applyEndOfTurnEffects(characterId, state, forPlayer, cumulativeAtkDef, cyreneThresholdMet);

        if (result.hasEffects()) {
            if (result.getPrismaticUseBonus() > 0) {
                for (int i = 0; i < result.getPrismaticUseBonus(); i++) {
                    state.addPrismaticUseByType(null, forPlayer, 1);
                }
            }

            if (result.shouldGrantArise()) {
                state.applyEffect(StatusEffect.ARISE, 1, forPlayer);
            }

            if (result.getAtkLevelBoost() > 0) {
                state.modifyCardAtkLevel(forPlayer, result.getAtkLevelBoost());
            }
        }
    }

    public static void onDefenseFail(BattleState state, boolean defenderIsPlayer) {
        String characterId = state.getCardId(defenderIsPlayer);
        if (characterId == null) return;
        CharacterPassives.onDefenseFail(characterId, state, defenderIsPlayer);
    }

    private static boolean isAttacker(BattleState state, boolean forPlayer) {
        return state.isPlayerAttacker() != forPlayer;
    }

    private static boolean isDefender(BattleState state, boolean forPlayer) {
        return state.isPlayerAttacker() == forPlayer;
    }
}