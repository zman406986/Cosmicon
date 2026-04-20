package data.scripts.cosmicon.battle;

import data.scripts.cosmicon.battle.BattleState.TurnType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class StatusEffectProcessor {

    public enum StatusEffect {
        POISON,
        STRENGTH,
        TOUGHNESS,
        DETERRENCE,
        FORCEFIELD,
        PERFORATION,
        COMBO,
        COUNTER,
        UNYIELDING,
        HACK,
        THORNS,
        ARISE,
        AWAKENING,
        LEVEL_UP,
        LAST_STAND,
        OVERLOAD,
        SIPHON,
        DESTINED,
        VENOM,
        INSTANT_DAMAGE,
        TACTICS,
        YAO_GUANG_REROLLS,
        REFLECT
    }

    public enum Phase {
        START_OF_TURN,
        BEFORE_ROLL,
        AFTER_ROLL,
        BEFORE_SELECT,
        AFTER_SELECT,
        BEFORE_RESOLUTION,
        AFTER_RESOLUTION,
        END_OF_TURN
    }

    private final Map<StatusEffect, Integer> effects;
    private final Map<StatusEffect, Integer> durations;
    private boolean unyieldingActive;
    private boolean comboTriggered;
    private int lastStandHpReduction;

    public static final int PERMANENT_DURATION = Integer.MAX_VALUE;

    public StatusEffectProcessor() {
        this.effects = new EnumMap<>(StatusEffect.class);
        this.durations = new EnumMap<>(StatusEffect.class);
        this.unyieldingActive = false;
        this.comboTriggered = false;
        this.lastStandHpReduction = 0;
    }

    public void addEffect(StatusEffect effect, int layers) {
        addEffect(effect, layers, PERMANENT_DURATION);
    }

    public void addEffect(StatusEffect effect, int layers, int duration) {
        effects.merge(effect, layers, Integer::sum);
        if (duration < PERMANENT_DURATION) {
            durations.put(effect, duration);
        } else {
            durations.remove(effect);
        }
    }

    public void removeEffect(StatusEffect effect) {
        effects.remove(effect);
        durations.remove(effect);
    }

    public void setEffect(StatusEffect effect, int layers) {
        if (layers <= 0) {
            effects.remove(effect);
            durations.remove(effect);
        } else {
            effects.put(effect, layers);
        }
    }

    public int getLayers(StatusEffect effect) {
        return effects.getOrDefault(effect, 0);
    }

    public boolean hasEffect(StatusEffect effect) {
        return getLayers(effect) > 0;
    }

    public int getDuration(StatusEffect effect) {
        return durations.getOrDefault(effect, PERMANENT_DURATION);
    }

    public void clearTemporaryEffects() {
        if (hasEffect(StatusEffect.THORNS)) {
            effects.remove(StatusEffect.THORNS);
            durations.remove(StatusEffect.THORNS);
        }
    }

    public int processPhase(Phase phase, TurnType turnType, BattleContext context) {
        int totalDamage = 0;

        switch (phase) {
            case START_OF_TURN -> totalDamage += processStartOfTurn();
            case BEFORE_ROLL -> processBeforeRoll(turnType, context);
            case AFTER_ROLL -> processAfterRoll(turnType, context);
            case AFTER_SELECT -> processAfterSelect(context);
            case BEFORE_RESOLUTION -> totalDamage += processBeforeResolution(context);
            case AFTER_RESOLUTION -> processAfterResolution(turnType, context);
            case END_OF_TURN -> totalDamage += processEndOfTurn();
            case BEFORE_SELECT -> {}
        }

        return totalDamage;
    }

    private int processStartOfTurn() {
        return 0;
    }

    private void processBeforeRoll(TurnType turnType, BattleContext context) {
        if (turnType == TurnType.ATTACK) {
            int tacticsLayers = getLayers(StatusEffect.TACTICS);
            if (tacticsLayers > 0) {
                context.addRerolls(tacticsLayers);
            }
            
            int yaoGuangRerolls = getLayers(StatusEffect.YAO_GUANG_REROLLS);
            if (yaoGuangRerolls > 0) {
                context.addRerolls(yaoGuangRerolls);
            }
        }

        int deterrenceLayers = getLayers(StatusEffect.DETERRENCE);
        if (deterrenceLayers > 0) {
            context.reduceRerolls(deterrenceLayers);
        }
    }

    private void processAfterRoll(TurnType turnType, BattleContext context) {
        if (hasEffect(StatusEffect.HACK) && turnType == TurnType.ATTACK) {
            context.applyHack();
        }

        if (hasEffect(StatusEffect.ARISE) && turnType == TurnType.ATTACK) {
            context.applyArise();
        }

        if (hasEffect(StatusEffect.DESTINED)) {
            context.markDestinedDice();
        }
    }

    private void processAfterSelect(BattleContext context) {
        if (hasEffect(StatusEffect.LEVEL_UP)) {
            int layers = getLayers(StatusEffect.LEVEL_UP);
            context.applyLevelUp(layers);
            effects.remove(StatusEffect.LEVEL_UP);
            durations.remove(StatusEffect.LEVEL_UP);
        }

        if (hasEffect(StatusEffect.AWAKENING)) {
            context.applyAwakening();
        }
    }

    private int processBeforeResolution(BattleContext context) {
        int damage = 0;

        if (hasEffect(StatusEffect.THORNS)) {
            int thornsDamage = getLayers(StatusEffect.THORNS);
            damage += thornsDamage;
            effects.remove(StatusEffect.THORNS);
            durations.remove(StatusEffect.THORNS);
        }

        if (hasEffect(StatusEffect.INSTANT_DAMAGE)) {
            int instantDamage = getLayers(StatusEffect.INSTANT_DAMAGE);
            context.addInstantDamageToOpponent(instantDamage);
            effects.remove(StatusEffect.INSTANT_DAMAGE);
            durations.remove(StatusEffect.INSTANT_DAMAGE);
        }

        return damage;
    }

    private void processAfterResolution(TurnType turnType, BattleContext context) {
        if (hasEffect(StatusEffect.LAST_STAND)) {
            lastStandHpReduction = context.getCurrentHp() - 1;
            context.setCurrentHp(1);
            effects.remove(StatusEffect.LAST_STAND);
            durations.remove(StatusEffect.LAST_STAND);
        }

        if (hasEffect(StatusEffect.UNYIELDING) && context.getCurrentHp() <= 0) {
            context.setCurrentHp(1);
            unyieldingActive = true;
        }

        if (hasEffect(StatusEffect.COMBO) && turnType == TurnType.ATTACK) {
            comboTriggered = true;
        }
    }

    private int processEndOfTurn() {
        int damage = 0;

        if (hasEffect(StatusEffect.POISON)) {
            int poisonLayers = getLayers(StatusEffect.POISON);
            int poisonDamage = poisonLayers;
            if (hasEffect(StatusEffect.VENOM)) {
                poisonDamage *= 2;
            }
            damage += poisonDamage;
            setEffect(StatusEffect.POISON, poisonLayers - 1);
        }

        decrementDurations();

        return damage;
    }

    private void decrementDurations() {
        List<StatusEffect> expired = new ArrayList<>();
        for (Map.Entry<StatusEffect, Integer> entry : durations.entrySet()) {
            StatusEffect effect = entry.getKey();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                expired.add(effect);
            } else {
                durations.put(effect, remaining);
            }
        }
        for (StatusEffect effect : expired) {
            effects.remove(effect);
            durations.remove(effect);
        }
    }

    public int calculateAttackBonus(TurnType turnType) {
        int bonus = 0;

        if (turnType == TurnType.ATTACK) {
            bonus += getLayers(StatusEffect.STRENGTH);

            if (hasEffect(StatusEffect.OVERLOAD)) {
                bonus += getLayers(StatusEffect.OVERLOAD);
            }

            if (hasEffect(StatusEffect.LAST_STAND) && lastStandHpReduction > 0) {
                bonus += lastStandHpReduction;
            }
        }

        return bonus;
    }

    public int calculateDefenseBonus(TurnType turnType) {
        int bonus = 0;

        if (turnType == TurnType.DEFENSE) {
            bonus += getLayers(StatusEffect.TOUGHNESS);
        }

        return bonus;
    }

    public int calculateOverloadSelfDamage(TurnType turnType) {
        if (turnType == TurnType.DEFENSE && hasEffect(StatusEffect.OVERLOAD)) {
            return getLayers(StatusEffect.OVERLOAD) / 2;
        }
        return 0;
    }

    public int calculateSiphonHeal(int damage) {
        if (hasEffect(StatusEffect.SIPHON) && damage > 0) {
            return getLayers(StatusEffect.SIPHON);
        }
        return 0;
    }

    public boolean shouldIgnoreDefense() {
        return hasEffect(StatusEffect.PERFORATION);
    }

    public boolean isForcefieldActive() {
        return hasEffect(StatusEffect.FORCEFIELD);
    }

    public int calculateCounterDamage(int attackValue, int defenseValue) {
        if (hasEffect(StatusEffect.COUNTER) && defenseValue > attackValue) {
            return defenseValue - attackValue;
        }
        return 0;
    }

    public void resetTurnState() {
    }

    public static class BattleContext {
        private int currentHp;
        private final int maxHp;
        private int rerollCount;
        private final List<Integer> diceValues;
        private final List<Boolean> diceSelected;
        private final List<Boolean> diceIsPrismatic;
        private int instantDamageToOpponent;

        public BattleContext(int hp, int maxHp) {
            this.currentHp = hp;
            this.maxHp = maxHp;
            this.rerollCount = 0;
            this.diceValues = new ArrayList<>();
            this.diceSelected = new ArrayList<>();
            this.diceIsPrismatic = new ArrayList<>();
            this.instantDamageToOpponent = 0;
        }

        public void setDiceValues(List<Integer> values, List<Boolean> isPrismatic) {
            diceValues.clear();
            diceValues.addAll(values);
            diceIsPrismatic.clear();
            diceIsPrismatic.addAll(isPrismatic);
            diceSelected.clear();
            for (int i = 0; i < values.size(); i++) {
                diceSelected.add(false);
            }
        }

        public int getCurrentHp() {
            return currentHp;
        }

        public void setCurrentHp(int hp) {
            this.currentHp = Math.max(0, Math.min(hp, maxHp));
        }

        public int getMaxHp() {
            return maxHp;
        }

        public int getRerollCount() {
            return rerollCount;
        }

        public void addRerolls(int count) {
            rerollCount += count;
        }

        public void reduceRerolls(int count) {
            rerollCount = Math.max(0, rerollCount - count);
        }

        public List<Integer> getDiceValues() {
            return diceValues;
        }

        public void applyHack() {
            int maxIndex = -1;
            int maxValue = -1;
            for (int i = 0; i < diceValues.size(); i++) {
                if (!diceIsPrismatic.get(i) && diceValues.get(i) > maxValue) {
                    maxValue = diceValues.get(i);
                    maxIndex = i;
                }
            }
            if (maxIndex >= 0) {
                diceValues.set(maxIndex, 2);
            }
        }

        public void applyArise() {
            int minIndex = -1;
            int minValue = Integer.MAX_VALUE;
            int minMaxFace = 0;
            for (int i = 0; i < diceValues.size(); i++) {
                if (!diceIsPrismatic.get(i) && diceValues.get(i) < minValue) {
                    minValue = diceValues.get(i);
                    minIndex = i;
                    minMaxFace = getDiceMaxFace(i);
                }
            }
            if (minIndex >= 0) {
                diceValues.set(minIndex, minMaxFace);
            }
        }

        private int getDiceMaxFace(int index) {
            int value = diceValues.get(index);
            if (value <= 4) return 4;
            if (value <= 6) return 6;
            if (value <= 8) return 8;
            return 12;
        }

        public void applyLevelUp(int count) {
            for (int i = 0; i < diceValues.size() && count > 0; i++) {
                if (diceSelected.get(i) && !diceIsPrismatic.get(i)) {
                    int current = diceValues.get(i);
                    int upgraded = upgradeDice(current);
                    diceValues.set(i, upgraded);
                    count--;
                }
            }
        }

        public void applyAwakening() {
            for (int i = 0; i < diceValues.size(); i++) {
                if (diceSelected.get(i)) {
                    diceValues.set(i, diceValues.get(i) * 2);
                }
            }
        }

        private int upgradeDice(int currentMax) {
            if (currentMax <= 4) return 6;
            if (currentMax <= 6) return 8;
            return 12;
        }

        public void markDestinedDice() {
            for (int i = 0; i < diceValues.size(); i++) {
                diceSelected.set(i, true);
            }
        }

        public void addInstantDamageToOpponent(int damage) {
            this.instantDamageToOpponent += damage;
        }

        public int getInstantDamageToOpponent() {
            return instantDamageToOpponent;
        }
    }
}