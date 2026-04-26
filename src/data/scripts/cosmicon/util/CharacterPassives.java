package data.scripts.cosmicon.util;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.util.PassiveResults.EndOfTurnPassiveResult;
import data.scripts.cosmicon.util.PassiveResults.PassiveResult;
import data.scripts.cosmicon.util.PassiveResults.PostDamageResult;
import java.util.List;

import static data.scripts.cosmicon.util.CharacterIds.*;
import static data.scripts.cosmicon.util.DiceEvaluator.*;

public class CharacterPassives {

    private static final int YAO_GUANG_EXTRA_REROLLS = 4;
    private static final int YAO_GUANG_FREE_REROLL_THRESHOLD = 2;
    private static final int YAO_GUANG_THORNS_PER_EXTRA_REROLL = 2;
    private static final int YAO_GUANG_ATTACK_THRESHOLD = 18;
    private static final int CYRENE_ATK_BOOST_THRESHOLD = 24;

    private CharacterPassives() {}

    public static void evaluateForCharacter(String characterId, PassiveResult result, 
                                            List<Integer> diceValues, boolean isAttacking,
                                            int currentHp, int maxHp, int currentToughnessLayers) {
        switch (characterId) {
            case AVENTURINE -> evaluateAventurine(result, diceValues, isAttacking, currentToughnessLayers);
            case FIREFLY -> evaluateFirefly(result, diceValues, isAttacking, currentHp, maxHp);
            case ACHERON -> evaluateAcheron(result, diceValues, isAttacking);
            case KAFKA -> evaluateKafka(result, diceValues, isAttacking);
            case ROBIN -> evaluateRobin(result, diceValues, isAttacking);
            case MARCH_7TH -> evaluateMarch7th(result, diceValues);
            case HYACINE -> evaluateHyacine(result, diceValues, isAttacking);
            case DAN_HENG -> evaluateDanHengPT(result, diceValues, isAttacking);
            case PHAINON -> evaluatePhainon(result, diceValues, isAttacking);
            case THE_HERTA, CYRENE, CASTORICE, YAO_GUANG -> {}
            case SPARXIE -> evaluateSparxie(result, diceValues);
        }
    }

    private static void evaluateAventurine(PassiveResult result, List<Integer> values, boolean isAttacking, int currentToughnessLayers) {
        if (!isAttacking || values == null || values.isEmpty()) return;
        int oddCount = countOddNumbers(values);
        result.addGrantedEffect(StatusEffect.TOUGHNESS, oddCount);
        
        int totalToughness = currentToughnessLayers + oddCount;
        if (totalToughness >= 7) {
            result.setToughnessTrigger(7, 7);
        }
    }

    private static void evaluateFirefly(PassiveResult result, List<Integer> values, boolean isAttacking, int currentHp, int maxHp) {
        if (!isAttacking || values == null || values.isEmpty()) return;
        if (hasTwoPairs(values)) {
            result.addGrantedEffect(StatusEffect.COMBO, 1);
        }
        if (maxHp > 0 && currentHp == maxHp) {
            result.addAttackBonus(5);
        }
    }

    private static void evaluateAcheron(PassiveResult result, List<Integer> values, boolean isAttacking) {
        if (!isAttacking || values == null || values.isEmpty()) return;
        if (allDiceEqualFour(values)) {
            result.setPerforation(true);
        }
    }

    private static void evaluateKafka(PassiveResult result, List<Integer> values, boolean isAttacking) {
        if (!isAttacking || values == null || values.isEmpty()) return;
        int distinct = countDistinctValues(values);
        result.addGrantedEffect(StatusEffect.POISON, distinct);
    }

    private static void evaluateRobin(PassiveResult result, List<Integer> values, boolean isAttacking) {
        if (!isAttacking || values == null || values.isEmpty()) return;
        if (allEven(values)) {
            result.addGrantedEffect(StatusEffect.LEVEL_UP, 1);
        }
    }

