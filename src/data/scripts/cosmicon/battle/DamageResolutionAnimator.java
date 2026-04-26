package data.scripts.cosmicon.battle;

import java.awt.Color;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;

import data.scripts.cosmicon.util.ColorHelper;

public class DamageResolutionAnimator {
    private static final float ICON_PREP_DURATION = 0.5f;
    private static final float ICON_CLASH_DURATION = 0.8f;
    private static final float ICON_IMPACT_DURATION = 0.6f;
    private static final float WINNER_IMPACT_DURATION = 0.6f;
    private static final float ICON_RETREAT_DURATION = 0.5f;
    private static final float SHATTER_RESTORE_DURATION = 0.4f;
    private static final float RESULT_FLIGHT_DURATION = 0.8f;
    private static final float IMPACT_FLASH_DURATION = 0.5f;
    private static final float POST_IMPACT_PAUSE_DURATION = 1.0f;
    private static final float COMBO_PAUSE_DURATION = 0.5f;
    private static final float DEFENDER_PULSE_DURATION = 0.4f;
    
    private static final Color DAMAGE_RESULT_COLOR = FlyingNumber.DAMAGE_RESULT;
    private static final Color DEFENDER_PULSE_COLOR = new Color(80, 200, 120);
    
    public enum Phase {
        IDLE,
        ICON_PREPARATION,
        ICON_CLASH,
        WAIT_FOR_CLASH_CLICK,
        ICON_IMPACT,
        WAIT_FOR_IMPACT_CLICK,
        WINNER_IMPACT,
        ICON_RETREAT,
        SHATTER_RESTORE,
        RESULT_FLIGHT,
        IMPACT_FLASH,
        DEFENDER_PULSE,
        POST_IMPACT_PAUSE,
        COMBO_PAUSE,
        COMBO_SECOND_CLASH,
        COMBO_SECOND_IMPACT,
        COMPLETE
    }
    
    private FlyingNumber atkNumber;
    private FlyingNumber defNumber;
    private FlyingNumber resultNumber;
    private FlyingNumber comboResultNumber;
    private final ImpactEffect impactEffect;
    private final IconShatterEffect shatterEffect;
    
    private FlyingIcon atkFlyingIcon;
    private FlyingIcon defFlyingIcon;

    private Phase phase;
    private float phaseElapsed;
    private boolean complete;
    private boolean waitingForClick;
    
    private int attackValue;
    private int defenseValue;
    private int resultValue;
    private boolean perforation;
    private boolean combo;
    private boolean attackWins;
    private boolean isDraw;
    
    private float centerX;
    private float centerY;
    private float defenderTargetX;
    private float defenderTargetY;
    
    private float atkIconCenterX;
    private float atkIconCenterY;
    private float defIconCenterX;
    private float defIconCenterY;
    private float iconSize;

    private int comboDamage;
    private float shatterRestoreAlpha;
    
    private CustomPanelAPI panel;
    
    public DamageResolutionAnimator() {
        phase = Phase.IDLE;
        phaseElapsed = 0f;
        complete = false;
        waitingForClick = false;
        shatterRestoreAlpha = 0f;
        impactEffect = new ImpactEffect();
        shatterEffect = new IconShatterEffect();
    }
    
