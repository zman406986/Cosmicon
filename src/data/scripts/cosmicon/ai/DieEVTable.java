package data.scripts.cosmicon.ai;

import data.scripts.cosmicon.battle.DiceType;
import java.util.EnumMap;
import java.util.Map;

public final class DieEVTable {

    public static final int MAX_REROLLS = 7;
    private static final Map<DiceType, DieEVTable> CACHE = new EnumMap<>(DiceType.class);

    static {
        CACHE.put(DiceType.BLUE_D4, compute(4));
        CACHE.put(DiceType.PURPLE_D6, compute(6));
        CACHE.put(DiceType.ORANGE_D8, compute(8));
        CACHE.put(DiceType.YELLOW_D12, compute(12));
        CACHE.put(DiceType.PRISMATIC, compute(6));
    }

    private final int maxFace;
    private final double[] freshEV;
    private final double[][] expected;
    private final int[] thresholds;

    private DieEVTable(int maxFace, double[] freshEV, double[][] expected, int[] thresholds) {
        this.maxFace = maxFace;
        this.freshEV = freshEV;
        this.expected = expected;
        this.thresholds = thresholds;
    }

    public static DieEVTable get(DiceType type) {
        return CACHE.computeIfAbsent(type, k -> compute(k.getMaxFace()));
    }

    public double freshEV(int rerollsLeft) {
        if (rerollsLeft < 0) rerollsLeft = 0;
        if (rerollsLeft >= freshEV.length) rerollsLeft = freshEV.length - 1;
        return freshEV[rerollsLeft];
    }

    public double gain(int currentValue, int rerollsLeft) {
        if (rerollsLeft <= 0) return 0;
        return freshEV(rerollsLeft - 1) - currentValue;
    }

    public double expectedValue(int rerollsLeft, int face) {
        int r = Math.max(0, Math.min(rerollsLeft, expected.length - 1));
        int f = Math.max(1, Math.min(face, maxFace));
        return expected[r][f];
    }

    public int threshold(int rerollsLeft) {
        if (rerollsLeft < 0) rerollsLeft = 0;
        if (rerollsLeft >= thresholds.length) rerollsLeft = thresholds.length - 1;
        return thresholds[rerollsLeft];
    }

    private static DieEVTable compute(int maxFace) {
        double[][] expected = new double[MAX_REROLLS + 1][maxFace + 1];
        int[] thresholds = new int[MAX_REROLLS + 1];

        for (int face = 1; face <= maxFace; face++) {
            expected[0][face] = face;
        }
        thresholds[0] = maxFace + 1;

        double[] freshEV = new double[MAX_REROLLS + 1];
        freshEV[0] = (maxFace + 1.0) / 2.0;

        for (int r = 1; r <= MAX_REROLLS; r++) {
            double sum = 0;
            for (int newFace = 1; newFace <= maxFace; newFace++) {
                sum += expected[r - 1][newFace];
            }
            double fe = sum / maxFace;
            freshEV[r] = fe;

            int ceilFresh = (int) Math.ceil(fe);
            thresholds[r] = Math.min(ceilFresh, maxFace);

            for (int face = 1; face <= maxFace; face++) {
                expected[r][face] = Math.max(face, fe);
            }
        }

        return new DieEVTable(maxFace, freshEV, expected, thresholds);
    }
}
