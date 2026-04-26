package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DicePathPlanner {

    private static final Random rand = new Random();
    private static final float COLLISION_BUFFER = 15f;
    private static final float DICE_SIZE = AnimationConstants.DICE_SIZE;
    private static final float[] TRAVEL_DISTANCES = AnimationConstants.TRAVEL_DISTANCES;
    private static final int MAX_RETRIES = 16;
    private static final int SCATTER_MAX_RETRIES = 8;
    private static final int SCATTER_FULL_RESET_LIMIT = 4;
    private static final float SCATTER_MARGIN = AnimationConstants.SCATTER_PANEL_EDGE_MARGIN;

    public record PlannedPath(float rotation, float travelDistance, int bounceCount, float[] bounceHeights,
                              float startX, float startY, float delay, float targetCenterX, float targetCenterY)
    {
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
            
            PlannedPath path = planSingleDice(diceX, startY, delay, plannedEndpoints, targetX, targetY);
            paths.add(path);
            
            float endX = diceX + (float)Math.cos(Math.toRadians(path.rotation)) * path.travelDistance;
            float endY = startY + (float)Math.sin(Math.toRadians(path.rotation)) * path.travelDistance;
            plannedEndpoints.add(new float[]{endX, endY});
        }
        
        return paths;
    }
    
    private static PlannedPath planSingleDice(float startX, float startY, 
                                               float baseDelay, List<float[]> plannedEndpoints, 
                                               float targetCenterX, float targetCenterY) {
        return planSingleDice(startX, startY, baseDelay, plannedEndpoints,
            targetCenterX, targetCenterY, Float.MAX_VALUE, Float.MAX_VALUE);
    }
    
    private static PlannedPath planSingleDice(float startX, float startY, 
                                               float baseDelay, List<float[]> plannedEndpoints, 
                                               float targetCenterX, float targetCenterY,
                                               float panelW, float panelH) {
        float bestRotation = 0f;
        float bestTravelDistance = TRAVEL_DISTANCES[0];
        
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            float rotation = rand.nextFloat() * 360f;
            float travelDistance = TRAVEL_DISTANCES[rand.nextInt(TRAVEL_DISTANCES.length)];
            
            float endX = startX + (float)Math.cos(Math.toRadians(rotation)) * travelDistance;
            float endY = startY + (float)Math.sin(Math.toRadians(rotation)) * travelDistance;
            
            if (!isWithinPanelBounds(endX, endY, panelW, panelH)) continue;
            
            if (collidesWithPlannedPaths(endX, endY, plannedEndpoints)) {
                bestRotation = rotation;
                bestTravelDistance = travelDistance;
                break;
            }
        }
        
        int bounceCount = rand.nextInt(3);
        float[] bounceHeights = new float[bounceCount];
        for (int j = 0; j < bounceCount; j++) {
            float factor = 1f - (j * 0.2f);
            bounceHeights[j] = (1.15f + rand.nextFloat() * 0.25f) * factor;
            bounceHeights[j] = Math.max(1.15f, bounceHeights[j]);
        }
        
        return new PlannedPath(bestRotation, bestTravelDistance, bounceCount, bounceHeights, 
                               startX, startY, baseDelay, targetCenterX, targetCenterY);
    }
    
    private static boolean collidesWithPlannedPaths(float x, float y, List<float[]> endpoints) {
        for (float[] endpoint : endpoints) {
            float dx = Math.abs(x - endpoint[0]);
            float dy = Math.abs(y - endpoint[1]);
            float minDist = DICE_SIZE + COLLISION_BUFFER;
            if (dx < minDist && dy < minDist) {
                return false;
            }
        }
        return true;
    }
    
    private static boolean isWithinPanelBounds(float x, float y, float panelW, float panelH) {
        return x >= SCATTER_MARGIN && x <= panelW - SCATTER_MARGIN
            && y >= SCATTER_MARGIN && y <= panelH - SCATTER_MARGIN;
    }
    
    public static float[][] planScatterDestinations(int count, float panelW, float panelH) {
        float[][] destinations = new float[count][2];
        List<float[]> placedDestinations = new ArrayList<>();
        
        for (int fullReset = 0; fullReset < SCATTER_FULL_RESET_LIMIT; fullReset++) {
            placedDestinations.clear();
            boolean allPlaced = true;
            
            for (int i = 0; i < count; i++) {
                boolean placed = false;
                for (int attempt = 0; attempt < SCATTER_MAX_RETRIES; attempt++) {
                    float sx = SCATTER_MARGIN + rand.nextFloat() * (panelW - 2f * SCATTER_MARGIN);
                    float sy = SCATTER_MARGIN + rand.nextFloat() * (panelH - 2f * SCATTER_MARGIN);
                    
                    if (collidesWithPlannedPaths(sx, sy, placedDestinations)) {
                        destinations[i][0] = sx;
                        destinations[i][1] = sy;
                        placedDestinations.add(new float[]{sx, sy});
                        placed = true;
                        break;
                    }
                }
                if (!placed) {
                    allPlaced = false;
                    break;
                }
            }
            if (allPlaced) return destinations;
        }
        
        float spacing = (panelW - 2f * SCATTER_MARGIN) / Math.max(count, 1);
        float startX = SCATTER_MARGIN + spacing / 2f;
        float y = panelH / 2f;
        for (int i = 0; i < count; i++) {
            destinations[i][0] = Math.min(startX + i * spacing, panelW - SCATTER_MARGIN);
            destinations[i][1] = y;
        }
        return destinations;
    }
    
    public static List<PlannedPath> planTravelPaths(float[][] startPositions,
            float[] targetCenterXs, float[] targetCenterYs, float panelW, float panelH) {
        int count = startPositions.length;
        List<PlannedPath> paths = new ArrayList<>(count);
        List<float[]> plannedEndpoints = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            float startX = startPositions[i][0];
            float startY = startPositions[i][1];
            float delay = i * 0.05f;
            
            PlannedPath path = planSingleDice(startX, startY, delay, plannedEndpoints,
                targetCenterXs[i], targetCenterYs[i], panelW, panelH);
            paths.add(path);
            
            float endX = startX + (float)Math.cos(Math.toRadians(path.rotation())) * path.travelDistance();
            float endY = startY + (float)Math.sin(Math.toRadians(path.rotation())) * path.travelDistance();
            plannedEndpoints.add(new float[]{endX, endY});
        }
        return paths;
    }
    
    public static List<PlannedPath> planRerollPaths(List<Integer> indices,
                                                     List<float[]> allDicePositions) {
        List<PlannedPath> paths = new ArrayList<>();
        List<float[]> plannedEndpoints = new ArrayList<>();

        for (int diceIndex : indices) {
            if (diceIndex >= allDicePositions.size()) {
                paths.add(null);
                continue;
            }

            float[] startPos = allDicePositions.get(diceIndex);
            float startX = startPos[0];
            float startY = startPos[1];

            List<float[]> otherPositions = new ArrayList<>();
            for (int j = 0; j < allDicePositions.size(); j++) {
                if (!indices.contains(j) && j != diceIndex) {
                    otherPositions.add(allDicePositions.get(j));
                }
            }
            otherPositions.addAll(plannedEndpoints);

            PlannedPath path = planSingleDice(startX, startY, 0f, otherPositions, startX, startY);
            paths.add(path);

            float endX = startX + (float)Math.cos(Math.toRadians(path.rotation())) * path.travelDistance();
            float endY = startY + (float)Math.sin(Math.toRadians(path.rotation())) * path.travelDistance();
            plannedEndpoints.add(new float[]{endX, endY});
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
        
        return planSingleDice(startX, startY, delay, existingPositions, targetX, targetY);
    }
}