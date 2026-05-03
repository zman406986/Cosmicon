package data.scripts.cosmicon.battle;

import java.awt.Color;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;

import data.scripts.cosmicon.util.ColorHelper;

public class DamageResolutionAnimator {
    private static final float ICON_CLASH_DURATION = 0.8f;
    private static final float ICON_ROTATION_DURATION = 0.3f;
    private static final float ICON_IMPACT_DURATION = 0.6f;
    private static final float WINNER_IMPACT_DURATION = 0.6f;
    private static final float ICON_RETREAT_DURATION = 0.5f;
    private static final float SHATTER_RESTORE_DURATION = 0.4f;
    private static final float RESULT_FLIGHT_DURATION = 0.8f;
    private static final float IMPACT_FLASH_DURATION = 0.5f;
    private static final float POST_IMPACT_PAUSE_DURATION = 1.0f;
    private static final float COMBO_PAUSE_DURATION = 0.5f;
    private static final float COMBO_ICON_FLY_DURATION = 0.6f;
    private static final float DEFENDER_PULSE_DURATION = 0.4f;
    
    private static final float ATK_ROTATION_TO_TOP_LEFT = -90f;
    private static final float ATK_ROTATION_TO_BOTTOM_RIGHT = 90f;
    
    private static final Color DAMAGE_RESULT_COLOR = FlyingNumber.DAMAGE_RESULT;
    private static final Color DEFENDER_PULSE_COLOR = new Color(80, 200, 120);
    
    private boolean playerAttacker;
    
    public enum Phase {
        IDLE,
        ICON_PREPARATION,
        ICON_ROTATION,
        ICON_CLASH,
        ICON_IMPACT,
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
        COMBO_ICON_RETREAT,
        COMPLETE
    }
    
    private FlyingNumber atkNumber;
    private FlyingNumber defNumber;
    private FlyingNumber resultNumber;
    private FlyingNumber comboResultNumber;
    private final ImpactEffect impactEffect;
    private final IconShatterEffect shatterEffect;
    private final IconSplitEffect splitEffect;
    
    private FlyingIcon atkFlyingIcon;
    private FlyingIcon defFlyingIcon;

    private Phase phase;
    private float phaseElapsed;
    private boolean complete;
    
    private int attackValue;
    private int defenseValue;
    private int originalDefenseValue;
    private int resultValue;
    private boolean perforation;
    private boolean combo;
    private boolean comboWillHit;
    private boolean attackWins;
    private boolean isDraw;
    
    private float centerX;
    private float centerY;
    private float defenderTargetX;
    private float defenderTargetY;
    
    private float atkClashX;
    private float atkClashY;
    private float defClashX;
    private float defClashY;
    
    private float atkIconCenterX;
    private float atkIconCenterY;
    private float defIconCenterX;
    private float defIconCenterY;
    private float iconSize;

    private int comboDamage;
    private float shatterRestoreAlpha;
    private boolean damageImpacted;

    private CustomPanelAPI panel;
    
    public DamageResolutionAnimator() {
        phase = Phase.IDLE;
        phaseElapsed = 0f;
        complete = false;
        shatterRestoreAlpha = 0f;
        damageImpacted = false;
        impactEffect = new ImpactEffect();
        shatterEffect = new IconShatterEffect();
        splitEffect = new IconSplitEffect();
    }
    
    public void startResolution(
            BattleState state,
            float defenderTargetX,
            float defenderTargetY,
            CustomPanelAPI panel) {
        
        cleanup();
        
        this.panel = panel;
        this.defenderTargetX = defenderTargetX;
        this.defenderTargetY = defenderTargetY;
        
        this.centerX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        this.centerY = BattleRenderingUtils.PANEL_HEIGHT / 2f;
        
        this.playerAttacker = state.isPlayerAttacker();
        
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
        
        this.originalDefenseValue = this.defenseValue;
        
        this.perforation = attackerEffects.shouldIgnoreDefense();
        
        if (perforation) {
            this.defenseValue = 0;
        }
        
        this.resultValue = Math.max(0, attackValue - defenseValue);
        
        boolean primaryDamageTriggersForcefield = defenderEffects.isForcefieldActive() && resultValue > 0;
        if (primaryDamageTriggersForcefield) {
            resultValue = 0;
        }
        
        this.isDraw = (resultValue == 0 && !perforation);
        
        this.attackWins = attackValue > defenseValue;
        
        this.combo = hasCombo(state);
        if (combo) {
            int attackerPrismaticValue = state.getPrismaticDiceTotalValue(state.isPlayerAttacker());
            this.comboDamage = attackValue - attackerPrismaticValue;
            if (!primaryDamageTriggersForcefield && defenderEffects.isForcefieldActive() && this.comboDamage > 0) {
                this.comboDamage = 0;
            }
        } else {
            this.comboDamage = 0;
        }
        
        this.comboWillHit = combo && comboDamage > 0;
        
        calculateStaticIconPositions(state);
        createFlyingNumbers();
        
        atkFlyingIcon.createLabel(panel);
        defFlyingIcon.createLabel(panel);
        
        if (attackWins) {
            atkFlyingIcon.startRotation(playerAttacker ? ATK_ROTATION_TO_BOTTOM_RIGHT : ATK_ROTATION_TO_TOP_LEFT);
        }
        
        phase = Phase.ICON_PREPARATION;
        phaseElapsed = 0f;
        complete = false;
        damageImpacted = false;
    }
    
