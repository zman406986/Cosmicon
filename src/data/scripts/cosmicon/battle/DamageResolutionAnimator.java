package data.scripts.cosmicon.battle;

import java.awt.Color;

import com.fs.starfarer.api.ui.CustomPanelAPI;

import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.GLStateUtil;

public class DamageResolutionAnimator {
    private static final float NUMBERS_CLASH_DURATION = 0.6f;
    private static final float IMPACT_PAUSE_DURATION = 0.4f;
    private static final float RESULT_FLIGHT_DURATION = 0.5f;
    private static final float IMPACT_FLASH_DURATION = 0.3f;
    private static final float POST_IMPACT_PAUSE_DURATION = 0.6f;
    private static final float COMBO_PAUSE_DURATION = 0.3f;
    
    private static final Color DAMAGE_RESULT_COLOR = FlyingNumber.DAMAGE_RESULT;
    
    private enum Phase {
        IDLE,
        NUMBERS_CLASH,
        IMPACT_PAUSE,
        RESULT_FLIGHT,
        IMPACT_FLASH,
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
    
    private Phase phase;
    private float phaseElapsed;
    private boolean complete;
    
    private int attackValue;
    private int defenseValue;
    private int resultValue;
    private boolean perforation;
    private boolean combo;
    
    private float centerX;
    private float centerY;
    private float defenderTargetX;
    private float defenderTargetY;
    
    private int defenderFinalHp;
    
    private int comboDamage;
    
    private CustomPanelAPI panel;
    
    public DamageResolutionAnimator() {
        phase = Phase.IDLE;
        phaseElapsed = 0f;
        complete = false;
        impactEffect = new ImpactEffect();
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
        
        createFlyingNumbers();
        
        phase = Phase.NUMBERS_CLASH;
        phaseElapsed = 0f;
        complete = false;
        
        atkNumber.startFrom(attackerStartX, attackerStartY);
        atkNumber.flyTo(centerX, centerY, NUMBERS_CLASH_DURATION);
        
        defNumber.startFrom(defenderTargetX, defenderTargetY);
        defNumber.flyTo(centerX, centerY, NUMBERS_CLASH_DURATION);
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
        
        phaseElapsed += amount;
        
        switch (phase) {
            case NUMBERS_CLASH -> advanceNumbersClash(amount);
            case IMPACT_PAUSE -> advanceImpactPause(amount);
            case RESULT_FLIGHT -> advanceResultFlight(amount);
            case IMPACT_FLASH -> advanceImpactFlash(amount);
            case POST_IMPACT_PAUSE -> advancePostImpactPause(amount);
            case COMBO_PAUSE -> advanceComboPause(amount);
            case COMBO_SECOND_CLASH -> advanceComboSecondClash(amount);
            case COMBO_SECOND_IMPACT -> advanceComboSecondImpact(amount);
        }
        
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
        
        if (impactEffect != null) {
            impactEffect.advance(amount);
        }
    }
    
    private void advanceNumbersClash(float amount) {
        if (phaseElapsed >= NUMBERS_CLASH_DURATION || 
            (atkNumber.hasImpacted() && defNumber.hasImpacted())) {
            
            impactEffect.triggerFlash(centerX, centerY, 60f, new Color(255, 255, 200));
            
            if (!perforation) {
                impactEffect.triggerParticles(centerX, centerY, 8, new Color(255, 200, 100));
            }
            
            phase = Phase.IMPACT_PAUSE;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceImpactPause(float amount) {
        if (phaseElapsed >= IMPACT_PAUSE_DURATION) {
            resultNumber.setValue(resultValue);
            resultNumber.setColor(DAMAGE_RESULT_COLOR);
            resultNumber.startFrom(centerX, centerY);
            resultNumber.flyTo(defenderTargetX, defenderTargetY, RESULT_FLIGHT_DURATION);
            
            phase = Phase.RESULT_FLIGHT;
            phaseElapsed = 0f;
        }
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
    
    public void render(float panelX, float panelY, float panelHeight, float alphaMult) {
        if (phase == Phase.IDLE) return;
        
        GLStateUtil.resetBlendState();
        
        if (atkNumber != null) {
            atkNumber.render(panelX, panelY, panelHeight, alphaMult, panel);
        }
        if (defNumber != null) {
            defNumber.render(panelX, panelY, panelHeight, alphaMult, panel);
        }
        if (resultNumber != null) {
            resultNumber.render(panelX, panelY, panelHeight, alphaMult, panel);
        }
        if (comboResultNumber != null) {
            comboResultNumber.render(panelX, panelY, panelHeight, alphaMult, panel);
        }
        
        if (impactEffect != null) {
            impactEffect.render(panelX, panelY, panelHeight, alphaMult);
        }
        
        GLStateUtil.resetColor();
    }
    
    public boolean isComplete() {
        return phase == Phase.COMPLETE && complete;
    }
    
    public void forceComplete() {
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
        
        phase = Phase.IDLE;
        phaseElapsed = 0f;
        complete = false;
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
}