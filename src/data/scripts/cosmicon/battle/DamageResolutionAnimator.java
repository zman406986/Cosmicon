package data.scripts.cosmicon.battle;

import java.awt.Color;

import com.fs.starfarer.api.ui.CustomPanelAPI;

import data.scripts.cosmicon.util.ColorHelper;

public class DamageResolutionAnimator {
    private static final float ICON_PREP_DURATION = 0.5f;
    private static final float ICON_CLASH_DURATION = 0.8f;
    private static final float ICON_IMPACT_DURATION = 0.6f;
    private static final float RESULT_FLIGHT_DURATION = 0.8f;
    private static final float IMPACT_FLASH_DURATION = 0.5f;
    private static final float POST_IMPACT_PAUSE_DURATION = 1.0f;
    private static final float COMBO_PAUSE_DURATION = 0.5f;
    private static final float DEFENDER_PULSE_DURATION = 0.4f;
    
    private static final Color DAMAGE_RESULT_COLOR = FlyingNumber.DAMAGE_RESULT;
    private static final Color DEFENDER_PULSE_COLOR = new Color(80, 200, 120);
    
    private enum Phase {
        IDLE,
        ICON_PREPARATION,
        ICON_CLASH,
        WAIT_FOR_CLASH_CLICK,
        ICON_IMPACT,
        WAIT_FOR_IMPACT_CLICK,
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
    
    private int defenderFinalHp;
    private int comboDamage;
    
    private CustomPanelAPI panel;
    
    public DamageResolutionAnimator() {
        phase = Phase.IDLE;
        phaseElapsed = 0f;
        complete = false;
        waitingForClick = false;
        impactEffect = new ImpactEffect();
        shatterEffect = new IconShatterEffect();
    }
    
    public void startResolution(
            BattleState state,
            DamageResolver.DamageResult result,
            float attackerStartX,
            float attackerStartY,
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
            this.defenderFinalHp = Math.max(0, 
                (state.isPlayerAttacker() ? state.getOpponentHp() : state.getPlayerHp()) 
                - resultValue - comboDamage);
        } else {
            this.comboDamage = 0;
            this.defenderFinalHp = Math.max(0,
                (state.isPlayerAttacker() ? state.getOpponentHp() : state.getPlayerHp())
                - resultValue);
        }
        
        cleanup();
        
        calculateStaticIconPositions(state);
        createFlyingNumbers();
        
        phase = Phase.ICON_PREPARATION;
        phaseElapsed = 0f;
        complete = false;
        waitingForClick = false;
    }
    
    private void calculateStaticIconPositions(BattleState state) {
        float halfH = BattleRenderingUtils.PANEL_HEIGHT / 2f;
        iconSize = halfH * BattleRenderingUtils.ROLE_ICON_SIZE_RATIO;
        
        float bottomIconCenterX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float bottomIconCenterY = (halfH - iconSize) / 2f + iconSize / 2f;
        
        float topIconCenterX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float topIconCenterY = halfH + (halfH - iconSize) / 2f + iconSize / 2f;
        
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
    }
    
    public void advance(float amount) {
        if (phase == Phase.IDLE || phase == Phase.COMPLETE) return;
        
        if (waitingForClick) return;
        
        phaseElapsed += amount;
        
        switch (phase) {
            case ICON_PREPARATION -> advanceIconPreparation(amount);
            case ICON_CLASH -> advanceIconClash(amount);
            case ICON_IMPACT -> advanceIconImpact(amount);
            case RESULT_FLIGHT -> advanceResultFlight(amount);
            case IMPACT_FLASH -> advanceImpactFlash(amount);
            case DEFENDER_PULSE -> advanceDefenderPulse(amount);
            case POST_IMPACT_PAUSE -> advancePostImpactPause(amount);
            case COMBO_PAUSE -> advanceComboPause(amount);
            case COMBO_SECOND_CLASH -> advanceComboSecondClash(amount);
            case COMBO_SECOND_IMPACT -> advanceComboSecondImpact(amount);
        }
        
        advanceFlyingNumbers(amount);
        
        if (impactEffect != null) {
            impactEffect.advance(amount);
        }
        if (shatterEffect != null) {
            shatterEffect.advance(amount);
        }
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
    }
    
    private void advanceIconPreparation(float amount) {
        if (phaseElapsed >= ICON_PREP_DURATION) {
            startIconClash();
            phase = Phase.ICON_CLASH;
            phaseElapsed = 0f;
        }
    }
    
    private void startIconClash() {
        atkNumber.startFrom(atkIconCenterX, atkIconCenterY);
        atkNumber.flyTo(centerX, centerY, ICON_CLASH_DURATION);
        
        defNumber.startFrom(defIconCenterX, defIconCenterY);
        defNumber.flyTo(centerX, centerY, ICON_CLASH_DURATION);
    }
    
    private void advanceIconClash(float amount) {
        if (phaseElapsed >= ICON_CLASH_DURATION || 
            (atkNumber.hasImpacted() && defNumber.hasImpacted())) {
            
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
            shatterEffect.trigger(defIconCenterX, defIconCenterY, 
                ColorHelper.DEFENSE_VALUE, iconSize);
        } else {
            shatterEffect.trigger(atkIconCenterX, atkIconCenterY, 
                ColorHelper.ATTACK_VALUE, iconSize);
        }
    }
    
