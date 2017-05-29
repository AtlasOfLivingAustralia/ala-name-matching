package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import au.org.ala.names.model.SynonymType;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.TaxonomicStatus;

import java.util.Set;

/**
 * An instance of a taxonomic concept from a particular source.
 * <p>
 * This contains enough information to allow us to resolve
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class TaxonConceptInstance {
    /** The parent taxon concept */
    private TaxonConcept taxonConcept;
    /** The taxon identifier */
    private String taxonID;
    /** The nomenclatural code */
    private NomenclaturalCode code;
    /** The name source */
    private NameProvider provider;
    /** The supplied name */
    private String scientificName;
    /** The supplied scientific name authorship */
    private String scientificNameAuthorship;
    /** The year of publication, if available */
    private String year;
    /** The taxonomic status */
    private TaxonomicStatus taxonomicStatus;
    /** The synonym type */
    private SynonymType synonymType;
    /** The rank */
    private RankType rank;
    /** The status of the name */
    private Set<NomenclaturalStatus> status;
    /** The parent name usage identifier, for accepted names */
    private String parentNameUsageID;
    /** The parent taxon */
    private TaxonConceptInstance parent;
    /** The accepted name usage identier, for synonyms */
    private String acceptedNameUsageID;
    /** The accepted taxon */
    private TaxonConceptInstance accepted;

    /**
     * Construct a new taxon concept instance
     *
     * @param taxonID The unique taxon identifier
     * @param code The nomenclatural code for the taxon
     * @param provider The name provider
     * @param scientificName The scientific name
     * @param scientificNameAuthorship The scientific name authorship
     * @param year The year of publication
     * @param taxonomicStatus The taxonomic status
     * @param synonymType The synonym type
     * @param rank The taxon rank
     * @param status The nomenclatural status
     * @param parentNameUsageID A link to the parent taxon, for accepted taxa
     * @param acceptedNameUsageID A link the the accepted taxon, for synonyms
     */
    public TaxonConceptInstance(String taxonID, NomenclaturalCode code, NameProvider provider, String scientificName, String scientificNameAuthorship, String year, TaxonomicStatus taxonomicStatus, SynonymType synonymType, RankType rank, Set<NomenclaturalStatus> status, String parentNameUsageID, String acceptedNameUsageID) {
        this.taxonID = taxonID;
        this.code = code;
        this.provider = provider;
        this.scientificName = scientificName;
        this.scientificNameAuthorship = scientificNameAuthorship;
        this.year = year;
        this.taxonomicStatus = taxonomicStatus;
        this.synonymType = synonymType;
        this.rank = rank;
        this.status = status;
        this.parentNameUsageID = parentNameUsageID;
        this.acceptedNameUsageID = acceptedNameUsageID;
    }

    /**
     * Get the parent taxon concept.
     *
     * @return The parent taxon concept
     */
    public TaxonConcept getTaxonConcept() {
        return taxonConcept;
    }

    /**
     * Set the parent taxon concept.
     *
     * @param taxonConcept The new taxon concept.
     */
    public void setTaxonConcept(TaxonConcept taxonConcept) {
        this.taxonConcept = taxonConcept;
    }

    /**
     * Get the nomenclatural code for this taxon
     *
     * @return The nomenclatural code
     */
    public NomenclaturalCode getCode() {
        return code;
    }

    /**
     * Get the taxon identifier, as specified by the source.
     *
     * @return The taxon identifier
     */
    public String getTaxonID() {
        return taxonID;
    }

    /**
     * Get the originating source for the data
     *
     * @return The name source
     */
    public NameProvider getProvider() {
        return provider;
    }

    /**
     * Get the scientific name, without authorship.
     *
     * @return The scientific name
     */
    public String getScientificName() {
        return scientificName;
    }

    /**
     * Get the scientific name authorship.
     *
     * @return The authorship.
     */
    public String getScientificNameAuthorship() {
        return scientificNameAuthorship;
    }

    /**
     * Get the year of publication.
     *
     * @return The year of publication.
     */
    public String getYear() {
        return year;
    }

    /**
     * Get the GBIF taxonomic status.
     * <p>
     * This gives a rough taxonomic status. The ALA synonym type gives more accurate synonyms.
     * </p>
     *
     * @return The taxonomic status.
     */
    public TaxonomicStatus getTaxonomicStatus() {
        return taxonomicStatus;
    }

    /**
     * If this is a synonym, get the synonym type.
     *
     * @return The ALA synonym type.
     */
    public SynonymType getSynonymType() {
        return synonymType;
    }

    /**
     * Get the taxon rank.
     *
     * @return The taxon rank.
     */
    public RankType getRank() {
        return rank;
    }

    /**
     * Get any nomenclatural status descriptions.
     *
     * @return The nomenclatural status
     */
    public Set<NomenclaturalStatus> getStatus() {
        return status;
    }

    /**
     * Get the taxonID of the parent for accepted taxa.
     *
     * @return The parent taxonID
     */
    public String getParentNameUsageID() {
        return parentNameUsageID;
    }

    /**
     * Get the resolved parent.
     *
     * @return The parent taxon concept instance.
     */
    public TaxonConceptInstance getParent() {
        return parent;
    }

    /**
     * Get the taxonID of the accepted name for synonyms.
     *
     * @return The accepted taxonID
     */
    public String getAcceptedNameUsageID() {
        return acceptedNameUsageID;
    }

    /**
     * Get the resolved accepted taxon concept instance.
     *
     * @return The resolved taxon concept.
     */
    public TaxonConceptInstance getAccepted() {
        return accepted;
    }

    /**
     * Clean up common messinesses with the instance caused by different conventions:
     * <ul>
     *     <li>If the acceptedNameUsageID is the same as the taxonID then it is set to null</li>
     *     <li>If the scientificName has the scientificNameAuthorship in it then the authorship is removed</li>
     * </ul>
     * @throws IndexBuilderException
     */
    public void normalise() throws IndexBuilderException {
        if (this.acceptedNameUsageID != null && this.acceptedNameUsageID.equals(this.taxonID))
            this.acceptedNameUsageID = null;
        if (this.scientificNameAuthorship != null) {
            int pos = this.scientificName.indexOf(this.scientificNameAuthorship);
            if (pos >= 0) {
                this.scientificName = this.scientificName.substring(0, pos) + this.scientificName.substring(pos + this.scientificNameAuthorship.length());
                this.scientificName = this.scientificName.trim();
            }
        }
    }

    /**
     * Make links to the actual parent and accepted instances.
     *
     * @param taxonomy The current taxonomy
     *
     * @throws IndexBuilderException If unable to make a link, usually due to a broken reference
     */
    public void resolveLinks(Taxonomy taxonomy) throws IndexBuilderException {
        if (this.parentNameUsageID != null) {
            this.parent = taxonomy.getInstance(this.parentNameUsageID);
            if (this.parent == null)
                throw new IndexBuilderException("Unable to find parent taxon " + this.parentNameUsageID);
        }
        if (this.acceptedNameUsageID != null) {
            this.accepted = taxonomy.getInstance(this.acceptedNameUsageID);
            if (this.accepted == null)
                throw new IndexBuilderException("Unable to find accepted taxon " + this.acceptedNameUsageID);
        }
    }
}
