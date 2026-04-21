package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.ui.CustomPanelAPI;

import data.scripts.cosmicon.util.CosmiconLogger;

public class DiceRollManager {

    private static final float STAGGER_DELAY = 0.05f;
    private static final float DICE_SPACING = 70f;
    private static final float DICE_SIZE = 60f;

    private final List<DiceAnimator> animators;
    private CustomPanelAPI panel;
    private boolean initialized;

    public DiceRollManager() {
        this.animators = new ArrayList<>();
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
        float totalWidth = DICE_SPACING * (count - 1) + DICE_SIZE;
        float startX = centerX - totalWidth / 2f;
        float startY = centerY - DICE_SIZE / 2f;
        
        for (int i = 0; i < count; i++) {
            DiceAnimator animator = new DiceAnimator();
            animator.init(panel);
            float diceX = startX + i * DICE_SPACING;
            float delay = i * STAGGER_DELAY;
            animator.start(types.get(i), results.get(i), diceX, startY, delay);
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

        float totalWidth = DICE_SPACING * (count - 1) + DICE_SIZE;
        float startX = centerX - totalWidth / 2f;
        float startY = centerY - DICE_SIZE / 2f;
        
        for (int i = 0; i < count; i++) {
            DiceAnimator animator = new DiceAnimator();
            animator.init(panel);
            float diceX = startX + i * DICE_SPACING;
            float delay = (animators.size() + i) * STAGGER_DELAY;
            animator.start(types.get(i), results.get(i), diceX, startY, delay);
            animators.add(animator);
        }
    }

    public void clear() {
        for (DiceAnimator animator : animators) {
            animator.getNumberLabel().setOpacity(0f);
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
}