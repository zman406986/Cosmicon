package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.ui.CustomPanelAPI;

public class DiceRollManager {

    private static final float DICE_SPACING = 130f;

    private final List<DiceAnimator> animators;
    private final List<DiceAnimator> opponentAnimators;
    private CustomPanelAPI panel;
    private boolean initialized;

    public DiceRollManager() {
        this.animators = new ArrayList<>();
        this.opponentAnimators = new ArrayList<>();
        this.initialized = false;
    }

    public void init(CustomPanelAPI panel) {
        this.panel = panel;
        this.initialized = true;
    }

    public void startRoll(List<DiceType> types, List<Integer> results, float centerX, float centerY) {
        if (!initialized) return;

        clear();
        animators.clear();

        int count = Math.min(types.size(), results.size());
        List<DicePathPlanner.PlannedPath> paths = DicePathPlanner.planPaths(types, results, centerX, centerY, DICE_SPACING);
        
        for (int i = 0; i < count; i++) {
            DiceAnimator animator = new DiceAnimator();
            animator.init(panel);
            DicePathPlanner.PlannedPath path = paths.get(i);
            
            animator.start(types.get(i), results.get(i), path.startX(), path.startY(), path.delay(),
                    path.rotation(), path.travelDistance(), path.bounceCount(), path.bounceHeights(),
                    path.targetCenterX(), path.targetCenterY());
            animators.add(animator);
        }
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
            animator.render(panelX, panelY, panelHeight, alphaMult);
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
        DicePathPlanner.PlannedPath path = DicePathPlanner.planSinglePrismaticPath(
            animators.size(), centerX, centerY, DICE_SPACING, existingPositions);

        DiceAnimator animator = new DiceAnimator();
        animator.init(panel);
        animator.start(type, value, path.startX(), path.startY(), path.delay(),
                path.rotation(), path.travelDistance(), path.bounceCount(), path.bounceHeights(),
                path.targetCenterX(), path.targetCenterY());
        animators.add(animator);
    }
    
    private List<DicePathPlanner.PlannedPath> collectExistingPaths() {
        List<DicePathPlanner.PlannedPath> paths = new ArrayList<>();
        for (DiceAnimator animator : animators) {
            float endX = animator.getVisualX();
            float endY = animator.getVisualY();
            paths.add(new DicePathPlanner.PlannedPath(animator.getRotation(), 
                0f, 0, new float[0], endX, endY, 0f, endX, endY));
        }
        return paths;
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
    }

    public void partialReroll(List<Integer> indices, List<Integer> newValues) {
        List<float[]> allPositions = collectAllDicePositions();
        List<DicePathPlanner.PlannedPath> rerollPaths = DicePathPlanner.planRerollPaths(indices, allPositions);
        
        for (int i = 0; i < indices.size(); i++) {
            int animatorIndex = indices.get(i);
            DicePathPlanner.PlannedPath path = rerollPaths.get(i);
            if (animatorIndex >= 0 && animatorIndex < animators.size() && path != null) {
                DiceAnimator animator = animators.get(animatorIndex);
                animator.rerollWithNewPath(newValues.get(i), path.rotation(), path.travelDistance(),
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

    public void startOpponentRoll(List<DiceType> types, List<Integer> results, float zoneX, float zoneY) {
        if (!initialized) return;

        clearOpponentAnimators();

        int count = Math.min(types.size(), results.size());
        float centerX = zoneX + BattleRenderingUtils.OPPONENT_DICE_ZONE_W / 2f;
        float centerY = zoneY + BattleRenderingUtils.OPPONENT_DICE_ZONE_H / 2f;
        
        List<DicePathPlanner.PlannedPath> paths = DicePathPlanner.planPaths(types, results, centerX, centerY, DICE_SPACING);
        
        for (int i = 0; i < count; i++) {
            DiceAnimator animator = new DiceAnimator();
            animator.init(panel);
            DicePathPlanner.PlannedPath path = paths.get(i);
            
            animator.start(types.get(i), results.get(i), path.startX(), path.startY(), path.delay(),
                    path.rotation(), path.travelDistance(), path.bounceCount(), path.bounceHeights(),
                    path.targetCenterX(), path.targetCenterY());
            opponentAnimators.add(animator);
        }
    }

    public void renderOpponentDice(float panelX, float panelY, float alphaMult) {
        for (DiceAnimator animator : opponentAnimators) {
            animator.render(panelX, panelY, BattleRenderingUtils.PANEL_HEIGHT, alphaMult);
        }
    }

    public void rerollOpponentDice(List<Integer> indices, List<Integer> newValues) {
        List<float[]> allPositions = collectAllOpponentDicePositions();
        List<DicePathPlanner.PlannedPath> rerollPaths = DicePathPlanner.planRerollPaths(indices, allPositions);
        
        for (int i = 0; i < indices.size(); i++) {
            int animatorIndex = indices.get(i);
            DicePathPlanner.PlannedPath path = rerollPaths.get(i);
            if (animatorIndex >= 0 && animatorIndex < opponentAnimators.size() && path != null) {
                DiceAnimator animator = opponentAnimators.get(animatorIndex);
                animator.rerollWithNewPath(newValues.get(i), path.rotation(), path.travelDistance(),
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
    }

    public List<DiceAnimator> getOpponentAnimators() {
        return opponentAnimators;
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
}