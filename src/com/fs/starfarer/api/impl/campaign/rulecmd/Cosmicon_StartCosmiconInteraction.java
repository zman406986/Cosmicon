package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.cosmicon.CosmiconInteraction;
import data.scripts.CosmiconConfig;
import data.scripts.Strings;

import java.awt.Color;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class Cosmicon_StartCosmiconInteraction extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        
        try {
            MarketAPI currentMarket = dialog.getInteractionTarget().getMarket();
            if (currentMarket != null) {
                int marketSize = currentMarket.getSize();
                
                if (marketSize < CosmiconConfig.MARKET_SIZE_MIN) {
                    dialog.getTextPanel().addPara(Strings.get("errors.market_too_small"), Color.RED);
                    return false;
                }
            }

            Global.getLogger(this.getClass()).info("Starting Cosmicon interaction");

            CosmiconInteraction.startInteraction(dialog);
            return true;
        } catch (Exception e) {
            Global.getLogger(this.getClass()).error("Error starting Cosmicon interaction", e);
            return false;
        }
    }
}