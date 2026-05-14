package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.CosmiconMusicPlugin;
import data.scripts.cosmicon.CosmiconInteraction;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.state.CosmiconEventState;
import data.scripts.cosmicon.state.CosmiconStats;

public class CosmiconNPCDialogPlugin extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
                           List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;

        boolean isTutorial = CosmiconStats.isInTutorialMode();

        if (isTutorial) {
            int gamesPlayed = CosmiconStats.getGamesPlayed();
            String opponentId = gamesPlayed == 0 ? "trashcan" : "robin";
            CosmiconEventState.setOpponentCharacter(opponentId);
            CosmiconEventState.setIsTutorialMode(true);
        } else {
            MemoryAPI personMem = memoryMap.get("person");
            if (personMem != null && personMem.contains("$cos_npc_char")) {
                String characterId = personMem.getString("$cos_npc_char");
                CosmiconEventState.setOpponentCharacter(characterId);

                CharacterCard opponentCard = CharacterRegistry.getCharacterById(characterId);
                if (opponentCard != null) {
                    configureOpponentPrismaticDefaults(opponentCard);
                }
            } else {
                assignRandomOpponent();
            }
        }

        CosmiconInteraction interaction = new CosmiconInteraction();
        interaction.setOnLeaveAction(() -> {
            CosmiconEventState.clearAll();
            CosmiconMusicPlugin.stopMusic();
            dialog.dismiss();
        });
        dialog.setPlugin(interaction);
        interaction.init(dialog);

        return true;
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
