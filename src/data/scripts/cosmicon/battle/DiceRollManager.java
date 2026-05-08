package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import data.scripts.cosmicon.battle.DicePathPlanner.PlannedPath;
import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.CosmiconLogger;

public class DiceRollManager {

    private static final float DICE_SPACING = 90f;

    private final DiceSide playerSide = new DiceSide();
    private final DiceSide opponentSide = new DiceSide();
    private boolean initialized;
    private BattleState battleState;

    public DiceRollManager() {
        this.initialized = false;
    }

    public void init() {
        this.initialized = true;
    }

    public void setBattleState(BattleState state) {
        this.battleState = state;
    }

    public void startStationaryPreview(List<DiceType> types, List<Integer> results, float centerX, float centerY) {
        if (!initialized) return;
        playerSide.startStationaryPreview(types, results, centerX, centerY);
    }

    public void triggerRollFromStationary() {
        playerSide.triggerRollFromStationary();
    }

    public boolean isWaitingForRollTrigger() {
        return playerSide.isWaitingForRollTrigger();
    }

    public void startOpponentStationaryPreview(List<DiceType> types, List<Integer> results, float centerX, float centerY) {
        if (!initialized) return;
        opponentSide.startStationaryPreview(types, results, centerX, centerY);
    }

    public void triggerOpponentRollFromStationary() {
        opponentSide.triggerRollFromStationary();
    }

    public boolean isOpponentWaitingForRollTrigger() {
        return opponentSide.isWaitingForRollTrigger();
    }

    public void advance(float amount) {
        playerSide.advanceAnimators(amount);
        opponentSide.advanceAnimators(amount);
        advanceRestAnimators(playerSide, amount);
        advanceRestAnimators(opponentSide, amount);
        advanceCentering(playerSide);
        advanceCentering(opponentSide);
    }

    private void advanceRestAnimators(DiceSide side, float amount) {
        if (!side.restAnimators.isEmpty()) {
            for (DiceAnimator animator : side.restAnimators) {
                animator.advance(amount);
            }
        }
    }

    private void advanceCentering(DiceSide side) {
        if (!side.animators.isEmpty()) {
            boolean anyWaiting = false;
            boolean anyStillAnimating = false;
            for (DiceAnimator animator : side.animators) {
                if (animator.isReadyForCentering()) {
                    anyWaiting = true;
                } else if (animator.isActive() && animator.isRunning()) {
                    anyStillAnimating = true;
                }
            }
            if (anyWaiting && !anyStillAnimating) {
                for (DiceAnimator animator : side.animators) {
                    animator.startCenteringAnimation();
                }
            }
        }
    }

    public void render(float panelX, float panelY, float panelWidth, float panelHeight, float alphaMult) {
        playerSide.render(panelX, panelY, panelWidth, panelHeight, alphaMult);
    }

    public boolean isComplete() {
        return playerSide.isComplete();
    }

    public boolean hasAnimators() {
        return playerSide.hasAnimators();
    }

    public void appendInstantDice(DiceType type, int value, float centerX, float centerY) {
        if (!initialized) return;
        playerSide.appendInstantDice(type, value, centerX, centerY);
    }

    public void appendOpponentInstantDice(DiceType type, int value, float centerX, float centerY) {
        if (!initialized) return;
        opponentSide.appendInstantDice(type, value, centerX, centerY);
    }

    public void clear() {
        playerSide.clear();
    }

    public void partialReroll(List<Integer> indices, List<Integer> newValues) {
        playerSide.reroll(indices, newValues, battleState, true);
    }

    public void forceCompleteAll() {
        playerSide.forceCompleteAll();
    }

    public void forceCompleteAllOpponent() {
        opponentSide.forceCompleteAll();
    }

    public void renderOpponentDice(float panelX, float panelY, float panelWidth, float panelHeight, float alphaMult) {
        opponentSide.render(panelX, panelY, panelWidth, panelHeight, alphaMult);
    }

    public void rerollOpponentDice(List<Integer> indices, List<Integer> newValues) {
        opponentSide.reroll(indices, newValues, battleState, false);
    }

    public boolean isOpponentComplete() {
        return opponentSide.isComplete();
    }

    public boolean hasOpponentAnimators() {
        return opponentSide.hasAnimators();
    }

    public void clearOpponentAnimators() {
        opponentSide.clear();
    }

