package data.scripts.cosmicon.battle;

import data.scripts.cosmicon.battle.TurnState.TurnType;
import data.scripts.cosmicon.util.CosmiconLogger;
import java.util.ArrayList;
import java.util.List;

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

    public enum DurationType {
        TURN_BASED,
        USAGE_BASED,
        PERMANENT
    }

    public record StatusEffectInstance(
        StatusEffect effect,
        String source,
        int layers,
        DurationType durationType,
        int remainingTurns
    ) {}

    private final List<StatusEffectInstance> activeEffects = new ArrayList<>();
    private int lastStandHpReduction;

    private final List<ProcessedEffect> processedEffects = new ArrayList<>();

    public record ProcessedEffect(StatusEffect effect, int layers) {}

    public StatusEffectProcessor() {
        this.lastStandHpReduction = 0;
    }

    public void addEffect(StatusEffect effect, String source, int layers, DurationType durationType) {
        addEffect(effect, source, layers, durationType, durationType == DurationType.TURN_BASED ? 1 : -1);
    }

    public void addEffect(StatusEffect effect, String source, int layers, DurationType durationType, int turns) {
        int existingIndex = -1;
        for (int i = 0; i < activeEffects.size(); i++) {
            StatusEffectInstance inst = activeEffects.get(i);
            if (inst.effect() == effect && inst.source().equals(source)) {
                existingIndex = i;
                break;
            }
        }

        String string = durationType == DurationType.TURN_BASED ? turns + "t" : durationType.name();
        if (existingIndex >= 0) {
            StatusEffectInstance old = activeEffects.get(existingIndex);
            int mergedLayers = old.layers() + layers;
            int newTurns = old.remainingTurns();
            if (durationType == DurationType.TURN_BASED && old.durationType() == DurationType.TURN_BASED) {
                newTurns = Math.min(old.remainingTurns(), turns);
            }
            activeEffects.set(existingIndex, new StatusEffectInstance(effect, source, mergedLayers, durationType, newTurns));
            CosmiconLogger.info("[STATUS] +%d %s from %s (was %d, now %d layers, duration=%s)",
                layers, effect.name(), source, old.layers(), mergedLayers,
                    string);
        } else {
            activeEffects.add(new StatusEffectInstance(effect, source, layers, durationType, turns));
            CosmiconLogger.info("[STATUS] +%d %s from %s (new instance, duration=%s)",
                layers, effect.name(), source,
                    string);
        }
    }

    public int getLayers(StatusEffect effect) {
        int total = 0;
        for (StatusEffectInstance inst : activeEffects) {
            if (inst.effect() == effect) {
                total += inst.layers();
            }
        }
        return total;
    }

    public boolean hasEffect(StatusEffect effect) {
        return getLayers(effect) > 0;
    }

    public List<StatusEffectInstance> getActiveEffects() {
        return activeEffects;
    }

    public void removeEffect(StatusEffect effect) {
        int totalLayers = getLayers(effect);
        if (totalLayers > 0) {
            CosmiconLogger.info("[STATUS] Removed all %s (was %d layers)", effect.name(), totalLayers);
        }
        activeEffects.removeIf(inst -> inst.effect() == effect);
    }

    public void removeFromSource(StatusEffect effect, String source) {
        boolean removed = activeEffects.removeIf(inst -> inst.effect() == effect && inst.source().equals(source));
        if (removed) {
            CosmiconLogger.info("[STATUS] Removed %s from source %s", effect.name(), source);
        }
    }

    public void removeLayersFromSource(StatusEffect effect, String source, int layers) {
        for (int i = 0; i < activeEffects.size(); i++) {
            StatusEffectInstance inst = activeEffects.get(i);
            if (inst.effect() == effect && inst.source().equals(source)) {
                if (inst.layers() <= layers) {
                    activeEffects.remove(i);
                    CosmiconLogger.info("[STATUS] Removed %s from %s (all layers consumed)", effect.name(), source);
                } else {
                    activeEffects.set(i, new StatusEffectInstance(effect, source, inst.layers() - layers, inst.durationType(), inst.remainingTurns()));
                    CosmiconLogger.info("[STATUS] -%d %s from %s (now %d layers)", layers, effect.name(), source, inst.layers() - layers);
                }
                return;
            }
        }
    }

    public void setEffectFromSource(StatusEffect effect, String source, int layers) {
        for (int i = 0; i < activeEffects.size(); i++) {
            StatusEffectInstance inst = activeEffects.get(i);
            if (inst.effect() == effect && inst.source().equals(source)) {
                if (layers <= 0) {
                    activeEffects.remove(i);
                    CosmiconLogger.info("[STATUS] Effect cleared: %s from %s (was %d layers)", effect.name(), source, inst.layers());
                } else {
                    activeEffects.set(i, new StatusEffectInstance(effect, source, layers, inst.durationType(), inst.remainingTurns()));
                    CosmiconLogger.info("[STATUS] Effect set: %s from %s %d layers (was %d)", effect.name(), source, layers, inst.layers());
                }
                return;
            }
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

    public void clearTemporaryEffects() {
        removeEffect(StatusEffect.LEVEL_UP);
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
            removeEffect(StatusEffect.LAST_STAND);
            CosmiconLogger.info("[STATUS] LAST_STAND triggered: HP %d -> 1, attack bonus = %d",
                lastStandHpReduction + 1, lastStandHpReduction);
        }
    }

    private void processBeforeRoll(TurnType turnType, BattleContext context) {
        if (turnType == TurnType.ATTACK) {
            int tacticsLayers = getLayers(StatusEffect.TACTICS);
            if (tacticsLayers > 0) {
                context.addRerolls(tacticsLayers);
                processedEffects.add(new ProcessedEffect(StatusEffect.TACTICS, tacticsLayers));
                CosmiconLogger.info("[STATUS] TACTICS: +%d rerolls", tacticsLayers);
            }

            int yaoGuangRerolls = getLayers(StatusEffect.YAO_GUANG_REROLLS);
            if (yaoGuangRerolls > 0) {
                context.addRerolls(yaoGuangRerolls);
                processedEffects.add(new ProcessedEffect(StatusEffect.YAO_GUANG_REROLLS, yaoGuangRerolls));
                CosmiconLogger.info("[STATUS] YAO_GUANG_REROLLS: +%d rerolls", yaoGuangRerolls);
            }
        }

        int deterrenceLayers = getLayers(StatusEffect.DETERRENCE);
        if (deterrenceLayers > 0) {
            context.reduceRerolls(deterrenceLayers);
            processedEffects.add(new ProcessedEffect(StatusEffect.DETERRENCE, deterrenceLayers));
            CosmiconLogger.info("[STATUS] DETERRENCE: -%d rerolls", deterrenceLayers);
        }
    }

    private void processAfterRoll(BattleContext context) {
        if (hasEffect(StatusEffect.DESTINED)) {
            int layers = getLayers(StatusEffect.DESTINED);
            processedEffects.add(new ProcessedEffect(StatusEffect.DESTINED, layers));
            CosmiconLogger.info("[STATUS] DESTINED: auto-selecting all dice");
            context.markDestinedDice();
            removeEffect(StatusEffect.DESTINED);
        }
    }

    private void processAfterSelect(BattleContext context) {
        if (hasEffect(StatusEffect.LEVEL_UP)) {
            int layers = getLayers(StatusEffect.LEVEL_UP);
            processedEffects.add(new ProcessedEffect(StatusEffect.LEVEL_UP, layers));
            CosmiconLogger.info("[STATUS] LEVEL_UP: layers=%d | context: types=%s selected=%s prismatic=%s",
                layers,
                context.getDiceTypes(),
                context.getDiceSelected(),
                context.getDiceIsPrismatic());
            context.applyLevelUp(layers);
        }

        if (hasEffect(StatusEffect.AWAKENING)) {
            int layers = getLayers(StatusEffect.AWAKENING);
            processedEffects.add(new ProcessedEffect(StatusEffect.AWAKENING, layers));
            CosmiconLogger.info("[STATUS] AWAKENING: doubling selected dice values");
            context.applyAwakening();
            removeEffect(StatusEffect.AWAKENING);
        }
    }

    private int processBeforeResolution(BattleContext context) {
        int damage = 0;

        if (hasEffect(StatusEffect.THORNS)) {
            int thornsDamage = getLayers(StatusEffect.THORNS);
            processedEffects.add(new ProcessedEffect(StatusEffect.THORNS, thornsDamage));
            CosmiconLogger.info("[STATUS] THORNS: dealing %d damage to attacker", thornsDamage);
            damage += thornsDamage;
        }

        if (hasEffect(StatusEffect.INSTANT_DAMAGE)) {
            for (StatusEffectInstance inst : activeEffects) {
                if (inst.effect() == StatusEffect.INSTANT_DAMAGE) {
                    processedEffects.add(new ProcessedEffect(StatusEffect.INSTANT_DAMAGE, inst.layers()));
                    CosmiconLogger.info("[STATUS] INSTANT_DAMAGE: %d self-damage from %s", inst.layers(), inst.source());
                    context.subtractInstantDamageFromHolder(inst.layers());
                    removeFromSource(StatusEffect.INSTANT_DAMAGE, inst.source());
                    break;
                }
            }
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
            }
            CosmiconLogger.info("[STATUS] POISON: %d damage%s", poisonDamage,
                hasEffect(StatusEffect.VENOM) ? " (doubled by VENOM)" : "");
            processedEffects.add(new ProcessedEffect(StatusEffect.POISON, poisonLayers));
            damage += poisonDamage;

            for (StatusEffectInstance inst : activeEffects) {
                if (inst.effect() == StatusEffect.POISON) {
                    removeLayersFromSource(StatusEffect.POISON, inst.source(), 1);
                    break;
                }
            }
        }

        decrementDurations();

        return damage;
    }

    private void decrementDurations() {
        List<StatusEffectInstance> expired = new ArrayList<>();
        List<StatusEffectInstance> surviving = new ArrayList<>();

        for (StatusEffectInstance inst : activeEffects) {
            if (inst.durationType() == DurationType.TURN_BASED) {
                int remaining = inst.remainingTurns() - 1;
                if (remaining <= 0) {
                    expired.add(inst);
                } else {
                    surviving.add(new StatusEffectInstance(inst.effect(), inst.source(), inst.layers(), inst.durationType(), remaining));
                }
            } else {
                surviving.add(inst);
            }
        }

        activeEffects.clear();
        activeEffects.addAll(surviving);

        for (StatusEffectInstance inst : expired) {
            CosmiconLogger.info("[STATUS] %s from %s expired (duration ended)", inst.effect().name(), inst.source());
        }
    }

    public int calculateAttackBonus(TurnType turnType) {
        int bonus = 0;

        if (turnType == TurnType.ATTACK) {
            for (StatusEffectInstance inst : activeEffects) {
                if (inst.effect() == StatusEffect.STRENGTH || inst.effect() == StatusEffect.OVERLOAD) {
                    bonus += inst.layers();
                }
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
                this.diceIsPrismatic.add(type == DiceType.PRISMATIC || type == DiceType.YELLOW_D12);
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

        public List<Boolean> getDiceSelected() {
            return diceSelected;
        }

        public List<Boolean> getDiceIsPrismatic() {
            return diceIsPrismatic;
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
                CosmiconLogger.info("[STATUS] HACK: transformed highest dice %d to 2", maxValue);
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
                CosmiconLogger.info("[STATUS] ARISE: transformed lowest dice %d to %d", minValue, minMaxFace);
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
        CosmiconLogger.info("[LEVEL_UP] count=%d | dice=%d | selected=%s | prismatic=%s | types=%s | maxFaces=%s",
            count,
            diceValues.size(),
            diceSelected,
            diceIsPrismatic,
            diceTypes,
            diceMaxFaces);

        int processedCount = 0;
        int skippedUnselected = 0;
        int skippedPrismatic = 0;

        for (int i = 0; i < diceValues.size(); i++) {
            boolean isSelected = diceSelected.get(i);
            boolean isPrismatic = diceIsPrismatic.get(i);

            if (!isSelected) {
                skippedUnselected++;
                CosmiconLogger.info("[LEVEL_UP] die[%d] SKIPPED: not selected | type=%s value=%d prismatic=%s",
                    i, i < diceTypes.size() ? diceTypes.get(i) : "?", diceValues.get(i), isPrismatic);
                continue;
            }

            if (isPrismatic) {
                skippedPrismatic++;
                CosmiconLogger.info("[LEVEL_UP] die[%d] SKIPPED: prismatic | type=%s value=%d selected=true",
                    i, i < diceTypes.size() ? diceTypes.get(i) : "?", diceValues.get(i));
                continue;
            }

            int currentMaxFace = (i < diceMaxFaces.size()) ? diceMaxFaces.get(i) : getDiceMaxFace(i);
            int oldMaxFace = currentMaxFace;
            DiceType oldType = i < diceTypes.size() ? diceTypes.get(i) : null;

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

            DiceType newType = DiceType.fromMaxFace(currentMaxFace);

            if (currentMaxFace != oldMaxFace) {
                CosmiconLogger.info("[LEVEL_UP] die[%d] UPGRADED: %s(d%d) -> %s(d%d) | diceValue=%d",
                    i, oldType, oldMaxFace,
                    newType, currentMaxFace,
                    diceValues.get(i));
                processedCount++;
            } else {
                CosmiconLogger.info("[LEVEL_UP] die[%d] AT MAX: %s(d%d) | diceValue=%d",
                    i, oldType, oldMaxFace,
                    diceValues.get(i));
            }
        }

        CosmiconLogger.info("[LEVEL_UP] SUMMARY: processed=%d skipped_unselected=%d skipped_prismatic=%d total=%d",
            processedCount, skippedUnselected, skippedPrismatic, diceValues.size());
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
