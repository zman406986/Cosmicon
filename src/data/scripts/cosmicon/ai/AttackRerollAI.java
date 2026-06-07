package data.scripts.cosmicon.ai;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.battle.WeatherType;
import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.prismatic.PrismaticEffect;
import data.scripts.CosmiconConfig;
import data.scripts.cosmicon.util.CosmiconLogger;
import data.scripts.cosmicon.util.DiceEvaluator;
import java.util.*;

public abstract class AttackRerollAI implements CharacterAIProfile {

    protected final int maxExactOutcomeEnumeration = 256;
    private static final int TOP_K_CANDIDATES = 12;
    private static final Map<DiceType, DieStoppingPolicy> policyCache = new EnumMap<>(DiceType.class);
    private static final Random SIM_RAND = new Random(42);

    // Pre-computed destined indices for current planReroll call (avoids repeated lookups)
    private Set<Integer> currentDestinedIndices = Set.of();
    private String currentDestinedKey = "[]";
    private final Map<String, List<Set<Integer>>> subsetCache = new HashMap<>();
    private final Map<Integer, Double> evalCache = new HashMap<>();
    private final Map<Long, Double> stateCache = new HashMap<>();

    static {
        policyCache.put(DiceType.BLUE_D4, computePolicyStatic(DiceType.BLUE_D4));
        policyCache.put(DiceType.PURPLE_D6, computePolicyStatic(DiceType.PURPLE_D6));
        policyCache.put(DiceType.ORANGE_D8, computePolicyStatic(DiceType.ORANGE_D8));
        policyCache.put(DiceType.YELLOW_D12, computePolicyStatic(DiceType.YELLOW_D12));
        policyCache.put(DiceType.PRISMATIC, computePolicyStatic(DiceType.PRISMATIC));
    }

    public AttackRerollAI() {}

    private static void rerollLog(String format, Object... args) {
        if (CosmiconConfig.REROLL_LOG_ENABLED) {
            CosmiconLogger.info("[AI_REROLL_LOG] " + String.format(format, args));
        }
    }

    // ==================== CharacterAIProfile defaults ====================

