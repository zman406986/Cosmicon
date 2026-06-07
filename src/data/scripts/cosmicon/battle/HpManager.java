package data.scripts.cosmicon.battle;

import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.CosmiconLogger;
import data.scripts.Strings;

public class HpManager {

    public interface UnyieldingCheck {
        void checkAndConsumeUnyielding(boolean isPlayer);
    }

    private int playerHp;
    private int opponentHp;
    private int playerTotalDamageTaken;
    private int opponentTotalDamageTaken;

    public HpManager() {
        this.playerHp = 0;
        this.opponentHp = 0;
        this.playerTotalDamageTaken = 0;
        this.opponentTotalDamageTaken = 0;
    }

    public int getPlayerHp() {
        return playerHp;
    }

    public void setPlayerHp(int hp, CharacterCard card) {
        this.playerHp = Math.max(0, Math.min(hp, card != null ? card.getMaxHp() : hp));
    }

    public int getOpponentHp() {
        return opponentHp;
    }

    public void setOpponentHp(int hp, CharacterCard card) {
        this.opponentHp = Math.max(0, Math.min(hp, card != null ? card.getMaxHp() : hp));
    }

    public int getPlayerTotalDamageTaken() {
        return playerTotalDamageTaken;
    }

    public int getOpponentTotalDamageTaken() {
        return opponentTotalDamageTaken;
    }

    public void recordDamageTaken(int damage, boolean isPlayer) {
        if (isPlayer) {
            playerTotalDamageTaken += damage;
        } else {
            opponentTotalDamageTaken += damage;
        }
    }

    public void applyDamageTo(boolean isPlayer, int damage, StatusEffectProcessor effects,
                              CharacterCard card, UnyieldingCheck unyieldingCheck) {
        int oldHp = getHp(isPlayer);
        String characterName = getCharacterName(card, isPlayer);

        setHp(isPlayer, Math.max(0, oldHp - damage));
        recordDamageTaken(damage, isPlayer);

        int newHp = getHp(isPlayer);

        if (newHp <= 0 && effects.hasEffect(StatusEffectProcessor.StatusEffect.UNYIELDING)) {
            newHp = 1;
            setHp(isPlayer, 1);
            effects.removeEffect(StatusEffectProcessor.StatusEffect.UNYIELDING);
            CosmiconLogger.info("%s: UNYIELDING prevented death (HP: 1)", characterName);

            if (card != null && CharacterIds.PHAINON.equals(card.getId())) {
                unyieldingCheck.checkAndConsumeUnyielding(isPlayer);
            }
        }

        int maxHp = card != null ? card.getMaxHp() : oldHp;

        CosmiconLogger.hpChange(characterName, oldHp, newHp, maxHp);
        if (damage > 0) {
            CosmiconLogger.info("%s took %d damage (HP: %d/%d)", characterName, damage, newHp, maxHp);
        }
    }

    public void applyHealTo(boolean isPlayer, int heal, CharacterCard card) {
        int maxHp = card != null ? card.getMaxHp() : Integer.MAX_VALUE;
        int oldHp = getHp(isPlayer);
        String characterName = getCharacterName(card, isPlayer);

        setHp(isPlayer, Math.min(oldHp + heal, maxHp));

        if (heal > 0) {
            CosmiconLogger.debug("%s healed %d (HP: %d -> %d/%d)", characterName, heal, oldHp,
                getHp(isPlayer), maxHp);
        }
    }

    private int getHp(boolean isPlayer) {
        return isPlayer ? playerHp : opponentHp;
    }

    private void setHp(boolean isPlayer, int value) {
        if (isPlayer) playerHp = value; else opponentHp = value;
    }

    private String getCharacterName(CharacterCard card, boolean isPlayer) {
        return card != null ? card.getName() :
            (isPlayer ? Strings.get("battle.player") : Strings.get("battle.opponent"));
    }

    public void cleanup() {
        playerHp = 0;
        opponentHp = 0;
        playerTotalDamageTaken = 0;
        opponentTotalDamageTaken = 0;
    }
}
