package data.scripts.cosmicon.npc;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import data.scripts.Strings;
import data.scripts.CosmiconConfig;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.util.CosmiconRandom;

import java.util.LinkedHashMap;

public class CosmiconCampaignListener extends BaseCampaignEventListener {

    private static final String NPC_TAG = "cosmicon_npc";
    private static final String SPAWN_TIME_KEY = "$cos_npc_spawn_time";
    private static final String STORED_CHAR_KEY = "$cos_npc_stored_char";

    private static final LinkedHashMap<String, String> CHARACTER_PORTRAITS = new LinkedHashMap<>();
    static {
        CHARACTER_PORTRAITS.put("acheron", "graphics/cosmicon/portraits/Acheron.png");
        CHARACTER_PORTRAITS.put("aventurine", "graphics/cosmicon/portraits/Aventurine.png");
        CHARACTER_PORTRAITS.put("castorice", "graphics/cosmicon/portraits/Castorice.png");
        CHARACTER_PORTRAITS.put("cyrene", "graphics/cosmicon/portraits/Cyrene.png");
        CHARACTER_PORTRAITS.put("dan_heng", "graphics/cosmicon/portraits/Dan Heng.png");
        CHARACTER_PORTRAITS.put("firefly", "graphics/cosmicon/portraits/Firefly.png");
        CHARACTER_PORTRAITS.put("hyacine", "graphics/cosmicon/portraits/Hyacine.png");
        CHARACTER_PORTRAITS.put("kafka", "graphics/cosmicon/portraits/Kafka.png");
        CHARACTER_PORTRAITS.put("march_7th", "graphics/cosmicon/portraits/March 7th.png");
        CHARACTER_PORTRAITS.put("phainon", "graphics/cosmicon/portraits/Phainon.png");
        CHARACTER_PORTRAITS.put("robin", "graphics/cosmicon/portraits/Robin.png");
        CHARACTER_PORTRAITS.put("sparxie", "graphics/cosmicon/portraits/Sparxie.png");
        CHARACTER_PORTRAITS.put("the_herta", "graphics/cosmicon/portraits/The Herta.png");
        CHARACTER_PORTRAITS.put("yao_guang", "graphics/cosmicon/portraits/Yao Guang.png");
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

        MemoryAPI marketMem = market.getMemory();
        if (marketMem.contains(SPAWN_TIME_KEY)) {
            long lastTimestamp = marketMem.getLong(SPAWN_TIME_KEY);
            if (Global.getSector().getClock().getElapsedDaysSince(lastTimestamp) < CosmiconConfig.NPC_SPAWN_INTERVAL_DAYS) {
                if (marketMem.contains(STORED_CHAR_KEY)) {
                    String charId = marketMem.getString(STORED_CHAR_KEY);
                    CharacterCard card = CharacterRegistry.getCharacterById(charId);
                    if (card != null) {
                        spawnTempNPC(market, card);
                    }
                }
                return;
            }
        }

        if (CosmiconRandom.nextFloat() >= CosmiconConfig.NPC_SPAWN_CHANCE) {
            marketMem.set(SPAWN_TIME_KEY, Global.getSector().getClock().getTimestamp());
            return;
        }

        CharacterCard opponentCard = CharacterRegistry.getRandomOpponent();
        if (opponentCard == null) return;
        spawnTempNPC(market, opponentCard);
        marketMem.set(SPAWN_TIME_KEY, Global.getSector().getClock().getTimestamp());
        marketMem.set(STORED_CHAR_KEY, opponentCard.getId());
    }

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {
        if (market == null) return;
        removeTempNPCs(market);
    }

    private void spawnTempNPC(MarketAPI market, CharacterCard opponentCard) {
        String characterId = opponentCard.getId();
        String portraitSprite = CHARACTER_PORTRAITS.get(characterId);
        if (portraitSprite == null) {
            portraitSprite = "graphics/portraits/portrait_generic.png";
        }

        PersonAPI person = Global.getFactory().createPerson();
        String personId = "cosmicon_npc_" + market.getId();
        person.setId(personId);
        person.setFaction(Factions.INDEPENDENT);

        String charName = opponentCard.getName();
        int spaceIdx = charName.indexOf(' ');
        if (spaceIdx > 0) {
            person.getName().setFirst(charName.substring(0, spaceIdx));
            person.getName().setLast(charName.substring(spaceIdx + 1));
        } else {
            person.getName().setFirst(charName);
            person.getName().setLast("");
        }

        person.setPortraitSprite(portraitSprite);
        person.addTag(NPC_TAG);

        String charTitle = Strings.get("character." + characterId + ".title");
        if (charTitle != null && !charTitle.isEmpty()) {
            person.setPostId(charTitle);
        } else {
            person.setPostId(Ranks.POST_ENTREPRENEUR);
        }
        person.setRankId(Ranks.CITIZEN);

        person.getMemory().set("$cos_npc_char", characterId);
        person.getMemory().set("$cos_npc_market_id", market.getId());

        String openLine = Strings.get("character." + characterId + ".open");
        if (openLine != null && !openLine.isEmpty()) {
            person.getMemory().set("$cos_npc_open", openLine);
        }
        if (charTitle != null && !charTitle.isEmpty()) {
            person.getMemory().set("$cos_npc_title", charTitle);
        }

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
