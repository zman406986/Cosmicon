package data.scripts.cosmicon.ai;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.prismatic.PrismaticEffect;
import data.scripts.cosmicon.util.CosmiconLogger;
import data.scripts.cosmicon.util.DiceEvaluator;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AttackRerollAI implements CharacterAIProfile {

    protected int rolloutsPerEvaluation = 20;
    protected int maxExactOutcomeEnumeration = 256;
    private static final Map<String, DieStoppingPolicy> policyCache = new HashMap<>();
    private static final Random SIM_RAND = new Random(42);
    private static final int MAX_REROLLS = 7;

    // Pre-computed destined indices for current planReroll call (avoids repeated lookups)
    private Set<Integer> currentDestinedIndices = Set.of();
    private String currentDestinedKey = "[]"; // Cached toString to avoid repeated allocation
    private final Map<String, List<Set<Integer>>> subsetCache = new HashMap<>();

    static {
        for (int maxFace : new int[]{4, 6, 8, 12}) {
            DieStoppingPolicy policy = computePolicyStatic(maxFace);
            policyCache.put(DiceType.fromMaxFace(maxFace).name(), policy);
        }
        policyCache.put(DiceType.PRISMATIC.name(), computePolicyStatic(6));
    }

    public AttackRerollAI() {}

    // ==================== CharacterAIProfile defaults ====================

    @Override
    public boolean prefersHighValues(boolean isAttacking) {
        return true;
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        return isAttacking;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        return PassiveEvaluation.notTriggered();
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking, BattleState state, boolean forPlayer) {
        return evaluatePassiveTrigger(selectedValues, selectedTypes, isAttacking);
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        return 0f;
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking, BattleState state, boolean forPlayer) {
        return getPassiveBonusValue(selectedValues, isAttacking);
    }

    // ==================== Main entry point ====================

    public Set<Integer> planReroll(List<Integer> currentValues, List<DiceType> diceTypes,
                                    int requiredSelectCount, int rerollsAvailable,
                                    boolean isAttacking, BattleState state, boolean forPlayer) {
        if (currentValues == null || currentValues.isEmpty() || rerollsAvailable <= 0) {
            return Set.of();
        }

        int effectiveRequired = Math.min(requiredSelectCount, currentValues.size());
        SimPool pool = createSimPool(currentValues, diceTypes, state, forPlayer);

        // Pre-compute destined indices once per planReroll call (4.7 optimization)
        this.currentDestinedIndices = findDestinedIndices(pool.size(), state, forPlayer);
        this.currentDestinedKey = currentDestinedIndices.toString(); // Cache key string
        this.subsetCache.clear();

        double currentEV = evaluateFinalPool(pool, 0, effectiveRequired, isAttacking, state, forPlayer);

        List<Set<Integer>> candidates = generateCandidateActions(pool, rerollsAvailable, effectiveRequired, isAttacking, state, forPlayer);

        if (candidates.isEmpty()) {
            CosmiconLogger.debug("[AI_REROLL] %s: no candidates generated", getCharacterId());
            return Set.of();
        }

        double bestEV = currentEV;
        Set<Integer> bestAction = Set.of();

        for (Set<Integer> diceToReroll : candidates) {
            double ev = evaluateImmediateAction(pool, diceToReroll, rerollsAvailable, effectiveRequired, isAttacking, state, forPlayer);
            if (ev > bestEV) {
                bestEV = ev;
                bestAction = diceToReroll;
            }
        }

        if (!bestAction.isEmpty()) {
            CosmiconLogger.debug("[AI_REROLL] %s: rerolling indices %s, EV %.1f vs current %.1f",
                getCharacterId(), bestAction, bestEV, currentEV);
        } else {
            CosmiconLogger.debug("[AI_REROLL] %s: no beneficial reroll (current EV %.1f)", getCharacterId(), currentEV);
        }

        return bestAction;
    }

    // ==================== Immediate action evaluation ====================

    private double evaluateImmediateAction(SimPool pool, Set<Integer> diceToReroll,
                                            int rerollsLeft, int requiredCount,
                                            boolean isAttacking, BattleState state, boolean forPlayer) {
        List<Outcome> outcomes = generateOutcomes(pool, diceToReroll);
        double totalProb = 0;
        double weightedUtility = 0;

        for (Outcome outcome : outcomes) {
            SimPool newPool = pool.createCopy();
            newPool.applyOutcome(diceToReroll, outcome);
            double util = evaluateStateWithRerolls(newPool, rerollsLeft - 1, requiredCount, isAttacking, state, forPlayer);
            weightedUtility += outcome.probability * util;
            totalProb += outcome.probability;
        }

        return totalProb > 0 ? weightedUtility / totalProb : Double.NEGATIVE_INFINITY;
    }

    // ==================== Future value estimator ====================

    private double evaluateStateWithRerolls(SimPool pool, int rerollsLeft, int requiredCount,
                                             boolean isAttacking, BattleState state, boolean forPlayer) {
        if (rerollsLeft <= 0) {
            return evaluateFinalPool(pool, 0, requiredCount, isAttacking, state, forPlayer);
        }

        double totalUtility = 0;
        for (int i = 0; i < rolloutsPerEvaluation; i++) {
            RolloutResult result = simulateBasePolicy(pool, rerollsLeft, isAttacking, state, forPlayer);
            double finalUtil = evaluateFinalPool(result.pool, result.rerollsUsed, requiredCount, isAttacking, state, forPlayer);
            totalUtility += finalUtil;
        }
        return totalUtility / rolloutsPerEvaluation;
    }

    private double evaluateFinalPool(SimPool pool, int rerollsUsed, int requiredCount,
                                      boolean isAttacking, BattleState state, boolean forPlayer) {
        SubsetEvaluator evaluator = getSubsetEvaluator();
        List<Set<Integer>> choices = enumerateLegalSubsets(pool, requiredCount);

        if (choices.isEmpty()) {
            return 0;
        }

        double bestUtil = Double.NEGATIVE_INFINITY;
        for (Set<Integer> subset : choices) {
            List<Integer> selectedValues = new ArrayList<>();
            List<DiceType> selectedTypes = new ArrayList<>();
            for (int idx : subset) {
                selectedValues.add(pool.getValue(idx));
                selectedTypes.add(pool.getType(idx));
            }
            double util = evaluator.evaluateSelection(selectedValues, selectedTypes, isAttacking, state, forPlayer);
            util -= calculateRerollSelfDamage(rerollsUsed);
            if (util > bestUtil) bestUtil = util;
        }
        return bestUtil;
    }

    // ==================== Base policy simulation ====================

    private RolloutResult simulateBasePolicy(SimPool pool, int rerollsLeft,
                                              boolean isAttacking, BattleState state, boolean forPlayer) {
        int r = rerollsLeft;
        int rerollsUsed = 0;
        SimPool copy = pool.createCopy();

        while (r > 0) {
            Set<Integer> toReroll = new HashSet<>();
            for (int i = 0; i < copy.size(); i++) {
                if (isSpecialFaceNeverReroll(i, copy.getType(i), isAttacking, state, forPlayer)) {
                    continue;
                }
                DieStoppingPolicy policy = getPolicyForDie(copy.getType(i));
                boolean shouldReroll = isAttacking
                    ? policy.shouldReroll(copy.getValue(i), r)
                    : policy.shouldRerollConservative(copy.getValue(i), r);
                if (shouldReroll) {
                    toReroll.add(i);
                }
            }
            if (toReroll.isEmpty()) break;

            copy.simReroll(toReroll);
            rerollsUsed++;
            r--;
        }
        return new RolloutResult(copy, rerollsUsed);
    }

    // ==================== Candidate action generation ====================

    protected List<Set<Integer>> generateCandidateActions(SimPool pool, int rerollsLeft, int requiredCount,
                                                           boolean isAttacking, BattleState state, boolean forPlayer) {
        List<Set<Integer>> candidates = new ArrayList<>();

        // Pre-compute neverReroll flags to avoid duplicate checks (4.4 optimization)
        boolean[] neverReroll = new boolean[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            neverReroll[i] = isSpecialFaceNeverReroll(i, pool.getType(i), isAttacking, state, forPlayer);
        }

        Set<Integer> thresholdSet = new HashSet<>();
        for (int i = 0; i < pool.size(); i++) {
            if (!neverReroll[i]) {
                DieStoppingPolicy policy = getPolicyForDie(pool.getType(i));
                if (policy.shouldReroll(pool.getValue(i), rerollsLeft)) {
                    thresholdSet.add(i);
                }
            }
        }
        if (!thresholdSet.isEmpty()) {
            candidates.add(thresholdSet);
        }

        for (int i = 0; i < pool.size(); i++) {
            if (!neverReroll[i]) {
                candidates.add(new HashSet<>(Collections.singletonList(i)));
            }
        }

        candidates.addAll(generateComboCandidates(pool, rerollsLeft, requiredCount, isAttacking, state, forPlayer));

        return candidates.stream().distinct().collect(Collectors.toList());
    }

    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        return Collections.emptyList();
    }

    protected static Set<Integer> findComboForSpecificValue(SimPool pool, int requiredCount, int targetValue) {
        List<Integer> nonMatchingIndices = new ArrayList<>();
        int matchCount = 0;
        for (int i = 0; i < pool.size(); i++) {
            if (pool.getValue(i) == targetValue) matchCount++;
            else nonMatchingIndices.add(i);
        }
        if (matchCount >= requiredCount - 1 && matchCount < requiredCount && !nonMatchingIndices.isEmpty()) {
            return new HashSet<>(nonMatchingIndices);
        }
        return null;
    }

    protected static Set<Integer> findComboForMostCommonValue(SimPool pool, int requiredCount) {
        Map<Integer, Integer> freq = new HashMap<>();
        for (int i = 0; i < pool.size(); i++) {
            freq.merge(pool.getValue(i), 1, Integer::sum);
        }

        int bestValue = -1;
        int bestCount = 0;
        for (Map.Entry<Integer, Integer> entry : freq.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                bestValue = entry.getKey();
            }
        }

        if (bestCount >= requiredCount - 1 && bestCount < requiredCount) {
            Set<Integer> nonMatching = new HashSet<>();
            for (int i = 0; i < pool.size(); i++) {
                if (pool.getValue(i) != bestValue) {
                    nonMatching.add(i);
                }
            }
            if (!nonMatching.isEmpty()) {
                return nonMatching;
            }
        }
        return null;
    }

    // ==================== Die stopping policy ====================

    protected static class DieStoppingPolicy {
        private final int[] thresholds;
        public DieStoppingPolicy(int[] thresholds) {
            this.thresholds = thresholds;
        }

        public boolean shouldReroll(int currentFace, int rerollsLeft) {
            if (rerollsLeft <= 0) return false;
            if (rerollsLeft >= thresholds.length) rerollsLeft = thresholds.length - 1;
            return currentFace < thresholds[rerollsLeft];
        }

        public boolean shouldRerollConservative(int currentFace, int rerollsLeft) {
            if (rerollsLeft <= 0) return false;
            if (rerollsLeft >= thresholds.length) rerollsLeft = thresholds.length - 1;
            return currentFace < Math.max(1, thresholds[rerollsLeft] - 1);
        }

    }

    protected DieStoppingPolicy getPolicyForDie(DiceType type) {
        return policyCache.get(type.name());
    }

    private static DieStoppingPolicy computePolicyStatic(int maxFace) {
        double[][] expected = new double[MAX_REROLLS + 1][maxFace + 1];
        int[] thresholds = new int[MAX_REROLLS + 1];

        for (int face = 1; face <= maxFace; face++) {
            expected[0][face] = face;
        }
        thresholds[0] = maxFace + 1;

        for (int r = 1; r <= MAX_REROLLS; r++) {
            double freshExpectation = 0;
            for (int newFace = 1; newFace <= maxFace; newFace++) {
                freshExpectation += expected[r - 1][newFace];
            }
            freshExpectation /= maxFace;

            int ceilFresh = (int) Math.ceil(freshExpectation);
            thresholds[r] = Math.min(ceilFresh, maxFace);

            for (int face = 1; face <= maxFace; face++) {
                expected[r][face] = Math.max(face, freshExpectation);
            }
        }

        return new DieStoppingPolicy(thresholds);
    }

    // ==================== Outcome generation ====================

    private List<Outcome> generateOutcomes(SimPool pool, Set<Integer> diceToReroll) {
        List<int[]> diceFaces = new ArrayList<>();
        for (int idx : diceToReroll) {
            diceFaces.add(pool.getPossibleFaces(idx));
        }

        long product = 1;
        for (int[] faces : diceFaces) {
            product *= faces.length;
            if (product > maxExactOutcomeEnumeration) break;
        }

        if (product <= maxExactOutcomeEnumeration) {
            return enumerateOutcomes(diceFaces);
        } else {
            return sampleOutcomes(diceFaces, maxExactOutcomeEnumeration);
        }
    }

    private List<Outcome> enumerateOutcomes(List<int[]> diceFaces) {
        List<Outcome> result = new ArrayList<>();
        int totalCombos = 1;
        for (int[] faces : diceFaces) totalCombos *= faces.length;
        double probPerCombo = 1.0 / totalCombos;

        int[] currentFaces = new int[diceFaces.size()];
        enumerateRec(diceFaces, 0, currentFaces, probPerCombo, result);
        return result;
    }

    private void enumerateRec(List<int[]> diceFaces, int index, int[] currentFaces, double prob, List<Outcome> out) {
        if (index == diceFaces.size()) {
            out.add(new Outcome(currentFaces.clone(), prob));
            return;
        }
        for (int face : diceFaces.get(index)) {
            currentFaces[index] = face;
            enumerateRec(diceFaces, index + 1, currentFaces, prob / diceFaces.get(index).length, out);
        }
    }

    private List<Outcome> sampleOutcomes(List<int[]> diceFaces, int sampleCount) {
        List<Outcome> result = new ArrayList<>();
        for (int s = 0; s < sampleCount; s++) {
            int[] faces = new int[diceFaces.size()];
            for (int i = 0; i < diceFaces.size(); i++) {
                int[] possible = diceFaces.get(i);
                faces[i] = possible[SIM_RAND.nextInt(possible.length)];
            }
            result.add(new Outcome(faces, 1.0 / sampleCount));
        }
        return result;
    }

    // ==================== Subset enumeration ====================

    protected List<Set<Integer>> enumerateLegalSubsets(SimPool pool, int requiredCount) {
        // Use pre-computed destined indices and cached key (4.7 optimization)
        String cacheKey = pool.size() + "_" + requiredCount + "_" + currentDestinedKey;
        return subsetCache.computeIfAbsent(cacheKey, k -> {
            List<Set<Integer>> results = new ArrayList<>();
            findCombinations(pool.size(), 0, requiredCount, new HashSet<>(), results, currentDestinedIndices);
            return results;
        });
    }

    private Set<Integer> findDestinedIndices(int poolSize, BattleState state, boolean forPlayer) {
        Set<Integer> destined = new HashSet<>();
        if (state == null) return destined;
        Map<Integer, PrismaticDiceInstance> prismaticMap = state.getPrismaticDiceMap(forPlayer);
        for (Map.Entry<Integer, PrismaticDiceInstance> entry : prismaticMap.entrySet()) {
            if (entry.getKey() >= poolSize) continue;
            PrismaticDiceInstance pd = entry.getValue();
            if (pd == null) continue;
            if (pd.isMustSelect()) {
                destined.add(entry.getKey());
                continue;
            }
            if (pd.type.getEffect() != null
                && pd.type.getEffect().isGrantStatus()
                && pd.type.getEffect().getGrantedEffect() == StatusEffect.DESTINED) {
                destined.add(entry.getKey());
            }
        }
        return destined;
    }

    private void findCombinations(int n, int start, int remaining, Set<Integer> current,
                                   List<Set<Integer>> results, Set<Integer> destinedIndices) {
        if (remaining == 0) {
            if (current.containsAll(destinedIndices)) {
                results.add(new HashSet<>(current));
            }
            return;
        }
        if (start >= n) return;
        current.add(start);
        findCombinations(n, start + 1, remaining - 1, current, results, destinedIndices);
        current.remove(start);
        findCombinations(n, start + 1, remaining, current, results, destinedIndices);
    }

    // ==================== Override points ====================

    protected SubsetEvaluator getSubsetEvaluator() {
        return (selectedValues, selectedTypes, isAttacking, state, forPlayer) -> {
            int sum = DiceEvaluator.sumOfValues(selectedValues);
            float bonus = state != null
                ? getPassiveBonusValue(selectedValues, isAttacking, state, forPlayer)
                : getPassiveBonusValue(selectedValues, isAttacking);
            return sum + bonus;
        };
    }

    protected double calculateRerollSelfDamage(int rerollsUsed) {
        float penaltyPerReroll = getThornsPenaltyPerReroll();
        return rerollsUsed * penaltyPerReroll;
    }

    protected boolean isSpecialFaceNeverReroll(int index, DiceType type,
                                                boolean isAttacking, BattleState state, boolean forPlayer) {
        if (type != DiceType.PRISMATIC || state == null) return false;
        PrismaticDiceInstance pd = state.getPrismaticDiceAt(index, forPlayer);
        if (pd == null) return false;
        if (pd.rolledFace >= 6 || pd.isSpecialFace) return true;
        if (!isAttacking && pd.type.getEffect() != null) {
            PrismaticEffect effect = pd.type.getEffect();
            if (effect.isHealHp()) return true;
            if (effect.isGrantStatus() && isDefenseStatusEffect(effect.getGrantedEffect())) return true;
        }
        return false;
    }

    protected boolean isDefenseStatusEffect(StatusEffect effect) {
        return effect == StatusEffect.TOUGHNESS
            || effect == StatusEffect.FORCEFIELD
            || effect == StatusEffect.UNYIELDING
            || effect == StatusEffect.DESTINED
            || effect == StatusEffect.DETERRENCE
            || effect == StatusEffect.COUNTER
            || effect == StatusEffect.REFLECT;
    }

    // ==================== SimPool ====================

    protected static class SimPool {
        private final int[] values;
        private final DiceType[] types;
        private final int[][] possibleFaces;

        public SimPool(int[] values, DiceType[] types, int[][] possibleFaces) {
            this.values = values;
            this.types = types;
            this.possibleFaces = possibleFaces;
        }

        public int size() { return values.length; }
        public int getValue(int idx) { return values[idx]; }
        public DiceType getType(int idx) { return types[idx]; }
        public int[] getPossibleFaces(int idx) { return possibleFaces[idx]; }

        public void applyOutcome(Set<Integer> indices, Outcome outcome) {
            int i = 0;
            for (int idx : indices) {
                values[idx] = outcome.newFaces[i++];
            }
        }

        public void simReroll(Set<Integer> indices) {
            for (int idx : indices) {
                int[] faces = possibleFaces[idx];
                values[idx] = faces[SIM_RAND.nextInt(faces.length)];
            }
        }

        public SimPool createCopy() {
            return new SimPool(values.clone(), types, possibleFaces);
        }
    }

    private SimPool createSimPool(List<Integer> currentValues, List<DiceType> diceTypes,
                                  BattleState state, boolean forPlayer) {
        int n = currentValues.size();
        int[] values = new int[n];
        DiceType[] types = new DiceType[n];
        int[][] possibleFaces = new int[n][];

        for (int i = 0; i < n; i++) {
            values[i] = currentValues.get(i);
            types[i] = diceTypes.get(i);

            if (types[i] == DiceType.PRISMATIC && state != null) {
                PrismaticDiceInstance pd = state.getPrismaticDiceAt(i, forPlayer);
                if (pd != null) {
                    possibleFaces[i] = pd.type.getFaces(pd.isTrueVersion);
                } else {
                    possibleFaces[i] = regularFaces(types[i].getMaxFace());
                }
            } else {
                possibleFaces[i] = regularFaces(types[i].getMaxFace());
            }
        }

        return new SimPool(values, types, possibleFaces);
    }

    private static final Map<Integer, int[]> regularFacesCache = new HashMap<>();

    private static int[] regularFaces(int maxFace) {
        return regularFacesCache.computeIfAbsent(maxFace, mf -> {
            int[] faces = new int[mf];
            for (int i = 0; i < mf; i++) {
                faces[i] = i + 1;
            }
            return faces;
        });
    }

    // ==================== Inner classes ====================

    protected static class RolloutResult {
        final SimPool pool;
        final int rerollsUsed;
        RolloutResult(SimPool pool, int rerollsUsed) {
            this.pool = pool;
            this.rerollsUsed = rerollsUsed;
        }
    }

    protected static class Outcome {
        final int[] newFaces;
        final double probability;
        Outcome(int[] newFaces, double probability) {
            this.newFaces = newFaces;
            this.probability = probability;
        }
    }

    protected interface SubsetEvaluator {
        double evaluateSelection(List<Integer> selectedValues, List<DiceType> selectedTypes,
                                 boolean isAttacking, BattleState state, boolean forPlayer);
    }
}
