package data.scripts.cosmicon.ai;

import java.util.Arrays;
import java.util.List;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.prismatic.PrismaticEffect;
import data.scripts.cosmicon.prismatic.PrismaticManager;

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

        PrismaticDecision bestDecision = null;
        float bestScore = 0f;

        for (PrismaticDiceType type : available) {
            float score = evaluatePrismaticValue(type, state, isAttacking, currentSelectionSum, forPlayer, useTrueVersion);
            if (score > bestScore) {
                bestScore = score;
                PrismaticDiceInstance instance = PrismaticDiceInstance.roll(type, useTrueVersion);
                bestDecision = new PrismaticDecision(instance, score, score >= USE_THRESHOLD);
            }
        }

        return bestDecision;
    }

    private static float evaluatePrismaticValue(PrismaticDiceType type, BattleState state,
                                                 boolean isAttacking, int currentSelectionSum,
                                                 boolean forPlayer, boolean useTrueVersion) {
        float baseValue = (float) Arrays.stream(type.getFaces(useTrueVersion)).average().orElse(0f);

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
                case THORNS -> isAttacking ? 0f : 3f;
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