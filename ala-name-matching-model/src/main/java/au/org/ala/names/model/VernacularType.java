package au.org.ala.names.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Types of vernacular names.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2018 Atlas of Living Australia
 */
public enum VernacularType {
    STANDARD("standard", 900, false),
    PREFERRED("preferred", 800, false),
    COMMON("common", 700, false, "vernacular"),
    TRADITIONAL_KNOWLEDGE("traditionalKnowledge", 600, false, "traditional knowledge", "traditional"),
    LEGISLATIVE("legislated", 500, true, "legislative"),
    MISCELLANEOUS_LITERATURE("miscellaneousLiterature", 400, true, "miscellaneous literature", "misc lit"),
    LOCAL("local", 300, false);

    private static Map<String, VernacularType> nameMap = new HashMap<>();

    static {
        for (VernacularType type: VernacularType.values()) {
            nameMap.put(type.term, type);
            for (String alt: type.altTerms)
                nameMap.put(alt, type);
        }
    }

    /** The preferred term */
    private String term;
    /** Any other terms that might descript this name */
    private String[] altTerms;
    /** Is this a pseudo-scientific name? */
    private boolean pseudoScientific;
    /** The name priority */
    private int priority;

    VernacularType(String term, int priority, boolean pseudoScientific, String... altTerms) {
        this.term = term;
        this.priority = priority;
        this.pseudoScientific = pseudoScientific;
        this.altTerms = altTerms;
    }

    /**
     * Get the vocabulary term
     *
     * @return The term
     */
    public String getTerm() {
        return term;
    }

    /**
     * Is this a pseudo-scientific name?
     * <p>
     * Pseudo-scientific names look like a latinised scientific name but are a by-product of other sources
     * and are treated as vernacular names.
     * </p>
     *
     * @return True is pseudo-scientific
     */
    public boolean isPseudoScientific() {
        return pseudoScientific;
    }

    /**
     * Get the priority, higher names should come first.
     *
     * @return The priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Get a type based on a vocabulary term.
     *
     * @param term The term to look up
     * @param dft The default value if not found
     *
     * @return The type
     */
    public static VernacularType forTerm(String term, VernacularType dft) {
        return nameMap.getOrDefault(term, dft);
    }
}
