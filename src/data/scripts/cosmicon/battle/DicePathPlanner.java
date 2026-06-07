package data.scripts.cosmicon.battle;

import java.util.*;

public class DicePathPlanner {

    private static final Random rand = new Random();
    private static final float COLLISION_BUFFER = 15f;
    private static final float DICE_SIZE = AnimationConstants.DICE_SIZE;
    private static final float MIN_COLLISION_DIST_SQ =
        (AnimationConstants.DICE_SIZE + COLLISION_BUFFER) * (AnimationConstants.DICE_SIZE + COLLISION_BUFFER);
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

    private record AngleResult(float angle, int fractionIndex) {}

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
        for (int cpIdx = 0; cpIdx < CHECKPOINT_COUNT; cpIdx++) {
            float[] newCp = newCheckpoints[cpIdx];
            for (float[][] existing : existingCheckpoints) {
                float[] exCp = existing[cpIdx];
                float dx = newCp[0] - exCp[0];
                float dy = newCp[1] - exCp[1];
                if (dx * dx + dy * dy < MIN_COLLISION_DIST_SQ) {
                    return cpIdx;
                }
            }
        }
        return -1;
    }

    private static AngleResult getApproachAngleToTarget(Set<Integer> ruledOut, float targetRotation) {
        List<Integer> available = new ArrayList<>();
        for (int f = 0; f < MAX_FRACTIONS; f++) {
            if (!ruledOut.contains(f)) {
                available.add(f);
            }
        }

        if (available.isEmpty()) {
            return new AngleResult(-1000f, -1);
        }

        available.sort(Comparator.comparingInt(a -> Math.abs(a - 4)));

        int chosenFraction = available.get(0);

        float fractionOffset = (chosenFraction - 4) * 45f + rand.nextFloat() * 45f - 22.5f;

        return new AngleResult(targetRotation + fractionOffset, chosenFraction);
    }

    private static float[] shiftStartPosition(float startX, float startY, float panelW, float panelH) {
        float offsetX = (rand.nextFloat() - 0.5f) * 2f * REPOSITION_OFFSET;
        float offsetY = (rand.nextFloat() - 0.5f) * 2f * REPOSITION_OFFSET;
        float newX = startX + offsetX;
        float newY = startY + offsetY;
        return clampToBounds(newX, newY, panelW, panelH);
    }

    public static float[][] planGridTargets(int count, float centerX, float centerY, float spacing) {
        float[][] targets = new float[count][2];
        float startX = centerX - (count - 1) * spacing / 2f;
        float y = centerY - DICE_SIZE / 2f;
        for (int i = 0; i < count; i++) {
            targets[i][0] = startX + i * spacing;
            targets[i][1] = y;
        }
        return targets;
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

                AngleResult ar = getApproachAngleToTarget(ruledOutFractions, targetRotation);
                if (ar.angle() < -1000f) {
                    break;
                }

                float travelDistance = targetDistance * (0.9f + rand.nextFloat() * 0.2f);

                float endX = currentStartX + (float)Math.cos(Math.toRadians(ar.angle())) * travelDistance;
                float endY = currentStartY + (float)Math.sin(Math.toRadians(ar.angle())) * travelDistance;

                if (!isWithinPanelBounds(endX, endY, panelW, panelH)) {
                    ruledOutFractions.add(ar.fractionIndex());
                    continue;
                }

                List<float[]> existingEndpoints = new ArrayList<>();
                for (float[][] cp : existingCheckpoints) {
                    existingEndpoints.add(cp[CHECKPOINT_COUNT - 1]);
                }
                if (!isClearFromEndpoints(endX, endY, existingEndpoints)) {
                    ruledOutFractions.add(ar.fractionIndex());
                    continue;
                }

                float[][] checkpoints = calculateCheckpoints(currentStartX, currentStartY, endX, endY);
                int collisionCp = findCollisionCheckpoint(checkpoints, existingCheckpoints);
                if (collisionCp >= 0) {
                    ruledOutFractions.add(ar.fractionIndex());
                    continue;
                }

                bestRotation = ar.angle();
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

            dx = targetCenterX - currentStartX;
            dy = targetCenterY - currentStartY;
            targetRotation = (float)Math.toDegrees(Math.atan2(dy, dx));
            targetDistance = (float)Math.sqrt(dx * dx + dy * dy);
        }

        if (!foundValid) {
            bestRotation = (float)Math.toDegrees(Math.atan2(dy, dx));
            bestTravelDistance = (float)Math.sqrt(dx * dx + dy * dy);

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
            bounceHeights[j] = (1.25f + rand.nextFloat() * 0.25f) * factor;
            bounceHeights[j] = Math.max(1.25f, bounceHeights[j]);
        }

        return PlannedPath.create(bestRotation, bestTravelDistance, bounceCount, bounceHeights,
                                  currentStartX, currentStartY, baseDelay, targetCenterX, targetCenterY);
    }

    private static boolean isClearFromEndpoints(float x, float y, List<float[]> endpoints) {
        for (float[] endpoint : endpoints) {
            float dx = x - endpoint[0];
            float dy = y - endpoint[1];
            if (dx * dx + dy * dy < MIN_COLLISION_DIST_SQ) {
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