    public void moveSelectedToRestGrid(boolean forPlayer,
                                         float gridCenterX, float gridCenterY) {
        if (!initialized) return;

        DiceSide side = forPlayer ? playerSide : opponentSide;
        List<Boolean> selected = battleState.getDiceSelected(forPlayer);
        List<DiceType> types = battleState.getDiceTypes(forPlayer);
        List<Integer> values = battleState.getDiceValues(forPlayer);

        if (selected == null || types == null || values == null) return;

        int totalCount = Math.min(selected.size(), side.animators.size());
        if (totalCount == 0) {
            CosmiconLogger.info("[DICE-REST] moveSelectedToRestGrid SKIPPED: forPlayer=%s selected=%d animators=%d restAnimators=%d",
                    forPlayer, selected.size(), side.animators.size(), side.restAnimators.size());
            return;
        }

        int activeCount = 0;
        int reserveCount = 0;
        for (int i = 0; i < totalCount; i++) {
            if (selected.get(i)) activeCount++;
            else reserveCount++;
        }
        if (activeCount == 0 && reserveCount == 0) return;

        float spacing = BattleRenderingUtils.REST_GRID_DICE_SPACING;
        float gap = BattleRenderingUtils.REST_GRID_GROUP_GAP;

        float diceSize = AnimationConstants.DICE_SIZE;
        float activeWidth = activeCount > 0 ? spacing * (activeCount - 1) + diceSize : 0f;
        float reserveWidth = reserveCount > 0 ? spacing * (reserveCount - 1) + diceSize : 0f;
        float combinedWidth = activeWidth + reserveWidth;
        if (activeCount > 0 && reserveCount > 0) combinedWidth += gap;

        float startX = gridCenterX - combinedWidth / 2f + diceSize / 2f;
        float startY = gridCenterY - AnimationConstants.DICE_SIZE / 2f;

        float reserveGroupStartX;
        float activeGroupStartX;

        if (forPlayer) {
            reserveGroupStartX = startX;
            activeGroupStartX = startX + reserveWidth + (reserveCount > 0 ? gap : 0f);
        } else {
            activeGroupStartX = startX;
            reserveGroupStartX = startX + activeWidth + (activeCount > 0 ? gap : 0f);
        }

        int activeIndex = 0;
        int reserveIndex = 0;

        for (int i = 0; i < totalCount; i++) {
            DiceAnimator sourceAnimator = side.animators.get(i);
            float targetX;
            boolean isReserve;

            if (selected.get(i)) {
                targetX = activeGroupStartX + activeIndex * spacing;
                isReserve = false;
                activeIndex++;
            } else {
                targetX = reserveGroupStartX + reserveIndex * spacing;
                isReserve = true;
                reserveIndex++;
            }

            DiceAnimator restAnimator = new DiceAnimator();
            restAnimator.init();
            int displayValue = values.get(i);
            if (battleState.isPrismaticDiceAt(i, forPlayer)) {
                PrismaticDiceInstance prismatic = battleState.getPrismaticDiceAt(i, forPlayer);
                if (prismatic != null) {
                    displayValue = prismatic.faceIndex;
                }
            }
            restAnimator.startTravelToRestFrom(sourceAnimator, types.get(i), displayValue,
                targetX, startY);
            restAnimator.setReserve(isReserve);
            side.restAnimators.add(restAnimator);
        }

        side.clearMain();
        CosmiconLogger.info("[DICE-REST] moveSelectedToRestGrid OK: forPlayer=%s total=%d active=%d reserve=%d restAnimators=%d",
                forPlayer, totalCount, activeCount, reserveCount, side.restAnimators.size());
    }

    public void renderRestingDice(float panelX, float panelY, float panelWidth, float panelHeight, float alphaMult) {
        renderRestGroupBoxes(playerSide.restAnimators, panelX, panelY, panelHeight, alphaMult);
        renderRestGroupBoxes(opponentSide.restAnimators, panelX, panelY, panelHeight, alphaMult);

        for (DiceAnimator animator : playerSide.restAnimators) {
            animator.render(panelX, panelY, panelWidth, panelHeight, alphaMult);
        }
        for (DiceAnimator animator : opponentSide.restAnimators) {
            animator.render(panelX, panelY, panelWidth, panelHeight, alphaMult);
        }
    }

