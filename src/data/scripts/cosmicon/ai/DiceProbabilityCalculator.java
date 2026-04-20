package data.scripts.cosmicon.ai;

import data.scripts.cosmicon.battle.DiceType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DiceProbabilityCalculator {

    private DiceProbabilityCalculator() {}

    private static final Map<String, float[]> SUM_PMF_CACHE = new HashMap<>();
    private static final int MAX_CACHE_KEY_LENGTH = 8;

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

        String cacheKey = buildCacheKey(diceTypes, selectCount);
        if (SUM_PMF_CACHE.containsKey(cacheKey)) {
            return SUM_PMF_CACHE.get(cacheKey);
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

    public static float probabilityAllSame(List<Integer> values) {
        if (values == null || values.isEmpty()) return 0f;
        int first = values.get(0);
        for (int v : values) {
            if (v != first) return 0f;
        }
        return 1.0f;
    }

    public static float probabilityAllEven(List<Integer> values) {
        if (values == null || values.isEmpty()) return 0f;
        for (int v : values) {
            if (v % 2 != 0) return 0f;
        }
        return 1.0f;
    }

    public static float probabilityAllFour(List<Integer> values) {
        if (values == null || values.isEmpty()) return 0f;
        for (int v : values) {
            if (v != 4) return 0f;
        }
        return 1.0f;
    }

    public static int countPairs(List<Integer> values) {
        if (values == null || values.isEmpty()) return 0;
        Map<Integer, Integer> counts = new HashMap<>();
        for (int v : values) {
            counts.merge(v, 1, Integer::sum);
        }
        int pairs = 0;
        for (int count : counts.values()) {
            pairs += count / 2;
        }
        return pairs;
    }

    private static String buildCacheKey(List<DiceType> diceTypes, int selectCount) {
        StringBuilder sb = new StringBuilder();
        sb.append(selectCount).append(":");
        for (DiceType type : diceTypes) {
            sb.append(type.getMaxFace()).append(",");
        }
        return sb.toString();
    }

    public static void clearCache() {
        SUM_PMF_CACHE.clear();
    }

    public static int getMinPossibleSum(List<DiceType> diceTypes, int selectCount) {
        return selectCount;
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
}