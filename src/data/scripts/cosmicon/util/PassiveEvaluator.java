package data.scripts.cosmicon.util;

import data.scripts.cosmicon.ai.CharacterAIProfile.PassiveEvaluation;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.StatusEffectProcessor;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PassiveEvaluator {

    public static class PassiveResult {
        private int attackBonus;
        private int defenseBonus;
        private boolean perforation;
        private final List<GrantedEffect> grantedEffects;
        private int healAmount;
        private int instantDamageToOpponent;
        private int toughnessToRemove;
        private boolean triggerToughnessInstantDamage;

        public PassiveResult() {
            this.attackBonus = 0;
            this.defenseBonus = 0;
            this.perforation = false;
            this.grantedEffects = new ArrayList<>();
            this.healAmount = 0;
            this.instantDamageToOpponent = 0;
            this.toughnessToRemove = 0;
            this.triggerToughnessInstantDamage = false;
        }

        public void addAttackBonus(int bonus) {
            this.attackBonus += bonus;
        }

        public void addDefenseBonus(int bonus) {
            this.defenseBonus += bonus;
        }

        public void setPerforation(boolean perforation) {
            this.perforation = perforation;
        }

        public void addGrantedEffect(StatusEffect effect, int layers) {
            grantedEffects.add(new GrantedEffect(effect, layers));
        }

        public void setHealAmount(int heal) {
            this.healAmount = heal;
        }

        public void setInstantDamageToOpponent(int damage) {
            this.instantDamageToOpponent = damage;
        }

        public void setToughnessTrigger(int damage, int remove) {
            this.triggerToughnessInstantDamage = true;
            this.instantDamageToOpponent = damage;
            this.toughnessToRemove = remove;
        }

        public int getAttackBonus() {
            return attackBonus;
        }

        public int getDefenseBonus() {
            return defenseBonus;
        }

        public boolean hasPerforation() {
            return perforation;
        }

        public List<GrantedEffect> getGrantedEffects() {
            return new ArrayList<>(grantedEffects);
        }

        public int getHealAmount() {
            return healAmount;
        }

        public int getInstantDamageToOpponent() {
            return instantDamageToOpponent;
        }

        public int getToughnessToRemove() {
            return toughnessToRemove;
        }

        public boolean shouldTriggerToughnessInstantDamage() {
            return triggerToughnessInstantDamage;
        }

        public boolean hasEffects() {
            return attackBonus > 0 || defenseBonus > 0 || perforation || !grantedEffects.isEmpty() 
                || healAmount > 0 || instantDamageToOpponent > 0;
        }
    }

    public record GrantedEffect(StatusEffect effect, int layers)
    {
    }

    public static class PostDamageResult {
        private int atkLevelIncrease;
        private int defLevelIncrease;
        private int instantDamageToAttacker;
        private final List<GrantedEffect> grantedEffects;
        private String description;

        public PostDamageResult() {
            this.atkLevelIncrease = 0;
            this.defLevelIncrease = 0;
            this.instantDamageToAttacker = 0;
            this.grantedEffects = new ArrayList<>();
            this.description = "";
        }

        public void addAtkLevelIncrease(int increase) {
            this.atkLevelIncrease += increase;
        }

        public void addDefLevelIncrease(int increase) {
            this.defLevelIncrease += increase;
        }

        public void setInstantDamageToAttacker(int damage) {
            this.instantDamageToAttacker = damage;
        }

        public void addGrantedEffect(StatusEffect effect, int layers) {
            grantedEffects.add(new GrantedEffect(effect, layers));
        }

        public void setDescription(String desc) {
            this.description = desc;
        }

        public int getAtkLevelIncrease() {
            return atkLevelIncrease;
        }

        public int getDefLevelIncrease() {
            return defLevelIncrease;
        }

        public int getInstantDamageToAttacker() {
            return instantDamageToAttacker;
        }

        public List<GrantedEffect> getGrantedEffects() {
            return new ArrayList<>(grantedEffects);
        }

        public String getDescription() {
            return description;
        }

        public boolean hasEffects() {
            return atkLevelIncrease > 0 || defLevelIncrease > 0 || instantDamageToAttacker > 0 || !grantedEffects.isEmpty();
        }
    }

    private PassiveEvaluator() {}

    public static boolean allDiceEqualFour(List<Integer> values) {
        if (values == null || values.isEmpty()) return false;
        for (int v : values) {
            if (v != 4) return false;
        }
        return true;
    }

    public static boolean hasTwoPairs(List<Integer> values) {
        return countPairs(values) >= 2;
    }

    public static boolean allEven(List<Integer> values) {
        if (values == null || values.isEmpty()) return false;
        for (int v : values) {
            if (v % 2 != 0) return false;
        }
        return true;
    }

    public static boolean allSame(List<Integer> values) {
        if (values == null || values.isEmpty()) return false;
        int first = values.get(0);
        for (int v : values) {
            if (v != first) return false;
        }
        return true;
    }

    public static boolean allDiceEqualSix(List<Integer> values) {
        if (values == null || values.isEmpty()) return false;
        for (int v : values) {
            if (v != 6) return false;
        }
        return true;
    }

    public static int countPairs(List<Integer> values) {
        if (values == null || values.isEmpty()) return 0;
        Map<Integer, Integer> counts = new HashMap<>();
        for (int v : values) {
            counts.merge(v, 1, Integer::sum);
        }
        int pairs = 0;
        for (int count : counts.values()) {
            pairs += count / 2;
        }
        return pairs;
    }

    public static int countOddNumbers(List<Integer> values) {
        if (values == null) return 0;
        int count = 0;
        for (int v : values) {
            if (v % 2 != 0) count++;
        }
        return count;
    }

    public static int countDistinctValues(List<Integer> values) {
        if (values == null || values.isEmpty()) return 0;
        Map<Integer, Integer> counts = new HashMap<>();
        for (int v : values) {
            counts.put(v, 1);
        }
        return counts.size();
    }

    public static int sumOfValues(List<Integer> values) {
        if (values == null || values.isEmpty()) return 0;
        int sum = 0;
        for (int v : values) {
            sum += v;
        }
        return sum;
    }

    public static boolean sumAtLeast(List<Integer> values, int threshold) {
        return sumOfValues(values) >= threshold;
    }

    public static boolean hasIdenticalNumbers(List<Integer> values) {
        if (values == null || values.size() < 2) return false;
        Map<Integer, Integer> counts = new HashMap<>();
        for (int v : values) {
            counts.merge(v, 1, Integer::sum);
            if (counts.get(v) >= 2) return true;
        }
        return false;
    }

    public static PassiveResult evaluateForCharacter(String characterId, List<Integer> diceValues, boolean isAttacking) {
        return evaluateForCharacter(characterId, diceValues, isAttacking, -1, -1, 0);
    }

    public static PassiveResult evaluateForCharacter(String characterId, List<Integer> diceValues, boolean isAttacking, 
                                                       int currentHp, int maxHp, int currentToughnessLayers) {
        PassiveResult result = new PassiveResult();

        switch (characterId) {
            case "aventurine" -> evaluateAventurine(result, diceValues, isAttacking, currentToughnessLayers);
            case "firefly" -> evaluateFirefly(result, diceValues, isAttacking, currentHp, maxHp);
            case "acheron" -> evaluateAcheron(result, diceValues, isAttacking);
            case "kafka" -> evaluateKafka(result, diceValues, isAttacking);
            case "robin" -> evaluateRobin(result, diceValues, isAttacking);
            case "march_7th" -> evaluateMarch7th(result, diceValues, isAttacking);
            case "hyacine" -> evaluateHyacine(result, diceValues, isAttacking);
            case "dan_heng_pt" -> evaluateDanHengPT(result, diceValues, isAttacking);
            case "phainon" -> evaluatePhainon(result, diceValues, isAttacking);
            case "the_herta" -> evaluateTheHerta(result, diceValues, isAttacking);
            case "sparxie" -> evaluateSparxie(result, diceValues, isAttacking);
            case "cyrene" -> evaluateCyrene(result, diceValues, isAttacking);
            case "castorice" -> {}
            case "yao_guang" -> evaluateYaoGuang(result, diceValues, isAttacking);
        }

        return result;
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
            result.addAttackBonus(15);
        }
        if (currentHp > 0 && maxHp > 0 && currentHp == maxHp) {
            result.addAttackBonus(5);
        }
    }

    private static void evaluateAcheron(PassiveResult result, List<Integer> values, boolean isAttacking) {
        if (!isAttacking || values == null || values.isEmpty()) return;
        if (allDiceEqualFour(values)) {
            result.setPerforation(true);
            result.addAttackBonus(20);
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
            result.addAttackBonus(10);
        }
    }

    private static void evaluateMarch7th(PassiveResult result, List<Integer> values, boolean isAttacking) {
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
            result.addGrantedEffect(StatusEffect.COUNTER, 1);
            result.addDefenseBonus(10);
        }
    }

    private static void evaluatePhainon(PassiveResult result, List<Integer> values, boolean isAttacking) {
        if (values == null || values.isEmpty()) return;
        if (isAttacking) {
            result.addGrantedEffect(StatusEffect.SIPHON, 1);
        } else {
            if (allSame(values)) {
                result.addGrantedEffect(StatusEffect.UNYIELDING, 1);
                result.addDefenseBonus(50);
            }
        }
    }
    
    private static void evaluateTheHerta(PassiveResult result, List<Integer> values, boolean isAttacking) {
        // End-of-turn passive only, handled in evaluateEndOfTurnPassive
    }
    
    private static void evaluateSparxie(PassiveResult result, List<Integer> values, boolean isAttacking) {
        if (values == null || values.isEmpty()) return;
        if (hasIdenticalNumbers(values)) {
            result.addGrantedEffect(StatusEffect.HACK, 1);
        }
    }
    
    private static void evaluateCyrene(PassiveResult result, List<Integer> values, boolean isAttacking) {
        // Cumulative-based passive, handled in evaluateCyreneProgress
    }
    
    private static void evaluateYaoGuang(PassiveResult result, List<Integer> values, boolean isAttacking) {
        // Complex passive logic handled by separate processor
    }
    
    public static class EndOfTurnPassiveResult {
        private int prismaticUseBonus;
        private boolean grantArise;
        private int atkLevelBoost;
        
        public EndOfTurnPassiveResult() {
            this.prismaticUseBonus = 0;
            this.grantArise = false;
            this.atkLevelBoost = 0;
        }
        
        public void addPrismaticUseBonus(int bonus) {
            this.prismaticUseBonus += bonus;
        }
        
        public void setGrantArise(boolean grant) {
            this.grantArise = grant;
        }
        
        public void setAtkLevelBoost(int boost) {
            this.atkLevelBoost = boost;
        }
        
        public int getPrismaticUseBonus() {
            return prismaticUseBonus;
        }
        
        public boolean shouldGrantArise() {
            return grantArise;
        }
        
        public int getAtkLevelBoost() {
            return atkLevelBoost;
        }
        
        public boolean hasEffects() {
            return prismaticUseBonus > 0 || grantArise || atkLevelBoost > 0;
        }
    }
    
    public static EndOfTurnPassiveResult evaluateEndOfTurnPassive(String characterId, int prismaticTriggerCount, int cumulativeAtkDef) {
        EndOfTurnPassiveResult result = new EndOfTurnPassiveResult();
        
        switch (characterId) {
            case "the_herta" -> evaluateTheHertaEndOfTurn(result, prismaticTriggerCount);
            case "cyrene" -> evaluateCyreneEndOfTurn(result, cumulativeAtkDef);
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
        if (cumulativeTotal > 24) {
            result.setGrantArise(true);
        }
    }
    
    public static boolean shouldApplyCyreneAtkBoost(boolean thresholdAlreadyMet, int cumulativeTotal) {
        return !thresholdAlreadyMet && cumulativeTotal > 24;
    }

    public static CyreneProgressResult evaluateCyreneProgress(int cumulativeTotal, int currentTurnValue) {
        CyreneProgressResult result = new CyreneProgressResult();
        int newTotal = cumulativeTotal + currentTurnValue;
        result.setNewCumulativeTotal(newTotal);
        result.setThresholdMet(newTotal > 24);
        if (result.isThresholdMet()) {
            result.setAtkLevelBoost(2);
            result.setGrantArise(true);
        }
        return result;
    }

    public static class CyreneProgressResult {
        private int newCumulativeTotal;
        private boolean thresholdMet;
        private int atkLevelBoost;
        private boolean grantArise;

        public int getNewCumulativeTotal() { return newCumulativeTotal; }
        public void setNewCumulativeTotal(int value) { this.newCumulativeTotal = value; }
        public boolean isThresholdMet() { return thresholdMet; }
        public void setThresholdMet(boolean met) { this.thresholdMet = met; }
        public int getAtkLevelBoost() { return atkLevelBoost; }
        public void setAtkLevelBoost(int boost) { this.atkLevelBoost = boost; }
        public boolean shouldGrantArise() { return grantArise; }
        public void setGrantArise(boolean grant) { this.grantArise = grant; }
    }

    public static PassiveEvaluation toPassiveEvaluation(PassiveResult result, String description) {
        if (!result.hasEffects()) {
            return PassiveEvaluation.notTriggered();
        }
        int totalBonus = result.getAttackBonus() + result.getDefenseBonus();
        for (GrantedEffect ge : result.getGrantedEffects()) {
            totalBonus += ge.layers;
        }
        return PassiveEvaluation.triggered(totalBonus, description);
    }

    public static PostDamageResult evaluatePostDamageForCharacter(String characterId, int damageTaken) {
        PostDamageResult result = new PostDamageResult();
        
        switch (characterId) {
            case "castorice" -> evaluateCastoricePostDamage(result, damageTaken);
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

    public static void applyPassiveEffects(PassiveResult result, BattleState state, boolean forPlayer) {
        if (result == null || !result.hasEffects()) return;
        
        StatusEffectProcessor effects = (forPlayer ? state.getPlayerEffects() : state.getOpponentEffects());
        
        for (GrantedEffect ge : result.getGrantedEffects()) {
            effects.addEffect(ge.effect, ge.layers);
        }
        
        if (result.shouldTriggerToughnessInstantDamage()) {
            int currentToughness = effects.getLayers(StatusEffect.TOUGHNESS);
            int newToughness = Math.max(0, currentToughness - result.getToughnessToRemove());
            effects.setEffect(StatusEffect.TOUGHNESS, newToughness);
            
            boolean opponent = !forPlayer;
            state.getEffectManager().applyEffect(StatusEffect.INSTANT_DAMAGE, result.getInstantDamageToOpponent(), opponent);
        }
        
        if (result.getHealAmount() > 0) {
            state.applyHealTo(forPlayer, result.getHealAmount());
        }
        
        if (result.hasPerforation()) {
            effects.addEffect(StatusEffect.PERFORATION, 1);
        }
    }
}