    private void calculateStaticIconPositions(BattleState state) {
        float halfH = BattleRenderingUtils.PANEL_HEIGHT / 2f;
        iconSize = halfH * BattleRenderingUtils.ROLE_ICON_SIZE_RATIO;
        
        float topIconCenterX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float topIconCenterY = (halfH - iconSize) / 2f + iconSize / 2f;
        
        float bottomIconCenterX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float bottomIconCenterY = halfH + (halfH - iconSize) / 2f + iconSize / 2f;
        
        float clashOffset = iconSize * 0.25f;
        
        if (state.isPlayerAttacker()) {
            atkIconCenterX = bottomIconCenterX;
            atkIconCenterY = bottomIconCenterY;
            defIconCenterX = topIconCenterX;
            defIconCenterY = topIconCenterY;
            
            atkClashX = centerX;
            atkClashY = centerY + clashOffset;
            defClashX = centerX;
            defClashY = centerY - clashOffset;
        } else {
            atkIconCenterX = topIconCenterX;
            atkIconCenterY = topIconCenterY;
            defIconCenterX = bottomIconCenterX;
            defIconCenterY = bottomIconCenterY;
            
            atkClashX = centerX;
            atkClashY = centerY - clashOffset;
            defClashX = centerX;
            defClashY = centerY + clashOffset;
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
        defNumber.setValue(originalDefenseValue);
        defNumber.setColor(ColorHelper.DEFENSE_VALUE);
        
        if (perforation && originalDefenseValue > 0) {
            defNumber.setShatterOnImpact(true);
        }
        
        resultNumber = new FlyingNumber();
        comboResultNumber = null;

        SpriteAPI atkRoleIcon = CosmiconSprites.getAtkIcon();
        SpriteAPI defRoleIcon = CosmiconSprites.getDefIcon();
        
        atkFlyingIcon = new FlyingIcon(atkRoleIcon, iconSize, ColorHelper.ATTACK_VALUE, atkIconCenterX, atkIconCenterY);
        atkFlyingIcon.setValue(attackValue);
        
        defFlyingIcon = new FlyingIcon(defRoleIcon, iconSize, ColorHelper.DEFENSE_VALUE, defIconCenterX, defIconCenterY);
        defFlyingIcon.setValue(originalDefenseValue);
    }
    
    public void advance(float amount) {
        if (phase == Phase.IDLE || phase == Phase.COMPLETE) return;
        
        phaseElapsed += amount;
        
        switch (phase) {
            case ICON_PREPARATION -> advanceIconPreparation();
            case ICON_ROTATION -> advanceIconRotation();
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
            case COMBO_ICON_RETREAT -> advanceComboIconRetreat();
        }
        
        advanceFlyingNumbers(amount);
        
        impactEffect.advance(amount);
        shatterEffect.advance(amount);
        splitEffect.advance(amount);
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
        phase = Phase.ICON_ROTATION;
        phaseElapsed = 0f;
    }
    
    private void advanceIconRotation() {
        boolean atkDone = atkFlyingIcon == null || !atkFlyingIcon.isRotating();
        boolean defDone = defFlyingIcon == null || !defFlyingIcon.isRotating();
        
        if (phaseElapsed >= ICON_ROTATION_DURATION || (atkDone && defDone)) {
            if (atkFlyingIcon != null) {
                atkFlyingIcon.flyTo(atkClashX, atkClashY, ICON_CLASH_DURATION, true, true);
            }
            if (defFlyingIcon != null) {
                defFlyingIcon.flyTo(defClashX, defClashY, ICON_CLASH_DURATION, true, true);
            }
            
            phase = Phase.ICON_CLASH;
            phaseElapsed = 0f;
            startIconClash();
        }
    }
    
    private void startIconClash() {
        if (atkFlyingIcon != null) {
            atkFlyingIcon.startFlight();
        }
        if (defFlyingIcon != null) {
            defFlyingIcon.startFlight();
        }
    }
    
    private void advanceIconClash() {
        boolean atkDone = atkFlyingIcon == null || atkFlyingIcon.isComplete();
        boolean defDone = defFlyingIcon == null || defFlyingIcon.isComplete();
        
        if (phaseElapsed >= ICON_CLASH_DURATION || (atkDone && defDone)) {
            
            triggerIconImpact();
            phase = Phase.ICON_IMPACT;
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
            splitEffect.trigger(
                defFlyingIcon != null ? defFlyingIcon.getSprite() : CosmiconSprites.getDefIcon(),
                defClashX, defClashY, ColorHelper.DEFENSE_VALUE, iconSize);
            if (defFlyingIcon != null) {
                defFlyingIcon.setLabelOpacity(0f);
            }
        } else if (!comboWillHit) {
            shatterEffect.trigger(centerX, centerY, 
                ColorHelper.ATTACK_VALUE, iconSize);
            splitEffect.trigger(
                atkFlyingIcon != null ? atkFlyingIcon.getSprite() : CosmiconSprites.getAtkIcon(),
                atkClashX, atkClashY, ColorHelper.ATTACK_VALUE, iconSize);
            if (atkFlyingIcon != null) {
                atkFlyingIcon.setLabelOpacity(0f);
            }
        }
    }
    
    private void advanceIconImpact() {
        if (phaseElapsed >= ICON_IMPACT_DURATION) {
            proceedFromImpactWait();
            phaseElapsed = 0f;
        }
    }
    
    private void proceedFromImpactWait() {
        if (attackWins) {
            atkFlyingIcon.setValue(resultValue);
            atkFlyingIcon.flyDirectTo(defenderTargetX, defenderTargetY, WINNER_IMPACT_DURATION);
            phase = Phase.WINNER_IMPACT;
        } else if (comboWillHit) {
            if (defFlyingIcon != null) {
                defFlyingIcon.flyDirectTo(defIconCenterX, defIconCenterY, ICON_RETREAT_DURATION);
            }
            if (atkFlyingIcon != null) {
                atkFlyingIcon.flyDirectTo(atkIconCenterX, atkIconCenterY, ICON_RETREAT_DURATION);
            }
            phase = Phase.ICON_RETREAT;
        } else {
            if (defFlyingIcon != null) {
                defFlyingIcon.flyDirectTo(defIconCenterX, defIconCenterY, ICON_RETREAT_DURATION);
            }
            if (atkFlyingIcon != null) {
                atkFlyingIcon.flyDirectTo(atkIconCenterX, atkIconCenterY, ICON_RETREAT_DURATION);
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

            damageImpacted = true;

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
                atkFlyingIcon.setTargetRotation(0f);
                atkFlyingIcon.flyDirectTo(atkIconCenterX, atkIconCenterY, ICON_RETREAT_DURATION);
            }
        } else {
            if (defFlyingIcon != null) {
                defFlyingIcon.flyDirectTo(defIconCenterX, defIconCenterY, ICON_RETREAT_DURATION);
            }
        }
    }
    
    private void advanceIconRetreat() {
        boolean atkDone = atkFlyingIcon == null || atkFlyingIcon.isComplete();
        boolean defDone = defFlyingIcon == null || defFlyingIcon.isComplete();
        
        if (phaseElapsed >= ICON_RETREAT_DURATION || (atkDone && defDone)) {
            if (comboWillHit && !attackWins) {
                phase = Phase.POST_IMPACT_PAUSE;
            } else {
                phase = Phase.SHATTER_RESTORE;
            }
            phaseElapsed = 0f;
        }
    }
    
    private void advanceShatterRestore() {
        if (splitEffect.isActive() && !splitEffect.isRestoring()) {
            splitEffect.startRestore(SHATTER_RESTORE_DURATION);
        }
        
        if (splitEffect.isRestoring()) {
            float progress = splitEffect.getRestoreProgress();
            shatterRestoreAlpha = progress;
        } else {
            shatterRestoreAlpha = 1f;
        }
        
        boolean atkDone = atkFlyingIcon == null || atkFlyingIcon.isComplete();
        boolean defDone = defFlyingIcon == null || defFlyingIcon.isComplete();
        boolean splitDone = !splitEffect.isActive();
        
        if (phaseElapsed >= SHATTER_RESTORE_DURATION && atkDone && defDone && splitDone) {
            shatterRestoreAlpha = 1f;
            
            boolean atkShattered = (!attackWins || isDraw) && !comboWillHit;
            if (atkShattered && atkFlyingIcon != null) {
                atkFlyingIcon.setRotation(0f);
                atkFlyingIcon.setLabelOpacity(1f);
            }
            if (!atkShattered && defFlyingIcon != null) {
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
                shatterEffect.clear();
                splitEffect.clear();
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
            
            if (atkFlyingIcon != null) {
                atkFlyingIcon.setValue(comboDamage);
                atkFlyingIcon.setLabelOpacity(0f);
                atkFlyingIcon.setTargetRotation(playerAttacker ? ATK_ROTATION_TO_BOTTOM_RIGHT : ATK_ROTATION_TO_TOP_LEFT);
                atkFlyingIcon.flyDirectTo(defenderTargetX, defenderTargetY, COMBO_ICON_FLY_DURATION);
            }
            
            phase = Phase.COMBO_SECOND_CLASH;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceComboSecondClash() {
        boolean iconDone = atkFlyingIcon == null || atkFlyingIcon.isComplete();
        boolean numberDone = comboResultNumber == null || comboResultNumber.hasImpacted();
        
        if ((phaseElapsed >= RESULT_FLIGHT_DURATION || numberDone) && iconDone) {
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
            if (atkFlyingIcon != null) {
                atkFlyingIcon.setTargetRotation(0f);
                atkFlyingIcon.flyDirectTo(atkIconCenterX, atkIconCenterY, ICON_RETREAT_DURATION);
            }
            phase = Phase.COMBO_ICON_RETREAT;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceComboIconRetreat() {
        boolean atkDone = atkFlyingIcon == null || atkFlyingIcon.isComplete();
        
        if (phaseElapsed >= ICON_RETREAT_DURATION || atkDone) {
            if (atkFlyingIcon != null) {
                atkFlyingIcon.setLabelOpacity(0f);
            }
            phase = Phase.COMPLETE;
            complete = true;
        }
    }
    
    public void render(float panelX, float panelY, float panelWidth, float panelHeight, float alphaMult) {
        if (phase == Phase.IDLE) return;
        
        boolean isComboPhase = phase == Phase.COMBO_PAUSE ||
            phase == Phase.COMBO_SECOND_CLASH ||
            phase == Phase.COMBO_SECOND_IMPACT ||
            phase == Phase.COMBO_ICON_RETREAT;
        
        renderNumbersOnIcons(panelX, panelY, panelWidth, panelHeight, alphaMult);
        
        if (!isComboPhase) {
            shatterEffect.render(panelX, panelY, panelWidth, panelHeight, alphaMult);
            splitEffect.render(panelX, panelY, panelWidth, panelHeight, alphaMult);
        }
        
        renderResultNumbers(panelX, panelY, panelHeight, alphaMult);
        
        impactEffect.render(panelX, panelY, panelWidth, panelHeight, alphaMult);
    }
    
    private void renderNumbersOnIcons(float panelX, float panelY, float panelWidth, float panelHeight, float alphaMult) {
        boolean shouldRenderNumbers = phase == Phase.ICON_PREPARATION ||
            phase == Phase.ICON_ROTATION ||
            phase == Phase.ICON_CLASH ||
            phase == Phase.ICON_IMPACT ||
            phase == Phase.WINNER_IMPACT ||
            phase == Phase.ICON_RETREAT ||
            phase == Phase.SHATTER_RESTORE;
        
        boolean isComboPhase = phase == Phase.COMBO_PAUSE ||
            phase == Phase.COMBO_SECOND_CLASH ||
            phase == Phase.COMBO_SECOND_IMPACT ||
            phase == Phase.COMBO_ICON_RETREAT;
        
        if (!shouldRenderNumbers && !isComboPhase) return;
        
        if (isComboPhase) {
            if (atkFlyingIcon != null) {
                atkFlyingIcon.render(panelX, panelY, panelWidth, panelHeight, alphaMult);
                atkFlyingIcon.setLabelOpacity(0f);
            }
            return;
        }
        
        boolean duringClash = phase == Phase.ICON_PREPARATION ||
                              phase == Phase.ICON_ROTATION ||
                              phase == Phase.ICON_CLASH ||
                              phase == Phase.ICON_IMPACT;
        
        if (duringClash) {
            boolean atkHiddenBySplit = splitEffect.isActive() && !attackWins;
            boolean defHiddenBySplit = splitEffect.isActive() && attackWins;
            
            if (atkFlyingIcon != null && !atkHiddenBySplit) {
                atkFlyingIcon.render(panelX, panelY, panelWidth, panelHeight, alphaMult);
                atkFlyingIcon.setLabelOpacity(alphaMult);
            }
            if (defFlyingIcon != null && !defHiddenBySplit) {
                defFlyingIcon.render(panelX, panelY, panelWidth, panelHeight, alphaMult);
                defFlyingIcon.setLabelOpacity(alphaMult);
            }
            return;
        }
        
        boolean atkShattered = (!attackWins || isDraw) && !comboWillHit;
        boolean defShattered = attackWins;
        boolean restoring = phase == Phase.SHATTER_RESTORE;
        boolean splitDone = !splitEffect.isActive();
        boolean splitRestoring = splitEffect.isRestoring();
        boolean atkHiddenBySplit = (splitEffect.isActive() && atkShattered) || (splitRestoring && atkShattered);
        boolean defHiddenBySplit = (splitEffect.isActive() && defShattered) || (splitRestoring && defShattered);
        
        if (atkFlyingIcon != null && !atkHiddenBySplit && (!atkShattered || (restoring && splitDone))) {
            float restoreAlpha = atkShattered ? shatterRestoreAlpha : alphaMult;
            atkFlyingIcon.render(panelX, panelY, panelWidth, panelHeight, restoreAlpha);
            atkFlyingIcon.setLabelOpacity(restoreAlpha);
        }
        if (defFlyingIcon != null && !defHiddenBySplit && (!defShattered || (restoring && splitDone))) {
            float restoreAlpha = defShattered ? shatterRestoreAlpha : alphaMult;
            defFlyingIcon.render(panelX, panelY, panelWidth, panelHeight, restoreAlpha);
            defFlyingIcon.setLabelOpacity(restoreAlpha);
        }
    }
    
    public boolean isIconClashActive() {
        return phase == Phase.ICON_PREPARATION ||
               phase == Phase.ICON_ROTATION ||
               phase == Phase.ICON_CLASH ||
               phase == Phase.ICON_IMPACT ||
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
            phase != Phase.ICON_IMPACT &&
            phase != Phase.WINNER_IMPACT;
    }
    
    public boolean isComplete() {
        return phase == Phase.COMPLETE && complete;
    }
    
    public void forceComplete() {
        if (phase == Phase.IDLE) {
            return;
        }
        
        if (phase == Phase.ICON_PREPARATION || phase == Phase.ICON_ROTATION || phase == Phase.ICON_CLASH) {
            triggerIconImpact();
            phase = Phase.COMPLETE;
            complete = true;
        } else if (phase == Phase.ICON_IMPACT || phase == Phase.WINNER_IMPACT ||
                   phase == Phase.ICON_RETREAT || phase == Phase.SHATTER_RESTORE) {
            if (!attackWins || isDraw) {
                proceedFromImpactWait();
            }
            phase = Phase.COMPLETE;
            complete = true;
        } else if (phase == Phase.RESULT_FLIGHT || phase == Phase.IMPACT_FLASH || 
                   phase == Phase.DEFENDER_PULSE || phase == Phase.POST_IMPACT_PAUSE ||
                   phase == Phase.COMBO_PAUSE || phase == Phase.COMBO_SECOND_CLASH ||
                   phase == Phase.COMBO_SECOND_IMPACT || phase == Phase.COMBO_ICON_RETREAT) {
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
        splitEffect.clear();
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
        splitEffect.clear();
        
        attackValue = 0;
        defenseValue = 0;
        originalDefenseValue = 0;
        resultValue = 0;
        comboDamage = 0;
        
        perforation = false;
        combo = false;
        comboWillHit = false;
        attackWins = false;
        isDraw = false;
        playerAttacker = false;
        
        centerX = 0f;
        centerY = 0f;
        defenderTargetX = 0f;
        defenderTargetY = 0f;
        atkClashX = 0f;
        atkClashY = 0f;
        defClashX = 0f;
        defClashY = 0f;
        atkIconCenterX = 0f;
        atkIconCenterY = 0f;
        defIconCenterX = 0f;
        defIconCenterY = 0f;
        iconSize = 0f;
        
        phase = Phase.IDLE;
        phaseElapsed = 0f;
        complete = false;
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

    public boolean hasDamageImpacted() {
        return damageImpacted;
    }
}