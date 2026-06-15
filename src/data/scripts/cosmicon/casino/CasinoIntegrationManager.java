package data.scripts.cosmicon.casino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;

import data.scripts.CosmiconConfig;
import data.scripts.cosmicon.CosmiconInteraction;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.state.CosmiconEventState;
import data.scripts.cosmicon.state.CosmiconPlayerState;
import data.scripts.cosmicon.state.CosmiconStats;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.PrismaticDisplayHelper;

@SuppressWarnings("unused")
public class CasinoIntegrationManager {

    private static final int TOURNAMENT_SIZE = 8;
    private static final int TOURNAMENT_OPPONENT_COUNT = TOURNAMENT_SIZE - 1;

    public static boolean isCasinoLoaded() {
        return Global.getSettings().getModManager().isModEnabled("interastral_peace_casino");
    }

    public static boolean isTutorialComplete() {
        return !CosmiconStats.isInTutorialMode();
    }

    public static int getLegendLevel() {
        return CosmiconEventState.getLegendLevel();
    }

    public static void updateLegendLevel(int damageDealt) {
        int bonusHp = CosmiconEventState.getCasinoBattleBonusHp();
        int maxHp = (bonusHp >= 974) ? 999 : 99;
        int capped = Math.min(damageDealt, maxHp);
        int current = getLegendLevel();
        if (capped > current) {
            CosmiconEventState.setLegendLevel(capped);
        }
    }

    public static int getCreditReward() {
        int playerLevel = Global.getSector().getPlayerStats().getLevel();
        return CosmiconStats.calculateCreditReward(playerLevel);
    }

    public static int getTournamentCreditReward() {
        int playerLevel = Global.getSector().getPlayerStats().getLevel();
        return CosmiconConfig.NORMAL_ENCOUNTER_CREDIT_PER_LEVEL * CosmiconConfig.TOURNAMENT_CREDIT_PER_LEVEL_MULTIPLIER * Math.max(1, playerLevel);
    }

    public static int getTournamentParticipationCredits(int totalGames) {
        int playerLevel = Global.getSector().getPlayerStats().getLevel();
        return CosmiconConfig.NORMAL_ENCOUNTER_CREDIT_PER_LEVEL * totalGames * Math.max(1, playerLevel);
    }

    public static void startBossBattle(InteractionDialogAPI dialog, Runnable onLeave) {
        CosmiconEventState.clearCasinoBattleState();
        CosmiconEventState.setIsEmbeddedEntry(true);
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
        CosmiconEventState.setCasinoBattleBonusHp(CosmiconConfig.BOSS_BONUS_HP);
        CosmiconEventState.setCasinoBattleUseTrue(useTrue);

        CosmiconInteraction interaction = new CosmiconInteraction();
        interaction.setOnLeaveAction(onLeave);
        dialog.setPlugin(interaction);
        interaction.init(dialog);
    }

    public static void startLegendBattle(InteractionDialogAPI dialog, Runnable onLeave) {
        CosmiconEventState.clearCasinoBattleState();
        CosmiconEventState.setIsEmbeddedEntry(true);
        CosmiconEventState.setReplayTutorialGame(-1);
        CosmiconEventState.setIsTutorialMode(false);
        CosmiconEventState.setCasinoBattleMode(true);
        CosmiconEventState.setCasinoBattleIsBoss(false);
        CosmiconEventState.setCasinoBattleOpponent(CharacterIds.TRASHCAN);
        int bonusHp = CosmiconStats.isLegend999Unlocked() ? CosmiconConfig.LEGEND_999_BONUS_HP : CosmiconConfig.LEGEND_BONUS_HP;
        CosmiconEventState.setCasinoBattleBonusHp(bonusHp);
        CosmiconEventState.setCasinoBattleUseTrue(false);
        CosmiconEventState.setLegendSkipEnabled(CosmiconStats.isLegendTitleInherited());

        CosmiconInteraction interaction = new CosmiconInteraction();
        interaction.setOnLeaveAction(onLeave);
        dialog.setPlugin(interaction);
        interaction.init(dialog);
    }

