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
        int oldHp = isPlayer ? playerHp : opponentHp;
        String characterName = card != null ? card.getName() :
            (isPlayer ? Strings.get("battle.player") : Strings.get("battle.opponent"));

        if (isPlayer) {
            playerHp = Math.max(0, playerHp - damage);
        } else {
            opponentHp = Math.max(0, opponentHp - damage);
        }
        recordDamageTaken(damage, isPlayer);

        int newHp = isPlayer ? playerHp : opponentHp;

        if (newHp <= 0 && effects.hasEffect(StatusEffectProcessor.StatusEffect.UNYIELDING)) {
            newHp = 1;
            if (isPlayer) {
                playerHp = 1;
            } else {
                opponentHp = 1;
            }
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
        int oldHp = isPlayer ? playerHp : opponentHp;
        String characterName = card != null ? card.getName() :
            (isPlayer ? Strings.get("battle.player") : Strings.get("battle.opponent"));

        if (isPlayer) {
            playerHp = Math.min(playerHp + heal, maxHp);
        } else {
            opponentHp = Math.min(opponentHp + heal, maxHp);
        }

        if (heal > 0) {
            CosmiconLogger.debug("%s healed %d (HP: %d -> %d/%d)", characterName, heal, oldHp,
                isPlayer ? playerHp : opponentHp, maxHp);
        }
    }

    public void cleanup() {
        playerHp = 0;
        opponentHp = 0;
        playerTotalDamageTaken = 0;
        opponentTotalDamageTaken = 0;
    }
}