    private void renderRestGroupBoxes(List<DiceAnimator> restList, float panelX, float panelY,
                                       float panelHeight, float alphaMult) {
        if (restList.isEmpty()) return;

        float padding = BattleRenderingUtils.REST_GRID_BOX_PADDING;
        float diceSize = AnimationConstants.DICE_SIZE;

        float activeMinX = Float.MAX_VALUE, activeMinY = Float.MAX_VALUE;
        float activeMaxX = Float.MIN_VALUE, activeMaxY = Float.MIN_VALUE;
        float reserveMinX = Float.MAX_VALUE, reserveMinY = Float.MAX_VALUE;
        float reserveMaxX = Float.MIN_VALUE, reserveMaxY = Float.MIN_VALUE;
        boolean hasActive = false, hasReserve = false;

        for (DiceAnimator animator : restList) {
            float tx = animator.getTargetSlotX();
            float ty = animator.getTargetSlotY();

            if (animator.isReserve()) {
                hasReserve = true;
                reserveMinX = Math.min(reserveMinX, tx);
                reserveMinY = Math.min(reserveMinY, ty);
                reserveMaxX = Math.max(reserveMaxX, tx + diceSize);
                reserveMaxY = Math.max(reserveMaxY, ty + diceSize);
            } else {
                hasActive = true;
                activeMinX = Math.min(activeMinX, tx);
                activeMinY = Math.min(activeMinY, ty);
                activeMaxX = Math.max(activeMaxX, tx + diceSize);
                activeMaxY = Math.max(activeMaxY, ty + diceSize);
            }
        }

        if (hasActive) {
            float uiLeft = activeMinX - padding;
            float uiTop = activeMinY - padding;
            float uiRight = activeMaxX + padding;
            float uiBottom = activeMaxY + padding;
            float glX = panelX + uiLeft;
            float glY = panelY + panelHeight - uiBottom;
            float glW = uiRight - uiLeft;
            float glH = uiBottom - uiTop;
            BattleRenderingUtils.renderRestGroupBox(glX, glY, glW, glH,
                ColorHelper.REST_ACTIVE_BG, ColorHelper.REST_ACTIVE_BORDER, alphaMult);
        }

        if (hasReserve) {
            float uiLeft = reserveMinX - padding;
            float uiTop = reserveMinY - padding;
            float uiRight = reserveMaxX + padding;
            float uiBottom = reserveMaxY + padding;
            float glX = panelX + uiLeft;
            float glY = panelY + panelHeight - uiBottom;
            float glW = uiRight - uiLeft;
            float glH = uiBottom - uiTop;
            BattleRenderingUtils.renderRestGroupBox(glX, glY, glW, glH,
                ColorHelper.REST_RESERVE_BG, ColorHelper.REST_RESERVE_BORDER, alphaMult);
        }
    }

    public boolean isRestTravelComplete(boolean forPlayer) {
        DiceSide side = forPlayer ? playerSide : opponentSide;
        if (side.restAnimators.isEmpty()) return true;
        for (DiceAnimator animator : side.restAnimators) {
            if (!animator.isAtRest()) return false;
        }
        return true;
    }

    public void updateRestDiceValue(int index, int newValue, boolean forPlayer) {
        DiceSide side = forPlayer ? playerSide : opponentSide;
        if (index >= 0 && index < side.restAnimators.size()) {
            int displayValue = newValue;
            if (battleState.isPrismaticDiceAt(index, forPlayer)) {
                PrismaticDiceInstance prismatic = battleState.getPrismaticDiceAt(index, forPlayer);
                if (prismatic != null) {
                    displayValue = prismatic.faceIndex;
                }
            }
            CosmiconLogger.info("[DICE-REST] updateRestDiceValue: forPlayer=%s idx=%d newVal=%d displayVal=%d restCount=%d",
                    forPlayer, index, newValue, displayValue, side.restAnimators.size());
            side.restAnimators.get(index).animateValueChange(displayValue);
        } else {
            CosmiconLogger.info("[DICE-REST] updateRestDiceValue SKIPPED: forPlayer=%s idx=%d newVal=%d restCount=%d",
                    forPlayer, index, newValue, side.restAnimators.size());
        }
    }

    public void updateRestDiceType(int index, DiceType newType, boolean forPlayer) {
        DiceSide side = forPlayer ? playerSide : opponentSide;
        if (index >= 0 && index < side.restAnimators.size()) {
            side.restAnimators.get(index).setType(newType);
        }
    }

    public void setRestDiceEffect(int index, StatusEffectProcessor.StatusEffect effect, boolean forPlayer) {
        DiceSide side = forPlayer ? playerSide : opponentSide;
        if (index >= 0 && index < side.restAnimators.size()) {
            CosmiconLogger.info("[DICE-REST] setRestDiceEffect: forPlayer=%s idx=%d effect=%s restCount=%d",
                    forPlayer, index, effect, side.restAnimators.size());
            side.restAnimators.get(index).setDiceEffect(effect);
        } else {
            CosmiconLogger.info("[DICE-REST] setRestDiceEffect SKIPPED: forPlayer=%s idx=%d effect=%s restCount=%d",
                    forPlayer, index, effect, side.restAnimators.size());
        }
    }

