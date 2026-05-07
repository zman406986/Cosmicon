package data.scripts.cosmicon.util;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class CosmiconRandom {

    private CosmiconRandom() {}

    public static Random getRandom() {
        return ThreadLocalRandom.current();
    }

    public static int nextInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    public static float nextFloat() {
        return ThreadLocalRandom.current().nextFloat();
    }

    public static boolean nextBoolean() {
        return ThreadLocalRandom.current().nextBoolean();
    }
}
