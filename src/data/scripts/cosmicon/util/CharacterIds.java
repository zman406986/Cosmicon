package data.scripts.cosmicon.util;

/**
 * Centralized character ID constants for Cosmicon Dice.
 * Use these constants instead of string literals for type safety and maintainability.
 * All IDs are lowercase with underscores separating words.
 */
import java.util.List;

public final class CharacterIds {
    public static final String AVENTURINE = "aventurine";
    public static final String FIREFLY = "firefly";
    public static final String ACHERON = "acheron";
    public static final String KAFKA = "kafka";
    public static final String ROBIN = "robin";
    public static final String MARCH_7TH = "march_7th";
    public static final String DAN_HENG = "dan_heng";
    public static final String THE_HERTA = "the_herta";
    public static final String CYRENE = "cyrene";
    public static final String CASTORICE = "castorice";
    public static final String PHAINON = "phainon";
    public static final String SPARXIE = "sparxie";
    public static final String HYACINE = "hyacine";
    public static final String YAO_GUANG = "yao_guang";
    public static final String TRASHCAN = "trashcan";
    public static final String TRASHCAN_2STAR = "trashcan_2star";
    public static final String CHIMERA = "chimera";
    public static final String DROMAS = "dromas";
    public static final String AUTOMATON_BEETLE = "automaton_beetle";
    public static final String FURBO_JOURNALIST = "furbo_journalist";
    public static final String BANANADVISOR = "bananadvisor";
    public static final String SENIOR_STAFF = "senior_staff";

    public static final List<String> EASY_MODE_CHARACTERS = List.of(
        CHIMERA, DROMAS, AUTOMATON_BEETLE, TRASHCAN_2STAR,
        FURBO_JOURNALIST, BANANADVISOR, SENIOR_STAFF
    );

    public static final String TUTORIAL_1_DEFAULT_CHARACTER = CHIMERA;

    public static List<String> getAllIds() {
        return List.of(AVENTURINE, FIREFLY, ACHERON, KAFKA, ROBIN, MARCH_7TH, DAN_HENG,
            THE_HERTA, CYRENE, CASTORICE, PHAINON, SPARXIE, HYACINE, YAO_GUANG,
            TRASHCAN, TRASHCAN_2STAR, CHIMERA, DROMAS, AUTOMATON_BEETLE,
            FURBO_JOURNALIST, BANANADVISOR, SENIOR_STAFF);
    }

    private CharacterIds() {}
}