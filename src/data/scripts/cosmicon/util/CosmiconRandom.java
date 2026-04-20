package data.scripts.cosmicon.util;

import java.util.Random;

public final class CosmiconRandom {

    private static final Random RANDOM = new Random();

    private CosmiconRandom() {}

    public static Random getRandom() {
        return RANDOM;
    }

    public static int nextInt(int bound) {
        return RANDOM.nextInt(bound);
    }

    public static float nextFloat() {
        return RANDOM.nextFloat();
    }

    public static boolean nextBoolean() {
        return RANDOM.nextBoolean();
    }
}