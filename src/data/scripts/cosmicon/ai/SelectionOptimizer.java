package data.scripts.cosmicon.ai;

import data.scripts.cosmicon.battle.DiceType;
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
        
        if (diceValues == null || diceValues.isEmpty() || requiredCount <= 0) {
            return SelectionResult.empty();
        }

        if (profile != null && profile.shouldOptimizeForPassive(isAttacking)) {
            SelectionResult passiveResult = optimizeForPassive(diceValues, diceTypes, requiredCount, profile, isAttacking);
            if (passiveResult != null && passiveResult.passiveTriggered) {
                return passiveResult;
            }
        }

        SelectionResult greedyResult = greedyHighSelection(diceValues, diceTypes, requiredCount, isAttacking, profile);
        
        if (profile != null && !profile.shouldOptimizeForPassive(isAttacking)) {
            SelectionResult enhancedResult = considerPassiveBonus(diceValues, diceTypes, requiredCount, profile, isAttacking);
            if (enhancedResult.totalScore > greedyResult.totalScore) {
                return enhancedResult;
            }
        }

        return greedyResult;
    }

    private static SelectionResult greedyHighSelection(
            List<Integer> diceValues,
            List<DiceType> diceTypes,
            int requiredCount,
            boolean isAttacking,
            CharacterAIProfile profile) {
        
        boolean preferHigh = profile == null || profile.prefersHighValues(isAttacking);

        List<DiceIndexValue> indexedValues = new ArrayList<>();
        for (int i = 0; i < diceValues.size(); i++) {
            indexedValues.add(new DiceIndexValue(i, diceValues.get(i), diceTypes.get(i)));
        }

        Comparator<DiceIndexValue> comparator = preferHigh 
            ? Comparator.comparingInt(DiceIndexValue::value).reversed()
            : Comparator.comparingInt(DiceIndexValue::value);
        
        indexedValues.sort(comparator);

        Set<Integer> selectedIndices = new HashSet<>();
        int sum = 0;
        List<Integer> selectedValues = new ArrayList<>();
        List<DiceType> selectedTypes = new ArrayList<>();

        for (int i = 0; i < Math.min(requiredCount, indexedValues.size()); i++) {
            DiceIndexValue dv = indexedValues.get(i);
            selectedIndices.add(dv.index());
            sum += dv.value();
            selectedValues.add(dv.value());
            selectedTypes.add(dv.type());
        }

        return new SelectionResult(selectedIndices, sum, selectedValues, selectedTypes, false, 0f, sum);
    }

    private static SelectionResult optimizeForPassive(
            List<Integer> diceValues,
            List<DiceType> diceTypes,
            int requiredCount,
            CharacterAIProfile profile,
            boolean isAttacking) {
        
        List<Set<Integer>> candidateSelections = generateCandidateSelections(diceValues, requiredCount);

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

            CharacterAIProfile.PassiveEvaluation eval = profile.evaluatePassiveTrigger(selectedValues, selectedTypes, isAttacking);

            float score = sum + eval.bonusValue();
            if (eval.triggered()) {
                score += 100f;
            }

            if (score > bestScore) {
                bestScore = score;
                bestResult = new SelectionResult(selection, sum, selectedValues, selectedTypes,
                        eval.triggered(), eval.bonusValue(), score);
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

        indexedValues.sort(Comparator.comparingInt(DiceIndexValue::value).reversed());

        Set<Integer> selectedIndices = new HashSet<>();
        List<Integer> selectedValues = new ArrayList<>();
        List<DiceType> selectedTypes = new ArrayList<>();
        int sum = 0;

        for (int i = 0; i < Math.min(requiredCount, indexedValues.size()); i++) {
            DiceIndexValue dv = indexedValues.get(i);
            selectedIndices.add(dv.index());
            selectedValues.add(dv.value());
            selectedTypes.add(dv.type());
            sum += dv.value();
        }

        CharacterAIProfile.PassiveEvaluation eval = profile.evaluatePassiveTrigger(selectedValues, selectedTypes, isAttacking);
        float totalScore = sum + eval.bonusValue();

        return new SelectionResult(selectedIndices, sum, selectedValues, selectedTypes, eval.triggered(), eval.bonusValue(), totalScore);
    }

    private static List<Set<Integer>> generateCandidateSelections(List<Integer> diceValues, int requiredCount) {
        List<Set<Integer>> selections = new ArrayList<>();
        
        int n = diceValues.size();
        if (n <= 8 && requiredCount <= 4) {
            generateCombinations(selections, new HashSet<>(), 0, n, requiredCount);
        } else {
            Set<Integer> topK = new HashSet<>();
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < n; i++) indices.add(i);
            indices.sort(Comparator.comparingInt(diceValues::get).reversed());
            for (int i = 0; i < requiredCount; i++) {
                topK.add(indices.get(i));
            }
            selections.add(topK);
            
            for (int swapOut = 0; swapOut < requiredCount; swapOut++) {
                for (int swapIn = requiredCount; swapIn < n; swapIn++) {
                    Set<Integer> variant = new HashSet<>(topK);
                    Integer outIdx = indices.get(swapOut);
                    Integer inIdx = indices.get(swapIn);
                    variant.remove(outIdx);
                    variant.add(inIdx);
                    selections.add(variant);
                }
            }
        }

        return selections;
    }

    private static void generateCombinations(List<Set<Integer>> result, Set<Integer> current, 
                                             int start, int n, int k) {
        if (current.size() == k) {
            result.add(new HashSet<>(current));
            return;
        }
        if (start >= n) return;

        for (int i = start; i < n; i++) {
            current.add(i);
            generateCombinations(result, current, i + 1, n, k);
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
        remaining.sort(Comparator.comparingInt(DiceIndexValue::value).reversed());

        for (DiceIndexValue dv : remaining) {
            if (selectedIndices.size() >= requiredCount) break;
            selectedIndices.add(dv.index());
            selectedValues.add(dv.value());
            selectedTypes.add(dv.type());
        }

        int sum = 0;
        for (int v : selectedValues) sum += v;
        float bonus = pairsFound * 3;

        return new SelectionResult(selectedIndices, sum, selectedValues, selectedTypes, 
                                    pairsFound > 0, bonus, sum + bonus);
    }

    private record DiceIndexValue(int index, int value, DiceType type) {}

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

        public int getPairCount() {
            return DiceProbabilityCalculator.countPairs(selectedValues);
        }
    }
}