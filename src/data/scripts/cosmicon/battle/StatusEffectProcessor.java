package data.scripts.cosmicon.battle;

import data.scripts.cosmicon.battle.TurnState.TurnType;
import data.scripts.cosmicon.util.CosmiconLogger;
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
        REFLECT,
        CYRENE_TALLY
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
    private int lastStandHpReduction;

    private final List<ProcessedEffect> processedEffects = new ArrayList<>();

    public static final int PERMANENT_DURATION = Integer.MAX_VALUE;

    public record ProcessedEffect(StatusEffect effect, int layers) {}

    public StatusEffectProcessor() {
        this.effects = new EnumMap<>(StatusEffect.class);
        this.durations = new EnumMap<>(StatusEffect.class);
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

    public void removeLayers(StatusEffect effect, int layers) {
        int current = getLayers(effect);
        if (current <= layers) {
            removeEffect(effect);
        } else {
            effects.put(effect, current - layers);
        }
    }

    public void setEffect(StatusEffect effect, int layers) {
        if (layers <= 0) {
            if (hasEffect(effect)) {
                int oldLayers = getLayers(effect);
                effects.remove(effect);
                durations.remove(effect);
                CosmiconLogger.debug("Effect cleared: %s (was %d layers)", effect.name(), oldLayers);
            }
        } else {
            int oldLayers = getLayers(effect);
            effects.put(effect, layers);
            if (oldLayers != layers) {
                CosmiconLogger.debug("Effect set: %s %d layers (was %d)", effect.name(), layers, oldLayers);
            }
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
        if (hasEffect(StatusEffect.LEVEL_UP)) {
            effects.remove(StatusEffect.LEVEL_UP);
            durations.remove(StatusEffect.LEVEL_UP);
        }
        if (hasEffect(StatusEffect.UNYIELDING)) {
            effects.remove(StatusEffect.UNYIELDING);
            durations.remove(StatusEffect.UNYIELDING);
        }
    }

    public List<ProcessedEffect> getAndClearProcessedEffects() {
        List<ProcessedEffect> result = new ArrayList<>(processedEffects);
        processedEffects.clear();
        return result;
    }

    public void processedEffectsFromModification(StatusEffect effect) {
        if (hasEffect(effect)) {
            processedEffects.add(new ProcessedEffect(effect, getLayers(effect)));
        }
    }

    public int processPhase(Phase phase, TurnType turnType, BattleContext context) {
        int totalDamage = 0;

        switch (phase) {
            case START_OF_TURN -> processStartOfTurn(turnType, context);
            case BEFORE_ROLL -> processBeforeRoll(turnType, context);
            case AFTER_ROLL -> processAfterRoll(context);
            case AFTER_SELECT -> processAfterSelect(context);
            case BEFORE_RESOLUTION -> totalDamage += processBeforeResolution(context);
            case END_OF_TURN -> totalDamage += processEndOfTurn();
            case BEFORE_SELECT, AFTER_RESOLUTION -> {}
        }

        return totalDamage;
    }

    private void processStartOfTurn(TurnType turnType, BattleContext context) {
        if (turnType == TurnType.ATTACK && hasEffect(StatusEffect.LAST_STAND)) {
            int layers = getLayers(StatusEffect.LAST_STAND);
            lastStandHpReduction = context.getCurrentHp() - 1;
            context.setCurrentHp(1);
            processedEffects.add(new ProcessedEffect(StatusEffect.LAST_STAND, layers));
            effects.remove(StatusEffect.LAST_STAND);
            durations.remove(StatusEffect.LAST_STAND);
            CosmiconLogger.debug("LAST_STAND: HP reduced from %d to 1, bonus = %d", 
                lastStandHpReduction + 1, lastStandHpReduction);
        }
    }

    private void processBeforeRoll(TurnType turnType, BattleContext context) {
        if (turnType == TurnType.ATTACK) {
            int tacticsLayers = getLayers(StatusEffect.TACTICS);
            if (tacticsLayers > 0) {
                context.addRerolls(tacticsLayers);
                processedEffects.add(new ProcessedEffect(StatusEffect.TACTICS, tacticsLayers));
            }
            
            int yaoGuangRerolls = getLayers(StatusEffect.YAO_GUANG_REROLLS);
            if (yaoGuangRerolls > 0) {
                context.addRerolls(yaoGuangRerolls);
                processedEffects.add(new ProcessedEffect(StatusEffect.YAO_GUANG_REROLLS, yaoGuangRerolls));
            }
        }

        int deterrenceLayers = getLayers(StatusEffect.DETERRENCE);
        if (deterrenceLayers > 0) {
            context.reduceRerolls(deterrenceLayers);
            processedEffects.add(new ProcessedEffect(StatusEffect.DETERRENCE, deterrenceLayers));
        }
    }

    private void processAfterRoll(BattleContext context) {
        if (hasEffect(StatusEffect.DESTINED)) {
            processedEffects.add(new ProcessedEffect(StatusEffect.DESTINED, getLayers(StatusEffect.DESTINED)));
            context.markDestinedDice();
            effects.remove(StatusEffect.DESTINED);
            durations.remove(StatusEffect.DESTINED);
        }
    }

    private void processAfterSelect(BattleContext context) {
        if (hasEffect(StatusEffect.LEVEL_UP)) {
            int layers = getLayers(StatusEffect.LEVEL_UP);
            processedEffects.add(new ProcessedEffect(StatusEffect.LEVEL_UP, layers));
            context.applyLevelUp(layers);
        }

        if (hasEffect(StatusEffect.AWAKENING)) {
            processedEffects.add(new ProcessedEffect(StatusEffect.AWAKENING, getLayers(StatusEffect.AWAKENING)));
            context.applyAwakening();
            effects.remove(StatusEffect.AWAKENING);
            durations.remove(StatusEffect.AWAKENING);
        }
    }

    private int processBeforeResolution(BattleContext context) {
        int damage = 0;

        if (hasEffect(StatusEffect.THORNS)) {
            int thornsDamage = getLayers(StatusEffect.THORNS);
            processedEffects.add(new ProcessedEffect(StatusEffect.THORNS, thornsDamage));
            damage += thornsDamage;
        }

        if (hasEffect(StatusEffect.INSTANT_DAMAGE)) {
            int instantDamage = getLayers(StatusEffect.INSTANT_DAMAGE);
            processedEffects.add(new ProcessedEffect(StatusEffect.INSTANT_DAMAGE, instantDamage));
            context.subtractInstantDamageFromHolder(instantDamage);
            effects.remove(StatusEffect.INSTANT_DAMAGE);
            durations.remove(StatusEffect.INSTANT_DAMAGE);
        }

        return damage;
    }

    private int processEndOfTurn() {
        int damage = 0;

        if (hasEffect(StatusEffect.POISON)) {
            int poisonLayers = getLayers(StatusEffect.POISON);
            int poisonDamage = poisonLayers;
            if (hasEffect(StatusEffect.VENOM)) {
                poisonDamage *= 2;
                CosmiconLogger.debug("POISON ticked: %d damage (doubled by VENOM)", poisonDamage);
            } else {
                CosmiconLogger.debug("POISON ticked: %d damage", poisonDamage);
            }
            processedEffects.add(new ProcessedEffect(StatusEffect.POISON, poisonLayers));
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

            if (lastStandHpReduction > 0) {
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
            int percentage = getLayers(StatusEffect.SIPHON);
            return (int)(damage * percentage / 100.0f);
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
        lastStandHpReduction = 0;
    }

    public static class BattleContext {
        private int currentHp;
        private final int maxHp;
        private int rerollCount;
        private List<Integer> diceValues;
        private List<Boolean> diceSelected;
        private List<Boolean> diceIsPrismatic;
        private List<Integer> diceMaxFaces;
        private List<DiceType> diceTypes;
        private int instantDamageToOpponent;
        private int instantDamageFromHolder;

        public BattleContext(int hp, int maxHp) {
            this.currentHp = hp;
            this.maxHp = maxHp;
            this.rerollCount = 0;
            this.diceValues = new ArrayList<>();
            this.diceSelected = new ArrayList<>();
            this.diceIsPrismatic = new ArrayList<>();
            this.diceMaxFaces = new ArrayList<>();
            this.diceTypes = new ArrayList<>();
            this.instantDamageToOpponent = 0;
            this.instantDamageFromHolder = 0;
        }

        public void setDiceValues(List<Integer> values, List<DiceType> types) {
            this.diceValues = new ArrayList<>(values);
            this.diceIsPrismatic = new ArrayList<>();
            for (DiceType type : types) {
                this.diceIsPrismatic.add(type == DiceType.PRISMATIC);
            }
            this.diceSelected = new ArrayList<>(java.util.Collections.nCopies(values.size(), false));
        }

        public void setDiceMaxFaces(List<Integer> maxFaces) {
            this.diceMaxFaces = new ArrayList<>(maxFaces);
        }

        public void setDiceTypes(List<DiceType> types) {
            this.diceTypes = new ArrayList<>(types);
        }

        public List<DiceType> getDiceTypes() {
            return diceTypes;
        }

        public void setDiceSelected(List<Boolean> selected) {
            this.diceSelected = new ArrayList<>(selected);
        }

        public int getCurrentHp() {
            return currentHp;
        }

        public void setCurrentHp(int hp) {
            this.currentHp = Math.max(0, Math.min(hp, maxHp));
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

        public int applyHackToSelectedDice() {
            int maxIndex = -1;
            int maxValue = -1;
            for (int i = 0; i < diceValues.size(); i++) {
                if (diceSelected.get(i) && !diceIsPrismatic.get(i) && diceValues.get(i) > maxValue) {
                    maxValue = diceValues.get(i);
                    maxIndex = i;
                }
            }
            if (maxIndex >= 0) {
                CosmiconLogger.debug("HACK: Transformed highest dice %d to 2", maxValue);
                diceValues.set(maxIndex, 2);
                return maxIndex;
            }
            return -1;
        }

        public int applyArise() {
            int minIndex = -1;
            int minValue = Integer.MAX_VALUE;
            int minMaxFace = 0;
            for (int i = 0; i < diceValues.size(); i++) {
                if (diceSelected.get(i) && !diceIsPrismatic.get(i) && diceValues.get(i) < minValue) {
                    minValue = diceValues.get(i);
                    minIndex = i;
                    minMaxFace = (i < diceMaxFaces.size()) ? diceMaxFaces.get(i) : getDiceMaxFace(i);
                }
            }
            if (minIndex >= 0) {
                diceValues.set(minIndex, minMaxFace);
                return minIndex;
            }
            return -1;
        }

        private int getDiceMaxFace(int index) {
            int value = diceValues.get(index);
            if (value <= 4) return 4;
            if (value <= 6) return 6;
            if (value <= 8) return 8;
            return 12;
        }

        public void applyLevelUp(int count) {
            for (int i = 0; i < diceValues.size(); i++) {
                if (diceSelected.get(i) && !diceIsPrismatic.get(i)) {
                    int currentMaxFace = (i < diceMaxFaces.size()) ? diceMaxFaces.get(i) : getDiceMaxFace(i);
                    for (int j = 0; j < count; j++) {
                        if (currentMaxFace >= 12) break;
                        currentMaxFace = upgradeDiceMaxFace(currentMaxFace);
                    }
                    if (i < diceMaxFaces.size()) {
                        diceMaxFaces.set(i, currentMaxFace);
                    }
                    if (i < diceTypes.size()) {
                        diceTypes.set(i, DiceType.fromMaxFace(currentMaxFace));
                    }
                }
            }
        }

        private int upgradeDiceMaxFace(int currentMaxFace) {
            if (currentMaxFace <= 4) return 6;
            if (currentMaxFace <= 6) return 8;
            if (currentMaxFace <= 8) return 12;
            return currentMaxFace;
        }

        public void applyAwakening() {
            for (int i = 0; i < diceValues.size(); i++) {
                if (diceSelected.get(i)) {
                    diceValues.set(i, diceValues.get(i) * 2);
                }
            }
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

        public void subtractInstantDamageFromHolder(int damage) {
            this.instantDamageFromHolder += damage;
        }

        public int getInstantDamageFromHolder() {
            return instantDamageFromHolder;
        }
    }
}