    public void startResolution(
            BattleState state,
            float defenderTargetX,
            float defenderTargetY,
            CustomPanelAPI panel) {
        
        this.panel = panel;
        this.defenderTargetX = defenderTargetX;
        this.defenderTargetY = defenderTargetY;
        
        this.centerX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        this.centerY = BattleRenderingUtils.PANEL_HEIGHT / 2f;
        
        this.attackValue = state.getAttackValue();
        this.defenseValue = state.getDefenseValue();
        
        StatusEffectProcessor attackerEffects = state.isPlayerAttacker() 
            ? state.getPlayerEffects() : state.getOpponentEffects();
        StatusEffectProcessor defenderEffects = state.isPlayerAttacker() 
            ? state.getOpponentEffects() : state.getPlayerEffects();
        
        this.attackValue += attackerEffects.calculateAttackBonus(BattleState.TurnType.ATTACK);
        this.attackValue += state.getPrismaticDiceTotalValue(state.isPlayerAttacker());
        
        this.defenseValue += defenderEffects.calculateDefenseBonus(BattleState.TurnType.DEFENSE);
        this.defenseValue += state.getPrismaticDiceTotalValue(!state.isPlayerAttacker());
        
        this.perforation = attackerEffects.shouldIgnoreDefense();
        
        if (perforation) {
            this.defenseValue = 0;
        }
        
        this.resultValue = Math.max(0, attackValue - defenseValue);
        
        this.isDraw = (resultValue == 0 && !perforation);
        
        this.attackWins = attackValue > defenseValue;
        
        this.combo = hasCombo(state);
        if (combo) {
            this.comboDamage = resultValue;
        } else {
            this.comboDamage = 0;
        }
        
        cleanup();
        
        calculateStaticIconPositions(state);
        createFlyingNumbers();
        
        atkFlyingIcon.startFrom(atkIconCenterX, atkIconCenterY);
        defFlyingIcon.startFrom(defIconCenterX, defIconCenterY);
        atkFlyingIcon.createLabel(panel);
        defFlyingIcon.createLabel(panel);
        
        phase = Phase.ICON_PREPARATION;
        phaseElapsed = 0f;
        complete = false;
        waitingForClick = false;
    }
    
    private void calculateStaticIconPositions(BattleState state) {
        float halfH = BattleRenderingUtils.PANEL_HEIGHT / 2f;
        iconSize = halfH * BattleRenderingUtils.ROLE_ICON_SIZE_RATIO;
        
        float topIconCenterX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float topIconCenterY = (halfH - iconSize) / 2f + iconSize / 2f;
        
        float bottomIconCenterX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float bottomIconCenterY = halfH + (halfH - iconSize) / 2f + iconSize / 2f;
        
        if (state.isPlayerAttacker()) {
            atkIconCenterX = bottomIconCenterX;
            atkIconCenterY = bottomIconCenterY;
            defIconCenterX = topIconCenterX;
            defIconCenterY = topIconCenterY;
        } else {
            atkIconCenterX = topIconCenterX;
            atkIconCenterY = topIconCenterY;
            defIconCenterX = bottomIconCenterX;
            defIconCenterY = bottomIconCenterY;
        }
    }
    
    private boolean hasCombo(BattleState state) {
        return state.hasCombo();
    }
    
    private void createFlyingNumbers() {
        atkNumber = new FlyingNumber();
        atkNumber.setValue(attackValue);
        atkNumber.setColor(ColorHelper.ATTACK_VALUE);
        
        defNumber = new FlyingNumber();
        defNumber.setValue(defenseValue);
        defNumber.setColor(ColorHelper.DEFENSE_VALUE);
        
        if (perforation && defenseValue > 0) {
            defNumber.setShatterOnImpact(true);
        }
        
        resultNumber = new FlyingNumber();
        comboResultNumber = null;

        SpriteAPI atkRoleIcon = CosmiconSprites.getAtkIcon();
        SpriteAPI defRoleIcon = CosmiconSprites.getDefIcon();
        
        atkFlyingIcon = new FlyingIcon(atkRoleIcon, iconSize, ColorHelper.ATTACK_VALUE);
        atkFlyingIcon.setValue(attackValue);
        
        defFlyingIcon = new FlyingIcon(defRoleIcon, iconSize, ColorHelper.DEFENSE_VALUE);
        defFlyingIcon.setValue(defenseValue);
    }
    
