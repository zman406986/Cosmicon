package data.console.commands;

import data.scripts.cosmicon.casino.CasinoIntegrationManager;
import data.scripts.cosmicon.casino.TournamentManager;
import data.scripts.cosmicon.state.CosmiconEventState;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class CosmiconCasinoStatus implements BaseCommand {

    @Override
    public CommandResult runCommand(@NotNull String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        boolean casinoLoaded = CasinoIntegrationManager.isCasinoLoaded();
        boolean tutorialComplete = CasinoIntegrationManager.isTutorialComplete();
        int hunterLevel = CasinoIntegrationManager.getTrashcanHunterLevel();
        int rewardTier = CasinoIntegrationManager.getBossRewardTier();
        int creditReward = CasinoIntegrationManager.getCreditReward();

        boolean casinoMode = CosmiconEventState.isCasinoBattleMode();
        boolean isBoss = CosmiconEventState.isCasinoBattleBoss();
        String casinoOpponent = CosmiconEventState.getCasinoBattleOpponent();
        int casinoBonusHp = CosmiconEventState.getCasinoBattleBonusHp();
        boolean casinoUseTrue = CosmiconEventState.getCasinoBattleUseTrue();
        int casinoResultDamage = CosmiconEventState.getCasinoBattleResultDamage();

        Console.showMessage("=== Casino Collab Status ===");
        Console.showMessage("Casino Mod Loaded: " + (casinoLoaded ? "YES" : "NO"));
        Console.showMessage("Tutorial Complete: " + (tutorialComplete ? "YES" : "NO"));
        Console.showMessage("Master Dicer Level: " + hunterLevel);
        Console.showMessage("Boss Reward Tier: " + rewardTier
            + " (" + tierName(rewardTier) + ")");
        Console.showMessage("Credit Reward (base): " + creditReward);

        Console.showMessage("");
        Console.showMessage("--- Current Casino Battle State ---");
        Console.showMessage("  Active: " + (casinoMode ? "YES" : "no"));
        if (casinoMode) {
            Console.showMessage("  Type: " + (isBoss ? "BOSS" : "CHALLENGE"));
            Console.showMessage("  Opponent: " + (casinoOpponent != null ? casinoOpponent : "none"));
            Console.showMessage("  Bonus HP: " + casinoBonusHp);
            Console.showMessage("  Use True Prismatic: " + casinoUseTrue);
            Console.showMessage("  Result Damage: " + casinoResultDamage);
        }

        Console.showMessage("");
        Console.showMessage("--- Tournament Status ---");
        boolean tournamentUnlocked = CasinoIntegrationManager.isTournamentUnlocked();
        boolean tournamentActive = CosmiconEventState.isTournamentActive();
        Console.showMessage("  Tournament Unlocked: " + (tournamentUnlocked ? "YES" : "no"));
        Console.showMessage("  Tournament Active: " + (tournamentActive ? "YES" : "no"));
        if (tournamentActive) {
            int wins = CosmiconEventState.getTournamentWins();
            int losses = CosmiconEventState.getTournamentLosses();
            boolean inLoser = CosmiconEventState.isTournamentInLoserBracket();
            boolean grandFinal = CosmiconEventState.isTournamentGrandFinal();

            TournamentManager tm = CasinoIntegrationManager.getTournamentManager();
            String position = tm != null ? tm.getPlayerBracketPosition() : "Unknown";
            String nextOpponent = tm != null ? tm.getNextOpponentId() : "none";

            Console.showMessage("  Position: " + position);
            Console.showMessage("  Wins: " + wins + "  Losses: " + losses);
            Console.showMessage("  Loser Bracket: " + (inLoser ? "YES" : "no"));
            Console.showMessage("  Grand Final: " + (grandFinal ? "YES" : "no"));
            Console.showMessage("  Next Opponent: " + (nextOpponent != null ? nextOpponent : "none"));
        }

        Console.showMessage("");
        Console.showMessage("--- Reward Pools ---");
        Console.showMessage("  Locked Characters: " + CasinoIntegrationManager.getLockedCharacterIds().size());
        Console.showMessage("  Locked True Prismatic: " + CasinoIntegrationManager.getLockedPrismaticWithTrueVersion().size());
        Console.showMessage("  Locked Other Prismatic: " + CasinoIntegrationManager.getLockedPrismaticWithoutTrueVersion().size());

        if (casinoMode) {
            Console.showMessage("");
            Console.showMessage("--- Potential Rewards (Tier " + rewardTier + ") ---");
            java.util.List<String> candidates = CasinoIntegrationManager.getRewardCandidates(rewardTier, 3);
            for (int i = 0; i < candidates.size(); i++) {
                String name = CasinoIntegrationManager.getRewardDisplayName(candidates.get(i), rewardTier);
                Console.showMessage("  [" + (i + 1) + "] " + name + " (" + candidates.get(i) + ")");
            }
            if (candidates.isEmpty()) {
                int credits = creditReward * 3;
                Console.showMessage("  All unlocked! Would receive 3x credits: " + credits);
            }
        }

        return CommandResult.SUCCESS;
    }

    private static String tierName(int tier) {
        switch (tier) {
            case 1 -> { return "Character Unlock"; }
            case 2 -> { return "True Prismatic Unlock"; }
            case 3 -> { return "Prismatic Unlock"; }
            case 4 -> { return "Credits Only"; }
            default -> { return "Unknown"; }
        }
    }
}