    private static void evaluateMarch7th(PassiveResult result, List<Integer> values) {
        if (values == null || values.isEmpty()) return;
        int pairs = countPairs(values);
        if (pairs > 0) {
            result.addGrantedEffect(StatusEffect.INSTANT_DAMAGE, pairs * 3);
        }
    }

    private static void evaluateHyacine(PassiveResult result, List<Integer> values, boolean isAttacking) {
        if (!isAttacking || values == null || values.isEmpty()) return;
        if (allDiceEqualSix(values)) {
            result.addGrantedEffect(StatusEffect.STRENGTH, sumOfValues(values));
            result.setHealAmount(6);
        } else {
            result.addGrantedEffect(StatusEffect.STRENGTH, sumOfValues(values) / 2);
        }
    }

    private static void evaluateDanHengPT(PassiveResult result, List<Integer> values, boolean isAttacking) {
        if (!isAttacking || values == null || values.isEmpty()) return;
        if (sumAtLeast(values, 18)) {
            result.setPendingDefLevelBoost(3);
        }
    }

    private static void evaluatePhainon(PassiveResult result, List<Integer> values, boolean isAttacking) {
        if (values == null || values.isEmpty()) return;
        if (isAttacking) {
            result.addGrantedEffect(StatusEffect.SIPHON, 50);
        } else {
            if (allSame(values)) {
                result.addGrantedEffect(StatusEffect.UNYIELDING, 1);
            }
        }
    }

    private static void evaluateSparxie(PassiveResult result, List<Integer> values) {
        if (values == null || values.isEmpty()) return;
        if (hasIdenticalNumbers(values)) {
            result.addGrantedEffect(StatusEffect.HACK, 1);
        }
    }

    public static EndOfTurnPassiveResult evaluateEndOfTurnPassive(String characterId, int prismaticTriggerCount, int cumulativeAtkDef) {
        EndOfTurnPassiveResult result = new EndOfTurnPassiveResult();
        
        switch (characterId) {
            case THE_HERTA -> evaluateTheHertaEndOfTurn(result, prismaticTriggerCount);
            case CYRENE -> evaluateCyreneEndOfTurn(result, cumulativeAtkDef);
        }
        
        return result;
    }
    
    private static void evaluateTheHertaEndOfTurn(EndOfTurnPassiveResult result, int triggerCount) {
        result.addPrismaticUseBonus(1);
        if (triggerCount > 4) {
            result.setGrantArise(true);
        }
    }
    
    private static void evaluateCyreneEndOfTurn(EndOfTurnPassiveResult result, int cumulativeTotal) {
        if (cumulativeTotal > CYRENE_ATK_BOOST_THRESHOLD) {
            result.setGrantArise(true);
        }
    }

    public static PostDamageResult evaluatePostDamageForCharacter(String characterId, int damageTaken) {
        PostDamageResult result = new PostDamageResult();
        
        if (CASTORICE.equals(characterId)) {
            evaluateCastoricePostDamage(result, damageTaken);
        }
        
        return result;
    }

    private static void evaluateCastoricePostDamage(PostDamageResult result, int damageTaken) {
        if (damageTaken <= 0) return;
        
        if (damageTaken >= 8) {
            result.addAtkLevelIncrease(1);
            result.addDefLevelIncrease(1);
            result.setDescription("DMG >= 8: ATK/DEF Level +1");
        } else if (damageTaken <= 5) {
            result.setInstantDamageToAttacker(3);
            result.setDescription("DMG <= 5: 3 Instant Damage to attacker");
        }
    }

    public static void onStartOfAttackTurn(String characterId, BattleState state, boolean forPlayer) {
        if (characterId == null) return;
        
        if (YAO_GUANG.equals(characterId)) {
            state.getEffects(forPlayer).addEffect(StatusEffect.YAO_GUANG_REROLLS, YAO_GUANG_EXTRA_REROLLS);
        }
    }

