package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class DicePathPlanner {

    private static final Random rand = new Random();
    private static final float COLLISION_BUFFER = 15f;
    private static final float DICE_SIZE = AnimationConstants.DICE_SIZE;
    private static final int SCATTER_MAX_RETRIES = 8;
    private static final int SCATTER_FULL_RESET_LIMIT = 4;
    private static final float SCATTER_MARGIN = AnimationConstants.SCATTER_PANEL_EDGE_MARGIN;
    
    private static final int CHECKPOINT_COUNT = 4;
    private static final float[] CHECKPOINT_RATIOS = {0.25f, 0.50f, 0.75f, 1.00f};
    private static final int MAX_FRACTIONS = 8;
    private static final int MAX_REPOSITION_ATTEMPTS = 4;
    private static final float REPOSITION_OFFSET = 30f;

    public record PlannedPath(float rotation, float travelDistance, int bounceCount, float[] bounceHeights,
                              float startX, float startY, float delay, float targetCenterX, float targetCenterY,
                              float actualLandingX, float actualLandingY)
    {
        public static PlannedPath create(float rotation, float travelDistance, int bounceCount, float[] bounceHeights,
                                         float startX, float startY, float delay, float targetCenterX, float targetCenterY) {
            float endX = startX + (float)Math.cos(Math.toRadians(rotation)) * travelDistance;
            float endY = startY + (float)Math.sin(Math.toRadians(rotation)) * travelDistance;
            return new PlannedPath(rotation, travelDistance, bounceCount, bounceHeights, 
                                   startX, startY, delay, targetCenterX, targetCenterY, endX, endY);
        }
    }
    
    private static float[][] calculateCheckpoints(float startX, float startY, float endX, float endY) {
        float[][] checkpoints = new float[CHECKPOINT_COUNT][2];
        for (int i = 0; i < CHECKPOINT_COUNT; i++) {
            float ratio = CHECKPOINT_RATIOS[i];
            checkpoints[i][0] = startX + (endX - startX) * ratio;
            checkpoints[i][1] = startY + (endY - startY) * ratio;
        }
        return checkpoints;
    }
    
    private static int findCollisionCheckpoint(float[][] newCheckpoints, List<float[][]> existingCheckpoints) {
        float minDist = DICE_SIZE + COLLISION_BUFFER;
        for (int cpIdx = 0; cpIdx < CHECKPOINT_COUNT; cpIdx++) {
            float[] newCp = newCheckpoints[cpIdx];
            for (float[][] existing : existingCheckpoints) {
                for (int exCpIdx = 0; exCpIdx < CHECKPOINT_COUNT; exCpIdx++) {
                    float[] exCp = existing[exCpIdx];
                    float dx = Math.abs(newCp[0] - exCp[0]);
                    float dy = Math.abs(newCp[1] - exCp[1]);
                    if (dx < minDist && dy < minDist) {
                        return cpIdx;
                    }
                }
            }
        }
        return -1;
    }
    
    private static int getFractionIndex(float angleDegrees) {
        float normalized = ((angleDegrees % 360f) + 360f) % 360f;
        return (int)(normalized / 45f);
    }
    
    // Get approach angle toward target (fractions around target direction)
    private static float getApproachAngleToTarget(Set<Integer> ruledOut, float targetRotation) {
        List<Integer> available = new ArrayList<>();
        for (int f = 0; f < MAX_FRACTIONS; f++) {
            if (!ruledOut.contains(f)) {
                available.add(f);
            }
        }
        
        if (available.isEmpty()) {
            return -1000f;  // Invalid marker
        }
        
        int chosenFraction = available.get(rand.nextInt(available.size()));
        
        // Fraction offset from target direction (-180 to +180 degrees)
        float fractionOffset = (chosenFraction - 4) * 45f + rand.nextFloat() * 45f - 22.5f;
        
        return targetRotation + fractionOffset;
    }
    
    private static float[] shiftStartPosition(float startX, float startY, float panelW, float panelH) {
        float offsetX = (rand.nextFloat() - 0.5f) * 2f * REPOSITION_OFFSET;
        float offsetY = (rand.nextFloat() - 0.5f) * 2f * REPOSITION_OFFSET;
        float newX = startX + offsetX;
        float newY = startY + offsetY;
        return clampToBounds(newX, newY, panelW, panelH);
    }
    
    public static List<PlannedPath> planPaths(List<DiceType> types, List<Integer> results,
                                               float centerX, float centerY, float spacing,
                                               float panelW, float panelH) {
        int count = Math.min(types.size(), results.size());
        List<PlannedPath> paths = new ArrayList<>(count);
        
        float totalWidth = spacing * (count - 1) + DICE_SIZE;
        float startX = centerX - totalWidth / 2f;
        float startY = centerY - DICE_SIZE / 2f;
        
        float targetStartX = centerX - (count - 1) * spacing / 2f - DICE_SIZE / 2f;
        
        List<float[][]> plannedCheckpoints = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            float diceX = startX + i * spacing;
            float delay = i * 0.05f;
            
            float targetX = targetStartX + i * spacing;
            float targetY = centerY - DICE_SIZE / 2f;
            
            PlannedPath path = planSingleDice(diceX, startY, delay, plannedCheckpoints, 
                                              targetX, targetY, panelW, panelH);
            paths.add(path);
            
            float[][] checkpoints = calculateCheckpoints(path.startX(), path.startY(), 
                                                         path.actualLandingX(), path.actualLandingY());
            plannedCheckpoints.add(checkpoints);
        }
        
        return paths;
    }
    
    private static PlannedPath planSingleDice(float startX, float startY, 
                                               float baseDelay, List<float[][]> existingCheckpoints, 
                                               float targetCenterX, float targetCenterY,
                                               float panelW, float panelH) {
        float[] clamped = clampToBounds(startX, startY, panelW, panelH);
        float currentStartX = clamped[0];
        float currentStartY = clamped[1];
        
        float dx = targetCenterX - currentStartX;
        float dy = targetCenterY - currentStartY;
        float targetRotation = (float)Math.toDegrees(Math.atan2(dy, dx));
        float targetDistance = (float)Math.sqrt(dx * dx + dy * dy);
        
        Set<Integer> ruledOutFractions = new HashSet<>();
        
        float bestRotation = targetRotation;
        float bestTravelDistance = targetDistance;
        boolean foundValid = false;
        
        for (int repositionAttempt = 0; repositionAttempt < MAX_REPOSITION_ATTEMPTS; repositionAttempt++) {
            for (int fractionAttempt = 0; fractionAttempt < MAX_FRACTIONS; fractionAttempt++) {
                if (ruledOutFractions.size() >= MAX_FRACTIONS) {
                    break;
                }
                
                // Get approach angle toward target (fractions around target direction)
                float rotation = getApproachAngleToTarget(ruledOutFractions, targetRotation);
                if (rotation < -1000f) {  // Invalid marker
                    break;
                }
                
                // Travel distance toward target (slightly randomized for variety)
                float travelDistance = targetDistance * (0.9f + rand.nextFloat() * 0.2f);
                
                float endX = currentStartX + (float)Math.cos(Math.toRadians(rotation)) * travelDistance;
                float endY = currentStartY + (float)Math.sin(Math.toRadians(rotation)) * travelDistance;
                
                int fractionIdx = getFractionIndex(rotation - targetRotation + 180f);  // Offset relative to target
                
                if (!isWithinPanelBounds(endX, endY, panelW, panelH)) {
                    ruledOutFractions.add(fractionIdx);
                    continue;
                }
                
                List<float[]> existingEndpoints = new ArrayList<>();
                for (float[][] cp : existingCheckpoints) {
                    existingEndpoints.add(cp[CHECKPOINT_COUNT - 1]);
                }
                if (!isClearFromEndpoints(endX, endY, existingEndpoints)) {
                    ruledOutFractions.add(fractionIdx);
                    continue;
                }
                
                float[][] checkpoints = calculateCheckpoints(currentStartX, currentStartY, endX, endY);
                int collisionCp = findCollisionCheckpoint(checkpoints, existingCheckpoints);
                if (collisionCp >= 0) {
                    ruledOutFractions.add(fractionIdx);
                    continue;
                }
                
                bestRotation = rotation;
                bestTravelDistance = travelDistance;
                foundValid = true;
                break;
            }
            
            if (foundValid) {
                break;
            }
            
            float[] shifted = shiftStartPosition(currentStartX, currentStartY, panelW, panelH);
            currentStartX = shifted[0];
            currentStartY = shifted[1];
            ruledOutFractions.clear();
        }
        
        if (!foundValid) {
            float dx2 = targetCenterX - currentStartX;
            float dy2 = targetCenterY - currentStartY;
            bestRotation = (float)Math.toDegrees(Math.atan2(dy2, dx2));
            bestTravelDistance = (float)Math.sqrt(dx2 * dx2 + dy2 * dy2);
            
            float rawEndX = currentStartX + (float)Math.cos(Math.toRadians(bestRotation)) * bestTravelDistance;
            float rawEndY = currentStartY + (float)Math.sin(Math.toRadians(bestRotation)) * bestTravelDistance;
            float[] clampedEndpoint = clampToBounds(rawEndX, rawEndY, panelW, panelH);
            
            float cdx = clampedEndpoint[0] - currentStartX;
            float cdy = clampedEndpoint[1] - currentStartY;
            bestRotation = (float)Math.toDegrees(Math.atan2(cdy, cdx));
            bestTravelDistance = (float)Math.sqrt(cdx * cdx + cdy * cdy);
        }
        
        int bounceCount = rand.nextInt(3);
        float[] bounceHeights = new float[bounceCount];
        for (int j = 0; j < bounceCount; j++) {
            float factor = 1f - (j * 0.2f);
            bounceHeights[j] = (1.15f + rand.nextFloat() * 0.25f) * factor;
            bounceHeights[j] = Math.max(1.15f, bounceHeights[j]);
        }
        
        return PlannedPath.create(bestRotation, bestTravelDistance, bounceCount, bounceHeights,
                                  currentStartX, currentStartY, baseDelay, targetCenterX, targetCenterY);
    }
    
    private static boolean isClearFromEndpoints(float x, float y, List<float[]> endpoints) {
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
                    
                    if (isClearFromEndpoints(sx, sy, placedDestinations)) {
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
        
        float spacing = (panelW - 2f * SCATTER_MARGIN) / count;
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
        List<float[][]> plannedCheckpoints = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            float startX = startPositions[i][0];
            float startY = startPositions[i][1];
            float delay = i * 0.05f;
            
            PlannedPath path = planSingleDice(startX, startY, delay, plannedCheckpoints,
                targetCenterXs[i], targetCenterYs[i], panelW, panelH);
            paths.add(path);
            
            float[][] checkpoints = calculateCheckpoints(path.startX(), path.startY(),
                                                         path.actualLandingX(), path.actualLandingY());
            plannedCheckpoints.add(checkpoints);
        }
        return paths;
    }
    
    public static List<PlannedPath> planRerollPaths(List<Integer> indices,
                                                     List<float[]> allDicePositions,
                                                     float panelW, float panelH) {
        List<Integer> sortedIndices = new ArrayList<>(indices);
        Collections.sort(sortedIndices);
        
        List<PlannedPath> paths = new ArrayList<>();
        List<float[][]> plannedCheckpoints = new ArrayList<>();

        for (int diceIndex : sortedIndices) {
            if (diceIndex >= allDicePositions.size()) {
                paths.add(null);
                continue;
            }

            float[] startPos = allDicePositions.get(diceIndex);
            if (startPos == null || startPos.length < 2) {
                throw new IllegalArgumentException("allDicePositions[" + diceIndex + "] is invalid");
            }
            float startX = startPos[0];
            float startY = startPos[1];

            List<float[][]> otherCheckpoints = new ArrayList<>();
            for (int j = 0; j < allDicePositions.size(); j++) {
                if (!sortedIndices.contains(j) && j != diceIndex) {
                    float[] pos = allDicePositions.get(j);
                    float[][] cp = calculateCheckpoints(pos[0], pos[1], pos[0], pos[1]);
                    otherCheckpoints.add(cp);
                }
            }
            otherCheckpoints.addAll(plannedCheckpoints);

float angle = rand.nextFloat() * 360f;
            float dist = 150f + rand.nextFloat() * 200f;
            float targetX = startX + (float)Math.cos(Math.toRadians(angle)) * dist;
            float targetY = startY + (float)Math.sin(Math.toRadians(angle)) * dist;
            targetX = Math.max(SCATTER_MARGIN, Math.min(panelW - SCATTER_MARGIN, targetX));
            targetY = Math.max(SCATTER_MARGIN, Math.min(panelH - SCATTER_MARGIN, targetY));
            
            PlannedPath path = planSingleDice(startX, startY, 0f, otherCheckpoints, 
                                               targetX, targetY, panelW, panelH);
            paths.add(path);

            float[][] checkpoints = calculateCheckpoints(path.startX(), path.startY(),
                                                         path.actualLandingX(), path.actualLandingY());
            plannedCheckpoints.add(checkpoints);
        }
        
        return paths;
    }
    
    public static PlannedPath planSinglePrismaticPath(int diceIndex, float centerX, float centerY,
                                                        float spacing, List<float[]> existingPositions,
                                                        float panelW, float panelH) {
        for (float[] pos : existingPositions) {
            if (pos == null || pos.length < 2) {
                throw new IllegalArgumentException("existingPositions contains invalid position data");
            }
        }
        
        float startY = centerY - DICE_SIZE / 2f;
        float startX = centerX - DICE_SIZE / 2f;
        
        float targetX = centerX - DICE_SIZE / 2f;
        float targetY = centerY - DICE_SIZE / 2f;
        
        if (!existingPositions.isEmpty()) {
            float maxX = 0f;
            for (float[] pos : existingPositions) {
                if (pos[0] > maxX) maxX = pos[0];
            }
            targetX = Math.min(maxX + spacing, panelW - SCATTER_MARGIN);
        }
        
        float delay = diceIndex * 0.05f;
        
        List<float[][]> existingCheckpoints = new ArrayList<>();
        for (float[] pos : existingPositions) {
            float[][] cp = calculateCheckpoints(pos[0], pos[1], pos[0], pos[1]);
            existingCheckpoints.add(cp);
        }
        
        return planSingleDice(startX, startY, delay, existingCheckpoints, 
                              targetX, targetY, panelW, panelH);
    }
    
    private static float[] clampToBounds(float x, float y, float panelW, float panelH) {
        float clampedX = Math.max(SCATTER_MARGIN, Math.min(panelW - SCATTER_MARGIN, x));
        float clampedY = Math.max(SCATTER_MARGIN, Math.min(panelH - SCATTER_MARGIN, y));
        return new float[]{clampedX, clampedY};
    }
}