    @Override
    public boolean prefersHighValues(boolean isAttacking) {
        // Always return true because both attacking and defending benefit from higher dice values
        // (higher attack sum = more damage, higher defense sum = better block).
        // The isAttacking parameter is reserved for future profiles where a passive might
        // incentivize selecting low values (e.g. "if sum <= 5, gain bonus").
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
        this.currentDestinedKey = currentDestinedIndices.toString();
        this.subsetCache.clear();
        this.evalCache.clear();
        this.stateCache.clear();

        double currentEV = evaluateFinalPool(pool, 0, 0, effectiveRequired, isAttacking, state, forPlayer);

        List<Set<Integer>> candidates = generateCandidateActions(pool, rerollsAvailable, effectiveRequired, isAttacking, state, forPlayer);

        rerollLog("%s: pool=%s, rerolls=%d, required=%d, candidates=%d, currentEV=%.1f",
            getCharacterId(), currentValues, rerollsAvailable, effectiveRequired, candidates.size(), currentEV);

        if (candidates.isEmpty()) {
            CosmiconLogger.debug("[AI_REROLL] %s: no candidates generated", getCharacterId());
            return Set.of();
        }

        double bestEV = currentEV;
        Set<Integer> bestAction = Set.of();

        // Top-K heuristic pruning: score candidates, keep top 4 + guaranteed candidate
        List<Set<Integer>> evaluated;
        if (candidates.size() <= TOP_K_CANDIDATES) {
            evaluated = candidates;
        } else {
            Set<Integer> guaranteedCandidate = null;
            for (Set<Integer> c : candidates) {
                boolean allOnes = true;
                for (int i : c) {
                    if (pool.getValue(i) != 1) { allOnes = false; break; }
                }
                if (allOnes) {
                    guaranteedCandidate = c;
                    break;
                }
            }

            List<Map.Entry<Set<Integer>, Double>> scored = new ArrayList<>();
            for (Set<Integer> c : candidates) {
                double h = scoreCandidateHeuristic(pool, c, rerollsAvailable);
                scored.add(Map.entry(c, h));
            }
            scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            Set<Set<Integer>> topK = new LinkedHashSet<>();
            for (int i = 0; i < Math.min(TOP_K_CANDIDATES, scored.size()); i++) {
                topK.add(scored.get(i).getKey());
            }
            if (guaranteedCandidate != null) {
                topK.add(guaranteedCandidate);
            }
            evaluated = new ArrayList<>(topK);

            rerollLog("%s: pruned %d candidates to %d (top-%d + guaranteed)",
                getCharacterId(), candidates.size(), evaluated.size(), TOP_K_CANDIDATES);
        }

        // Upper-bound pruning + full evaluation
        int prunedCount = 0;
        for (Set<Integer> diceToReroll : evaluated) {
            double ub = computeRerollUpperBound(pool, diceToReroll);
            if (ub <= bestEV) {
                prunedCount++;
                rerollLog("PRUNE %s: UB %.1f <= bestEV %.1f", diceToReroll, ub, bestEV);
                continue;
            }
            SIM_RAND.setSeed(42);
            double ev = evaluateImmediateAction(pool, diceToReroll, rerollsAvailable, effectiveRequired, isAttacking, state, forPlayer);
            if (ev > bestEV) {
                bestEV = ev;
                bestAction = diceToReroll;
            }
        }
        if (prunedCount > 0) {
            rerollLog("%s: pruned %d/%d candidates by UB", getCharacterId(), prunedCount, evaluated.size());
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
        double weightedUtility = 0;

        for (Outcome outcome : outcomes) {
            SimPool newPool = pool.createCopy();
            newPool.applyOutcome(diceToReroll, outcome);
            double util = evaluateStateWithRerolls(newPool, rerollsLeft - 1, requiredCount, isAttacking, state, forPlayer);
            weightedUtility += outcome.probability * util;
        }

        return outcomes.isEmpty() ? Double.NEGATIVE_INFINITY : weightedUtility;
    }

    // ==================== Future value estimator ====================

    private double evaluateStateWithRerolls(SimPool pool, int rerollsLeft, int requiredCount,
                                              boolean isAttacking, BattleState state, boolean forPlayer) {
        // Optimization 1: Three-tier die elimination (frozen + doomed)
        ThreeTierSplit split = splitThreeTiers(pool, rerollsLeft, isAttacking, currentDestinedIndices);
        int frozenSum = 0;
        int doomedSum = 0;
        int doomedRerollsUsed = 0;
        Set<Integer> savedDestined = null;
        String savedDestinedKey = null;
        if (split != null) {
            int removedCount = pool.size() - split.reduced().size();
            if (removedCount >= requiredCount) {
                rerollLog("removed=%d >= required=%d, evaluating full pool", removedCount, requiredCount);
                return evaluateFinalPool(pool, 0, 0, requiredCount, isAttacking, state, forPlayer);
            }
            frozenSum = split.frozenSum();
            doomedSum = split.doomedSum();
            doomedRerollsUsed = split.doomedRerollsUsed();
            int frozenCount = removedCount - doomedRerollsUsed;
            int effectiveRequired = requiredCount - frozenCount;
            rerollLog("frozen=%d (sum=%d), doomed=%d (ev=%d, rerollsUsed=%d), reduced pool=%d, effectiveRequired=%d",
                frozenCount, frozenSum, doomedRerollsUsed, doomedSum, doomedRerollsUsed,
                split.reduced().size(), effectiveRequired);
            savedDestined = this.currentDestinedIndices;
            savedDestinedKey = this.currentDestinedKey;
            this.currentDestinedIndices = remapDestinedIndices(currentDestinedIndices, split);
            this.currentDestinedKey = this.currentDestinedIndices.toString();
            pool = split.reduced();
            requiredCount = effectiveRequired;
            rerollsLeft -= doomedRerollsUsed;
        }

        // Cache lookup (after split, using reduced pool)
        long cacheKey = (long) pool.fingerprint() * 31L
                      + (long) rerollsLeft * 1_000_003L
                      + (long) frozenSum * 1_000_033L
                      + (long) doomedSum * 1_000_037L
                      + (long) requiredCount * 1_000_039L;
        Double cached = stateCache.get(cacheKey);
        if (cached != null) {
            if (savedDestined != null) {
                this.currentDestinedIndices = savedDestined;
                this.currentDestinedKey = savedDestinedKey;
            }
            return cached;
        }

        int baseSum = frozenSum + doomedSum;
        double result;
        if (rerollsLeft <= 0) {
            result = evaluateFinalPool(pool, baseSum, 0, requiredCount, isAttacking, state, forPlayer);
        } else {
            Set<Integer> toReroll = findPolicyRerollSet(pool, rerollsLeft, isAttacking, state, forPlayer);

            if (toReroll.isEmpty()) {
                result = evaluateFinalPool(pool, baseSum, 0, requiredCount, isAttacking, state, forPlayer);
            } else {
                long product = 1;
                for (int idx : toReroll) {
                    product *= pool.getPossibleFaces(idx).length;
                    if (product > maxExactOutcomeEnumeration) break;
                }

                if (product <= maxExactOutcomeEnumeration) {
                    rerollLog("exact enumeration: product=%d, rerollSet=%d dice", product, toReroll.size());
                    List<Outcome> outcomes = generateOutcomes(pool, toReroll);
                    double totalUtility = 0;
                    for (Outcome outcome : outcomes) {
                        SimPool newPool = pool.createCopy();
                        newPool.applyOutcome(toReroll, outcome);
                        double util = evaluateStateWithRerolls(newPool, rerollsLeft - 1, requiredCount, isAttacking, state, forPlayer);
                        totalUtility += outcome.probability * util;
                    }
                    result = totalUtility + baseSum;
                } else {
                    // Optimization 3: Adaptive Monte Carlo rollouts scaled by rerollsLeft
                    int minRollouts = getMinRollouts(rerollsLeft);
                    int maxRollouts = getMaxRollouts(rerollsLeft);
                    double sum = 0, sumSq = 0;
                    int actualRollouts = 0;

                    for (int i = 0; i < maxRollouts; i++) {
                        RolloutResult rollout = simulateBasePolicy(pool, rerollsLeft, isAttacking, state, forPlayer);
                        double finalUtil = evaluateFinalPool(rollout.pool, baseSum, rollout.rerollsUsed, requiredCount, isAttacking, state, forPlayer);
                        sum += finalUtil;
                        sumSq += finalUtil * finalUtil;
                        actualRollouts = i + 1;

                        if (i >= minRollouts - 1) {
                            double mean = sum / actualRollouts;
                            double variance = (sumSq / actualRollouts) - mean * mean;
                            double stderr = Math.sqrt(Math.max(0, variance) / actualRollouts);
                            if (stderr < Math.max(Math.abs(mean) * 0.03, 0.3)) break;
                        }
                    }
                    rerollLog("Monte Carlo: %d/%d rollouts (converged=%b)", actualRollouts, maxRollouts, actualRollouts < maxRollouts);
                    result = sum / actualRollouts;
                }
            }
        }

        stateCache.put(cacheKey, result);

        if (savedDestined != null) {
            this.currentDestinedIndices = savedDestined;
            this.currentDestinedKey = savedDestinedKey;
        }
        return result;
    }

    private Set<Integer> findPolicyRerollSet(SimPool pool, int rerollsLeft,
                                              boolean isAttacking, BattleState state, boolean forPlayer) {
        Set<Integer> toReroll = new HashSet<>();
        for (int i = 0; i < pool.size(); i++) {
            if (isSpecialFaceNeverReroll(i, pool.getType(i), isAttacking, state, forPlayer)) continue;
            DieStoppingPolicy policy = getStoppingPolicy(pool.getType(i));
            boolean shouldReroll = isAttacking
                ? policy.shouldReroll(pool.getValue(i), rerollsLeft)
                : policy.shouldRerollConservative(pool.getValue(i), rerollsLeft);
            if (shouldReroll) toReroll.add(i);
        }
        return toReroll;
    }

    private double evaluateFinalPool(SimPool pool, int frozenSum, int rerollsUsed, int requiredCount,
                                      boolean isAttacking, BattleState state, boolean forPlayer) {
        int cacheKey = pool.fingerprint() * 31 + frozenSum * 37 + rerollsUsed * 31 + requiredCount;
        Double cached = evalCache.get(cacheKey);
        if (cached != null) return cached;

        SubsetEvaluator evaluator = getSubsetEvaluator();
        List<Set<Integer>> choices = enumerateLegalSubsets(pool, requiredCount);

        if (choices.isEmpty()) {
            evalCache.put(cacheKey, (double) frozenSum);
            return frozenSum;
        }

        float rerollDamage = 0f;
        if (rerollsUsed > 2) {
            float penaltyPerReroll = getThornsPenaltyPerReroll();
            if (state != null && state.getWeatherController() != null
                && state.getWeatherController().getCurrentWeather() == WeatherType.PARHELION) {
                penaltyPerReroll += 2f;
            }
            rerollDamage = (rerollsUsed - 2) * penaltyPerReroll;
        }
        boolean fineSnowBonus = rerollsUsed == 0 && isAttacking && state != null
            && state.getWeatherController() != null
            && state.getWeatherController().getCurrentWeather() == WeatherType.FINE_SNOW;

        double bestUtil = Double.NEGATIVE_INFINITY;
        for (Set<Integer> subset : choices) {
            List<Integer> selectedValues = new ArrayList<>();
            List<DiceType> selectedTypes = new ArrayList<>();
            for (int idx : subset) {
                selectedValues.add(pool.getValue(idx));
                selectedTypes.add(pool.getType(idx));
            }
            double util = evaluator.evaluateSelection(selectedValues, selectedTypes, isAttacking, state, forPlayer);
            util -= rerollDamage;
            if (fineSnowBonus) util += 3f;
            if (util > bestUtil) bestUtil = util;
        }
        double result = bestUtil + frozenSum;
        evalCache.put(cacheKey, result);
        return result;
    }

    protected int getMinRollouts(int rerollsLeft) {
        return Math.max(10, 8 * rerollsLeft);
    }

    protected int getMaxRollouts(int rerollsLeft) {
        return Math.max(50, 80 * rerollsLeft);
    }

    private double computeRerollUpperBound(SimPool pool, Set<Integer> diceToReroll) {
        double sum = 0;
        for (int i = 0; i < pool.size(); i++) {
            if (diceToReroll.contains(i)) {
                sum += pool.getType(i).getMaxFace();
            } else {
                sum += pool.getValue(i);
            }
        }
        return sum;
    }

    private double scoreCandidateHeuristic(SimPool pool, Set<Integer> diceToReroll, int rerollsLeft) {
        double score = 0;
        for (int idx : diceToReroll) {
            DieEVTable table = DieEVTable.get(pool.getType(idx));
            score += Math.max(0, table.gain(pool.getValue(idx), rerollsLeft));
        }
        return score;
    }

    // ==================== Base policy simulation ====================

    private RolloutResult simulateBasePolicy(SimPool pool, int rerollsLeft,
                                               boolean isAttacking, BattleState state, boolean forPlayer) {
        int r = rerollsLeft;
        int rerollsUsed = 0;
        SimPool copy = pool.createCopy();

        while (r > 0) {
            Set<Integer> toReroll = findPolicyRerollSet(copy, r, isAttacking, state, forPlayer);
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

        boolean[] neverReroll = new boolean[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            neverReroll[i] = isSpecialFaceNeverReroll(i, pool.getType(i), isAttacking, state, forPlayer);
        }

        Set<Integer> thresholdSet = new HashSet<>();
        for (int i = 0; i < pool.size(); i++) {
            if (!neverReroll[i]) {
                DieStoppingPolicy policy = getStoppingPolicy(pool.getType(i));
                if (policy.shouldReroll(pool.getValue(i), rerollsLeft)) {
                    thresholdSet.add(i);
                }
            }
        }

        // Optimization 2: Split below-threshold into guaranteed (value-1) and borderline
        List<Integer> guaranteed = new ArrayList<>();
        List<Integer> borderline = new ArrayList<>();
        for (int i : thresholdSet) {
            if (pool.getValue(i) == 1) guaranteed.add(i);
            else borderline.add(i);
        }
        rerollLog("below-threshold: %d guaranteed (value-1), %d borderline", guaranteed.size(), borderline.size());

        if (!borderline.isEmpty()) {
            List<Set<Integer>> borderlineSubs = new ArrayList<>();
            generateCandidateSubsets(borderline, borderlineSubs, pool);
            Set<Integer> guaranteedSet = new HashSet<>(guaranteed);
            for (Set<Integer> sub : borderlineSubs) {
                Set<Integer> combined = new HashSet<>(guaranteedSet);
                combined.addAll(sub);
                candidates.add(combined);
            }
        }
        if (!guaranteed.isEmpty()) {
            candidates.add(new HashSet<>(guaranteed));
        }

        Set<Integer> aboveThresholdSeen = new HashSet<>();
        for (int i = 0; i < pool.size(); i++) {
            if (!neverReroll[i] && !thresholdSet.contains(i)) {
                int key = symmetryKey(pool, i);
                if (aboveThresholdSeen.add(key)) {
                    candidates.add(Set.of(i));
                }
            }
        }

        candidates.addAll(generateComboCandidates(pool, rerollsLeft, requiredCount, isAttacking, state, forPlayer));

        return new ArrayList<>(new LinkedHashSet<>(candidates));
    }

    private int symmetryKey(SimPool pool, int index) {
        DiceType type = pool.getType(index);
        int hash = type.hashCode() * 31 + pool.getValue(index);
        if (type == DiceType.PRISMATIC) {
            hash = hash * 31 + java.util.Arrays.hashCode(pool.getPossibleFaces(index));
        }
        return hash;
    }

    private void generateCandidateSubsets(List<Integer> elements, List<Set<Integer>> result, SimPool pool) {
        Map<Integer, List<Integer>> groups = new LinkedHashMap<>();
        for (int idx : elements) {
            groups.computeIfAbsent(symmetryKey(pool, idx), k -> new ArrayList<>()).add(idx);
        }
        List<List<Integer>> groupList = new ArrayList<>(groups.values());
        for (List<Integer> g : groupList) Collections.sort(g);

        int n = elements.size();
        if (n <= 5) {
            generateCanonicalSubsets(groupList, 0, new HashSet<>(), result);
        } else {
            for (List<Integer> g : groupList) {
                result.add(Set.of(g.get(0)));
            }
            for (int i = 0; i < groupList.size(); i++) {
                for (int j = i; j < groupList.size(); j++) {
                    List<Integer> gi = groupList.get(i);
                    List<Integer> gj = groupList.get(j);
                    if (i == j) {
                        if (gi.size() >= 2) {
                            result.add(Set.of(gi.get(0), gi.get(1)));
                        }
                    } else {
                        result.add(Set.of(gi.get(0), gj.get(0)));
                    }
                }
            }
        }
    }

    private void generateCanonicalSubsets(List<List<Integer>> groups, int groupIdx,
                                           Set<Integer> current, List<Set<Integer>> result) {
        if (!current.isEmpty()) {
            result.add(new HashSet<>(current));
        }
        for (int g = groupIdx; g < groups.size(); g++) {
            List<Integer> group = groups.get(g);
            for (int take = 1; take <= group.size(); take++) {
                for (int i = 0; i < take; i++) current.add(group.get(i));
                generateCanonicalSubsets(groups, g + 1, current, result);
                for (int i = 0; i < take; i++) current.remove(group.get(i));
            }
        }
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
        if (matchCount >= 1 && matchCount < requiredCount && !nonMatchingIndices.isEmpty()) {
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

    public static class DieStoppingPolicy {
        private final int[] thresholds;
        private final double[] freshExpectation;

        public DieStoppingPolicy(int[] thresholds, double[] freshExpectation) {
            this.thresholds = thresholds;
            this.freshExpectation = freshExpectation;
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

        public double getFreshExpectation(int rerollsLeft) {
            if (rerollsLeft <= 0) return 0;
            if (rerollsLeft >= freshExpectation.length) rerollsLeft = freshExpectation.length - 1;
            return freshExpectation[rerollsLeft];
        }
    }

    public static DieStoppingPolicy getStoppingPolicy(DiceType type) {
        return policyCache.computeIfAbsent(type, k -> computePolicyStatic(k));
    }

    private static DieStoppingPolicy computePolicyStatic(DiceType type) {
        DieEVTable table = DieEVTable.get(type);
        int[] thresholds = new int[DieEVTable.MAX_REROLLS + 1];
        double[] freshEV = new double[DieEVTable.MAX_REROLLS + 1];
        for (int r = 0; r <= DieEVTable.MAX_REROLLS; r++) {
            thresholds[r] = table.threshold(r);
            freshEV[r] = table.freshEV(r);
        }
        return new DieStoppingPolicy(thresholds, freshEV);
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
            return sampleOutcomes(diceFaces, (int) Math.min(maxExactOutcomeEnumeration, product));
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

    private int computeSampleCount(int faceProduct) {
        return Math.max(16, Math.min(maxExactOutcomeEnumeration, faceProduct * 4));
    }

    private List<Outcome> sampleOutcomes(List<int[]> diceFaces, int faceProduct) {
        int sampleCount = computeSampleCount(faceProduct);
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

        for (int idx : destinedIndices) {
            if (idx < start && !current.contains(idx)) {
                return;
            }
        }

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

    protected double calculateRerollSelfDamage(int rerollsUsed, BattleState state) {
        float penaltyPerReroll = getThornsPenaltyPerReroll();
        if (state != null && state.getWeatherController() != null
            && state.getWeatherController().getCurrentWeather() == WeatherType.PARHELION) {
            penaltyPerReroll += 2f;
        }
        return Math.max(0, rerollsUsed - 2) * penaltyPerReroll;
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
            || effect == StatusEffect.COUNTER;
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

        public SimPool subset(int[] indices) {
            int n = indices.length;
            int[] newValues = new int[n];
            DiceType[] newTypes = new DiceType[n];
            int[][] newFaces = new int[n][];
            for (int i = 0; i < n; i++) {
                newValues[i] = values[indices[i]];
                newTypes[i] = types[indices[i]];
                newFaces[i] = possibleFaces[indices[i]];
            }
            return new SimPool(newValues, newTypes, newFaces);
        }

        public int fingerprint() {
            return java.util.Arrays.hashCode(values);
        }
    }

    // ==================== Frozen die splitting ====================

    private record SplitPool(SimPool reduced, int frozenSum, int[] indexRemap, Map<Integer, Integer> reverseRemap) {}

    private SplitPool splitFrozenDice(SimPool pool, boolean isAttacking) {
        List<Integer> keep = new ArrayList<>();
        int frozenSum = 0;
        for (int i = 0; i < pool.size(); i++) {
            if (pool.getValue(i) == pool.getType(i).getMaxFace()
                && !isSpecialFaceNeverReroll(i, pool.getType(i), isAttacking, null, false)) {
                frozenSum += pool.getValue(i);
            } else {
                keep.add(i);
            }
        }
        if (keep.isEmpty()) return null;
        int[] remap = new int[keep.size()];
        for (int i = 0; i < keep.size(); i++) remap[i] = keep.get(i);
        Map<Integer, Integer> reverse = new HashMap<>();
        for (int i = 0; i < remap.length; i++) {
            reverse.put(remap[i], i);
        }
        return new SplitPool(pool.subset(remap), frozenSum, remap, reverse);
    }

    private Set<Integer> remapDestinedIndices(Set<Integer> original, SplitPool split) {
        Set<Integer> remapped = new HashSet<>();
        for (int idx : original) {
            Integer newIdx = split.reverseRemap().get(idx);
            if (newIdx != null) {
                remapped.add(newIdx);
            }
        }
        return remapped;
    }

    private Set<Integer> remapDestinedIndices(Set<Integer> original, ThreeTierSplit split) {
        Set<Integer> remapped = new HashSet<>();
        for (int idx : original) {
            Integer newIdx = split.reverseRemap().get(idx);
            if (newIdx != null) {
                remapped.add(newIdx);
            }
        }
        return remapped;
    }

    private record ThreeTierSplit(
        SimPool reduced,
        int frozenSum,
        int doomedSum,
        int doomedRerollsUsed,
        int[] indexRemap,
        Map<Integer, Integer> reverseRemap
    ) {}

    private ThreeTierSplit splitThreeTiers(SimPool pool, int rerollsLeft,
                                             boolean isAttacking, Set<Integer> destinedIndices) {
        if (rerollsLeft <= 0) {
            SplitPool frozen = splitFrozenDice(pool, isAttacking);
            if (frozen == null) return null;
            return new ThreeTierSplit(frozen.reduced(), frozen.frozenSum(), 0, 0,
                frozen.indexRemap(), frozen.reverseRemap());
        }

        List<Integer> active = new ArrayList<>();
        List<int[]> doomedEntries = new ArrayList<>();
        int frozenSum = 0;

        for (int i = 0; i < pool.size(); i++) {
            DiceType type = pool.getType(i);
            int value = pool.getValue(i);

            if (isSpecialFaceNeverReroll(i, type, isAttacking, null, false)) {
                active.add(i);
                continue;
            }

            if (value == type.getMaxFace()) {
                frozenSum += value;
                continue;
            }

            DieStoppingPolicy policy = getStoppingPolicy(type);
            if (policy.shouldReroll(value, rerollsLeft) && !destinedIndices.contains(i)) {
                doomedEntries.add(new int[]{i, value});
            } else {
                active.add(i);
            }
        }

        if (doomedEntries.isEmpty()) {
            if (active.isEmpty()) return null;
            Collections.sort(active);
            int[] remap = new int[active.size()];
            for (int i = 0; i < active.size(); i++) remap[i] = active.get(i);
            Map<Integer, Integer> reverse = new HashMap<>();
            for (int i = 0; i < remap.length; i++) reverse.put(remap[i], i);
            return new ThreeTierSplit(pool.subset(remap), frozenSum, 0, 0, remap, reverse);
        }

        int doomedToRemove = Math.min(doomedEntries.size(), rerollsLeft);
        if (doomedEntries.size() > rerollsLeft) {
            doomedEntries.sort(Comparator.comparingInt(e -> e[1]));
            for (int i = rerollsLeft; i < doomedEntries.size(); i++) {
                active.add(doomedEntries.get(i)[0]);
            }
        }

        int doomedSum = 0;
        for (int i = 0; i < doomedToRemove; i++) {
            int origIdx = doomedEntries.get(i)[0];
            DiceType type = pool.getType(origIdx);
            DieStoppingPolicy policy = getStoppingPolicy(type);
            doomedSum += (int) Math.ceil(policy.getFreshExpectation(rerollsLeft - 1));
        }

        Collections.sort(active);
        int[] remap = new int[active.size()];
        for (int i = 0; i < active.size(); i++) remap[i] = active.get(i);
        Map<Integer, Integer> reverse = new HashMap<>();
        for (int i = 0; i < remap.length; i++) {
            reverse.put(remap[i], i);
        }

        if (remap.length == 0) return null;

        return new ThreeTierSplit(
            pool.subset(remap),
            frozenSum,
            doomedSum,
            doomedToRemove,
            remap,
            reverse
        );
    }

    private SimPool createSimPool(List<Integer> currentValues, List<DiceType> diceTypes,
                                  BattleState state, boolean forPlayer) {
        int n = currentValues.size();
        int[] values = new int[n];
        DiceType[] types = new DiceType[n];
        int[][] possibleFaces = new int[n][];

        WeatherType weather = state != null && state.getWeatherController() != null
            ? state.getWeatherController().getCurrentWeather() : null;
        boolean preventMin = weather == WeatherType.FROG_RAIN;
        boolean isDefender = state != null && !state.isAttacker(forPlayer);
        boolean preventMax = weather == WeatherType.SUNSHOWER && isDefender;

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

            if (preventMin || preventMax) {
                possibleFaces[i] = adjustFacesForWeather(possibleFaces[i], preventMin, preventMax);
            }
        }

        return new SimPool(values, types, possibleFaces);
    }

    private static int[] adjustFacesForWeather(int[] faces, boolean preventMin, boolean preventMax) {
        int min = preventMin ? 2 : 1;
        int max = preventMax ? faces[faces.length - 1] - 1 : faces[faces.length - 1];
        if (min > max) return faces;
        int[] adjusted = new int[max - min + 1];
        for (int v = min; v <= max; v++) {
            adjusted[v - min] = v;
        }
        return adjusted;
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