    public static void onRerollCompleted(String characterId, BattleState state, boolean forPlayer) {
        if (characterId == null) return;
        
        if (YAO_GUANG.equals(characterId)) {
            int rerollsUsed = state.getRerollsUsedThisTurn(forPlayer);
            if (rerollsUsed > YAO_GUANG_FREE_REROLL_THRESHOLD) {
                state.getEffects(forPlayer).addEffect(StatusEffect.THORNS, YAO_GUANG_THORNS_PER_EXTRA_REROLL);
            }
        }
    }

    public static void onAttackResolution(String characterId, BattleState state, boolean forPlayer) {
        if (characterId == null) return;
        
        if (YAO_GUANG.equals(characterId)) {
            int attackValue = state.getAttackValue();
            if (attackValue >= YAO_GUANG_ATTACK_THRESHOLD) {
                state.getEffects(forPlayer).removeEffect(StatusEffect.THORNS);
                state.addPrismaticUse(1);
                for (PrismaticDiceType type : PrismaticDiceRegistry.getAll().values()) {
                    state.addPrismaticUseByType(type, forPlayer, 1);
                }
            }
        }
    }

    public static void onPerforationSuccess(String characterId, BattleState state, boolean attackerIsPlayer) {
        if (characterId == null) return;
        
        if (ACHERON.equals(characterId)) {
            state.modifyCardAtkLevel(attackerIsPlayer, 1);
        }
    }

    public static void onStartOfDefenseTurn(String characterId, BattleState state, boolean forPlayer) {
        if (characterId == null) return;
        
        int pendingBoost = state.getPendingDefLevelBoost(forPlayer);
        if (pendingBoost > 0) {
            var card = state.getCard(forPlayer);
            if (card != null) {
                int originalDefLevel = card.getDefLevel();
                state.setOriginalDefLevel(forPlayer, originalDefLevel);
                card.setDefLevel(originalDefLevel + pendingBoost);
            }
            state.getEffects(forPlayer).addEffect(StatusEffect.COUNTER, 1);
            state.clearPendingDefLevelBoost(forPlayer);
        }
    }

    public static void onEndOfDefenseTurn(String characterId, BattleState state, boolean forPlayer) {
        if (characterId == null) return;
        
        int originalDefLevel = state.getOriginalDefLevel(forPlayer);
        if (originalDefLevel > 0) {
            var card = state.getCard(forPlayer);
            if (card != null) {
                card.setDefLevel(originalDefLevel);
            }
            state.setOriginalDefLevel(forPlayer, 0);
        }
    }

    public static void onDefenseFail(String characterId, BattleState state, boolean defenderIsPlayer) {
        if (characterId == null) return;
        
        if (KAFKA.equals(characterId)) {
            boolean opponentIsPlayer = !defenderIsPlayer;
            var opponentEffects = state.getEffects(opponentIsPlayer);
            int currentPoison = opponentEffects.getLayers(StatusEffect.POISON);
            if (currentPoison > 0) {
                opponentEffects.setEffect(StatusEffect.POISON, currentPoison - 1);
            }
        }
    }

    public static void applyEndOfTurnEffects(String characterId, BattleState state, boolean forPlayer, 
                                              int cumulativeAtkDef, boolean cyreneThresholdMet) {
        if (characterId == null) return;
        
        if (CYRENE.equals(characterId) && shouldApplyCyreneAtkBoost(cyreneThresholdMet, cumulativeAtkDef)) {
            var card = state.getCard(forPlayer);
            if (card != null) {
                card.setAtkLevel(5);
            }
            state.setCyreneThresholdMet(forPlayer, true);
        }
    }

    public static boolean shouldApplyCyreneAtkBoost(boolean thresholdAlreadyMet, int cumulativeTotal) {
        return !thresholdAlreadyMet && cumulativeTotal > CYRENE_ATK_BOOST_THRESHOLD;
    }
}