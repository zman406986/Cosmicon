package data.scripts.cosmicon.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiceEvaluator {

    private DiceEvaluator() {}

    private static int[] frequencies(List<Integer> values) {
        int[] freq = new int[13];
        for (int v : values) {
            if (v >= 1 && v <= 12) freq[v]++;
        }
        return freq;
    }

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
        int[] freq = frequencies(values);
        int pairs = 0;
        for (int f : freq) pairs += f / 2;
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
        int[] freq = frequencies(values);
        int distinct = 0;
        for (int f : freq) {
            if (f > 0) distinct++;
        }
        return distinct;
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
        if (values == null) return threshold <= 0;
        int sum = 0;
        for (int v : values) {
            sum += v;
            if (sum >= threshold) return true;
        }
        return false;
    }

    public static boolean hasIdenticalNumbers(List<Integer> values) {
        if (values == null || values.size() < 2) return false;
        int[] freq = frequencies(values);
        for (int f : freq) {
            if (f >= 2) return true;
        }
        return false;
    }

    public static int[] frequencyArray(List<Integer> values) {
        return frequencies(values);
    }

    public static boolean hasThreeConsecutive(List<Integer> values) {
        if (values == null || values.size() < 3) return false;
        java.util.Set<Integer> set = new java.util.HashSet<>(values);
        for (int v : set) {
            if (set.contains(v + 1) && set.contains(v + 2)) return true;
        }
        return false;
    }

    public static Map<Integer, Integer> computeFrequencyMap(List<Integer> values) {
        if (values == null || values.isEmpty()) return Collections.emptyMap();
        int[] freq = frequencies(values);
        Map<Integer, Integer> counts = new HashMap<>();
        for (int v = 1; v <= 12; v++) {
            if (freq[v] > 0) counts.put(v, freq[v]);
        }
        return counts;
    }
}
