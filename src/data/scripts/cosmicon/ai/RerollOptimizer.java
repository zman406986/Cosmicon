package data.scripts.cosmicon.ai;

import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.CosmiconLogger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RerollOptimizer {

    private RerollOptimizer() {}

    public static Set<Integer> optimalRerolls(
            List<Integer> currentValues,
            List<DiceType> diceTypes,
            int requiredSelectCount,
            int rerollsAvailable,
            int targetSum,
            boolean isAttacking) {
        
        CosmiconLogger.debug("[AI_REROLL_DIAG] RerollOptimizer.optimalRerolls: currentValues=%s, diceTypes=%s", 
            currentValues, diceTypes);
        CosmiconLogger.debug("[AI_REROLL_DIAG] requiredSelectCount=%d, rerollsAvailable=%d, targetSum=%d, isAttacking=%s", 
            requiredSelectCount, rerollsAvailable, targetSum, isAttacking);
        
        if (currentValues == null || currentValues.isEmpty() || rerollsAvailable <= 0) {
            CosmiconLogger.debug("[AI_REROLL_DIAG] optimalRerolls returning empty: currentValues=%s, rerollsAvailable=%d", 
                currentValues, rerollsAvailable);
            return Set.of();
        }

        if (targetSum <= 0) {
            int expectedSum = (int) DiceProbabilityCalculator.expectedSum(diceTypes, requiredSelectCount);
            targetSum = isAttacking ? expectedSum + 3 : expectedSum;
            CosmiconLogger.debug("[AI_REROLL_DIAG] targetSum was <=0, calculated expectedSum=%d, new targetSum=%d", 
                expectedSum, targetSum);
        }

        Set<Integer> alreadySelected = findCurrentBestSelection(currentValues, requiredSelectCount, isAttacking);
        int currentSum = calculateSum(currentValues, alreadySelected);
        
        CosmiconLogger.debug("[AI_REROLL_DIAG] bestSelection indices=%s, currentSum=%d, targetSum=%d", 
            alreadySelected, currentSum, targetSum);

        if (currentSum >= targetSum) {
            CosmiconLogger.debug("[AI_REROLL_DIAG] currentSum %d >= targetSum %d, NO REROLL NEEDED", currentSum, targetSum);
            return Set.of();
        }

        List<RerollCandidate> candidates;
        int maxReroll = Math.min(rerollsAvailable, currentValues.size());

        if (maxReroll <= 3 && currentValues.size() <= 7) {
            candidates = enumerateAllRerollSubsets(currentValues, diceTypes, alreadySelected, 
                                                    requiredSelectCount, targetSum, maxReroll);
        } else {
            candidates = greedyRerollCandidates(currentValues, diceTypes, alreadySelected, 
                                                requiredSelectCount, targetSum, maxReroll);
        }

        if (candidates.isEmpty()) {
            CosmiconLogger.debug("[AI_REROLL_DIAG] NO VALID CANDIDATES found, returning empty");
            return Set.of();
        }

        candidates.sort(Comparator.comparingDouble(RerollCandidate::expectedImprovement).reversed());
        
        CosmiconLogger.debug("[AI_REROLL_DIAG] Found %d candidates, best improvement=%.1f", 
            candidates.size(), candidates.get(0).expectedImprovement());

        RerollCandidate best = candidates.get(0);
        if (best.expectedImprovement() > 0) {
            CosmiconLogger.debug("[AI_REROLL_DIAG] WILL REROLL: indices %s, expected improvement %.1f (current: %d, target: %d)", 
                best.rerollIndices(), best.expectedImprovement(), currentSum, targetSum);
            return best.rerollIndices();
        }

        CosmiconLogger.debug("[AI_REROLL_DIAG] NO POSITIVE IMPROVEMENT: best=%.1f, keeping current dice", best.expectedImprovement());
        return Set.of();
    }

    private static Set<Integer> findCurrentBestSelection(List<Integer> values, int required, boolean preferHigh) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) indices.add(i);

        Comparator<Integer> comparator = preferHigh 
            ? Comparator.comparingInt(values::get).reversed()
            : Comparator.comparingInt(values::get);
        indices.sort(comparator);

        Set<Integer> selected = new HashSet<>();
        for (int i = 0; i < Math.min(required, indices.size()); i++) {
            selected.add(indices.get(i));
        }
        return selected;
    }

    private static int calculateSum(List<Integer> values, Set<Integer> indices) {
        int sum = 0;
        for (int idx : indices) {
            sum += values.get(idx);
        }
        return sum;
    }

    private static List<RerollCandidate> enumerateAllRerollSubsets(
            List<Integer> currentValues,
            List<DiceType> diceTypes,
            Set<Integer> currentSelection,
            int requiredCount,
            int targetSum,
            int maxReroll) {
        
        List<RerollCandidate> candidates = new ArrayList<>();
        int n = currentValues.size();

        for (int k = 1; k <= maxReroll; k++) {
            enumerateRerollCombinations(candidates, new HashSet<>(), 0, n, k,
                                        currentValues, diceTypes, currentSelection, requiredCount, targetSum);
        }

        return candidates;
    }

    private static void enumerateRerollCombinations(
            List<RerollCandidate> result,
            Set<Integer> currentRerolls,
            int start,
            int n,
            int k,
            List<Integer> values,
            List<DiceType> types,
            Set<Integer> originalSelection,
            int required,
            int target) {
        
        if (currentRerolls.size() == k) {
            float improvement = calculateExpectedImprovement(values, types, currentRerolls, 
                                                             originalSelection, required, target);
            result.add(new RerollCandidate(new HashSet<>(currentRerolls), improvement));
            return;
        }
        if (start >= n) return;

        for (int i = start; i < n; i++) {
            currentRerolls.add(i);
            enumerateRerollCombinations(result, currentRerolls, i + 1, n, k,
                                        values, types, originalSelection, required, target);
            currentRerolls.remove(i);
        }
    }

    private static List<RerollCandidate> greedyRerollCandidates(
            List<Integer> currentValues,
            List<DiceType> diceTypes,
            Set<Integer> currentSelection,
            int requiredCount,
            int targetSum,
            int maxReroll) {
        
        List<RerollCandidate> candidates = new ArrayList<>();

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < currentValues.size(); i++) indices.add(i);

        for (int i = 0; i < Math.min(maxReroll, indices.size()); i++) {
            Set<Integer> rerollSet = new HashSet<>();
            
            for (int j = 0; j <= i; j++) {
                int idx = indices.get(j);
                if (!currentSelection.contains(idx)) {
                    rerollSet.add(idx);
                }
            }

            if (!rerollSet.isEmpty()) {
                float improvement = calculateExpectedImprovement(currentValues, diceTypes, rerollSet,
                                                                 currentSelection, requiredCount, targetSum);
                candidates.add(new RerollCandidate(rerollSet, improvement));
            }
        }

        return candidates;
    }

    private static float calculateExpectedImprovement(
            List<Integer> currentValues,
            List<DiceType> diceTypes,
            Set<Integer> rerollIndices,
            Set<Integer> currentSelection,
            int requiredCount,
            int targetSum) {
        
        int currentSum = calculateSum(currentValues, currentSelection);
        
        Set<Integer> newSelection = findOptimalSelectionAfterReroll(
            currentValues, diceTypes, rerollIndices, currentSelection, requiredCount);
        
        float expectedNewSum = calculateExpectedSumAfterReroll(currentValues, diceTypes, rerollIndices, newSelection);
        
        float improvement = expectedNewSum - currentSum;
        
        if (expectedNewSum >= targetSum && currentSum < targetSum) {
            improvement += (targetSum - currentSum) * 0.5f;
        }

        return improvement;
    }

    private static Set<Integer> findOptimalSelectionAfterReroll(
            List<Integer> currentValues,
            List<DiceType> diceTypes,
            Set<Integer> rerollIndices,
            Set<Integer> currentSelection,
            int required) {
        
        List<RerolledDieValue> candidates = new ArrayList<>();
        
        for (int idx : currentSelection) {
            if (!rerollIndices.contains(idx)) {
                candidates.add(new RerolledDieValue(idx, currentValues.get(idx), false));
            }
        }
        
        for (int idx : rerollIndices) {
            int expectedVal = (int) DiceProbabilityCalculator.expectedValue(diceTypes.get(idx));
            candidates.add(new RerolledDieValue(idx, expectedVal, true));
        }

        candidates.sort(Comparator.comparingInt(RerolledDieValue::value).reversed());

        Set<Integer> newSelection = new HashSet<>();
        for (int i = 0; i < Math.min(required, candidates.size()); i++) {
            newSelection.add(candidates.get(i).index());
        }

        return newSelection;
    }

    private static float calculateExpectedSumAfterReroll(
            List<Integer> currentValues,
            List<DiceType> diceTypes,
            Set<Integer> rerollIndices,
            Set<Integer> newSelection) {
        
        float sum = 0;
        for (int idx : newSelection) {
            if (rerollIndices.contains(idx)) {
                sum += DiceProbabilityCalculator.expectedValue(diceTypes.get(idx));
            } else {
                sum += currentValues.get(idx);
            }
        }
        return sum;
    }

    private record RerollCandidate(Set<Integer> rerollIndices, float expectedImprovement) {}

    private record RerolledDieValue(int index, int value, boolean isRerolled) {}
}