    private void advanceIconImpact(float amount) {
        if (phaseElapsed >= ICON_IMPACT_DURATION) {
            phase = Phase.WAIT_FOR_IMPACT_CLICK;
            waitingForClick = true;
            phaseElapsed = 0f;
        }
    }
    
    private void proceedFromImpactWait() {
        if (attackWins && resultValue > 0) {
            startResultFlight();
            phase = Phase.RESULT_FLIGHT;
        } else if (!attackWins || isDraw) {
            if (isDraw) {
                resultNumber.setValue(0);
                resultNumber.setColor(DEFENDER_PULSE_COLOR);
            } else {
                resultNumber.setValue(resultValue);
                resultNumber.setColor(DAMAGE_RESULT_COLOR);
            }
            resultNumber.startFrom(centerX, centerY);
            phase = Phase.DEFENDER_PULSE;
        } else {
            resultNumber.setValue(resultValue);
            resultNumber.setColor(DAMAGE_RESULT_COLOR);
            resultNumber.startFrom(centerX, centerY);
            phase = Phase.IMPACT_FLASH;
        }
        phaseElapsed = 0f;
    }
    
    private void startResultFlight() {
        resultNumber.setValue(resultValue);
        resultNumber.setColor(DAMAGE_RESULT_COLOR);
        resultNumber.startFrom(centerX, centerY);
        resultNumber.flyTo(defenderTargetX, defenderTargetY, RESULT_FLIGHT_DURATION);
    }
    
    private void advanceResultFlight(float amount) {
        if (phaseElapsed >= RESULT_FLIGHT_DURATION || resultNumber.hasImpacted()) {
            impactEffect.triggerFlash(defenderTargetX, defenderTargetY, 40f, DAMAGE_RESULT_COLOR);
            impactEffect.triggerShockwave(defenderTargetX, defenderTargetY);
            
            if (resultValue > 0) {
                impactEffect.triggerParticles(defenderTargetX, defenderTargetY, 6, DAMAGE_RESULT_COLOR);
            }
            
            phase = Phase.IMPACT_FLASH;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceImpactFlash(float amount) {
        if (phaseElapsed >= IMPACT_FLASH_DURATION) {
            phase = Phase.POST_IMPACT_PAUSE;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceDefenderPulse(float amount) {
        if (phaseElapsed >= DEFENDER_PULSE_DURATION) {
            phase = Phase.POST_IMPACT_PAUSE;
            phaseElapsed = 0f;
        }
    }
    
    private void advancePostImpactPause(float amount) {
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
    
    private void advanceComboPause(float amount) {
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
    
    private void advanceComboSecondClash(float amount) {
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
    
    private void advanceComboSecondImpact(float amount) {
        if (phaseElapsed >= IMPACT_FLASH_DURATION) {
            phase = Phase.COMPLETE;
            complete = true;
        }
    }
    
    private float easeOutQuad(float t) {
        return 1f - (1f - t) * (1f - t);
    }
    
    public void render(float panelX, float panelY, float panelHeight, float alphaMult) {
        if (phase == Phase.IDLE) return;
        
        renderNumbersOnIcons(panelX, panelY, panelHeight, alphaMult);
        
        if (shatterEffect != null) {
            shatterEffect.render(panelX, panelY, panelHeight, alphaMult);
        }
        
        renderResultNumbers(panelX, panelY, panelHeight, alphaMult);
        
        if (impactEffect != null) {
            impactEffect.render(panelX, panelY, panelHeight, alphaMult);
        }
    }
    
    private void renderNumbersOnIcons(float panelX, float panelY, float panelHeight, float alphaMult) {
        boolean shouldRenderNumbers = phase == Phase.ICON_PREPARATION ||
            phase == Phase.ICON_CLASH ||
            phase == Phase.WAIT_FOR_CLASH_CLICK ||
            phase == Phase.ICON_IMPACT ||
            phase == Phase.WAIT_FOR_IMPACT_CLICK;
        
        if (!shouldRenderNumbers) return;
        
        boolean atkShattered = !attackWins || isDraw;
        boolean defShattered = attackWins;
        
        if (atkNumber != null && !atkShattered) {
            atkNumber.render(panelX, panelY, panelHeight, alphaMult, panel);
        }
        
        if (defNumber != null && !defShattered) {
            defNumber.render(panelX, panelY, panelHeight, alphaMult, panel);
        }
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
            phase != Phase.ICON_IMPACT;
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
        
        phase = Phase.COMPLETE;
        complete = true;
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
        
        if (impactEffect != null) {
            impactEffect.clear();
        }
        if (shatterEffect != null) {
            shatterEffect.clear();
        }
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
        
        if (impactEffect != null) {
            impactEffect.clear();
        }
        if (shatterEffect != null) {
            shatterEffect.clear();
        }
        
        phase = Phase.IDLE;
        phaseElapsed = 0f;
        complete = false;
        waitingForClick = false;
    }
    
    public int getDefenderFinalHp() {
        return defenderFinalHp;
    }
    
    public boolean isPerforation() {
        return perforation;
    }
    
    public int getResultValue() {
        return resultValue;
    }
    
    public int getComboDamage() {
        return comboDamage;
    }
    
    public Phase getPhase() {
        return phase;
    }
    
    public boolean isWaitingForClick() {
        return waitingForClick;
    }
    
    public boolean isDraw() {
        return isDraw;
    }
    
    public boolean isAttackWins() {
        return attackWins;
    }
}