    public void advance(float amount) {
        if (phase == Phase.IDLE || phase == Phase.COMPLETE) return;
        
        if (waitingForClick) return;
        
        phaseElapsed += amount;
        
        switch (phase) {
            case ICON_PREPARATION -> advanceIconPreparation();
            case ICON_CLASH -> advanceIconClash();
            case ICON_IMPACT -> advanceIconImpact();
            case WINNER_IMPACT -> advanceWinnerImpact();
            case ICON_RETREAT -> advanceIconRetreat();
            case SHATTER_RESTORE -> advanceShatterRestore();
            case IMPACT_FLASH -> advanceImpactFlash();
            case DEFENDER_PULSE -> advanceDefenderPulse();
            case POST_IMPACT_PAUSE -> advancePostImpactPause();
            case COMBO_PAUSE -> advanceComboPause();
            case COMBO_SECOND_CLASH -> advanceComboSecondClash();
            case COMBO_SECOND_IMPACT -> advanceComboSecondImpact();
        }
        
        advanceFlyingNumbers(amount);
        
        impactEffect.advance(amount);
        shatterEffect.advance(amount);
    }
    
    private void advanceFlyingNumbers(float amount) {
        if (atkNumber != null) {
            atkNumber.advance(amount);
        }
        if (defNumber != null) {
            defNumber.advance(amount);
        }
        if (resultNumber != null) {
            resultNumber.advance(amount);
        }
        if (comboResultNumber != null) {
            comboResultNumber.advance(amount);
        }
        if (atkFlyingIcon != null) {
            atkFlyingIcon.advance(amount);
        }
        if (defFlyingIcon != null) {
            defFlyingIcon.advance(amount);
        }
    }
    
    private void advanceIconPreparation() {
        if (phaseElapsed >= ICON_PREP_DURATION) {
            startIconClash();
            phase = Phase.ICON_CLASH;
            phaseElapsed = 0f;
        }
    }
    
    private void startIconClash() {
        atkFlyingIcon.flyTo(centerX, centerY, ICON_CLASH_DURATION);
        defFlyingIcon.flyTo(centerX, centerY, ICON_CLASH_DURATION);
    }
    
    private void advanceIconClash() {
        if (phaseElapsed >= ICON_CLASH_DURATION || 
            (atkFlyingIcon.isComplete() && defFlyingIcon.isComplete())) {
            
            phase = Phase.WAIT_FOR_CLASH_CLICK;
            waitingForClick = true;
            phaseElapsed = 0f;
        }
    }
    
    private void triggerIconImpact() {
        impactEffect.triggerFlash(centerX, centerY, 60f, new Color(255, 255, 200));
        
        if (!perforation) {
            impactEffect.triggerParticles(centerX, centerY, 8, new Color(255, 200, 100));
        }
        
        if (attackWins) {
            shatterEffect.trigger(centerX, centerY, 
                ColorHelper.DEFENSE_VALUE, iconSize);
            if (defFlyingIcon != null) {
                defFlyingIcon.setLabelOpacity(0f);
            }
        } else {
            shatterEffect.trigger(atkIconCenterX, atkIconCenterY, 
                ColorHelper.ATTACK_VALUE, iconSize);
            if (atkFlyingIcon != null) {
                atkFlyingIcon.setLabelOpacity(0f);
            }
        }
    }
    
    private void advanceIconImpact() {
        if (phaseElapsed >= ICON_IMPACT_DURATION) {
            phase = Phase.WAIT_FOR_IMPACT_CLICK;
            waitingForClick = true;
            phaseElapsed = 0f;
        }
    }
    
    private void proceedFromImpactWait() {
        if (attackWins) {
            atkFlyingIcon.setValue(resultValue);
            atkFlyingIcon.flyTo(defenderTargetX, defenderTargetY, WINNER_IMPACT_DURATION);
            phase = Phase.WINNER_IMPACT;
        } else {
            if (defFlyingIcon != null) {
                defFlyingIcon.flyTo(defIconCenterX, defIconCenterY, ICON_RETREAT_DURATION);
            }
            if (atkFlyingIcon != null) {
                atkFlyingIcon.startFrom(atkIconCenterX, atkIconCenterY);
            }
            phase = Phase.SHATTER_RESTORE;
        }
        phaseElapsed = 0f;
    }
    