    public void clearRestAnimators(boolean forPlayer) {
        DiceSide side = forPlayer ? playerSide : opponentSide;
        for (DiceAnimator animator : side.restAnimators) {
            animator.forceComplete();
            animator.setDiceEffect(null);
        }
        side.restAnimators.clear();
    }

    public void clearAllRestAnimators() {
        clearRestAnimators(true);
        clearRestAnimators(false);
    }

    public boolean hasRestAnimators(boolean forPlayer) {
        DiceSide side = forPlayer ? playerSide : opponentSide;
        return !side.restAnimators.isEmpty();
    }

    public void startRollFromRest(boolean forPlayer, List<DiceType> allTypes, List<Integer> allValues,
                                    float centerX, float centerY) {
        if (!initialized) return;

        DiceSide side = forPlayer ? playerSide : opponentSide;
        side.startRollFromRest(allTypes, allValues, centerX, centerY, battleState, forPlayer);
    }

    public List<DiceAnimator> getOpponentAnimators() {
        return new ArrayList<>(opponentSide.animators);
    }

    public List<DiceAnimator> getAnimators() {
        return new ArrayList<>(playerSide.animators);
    }

    public float getAnimatorVisualX(int index) {
        if (index < 0 || index >= playerSide.animators.size()) return -1f;
        return playerSide.animators.get(index).getVisualX();
    }

    public float getAnimatorVisualY(int index) {
        if (index < 0 || index >= playerSide.animators.size()) return -1f;
        return playerSide.animators.get(index).getVisualY();
    }

    public int getAnimatorCount() {
        return playerSide.animators.size();
    }

    public float getAnimatorTargetSlotX(int index) {
        if (index < 0 || index >= playerSide.animators.size()) return -1f;
        return playerSide.animators.get(index).getTargetSlotX();
    }

    public float getAnimatorTargetSlotY(int index) {
        if (index < 0 || index >= playerSide.animators.size()) return -1f;
        return playerSide.animators.get(index).getTargetSlotY();
    }

    private static class DiceSide {

        final List<DiceAnimator> animators = new ArrayList<>();
        final List<DiceAnimator> restAnimators = new ArrayList<>();
        boolean waitingForRollTrigger = false;
        List<PlannedPath> pendingRollPaths;
        float[][] pendingScatterDestinations;

        void startStationaryPreview(List<DiceType> types, List<Integer> results, float centerX, float centerY) {
            clear();
            clearRestAnimatorsForceComplete();
            pendingRollPaths = null;
            pendingScatterDestinations = null;

            int count = Math.min(types.size(), results.size());
            List<PlannedPath> gridPaths = DicePathPlanner.planPaths(types, results, centerX, centerY, DICE_SPACING,
                    BattleRenderingUtils.PANEL_WIDTH, BattleRenderingUtils.PANEL_HEIGHT);

            float panelW = BattleRenderingUtils.PANEL_WIDTH;
            float panelH = BattleRenderingUtils.PANEL_HEIGHT;
            float[][] scatters = DicePathPlanner.planScatterDestinations(count, panelW, panelH);
            pendingScatterDestinations = scatters;

            float[] targetXs = new float[count];
            float[] targetYs = new float[count];
            for (int i = 0; i < count; i++) {
                targetXs[i] = gridPaths.get(i).targetCenterX();
                targetYs[i] = gridPaths.get(i).targetCenterY();
            }
            pendingRollPaths = DicePathPlanner.planTravelPaths(
                scatters, targetXs, targetYs, panelW, panelH);

            for (int i = 0; i < count; i++) {
                DiceAnimator animator = new DiceAnimator();
                animator.init();
                PlannedPath path = gridPaths.get(i);

                animator.startStationaryPreview(types.get(i), results.get(i), path.targetCenterX(), path.targetCenterY());
                animators.add(animator);
            }

            waitingForRollTrigger = true;
        }

