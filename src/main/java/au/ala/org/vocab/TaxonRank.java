package au.ala.org.vocab;

/**
 * A taxonomic rank
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class TaxonRank extends Concept<TaxonRank> {
    /** The rank level */
    private int level;
    /** Is this rank comparable */
    private boolean comparable;
    /** Is this one of the standard linnaean ranks? */
    private boolean linnaean;

    public TaxonRank() {
    }

    public TaxonRank(Vocabulary<TaxonRank> vocabulary, String id, int level, boolean comparable, boolean linnaean, String... names) {
        super(vocabulary, id, names);
        this.level = level;
        this.comparable = comparable;
        this.linnaean = linnaean;
    }

    /**
     * The rank level.
     * <p>
     * Larger indicates a lower order (more specific) taxon
     * </p>
     *
     * @return The level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Is this a comparable rank?
     * <p>
     * Comparable ranks should have taxa with parent-child in order.
     * Non-comparable ranks indicate a
     * </p>
     *
     * @return True if the rank is comparable
     */
    public boolean isComparable() {
        return comparable;
    }

    /**
     * Is this a Linnaean rank?
     * <p>
     * One of the big seven (kingdom, phylum, class, order, familty, genus, species)
     * </p>
     *
     * @return True if a linnaean rank
     */
    public boolean isLinnaean() {
        return linnaean;
    }
}
