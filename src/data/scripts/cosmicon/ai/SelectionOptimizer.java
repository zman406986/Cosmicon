package data.scripts.cosmicon.ai;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.prismatic.PrismaticEffect;
import data.scripts.cosmicon.util.CosmiconLogger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SelectionOptimizer {

    private SelectionOptimizer() {}

    public static SelectionResult optimalSelection(
            List<Integer> diceValues,
            List<DiceType> diceTypes,
            int requiredCount,
            boolean isAttacking,
            CharacterAIProfile profile) {
        return optimalSelection(diceValues, diceTypes, requiredCount, isAttacking, profile, null, true);
    }

    public static SelectionResult optimalSelection(
            List<Integer> diceValues,
            List<DiceType> diceTypes,
            int requiredCount,
            boolean isAttacking,
            CharacterAIProfile profile,
            BattleState state,
            boolean forPlayer) {
        
        if (diceValues == null || diceValues.isEmpty() || requiredCount <= 0
                || diceTypes == null || diceTypes.size() != diceValues.size()) {
            return SelectionResult.empty();
        }

        int effectiveRequired = Math.min(requiredCount, diceValues.size());

        Set<Integer> forcedIndices = findForcedSelectionIndices(diceValues, state, forPlayer);
        int forcedCount = forcedIndices.size();
        int freeSlots = Math.max(0, effectiveRequired - forcedCount);

        if (profile != null && profile.shouldOptimizeForPassive(isAttacking)) {
            SelectionResult passiveResult = optimizeForPassive(diceValues, diceTypes, effectiveRequired, 
                profile, isAttacking, state, forPlayer, forcedIndices, freeSlots);
            if (passiveResult != null && passiveResult.passiveTriggered) {
                CosmiconLogger.debug("Selection: passive optimization triggered for profile, indices: %s, bonus: %.1f", 
                    passiveResult.selectedIndices, passiveResult.passiveBonus);
                return passiveResult;
            }
        }

        if (profile != null && profile.prefersPairs()) {
            SelectionResult pairResult = selectForPairs(diceValues, diceTypes, freeSlots);
            if (pairResult.passiveTriggered) {
                pairResult = mergeForcedIndices(pairResult, forcedIndices);
                CosmiconLogger.debug("Selection: pair optimization triggered, pairs found, indices: %s", 
                    pairResult.selectedIndices);
                return pairResult;
            }
        }

        SelectionResult greedyResult = greedyHighSelection(diceValues, diceTypes, freeSlots, 
            isAttacking, profile, state, forPlayer);
        greedyResult = mergeForcedIndices(greedyResult, forcedIndices);
        
        if (profile != null && !profile.shouldOptimizeForPassive(isAttacking)) {
            SelectionResult enhancedResult = considerPassiveBonus(diceValues, diceTypes, freeSlots, profile, isAttacking);
            enhancedResult = mergeForcedIndices(enhancedResult, forcedIndices);
            if (enhancedResult.totalScore > greedyResult.totalScore) {
                CosmiconLogger.debug("Selection: passive bonus enhanced score from %.1f to %.1f", 
                    greedyResult.totalScore, enhancedResult.totalScore);
                return enhancedResult;
            }
        }

        CosmiconLogger.debug("Selection: greedy selection chosen, indices: %s, sum: %d, score: %.1f", 
            greedyResult.selectedIndices, greedyResult.sumValue, greedyResult.totalScore);
        return greedyResult;
    }

    private static Set<Integer> findForcedSelectionIndices(
            List<Integer> diceValues, BattleState state, boolean forPlayer) {
        Set<Integer> forced = new HashSet<>();
        if (state == null) return forced;
        for (int i = 0; i < diceValues.size(); i++) {
            var pd = state.getPrismaticDiceAt(i, forPlayer);
            if (pd != null && pd.isMustSelect()) {
                forced.add(i);
            }
        }
        return forced;
    }

    private static SelectionResult mergeForcedIndices(SelectionResult result, Set<Integer> forcedIndices) {
        if (forcedIndices.isEmpty() || result == null) return result;
        Set<Integer> merged = new HashSet<>(result.selectedIndices);
        merged.addAll(forcedIndices);
        
        List<Integer> mergedValues = new ArrayList<>(result.selectedValues);
        List<DiceType> mergedTypes = new ArrayList<>(result.selectedTypes);
        int mergedSum = result.sumValue;
        float mergedScore = result.totalScore;
        
        for (int idx : forcedIndices) {
            if (!result.selectedIndices.contains(idx)) {
                mergedValues.add(null);
                mergedTypes.add(null);
            }
        }
        
        return new SelectionResult(merged, mergedSum, mergedValues, mergedTypes,
                result.passiveTriggered, result.passiveBonus, mergedScore);
    }

    private static SelectionResult greedyHighSelection(
            List<Integer> diceValues,
            List<DiceType> diceTypes,
            int requiredCount,
            boolean isAttacking,
            CharacterAIProfile profile,
            BattleState state,
            boolean forPlayer) {
        
        boolean preferHigh = profile == null || profile.prefersHighValues(isAttacking);

        List<DiceIndexValue> indexedValues = new ArrayList<>();
        for (int i = 0; i < diceValues.size(); i++) {
            float value = diceValues.get(i);
            if (diceTypes.get(i) == DiceType.PRISMATIC && state != null) {
                PrismaticDiceInstance pd = state.getPrismaticDiceAt(i, forPlayer);
                if (pd != null && pd.isSpecialFace) {
                    value += getEffectBonusForSelection(pd.type.getEffect(), isAttacking, state, forPlayer);
                }
            }
            if (profile != null && diceTypes.get(i) == DiceType.PRISMATIC) {
                value += profile.getPrismaticDiceBonus(diceTypes.get(i), diceValues.get(i), isAttacking);
            }
            indexedValues.add(new DiceIndexValue(i, value, diceTypes.get(i)));
        }

        Comparator<DiceIndexValue> comparator = preferHigh 
            ? Comparator.comparingDouble(DiceIndexValue::sortValue).reversed()
            : Comparator.comparingDouble(DiceIndexValue::sortValue);
        
        indexedValues.sort(comparator);

        Set<Integer> selectedIndices = new HashSet<>();
        int sum = 0;
        float effectiveScore = 0f;
        List<Integer> selectedValues = new ArrayList<>();
        List<DiceType> selectedTypes = new ArrayList<>();

        for (int i = 0; i < Math.min(requiredCount, indexedValues.size()); i++) {
            DiceIndexValue dv = indexedValues.get(i);
            selectedIndices.add(dv.index());
            sum += diceValues.get(dv.index());
            effectiveScore += dv.sortValue();
            selectedValues.add(diceValues.get(dv.index()));
            selectedTypes.add(dv.type());
        }

        return new SelectionResult(selectedIndices, sum, selectedValues, selectedTypes, false, 0f, effectiveScore);
    }

    private static float getEffectBonusForSelection(PrismaticEffect effect, boolean isAttacking, BattleState state, boolean forPlayer) {
        if (effect == null || effect.isNone()) return 0f;

        if (effect.isDoubleValue()) return 3f;

        if (effect.isGrantStatus()) {
            StatusEffect grantedEffect = effect.getGrantedEffect();
            if (grantedEffect == null) return 0f;

            return switch (grantedEffect) {
                case FORCEFIELD -> isAttacking ? 0f : 5f;
                case COMBO -> {
                    if (!isAttacking) yield 0f;
                    int attackVal = state != null ? state.getAttackValue() : 0;
                    yield (attackVal > 0) ? (float) attackVal : 6f;
                }
                case UNYIELDING -> {
                    if (state == null || isAttacking) yield 0f;
                    int hp = forPlayer ? state.getPlayerHp() : state.getOpponentHp();
                    yield (hp <= 3) ? 4f : 0f;
                }
                case DESTINED -> 3f;
                case THORNS -> 0f;
                case HACK -> 2f;
                default -> 1f;
            };
        }

        if (effect.isHealHp()) return 2f;
        if (effect.isInstantDamage()) return isAttacking ? 3f : 0f;
        if (effect.isGainPrismaticUse()) return 2f;

        return 1f;
    }

    private static SelectionResult optimizeForPassive(
            List<Integer> diceValues,
            List<DiceType> diceTypes,
            int requiredCount,
            CharacterAIProfile profile,
            boolean isAttacking,
            BattleState state,
            boolean forPlayer,
            Set<Integer> forcedIndices,
            int freeSlots) {
        
        List<Set<Integer>> candidateSelections = generateCandidateSelections(
            diceValues, diceTypes, state, forPlayer, isAttacking, requiredCount, forcedIndices, freeSlots);

        SelectionResult bestResult = null;
        float bestScore = Float.MIN_VALUE;

        for (Set<Integer> selection : candidateSelections) {
            List<Integer> selectedValues = new ArrayList<>();
            List<DiceType> selectedTypes = new ArrayList<>();
            int sum = 0;

            for (int idx : selection) {
                selectedValues.add(diceValues.get(idx));
                selectedTypes.add(diceTypes.get(idx));
                sum += diceValues.get(idx);
            }

            CharacterAIProfile.PassiveEvaluation eval = profile.evaluatePassiveTrigger(selectedValues, selectedTypes, isAttacking, state, forPlayer);

            float passiveScore = profile.getPassiveBonusValue(selectedValues, isAttacking, state, forPlayer);
            float score = sum + passiveScore;
            if (eval.triggered()) {
                score += 100f;
            }

            if (score > bestScore) {
                bestScore = score;
                bestResult = new SelectionResult(selection, sum, selectedValues, selectedTypes,
                        eval.triggered(), passiveScore, score);
            }
        }

        return bestResult;
    }

    private static SelectionResult considerPassiveBonus(
            List<Integer> diceValues,
            List<DiceType> diceTypes,
            int requiredCount,
            CharacterAIProfile profile,
            boolean isAttacking) {
        
        List<DiceIndexValue> indexedValues = new ArrayList<>();
        for (int i = 0; i < diceValues.size(); i++) {
            indexedValues.add(new DiceIndexValue(i, diceValues.get(i), diceTypes.get(i)));
        }

        indexedValues.sort(Comparator.comparingDouble(DiceIndexValue::sortValue).reversed());

        Set<Integer> selectedIndices = new HashSet<>();
        List<Integer> selectedValues = new ArrayList<>();
        List<DiceType> selectedTypes = new ArrayList<>();
        int sum = 0;

        for (int i = 0; i < Math.min(requiredCount, indexedValues.size()); i++) {
            DiceIndexValue dv = indexedValues.get(i);
            selectedIndices.add(dv.index());
            selectedValues.add(diceValues.get(dv.index()));
            selectedTypes.add(dv.type());
            sum += diceValues.get(dv.index());
        }

        CharacterAIProfile.PassiveEvaluation eval = profile.evaluatePassiveTrigger(selectedValues, selectedTypes, isAttacking);
        float passiveScore = profile.getPassiveBonusValue(selectedValues, isAttacking);
        float totalScore = sum + passiveScore;

        return new SelectionResult(selectedIndices, sum, selectedValues, selectedTypes, eval.triggered(), passiveScore, totalScore);
    }

    private static List<Set<Integer>> generateCandidateSelections(
            List<Integer> diceValues,
            List<DiceType> diceTypes,
            BattleState state,
            boolean forPlayer,
            boolean isAttacking,
            int requiredCount,
            Set<Integer> forcedIndices,
            int freeSlots) {
        
        List<Set<Integer>> selections = new ArrayList<>();
        
        int n = diceValues.size();
        if (n <= 8 && requiredCount <= 4) {
            generateCombinations(selections, new HashSet<>(forcedIndices), 0, n, requiredCount, forcedIndices);
        } else {
            List<WeightedIndex> indices = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (forcedIndices.contains(i)) continue;
                float weight = diceValues.get(i);
                if (diceTypes != null && i < diceTypes.size() && diceTypes.get(i) == DiceType.PRISMATIC && state != null) {
                    var pd = state.getPrismaticDiceAt(i, forPlayer);
                    if (pd != null && pd.isSpecialFace) {
                        weight += getEffectBonusForSelection(pd.type.getEffect(), isAttacking, state, forPlayer);
                    }
                }
                indices.add(new WeightedIndex(i, weight));
            }
            indices.sort(Comparator.comparingDouble(WeightedIndex::weight).reversed());

            Set<Integer> topK = new HashSet<>(forcedIndices);
            int pickCount = Math.min(freeSlots, indices.size());
            for (int i = 0; i < pickCount; i++) {
                topK.add(indices.get(i).index());
            }
            selections.add(topK);
            
            for (int swapOut = 0; swapOut < pickCount; swapOut++) {
                for (int swapIn = pickCount; swapIn < indices.size(); swapIn++) {
                    Set<Integer> variant = new HashSet<>(topK);
                    variant.remove(indices.get(swapOut).index());
                    variant.add(indices.get(swapIn).index());
                    selections.add(variant);
                }
            }
        }

        return selections;
    }

    private record WeightedIndex(int index, float weight) {}

    private static void generateCombinations(List<Set<Integer>> result, Set<Integer> current, 
                                             int start, int n, int k, Set<Integer> forcedIndices) {
        if (current.size() == k) {
            result.add(new HashSet<>(current));
            return;
        }
        if (start >= n) return;

        for (int i = start; i < n; i++) {
            if (forcedIndices.contains(i)) continue;
            current.add(i);
            generateCombinations(result, current, i + 1, n, k, forcedIndices);
            current.remove(i);
        }
    }

    public static SelectionResult selectForPairs(List<Integer> diceValues, List<DiceType> diceTypes, int requiredCount) {
        Map<Integer, List<Integer>> valueToIndices = new HashMap<>();
        for (int i = 0; i < diceValues.size(); i++) {
            valueToIndices.computeIfAbsent(diceValues.get(i), k -> new ArrayList<>()).add(i);
        }

        Set<Integer> selectedIndices = new HashSet<>();
        List<Integer> selectedValues = new ArrayList<>();
        List<DiceType> selectedTypes = new ArrayList<>();
        int pairsFound = 0;

        for (Map.Entry<Integer, List<Integer>> entry : valueToIndices.entrySet()) {
            List<Integer> indices = entry.getValue();
            if (indices.size() >= 2 && selectedIndices.size() + 2 <= requiredCount) {
                selectedIndices.add(indices.get(0));
                selectedIndices.add(indices.get(1));
                selectedValues.add(entry.getKey());
                selectedValues.add(entry.getKey());
                selectedTypes.add(diceTypes.get(indices.get(0)));
                selectedTypes.add(diceTypes.get(indices.get(1)));
                pairsFound++;
            }
        }

        List<DiceIndexValue> remaining = new ArrayList<>();
        for (int i = 0; i < diceValues.size(); i++) {
            if (!selectedIndices.contains(i)) {
                remaining.add(new DiceIndexValue(i, diceValues.get(i), diceTypes.get(i)));
            }
        }
        remaining.sort(Comparator.comparingDouble(DiceIndexValue::sortValue).reversed());

        for (DiceIndexValue dv : remaining) {
            if (selectedIndices.size() >= requiredCount) break;
            selectedIndices.add(dv.index());
            selectedValues.add(diceValues.get(dv.index()));
            selectedTypes.add(dv.type());
        }

        int sum = 0;
        for (int v : selectedValues) sum += v;
        float bonus = pairsFound * 3;

        return new SelectionResult(selectedIndices, sum, selectedValues, selectedTypes, 
                                    pairsFound > 0, bonus, sum + bonus);
    }

    private record DiceIndexValue(int index, float sortValue, DiceType type) {}

    public static final class SelectionResult {
        public final Set<Integer> selectedIndices;
        public final int sumValue;
        public final List<Integer> selectedValues;
        public final List<DiceType> selectedTypes;
        public final boolean passiveTriggered;
        public final float passiveBonus;
        public final float totalScore;

        private SelectionResult(Set<Integer> indices, int sum, List<Integer> values, List<DiceType> types,
                                boolean triggered, float bonus, float score) {
            this.selectedIndices = indices;
            this.sumValue = sum;
            this.selectedValues = values;
            this.selectedTypes = types;
            this.passiveTriggered = triggered;
            this.passiveBonus = bonus;
            this.totalScore = score;
        }

        public static SelectionResult empty() {
            return new SelectionResult(Set.of(), 0, List.of(), List.of(), false, 0f, 0f);
        }

        public List<Integer> getSelectedIndicesList() {
            return new ArrayList<>(selectedIndices);
        }
    }
}