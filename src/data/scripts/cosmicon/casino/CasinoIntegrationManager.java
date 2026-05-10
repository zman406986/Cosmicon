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

    private static final int TOURNAMENT_SIZE = 8;
    private static final int TOURNAMENT_OPPONENT_COUNT = TOURNAMENT_SIZE - 1;

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
        int maxHp = CosmiconEventState.isTournamentUnlocked() ? 999 : 99;
        int capped = Math.min(damageDealt, maxHp);
        int current = getTrashcanHunterLevel();
        if (capped > current) {
            CosmiconEventState.setTrashcanHunterLevel(capped);
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
        interaction.showTournamentBracketPanel();
    }

    public static void startGatekeeperBattle(InteractionDialogAPI dialog, Runnable onLeave) {
        CosmiconEventState.clearCasinoBattleState();
        CosmiconEventState.setReplayTutorialGame(-1);
        CosmiconEventState.setIsTutorialMode(false);
        CosmiconEventState.setCasinoBattleMode(true);
        CosmiconEventState.setCasinoBattleIsBoss(false);
        CosmiconEventState.setCasinoBattleOpponent(CharacterIds.TRASHCAN);
        int bonusHp = CosmiconEventState.isTournamentUnlocked() ? 974 : 74;
        CosmiconEventState.setCasinoBattleBonusHp(bonusHp);
        CosmiconEventState.setCasinoBattleUseTrue(false);
        CosmiconEventState.setIsBarEvent(false);

        CosmiconInteraction interaction = new CosmiconInteraction();
        interaction.setOnLeaveAction(onLeave);
        dialog.setPlugin(interaction);
        interaction.init(dialog);
    }

    public static void startTournament(InteractionDialogAPI dialog, Runnable onLeave) {
        CosmiconEventState.clearTournamentState();
        CosmiconEventState.setReplayTutorialGame(-1);
        CosmiconEventState.setIsTutorialMode(false);

        String playerCharId = CosmiconEventState.getOpponentCharacter();
        if (playerCharId == null) {
            playerCharId = CharacterIds.TRASHCAN;
        }

        List<String> opponentPool = new ArrayList<>();
        for (CharacterCard card : CharacterRegistry.getAllCards()) {
            String id = card.getId();
            if (!id.equals(CharacterIds.TRASHCAN) && !id.equals(playerCharId)) {
                opponentPool.add(id);
            }
        }
        Collections.shuffle(opponentPool, ThreadLocalRandom.current());
        List<String> selectedOpponents = opponentPool.subList(0, Math.min(TOURNAMENT_OPPONENT_COUNT, opponentPool.size()));

        TournamentManager tournament = TournamentManager.createNew(selectedOpponents);
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
        if (bracketJson == null) return;

        TournamentManager tournament = TournamentManager.fromJson(bracketJson);
        if (tournament == null) return;

        CosmiconEventState.clearCasinoBattleState();
        CosmiconEventState.setCasinoBattleMode(true);
        CosmiconEventState.setCasinoBattleIsBoss(false);

        CosmiconInteraction interaction = new CosmiconInteraction();
        interaction.setOnLeaveAction(onLeave);
        dialog.setPlugin(interaction);
        interaction.init(dialog);
    }

    public static void startTournamentBattle(InteractionDialogAPI dialog, Runnable onLeave) {
        String bracketJson = CosmiconEventState.getTournamentBracketData();
        if (bracketJson == null) return;

        TournamentManager tournament = TournamentManager.fromJson(bracketJson);
        if (tournament == null) return;

        String nextOppId = tournament.getNextOpponentId();
        if (nextOppId == null) return;

        CosmiconEventState.clearCasinoBattleState();
        CosmiconEventState.setReplayTutorialGame(-1);
        CosmiconEventState.setIsTutorialMode(false);
        CosmiconEventState.setCasinoBattleMode(true);
        CosmiconEventState.setCasinoBattleIsBoss(false);
        CosmiconEventState.setCasinoBattleOpponent(nextOppId);
        CosmiconEventState.setCasinoBattleBonusHp(0);
        CosmiconEventState.setCasinoBattleUseTrue(false);
        CosmiconEventState.setIsBarEvent(false);

        CosmiconInteraction interaction = new CosmiconInteraction();
        interaction.setOnLeaveAction(onLeave);
        dialog.setPlugin(interaction);
        interaction.init(dialog);
    }

    public static void handleTournamentVictory() {
        String bracketJson = CosmiconEventState.getTournamentBracketData();
        if (bracketJson == null) return;

        TournamentManager tournament = TournamentManager.fromJson(bracketJson);
        if (tournament == null) return;

        tournament.recordPlayerMatch(true);
        CosmiconEventState.setTournamentBracketData(tournament.toJson());

        int wins = CosmiconEventState.getTournamentWins();
        CosmiconEventState.setTournamentWins(wins + 1);

        CosmiconEventState.setTournamentGrandFinal(tournament.isGrandFinal());
        CosmiconEventState.setTournamentInLoserBracket(
            !tournament.isGrandFinal() && !tournament.isPlayerChampion() && !tournament.isPlayerEliminated()
        );
    }

    public static void handleTournamentDefeat() {
        String bracketJson = CosmiconEventState.getTournamentBracketData();
        if (bracketJson == null) return;

        TournamentManager tournament = TournamentManager.fromJson(bracketJson);
        if (tournament == null) return;

        tournament.recordPlayerMatch(false);
        CosmiconEventState.setTournamentBracketData(tournament.toJson());

        int losses = CosmiconEventState.getTournamentLosses();
        CosmiconEventState.setTournamentLosses(losses + 1);

        CosmiconEventState.setTournamentGrandFinal(tournament.isGrandFinal());
        CosmiconEventState.setTournamentInLoserBracket(
            !tournament.isGrandFinal() && !tournament.isPlayerEliminated()
        );
    }

    public static boolean handleGrandFinalGame(boolean playerWon) {
        String bracketJson = CosmiconEventState.getTournamentBracketData();
        if (bracketJson == null) return false;

        TournamentManager tournament = TournamentManager.fromJson(bracketJson);
        if (tournament == null) return false;

        tournament.recordGrandFinalGame(playerWon);
        CosmiconEventState.setTournamentBracketData(tournament.toJson());

        return tournament.isPlayerChampion() || tournament.isPlayerEliminated();
    }

    public static int getTournamentRewardSelections() {
        String bracketJson = CosmiconEventState.getTournamentBracketData();
        if (bracketJson == null) return 1;

        TournamentManager tournament = TournamentManager.fromJson(bracketJson);
        if (tournament == null) return 1;

        if (tournament.isPlayerChampion()) return 3;

        TournamentManager.BracketData data = tournament.getBracketData();
        if (data.currentBracket == TournamentManager.BRACKET_GF) return 2;

        return 1;
    }

    public static int calculateTournamentCredits(int wins) {
        return wins * 3 * getCreditReward();
    }

    public static boolean isTournamentUnlocked() {
        return CosmiconEventState.isTournamentUnlocked();
    }

    public static void setTournamentUnlocked(boolean unlocked) {
        CosmiconEventState.setTournamentUnlocked(unlocked);
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
        List<String> lockedChars = getLockedCharacterIds();
        if (!lockedChars.isEmpty()) return 1;

        List<String> lockedTrueVersion = getLockedPrismaticTrueVersion();
        if (!lockedTrueVersion.isEmpty()) return 2;

        List<String> lockedTrueVersionPrismatic = getLockedPrismaticWithTrueVersion();
        if (!lockedTrueVersionPrismatic.isEmpty()) return 3;

        List<String> lockedOtherPrismatic = getLockedPrismaticWithoutTrueVersion();
        if (!lockedOtherPrismatic.isEmpty()) return 4;

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