package data.scripts.cosmicon.events;

import java.util.Map;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEvent;
import com.fs.starfarer.api.campaign.TextPanelAPI;

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
        INIT,
        PLAY,
        DECLINE
    }

    private CosmiconInteraction interaction;

    @Override
    public boolean shouldShowAtMarket(MarketAPI market) {
        if (!super.shouldShowAtMarket(market)) return false;
        if (market == null) return false;
        if (market.getSize() < 3) return false;
        return true;
    }

    @Override
    public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        super.addPromptAndOption(dialog, memoryMap);

        boolean isTutorial = CosmiconStats.isInTutorialMode();
        TextPanelAPI textPanel = dialog.getTextPanel();

        if (isTutorial) {
            textPanel.addPara(Strings.get("bar_event.tutorial_prompt"));
        } else {
            textPanel.addPara(Strings.get("bar_event.standard_prompt"));
        }

        dialog.getOptionPanel().addOption(Strings.get("bar_event.approach"), this);
    }

    @Override
    public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        super.init(dialog, memoryMap);
        done = false;
        optionSelected(null, OptionId.INIT);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (!(optionData instanceof OptionId) && !(optionData instanceof String)) {
            return;
        }

        if (optionData instanceof String && interaction != null) {
            interaction.optionSelected(optionText, optionData);
            return;
        }

        if (optionData instanceof OptionId option) {

            switch (option) {
                case INIT:
                    options.clearOptions();
                    options.addOption(Strings.get("bar_event.accept"), OptionId.PLAY);
                    options.addOption(Strings.get("bar_event.decline"), OptionId.DECLINE);
                    break;

                case PLAY:
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

                            java.util.Map<String, Integer> oppPrismatic = opponentCard.getPrismaticDiceIds();
                            if (!oppPrismatic.isEmpty()) {
                                String defaultPrismaticId = oppPrismatic.keySet().iterator().next();
                                CosmiconEventState.setOpponentPrismatic(defaultPrismaticId);

                                if (CosmiconStats.isPrismaticDiceUnlocked(defaultPrismaticId)) {
                                    PrismaticDiceType diceType = PrismaticDiceRegistry.get(defaultPrismaticId);
                                    if (diceType != null && diceType.hasTrueVersion()) {
                                        CosmiconEventState.setOpponentUsesTrue(true);
                                    }
                                }
                            }
                        }
                    }

                    CosmiconEventState.setIsBarEvent(true);
                    BarEventManager.getInstance().notifyWasInteractedWith(this);

                    interaction = new CosmiconInteraction();
                    interaction.setOnLeaveAction(() -> done = true);
                    interaction.init(dialog);
                    break;

                case DECLINE:
                    text.addPara(Strings.get("bar_event.declined"));
                    done = true;
                    break;
            }
        }
    }
}
