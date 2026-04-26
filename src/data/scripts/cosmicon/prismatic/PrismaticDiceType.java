package data.scripts.cosmicon.prismatic;

import data.scripts.cosmicon.prismatic.AvailabilityCondition.ConditionContext;
import java.util.HashSet;
import java.util.Set;

public class PrismaticDiceType {
    
    private final String id;
    private final int[] defaultFaces;
    private final int[] trueFaces;
    private final Set<Integer> defaultSpecialIndices;
    private final Set<Integer> trueSpecialIndices;
    private final PrismaticEffect effect;
    private final AvailabilityCondition condition;

    private PrismaticDiceType(String id, int[] defaultFaces, int[] trueFaces,
                              Set<Integer> defaultSpecialIndices, Set<Integer> trueSpecialIndices,
                              PrismaticEffect effect, AvailabilityCondition condition) {
        this.id = id;
        this.defaultFaces = defaultFaces.clone();
        this.trueFaces = trueFaces != null ? trueFaces.clone() : null;
        this.defaultSpecialIndices = defaultSpecialIndices;
        this.trueSpecialIndices = trueSpecialIndices != null ? trueSpecialIndices : defaultSpecialIndices;
        this.effect = effect;
        this.condition = condition;
    }
    
    public static PrismaticDiceType create(String id, int[] faces,
                                           PrismaticEffect effect, AvailabilityCondition condition) {
        return new PrismaticDiceType(id, faces, null, new HashSet<>(), null, effect, condition);
    }
    
    public static PrismaticDiceType createWithSpecialFaces(String id,
                                                           int[] faces, Set<Integer> specialIndices,
                                                           PrismaticEffect effect,
                                                           AvailabilityCondition condition) {
        return new PrismaticDiceType(id, faces, null, specialIndices, null, effect, condition);
    }
    
    public static PrismaticDiceType createWithVersions(String id,
                                                       int[] defaultFaces, int[] trueFaces,
                                                       Set<Integer> specialIndices,
                                                       PrismaticEffect effect,
                                                       AvailabilityCondition condition) {
        return new PrismaticDiceType(id, defaultFaces, trueFaces, specialIndices, null, effect, condition);
    }
    
    public static PrismaticDiceType createWithVersionsAndSpecialFaces(String id,
                                                                       int[] defaultFaces, int[] trueFaces,
                                                                       Set<Integer> defaultSpecialIndices,
                                                                       Set<Integer> trueSpecialIndices,
                                                                       PrismaticEffect effect,
                                                                       AvailabilityCondition condition) {
        return new PrismaticDiceType(id, defaultFaces, trueFaces, defaultSpecialIndices, trueSpecialIndices, effect, condition);
    }
    
    public String getId() {
        return id;
    }
    
    public int[] getFaces(boolean useTrueVersion) {
        if (useTrueVersion && trueFaces != null) {
            return trueFaces.clone();
        }
        return defaultFaces.clone();
    }
    
    public boolean isSpecialFace(int index, boolean useTrueVersion) {
        Set<Integer> indices = useTrueVersion && trueSpecialIndices != null ? trueSpecialIndices : defaultSpecialIndices;
        return indices.contains(index);
    }
    
    public PrismaticEffect getEffect() {
        return effect;
    }
    
    public boolean isAvailable(ConditionContext context) {
        return condition.isAvailable(context);
    }
    
    public int roll(boolean useTrueVersion, java.util.Random random) {
        int[] faces = getFaces(useTrueVersion);
        int index = random.nextInt(faces.length);
        return faces[index];
    }
    
    @Override
    public String toString() {
        return "PrismaticDiceType[" + id + "]";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PrismaticDiceType other = (PrismaticDiceType) obj;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}