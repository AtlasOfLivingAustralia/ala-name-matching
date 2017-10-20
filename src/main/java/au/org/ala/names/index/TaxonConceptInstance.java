package au.org.ala.names.index;

import au.org.ala.vocab.ALATerm;
import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonomicType;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;

import javax.annotation.Nullable;
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
public class TaxonConceptInstance extends TaxonomicElement<TaxonConceptInstance, TaxonConcept> {
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
    protected static final List<RankType> CLASSIFICATION_RANKS = Arrays.asList(
            RankType.KINGDOM,
            RankType.PHYLUM,
            RankType.CLASS,
            RankType.ORDER,
            RankType.FAMILY,
            RankType.GENUS,
            RankType.SPECIES,
            RankType.SUBSPECIES
    );

    /** The taxon identifier */
    private String taxonID;
    /** The nomenclatural code */
    private NomenclaturalCode code;
    /** The supplied nomenclatural code */
    private String verbatimNomenclaturalCode;
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
    /** The supplied taxonomic status */
    private String verbatimTaxonomicStatus;
    /** The rank */
    private RankType rank;
    /** The verbatim rank */
    private String verbatimTaxonRank;
    /** The status of the name */
    private Set<NomenclaturalStatus> status;
    /** The supplied nomenclatural status */
    private String verbatimNomenclaturalStatus;
    /** The parent name usage, for accepted names */
    private String parentNameUsage;
    /** The parent name usage identifier, for accepted names */
    private String parentNameUsageID;
    /** The parent taxon */
    private TaxonomicElement parent;
    /** The accepted name usage, for synonyms */
    private String acceptedNameUsage;
    /** The accepted name usage identier, for synonyms */
    private String acceptedNameUsageID;
    /** The accepted taxon */
    private TaxonomicElement accepted;
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
     * @param verbatimNomenclaturalCode The nomenclatural code as supplied
     * @param provider The name provider
     * @param scientificName The scientific name
     * @param scientificNameAuthorship The scientific name authorship
     * @param year The year of publication
     * @param taxonomicStatus The taxonomic status
     * @param verbatimTaxonomicStatus The taxonomic status as supplied
     * @param rank The taxon rank
     * @param verbatimTaxonRank The taxon rank as supplied
     * @param status The nomenclatural status
     * @param verbatimNomenclaturalStatus The nomenclatural status as supplied
     * @param parentNameUsage The parent name, for accepted taxa (parentNameUsageID is preferred if possible)
     * @param parentNameUsageID A link to the parent taxon, for accepted taxa
     * @param acceptedNameUsage The accepted name, for synonyms (acceptedNameUsageID is preferred if possible)
     * @param acceptedNameUsageID A link the the accepted taxon, for synonyms
     * @param classification The taxonomic classification
     */
    public TaxonConceptInstance(
            String taxonID,
            NomenclaturalCode code,
            String verbatimNomenclaturalCode,
            NameProvider provider,
            String scientificName,
            String scientificNameAuthorship,
            String year,
            TaxonomicType taxonomicStatus,
            String verbatimTaxonomicStatus,
            RankType rank,
            String verbatimTaxonRank,
            Set<NomenclaturalStatus> status,
            String verbatimNomenclaturalStatus,
            String parentNameUsage,
            String parentNameUsageID,
            String acceptedNameUsage,
            String acceptedNameUsageID,
            Map<Term, Optional<String>> classification) {
        this.taxonID = taxonID;
        this.code = code;
        this.verbatimNomenclaturalCode = verbatimNomenclaturalCode;
        this.provider = provider;
        this.scientificName = scientificName;
        this.scientificNameAuthorship = scientificNameAuthorship;
        this.year = year;
        this.taxonomicStatus = taxonomicStatus;
        this.verbatimTaxonomicStatus = verbatimTaxonomicStatus;
        this.rank = rank;
        this.verbatimTaxonRank = verbatimTaxonRank;
        this.status = status;
        this.verbatimNomenclaturalStatus = verbatimNomenclaturalStatus;
        this.parentNameUsage = parentNameUsage;
        this.parentNameUsageID = parentNameUsageID;
        this.acceptedNameUsage = acceptedNameUsage;
        this.acceptedNameUsageID = acceptedNameUsageID;
        this.classification = classification;
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
     * A taxon concept instance represents itself.
     *
     * @return This instance
     */
    @Override
    public TaxonConceptInstance getRepresentative() {
        return this;
    }

    /**
     * Get the score of the principal component
     *
     * @return The score
     */
    @Override
    public int getPrincipalScore() {
        return this.getScore();
    }

    /**
     * Get the default score of the provider.
     * <p>
     * Used for tie-breaking when we have multiple candidates for something.
     * </p>
     *
     * @return The provider score
     */
    @Override
    public int getProviderScore() {
        return this.provider.getDefaultScore();
    }

    /**
     * Not supported
     */
    @Override
    public TaxonConceptInstance addInstance(NameKey instanceKey, TaxonConceptInstance instance) {
        throw new UnsupportedOperationException("Unable to add taxon concept instance " + instance + " to taxon concept instance " + this);
    }

    /**
     * Not supported
     */
    @Override
    public void reallocate(TaxonConceptInstance element, Taxonomy taxonomy) {
        throw new UnsupportedOperationException("Unable to reallocate taxon concept instance " + element + " to taxon concept instance " + this);
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
    @Override
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
     * Get the name of the parent for accepted taxa.
     *
     * @return The parent name
     */
    public String getParentNameUsage() {
        return parentNameUsage;
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
    public TaxonomicElement getParent() {
        return parent;
    }


    /**
     * Get the  accepted name for synonyms.
     *
     * @return The accepted name
     */
    public String getAcceptedNameUsage() {
        return acceptedNameUsage;
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
    public TaxonomicElement getAccepted() {
        return accepted;
    }

    /**
     * Get the supplied nomenclaural code.
     *
     * @return The nomenclatural code, as supplied
     */
    public String getVerbatimNomenclaturalCode() {
        return verbatimNomenclaturalCode;
    }

    /**
     * Get the supplied taxonomic status.
     *
     * @return The taxonomic status, as supplied
     */
    public String getVerbatimTaxonomicStatus() {
        return verbatimTaxonomicStatus;
    }

    /**
     * Get the supplied taxon rank.
     *
     * @return The taxon rank, as supplied
     */
    public String getVerbatimTaxonRank() {
        return verbatimTaxonRank;
    }

    /**
     * Get the supplied nomenclaural status.
     *
     * @return The nomenclatural status, as supplied
     */
    public String getVerbatimNomenclaturalStatus() {
        return verbatimNomenclaturalStatus;
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
            this.baseScore = this.provider.computeBaseScore(this, this);
        return this.baseScore;
    }


    /**
     * Get the base score for this instance.
     * <p>
     * The score is lazily computed from the parent score, if one exists, or by a specific
     * </p>
     *
     * @return The base score
     */
    public int getBaseScore(TaxonConceptInstance original) {
        if (original == this)
            throw new IllegalStateException("Loop in score computation from " + original);
        if (this.baseScore == null)
            this.baseScore = this.provider.computeBaseScore(original, this);
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
     * @return The resolved instance
     */
    public TaxonConceptInstance getResolved() {
        if (this.getContainer() == null)
            throw new IllegalStateException("Not taxon concept. Unable to resolve " + this);
        return this.getContainer().getResolved(this);
    }

    /**
     * Get a completely resolved parent.
     * <p>
     * Synonyms and parent taxa are traced through the resolution
     * </p>
     *
     * @return The resolved parent
     */
    public TaxonConceptInstance getResolvedParent() {
        return this.getResolvedParent(this, MAX_RESOLUTION_STEPS, null, true);
    }

    /**
     * Trace the parent taxon.
     *
     * @return A list of taxa in the accepted/parent chain
     */
    private List<TaxonomicElement> traceParent() {
        List<TaxonomicElement> trace = new ArrayList<>();
        trace.add(this);
        this.getResolvedParent(this, MAX_RESOLUTION_STEPS, trace, false);
        return trace;
    }

    /**
     * Trace all parents, making sure that there isn't a loop.
     *
     * @param taxonomy The source taxonomy
     *
     * @return True if the parent chain is valid
     */
    private boolean validateParent(Taxonomy taxonomy) {
        if (this.parent == null)
            return true;
        TaxonomicElement elt = this;
        List<TaxonomicElement> parents = new ArrayList<>(MAX_RESOLUTION_STEPS);
        parents.add(this);
        while (elt != null) {
            if (elt instanceof TaxonConceptInstance) {
                elt = ((TaxonConceptInstance) elt).parent;
                if (elt != null) {
                    if (parents.contains(elt)) {
                        parents.add(elt);
                        taxonomy.report(IssueType.VALIDATION, "instance.validation.parent.loop", parents.toArray(new TaxonConceptInstance[0]));
                        return false;
                    }
                    parents.add(elt);
                }
            } else {
                elt = null;
            }
         }
        return true;
    }

    /**
     * Find the resolved parent, one step at a time
     *
     * @param original The orignal instance
     * @param steps The number of steps allowed in resolution
     * @param trace The trace of steps (null for none)
     * @param exception Throw an exception if a loop is detected, otherwise return null
     *
     * @return The resolved parent or null for none
     */
    private TaxonConceptInstance getResolvedParent(TaxonConceptInstance original, int steps, @Nullable List<TaxonomicElement> trace, boolean exception) {
        if (steps <= 0) {
            if (exception)
                throw new ResolutionException("Detected possible loop resolving parent " + original, trace);
            return null;
        }
        TaxonConceptInstance resolved = this.getResolvedAccepted(original, steps - 1, trace, exception);
        if (resolved == null)
            return null;
        TaxonomicElement pe = resolved.getParent();
        if (pe == null)
            return null;
        TaxonConceptInstance parent = pe.getRepresentative();
        parent = parent.getResolvedAccepted(original, steps - 1, trace, exception);
        if (trace != null && trace.contains(parent)) {
            trace.add(parent);
            if (exception)
                throw new ResolutionException("Detected possible loop resolving parent " + original, trace);
            return null;
        }
        if (trace != null)
            trace.add(parent);
        if (parent == null || !parent.isForbidden())
            return parent;
        return parent.getResolvedParent(original, steps - 1, trace, exception);
    }

    /**
     * Get the completely resolved, accepted taxon concept instance for this instance.
     *
     * @return The final accepted instance
     */
    public TaxonConceptInstance getResolvedAccepted() {
        return this.getResolvedAccepted(this, MAX_RESOLUTION_STEPS, null, true);
    }

    /**
     * Trace the accepted taxon.
     *
     * @return A list of taxa in the accepted chain
     */
    private List<TaxonomicElement> traceAccepted() {
        List<TaxonomicElement> trace = new ArrayList<>();
        trace.add(this);
        this.getResolvedAccepted(this, MAX_RESOLUTION_STEPS, trace, false);
        return trace;
    }

    /**
     * Trace the accepted taxon, one step at a time
     *
     * @param original The intiial instance
     * @param steps The number of resolution steps allowed
     * @param trace An accuulating trace of steps (may be null)
     * @param exception Throw an exceptioon if a loop is detected, otherwise the original is returned
     *
     * @return The accepted taxon concept instance
     */
    private TaxonConceptInstance getResolvedAccepted(TaxonConceptInstance original, int steps, @Nullable List<TaxonomicElement> trace, boolean exception) {
        if (steps <= 0) {
            if (exception)
                throw new ResolutionException("Detected possible loop resolving accepted " + original, trace);
            return original;
        }
        TaxonConceptInstance resolved = this.getResolved(original, steps - 1);
        TaxonomicElement ae = resolved.getAccepted();
        if (ae == null || ae == resolved)
            return resolved;
        if (trace != null && trace.contains(ae)) {
            trace.add(ae);
            if (exception)
                throw new ResolutionException("Detected possible loop resolving accepted " + original, trace);
            return original;
        }
        if (trace != null)
            trace.add(ae);
        TaxonConceptInstance accepted = ae.getRepresentative();
        accepted = accepted.getResolvedAccepted(original, steps - 1, trace, exception);
        if (!accepted.isForbidden())
            return accepted;
        return accepted.getResolvedParent(original, steps - 1, trace, exception);
    }

    private TaxonConceptInstance getResolved(TaxonConceptInstance original, int steps) {
        if (steps <= 0)
            throw new ResolutionException("Detected possible loop resolving " + original);
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
        }
        if (this.parentNameUsage != null && this.parent == null) {
            this.parent = taxonomy.findElement(this.code, this.parentNameUsage, this.provider, null);
        }
        if (this.parent == null && this.taxonomicStatus.isAccepted() && this.classification != null) {
            for (int i = 0; i < CLASSIFICATION_FIELDS.size(); i++) {
                Term cls = CLASSIFICATION_FIELDS.get(i);
                RankType clr = CLASSIFICATION_RANKS.get(i);
                Optional<String> name = this.classification.get(cls);
                if (name != null && name.isPresent() && !name.get().equals(this.scientificName)) {
                    TaxonomicElement p = taxonomy.findElement(this.code, name.get(), this.provider, clr);
                    RankType pr = p != null ? p.getRank() : null;
                    if (p != null && p != this && (pr == null || pr.isHigherThan(this.rank)))
                        this.parent = p;
                }
            }
        }
        if (this.parent == null && (this.parentNameUsage != null || this.parentNameUsageID != null))
            throw new IndexBuilderException("Unable to find parent taxon for " + this + " from " + this.parentNameUsageID + " - " + this.parentNameUsage);
        if (this.acceptedNameUsageID != null) {
            this.accepted = taxonomy.getInstance(this.acceptedNameUsageID);
        }
        if (this.acceptedNameUsage != null && this.accepted == null) {
            this.accepted = taxonomy.findElement(this.code, this.acceptedNameUsage, this.provider, null);
        }
        if (this.accepted == null && (this.acceptedNameUsage != null || this.acceptedNameUsageID != null))
            throw new IndexBuilderException("Unable to find accepted taxon for " + this + " from " + this.acceptedNameUsageID + " - " + this.acceptedNameUsage);
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
     * Is this an instance of an inferred synonym?
     *
     * @return True if this is a created inferred synonym
     */
    public boolean isInferredSynonym() {
        return this.taxonomicStatus == TaxonomicType.INFERRED_SYNONYM;
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
        final Map<Term, String> values;
        if (valuesList.isEmpty()) {
            if (this.provider != taxonomy.getInferenceProvider())
                taxonomy.report(IssueType.NOTE,"instance.noIndex", this);
            values = new HashMap<>();
        } else {
            if (valuesList.size() > 1)
                taxonomy.report(IssueType.ERROR,"instance.multiIndex", this);
            values = valuesList.get(0);
        }
        values.put(DwcTerm.taxonID, this.taxonID);
        values.put(DwcTerm.nomenclaturalCode, this.code == null ? null : this.code.getAcronym());
        values.put(ALATerm.verbatimNomenclaturalCode, this.verbatimNomenclaturalCode);
        values.put(DwcTerm.datasetID, this.provider.getId());
        values.put(DwcTerm.scientificName, this.scientificName);
        values.put(DwcTerm.scientificNameAuthorship, this.scientificNameAuthorship);
        values.put(DwcTerm.namePublishedInYear, this.year);
        values.put(DwcTerm.taxonomicStatus, this.taxonomicStatus.getTerm());
        values.put(ALATerm.verbatimTaxonomicStatus, this.verbatimTaxonomicStatus);
        values.put(DwcTerm.nomenclaturalStatus, this.status == null ? null : this.status.stream().map(NomenclaturalStatus::getAbbreviatedLabel).collect(Collectors.joining(", ")));
        values.put(ALATerm.verbatimNomenclaturalStatus, this.verbatimNomenclaturalStatus);
        values.put(DwcTerm.taxonRank, this.rank.getRank());
        values.put(DwcTerm.verbatimTaxonRank, this.verbatimTaxonRank);
        values.put(ALATerm.priority, Integer.toString(this.getScore()));
        if (this.parentNameUsageID != null) {
            String pid = null;
            try {
                TaxonConceptInstance rp = this.getResolvedParent();
                pid = rp == null ? null : rp.getTaxonID();
                if (pid == null) {
                    taxonomy.report(IssueType.ERROR, "instance.parent.resolve", this);
                }
            } catch (ResolutionException ex) {
                taxonomy.report(IssueType.ERROR, "instance.parent.resolve.loop", this.traceParent().toArray(new TaxonConceptInstance[0]));
            }
            values.put(DwcTerm.parentNameUsageID, pid);
        }
        if (this.acceptedNameUsageID != null) {
            String aid = null;
            try {
                TaxonConceptInstance ra = this.getResolvedAccepted();
                aid = ra == null ? null : ra.getTaxonID();
                if (aid == null) {
                    taxonomy.report(IssueType.ERROR, "instance.accepted.resolve", this);
                }
             } catch (ResolutionException ex) {
                taxonomy.report(IssueType.ERROR, "instance.accepted.resolve.loop", this.traceAccepted().toArray(new TaxonConceptInstance[0]));
            }
            values.put(DwcTerm.acceptedNameUsageID, aid);
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
     * @param concept The parent taxon concept
     * @param scientificName The source name of the synonym
     * @param scientificNameAuthorship The authorship of the synonym
     * @param year The year of authorship
     * @param taxonomy The base taxonomy
     *
     * @return A synonym that points a name towards this instance
     */
    public TaxonConceptInstance createInferredSynonym(TaxonConcept concept, String scientificName, String scientificNameAuthorship, String year, Taxonomy taxonomy) {
        TaxonConceptInstance synonym = new TaxonConceptInstance(
                UUID.randomUUID().toString(),
                this.code,
                this.verbatimNomenclaturalCode,
                taxonomy.getInferenceProvider(),
                scientificName,
                scientificNameAuthorship,
                year,
                TaxonomicType.INFERRED_SYNONYM,
                this.verbatimTaxonomicStatus,
                this.rank,
                this.verbatimTaxonRank,
                this.status,
                this.verbatimNomenclaturalStatus,
                null,
                null,
                null,
                this.getTaxonID(),
                this.classification
        );
        synonym.setContainer(concept);
        synonym.accepted = this;
        synonym.baseScore = null;
        synonym.score = null;
        synonym.forbidden = false;
        taxonomy.addInferredInstance(synonym);
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
        if ((this.parentNameUsageID != null || this.parentNameUsage != null || this.isAccepted() && !(this.classification == null || this.classification.isEmpty())) && this.parent == null) {
            if (this.provider.isLoose())
                taxonomy.report(IssueType.NOTE, "instance.validation.noParent.loose", this);
            else {
                taxonomy.report(IssueType.VALIDATION, "instance.validation.noParent", this);
                valid = false;
            }

        }
        if ((this.acceptedNameUsageID != null || this.acceptedNameUsage != null) && this.accepted == null) {
            if (this.provider.isLoose())
                taxonomy.report(IssueType.NOTE, "instance.validation.noAccepted.loose", this);
            else {
                taxonomy.report(IssueType.VALIDATION, "instance.validation.noAccepted", this);
                valid = false;
            }

        }
        if (this.getContainer() == null) {
            taxonomy.report(IssueType.VALIDATION, "instance.validation.noTaxonConcept", this);
            valid = false;

        } else  if (this.getContainer().getContainer() == null) {
            taxonomy.report(IssueType.VALIDATION, "instance.validation.noScientificName", this);
            valid = false;
        }
        valid = valid && this.validateParent(taxonomy);
        return valid;
    }

}
