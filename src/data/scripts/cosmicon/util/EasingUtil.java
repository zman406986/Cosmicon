package data.scripts.cosmicon.util;

public class EasingUtil {
    
    private EasingUtil() {
    }
    
    public static float easeOutQuad(float t) {
        return 1f - (1f - t) * (1f - t);
    }
    
    public static float easeInQuad(float t) {
        return t * t;
    }
    
    public static float easeInOutQuad(float t) {
        return t < 0.5f ? 2f * t * t : 1f - (1f - t) * (1f - t) * 2f;
    }
    
    public static float easeOutCubic(float t) {
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }
}