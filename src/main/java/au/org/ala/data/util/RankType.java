package au.org.ala.data.util;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.gbif.ecat.voc.Rank;

/**
 * An enumeration of the rank types available in ALA.  The rank names come
 * from Checklist Bank and the identifiers come from ALA Portal.
 *
 * This is based on org.ala.web.util.RankFacet
 *
 * @author Natasha
 */
public enum RankType {

    KINGDOM(1000, "kingdom", Rank.KINGDOM),
    SUBKINGDOM(1200, "subkingdom", Rank.Subkingdom),
    PHYLUM(2000, "phylum", Rank.PHYLUM),
    SUBPHYLUM(2200, "subphylum", Rank.Subphylum),
    SUPERCLASS(2800, "superclass", Rank.Superclass),
    CLASS(3000, "class", Rank.CLASS),
    SUBCLASS(3200, "subclass", Rank.Subclass),
    SUPERORDER(3800, "superorder", Rank.Superorder),
    ORDER(4000, "order", Rank.ORDER),
    SUBORDER(4200, "suborder", Rank.Suborder),
    INFRAORDER(4350, "infraorder", Rank.Infraorder),
    SUPERFAMILY(4500, "superfamily", Rank.Superfamily),
    FAMILY(5000, "family", Rank.FAMILY),
    SUBFAMILY(5500, "subfamily", Rank.Subfamily),
    TRIBE(5600, "tribe", Rank.Tribe),
    SUBTRIBE(5700, "subtribe", Rank.Subtribe),
    GENUS(6000, "genus", Rank.GENUS),
    SUBGENUS(6500, "subgenus", Rank.SUBGENUS),
    SECTION(6600, "section", Rank.Section),
    SUBSECTION(6700, "subsection", Rank.Subsection),
    SERIES(6800, "series", Rank.Series),
    SUBSERIES(6900, "subseries", Rank.Subseries),
    INFRAGENERICNNAME(6925, "infragenericname", Rank.InfragenericName),
    SPECIES(7000, "species", Rank.SPECIES),
    INFRASPECIFICNAME(8090, "infraspecificname", Rank.InfraspecificName),
    SUBSPECIES(8000, "subspecies", Rank.SUBSPECIES),
    INFRASUBSPECIESNAME(-1, "infrasubspeciesname", Rank.InfrasubspecificName),
    VARIETY(8010, "variety", Rank.VARIETY),
    SUBVARIETY(8015, "subvariety", Rank.Subvariety),
    FORM(8020, "form", Rank.Form),
    SUBFORM(-1, "subform", Rank.Subform),
    CULTIVARGROUP(-1, "cultivargroup", Rank.CultivarGroup),
    CULTIVAR(8050, "cultivar", Rank.Cultivar),
    INFORMAL(-1, "informal", Rank.Informal),
    UNRANKED(0, "unranked", Rank.Unranked),
    SUPRAGENERICNAME(8200, "supragenericname", Rank.SupragenericName),
    HYBRID(8150, "hybrid",null);
    // Allow reverse-lookup (based on http://www.ajaxonomy.com/2007/java/making-the-most-of-java-50-enum-tricks)
    private static final Map<String, RankType> fieldLookup = new HashMap<String, RankType>();
    private static final Map<Integer, RankType> idLookup = new HashMap<Integer, RankType>();
    private static final Map<Rank, RankType> cbRankLookup = new HashMap<Rank, RankType>();

    static {
        for (RankType rt : EnumSet.allOf(RankType.class)) {
            fieldLookup.put(rt.getRank(), rt);
            idLookup.put(rt.getId(), rt);
            if(rt.cbRank != null)
                cbRankLookup.put(rt.cbRank,rt);
        }
    }
    private Integer id;
    private String field;
    private Rank cbRank;

    private RankType(Integer id, String field, Rank rank) {
        this.id = id;
        this.field = field;
        this.cbRank = rank;
    }
    public static Set<RankType> getAllRanksBelow(Integer rank){
        Set<RankType> ranks = new TreeSet<RankType>();
        for(RankType rt : EnumSet.allOf(RankType.class)){
            if(rt.getId() >= rank)
                ranks.add(rt);
        }
        return ranks;
    }

    /**
     * @return id the id
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * @return field the field
     */
    public String getRank() {
        return this.field;
    }

    /**
     * @param field
     * @return The rankType for the field
     */
    public static RankType getForName(String field) {
        return fieldLookup.get(field);
    }

    public static RankType getForCBRank(Rank cbRank){
        return cbRankLookup.get(cbRank);
    }

    /**
     * @param id
     * @return The RankType for the specified Portal id
     */
    public static RankType getForId(Integer id) {
        return idLookup.get(id);
    }
    //invertibrate phylums: Porifera, Cnidaria, Platyhelminthes, Nematoda, Mollusca, Annelida, Arthropoda (sub phylum except no access), Echinodermata by phylum
    //vertibrates:
    //Chordates - by class
}