    private void advanceWinnerImpact() {
        boolean atkDone = atkFlyingIcon == null || atkFlyingIcon.isComplete();
        boolean defDone = defFlyingIcon == null || defFlyingIcon.isComplete();
        
        if (phaseElapsed >= WINNER_IMPACT_DURATION || (atkDone && defDone)) {
            impactEffect.triggerFlash(defenderTargetX, defenderTargetY, 40f, DAMAGE_RESULT_COLOR);
            impactEffect.triggerShockwave(defenderTargetX, defenderTargetY);
            
            if (resultValue > 0) {
                impactEffect.triggerParticles(defenderTargetX, defenderTargetY, 6, DAMAGE_RESULT_COLOR);
            }
            
            startResultFlight();
            startIconRetreat();
            phase = Phase.ICON_RETREAT;
            phaseElapsed = 0f;
        }
    }
    
    private void startResultFlight() {
        resultNumber.setValue(resultValue);
        resultNumber.setColor(DAMAGE_RESULT_COLOR);
        resultNumber.startFrom(defenderTargetX, defenderTargetY);
        resultNumber.flyTo(defenderTargetX, defenderTargetY - 30f, RESULT_FLIGHT_DURATION);
    }
    
    private void startIconRetreat() {
        if (attackWins) {
            if (atkFlyingIcon != null) {
                atkFlyingIcon.flyTo(atkIconCenterX, atkIconCenterY, ICON_RETREAT_DURATION);
            }
        } else {
            if (defFlyingIcon != null) {
                defFlyingIcon.flyTo(defIconCenterX, defIconCenterY, ICON_RETREAT_DURATION);
            }
        }
    }
    
