package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.ui.CustomPanelAPI;

import data.scripts.cosmicon.util.CosmiconLogger;

public class DiceRollManager {

    private static final Random rand = new Random();
    private static final float STAGGER_DELAY = 0.05f;
    private static final float DICE_SPACING = 130f;
    private static final float MIN_TRAVEL_DISTANCE = 50f;
    private static final float MAX_TRAVEL_DISTANCE = 500f;
    private static final float MIN_BOUNCE_HEIGHT = 1.1f;
    private static final float MAX_BOUNCE_HEIGHT = 1.3f;
    private static final int MAX_BOUNCES = 2;

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
        float totalWidth = DICE_SPACING * (count - 1) + DiceAnimator.DICE_SIZE;
        float startX = centerX - totalWidth / 2f;
        float startY = centerY - DiceAnimator.DICE_SIZE / 2f;
        
        for (int i = 0; i < count; i++) {
            DiceAnimator animator = new DiceAnimator();
            animator.init(panel);
            float diceX = startX + i * DICE_SPACING;
            float delay = i * STAGGER_DELAY;
            
            float rotation = rand.nextFloat() * 360f;
            float travelDistance = MIN_TRAVEL_DISTANCE + rand.nextFloat() * (MAX_TRAVEL_DISTANCE - MIN_TRAVEL_DISTANCE);
            int bounceCount = rand.nextInt(MAX_BOUNCES + 1);
            
            float[] bounceHeights = new float[bounceCount];
            for (int j = 0; j < bounceCount; j++) {
                float factor = 1f - (j * 0.2f);
                bounceHeights[j] = (MIN_BOUNCE_HEIGHT + rand.nextFloat() * (MAX_BOUNCE_HEIGHT - MIN_BOUNCE_HEIGHT)) * factor;
                bounceHeights[j] = Math.max(1.05f, bounceHeights[j]);
            }
            
            animator.start(types.get(i), results.get(i), diceX, startY, delay,
                           rotation, travelDistance, bounceCount, bounceHeights);
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

        float totalWidth = DICE_SPACING * (count - 1) + DiceAnimator.DICE_SIZE;
        float startX = centerX - totalWidth / 2f;
        float startY = centerY - DiceAnimator.DICE_SIZE / 2f;
        
        for (int i = 0; i < count; i++) {
            DiceAnimator animator = new DiceAnimator();
            animator.init(panel);
            float diceX = startX + i * DICE_SPACING;
            float delay = (animators.size() + i) * STAGGER_DELAY;
            
            float rotation = rand.nextFloat() * 360f;
            float travelDistance = MIN_TRAVEL_DISTANCE + rand.nextFloat() * (MAX_TRAVEL_DISTANCE - MIN_TRAVEL_DISTANCE);
            int bounceCount = rand.nextInt(MAX_BOUNCES + 1);
            
            float[] bounceHeights = new float[bounceCount];
            for (int j = 0; j < bounceCount; j++) {
                float factor = 1f - (j * 0.2f);
                bounceHeights[j] = (MIN_BOUNCE_HEIGHT + rand.nextFloat() * (MAX_BOUNCE_HEIGHT - MIN_BOUNCE_HEIGHT)) * factor;
                bounceHeights[j] = Math.max(1.05f, bounceHeights[j]);
            }
            
            animator.start(types.get(i), results.get(i), diceX, startY, delay,
                           rotation, travelDistance, bounceCount, bounceHeights);
            animators.add(animator);
        }
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
        float totalWidth = DICE_SPACING * (count - 1) + DiceAnimator.DICE_SIZE;
        float startX = zoneX + (BattleRenderingUtils.OPPONENT_DICE_ZONE_W - totalWidth) / 2f;
        float startY = zoneY + (BattleRenderingUtils.OPPONENT_DICE_ZONE_H - DiceAnimator.DICE_SIZE) / 2f;
        
        for (int i = 0; i < count; i++) {
            DiceAnimator animator = new DiceAnimator();
            animator.init(panel);
            float diceX = startX + i * DICE_SPACING;
            float delay = i * STAGGER_DELAY;
            
            float rotation = rand.nextFloat() * 360f;
            float travelDistance = MIN_TRAVEL_DISTANCE + rand.nextFloat() * (MAX_TRAVEL_DISTANCE - MIN_TRAVEL_DISTANCE);
            int bounceCount = rand.nextInt(MAX_BOUNCES + 1);
            
            float[] bounceHeights = new float[bounceCount];
            for (int j = 0; j < bounceCount; j++) {
                float factor = 1f - (j * 0.2f);
                bounceHeights[j] = (MIN_BOUNCE_HEIGHT + rand.nextFloat() * (MAX_BOUNCE_HEIGHT - MIN_BOUNCE_HEIGHT)) * factor;
                bounceHeights[j] = Math.max(1.05f, bounceHeights[j]);
            }
            
            animator.start(types.get(i), results.get(i), diceX, startY, delay,
                           rotation, travelDistance, bounceCount, bounceHeights);
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