        void triggerRollFromStationary() {
            if (!waitingForRollTrigger) return;
            if (pendingRollPaths == null || pendingRollPaths.isEmpty()) return;
            if (pendingScatterDestinations == null) return;

            int count = Math.min(animators.size(), Math.min(pendingRollPaths.size(), pendingScatterDestinations.length));
            for (int i = 0; i < count; i++) {
                DiceAnimator animator = animators.get(i);
                PlannedPath path = pendingRollPaths.get(i);
                float scatterX = pendingScatterDestinations[i][0];
                float scatterY = pendingScatterDestinations[i][1];

                animator.startScatterFromPreview(scatterX, scatterY, path.delay(),
                        path.rotation(), path.travelDistance(),
                        path.bounceCount(), path.bounceHeights(),
                        path.targetCenterX(), path.targetCenterY());
            }

            waitingForRollTrigger = false;
            pendingRollPaths = null;
            pendingScatterDestinations = null;
        }

        boolean isWaitingForRollTrigger() {
            return waitingForRollTrigger;
        }

        void clear() {
            for (DiceAnimator animator : animators) {
                animator.forceComplete();
            }
            animators.clear();
            waitingForRollTrigger = false;
            pendingRollPaths = null;
            pendingScatterDestinations = null;
        }

        void clearMain() {
            for (DiceAnimator animator : animators) {
                animator.forceComplete();
            }
            animators.clear();
        }

        void forceCompleteAll() {
            for (DiceAnimator animator : animators) {
                animator.forceComplete();
            }
        }

        void render(float panelX, float panelY, float panelWidth, float panelHeight, float alphaMult) {
            for (DiceAnimator animator : animators) {
                animator.render(panelX, panelY, panelWidth, panelHeight, alphaMult);
            }
        }

        boolean isComplete() {
            if (animators.isEmpty()) return true;
            for (DiceAnimator animator : animators) {
                if (animator.isRunning()) {
                    return false;
                }
            }
            return true;
        }

        boolean hasAnimators() {
            return !animators.isEmpty();
        }

        List<float[]> collectAllDicePositions() {
            List<float[]> positions = new ArrayList<>();
            for (DiceAnimator animator : animators) {
                positions.add(new float[]{
                    animator.getTargetSlotX(),
                    animator.getTargetSlotY()
                });
            }
            return positions;
        }

        void appendInstantDice(DiceType type, int value, float centerX, float centerY) {
            List<float[]> existingPositions = collectAllDicePositions();
            PlannedPath prismaticPath = DicePathPlanner.planSinglePrismaticPath(
                animators.size(), centerX, centerY, DICE_SPACING, existingPositions,
                BattleRenderingUtils.PANEL_WIDTH, BattleRenderingUtils.PANEL_HEIGHT);

            float panelW = BattleRenderingUtils.PANEL_WIDTH;
            float panelH = BattleRenderingUtils.PANEL_HEIGHT;

            float[][] scatterDest = DicePathPlanner.planScatterDestinations(1, panelW, panelH);
            float scatterX = scatterDest[0][0];
            float scatterY = scatterDest[0][1];

            float[][] singlePos = new float[][]{{scatterX, scatterY}};
            float[] targetXs = new float[]{prismaticPath.targetCenterX()};
            float[] targetYs = new float[]{prismaticPath.targetCenterY()};
            List<PlannedPath> travelPaths = DicePathPlanner.planTravelPaths(
                singlePos, targetXs, targetYs, panelW, panelH);
            PlannedPath travelPath = travelPaths.get(0);

            DiceAnimator animator = new DiceAnimator();
            animator.init();
            animator.startFromScatterPosition(type, value, scatterX, scatterY, prismaticPath.delay(),
                    travelPath.rotation(), travelPath.travelDistance(),
                    travelPath.bounceCount(), travelPath.bounceHeights(),
                    prismaticPath.targetCenterX(), prismaticPath.targetCenterY());
            animators.add(animator);
        }

