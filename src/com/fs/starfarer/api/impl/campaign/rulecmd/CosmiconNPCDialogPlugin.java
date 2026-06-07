package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.CosmiconMusicPlugin;
import data.scripts.cosmicon.CosmiconInteraction;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.state.CosmiconEventState;
import data.scripts.cosmicon.state.CosmiconStats;

@SuppressWarnings("unused")
public class CosmiconNPCDialogPlugin extends BaseCommandPlugin implements InteractionDialogPlugin {

    private CosmiconInteraction interaction;

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
                           List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;

        boolean isTutorial = CosmiconStats.isInTutorialMode();
        String npcMarketId = null;

        MemoryAPI personMem = memoryMap.get("person");
        String npcCharId = (personMem != null && personMem.contains("$cos_npc_char"))
            ? personMem.getString("$cos_npc_char") : null;

        if (isTutorial) {
            if (npcCharId != null) {
                CosmiconEventState.setOriginalNpcCharId(npcCharId);
            }
            int gamesPlayed = CosmiconStats.getGamesPlayed();
            String opponentId = gamesPlayed == 0 ? "trashcan" : "robin";
            CosmiconEventState.setOpponentCharacter(opponentId);
            CosmiconEventState.setIsTutorialMode(true);
        } else {
            if (npcCharId != null) {
                CosmiconEventState.setOpponentCharacter(npcCharId);
                CosmiconEventState.setOriginalNpcCharId(npcCharId);

                CharacterCard opponentCard = CharacterRegistry.getCharacterById(npcCharId);
                if (opponentCard != null) {
                    configureOpponentPrismaticDefaults(opponentCard);
                }
                if (personMem.contains("$cos_npc_market_id")) {
                    npcMarketId = personMem.getString("$cos_npc_market_id");
                }
            } else {
                assignRandomOpponent();
            }
        }

        CosmiconEventState.clearCasinoBattleState();
        CosmiconEventState.setIsEmbeddedEntry(false);

        dialog.setPlugin(this);

        final String capturedMarketId = npcMarketId;
        interaction = new CosmiconInteraction();
        interaction.setOnLeaveAction(() -> {
            if (capturedMarketId != null && CosmiconEventState.isSessionWon()) {
                MarketAPI mkt = Global.getSector().getEconomy().getMarket(capturedMarketId);
                if (mkt != null) {
                    mkt.getMemory().unset("$cos_npc_stored_char");
                }
            }
            if (CosmiconEventState.isTournamentActive()) {
                CosmiconEventState.setIsEmbeddedEntry(true);
                CosmiconEventState.setIsBarEvent(false);
                CosmiconMusicPlugin.stopMusic();
            } else {
                CosmiconEventState.clearAll();
                CosmiconMusicPlugin.stopMusic();
            }
            dialog.dismiss();
        });
        interaction.init(dialog);

        return true;
    }

    @Override
    public void init(InteractionDialogAPI dialog) {
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (interaction != null) {
            interaction.optionSelected(optionText, optionData);
        }
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
        if (interaction != null) {
            interaction.optionMousedOver(optionText, optionData);
        }
    }

    @Override
    public void advance(float amount) {
        if (interaction != null) {
            interaction.advance(amount);
        }
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {
        if (interaction != null) {
            interaction.backFromEngagement(battleResult);
        }
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }

    private void assignRandomOpponent() {
        CharacterCard opponentCard = CharacterRegistry.getRandomOpponent();
        if (opponentCard != null) {
            CosmiconEventState.setOpponentCharacter(opponentCard.getId());
            configureOpponentPrismaticDefaults(opponentCard);
        }
    }

    private void configureOpponentPrismaticDefaults(CharacterCard opponentCard) {
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
