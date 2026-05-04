package data.scripts.cosmicon.ai;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.util.CosmiconLogger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RerollOptimizer {

    private RerollOptimizer() {}

    public static Set<Integer> optimalRerolls(
            List<Integer> currentValues,
            List<DiceType> diceTypes,
            int requiredSelectCount,
            int rerollsAvailable,
            int targetSum,
            boolean isAttacking,
            BattleState state,
            boolean forPlayer) {

        Map<Integer, PrismaticDiceInstance> prismaticMap = state != null
            ? state.getPrismaticDiceMap(forPlayer) : null;

        return optimalRerollsInternal(currentValues, diceTypes, requiredSelectCount,
            rerollsAvailable, targetSum, isAttacking, prismaticMap, null, 0f);
    }

    public static Set<Integer> optimalRerolls(
            List<Integer> currentValues,
            List<DiceType> diceTypes,
            int requiredSelectCount,
            int rerollsAvailable,
            int targetSum,
            boolean isAttacking,
            BattleState state,
            boolean forPlayer,
            CharacterAIProfile profile) {

        Map<Integer, PrismaticDiceInstance> prismaticMap = state != null
            ? state.getPrismaticDiceMap(forPlayer) : null;

        Set<Integer> profileSelection = computeProfileSelection(
            currentValues, diceTypes, requiredSelectCount, isAttacking, profile, state, forPlayer);
        float thornsPenalty = profile != null ? profile.getThornsPenaltyPerReroll() : 0f;

        return optimalRerollsInternal(currentValues, diceTypes, requiredSelectCount,
            rerollsAvailable, targetSum, isAttacking, prismaticMap, profileSelection, thornsPenalty);
    }

    public static Set<Integer> optimalRerolls(
            List<Integer> currentValues,
            List<DiceType> diceTypes,
            int requiredSelectCount,
            int rerollsAvailable,
            int targetSum,
            boolean isAttacking) {

        return optimalRerollsInternal(currentValues, diceTypes, requiredSelectCount,
            rerollsAvailable, targetSum, isAttacking, null, null, 0f);
    }

    public static Set<Integer> optimalRerolls(
            List<Integer> currentValues,
            List<DiceType> diceTypes,
            int requiredSelectCount,
            int rerollsAvailable,
            int targetSum,
            boolean isAttacking,
            CharacterAIProfile profile) {

        Set<Integer> profileSelection = computeProfileSelection(
            currentValues, diceTypes, requiredSelectCount, isAttacking, profile, null, false);
        float thornsPenalty = profile != null ? profile.getThornsPenaltyPerReroll() : 0f;

        return optimalRerollsInternal(currentValues, diceTypes, requiredSelectCount,
            rerollsAvailable, targetSum, isAttacking, null, profileSelection, thornsPenalty);
    }

    private static Set<Integer> computeProfileSelection(
            List<Integer> currentValues, List<DiceType> diceTypes,
            int requiredSelectCount, boolean isAttacking,
            CharacterAIProfile profile, BattleState state, boolean forPlayer) {
        if (profile == null) return null;
        SelectionOptimizer.SelectionResult result;
        if (state != null) {
            result = SelectionOptimizer.optimalSelection(
                currentValues, diceTypes, requiredSelectCount, isAttacking, profile, state, forPlayer);
        } else {
            result = SelectionOptimizer.optimalSelection(
                currentValues, diceTypes, requiredSelectCount, isAttacking, profile);
        }
        return result != null ? result.selectedIndices : null;
    }

    private static Set<Integer> optimalRerollsInternal(
            List<Integer> currentValues,
            List<DiceType> diceTypes,
            int requiredSelectCount,
            int rerollsAvailable,
            int targetSum,
            boolean isAttacking,
            Map<Integer, PrismaticDiceInstance> prismaticMap,
            Set<Integer> profileSelection,
            float thornsPenalty) {

        CosmiconLogger.debug("[AI_REROLL_DIAG] RerollOptimizer.optimalRerolls: currentValues=%s, diceTypes=%s",
            currentValues, diceTypes);
        CosmiconLogger.debug("[AI_REROLL_DIAG] requiredSelectCount=%d, rerollsAvailable=%d, targetSum=%d, isAttacking=%s",
            requiredSelectCount, rerollsAvailable, targetSum, isAttacking);

        if (currentValues == null || currentValues.isEmpty() || rerollsAvailable <= 0) {
            CosmiconLogger.debug("[AI_REROLL_DIAG] optimalRerolls returning empty: currentValues=%s, rerollsAvailable=%d",
                currentValues, rerollsAvailable);
            return Set.of();
        }

        int effectiveRequired = Math.min(requiredSelectCount, currentValues.size());

        if (targetSum <= 0) {
            int expectedSum = (int) DiceProbabilityCalculator.expectedSum(diceTypes, effectiveRequired);
            targetSum = isAttacking ? expectedSum + 3 : expectedSum;
            CosmiconLogger.debug("[AI_REROLL_DIAG] targetSum was <=0, calculated expectedSum=%d, new targetSum=%d",
                expectedSum, targetSum);
        }

        Set<Integer> alreadySelected = profileSelection != null
            ? profileSelection
            : findCurrentBestSelection(currentValues, requiredSelectCount, isAttacking, prismaticMap);
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
                                                    requiredSelectCount, targetSum, maxReroll, prismaticMap,
                                                    thornsPenalty);
        } else {
            candidates = greedyRerollCandidates(currentValues, diceTypes, alreadySelected,
                                                requiredSelectCount, targetSum, maxReroll, prismaticMap,
                                                thornsPenalty);
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

    private static Set<Integer> findCurrentBestSelection(List<Integer> values, int required,
            boolean preferHigh, Map<Integer, PrismaticDiceInstance> prismaticMap) {
        Set<Integer> forcedIndices = new HashSet<>();
        if (prismaticMap != null) {
            for (Map.Entry<Integer, PrismaticDiceInstance> entry : prismaticMap.entrySet()) {
                PrismaticDiceInstance pd = entry.getValue();
                if (pd.isMustSelect() || isDestinedEffect(pd)) {
                    forcedIndices.add(entry.getKey());
                }
            }
        }

        int remaining = required - forcedIndices.size();
        if (remaining <= 0) {
            return forcedIndices;
        }

        List<Integer> freeIndices = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            if (!forcedIndices.contains(i)) {
                freeIndices.add(i);
            }
        }

        Comparator<Integer> comparator = preferHigh
            ? Comparator.comparingInt(values::get).reversed()
            : Comparator.comparingInt(values::get);
        freeIndices.sort(comparator);

        Set<Integer> selected = new HashSet<>(forcedIndices);
        for (int i = 0; i < Math.min(remaining, freeIndices.size()); i++) {
            selected.add(freeIndices.get(i));
        }
        return selected;
    }

    private static boolean isDestinedEffect(PrismaticDiceInstance pd) {
        if (pd == null || pd.type == null) return false;
        var effect = pd.type.getEffect();
        if (effect == null) return false;
        return effect.isGrantStatus()
            && effect.getGrantedEffect() == data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect.DESTINED;
    }

    private static int calculateSum(List<Integer> values, Set<Integer> indices) {
        int sum = 0;
        for (int idx : indices) {
            sum += values.get(idx);
        }
        return sum;
    }

    private static boolean shouldSkipPrismaticReroll(int idx, DiceType type,
            Map<Integer, PrismaticDiceInstance> prismaticMap) {
        if (type != DiceType.PRISMATIC || prismaticMap == null) {
            return false;
        }
        PrismaticDiceInstance pd = prismaticMap.get(idx);
        if (pd == null) {
            return false;
        }
        return pd.rolledFace >= 6 || pd.isSpecialFace;
    }

    private static List<RerollCandidate> enumerateAllRerollSubsets(
            List<Integer> currentValues,
            List<DiceType> diceTypes,
            Set<Integer> currentSelection,
            int requiredCount,
            int targetSum,
            int maxReroll,
            Map<Integer, PrismaticDiceInstance> prismaticMap,
            float thornsPenalty) {

        List<RerollCandidate> candidates = new ArrayList<>();
        int n = currentValues.size();

        for (int k = 1; k <= maxReroll; k++) {
            enumerateRerollCombinations(candidates, new HashSet<>(), 0, n, k,
                                        currentValues, diceTypes, currentSelection, requiredCount, targetSum,
                                        prismaticMap, thornsPenalty);
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
            int target,
            Map<Integer, PrismaticDiceInstance> prismaticMap,
            float thornsPenalty) {

        if (currentRerolls.size() == k) {
            float improvement = calculateExpectedImprovement(values, types, currentRerolls,
                                                             originalSelection, required, target,
                                                             prismaticMap, thornsPenalty);
            result.add(new RerollCandidate(new HashSet<>(currentRerolls), improvement));
            return;
        }
        if (start >= n) return;

        for (int i = start; i < n; i++) {
            if (shouldSkipPrismaticReroll(i, types.get(i), prismaticMap)) {
                continue;
            }
            currentRerolls.add(i);
            enumerateRerollCombinations(result, currentRerolls, i + 1, n, k,
                                        values, types, originalSelection, required, target,
                                        prismaticMap, thornsPenalty);
            currentRerolls.remove(i);
        }
    }

    private static List<RerollCandidate> greedyRerollCandidates(
            List<Integer> currentValues,
            List<DiceType> diceTypes,
            Set<Integer> currentSelection,
            int requiredCount,
            int targetSum,
            int maxReroll,
            Map<Integer, PrismaticDiceInstance> prismaticMap,
            float thornsPenalty) {

        List<RerollCandidate> candidates = new ArrayList<>();

        List<Integer> sortedIndices = new ArrayList<>();
        for (int i = 0; i < currentValues.size(); i++) {
            if (!shouldSkipPrismaticReroll(i, diceTypes.get(i), prismaticMap)) {
                sortedIndices.add(i);
            }
        }
        sortedIndices.sort(Comparator.comparingInt(currentValues::get));

        Set<Integer> cumulativeRerollSet = new HashSet<>();
        int addedCount = 0;
        for (int idx : sortedIndices) {
            if (addedCount >= maxReroll) break;

            if (!currentSelection.contains(idx)) {
                cumulativeRerollSet.add(idx);
                addedCount++;
            }

            if (!cumulativeRerollSet.isEmpty()) {
                float improvement = calculateExpectedImprovement(currentValues, diceTypes, cumulativeRerollSet,
                                                                 currentSelection, requiredCount, targetSum,
                                                                 prismaticMap, thornsPenalty);
                candidates.add(new RerollCandidate(new HashSet<>(cumulativeRerollSet), improvement));
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
            int targetSum,
            Map<Integer, PrismaticDiceInstance> prismaticMap,
            float thornsPenalty) {

        int currentSum = calculateSum(currentValues, currentSelection);

        Set<Integer> newSelection = findOptimalSelectionAfterReroll(
            currentValues, diceTypes, rerollIndices, currentSelection, requiredCount, prismaticMap);

        float expectedNewSum = calculateExpectedSumAfterReroll(currentValues, diceTypes, rerollIndices, newSelection,
                                                               prismaticMap);

        float improvement = expectedNewSum - currentSum - thornsPenalty;

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
            int required,
            Map<Integer, PrismaticDiceInstance> prismaticMap) {

        Set<Integer> forcedIndices = new HashSet<>();
        if (prismaticMap != null) {
            for (Map.Entry<Integer, PrismaticDiceInstance> entry : prismaticMap.entrySet()) {
                PrismaticDiceInstance pd = entry.getValue();
                if (pd.isMustSelect() || isDestinedEffect(pd)) {
                    forcedIndices.add(entry.getKey());
                }
            }
        }

        int remaining = required - forcedIndices.size();
        if (remaining <= 0) {
            return forcedIndices;
        }

        List<RerolledDieValue> candidates = new ArrayList<>();
        for (int i = 0; i < currentValues.size(); i++) {
            if (forcedIndices.contains(i)) {
                continue;
            }
            if (rerollIndices.contains(i)) {
                DiceType type = diceTypes.get(i);
                PrismaticDiceInstance instance = prismaticMap != null ? prismaticMap.get(i) : null;
                float expectedVal = DiceProbabilityCalculator.expectedValue(type, instance);
                candidates.add(new RerolledDieValue(i, expectedVal, true));
            } else {
                candidates.add(new RerolledDieValue(i, currentValues.get(i), false));
            }
        }

        candidates.sort(Comparator.comparingDouble(RerolledDieValue::value).reversed());

        Set<Integer> newSelection = new HashSet<>(forcedIndices);
        for (int i = 0; i < Math.min(remaining, candidates.size()); i++) {
            newSelection.add(candidates.get(i).index());
        }

        return newSelection;
    }

    private static float calculateExpectedSumAfterReroll(
            List<Integer> currentValues,
            List<DiceType> diceTypes,
            Set<Integer> rerollIndices,
            Set<Integer> newSelection,
            Map<Integer, PrismaticDiceInstance> prismaticMap) {

        float sum = 0;
        for (int idx : newSelection) {
            if (rerollIndices.contains(idx)) {
                DiceType type = diceTypes.get(idx);
                PrismaticDiceInstance instance = prismaticMap != null ? prismaticMap.get(idx) : null;
                sum += DiceProbabilityCalculator.expectedValue(type, instance);
            } else {
                sum += currentValues.get(idx);
            }
        }
        return sum;
    }

    private record RerollCandidate(Set<Integer> rerollIndices, float expectedImprovement) {}

    private record RerolledDieValue(int index, float value, boolean isRerolled) {}
}
