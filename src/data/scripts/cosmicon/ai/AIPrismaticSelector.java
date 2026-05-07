package data.scripts.cosmicon.ai;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.prismatic.PrismaticEffect;
import data.scripts.cosmicon.prismatic.PrismaticManager;

import java.util.List;

public class AIPrismaticSelector {

    public record PrismaticDecision(PrismaticDiceInstance instance, float score, boolean shouldUse) {}

    private static final float USE_THRESHOLD = 3.0f;

    public static PrismaticDecision selectPrismaticDice(BattleState state, boolean forPlayer) {
        PrismaticManager manager = state.getPrismaticManager();
        if (manager == null) return null;

        List<PrismaticDiceType> available = manager.getAvailable(forPlayer, state);
        if (available.isEmpty()) return null;

        boolean isAttacking = state.isAttacker(forPlayer);
        int currentSelectionSum = state.calculateSelectedSum(forPlayer);
        CharacterCard card = state.getCard(forPlayer);
        boolean useTrueVersion = card != null && card.isUseTruePrismatic();

        PrismaticDiceType bestType = null;
        float bestScore = 0f;

        for (PrismaticDiceType type : available) {
            float score = evaluatePrismaticValue(type, state, isAttacking, currentSelectionSum, forPlayer, useTrueVersion);
            if (score > bestScore) {
                bestScore = score;
                bestType = type;
            }
        }

        if (bestType != null) {
            PrismaticDiceInstance instance = PrismaticDiceInstance.roll(bestType, useTrueVersion);
            return new PrismaticDecision(instance, bestScore, bestScore >= USE_THRESHOLD);
        }

        return null;
    }

    private static float evaluatePrismaticValue(PrismaticDiceType type, BattleState state,
                                                 boolean isAttacking, int currentSelectionSum,
                                                 boolean forPlayer, boolean useTrueVersion) {
        int[] faces = type.getFaces(useTrueVersion);
        int sum = 0;
        for (int f : faces) sum += f;
        float baseValue = (float) sum / faces.length;

        PrismaticEffect effect = type.getEffect();
        float effectBonus = calculateEffectBonus(effect, state, isAttacking, currentSelectionSum, forPlayer);

        return baseValue + effectBonus;
    }

    private static float calculateEffectBonus(PrismaticEffect effect, BattleState state,
                                               boolean isAttacking, int currentSelectionSum,
                                               boolean forPlayer) {
        if (effect == null || effect.isNone()) return 0f;

        if (effect.isDoubleValue()) {
            return currentSelectionSum * 0.8f;
        }

        if (effect.isGrantStatus()) {
            StatusEffect grantedEffect = effect.getGrantedEffect();
            if (grantedEffect == null) return 0f;

            return switch (grantedEffect) {
                case FORCEFIELD -> isAttacking ? 0f : 5f;
                case COMBO -> isAttacking ? state.getAttackValue() : 0f;
                case UNYIELDING -> {
                    int hp = forPlayer ? state.getPlayerHp() : state.getOpponentHp();
                    yield (hp <= 3 && !isAttacking) ? 4f : 0f;
                }
                case DESTINED -> 3f;
                case HACK -> 2f;
                default -> 0f;
            };
        }

        if (effect.isHealHp()) {
            CharacterCard card = state.getCard(forPlayer);
            int hp = forPlayer ? state.getPlayerHp() : state.getOpponentHp();
            int maxHp = card != null ? card.getMaxHp() : hp;
            int missingHp = maxHp - hp;
            return missingHp / 2f;
        }

        if (effect.isInstantDamage()) {
            return isAttacking ? effect.getInstantDamageAmount() : 0f;
        }

        if (effect.isGainPrismaticUse()) {
            return 2f;
        }

        return 0f;
    }
}