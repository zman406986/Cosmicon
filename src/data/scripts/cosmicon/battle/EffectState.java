package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.List;

import data.scripts.cosmicon.util.CosmiconLogger;

public class EffectState {

    public record ModificationRecord(StatusEffectProcessor.StatusEffect effect, boolean forPlayer, int sequence, int diceIndex) {
        public ModificationRecord(StatusEffectProcessor.StatusEffect effect, boolean forPlayer, int sequence) {
            this(effect, forPlayer, sequence, -1);
        }
    }

    private final StatusEffectProcessor playerEffects;
    private final StatusEffectProcessor opponentEffects;
    private final List<ModificationRecord> modificationOrder;
    private int modificationSequenceCounter;

    public EffectState() {
        this.playerEffects = new StatusEffectProcessor();
        this.opponentEffects = new StatusEffectProcessor();
        this.modificationOrder = new ArrayList<>();
        this.modificationSequenceCounter = 0;
    }

    public StatusEffectProcessor getPlayerEffects() {
        return playerEffects;
    }

    public StatusEffectProcessor getOpponentEffects() {
        return opponentEffects;
    }

    public StatusEffectProcessor getEffects(boolean forPlayer) {
        return forPlayer ? playerEffects : opponentEffects;
    }

    public void applyEffect(StatusEffectProcessor.StatusEffect effect, int layers, boolean toPlayer) {
        getEffects(toPlayer).addEffect(effect, layers);
        if (effect == StatusEffectProcessor.StatusEffect.HACK || effect == StatusEffectProcessor.StatusEffect.ARISE) {
            modificationOrder.add(new ModificationRecord(effect, toPlayer, modificationSequenceCounter++));
        }
        CosmiconLogger.debug("Effect applied to %s (%d layers)", toPlayer ? "Player" : "Opponent", layers);
    }

    public List<ModificationRecord> getModificationOrder() {
        return new ArrayList<>(modificationOrder);
    }

    public void clearModificationOrder() {
        modificationOrder.clear();
    }

    public boolean hasPendingModification() {
        boolean playerHasHack = playerEffects.hasEffect(StatusEffectProcessor.StatusEffect.HACK);
        boolean opponentHasHack = opponentEffects.hasEffect(StatusEffectProcessor.StatusEffect.HACK);
        boolean playerHasArise = playerEffects.hasEffect(StatusEffectProcessor.StatusEffect.ARISE);
        boolean opponentHasArise = opponentEffects.hasEffect(StatusEffectProcessor.StatusEffect.ARISE);
        return playerHasHack || opponentHasHack || playerHasArise || opponentHasArise;
    }

    public void resetEffectTurnState() {
        playerEffects.resetTurnState();
        opponentEffects.resetTurnState();
        modificationOrder.clear();
        modificationSequenceCounter = 0;
    }

    public void clearTemporaryEffects() {
        playerEffects.clearTemporaryEffects();
        opponentEffects.clearTemporaryEffects();
    }

    public void cleanup() {
        clearTemporaryEffects();
        resetEffectTurnState();
    }
}
