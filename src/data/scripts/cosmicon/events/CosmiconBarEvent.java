package data.scripts.cosmicon.events;

import java.util.Map;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEvent;

import data.scripts.Strings;
import data.scripts.cosmicon.CosmiconInteraction;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.state.CosmiconEventState;
import data.scripts.cosmicon.state.CosmiconStats;

public class CosmiconBarEvent extends BaseBarEvent {

    public enum OptionId {
        PLAY,
        DECLINE
    }

    @Override
    public boolean shouldShowAtMarket(MarketAPI market) {
        if (!super.shouldShowAtMarket(market)) return false;
        if (market == null) return false;
        if (market.getSize() < 3) return false;
        if (!CosmiconStats.hasAnyCharacterUnlocked()) return false;
        return true;
    }

    @Override
    public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        this.dialog = dialog;
        this.memoryMap = memoryMap;
        text = dialog.getTextPanel();
        options = dialog.getOptionPanel();

        boolean isTutorial = CosmiconStats.isInTutorialMode();

        if (isTutorial) {
            int remaining = CosmiconStats.getRemainingTutorialGames();
            text.addPara(Strings.format("bar_event.tutorial_prompt", remaining));
        } else {
            text.addPara(Strings.get("bar_event.standard_prompt"));
        }

        options.addOption(Strings.get("bar_event.accept"), OptionId.PLAY);
        options.addOption(Strings.get("bar_event.decline"), OptionId.DECLINE);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (optionData == OptionId.PLAY) {
            boolean isTutorial = CosmiconStats.isInTutorialMode();

            if (isTutorial) {
                int gamesPlayed = CosmiconStats.getGamesPlayed();
                String opponentId;
                if (gamesPlayed == 0) {
                    opponentId = "trashcan";
                } else {
                    opponentId = "robin";
                }
                CosmiconEventState.setOpponentCharacter(opponentId);
                CosmiconEventState.setIsTutorialMode(true);
            } else {
                CharacterCard opponentCard = CharacterRegistry.getRandomOpponent();
                if (opponentCard != null) {
                    CosmiconEventState.setOpponentCharacter(opponentCard.getId());
                }

                PrismaticDiceType prismatic = PrismaticDiceRegistry.getRandomPrismatic();
                if (prismatic != null) {
                    CosmiconEventState.setOpponentPrismatic(prismatic.getId());
                }
            }

            CosmiconEventState.setIsBarEvent(true);
            BarEventManager.getInstance().notifyWasInteractedWith(this);

            CosmiconInteraction.startInteraction(dialog);
        } else if (optionData == OptionId.DECLINE) {
            text.addPara(Strings.get("bar_event.declined"));
            done = true;
        }
    }
}
