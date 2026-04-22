package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DicePathPlanner {

    private static final Random rand = new Random();
    private static final float COLLISION_BUFFER = 15f;
    private static final float DICE_SIZE = DiceAnimator.DICE_SIZE;
    private static final float MIN_TRAVEL_DISTANCE = 50f;
    private static final float MAX_TRAVEL_DISTANCE = 500f;
    private static final int MAX_RETRIES = 8;
    private static final float TIME_STEP = 0.05f;
    
    public static class PlannedPath {
        public final float rotation;
        public final float travelDistance;
        public final int bounceCount;
        public final float[] bounceHeights;
        public final float startX;
        public final float startY;
        public final float delay;
        public final float targetCenterX;
        public final float targetCenterY;
        
        public PlannedPath(float rotation, float travelDistance, int bounceCount,
                          float[] bounceHeights, float startX, float startY, float delay,
                          float targetCenterX, float targetCenterY) {
            this.rotation = rotation;
            this.travelDistance = travelDistance;
            this.bounceCount = bounceCount;
            this.bounceHeights = bounceHeights;
            this.startX = startX;
            this.startY = startY;
            this.delay = delay;
            this.targetCenterX = targetCenterX;
            this.targetCenterY = targetCenterY;
        }
    }
    
    public static List<PlannedPath> planPaths(List<DiceType> types, List<Integer> results,
                                               float centerX, float centerY, float spacing) {
        int count = Math.min(types.size(), results.size());
        List<PlannedPath> paths = new ArrayList<>(count);
        
        float totalWidth = spacing * (count - 1) + DICE_SIZE;
        float startX = centerX - totalWidth / 2f;
        float startY = centerY - DICE_SIZE / 2f;
        
        float targetStartX = centerX - (count - 1) * spacing / 2f - DICE_SIZE / 2f;
        
        List<float[]> plannedEndpoints = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            float diceX = startX + i * spacing;
            float delay = i * 0.05f;
            
            float targetX = targetStartX + i * spacing;
            float targetY = centerY - DICE_SIZE / 2f;
            
            PlannedPath path = planSingleDice(i, diceX, startY, delay, plannedEndpoints, count, targetX, targetY);
            paths.add(path);
            
            float endX = diceX + (float)Math.cos(Math.toRadians(path.rotation)) * path.travelDistance;
            float endY = startY + (float)Math.sin(Math.toRadians(path.rotation)) * path.travelDistance;
            plannedEndpoints.add(new float[]{endX, endY});
        }
        
        return paths;
    }
    
    private static PlannedPath planSingleDice(int diceIndex, float startX, float startY, 
                                               float baseDelay, List<float[]> plannedEndpoints, 
                                               int totalDice, float targetCenterX, float targetCenterY) {
        float bestRotation = rand.nextFloat() * 360f;
        float bestTravelDistance = MIN_TRAVEL_DISTANCE + rand.nextFloat() * (MAX_TRAVEL_DISTANCE - MIN_TRAVEL_DISTANCE);
        boolean foundValid = false;
        
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            float rotation = rand.nextFloat() * 360f;
            float travelDistance = MIN_TRAVEL_DISTANCE + rand.nextFloat() * (MAX_TRAVEL_DISTANCE - MIN_TRAVEL_DISTANCE);
            
            float endX = startX + (float)Math.cos(Math.toRadians(rotation)) * travelDistance;
            float endY = startY + (float)Math.sin(Math.toRadians(rotation)) * travelDistance;
            
            if (!collidesWithPlannedPaths(endX, endY, plannedEndpoints, COLLISION_BUFFER)) {
                bestRotation = rotation;
                bestTravelDistance = travelDistance;
                foundValid = true;
                break;
            }
        }
        
        float adjustedDelay = baseDelay;
        if (!foundValid && !plannedEndpoints.isEmpty()) {
            float travelDuration = calculateTravelDuration(bestTravelDistance);
            float[] timing = findCollisionFreeTiming(diceIndex, startX, startY, 
                    bestRotation, bestTravelDistance, travelDuration, plannedEndpoints, totalDice);
            adjustedDelay = timing[0];
            bestTravelDistance = timing[1] > 0 ? timing[1] : bestTravelDistance;
        }
        
        int bounceCount = rand.nextInt(3);
        float[] bounceHeights = new float[bounceCount];
        for (int j = 0; j < bounceCount; j++) {
            float factor = 1f - (j * 0.2f);
            bounceHeights[j] = (1.1f + rand.nextFloat() * 0.2f) * factor;
            bounceHeights[j] = Math.max(1.05f, bounceHeights[j]);
        }
        
        return new PlannedPath(bestRotation, bestTravelDistance, bounceCount, bounceHeights, 
                               startX, startY, adjustedDelay, targetCenterX, targetCenterY);
    }
    
    private static boolean collidesWithPlannedPaths(float x, float y, List<float[]> endpoints, float buffer) {
        for (float[] endpoint : endpoints) {
            float dx = Math.abs(x - endpoint[0]);
            float dy = Math.abs(y - endpoint[1]);
            float minDist = DICE_SIZE + buffer;
            if (dx < minDist && dy < minDist) {
                return true;
            }
        }
        return false;
    }
    
    private static float[] findCollisionFreeTiming(int diceIndex, float startX, float startY,
                                                    float rotation, float travelDistance, 
                                                    float travelDuration, List<float[]> plannedEndpoints,
                                                    int totalDice) {
        float baseDelay = diceIndex * 0.05f;
        float adjustedDelay = baseDelay;
        float adjustedDistance = travelDistance;
        
        float endX = startX + (float)Math.cos(Math.toRadians(rotation)) * travelDistance;
        float endY = startY + (float)Math.sin(Math.toRadians(rotation)) * travelDistance;
        
        for (float delayOffset = 0f; delayOffset <= 0.3f; delayOffset += 0.1f) {
            adjustedDelay = baseDelay + delayOffset;
            if (adjustedDelay > totalDice * 0.05f + 0.5f) break;
            
            if (!wouldCollideDuringTravel(startX, startY, rotation, travelDistance, 
                                          adjustedDelay, travelDuration, plannedEndpoints)) {
                return new float[]{adjustedDelay, travelDistance};
            }
        }
        
        float[] distances = {travelDistance * 0.5f, travelDistance * 0.7f, travelDistance * 1.3f, 
                            MIN_TRAVEL_DISTANCE, MAX_TRAVEL_DISTANCE * 0.3f};
        for (float dist : distances) {
            dist = Math.max(MIN_TRAVEL_DISTANCE, Math.min(MAX_TRAVEL_DISTANCE, dist));
            if (!collidesWithPlannedPaths(
                    startX + (float)Math.cos(Math.toRadians(rotation)) * dist,
                    startY + (float)Math.sin(Math.toRadians(rotation)) * dist,
                    plannedEndpoints, COLLISION_BUFFER)) {
                return new float[]{baseDelay, dist};
            }
        }
        
        return new float[]{baseDelay + 0.15f, travelDistance * 0.6f};
    }
    
    private static boolean wouldCollideDuringTravel(float startX, float startY, float rotation,
                                                      float travelDistance, float delay, 
                                                      float travelDuration, List<float[]> plannedEndpoints) {
        int steps = Math.max(5, (int)(travelDuration / TIME_STEP));
        
        for (int step = 0; step <= steps; step++) {
            float progress = (float)step / steps;
            float currentX = startX + (float)Math.cos(Math.toRadians(rotation)) * travelDistance * progress;
            float currentY = startY + (float)Math.sin(Math.toRadians(rotation)) * travelDistance * progress;
            
            for (float[] endpoint : plannedEndpoints) {
                float dx = Math.abs(currentX - endpoint[0]);
                float dy = Math.abs(currentY - endpoint[1]);
                float minDist = DICE_SIZE * 0.8f + COLLISION_BUFFER;
                if (dx < minDist && dy < minDist) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private static float calculateTravelDuration(float travelDistance) {
        float normalized = (travelDistance - MIN_TRAVEL_DISTANCE) / (MAX_TRAVEL_DISTANCE - MIN_TRAVEL_DISTANCE);
        return 1f + Math.round(normalized * 2f);
    }
    
    public static float getMinTravelDistance() {
        return MIN_TRAVEL_DISTANCE;
    }
    
    public static float getMaxTravelDistance() {
        return MAX_TRAVEL_DISTANCE;
    }
    
    public static List<PlannedPath> planPathsAppend(List<DiceType> types, List<Integer> results,
                                                     float centerX, float centerY, float spacing,
                                                     int existingCount, List<PlannedPath> existingPaths) {
        int count = Math.min(types.size(), results.size());
        List<PlannedPath> paths = new ArrayList<>(count);
        
        float totalWidth = spacing * (count - 1) + DICE_SIZE;
        float startX = centerX - totalWidth / 2f;
        float startY = centerY - DICE_SIZE / 2f;
        
        float targetStartX = centerX - (existingCount + count - 1) * spacing / 2f - DICE_SIZE / 2f;
        
        List<float[]> plannedEndpoints = new ArrayList<>();
        for (PlannedPath existing : existingPaths) {
            plannedEndpoints.add(new float[]{existing.startX, existing.startY});
        }
        
        for (int i = 0; i < count; i++) {
            float diceX = startX + i * spacing;
            float delay = (existingCount + i) * 0.05f;
            
            float targetX = targetStartX + (existingCount + i) * spacing;
            float targetY = centerY - DICE_SIZE / 2f;
            
            PlannedPath path = planSingleDice(existingCount + i, diceX, startY, delay, plannedEndpoints, 
                                              existingCount + count, targetX, targetY);
            paths.add(path);
            
            float endX = diceX + (float)Math.cos(Math.toRadians(path.rotation)) * path.travelDistance;
            float endY = startY + (float)Math.sin(Math.toRadians(path.rotation)) * path.travelDistance;
            plannedEndpoints.add(new float[]{endX, endY});
        }
        
        return paths;
    }
    
    public static List<PlannedPath> planRerollPaths(List<Integer> indices, List<Integer> results,
                                                     List<float[]> allDicePositions) {
        List<PlannedPath> paths = new ArrayList<>();
        
        for (int i = 0; i < indices.size(); i++) {
            int diceIndex = indices.get(i);
            if (diceIndex >= allDicePositions.size()) continue;
            
            float[] startPos = allDicePositions.get(diceIndex);
            float startX = startPos[0];
            float startY = startPos[1];
            
            float targetX = startX;
            float targetY = startY;
            
            List<float[]> otherPositions = new ArrayList<>();
            for (int j = 0; j < allDicePositions.size(); j++) {
                if (j != diceIndex) {
                    otherPositions.add(allDicePositions.get(j));
                }
            }
            
            PlannedPath path = planSingleDice(diceIndex, startX, startY, 0f, otherPositions, 
                                              allDicePositions.size(), targetX, targetY);
            paths.add(path);
        }
        
        return paths;
    }
    
    public static PlannedPath planSinglePrismaticPath(int diceIndex, float centerX, float centerY,
                                                        float spacing, List<float[]> existingPositions) {
        float startY = centerY - DICE_SIZE / 2f;
        float startX = centerX - DICE_SIZE / 2f;
        
        float targetX = centerX - DICE_SIZE / 2f;
        float targetY = centerY - DICE_SIZE / 2f;
        
        if (!existingPositions.isEmpty()) {
            float maxX = 0f;
            for (float[] pos : existingPositions) {
                if (pos[0] > maxX) maxX = pos[0];
            }
            targetX = maxX + spacing;
        }
        
        float delay = diceIndex * 0.05f;
        
        PlannedPath path = planSingleDice(diceIndex, startX, startY, delay, existingPositions,
                                          diceIndex + 1, targetX, targetY);
        
        return path;
    }
}