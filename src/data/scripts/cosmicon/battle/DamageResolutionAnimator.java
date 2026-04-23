package data.scripts.cosmicon.battle;

import java.awt.Color;

import com.fs.starfarer.api.ui.CustomPanelAPI;

import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.GLStateUtil;

public class DamageResolutionAnimator {
    private static final float NUMBERS_CLASH_DURATION = 0.6f;
    private static final float IMPACT_PAUSE_DURATION = 0.4f;
    private static final float RESULT_FLIGHT_DURATION = 0.5f;
    private static final float HP_FLASH_DURATION = 0.3f;
    private static final float MAX_HP_DRAIN_DURATION = 1.0f;
    private static final float POST_IMPACT_PAUSE_DURATION = 0.6f;
    private static final float COMBO_PAUSE_DURATION = 0.3f;
    
    private static final Color DAMAGE_RESULT_COLOR = FlyingNumber.DAMAGE_RESULT;
    
    private enum Phase {
        IDLE,
        NUMBERS_CLASH,
        IMPACT_PAUSE,
        RESULT_FLIGHT,
        HP_FLASH,
        HP_DRAIN,
        POST_IMPACT_PAUSE,
        COMBO_PAUSE,
        COMBO_SECOND_CLASH,
        COMBO_SECOND_IMPACT,
        COMPLETE
    }
    
    private HeartHpBar defenderHeart;
    private FlyingNumber atkNumber;
    private FlyingNumber defNumber;
    private FlyingNumber resultNumber;
    private FlyingNumber comboResultNumber;
    private ImpactEffect impactEffect;
    
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
    private float attackerHeartX;
    private float attackerHeartY;
    private float defenderHeartX;
    private float defenderHeartY;
    
