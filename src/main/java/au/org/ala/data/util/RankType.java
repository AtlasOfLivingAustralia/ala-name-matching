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

    KINGDOM(1000, "kingdom", Rank.KINGDOM, 2f),
    SUBKINGDOM(1200, "subkingdom", Rank.Subkingdom, null),
    PHYLUM(2000, "phylum", Rank.PHYLUM, 2f),
    SUBPHYLUM(2200, "subphylum", Rank.Subphylum, null),
    SUPERCLASS(2800, "superclass", Rank.Superclass, null),
    CLASS(3000, "class", Rank.CLASS, 2f),
    SUBCLASS(3200, "subclass", Rank.Subclass, null),
    SUPERORDER(3800, "superorder", Rank.Superorder, null),
    ORDER(4000, "order", Rank.ORDER, 2f),
    SUBORDER(4200, "suborder", Rank.Suborder, null),
    INFRAORDER(4350, "infraorder", Rank.Infraorder, null),
    SUPERFAMILY(4500, "superfamily", Rank.Superfamily, null),
    FAMILY(5000, "family", Rank.FAMILY, 2f),
    SUBFAMILY(5500, "subfamily", Rank.Subfamily, null),
    TRIBE(5600, "tribe", Rank.Tribe, null),
    SUBTRIBE(5700, "subtribe", Rank.Subtribe, null),
    GENUS(6000, "genus", Rank.GENUS, 3f),
    SUBGENUS(6500, "subgenus", Rank.SUBGENUS, null),
    SECTION(6600, "section", Rank.Section, null),
    SUBSECTION(6700, "subsection", Rank.Subsection, null),
    SERIES(6800, "series", Rank.Series, null),
    SUBSERIES(6900, "subseries", Rank.Subseries, null),
    INFRAGENERICNNAME(6925, "infragenericname", Rank.InfragenericName, null),
    SPECIES(7000, "species", Rank.SPECIES, 2f),
    INFRASPECIFICNAME(8090, "infraspecificname", Rank.InfraspecificName, null),
    SUBSPECIES(8000, "subspecies", Rank.SUBSPECIES, null),
    INFRASUBSPECIESNAME(-1, "infrasubspeciesname", Rank.InfrasubspecificName, null),
    VARIETY(8010, "variety", Rank.VARIETY, null),
    SUBVARIETY(8015, "subvariety", Rank.Subvariety, null),
    FORM(8020, "form", Rank.Form, null),
    SUBFORM(-1, "subform", Rank.Subform, null),
    CULTIVARGROUP(-1, "cultivargroup", Rank.CultivarGroup, null),
    CULTIVAR(8050, "cultivar", Rank.Cultivar, null),
    INFORMAL(-1, "informal", Rank.Informal, null),
    UNRANKED(0, "unranked", Rank.Unranked, null),
    SUPRAGENERICNAME(8200, "supragenericname", Rank.SupragenericName, null),
    HYBRID(8150, "hybrid",null, null);
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
    private Float boost;
    
	private RankType(Integer id, String field, Rank rank, Float boost) {
        this.id = id;
        this.field = field;
        this.cbRank = rank;
        this.boost = boost;
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

    public Float getBoost() {
		return boost;
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


