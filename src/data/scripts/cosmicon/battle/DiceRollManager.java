package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.ui.CustomPanelAPI;

import data.scripts.cosmicon.util.CosmiconLogger;

public class DiceRollManager {

    private static final float DICE_SPACING = 130f;

    private final List<DiceAnimator> animators;
    private final List<DiceAnimator> opponentAnimators;
    private float opponentZoneX;
    private float opponentZoneY;
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
            
            animator.start(types.get(i), results.get(i), path.startX, path.startY, path.delay,
                           path.rotation, path.travelDistance, path.bounceCount, path.bounceHeights);
            animators.add(animator);
        }
    }

    public void advance(float amount) {
        for (DiceAnimator animator : animators) {
            animator.advance(amount);
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
    
    public void appendRoll(List<DiceType> types, List<Integer> results, float centerX, float centerY) {
        if (!initialized) return;

        int count = Math.min(types.size(), results.size());
        CosmiconLogger.debug("Appending %d dice to existing %d animators at center (%.0f, %.0f)",
            count, animators.size(), centerX, centerY);

        List<DicePathPlanner.PlannedPath> existingPaths = collectExistingPaths();
        List<DicePathPlanner.PlannedPath> newPaths = DicePathPlanner.planPathsAppend(
            types, results, centerX, centerY, DICE_SPACING, animators.size(), existingPaths);
        
        for (int i = 0; i < count; i++) {
            DiceAnimator animator = new DiceAnimator();
            animator.init(panel);
            DicePathPlanner.PlannedPath path = newPaths.get(i);
            
            animator.start(types.get(i), results.get(i), path.startX, path.startY, path.delay,
                           path.rotation, path.travelDistance, path.bounceCount, path.bounceHeights);
            animators.add(animator);
        }
    }
    
    private List<DicePathPlanner.PlannedPath> collectExistingPaths() {
        List<DicePathPlanner.PlannedPath> paths = new ArrayList<>();
        for (DiceAnimator animator : animators) {
            float endX = animator.getX() + animator.getPosXOffset();
            float endY = animator.getY() + animator.getPosYOffset();
            paths.add(new DicePathPlanner.PlannedPath(animator.getRotation(), 
                0f, 0, new float[0], endX, endY, 0f));
        }
        return paths;
    }

    public void clear() {
        for (DiceAnimator animator : animators) {
            animator.forceComplete();
        }
        animators.clear();
    }

    public void partialReroll(List<Integer> indices, List<Integer> newValues) {
        for (int animatorIndex : indices)
        {
            if (animatorIndex >= 0 && animatorIndex < animators.size())
            {
                DiceAnimator animator = animators.get(animatorIndex);
                animator.reroll(newValues.get(animatorIndex));
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
        
        this.opponentZoneX = zoneX;
        this.opponentZoneY = zoneY;

        int count = Math.min(types.size(), results.size());
        float centerX = zoneX + BattleRenderingUtils.OPPONENT_DICE_ZONE_W / 2f;
        float centerY = zoneY + BattleRenderingUtils.OPPONENT_DICE_ZONE_H / 2f;
        
        List<DicePathPlanner.PlannedPath> paths = DicePathPlanner.planPaths(types, results, centerX, centerY, DICE_SPACING);
        
        for (int i = 0; i < count; i++) {
            DiceAnimator animator = new DiceAnimator();
            animator.init(panel);
            DicePathPlanner.PlannedPath path = paths.get(i);
            
            animator.start(types.get(i), results.get(i), path.startX, path.startY, path.delay,
                           path.rotation, path.travelDistance, path.bounceCount, path.bounceHeights);
            opponentAnimators.add(animator);
        }
    }

    public void renderOpponentDice(float panelX, float panelY, float alphaMult) {
        for (DiceAnimator animator : opponentAnimators) {
            animator.render(panelX, panelY, BattleRenderingUtils.PANEL_HEIGHT, alphaMult);
        }
    }

    public void rerollOpponentDice(List<Integer> indices, List<Integer> newValues) {
        for (int animatorIndex : indices) {
            if (animatorIndex >= 0 && animatorIndex < opponentAnimators.size()) {
                DiceAnimator animator = opponentAnimators.get(animatorIndex);
                animator.reroll(newValues.get(animatorIndex));
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

    public void forceCompleteOpponent() {
        for (DiceAnimator animator : opponentAnimators) {
            animator.forceComplete();
        }
    }

    public List<DiceAnimator> getOpponentAnimators() {
        return opponentAnimators;
    }

    public void clearAll() {
        clear();
        clearOpponentAnimators();
    }
}