    private int defenderCurrentHp;
    private int defenderMaxHp;
    private int defenderTargetHp;
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
            float attackerHeartX,
            float attackerHeartY,
            float defenderHeartX,
            float defenderHeartY,
            CustomPanelAPI panel) {
        
        this.panel = panel;
        this.attackerHeartX = attackerHeartX;
        this.attackerHeartY = attackerHeartY;
        this.defenderHeartX = defenderHeartX;
        this.defenderHeartY = defenderHeartY;
        
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
        
        boolean defenderIsPlayer = !state.isPlayerAttacker();
        this.defenderCurrentHp = defenderIsPlayer ? state.getPlayerHp() : state.getOpponentHp();
        this.defenderMaxHp = defenderIsPlayer 
            ? (state.getPlayerCard() != null ? state.getPlayerCard().getMaxHp() : defenderCurrentHp)
            : (state.getOpponentCard() != null ? state.getOpponentCard().getMaxHp() : defenderCurrentHp);
        
        this.resultValue = Math.max(0, attackValue - defenseValue);
        this.defenderTargetHp = Math.max(0, defenderCurrentHp - resultValue);
        
        this.combo = hasCombo(state);
        if (combo) {
            this.comboDamage = resultValue;
            this.defenderFinalHp = Math.max(0, defenderTargetHp - comboDamage);
        } else {
            this.comboDamage = 0;
            this.defenderFinalHp = defenderTargetHp;
        }
        
        cleanup();
        
        defenderHeart = new HeartHpBar();
        defenderHeart.init(panel);
        defenderHeart.setPosition(defenderHeartX, defenderHeartY);
        defenderHeart.setHp(defenderCurrentHp, defenderMaxHp);
        
        createFlyingNumbers();
        
        phase = Phase.NUMBERS_CLASH;
        phaseElapsed = 0f;
        complete = false;
        
        atkNumber.startFrom(attackerHeartX + HeartHpBar.HEART_SIZE / 2f, 
                            attackerHeartY + HeartHpBar.HEART_SIZE / 2f);
        atkNumber.flyTo(centerX, centerY, NUMBERS_CLASH_DURATION);
        
        defNumber.startFrom(defenderHeartX + HeartHpBar.HEART_SIZE / 2f,
                            defenderHeartY + HeartHpBar.HEART_SIZE / 2f);
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
            case HP_FLASH -> advanceHpFlash(amount);
            case HP_DRAIN -> advanceHpDrain(amount);
            case POST_IMPACT_PAUSE -> advancePostImpactPause(amount);
            case COMBO_PAUSE -> advanceComboPause(amount);
            case COMBO_SECOND_CLASH -> advanceComboSecondClash(amount);
            case COMBO_SECOND_IMPACT -> advanceComboSecondImpact(amount);
        }
        
        if (defenderHeart != null) {
            defenderHeart.advance(amount);
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
            resultNumber.flyTo(
                defenderHeartX + HeartHpBar.HEART_SIZE / 2f,
                defenderHeartY + HeartHpBar.HEART_SIZE / 2f,
                RESULT_FLIGHT_DURATION);
            
            phase = Phase.RESULT_FLIGHT;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceResultFlight(float amount) {
        if (phaseElapsed >= RESULT_FLIGHT_DURATION || resultNumber.hasImpacted()) {
            if (defenderHeart != null && resultValue > 0) {
                defenderHeart.flashDamage();
            }
            
            impactEffect.triggerShockwave(
                defenderHeartX + HeartHpBar.HEART_SIZE / 2f,
                defenderHeartY + HeartHpBar.HEART_SIZE / 2f);
            
            if (resultValue > 0) {
                impactEffect.triggerParticles(
                    defenderHeartX + HeartHpBar.HEART_SIZE / 2f,
                    defenderHeartY + HeartHpBar.HEART_SIZE / 2f,
                    6, DAMAGE_RESULT_COLOR);
            }
            
            phase = Phase.HP_FLASH;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceHpFlash(float amount) {
        if (phaseElapsed >= HP_FLASH_DURATION || 
            (defenderHeart != null && !defenderHeart.isFlashing())) {
            
            if (defenderHeart != null && resultValue > 0) {
                defenderHeart.drainTo(defenderTargetHp);
            }
            
            phase = Phase.HP_DRAIN;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceHpDrain(float amount) {
        boolean drainComplete = defenderHeart == null || !defenderHeart.isDraining();
        
        if (phaseElapsed >= MAX_HP_DRAIN_DURATION || drainComplete) {
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
            comboResultNumber.flyTo(
                defenderHeartX + HeartHpBar.HEART_SIZE / 2f,
                defenderHeartY + HeartHpBar.HEART_SIZE / 2f,
                RESULT_FLIGHT_DURATION);
            
            phase = Phase.COMBO_SECOND_CLASH;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceComboSecondClash(float amount) {
        if (phaseElapsed >= RESULT_FLIGHT_DURATION || 
            (comboResultNumber != null && comboResultNumber.hasImpacted())) {
            
            if (defenderHeart != null && comboDamage > 0) {
                defenderHeart.flashDamage();
            }
            
            impactEffect.triggerShockwave(
                defenderHeartX + HeartHpBar.HEART_SIZE / 2f,
                defenderHeartY + HeartHpBar.HEART_SIZE / 2f);
            
            if (comboDamage > 0) {
                impactEffect.triggerParticles(
                    defenderHeartX + HeartHpBar.HEART_SIZE / 2f,
                    defenderHeartY + HeartHpBar.HEART_SIZE / 2f,
                    6, DAMAGE_RESULT_COLOR);
            }
            
            phase = Phase.COMBO_SECOND_IMPACT;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceComboSecondImpact(float amount) {
        boolean flashComplete = defenderHeart == null || !defenderHeart.isFlashing();
        
        if (phaseElapsed >= HP_FLASH_DURATION || flashComplete) {
            if (defenderHeart != null && comboDamage > 0) {
                defenderHeart.drainTo(defenderFinalHp);
            }
            
            phase = Phase.COMPLETE;
            complete = true;
        }
    }
    
    public void render(float panelX, float panelY, float panelHeight, float alphaMult) {
        if (phase == Phase.IDLE) return;
        
        GLStateUtil.resetBlendState();
        
        if (defenderHeart != null) {
            defenderHeart.render(panelX, panelY, panelHeight, alphaMult);
        }
        
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
        
        if (defenderHeart != null) {
            defenderHeart.forceComplete();
        }
        
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
        
        if (defenderHeart != null) {
            defenderHeart.cleanup();
            defenderHeart = null;
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