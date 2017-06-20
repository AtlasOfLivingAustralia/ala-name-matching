package au.org.ala.names.index;

/**
 * A name key is a unique identifier for either a scientific name (code + name) or
 * a taxonomic concept (code + name + authorship)
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2017 CSIRO
 */

import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalCode;

public class NameKey implements Comparable<NameKey> {
    /** The analyser to use for comparisons */
    private NameAnalyser analyser;
    /** The nomenclatural code for scientific names */
    private NomenclaturalCode code;
    /** The scientific name */
    private String scientificName;
    /** The authorship */
    private String scientificNameAuthorship;
    /** The name type */
    private NameType type;

    /**
     * Construct a name key
     *
     * @param analyser The source analyser
     * @param code The nomenclatural code
     * @param scientificName The normalised scientific name
     * @param scientificNameAuthorship The normalised authorship, for taxon concepts (null for names)
     * @param type The type of name
     */
    public NameKey(NameAnalyser analyser, NomenclaturalCode code, String scientificName, String scientificNameAuthorship, NameType type) {
        this.analyser = analyser;
        this.code = code;
        this.scientificName = scientificName;
        this.scientificNameAuthorship = scientificNameAuthorship;
        this.type = type;
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
     * Get the type of name
     *
     * @return
     */
    public NameType getType() {
        return type;
    }


    /**
     * Is this a formaal name key, meaning that the name type represents something like a scientific name.
     *
     * @return True if this represents a formal name
     */
    public boolean isFormal() {
        return this.type != null && (this.type == NameType.SCIENTIFIC || this.type == NameType.HYBRID || this.type == NameType.CULTIVAR);
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
     * @return A name key without authorship or type
     */
    public NameKey toNameKey() {
        if (this.scientificNameAuthorship == null)
            return this;
        return new NameKey(this.analyser, this.code, this.scientificName, null, this.type);
    }

    @Override
    public String toString() {
        return "[" + code + ":" + scientificName + (scientificNameAuthorship != null ? "," + scientificNameAuthorship : "") + ']';
    }

    /**
     * Compare name keys.
     *
     * @param o The other key
     *
     * @return Less than, equal to or grater than zero for the key.
     */
    @Override
    public int compareTo(NameKey o) {
        return this.analyser.compare(this, o);
    }
}
