package au.org.ala.data.util;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * An enumeration of the rank types available in ALA.  The rank names come
 * from Checklist Bank and the identifiers come from ALA Portal.
 *
 * This is based on org.ala.web.util.RankFacet
 *
 * @author Natasha
 */
public enum RankType {

    KINGDOM(1000, "kingdom"),
    SUBKINGDOM(1200, "subkingdom"),
    PHYLUM(2000, "phylum"),
    SUBPHYLUM(2200, "subphylum"),
    SUPERCLASS(2800, "superclass"),
    CLASS(3000, "class"),
    SUBCLASS(3200, "subclass"),
    SUPERORDER(3800, "superorder"),
    ORDER(4000, "order"),
    SUBORDER(4200, "suborder"),
    INFRAORDER(4350, "infraorder"),
    SUPERFAMILY(4500, "superfamily"),
    FAMILY(5000, "family"),
    SUBFAMILY(5500, "subfamily"),
    TRIBE(5600, "tribe"),
    SUBTRIBE(5700, "subtribe"),
    GENUS(6000, "genus"),
    SUBGENUS(6500, "subgenus"),
    SECTION(6600, "section"),
    SUBSECTION(6700, "subsection"),
    SERIES(6800, "series"),
    SUBSERIES(6900, "subseries"),
    INFRAGENERICNNAME(6925, "infragenericname"),
    SPECIES(7000, "species"),
    INFRASPECIFICNAME(8090, "infraspecificname"),
    SUBSPECIES(8000, "subspecies"),
    INFRASUBSPECIESNAME(-1, "infrasubspeciesname"),
    VARIETY(8010, "variety"),
    SUBVARIETY(8015, "subvariety"),
    FORM(8020, "form"),
    SUBFORM(-1, "subform"),
    CULTIVARGROUP(-1, "cultivargroup"),
    CULTIVAR(8050, "cultivar"),
    INFORMAL(-1, "informal"),
    UNRANKED(0, "unranked"),
    SUPRAGENERICNAME(8200, "supragenericname");
    // Allow reverse-lookup (based on http://www.ajaxonomy.com/2007/java/making-the-most-of-java-50-enum-tricks)
    private static final Map<String, RankType> fieldLookup = new HashMap<String, RankType>();
    private static final Map<Integer, RankType> idLookup = new HashMap<Integer, RankType>();

    static {
        for (RankType rt : EnumSet.allOf(RankType.class)) {
            fieldLookup.put(rt.getRank(), rt);
            idLookup.put(rt.getId(), rt);
        }
    }
    private Integer id;
    private String field;

    private RankType(Integer id, String field) {
        this.id = id;
        this.field = field;
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

    /**
     * @param id
     * @return The RankType for the specified Portal id
     */
    public static RankType getForId(Integer id) {
        return idLookup.get(id);
    }
}