    public static void startTournament(InteractionDialogAPI dialog, Runnable onLeave) {
        CosmiconEventState.clearTournamentState();
        CosmiconEventState.setIsEmbeddedEntry(true);
        CosmiconEventState.setReplayTutorialGame(-1);
        CosmiconEventState.setIsTutorialMode(false);

        String playerCharId = CosmiconPlayerState.loadCharacter();
        if (playerCharId == null || playerCharId.isEmpty()) {
            playerCharId = CharacterIds.TRASHCAN;
        }

        boolean playerIsBasic = CharacterRegistry.isBasicCharacter(playerCharId);
        int basicNeeded = playerIsBasic ? 3 : 4;
        int advancedNeeded = playerIsBasic ? 4 : 3;

        List<String> basicPool = new ArrayList<>();
        List<String> advancedPool = new ArrayList<>();
        for (CharacterCard card : CharacterRegistry.getAllCards()) {
            String id = card.getId();
            if (id.equals(playerCharId)) continue;
            if (CharacterRegistry.isBasicCharacter(id)) {
                if (!id.equals(CharacterIds.TRASHCAN)) {
                    basicPool.add(id);
                }
            } else {
                advancedPool.add(id);
            }
        }
        Collections.shuffle(basicPool, ThreadLocalRandom.current());
        Collections.shuffle(advancedPool, ThreadLocalRandom.current());

        List<String> basics = new ArrayList<>(basicPool.subList(0, Math.min(basicNeeded, basicPool.size())));
        List<String> advanceds = new ArrayList<>(advancedPool.subList(0, Math.min(advancedNeeded, advancedPool.size())));

        // Slot layout: slot1=basic, slot2=advanced, slot3=basic, slot4=advanced,
        //              slot5=basic, slot6=advanced, slot7=basic
        // This ensures: R1 vs basic, if won vs advanced, if lost vs basic in LB
        List<String> opponents = new ArrayList<>();
        for (int i = 0; i < TOURNAMENT_OPPONENT_COUNT; i++) {
            if (i % 2 == 0) {
                opponents.add(basics.isEmpty() ? advanceds.remove(0) : basics.remove(0));
            } else {
                opponents.add(advanceds.isEmpty() ? basics.remove(0) : advanceds.remove(0));
            }
        }

        TournamentManager tournament = TournamentManager.createNew(opponents);
        tournament.simulateUpToPlayerMatch();

        String json = tournament.toJson();
        CosmiconEventState.setTournamentBracketData(json);
        CosmiconEventState.setTournamentWins(0);
        CosmiconEventState.setTournamentLosses(0);
        CosmiconEventState.setTournamentInLoserBracket(false);
        CosmiconEventState.setTournamentGrandFinal(false);

        CosmiconEventState.clearCasinoBattleState();
        CosmiconEventState.setCasinoBattleMode(true);
        CosmiconEventState.setCasinoBattleIsBoss(false);

        CosmiconInteraction interaction = new CosmiconInteraction();
        interaction.setOnLeaveAction(onLeave);
        dialog.setPlugin(interaction);
        interaction.init(dialog);
        interaction.showTournamentBracketPanel();
    }

    public static void continueTournament(InteractionDialogAPI dialog, Runnable onLeave) {
        String bracketJson = CosmiconEventState.getTournamentBracketData();
        if (bracketJson == null) {
            onLeave.run();
            return;
        }

        TournamentManager tournament = TournamentManager.fromJson(bracketJson);
        if (tournament == null) {
            CosmiconEventState.clearTournamentState();
            onLeave.run();
            return;
        }

        CosmiconEventState.clearCasinoBattleState();
        CosmiconEventState.setIsEmbeddedEntry(true);
        CosmiconEventState.setCasinoBattleMode(true);
        CosmiconEventState.setCasinoBattleIsBoss(false);

        CosmiconInteraction interaction = new CosmiconInteraction();
        interaction.setOnLeaveAction(onLeave);
        dialog.setPlugin(interaction);
        interaction.init(dialog);
        interaction.showTournamentBracketPanel();
    }

    public static boolean isTournamentUnlocked() {
        return CosmiconStats.isTournamentUnlocked();
    }

    public static void setTournamentUnlocked(boolean unlocked) {
        CosmiconStats.setTournamentUnlocked(unlocked);
    }

    public static boolean isTournamentActive() {
        return CosmiconEventState.isTournamentActive();
    }

    public static TournamentManager getTournamentManager() {
        String bracketJson = CosmiconEventState.getTournamentBracketData();
        if (bracketJson == null) return null;
        return TournamentManager.fromJson(bracketJson);
    }

    public static int getBossRewardTier() {
        if (!getLockedCharacterIds().isEmpty()) return 1;

        Set<String> unlocked = CosmiconStats.getUnlockedPrismaticDice();
        Set<String> trueUnlocked = CosmiconStats.getUnlockedPrismaticTrueDice();

        boolean hasLockedTrueUnlock = false;
        boolean hasLockedWithTrue = false;
        boolean hasLockedWithoutTrue = false;

        for (PrismaticDiceType type : PrismaticDiceRegistry.getAll().values()) {
            boolean isUnlocked = unlocked.contains(type.getId());
            if (type.hasTrueVersion()) {
                if (isUnlocked && !trueUnlocked.contains(type.getId())) {
                    hasLockedTrueUnlock = true;
                } else if (!isUnlocked) {
                    hasLockedWithTrue = true;
                }
            } else if (!isUnlocked) {
                hasLockedWithoutTrue = true;
            }
        }
        if (hasLockedTrueUnlock) return 2;
        if (hasLockedWithTrue) return 3;
        if (hasLockedWithoutTrue) return 4;
        return 5;
    }

    public static List<String> getRewardCandidates(int tier, int count) {
        List<String> pool;
        switch (tier) {
            case 1 -> pool = getLockedCharacterIds();
            case 2 -> pool = getLockedPrismaticTrueVersion();
            case 3 -> pool = getLockedPrismaticWithTrueVersion();
            case 4 -> pool = getLockedPrismaticWithoutTrueVersion();
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
            case 2, 3, 4 -> {
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

    public static void unlockPrismaticTrueReward(String diceId) {
        CosmiconStats.unlockPrismaticTrue(diceId);
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

    public static List<String> getLockedPrismaticTrueVersion() {
        Set<String> unlocked = CosmiconStats.getUnlockedPrismaticDice();
        Set<String> trueUnlocked = CosmiconStats.getUnlockedPrismaticTrueDice();
        List<String> locked = new ArrayList<>();
        for (PrismaticDiceType type : PrismaticDiceRegistry.getAll().values()) {
            if (type.hasTrueVersion() && unlocked.contains(type.getId()) && !trueUnlocked.contains(type.getId())) {
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