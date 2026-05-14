package data.scripts.cosmicon.npc;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import data.scripts.CosmiconConfig;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.util.CosmiconRandom;

import java.util.LinkedHashMap;

public class CosmiconCampaignListener extends BaseCampaignEventListener {

    private static final String NPC_TAG = "cosmicon_npc";

    private static final String[] FIRST_NAMES = {
        "Dice", "Roller", "Gambler", "Shaper", "Strategist",
        "Fortune", "Chance", "Wager", "Maverick", "Blaze"
    };

    private static final LinkedHashMap<String, String> CHARACTER_PORTRAITS = new LinkedHashMap<>();
    static {
        CHARACTER_PORTRAITS.put("acheron", "graphics/cosmicon/portraits/Acheron");
        CHARACTER_PORTRAITS.put("aventurine", "graphics/cosmicon/portraits/Aventurine");
        CHARACTER_PORTRAITS.put("castorice", "graphics/cosmicon/portraits/Castorice");
        CHARACTER_PORTRAITS.put("cyrene", "graphics/cosmicon/portraits/Cyrene");
        CHARACTER_PORTRAITS.put("dan_heng", "graphics/cosmicon/portraits/Dan Heng");
        CHARACTER_PORTRAITS.put("firefly", "graphics/cosmicon/portraits/Firefly");
        CHARACTER_PORTRAITS.put("hyacine", "graphics/cosmicon/portraits/Hyacine");
        CHARACTER_PORTRAITS.put("kafka", "graphics/cosmicon/portraits/Kafka");
        CHARACTER_PORTRAITS.put("march_7th", "graphics/cosmicon/portraits/March 7th");
        CHARACTER_PORTRAITS.put("phainon", "graphics/cosmicon/portraits/Phainon");
        CHARACTER_PORTRAITS.put("robin", "graphics/cosmicon/portraits/Robin");
        CHARACTER_PORTRAITS.put("sparxie", "graphics/cosmicon/portraits/Sparxie");
        CHARACTER_PORTRAITS.put("the_herta", "graphics/cosmicon/portraits/The Herta");
        CHARACTER_PORTRAITS.put("yao_guang", "graphics/cosmicon/portraits/Yao Guang");
    }

    public CosmiconCampaignListener() {
        super(true);
    }

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {
        if (market == null) return;
        if (!CosmiconConfig.NPC_ENABLED) return;

        for (PersonAPI person : market.getPeopleCopy()) {
            if (person.hasTag(NPC_TAG)) {
                return;
            }
        }

        if (CosmiconRandom.nextFloat() >= CosmiconConfig.NPC_SPAWN_CHANCE) return;

        spawnTempNPC(market);
    }

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {
        if (market == null) return;
        removeTempNPCs(market);
    }

    private void spawnTempNPC(MarketAPI market) {
        CharacterCard opponentCard = CharacterRegistry.getRandomOpponent();
        if (opponentCard == null) return;

        String characterId = opponentCard.getId();
        String portraitSprite = CHARACTER_PORTRAITS.get(characterId);
        if (portraitSprite == null) {
            portraitSprite = "graphics/portraits/portrait_generic.png";
        }

        PersonAPI person = Global.getFactory().createPerson();
        String personId = "cosmicon_npc_" + market.getId();
        person.setId(personId);
        person.setFaction(Factions.INDEPENDENT);

        String prefix = FIRST_NAMES[CosmiconRandom.nextInt(FIRST_NAMES.length)];
        person.getName().setFirst(prefix);
        person.getName().setLast(opponentCard.getName());

        person.setPortraitSprite(portraitSprite);
        person.addTag(NPC_TAG);
        person.getMemory().set("$cos_npc_char", characterId);

        market.getCommDirectory().addPerson(person, 0);
        market.addPerson(person);

        ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
        ip.addPerson(person);
        ip.checkOutPerson(person, "permanent_staff");

        Global.getLogger(this.getClass()).info(
            "Cosmicon temp NPC spawned: " + personId + " at " + market.getName());
    }

    private void removeTempNPCs(MarketAPI market) {
        ImportantPeopleAPI ip = Global.getSector().getImportantPeople();

        for (PersonAPI person : market.getPeopleCopy()) {
            if (person.hasTag(NPC_TAG)) {
                market.getCommDirectory().removePerson(person);
                market.removePerson(person);
                ip.removePerson(person);
                Global.getLogger(this.getClass()).info(
                    "Cosmicon temp NPC removed: " + person.getId());
            }
        }
    }
}
