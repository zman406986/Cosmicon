package data.scripts.cosmicon.battle;

import java.util.EnumMap;
import java.util.List;

public class DicePoolCounts {
    private final EnumMap<DiceType, Integer> counts;
    
    private DicePoolCounts(EnumMap<DiceType, Integer> counts) {
        this.counts = counts;
    }
    
    public static DicePoolCounts fromPool(List<DiceType> pool) {
        EnumMap<DiceType, Integer> map = new EnumMap<>(DiceType.class);
        if (pool != null) {
            for (DiceType d : pool) {
                map.merge(d, 1, Integer::sum);
            }
        }
        return new DicePoolCounts(map);
    }
    
    public int getCount(DiceType type) {
        return counts.getOrDefault(type, 0);
    }
}