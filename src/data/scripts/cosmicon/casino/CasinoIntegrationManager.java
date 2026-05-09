package data.scripts.cosmicon.casino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;

import data.scripts.cosmicon.CosmiconInteraction;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.state.CosmiconEventState;
import data.scripts.cosmicon.state.CosmiconStats;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.PrismaticDisplayHelper;

@SuppressWarnings("unused")
public class CasinoIntegrationManager {

    public static boolean isCasinoLoaded() {
        return Global.getSettings().getModManager().isModEnabled("interastral_peace_casino");
    }

    public static boolean isTutorialComplete() {
        return !CosmiconStats.isInTutorialMode();
    }

    public static int getTrashcanHunterLevel() {
        return CosmiconEventState.getTrashcanHunterLevel();
    }

    public static void updateTrashcanHunterLevel(int damageDealt) {
        int current = getTrashcanHunterLevel();
        if (damageDealt > current) {
            CosmiconEventState.setTrashcanHunterLevel(damageDealt);
        }
    }

    public static int getCreditReward() {
        int playerLevel = Global.getSector().getPlayerStats().getLevel();
        return CosmiconStats.calculateCreditReward(playerLevel);
    }

    public static void startBossBattle(InteractionDialogAPI dialog, Runnable onLeave) {
        CosmiconEventState.clearCasinoBattleState();
        CosmiconEventState.setCasinoBattleMode(true);
        CosmiconEventState.setCasinoBattleIsBoss(true);

        CharacterCard opponent = CharacterRegistry.getRandomOpponent();
        if (opponent == null) {
            opponent = CharacterRegistry.getCharacterById(CharacterIds.TRASHCAN);
        }
        String oppId = opponent.getId();

        boolean hasPrismatic = !opponent.getPrismaticDiceIds().isEmpty();
        boolean useTrue = false;
        if (hasPrismatic) {
            String defaultPrismatic = opponent.getPrismaticDiceIds().keySet().iterator().next();
            CosmiconEventState.setOpponentPrismatic(defaultPrismatic);
            PrismaticDiceType diceType = PrismaticDiceRegistry.get(defaultPrismatic);
            useTrue = diceType != null && diceType.hasTrueVersion();
        }

        CosmiconEventState.setCasinoBattleOpponent(oppId);
        CosmiconEventState.setCasinoBattleBonusHp(15);
        CosmiconEventState.setCasinoBattleUseTrue(useTrue);
        CosmiconEventState.setIsBarEvent(false);

        CosmiconInteraction interaction = new CosmiconInteraction();
        interaction.setOnLeaveAction(onLeave);
        dialog.setPlugin(interaction);
        interaction.init(dialog);
    }

    public static void startChallengeBattle(InteractionDialogAPI dialog, Runnable onLeave) {
        CosmiconEventState.clearCasinoBattleState();
        CosmiconEventState.setCasinoBattleMode(true);
        CosmiconEventState.setCasinoBattleIsBoss(false);
        CosmiconEventState.setCasinoBattleOpponent(CharacterIds.TRASHCAN);
        CosmiconEventState.setCasinoBattleBonusHp(74);
        CosmiconEventState.setCasinoBattleUseTrue(false);
        CosmiconEventState.setIsBarEvent(false);

        CosmiconInteraction interaction = new CosmiconInteraction();
        interaction.setOnLeaveAction(onLeave);
        dialog.setPlugin(interaction);
        interaction.init(dialog);
    }

    public static int getBossRewardTier() {
        List<String> lockedChars = getLockedCharacterIds();
        if (!lockedChars.isEmpty()) return 1;

        List<String> lockedTrueVersionPrismatic = getLockedPrismaticWithTrueVersion();
        if (!lockedTrueVersionPrismatic.isEmpty()) return 2;

        List<String> lockedOtherPrismatic = getLockedPrismaticWithoutTrueVersion();
        if (!lockedOtherPrismatic.isEmpty()) return 3;

        return 4;
    }

    public static List<String> getRewardCandidates(int tier, int count) {
        List<String> pool;
        switch (tier) {
            case 1 -> pool = getLockedCharacterIds();
            case 2 -> pool = getLockedPrismaticWithTrueVersion();
            case 3 -> pool = getLockedPrismaticWithoutTrueVersion();
            default -> { return Collections.emptyList(); }
        }
        return pickRandom(pool, count);
    }

    public static String getRewardDisplayName(String id, int tier) {
        switch (tier) {
            case 1 -> {
                CharacterCard card = CharacterRegistry.getCharacterById(id);
                return card != null ? card.getName() : id;
            }
            case 2, 3 -> {
                return PrismaticDisplayHelper.getDiceDisplayName(id);
            }
            default -> { return ""; }
        }
    }

    public static void unlockCharacterReward(String charId) {
        CosmiconStats.unlockCharacter(charId);
    }

    public static void unlockPrismaticReward(String diceId) {
        CosmiconStats.unlockPrismaticDice(diceId);
    }

    public static List<String> getLockedCharacterIds() {
        Set<String> unlocked = CosmiconStats.getUnlockedCharacters();
        List<String> locked = new ArrayList<>();
        for (CharacterCard card : CharacterRegistry.getAllCards()) {
            if (!unlocked.contains(card.getId())) {
                locked.add(card.getId());
            }
        }
        return locked;
    }

    public static List<String> getLockedPrismaticWithTrueVersion() {
        Set<String> unlocked = CosmiconStats.getUnlockedPrismaticDice();
        List<String> locked = new ArrayList<>();
        for (PrismaticDiceType type : PrismaticDiceRegistry.getAll().values()) {
            if (type.hasTrueVersion() && !unlocked.contains(type.getId())) {
                locked.add(type.getId());
            }
        }
        return locked;
    }

    public static List<String> getLockedPrismaticWithoutTrueVersion() {
        Set<String> unlocked = CosmiconStats.getUnlockedPrismaticDice();
        List<String> locked = new ArrayList<>();
        for (PrismaticDiceType type : PrismaticDiceRegistry.getAll().values()) {
            if (!type.hasTrueVersion() && !unlocked.contains(type.getId())) {
                locked.add(type.getId());
            }
        }
        return locked;
    }

    private static <T> List<T> pickRandom(List<T> source, int count) {
        List<T> shuffled = new ArrayList<>(source);
        Collections.shuffle(shuffled, ThreadLocalRandom.current());
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }
}