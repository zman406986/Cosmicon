package data.scripts.cosmicon.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DiceEvaluator {

    private DiceEvaluator() {}

    public static boolean allDiceEqualFour(List<Integer> values) {
        if (values == null || values.isEmpty()) return false;
        for (int v : values) {
            if (v != 4) return false;
        }
        return true;
    }

    public static boolean hasTwoPairs(List<Integer> values) {
        return countPairs(values) >= 2;
    }

    public static boolean allEven(List<Integer> values) {
        if (values == null || values.isEmpty()) return false;
        for (int v : values) {
            if (v % 2 != 0) return false;
        }
        return true;
    }

    public static boolean allSame(List<Integer> values) {
        if (values == null || values.isEmpty()) return false;
        int first = values.get(0);
        for (int v : values) {
            if (v != first) return false;
        }
        return true;
    }

    public static boolean allDiceEqualSix(List<Integer> values) {
        if (values == null || values.isEmpty()) return false;
        for (int v : values) {
            if (v != 6) return false;
        }
        return true;
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

    public static int countOddNumbers(List<Integer> values) {
        if (values == null) return 0;
        int count = 0;
        for (int v : values) {
            if (v % 2 != 0) count++;
        }
        return count;
    }

    public static int countDistinctValues(List<Integer> values) {
        if (values == null || values.isEmpty()) return 0;
        Set<Integer> distinct = new HashSet<>(values);
        return distinct.size();
    }

    public static int sumOfValues(List<Integer> values) {
        if (values == null || values.isEmpty()) return 0;
        int sum = 0;
        for (int v : values) {
            sum += v;
        }
        return sum;
    }

    public static boolean sumAtLeast(List<Integer> values, int threshold) {
        return sumOfValues(values) >= threshold;
    }

    public static boolean hasIdenticalNumbers(List<Integer> values) {
        if (values == null || values.size() < 2) return false;
        Map<Integer, Integer> counts = new HashMap<>();
        for (int v : values) {
            counts.merge(v, 1, Integer::sum);
            if (counts.get(v) >= 2) return true;
        }
        return false;
    }

    public static Map<Integer, Integer> computeFrequencyMap(List<Integer> values) {
        if (values == null || values.isEmpty()) return Collections.emptyMap();
        Map<Integer, Integer> counts = new HashMap<>();
        for (int v : values) {
            counts.merge(v, 1, Integer::sum);
        }
        return counts;
    }
}