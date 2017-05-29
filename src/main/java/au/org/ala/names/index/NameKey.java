package au.org.ala.names.index;

/**
 * A name key is a unique identifier for either a scientific name (code + name) or
 * a taxonomic concept (code + name + authorship)
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2017 CSIRO
 */

import org.gbif.api.vocabulary.NomenclaturalCode;

public class NameKey {
    /** The analyser to use for comparisons */
    private NameAnalyser analyser;
    /** The nomenclatural code for scientific names */
    private NomenclaturalCode code;
    /** The scientific name */
    private String scientificName;
    /** The authorship */
    private String scientificNameAuthorship;

    /**
     * Construct a name key
     *
     * @param analyser The source analyser
     * @param code The nomenclatural code
     * @param scientificName The normalised scientific name
     * @param scientificNameAuthorship The normalised authorship, for taxon concepts (null for names)
     */
    public NameKey(NameAnalyser analyser, NomenclaturalCode code, String scientificName, String scientificNameAuthorship) {
        this.analyser = analyser;
        this.code = code;
        this.scientificName = scientificName;
        this.scientificNameAuthorship = scientificNameAuthorship;
    }

    /**
     * Get the name analyser for this key.
     * <p>
     * This is, essentially the analyser used to build the key
     * </p>
     *
     * @return The name analyser
     */
    public NameAnalyser getAnalyser() {
        return analyser;
    }

    /**
     * Get the normenclatural code for this key.
     *
     * @return The normenclatural code
     */
    public NomenclaturalCode getCode() {
        return code;
    }

    /**
     * Get the normalised scientific name
     *
     * @return The scientific name
     */
    public String getScientificName() {
        return scientificName;
    }

    /**
     * Get the normalised scientific name authorship
     */
    public String getScientificNameAuthorship() {
        return scientificNameAuthorship;
    }

    /**
     * Compare for equality with another name key
     * <p>
     * Name keys are equal if the analyser says they are
     * </p>
     *
     * @param o The other object
     *
     * @return True if the name keys are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NameKey nameKey = (NameKey) o;
        return this.analyser.compare(this, nameKey) == 0;
    }

    /**
     * Compute a has code for the name key
     *
     * @return A hash code from the analyser
     */
    @Override
    public int hashCode() {
        return this.analyser.hashCode(this);
    }

    /**
     * Convert a full taxon key into a name-only key.
     *
     * @return A name key without authorship
     */
    public NameKey toNameKey() {
        if (this.scientificNameAuthorship == null)
            return this;
        return new NameKey(this.analyser, this.code, this.scientificName, null);
    }
}
