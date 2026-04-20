package data.scripts.cosmicon.ai;

import data.scripts.cosmicon.battle.DiceType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DiceProbabilityCalculator {

    private DiceProbabilityCalculator() {}

    private static final int MAX_CACHE_SIZE = 64;
    private static final int MAX_CACHE_KEY_LENGTH = 8;

    private static final Map<DiceCacheKey, float[]> SUM_PMF_CACHE = new LinkedHashMap<>(16, 0.75f, true)
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<DiceCacheKey, float[]> eldest)
        {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private static final class DiceCacheKey {
        private final int selectCount;
        private final int[] maxFaces;
        private final int cachedHashCode;

        DiceCacheKey(int selectCount, int[] maxFaces) {
            this.selectCount = selectCount;
            this.maxFaces = maxFaces;
            this.cachedHashCode = computeHashCode();
        }

        private int computeHashCode() {
            int h = Objects.hash(selectCount);
            for (int face : maxFaces) {
                h = 31 * h + face;
            }
            return h;
        }

        @Override
        public int hashCode() {
            return cachedHashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof DiceCacheKey other)) return false;
            if (selectCount != other.selectCount) return false;
            if (maxFaces.length != other.maxFaces.length) return false;
            for (int i = 0; i < maxFaces.length; i++) {
                if (maxFaces[i] != other.maxFaces[i]) return false;
            }
            return true;
        }
    }

    public static float expectedValue(DiceType type) {
        return switch (type) {
            case BLUE_D4 -> 2.5f;
            case PURPLE_D6 -> 3.5f;
            case ORANGE_D8 -> 4.5f;
            case PRISMATIC_D12 -> 6.5f;
        };
    }

    public static float expectedValue(int[] customFaces) {
        if (customFaces == null || customFaces.length == 0) return 0f;
        float sum = 0f;
        for (int face : customFaces) {
            sum += face;
        }
        return sum / customFaces.length;
    }

    public static float[] getSingleDicePMF(DiceType type) {
        int maxFace = type.getMaxFace();
        float[] pmf = new float[maxFace + 1];
        float probability = 1.0f / maxFace;
        for (int i = 1; i <= maxFace; i++) {
            pmf[i] = probability;
        }
        return pmf;
    }

    public static float[] getSingleDicePMF(int[] customFaces) {
        if (customFaces == null || customFaces.length == 0) return new float[0];
        int maxFace = 0;
        for (int face : customFaces) {
            maxFace = Math.max(maxFace, face);
        }
        float[] pmf = new float[maxFace + 1];
        float probability = 1.0f / customFaces.length;
        for (int face : customFaces) {
            pmf[face] += probability;
        }
        return pmf;
    }

    public static float[] getSumPMF(List<DiceType> diceTypes, int selectCount) {
        if (diceTypes == null || diceTypes.isEmpty() || selectCount <= 0) {
            return new float[0];
        }
        if (selectCount > diceTypes.size()) {
            selectCount = diceTypes.size();
        }

        DiceCacheKey cacheKey = buildCacheKey(diceTypes, selectCount);
        float[] cached = SUM_PMF_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<float[]> dicePMFs = new ArrayList<>();
        for (DiceType type : diceTypes) {
            dicePMFs.add(getSingleDicePMF(type));
        }

        float[] resultPMF = computeSelectKSumPMF(dicePMFs, selectCount);

        if (diceTypes.size() <= MAX_CACHE_KEY_LENGTH) {
            SUM_PMF_CACHE.put(cacheKey, resultPMF);
        }

        return resultPMF;
    }

    public static float[] getSumPMFWithCustomDice(List<int[]> customFaceArrays, int selectCount) {
        if (customFaceArrays == null || customFaceArrays.isEmpty() || selectCount <= 0) {
            return new float[0];
        }

        List<float[]> dicePMFs = new ArrayList<>();
        for (int[] faces : customFaceArrays) {
            dicePMFs.add(getSingleDicePMF(faces));
        }

        return computeSelectKSumPMF(dicePMFs, selectCount);
    }

    private static float[] computeSelectKSumPMF(List<float[]> dicePMFs, int k) {
        int n = dicePMFs.size();
        int maxSum = 0;
        for (float[] pmf : dicePMFs) {
            maxSum += pmf.length - 1;
        }

        float[][][] dp = new float[n + 1][k + 1][maxSum + 1];
        dp[0][0][0] = 1.0f;

        for (int i = 1; i <= n; i++) {
            float[] currentPMF = dicePMFs.get(i - 1);
            int currentMax = currentPMF.length - 1;

            for (int j = 0; j <= Math.min(i, k); j++) {
                for (int s = 0; s <= maxSum; s++) {
                    if (dp[i - 1][j][s] > 0) {
                        dp[i][j][s] += dp[i - 1][j][s];
                    }
                }

                if (j > 0) {
                    for (int s = 0; s <= maxSum - currentMax; s++) {
                        if (dp[i - 1][j - 1][s] > 0) {
                            for (int face = 1; face <= currentMax; face++) {
                                if (currentPMF[face] > 0) {
                                    dp[i][j][s + face] += dp[i - 1][j - 1][s] * currentPMF[face];
                                }
                            }
                        }
                    }
                }
            }
        }

        return dp[n][k];
    }

    public static float probabilitySumAtLeast(List<DiceType> diceTypes, int selectCount, int target) {
        float[] pmf = getSumPMF(diceTypes, selectCount);
        if (pmf.length == 0) return 0f;

        float cumulative = 0f;
        for (int s = target; s < pmf.length; s++) {
            cumulative += pmf[s];
        }
        return cumulative;
    }

    public static float expectedSum(List<DiceType> diceTypes, int selectCount) {
        float[] pmf = getSumPMF(diceTypes, selectCount);
        if (pmf.length == 0) return 0f;

        float expected = 0f;
        for (int s = 0; s < pmf.length; s++) {
            expected += s * pmf[s];
        }
        return expected;
    }

    private static DiceCacheKey buildCacheKey(List<DiceType> diceTypes, int selectCount) {
        int[] maxFaces = new int[diceTypes.size()];
        for (int i = 0; i < diceTypes.size(); i++) {
            maxFaces[i] = diceTypes.get(i).getMaxFace();
        }
        return new DiceCacheKey(selectCount, maxFaces);
    }

    public static void clearCache() {
        SUM_PMF_CACHE.clear();
    }

    public static int getMaxPossibleSum(List<DiceType> diceTypes, int selectCount) {
        int maxSum = 0;
        List<Integer> maxFaces = new ArrayList<>();
        for (DiceType type : diceTypes) {
            maxFaces.add(type.getMaxFace());
        }
        maxFaces.sort(java.util.Collections.reverseOrder());
        for (int i = 0; i < Math.min(selectCount, maxFaces.size()); i++) {
            maxSum += maxFaces.get(i);
        }
        return maxSum;
    }

    public static int countPairs(List<Integer> values) {
        if (values == null || values.size() < 2) return 0;
        Map<Integer, Integer> counts = new java.util.HashMap<>();
        for (int v : values) {
            counts.merge(v, 1, Integer::sum);
        }
        int pairs = 0;
        for (int count : counts.values()) {
            pairs += count / 2;
        }
        return pairs;
    }
}
