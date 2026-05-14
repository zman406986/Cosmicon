package data.scripts.cosmicon.npc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI.PersonDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;

import java.util.ArrayList;

public class CosmiconNPCManager {

    static final String PERSISTENT_DATA_KEY = "cosmicon_npc_registry";
    static final String NPC_TAG = "cosmicon_npc";

    private CosmiconNPCManager() {}

    public static void cleanupAllNPCs() {
        ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
        java.util.List<PersonAPI> toRemove = new ArrayList<>();
        for (PersonDataAPI data : ip.getPeopleCopy()) {
            PersonAPI person = data.getPerson();
            if (person != null && person.hasTag(NPC_TAG)) {
                toRemove.add(person);
            }
        }
        for (PersonAPI person : toRemove) {
            String marketId = person.getId().replace("cosmicon_npc_", "");
            MarketAPI market = Global.getSector().getEconomy().getMarket(marketId);
            if (market != null) {
                market.getCommDirectory().removePerson(person);
                market.removePerson(person);
            }
            ip.removePerson(person);
        }
        Global.getSector().getPersistentData().remove(PERSISTENT_DATA_KEY);
        Global.getLogger(CosmiconNPCManager.class).info(
            "Cosmicon: Cleaned up " + toRemove.size() + " persistent NPCs");
    }
}
