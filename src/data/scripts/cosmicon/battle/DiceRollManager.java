package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.List;

import data.scripts.cosmicon.battle.DicePathPlanner.PlannedPath;
import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;

public class DiceRollManager {

    private static final float DICE_SPACING = 90f;

    private final List<DiceAnimator> animators;
    private final List<DiceAnimator> opponentAnimators;
    private boolean initialized;
    private BattleState battleState;
    
    private boolean waitingForRollTrigger = false;
    private boolean opponentWaitingForRollTrigger = false;
    private List<PlannedPath> pendingRollPaths;
    private List<PlannedPath> pendingOpponentRollPaths;
    private float[][] pendingScatterDestinations;
    private float[][] pendingOpponentScatterDestinations;

    public DiceRollManager() {
        this.animators = new ArrayList<>();
        this.opponentAnimators = new ArrayList<>();
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

        clear();
        animators.clear();
        pendingRollPaths = null;
        pendingScatterDestinations = null;
        waitingForRollTrigger = false;

        int count = Math.min(types.size(), results.size());
        List<PlannedPath> gridPaths = DicePathPlanner.planPaths(types, results, centerX, centerY, DICE_SPACING);
        
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
    
    public void triggerRollFromStationary() {
        if (!waitingForRollTrigger) return;
        if (pendingRollPaths == null || pendingRollPaths.isEmpty()) return;
        if (pendingScatterDestinations == null) return;
        
        for (int i = 0; i < animators.size() && i < pendingRollPaths.size(); i++) {
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
    }
    
    public boolean isWaitingForRollTrigger() {
        return waitingForRollTrigger;
    }
    
    public void startOpponentStationaryPreview(List<DiceType> types, List<Integer> results, float centerX, float centerY) {
        if (!initialized) return;

        clearOpponentAnimators();
        pendingOpponentRollPaths = null;
        pendingOpponentScatterDestinations = null;
        opponentWaitingForRollTrigger = false;

        int count = Math.min(types.size(), results.size());
        List<PlannedPath> gridPaths = DicePathPlanner.planPaths(types, results, centerX, centerY, DICE_SPACING);
        
        float panelW = BattleRenderingUtils.PANEL_WIDTH;
        float panelH = BattleRenderingUtils.PANEL_HEIGHT;
        float[][] scatters = DicePathPlanner.planScatterDestinations(count, panelW, panelH);
        pendingOpponentScatterDestinations = scatters;
        
        float[] targetXs = new float[count];
        float[] targetYs = new float[count];
        for (int i = 0; i < count; i++) {
            targetXs[i] = gridPaths.get(i).targetCenterX();
            targetYs[i] = gridPaths.get(i).targetCenterY();
        }
        pendingOpponentRollPaths = DicePathPlanner.planTravelPaths(
            scatters, targetXs, targetYs, panelW, panelH);
        
        for (int i = 0; i < count; i++) {
            DiceAnimator animator = new DiceAnimator();
            animator.init();
            PlannedPath path = gridPaths.get(i);
            
            animator.startStationaryPreview(types.get(i), results.get(i), path.targetCenterX(), path.targetCenterY());
            opponentAnimators.add(animator);
        }
        
        opponentWaitingForRollTrigger = true;
    }
    
    public void triggerOpponentRollFromStationary() {
        if (!opponentWaitingForRollTrigger) return;
        if (pendingOpponentRollPaths == null || pendingOpponentRollPaths.isEmpty()) return;
        if (pendingOpponentScatterDestinations == null) return;
        
        for (int i = 0; i < opponentAnimators.size() && i < pendingOpponentRollPaths.size(); i++) {
            DiceAnimator animator = opponentAnimators.get(i);
            PlannedPath path = pendingOpponentRollPaths.get(i);
            float scatterX = pendingOpponentScatterDestinations[i][0];
            float scatterY = pendingOpponentScatterDestinations[i][1];
            
            animator.startScatterFromPreview(scatterX, scatterY, path.delay(),
                    path.rotation(), path.travelDistance(),
                    path.bounceCount(), path.bounceHeights(),
                    path.targetCenterX(), path.targetCenterY());
        }
        
        opponentWaitingForRollTrigger = false;
    }
    
    public boolean isOpponentWaitingForRollTrigger() {
        return opponentWaitingForRollTrigger;
    }

    public void advance(float amount) {
        for (DiceAnimator animator : animators) {
            animator.advance(amount);
        }
        for (DiceAnimator animator : opponentAnimators) {
            animator.advance(amount);
        }
        
        if (!animators.isEmpty()) {
            boolean anyWaiting = false;
            boolean anyStillAnimating = false;
            for (DiceAnimator animator : animators) {
                if (animator.isReadyForCentering()) {
                    anyWaiting = true;
                } else if (animator.isActive() && !animator.isComplete()) {
                    anyStillAnimating = true;
                }
            }
            if (anyWaiting && !anyStillAnimating) {
                for (DiceAnimator animator : animators) {
                    animator.startCenteringAnimation();
                }
            }
        }
        
        if (!opponentAnimators.isEmpty()) {
            boolean anyWaiting = false;
            boolean anyStillAnimating = false;
            for (DiceAnimator animator : opponentAnimators) {
                if (animator.isReadyForCentering()) {
                    anyWaiting = true;
                } else if (animator.isActive() && !animator.isComplete()) {
                    anyStillAnimating = true;
                }
            }
            if (anyWaiting && !anyStillAnimating) {
                for (DiceAnimator animator : opponentAnimators) {
                    animator.startCenteringAnimation();
                }
            }
        }
    }

    public void render(float panelX, float panelY, float panelHeight, float alphaMult) {
        for (DiceAnimator animator : animators) {
            animator.render(panelX, panelY, BattleRenderingUtils.PANEL_WIDTH, panelHeight, alphaMult);
        }
    }

    public boolean isComplete() {
        if (animators.isEmpty()) return true;
        for (DiceAnimator animator : animators) {
            if (!animator.isComplete()) {
                return false;
            }
        }
        return true;
    }
    
    public boolean hasAnimators() {
        return !animators.isEmpty();
    }
    
    public void appendInstantDice(DiceType type, int value, float centerX, float centerY) {
        if (!initialized) return;

        List<float[]> existingPositions = collectAllDicePositions();
        PlannedPath prismaticPath = DicePathPlanner.planSinglePrismaticPath(
            animators.size(), centerX, centerY, DICE_SPACING, existingPositions);
        
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
    
    
    
    private List<float[]> collectAllDicePositions() {
        List<float[]> positions = new ArrayList<>();
        for (DiceAnimator animator : animators) {
            positions.add(new float[]{
                animator.getVisualX(),
                animator.getVisualY()
            });
        }
        return positions;
    }
    
    private List<float[]> collectAllOpponentDicePositions() {
        List<float[]> positions = new ArrayList<>();
        for (DiceAnimator animator : opponentAnimators) {
            positions.add(new float[]{
                animator.getVisualX(),
                animator.getVisualY()
            });
        }
        return positions;
    }

    public void clear() {
        for (DiceAnimator animator : animators) {
            animator.forceComplete();
        }
        animators.clear();
        waitingForRollTrigger = false;
        pendingRollPaths = null;
        pendingScatterDestinations = null;
    }

    public void partialReroll(List<Integer> indices, List<Integer> newValues) {
        List<float[]> allPositions = collectAllDicePositions();
        List<PlannedPath> rerollPaths = DicePathPlanner.planRerollPaths(indices, allPositions);
        
        for (int i = 0; i < indices.size(); i++) {
            int animatorIndex = indices.get(i);
            PlannedPath path = rerollPaths.get(i);
            if (animatorIndex >= 0 && animatorIndex < animators.size() && path != null) {
                DiceAnimator animator = animators.get(animatorIndex);
                int rerollValue = newValues.get(animatorIndex);
                
                if (battleState != null && battleState.isPrismaticDiceAt(animatorIndex, true)) {
                    PrismaticDiceInstance prismatic = battleState.getPrismaticDiceAt(animatorIndex, true);
                    if (prismatic != null) {
                        rerollValue = prismatic.faceIndex;
                    }
                }
                
                animator.rerollWithNewPath(rerollValue, path.startX(), path.startY(),
                        path.rotation(), path.travelDistance(),
                        path.bounceCount(), path.bounceHeights(),
                        path.targetCenterX(), path.targetCenterY());
            }
        }
    }

    public void forceCompleteAll() {
        for (DiceAnimator animator : animators) {
            animator.forceComplete();
        }
    }

    public void renderOpponentDice(float panelX, float panelY, float alphaMult) {
        for (DiceAnimator animator : opponentAnimators) {
            animator.render(panelX, panelY, BattleRenderingUtils.PANEL_WIDTH, BattleRenderingUtils.PANEL_HEIGHT, alphaMult);
        }
    }

    public void rerollOpponentDice(List<Integer> indices, List<Integer> newValues) {
        List<float[]> allPositions = collectAllOpponentDicePositions();
        List<PlannedPath> rerollPaths = DicePathPlanner.planRerollPaths(indices, allPositions);
        
        for (int i = 0; i < indices.size(); i++) {
            int animatorIndex = indices.get(i);
            PlannedPath path = rerollPaths.get(i);
            if (animatorIndex >= 0 && animatorIndex < opponentAnimators.size() && path != null) {
                DiceAnimator animator = opponentAnimators.get(animatorIndex);
                int rerollValue = newValues.get(animatorIndex);
                
                if (battleState != null && battleState.isPrismaticDiceAt(animatorIndex, false)) {
                    PrismaticDiceInstance prismatic = battleState.getPrismaticDiceAt(animatorIndex, false);
                    if (prismatic != null) {
                        rerollValue = prismatic.faceIndex;
                    }
                }
                
                animator.rerollWithNewPath(rerollValue, path.startX(), path.startY(),
                        path.rotation(), path.travelDistance(),
                        path.bounceCount(), path.bounceHeights(),
                        path.targetCenterX(), path.targetCenterY());
            }
        }
    }

    public boolean isOpponentComplete() {
        if (opponentAnimators.isEmpty()) return true;
        for (DiceAnimator animator : opponentAnimators) {
            if (!animator.isComplete()) {
                return false;
            }
        }
        return true;
    }

    public boolean hasOpponentAnimators() {
        return !opponentAnimators.isEmpty();
    }

    public void clearOpponentAnimators() {
        for (DiceAnimator animator : opponentAnimators) {
            animator.forceComplete();
        }
        opponentAnimators.clear();
        opponentWaitingForRollTrigger = false;
        pendingOpponentRollPaths = null;
        pendingOpponentScatterDestinations = null;
    }

    public List<DiceAnimator> getOpponentAnimators() {
        return opponentAnimators;
    }
    
    public List<DiceAnimator> getAnimators() {
        return animators;
    }

    public float getAnimatorVisualX(int index) {
        if (index < 0 || index >= animators.size()) return -1f;
        return animators.get(index).getVisualX();
    }

    public float getAnimatorVisualY(int index) {
        if (index < 0 || index >= animators.size()) return -1f;
        return animators.get(index).getVisualY();
    }

    public int getAnimatorCount() {
        return animators.size();
    }
    
    
    
    public float getAnimatorTargetSlotX(int index) {
        if (index < 0 || index >= animators.size()) return -1f;
        return animators.get(index).getTargetSlotX();
    }
    
    public float getAnimatorTargetSlotY(int index) {
        if (index < 0 || index >= animators.size()) return -1f;
        return animators.get(index).getTargetSlotY();
    }
}