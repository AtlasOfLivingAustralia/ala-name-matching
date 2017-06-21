package au.org.ala.names.index;

import au.ala.org.vocab.ALATerm;
import au.org.ala.names.model.RankType;
import au.org.ala.names.model.SynonymType;
import au.org.ala.names.model.TaxonomicType;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An instance of a taxonomic concept from a particular source.
 * <p>
 * This contains enough information to allow us to resolve
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class TaxonConceptInstance extends TaxonomicElement {
    /** The maximum number of iterations to attempt during resolution before suspecting somethiing is wrong */
    public static final int MAX_RESOLUTION_STEPS = 20;

    /** Classification fields from name sources */
    protected static final List<Term> CLASSIFICATION_FIELDS = Arrays.asList(
            DwcTerm.kingdom,
            DwcTerm.phylum,
            DwcTerm.class_,
            DwcTerm.order,
            DwcTerm.family,
            DwcTerm.genus,
            DwcTerm.specificEpithet,
            DwcTerm.infraspecificEpithet
    );

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
    private TaxonomicType taxonomicStatus;
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
    /** Additional classification information */
    private Map<Term, Optional<String>> classification;
    /** The base score for position on the taxonomic tree */
    private Integer baseScore;
    /** The specific instance score */
    private Integer score;
    /** Is this concept forbidden? */
    private boolean forbidden;

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
     * @param rank The taxon rank
     * @param status The nomenclatural status
     * @param parentNameUsageID A link to the parent taxon, for accepted taxa
     * @param acceptedNameUsageID A link the the accepted taxon, for synonyms
     * @param classification The taxonomic classification
     */
    public TaxonConceptInstance(String taxonID, NomenclaturalCode code, NameProvider provider, String scientificName, String scientificNameAuthorship, String year, TaxonomicType taxonomicStatus, RankType rank, Set<NomenclaturalStatus> status, String parentNameUsageID, String acceptedNameUsageID, Map<Term, Optional<String>> classification) {
        this.taxonID = taxonID;
        this.code = code;
        this.provider = provider;
        this.scientificName = scientificName;
        this.scientificNameAuthorship = scientificNameAuthorship;
        this.year = year;
        this.taxonomicStatus = taxonomicStatus;
        this.rank = rank;
        this.status = status;
        this.parentNameUsageID = parentNameUsageID;
        this.acceptedNameUsageID = acceptedNameUsageID;
        this.classification = classification;
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
     * Get the taxon identifier, as specified by the source.
     *
     * @return The taxon identifier
     */
    @Override
    public String getId() {
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
    public TaxonomicType getTaxonomicStatus() {
        return taxonomicStatus;
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
     * Get the base score for this instance.
     * <p>
     * The score is lazily computed from the parent score, if one exists, or by a specific
     * </p>
     *
     * @return The base score
     */
    public int getBaseScore() {
        if (this.baseScore == null)
            this.baseScore = this.provider.computeBaseScore(this);
        return this.baseScore;
    }

    /**
     * Get the score for this instance.
     * <p>
     * The score is lazily computed from the base score and any modifications
     * based on taxonomic status, etc.
     * </p>
     *
     * @return The instance score
     */
    public int getScore() {
        if (this.score == null) {
            this.score = this.provider.computeScore(this);
        }
        return this.score;
    }

    /**
     * Is this present because it has to be but is otherwise
     * forbidden.
     *
     * @return True if this is a forbidden instance
     */
    public boolean isForbidden() {
        return forbidden;
    }

    /**
     * Set the forbidden flag
     *
     * @param forbidden
     */
    public void setForbidden(boolean forbidden) {
        this.forbidden = forbidden;
    }

    /**
     * Get the classification of the taxon in terms of higher-level taxa.
     *
     * @return
     */
    public Map<Term, Optional<String>> getClassification() {
        return classification;
    }

    /**
     * Get the resolved instance for this instance, based on the parent taxon concept.
     * <p>
     * This returns one step of resolution.
     * Use {@link #getResolvedParent()} or {@link #getResolvedAccepted()} for fully resolved result
     * </p>
     *
     * @return The resolved instance, or null for none.
     */
    public TaxonConceptInstance getResolved() {
        return this.taxonConcept == null ? null : this.taxonConcept.getResolved(this);
    }

    public TaxonConceptInstance getResolvedParent() {
        TaxonConceptInstance resolved = this.getResolved(this, MAX_RESOLUTION_STEPS);
        if (resolved == null)
            return null;
        TaxonConceptInstance parent = this.getParent();
        if (parent == null)
            return null;
        parent = parent.getResolved(this, MAX_RESOLUTION_STEPS);
        if (parent == null || !parent.isForbidden())
            return parent;
        return parent.getResolvedParent();
    }

    public TaxonConceptInstance getResolvedAccepted() {
        TaxonConceptInstance resolved = this.getResolved(this, MAX_RESOLUTION_STEPS);
        if (resolved == null)
            return null;
        TaxonConceptInstance accepted = this.getAccepted();
        if (accepted == null)
            return null;
        accepted = accepted.getResolved(this, MAX_RESOLUTION_STEPS);
        if (accepted == null || !accepted.isForbidden())
            return accepted;
        return accepted.getResolvedAccepted();
    }

    private TaxonConceptInstance getResolved(TaxonConceptInstance original, int steps) {
        if (steps <= 0)
            throw new IllegalArgumentException("Detected possible loop resovling " + original);
        TaxonConceptInstance resolved = this.getResolved();
        if (resolved != null && this != resolved)
            return resolved.getResolved(original, steps - 1);
        return resolved;
    }

    /**
     * Has this instance been resolved?
     *
     * @return True if there is a resolved instance attached to the parent concept.
     */
    public boolean isResolved() {
        return this.getResolved() != null;
    }

    /**
     * Is this a primary instance?
     *
     * @return True if the tanomomic status is primary
     */
    public boolean isPrimary() {
        return this.taxonomicStatus != null && this.taxonomicStatus.isPrimary();
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
        if (this.parent == null && this.taxonomicStatus.isAccepted() && this.classification != null) {
            for (Term cls: CLASSIFICATION_FIELDS) {
                Optional<String> name = this.classification.get(cls);
                if (name != null && name.isPresent() && !name.get().equals(this.scientificName)) {
                    TaxonConceptInstance p = taxonomy.findInstance(this.code, name.get(), this.provider);
                    if (p != null)
                        this.parent = p;
                }
            }
        }
        taxonomy.count("count.resolve.instance.links");
    }

    /**
     * Is this an instance of an accepted taxon?
     *
     * @return True if this is supplied as an accepted taxonomic status
     */
    public boolean isAccepted() {
        return this.taxonomicStatus.isAccepted();
    }

    /**
     * Is this an instance of an synonym taxon?
     *
     * @return True if this is supplied as an synonym taxonomic status
     */
    public boolean isSynonym() {
        return this.taxonomicStatus.isSynonym();
    }

    /**
     * Get a map of taxon information for this particular taxon instance.
     *
     * @param taxonomy The taxonomy to collect additional information from
     *
     * @return The taxon map
     *
     * @throws IOException if unable to retrive source documents
     */
    public Map<Term,String> getTaxonMap(Taxonomy taxonomy) throws IOException {
        List<Map<Term, String>> valuesList = taxonomy.getIndexValues(DwcTerm.Taxon, this.taxonID);
        Map<Term, String> values;
        if (valuesList.isEmpty()) {
            if (this.provider != taxonomy.getInferenceProvider())
                taxonomy.report(IssueType.NOTE,"instance.noIndex", this);
            values = new HashMap<Term, String>();
        } else {
            if (valuesList.size() > 1)
                taxonomy.report(IssueType.ERROR,"instance.multiIndex", this);
            values = valuesList.get(0);
        }
        values.put(DwcTerm.taxonID, this.taxonID);
        values.put(DwcTerm.nomenclaturalCode, this.code.getAcronym());
        values.put(DwcTerm.datasetID, this.provider.getId());
        values.put(DwcTerm.scientificName, this.scientificName);
        values.put(DwcTerm.scientificNameAuthorship, this.scientificNameAuthorship);
        values.put(DwcTerm.namePublishedInYear, this.year);
        values.put(DwcTerm.taxonomicStatus, this.taxonomicStatus.getTerm());
        values.put(DwcTerm.nomenclaturalStatus, this.status == null ? null : this.status.stream().map(NomenclaturalStatus::getAbbreviatedLabel).collect(Collectors.joining(", ")));
        values.put(DwcTerm.taxonRank, this.rank.getRank());
        if (this.parentNameUsageID != null) {
            TaxonConceptInstance rp = this.getResolvedParent();
            if (rp == null) {
                taxonomy.report(IssueType.ERROR, "instance.parent.resolve", this);
            } else {
                values.put(DwcTerm.parentNameUsageID, rp.taxonID);
            }
        }
        if (this.acceptedNameUsageID != null) {
            TaxonConceptInstance ra = this.getResolvedAccepted();
            if (ra == null) {
                taxonomy.report(IssueType.ERROR, "instance.accepted.resolve", this);
            } else {
                values.put(DwcTerm.acceptedNameUsageID, ra.taxonID);
            }
        }
        return values;
    }

    /**
     * Get a list of identifiers associated with this taxon instance.
     * <p>
     * The taxon, scientific name and taxon concept identifiers are added if they are not specifically listed.
     * </p>
     *
     * @param taxonomy The taxonomy to collect additional information from
     *
     * @return The identifier list
     *
     * @throws IOException if unable to retrive source documents
     */
    public List<Map<Term,String>> getIdentifierMaps(Taxonomy taxonomy) throws IOException {
        final Map<Term, String> taxon = this.getTaxonMap(taxonomy);
        final String scientificNameID = taxon.get(DwcTerm.scientificNameID);
        final String taxonConceptID = taxon.get(DwcTerm.taxonConceptID);
        final String source = taxon.get(DcTerm.source);
        List<Map<Term, String>> valuesList = taxonomy.getIndexValues(GbifTerm.Identifier, this.taxonID);
        Set<String> identifiers = valuesList.stream().map(values -> values.get(DcTerm.identifier)).collect(Collectors.toSet());
        if (!identifiers.contains(this.taxonID)) {
            Map<Term, String> values = new HashMap<>();
            values.put(DcTerm.identifier, this.taxonID);
            values.put(DwcTerm.datasetID, this.provider.getId());
            values.put(DcTerm.title, "Taxon");
            values.put(ALATerm.status, "current");
            if (source != null)
                values.put(DcTerm.source, source);
            valuesList.add(values);
        }
        if (scientificNameID != null && !identifiers.contains(scientificNameID)) {
            Map<Term, String> values = new HashMap<>();
            values.put(DcTerm.identifier, scientificNameID);
            values.put(DwcTerm.datasetID, this.provider.getId());
            values.put(DcTerm.title, "Scientific Name");
            values.put(ALATerm.status, "current");
            if (source != null)
                values.put(DcTerm.source, source);
            valuesList.add(values);
        }
        if (taxonConceptID != null && !identifiers.contains(taxonConceptID)) {
            Map<Term, String> values = new HashMap<>();
            values.put(DcTerm.identifier, taxonConceptID);
            values.put(DwcTerm.datasetID, this.provider.getId());
            values.put(DcTerm.title, "Taxon Concept");
            values.put(ALATerm.status, "current");
            if (source != null)
                values.put(DcTerm.source, source);
            valuesList.add(values);
        }
        return valuesList;
    }

    /**
     * Get a list of vernacular names associated with this taxon instance.
     *
     * @param taxonomy The taxonomy to collect additional information from
     *
     * @return The vernacular name list
     *
     * @throws IOException if unable to retrive source documents
     */
    public List<Map<Term,String>> getVernacularMaps(Taxonomy taxonomy) throws IOException {
        List<Map<Term, String>> valuesList = taxonomy.getIndexValues(GbifTerm.VernacularName, this.taxonID);
        return valuesList;
    }

    /**
     * Get a list of distribution maps associated with this taxon instance.
     *
     * @param taxonomy The taxonomy to collect additional information from
     *
     * @return The distribution list
     *
     * @throws IOException if unable to retrive source documents
     */
    public List<Map<Term,String>> getDistributionMaps(Taxonomy taxonomy) throws IOException {
        List<Map<Term, String>> valuesList = taxonomy.getIndexValues(GbifTerm.Distribution, this.taxonID);
        return valuesList;
    }

    /**
     * A human readbale label for the concept
     *
     * @return The label
     */
    @Override
    public String toString() {
        return "TCI[" + this.taxonID + ", " + this.scientificName + ", " + this.scientificNameAuthorship + "]";
    }

    /**
     * Is this an owned taxon concept.
     * <p>
     * A taxon concept is owned if the data provider asserts ownership over the name.
     * </p>
     *
     * @return True if this is owned by the data provider
     */
    public boolean isOwned() {
        return this.provider.owns(this.scientificName);
    }

    /**
     * Create an inferred synonym to this taxon.
     * <p>
     * This points another taxon towards this taxon.
     * </p>
     * @param scientificName The source name of the synonym
     * @param scientificNameAuthorship The authorship of the synonym
     * @param year The year of authorship
     * @param taxonomy The base taxonomy
     *
     * @return A synonym that points a name towards this instance
     */
    public TaxonConceptInstance createInferredSynonym(String scientificName, String scientificNameAuthorship, String year, Taxonomy taxonomy) {
        TaxonConceptInstance synonym = new TaxonConceptInstance(
                UUID.randomUUID().toString(),
                this.code,
                taxonomy.getInferenceProvider(),
                scientificName,
                scientificNameAuthorship,
                year,
                TaxonomicType.INFERRED_SYNONYM,
                this.rank,
                this.status,
                null,
                this.getTaxonID(),
                this.classification
        );
        synonym.accepted = this;
        synonym.baseScore = null;
        synonym.score = null;
        synonym.forbidden = false;
        return synonym;
    }

    /**
     * Validate this taxon concept instance.
     *
     * @param taxonomy The taxonomy to validate against and report to
     *
     * @return True if the scientific name is valid
     */
    @Override
    public boolean validate(Taxonomy taxonomy) {
        boolean valid = true;
        if ((this.parentNameUsageID != null || this.isAccepted() && !this.classification.isEmpty()) && this.parent == null) {
            taxonomy.report(IssueType.VALIDATION, "instance.validation.noParent", this);
            valid = false;

        }
        if (this.acceptedNameUsageID != null && this.accepted == null) {
            taxonomy.report(IssueType.VALIDATION, "instance.validation.noAccepted", this);
            valid = false;

        }
        if (this.taxonConcept == null) {
            taxonomy.report(IssueType.VALIDATION, "instance.validation.noTaxonConcept", this);
            valid = false;

        } else  if (this.taxonConcept.getName() == null) {
            taxonomy.report(IssueType.VALIDATION, "instance.validation.noScientificName", this);
            valid = false;
        }
        return valid;
    }

}