    private void advanceIconRetreat() {
        boolean atkDone = atkFlyingIcon == null || atkFlyingIcon.isComplete();
        boolean defDone = defFlyingIcon == null || defFlyingIcon.isComplete();
        
        if (phaseElapsed >= ICON_RETREAT_DURATION || (atkDone && defDone)) {
            phase = Phase.SHATTER_RESTORE;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceShatterRestore() {
        shatterRestoreAlpha = Math.min(phaseElapsed / SHATTER_RESTORE_DURATION, 1f);
        
        boolean atkDone = atkFlyingIcon == null || atkFlyingIcon.isComplete();
        boolean defDone = defFlyingIcon == null || defFlyingIcon.isComplete();
        
        if (phaseElapsed >= SHATTER_RESTORE_DURATION && atkDone && defDone) {
            shatterRestoreAlpha = 1f;
            
            boolean atkShattered = !attackWins || isDraw;
            if (atkShattered && atkFlyingIcon != null) {
                atkFlyingIcon.startFrom(atkIconCenterX, atkIconCenterY);
                atkFlyingIcon.setLabelOpacity(1f);
            }
            if (!atkShattered && defFlyingIcon != null) {
                defFlyingIcon.startFrom(defIconCenterX, defIconCenterY);
                defFlyingIcon.setLabelOpacity(1f);
            }
            
            if (isDraw) {
                resultNumber.setValue(0);
                resultNumber.setColor(DEFENDER_PULSE_COLOR);
            }
            phase = Phase.POST_IMPACT_PAUSE;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceImpactFlash() {
        if (phaseElapsed >= IMPACT_FLASH_DURATION) {
            phase = Phase.POST_IMPACT_PAUSE;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceDefenderPulse() {
        if (phaseElapsed >= DEFENDER_PULSE_DURATION) {
            phase = Phase.POST_IMPACT_PAUSE;
            phaseElapsed = 0f;
        }
    }
    
    private void advancePostImpactPause() {
        if (phaseElapsed >= POST_IMPACT_PAUSE_DURATION) {
            if (combo && comboDamage > 0) {
                phase = Phase.COMBO_PAUSE;
                phaseElapsed = 0f;
            } else {
                phase = Phase.COMPLETE;
                complete = true;
            }
        }
    }
    
    private void advanceComboPause() {
        if (phaseElapsed >= COMBO_PAUSE_DURATION) {
            comboResultNumber = new FlyingNumber();
            comboResultNumber.setValue(comboDamage);
            comboResultNumber.setColor(DAMAGE_RESULT_COLOR);
            comboResultNumber.startFrom(centerX, centerY);
            comboResultNumber.flyTo(defenderTargetX, defenderTargetY, RESULT_FLIGHT_DURATION);
            
            phase = Phase.COMBO_SECOND_CLASH;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceComboSecondClash() {
        if (phaseElapsed >= RESULT_FLIGHT_DURATION || 
            (comboResultNumber != null && comboResultNumber.hasImpacted())) {
            
            impactEffect.triggerFlash(defenderTargetX, defenderTargetY, 40f, DAMAGE_RESULT_COLOR);
            impactEffect.triggerShockwave(defenderTargetX, defenderTargetY);
            
            if (comboDamage > 0) {
                impactEffect.triggerParticles(defenderTargetX, defenderTargetY, 6, DAMAGE_RESULT_COLOR);
            }
            
            phase = Phase.COMBO_SECOND_IMPACT;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceComboSecondImpact() {
        if (phaseElapsed >= IMPACT_FLASH_DURATION) {
            phase = Phase.COMPLETE;
            complete = true;
        }
    }
    
    public void render(float panelX, float panelY, float panelHeight, float alphaMult) {
        if (phase == Phase.IDLE) return;
        
        renderNumbersOnIcons(panelX, panelY, panelHeight, alphaMult);
        
        shatterEffect.render(panelX, panelY, panelHeight, alphaMult);
        
        renderResultNumbers(panelX, panelY, panelHeight, alphaMult);
        
        impactEffect.render(panelX, panelY, panelHeight, alphaMult);
    }
    
    private void renderNumbersOnIcons(float panelX, float panelY, float panelHeight, float alphaMult) {
        boolean shouldRenderNumbers = phase == Phase.ICON_PREPARATION ||
            phase == Phase.ICON_CLASH ||
            phase == Phase.WAIT_FOR_CLASH_CLICK ||
            phase == Phase.ICON_IMPACT ||
            phase == Phase.WAIT_FOR_IMPACT_CLICK ||
            phase == Phase.WINNER_IMPACT ||
            phase == Phase.ICON_RETREAT ||
            phase == Phase.SHATTER_RESTORE;
        
        if (!shouldRenderNumbers) return;
        
        boolean duringClash = phase == Phase.ICON_PREPARATION ||
                              phase == Phase.ICON_CLASH ||
                              phase == Phase.WAIT_FOR_CLASH_CLICK;
        
        if (duringClash) {
            if (atkFlyingIcon != null) {
                atkFlyingIcon.render(panelX, panelY, panelHeight, alphaMult);
                atkFlyingIcon.setLabelOpacity(alphaMult);
            }
            if (defFlyingIcon != null) {
                defFlyingIcon.render(panelX, panelY, panelHeight, alphaMult);
                defFlyingIcon.setLabelOpacity(alphaMult);
            }
            return;
        }
        
        boolean atkShattered = !attackWins || isDraw;
        boolean defShattered = attackWins;
        boolean restoring = phase == Phase.SHATTER_RESTORE;
        
        if (atkFlyingIcon != null && (!atkShattered || restoring)) {
            float restoreAlpha = atkShattered ? shatterRestoreAlpha : alphaMult;
            atkFlyingIcon.render(panelX, panelY, panelHeight, restoreAlpha);
            atkFlyingIcon.setLabelOpacity(restoreAlpha);
        }
        if (defFlyingIcon != null && (!defShattered || restoring)) {
            float restoreAlpha = defShattered ? shatterRestoreAlpha : alphaMult;
            defFlyingIcon.render(panelX, panelY, panelHeight, restoreAlpha);
            defFlyingIcon.setLabelOpacity(restoreAlpha);
        }
    }
    
    public boolean isIconClashActive() {
        return phase == Phase.ICON_PREPARATION ||
               phase == Phase.ICON_CLASH ||
               phase == Phase.WAIT_FOR_CLASH_CLICK ||
               phase == Phase.ICON_IMPACT ||
               phase == Phase.WAIT_FOR_IMPACT_CLICK ||
               phase == Phase.WINNER_IMPACT ||
               phase == Phase.ICON_RETREAT ||
               phase == Phase.SHATTER_RESTORE;
    }
    
    private void renderResultNumbers(float panelX, float panelY, float panelHeight, float alphaMult) {
        if (resultNumber != null && shouldRenderResultNumber()) {
            resultNumber.render(panelX, panelY, panelHeight, alphaMult, panel);
        }
        if (comboResultNumber != null) {
            comboResultNumber.render(panelX, panelY, panelHeight, alphaMult, panel);
        }
    }
    
    private boolean shouldRenderResultNumber() {
        return phase != Phase.ICON_PREPARATION && 
            phase != Phase.ICON_CLASH && 
            phase != Phase.WAIT_FOR_CLASH_CLICK &&
            phase != Phase.ICON_IMPACT &&
            phase != Phase.WAIT_FOR_IMPACT_CLICK &&
            phase != Phase.WINNER_IMPACT;
    }
    
    public boolean isComplete() {
        return phase == Phase.COMPLETE && complete;
    }
    
    public void forceComplete() {
        waitingForClick = false;
        
        if (phase == Phase.WAIT_FOR_CLASH_CLICK) {
            triggerIconImpact();
            phase = Phase.ICON_IMPACT;
            phaseElapsed = 0f;
            return;
        }
        
        if (phase == Phase.WAIT_FOR_IMPACT_CLICK) {
            proceedFromImpactWait();
            return;
        }
        
        if (phase == Phase.IDLE) {
            return;
        }
        
        if (phase == Phase.ICON_PREPARATION || phase == Phase.ICON_CLASH) {
            triggerIconImpact();
            phase = Phase.COMPLETE;
            complete = true;
        } else if (phase == Phase.ICON_IMPACT || phase == Phase.WINNER_IMPACT ||
                   phase == Phase.ICON_RETREAT || phase == Phase.SHATTER_RESTORE) {
            proceedFromImpactWait();
            if (!attackWins || isDraw) {
                phase = Phase.COMPLETE;
                complete = true;
            }
        } else if (phase == Phase.RESULT_FLIGHT || phase == Phase.IMPACT_FLASH || 
                   phase == Phase.DEFENDER_PULSE || phase == Phase.POST_IMPACT_PAUSE ||
                   phase == Phase.COMBO_PAUSE || phase == Phase.COMBO_SECOND_CLASH ||
                   phase == Phase.COMBO_SECOND_IMPACT) {
            phase = Phase.COMPLETE;
            complete = true;
        }
        
        phaseElapsed = 0f;
        
        if (atkNumber != null) {
            atkNumber.forceComplete();
        }
        if (defNumber != null) {
            defNumber.forceComplete();
        }
        if (resultNumber != null) {
            resultNumber.forceComplete();
        }
        if (comboResultNumber != null) {
            comboResultNumber.forceComplete();
        }
        if (atkFlyingIcon != null) {
            atkFlyingIcon.setLabelOpacity(0f);
        }
        if (defFlyingIcon != null) {
            defFlyingIcon.setLabelOpacity(0f);
        }
        
        impactEffect.clear();
        shatterEffect.clear();
    }
    
    public void cleanup() {
        if (atkNumber != null) {
            atkNumber.cleanup();
            atkNumber = null;
        }
        if (defNumber != null) {
            defNumber.cleanup();
            defNumber = null;
        }
        if (resultNumber != null) {
            resultNumber.cleanup();
            resultNumber = null;
        }
        if (comboResultNumber != null) {
            comboResultNumber.cleanup();
            comboResultNumber = null;
        }
        if (atkFlyingIcon != null) {
            atkFlyingIcon.cleanup();
            atkFlyingIcon = null;
        }
        if (defFlyingIcon != null) {
            defFlyingIcon.cleanup();
            defFlyingIcon = null;
        }
        
        impactEffect.clear();
        shatterEffect.clear();
        
        phase = Phase.IDLE;
        phaseElapsed = 0f;
        complete = false;
        waitingForClick = false;
        shatterRestoreAlpha = 0f;
    }
    
    public boolean isPerforation() {
        return perforation;
    }
    
    public Phase getPhase() {
        return phase;
    }
    
    public boolean isDraw() {
        return isDraw;
    }
}