        void reroll(List<Integer> indices, List<Integer> newValues, BattleState battleState, boolean forPlayer) {
            if (indices == null || indices.isEmpty() || newValues == null) return;

            float panelW = BattleRenderingUtils.PANEL_WIDTH;
            float panelH = BattleRenderingUtils.PANEL_HEIGHT;

            List<Integer> sortedIndices = new ArrayList<>(indices);
            Collections.sort(sortedIndices);

            float[][] scatters = DicePathPlanner.planScatterDestinations(sortedIndices.size(), panelW, panelH);

            float[] targetXs = new float[sortedIndices.size()];
            float[] targetYs = new float[sortedIndices.size()];
            for (int i = 0; i < sortedIndices.size(); i++) {
                int idx = sortedIndices.get(i);
                if (idx >= 0 && idx < animators.size()) {
                    DiceAnimator animator = animators.get(idx);
                    targetXs[i] = animator.getTargetSlotX();
                    targetYs[i] = animator.getTargetSlotY();
                }
            }

            List<PlannedPath> travelPaths = DicePathPlanner.planTravelPaths(scatters, targetXs, targetYs, panelW, panelH);

            for (int i = 0; i < sortedIndices.size(); i++) {
                int animatorIndex = sortedIndices.get(i);
                if (animatorIndex < 0 || animatorIndex >= animators.size() || i >= travelPaths.size()) continue;

                PlannedPath path = travelPaths.get(i);
                if (path == null) continue;

                DiceAnimator animator = animators.get(animatorIndex);
                int rerollValue = newValues.get(animatorIndex);

                if (battleState != null && battleState.isPrismaticDiceAt(animatorIndex, forPlayer)) {
                    PrismaticDiceInstance prismatic = battleState.getPrismaticDiceAt(animatorIndex, forPlayer);
                    if (prismatic != null) {
                        rerollValue = prismatic.faceIndex;
                    }
                }

                float dropX = scatters[i][0];
                float dropY = scatters[i][1];

                animator.rerollWithNewPath(rerollValue, dropX, dropY,
                        path.rotation(), path.travelDistance(),
                        path.bounceCount(), path.bounceHeights(),
                        animator.getTargetSlotX(), animator.getTargetSlotY());
            }
        }

        void startRollFromRest(List<DiceType> allTypes, List<Integer> allValues,
                                float centerX, float centerY, BattleState battleState, boolean forPlayer) {
            clearMain();

            int count = allTypes.size();
            float panelW = BattleRenderingUtils.PANEL_WIDTH;
            float panelH = BattleRenderingUtils.PANEL_HEIGHT;

            List<PlannedPath> gridPaths = DicePathPlanner.planPaths(allTypes, allValues, centerX, centerY, DICE_SPACING,
                    panelW, panelH);

            float[][] scatters = DicePathPlanner.planScatterDestinations(count, panelW, panelH);

            float[] targetXs = new float[count];
            float[] targetYs = new float[count];
            for (int i = 0; i < count; i++) {
                targetXs[i] = gridPaths.get(i).targetCenterX();
                targetYs[i] = gridPaths.get(i).targetCenterY();
            }
            List<PlannedPath> travelPaths = DicePathPlanner.planTravelPaths(scatters, targetXs, targetYs, panelW, panelH);

            int restCount = restAnimators.size();
            CosmiconLogger.info("[DICE-REST] startRollFromRest: forPlayer=%s diceCount=%d restCount=%d animators=%d",
                    forPlayer, count, restCount, animators.size());

            for (int i = 0; i < count; i++) {
                DiceAnimator animator = new DiceAnimator();
                animator.init();
                PlannedPath path = travelPaths.get(i);
                int displayValue = allValues.get(i);
                if (battleState.isPrismaticDiceAt(i, forPlayer)) {
                    PrismaticDiceInstance prismatic = battleState.getPrismaticDiceAt(i, forPlayer);
                    if (prismatic != null) {
                        displayValue = prismatic.faceIndex;
                    }
                }

                if (i < restCount) {
                    DiceAnimator restAnimator = restAnimators.get(i);
                    float scatterX = scatters[i][0];
                    float scatterY = scatters[i][1];
                    animator.startFromRestPosition(allTypes.get(i), displayValue,
                        restAnimator.getX(), restAnimator.getY(),
                        scatterX, scatterY,
                        path.delay(), path.rotation(), path.travelDistance(),
                        path.bounceCount(), path.bounceHeights(),
                        path.targetCenterX(), path.targetCenterY());
                } else {
                    float scatterX = scatters[i][0];
                    float scatterY = scatters[i][1];
                    animator.startFromScatterPosition(allTypes.get(i), displayValue,
                        scatterX, scatterY, path.delay(),
                        path.rotation(), path.travelDistance(),
                        path.bounceCount(), path.bounceHeights(),
                        path.targetCenterX(), path.targetCenterY());
                }
                animators.add(animator);
            }

            restAnimators.clear();
        }

        void advanceAnimators(float amount) {
            if (!animators.isEmpty()) {
                for (DiceAnimator animator : animators) {
                    animator.advance(amount);
                }
            }
        }

        private void clearRestAnimatorsForceComplete() {
            for (DiceAnimator animator : restAnimators) {
                animator.forceComplete();
            }
            restAnimators.clear();
        }
    }
}
