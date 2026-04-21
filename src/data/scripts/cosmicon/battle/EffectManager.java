package data.scripts.cosmicon.battle;

import data.scripts.cosmicon.battle.BattleState.TurnType;
import data.scripts.cosmicon.battle.StatusEffectProcessor.Phase;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.util.CosmiconLogger;

public class EffectManager {
    
    private final StatusEffectProcessor playerEffects;
    private final StatusEffectProcessor opponentEffects;
    
    public EffectManager() {
        this.playerEffects = new StatusEffectProcessor();
        this.opponentEffects = new StatusEffectProcessor();
    }
    
    public StatusEffectProcessor getEffects(boolean forPlayer) {
        return forPlayer ? playerEffects : opponentEffects;
    }
    
    public void applyEffect(StatusEffect effect, int layers, boolean toPlayer) {
        getEffects(toPlayer).addEffect(effect, layers);
        CosmiconLogger.debug("EffectManager: %s applied to %s (%d layers)", effect.name(), toPlayer ? "Player" : "Opponent", layers);
    }
    
    public void resetTurnState() {
        playerEffects.resetTurnState();
        opponentEffects.resetTurnState();
    }
    
    public void clearTemporaryEffects() {
        playerEffects.clearTemporaryEffects();
        opponentEffects.clearTemporaryEffects();
    }
}