package data.scripts.cosmicon.prismatic;

import data.scripts.cosmicon.prismatic.AvailabilityCondition.ConditionContext;
import data.scripts.cosmicon.prismatic.conditions.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PrismaticDiceType {
    
    private final String id;
    private final int[] defaultFaces;
    private final int[] trueFaces;
    private final Set<Integer> specialFaceIndices;
    private final PrismaticEffect effect;
    private final AvailabilityCondition condition;

    private PrismaticDiceType(String id, String nameKey, int[] defaultFaces, int[] trueFaces,
                              Set<Integer> specialIndices, PrismaticEffect effect,
                              AvailabilityCondition condition, String source) {
        this.id = id;
        this.defaultFaces = defaultFaces.clone();
        this.trueFaces = trueFaces != null ? trueFaces.clone() : null;
        this.specialFaceIndices = specialIndices;
        this.effect = effect;
        this.condition = condition;
        boolean hasTrueVersion = trueFaces != null && !Arrays.equals(defaultFaces, trueFaces);
    }
    
    public static PrismaticDiceType create(String id, String nameKey, int[] faces,
                                           PrismaticEffect effect, AvailabilityCondition condition,
                                           String source) {
        return new PrismaticDiceType(id, nameKey, faces, null, new HashSet<>(), effect, condition, source);
    }
    
    public static PrismaticDiceType createWithSpecialFaces(String id, String nameKey,
                                                           int[] faces, Set<Integer> specialIndices,
                                                           PrismaticEffect effect,
                                                           AvailabilityCondition condition,
                                                           String source) {
        return new PrismaticDiceType(id, nameKey, faces, null, specialIndices, effect, condition, source);
    }
    
    public static PrismaticDiceType createWithVersions(String id, String nameKey,
                                                       int[] defaultFaces, int[] trueFaces,
                                                       Set<Integer> specialIndices,
                                                       PrismaticEffect effect,
                                                       AvailabilityCondition condition,
                                                       String source) {
        return new PrismaticDiceType(id, nameKey, defaultFaces, trueFaces, specialIndices, effect, condition, source);
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
    
    public boolean isSpecialFace(int index) {
        return specialFaceIndices.contains(index);
    }
    
    public PrismaticEffect getEffect() {
        return effect;
    }
    
    public boolean isAvailable(ConditionContext context) {
        return condition.isAvailable(context);
    }
    
    public int getMaxFace(boolean useTrueVersion) {
        int[] faces = getFaces(useTrueVersion);
        int max = 0;
        for (int face : faces) {
            if (face > max) max = face;
        }
        return max;
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