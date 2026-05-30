package data.scripts.cosmicon.events;

import java.util.Map;

import com.fs.starfarer.api.Global;
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
import data.scripts.cosmicon.casino.CasinoIntegrationManager;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.state.CosmiconEventState;
import data.scripts.cosmicon.state.CosmiconStats;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.CosmiconConfig;

// Retained for stale save compatibility: bar events from older saves may still reference this class.
// New bar events are no longer generated (creator registration removed from CosmiconModPlugin).
// NPC dialogue (CosmiconNPCDialogPlugin) is now the standalone entry point.
public class CosmiconBarEvent extends BaseBarEvent {

    public enum OptionId {
        INIT,
        PLAY,
        DECLINE
    }

    private CosmiconInteraction interaction;

    @Override
    public boolean shouldShowAtMarket(MarketAPI market) {
        if (!super.shouldShowAtMarket(market)) {
            if (CosmiconConfig.DEBUG_ENABLED) {
                Global.getLogger(this.getClass()).info("Cosmicon shouldShowAtMarket REJECTED by super: " +
                        (market != null ? market.getName() + " (size " + market.getSize() + ")" : "null"));
            }
            return false;
        }
        if (market == null) {
            if (CosmiconConfig.DEBUG_ENABLED) {
                Global.getLogger(this.getClass()).info("Cosmicon shouldShowAtMarket REJECTED: market is null");
            }
            return false;
        }
        if (market.getSize() < CosmiconConfig.MARKET_SIZE_MIN) {
            if (CosmiconConfig.DEBUG_ENABLED) {
                Global.getLogger(this.getClass()).info("Cosmicon shouldShowAtMarket REJECTED: " +
                        market.getName() + " size " + market.getSize() + " < min " + CosmiconConfig.MARKET_SIZE_MIN);
            }
            return false;
        }
        if (CosmiconConfig.DEBUG_ENABLED) {
            Global.getLogger(this.getClass()).info("Cosmicon shouldShowAtMarket ACCEPTED: " +
                    market.getName() + " (size " + market.getSize() + ")");
        }
        return true;
    }

    @Override
    public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        super.addPromptAndOption(dialog, memoryMap);

        if (CosmiconConfig.DEBUG_ENABLED) {
            Global.getLogger(this.getClass()).info("Cosmicon addPromptAndOption called - event appearing at bar");
        }

        boolean isTutorial = CosmiconStats.isInTutorialMode();
        TextPanelAPI textPanel = dialog.getTextPanel();

        if (CasinoIntegrationManager.isCasinoLoaded()) {
            int hunterLevel = CasinoIntegrationManager.getTrashcanHunterLevel();
            if (hunterLevel > 0) {
                textPanel.addPara(Strings.format("bar_event.trashcan_hunter_prompt", hunterLevel));
                dialog.getOptionPanel().addOption(Strings.get("bar_event.approach"), this);
                return;
            }
        }

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
                            opponentId = CharacterIds.TRASHCAN;
                        } else {
                            opponentId = CharacterIds.ROBIN;
                        }
                        CosmiconEventState.setOpponentCharacter(opponentId);
                        CosmiconEventState.setIsTutorialMode(true);
                    } else {
                        CharacterCard opponentCard = CharacterRegistry.getRandomOpponent();
                        if (opponentCard != null) {
                            CosmiconEventState.setOpponentCharacter(opponentCard.getId());

                            Map<String, Integer> oppPrismatic = opponentCard.getPrismaticDiceIds();
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

                    CosmiconEventState.clearCasinoBattleState();
                    CosmiconEventState.setIsStandaloneEntry(true);
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