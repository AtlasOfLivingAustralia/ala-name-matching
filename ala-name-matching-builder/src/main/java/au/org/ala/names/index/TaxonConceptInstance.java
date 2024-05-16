/*
 * Copyright (c) 2021 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 */

package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonFlag;
import au.org.ala.names.model.TaxonomicType;
import au.org.ala.vocab.ALATerm;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.MessageFormat;
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
    private static final Logger logger = LoggerFactory.getLogger(TaxonConceptInstance.class);

    /**
     * Compare instance base (priovider only) scores
     */
    public static Comparator<TaxonConceptInstance> PROVIDER_SCORE_COMPARATOR = new Comparator<TaxonConceptInstance>() {
        @Override
        public int compare(TaxonConceptInstance e1, TaxonConceptInstance e2) {
            if (e1 == null && e2 == null)
                return 0;
            if (e1 == null && e2 != null)
                return TaxonomicElement.MIN_SCORE;
            if (e1 != null && e2 == null)
                return TaxonomicElement.MAX_SCORE;
            int o1 = e1.getProviderScore();
            int o2 = e2.getProviderScore();
            try {
                return Math.subtractExact(o1, o2);
            } catch (Exception ex) {
                if (o1 > o2)
                    return TaxonomicElement.MAX_SCORE;
                if (o2 > o1)
                    return TaxonomicElement.MIN_SCORE;
                return 0; // Shouldn't need to return this
            }
        }
    };
    /**
     * Compare instance scores
     */
    public static Comparator<TaxonConceptInstance> SCORE_COMPARATOR = new Comparator<TaxonConceptInstance>() {
        @Override
        public int compare(TaxonConceptInstance e1, TaxonConceptInstance e2) {
            if (e1 == null && e2 == null)
                return 0;
            if (e1 == null && e2 != null)
                return TaxonomicElement.MIN_SCORE;
            if (e1 != null && e2 == null)
                return TaxonomicElement.MAX_SCORE;
            int o1 = e1.getScore();
            int o2 = e2.getScore();
            try {
                return Math.subtractExact(o1, o2);
            } catch (Exception ex) {
                if (o1 > o2)
                    return TaxonomicElement.MAX_SCORE;
                if (o2 > o1)
                    return TaxonomicElement.MIN_SCORE;
                return 0; // Shouldn't need to return this
            }
        }
    };
    /**
     * Inverse instance scores (for most important first)
     */
    public static Comparator<TaxonConceptInstance> INVERSE_SCORE_COMPARATOR = SCORE_COMPARATOR.reversed();

    /**
     * The maximum number of iterations to attempt during resolution before suspecting somethiing is wrong
     */
    public static final int MAX_RESOLUTION_STEPS = 40;

    /**
     * Classification fields from name sources
     */
    protected static final List<Term> CLASSIFICATION_FIELDS = Arrays.asList(
            DwcTerm.kingdom,
            DwcTerm.phylum,
            DwcTerm.class_,
            DwcTerm.order,
            DwcTerm.family,
            DwcTerm.genus,
            DwcTerm.specificEpithet
            // DwcTerm.infraspecificEpithet
    );
    /**
     * The ranks corresponding to {@link #CLASSIFICATION_FIELDS}. The two lists must correspond.
     */
    protected static final List<RankType> CLASSIFICATION_RANKS = Arrays.asList(
            RankType.KINGDOM,
            RankType.PHYLUM,
            RankType.CLASS,
            RankType.ORDER,
            RankType.FAMILY,
            RankType.GENUS,
            RankType.SPECIES
            // RankType.SUBSPECIES
    );

    /**
     * The taxon identifier
     */
    private String taxonID;
    /**
     * The nomenclatural code
     */
    private NomenclaturalClassifier code;
    /**
     * The supplied nomenclatural code
     */
    private String verbatimNomenclaturalCode;
    /**
     * The name source
     */
    private NameProvider provider;
    /**
     * The supplied name
     */
    private String scientificName;
    /**
     * The supplied scientific name authorship
     */
    private String scientificNameAuthorship;
    /**
     * The properly formatted complete name
     */
    @Nullable
    private String nameComplete;
    /**
     * The year of publication, if available
     */
    private String year;
    /**
     * The taxonomic status
     */
    private TaxonomicType taxonomicStatus;
    /**
     * The supplied taxonomic status
     */
    private String verbatimTaxonomicStatus;
    /**
     * The rank
     */
    private RankType rank;
    /**
     * The verbatim rank
     */
    private String verbatimTaxonRank;
    /**
     * The status of the name
     */
    private Set<NomenclaturalStatus> status;
    /**
     * The supplied nomenclatural status
     */
    private String verbatimNomenclaturalStatus;
    /**
     * The parent name usage, for accepted names
     */
    private String parentNameUsage;
    /**
     * The parent name usage identifier, for accepted names
     */
    private String parentNameUsageID;
    /**
     * The parent taxon
     */
    private TaxonomicElement parent;
    /**
     * The accepted name usage, for synonyms
     */
    private String acceptedNameUsage;
    /**
     * The accepted name usage identier, for synonyms
     */
    private String acceptedNameUsageID;
    /**
     * Any taxon remarks. This may be added to as processing occurs
     */
    private List<String> taxonRemarks;
    /**
     * The original taxon remarks
     */
    private String verbatimTaxonRemarks;
    /**
     * Any provenance information
     */
    private List<String> provenance;
    /**
     * The accepted taxon
     */
    private TaxonomicElement accepted;
    /**
     * Additional classification information
     */
    private Map<Term, Optional<String>> classification;
    /**
     * Any special flags
     */
    private Set<TaxonFlag> flags;
    /**
     * The taxon distribution
     */
    private List<Distribution> distribution;
    /**
     * The taxon vernacular names
     */
    private List<VernacularName> vernacularNames;
    /**
     * The base score for position on the taxonomic tree
     */
    private Integer baseScore;
    /**
     * The specific instance score
     */
    private Integer score;
    /**
     * Is this concept forbidden?
     */
    private boolean forbidden;

    /**
     * Construct a new taxon concept instance
     *
     * @param taxonID                     The unique taxon identifier
     * @param code                        The nomenclatural code for the taxon
     * @param verbatimNomenclaturalCode   The nomenclatural code as supplied
     * @param provider                    The name provider
     * @param scientificName              The scientific name
     * @param scientificNameAuthorship    The scientific name authorship
     * @param nameComplete                The properly formatted complete name
     * @param year                        The year of publication
     * @param taxonomicStatus             The taxonomic status
     * @param verbatimTaxonomicStatus     The taxonomic status as supplied
     * @param rank                        The taxon rank
     * @param verbatimTaxonRank           The taxon rank as supplied
     * @param status                      The nomenclatural status
     * @param verbatimNomenclaturalStatus The nomenclatural status as supplied
     * @param parentNameUsage             The parent name, for accepted taxa (parentNameUsageID is preferred if possible)
     * @param parentNameUsageID           A link to the parent taxon, for accepted taxa
     * @param acceptedNameUsage           The accepted name, for synonyms (acceptedNameUsageID is preferred if possible)
     * @param acceptedNameUsageID         A link the the accepted taxon, for synonyms
     * @param taxonRemarks                Any taxon remakrs
     * @param verbatimTaxonRemarks        The original taxon remarks
     * @param provenance                  Provenance information
     * @param classification              The taxonomic classification
     * @param flags                       The taxonomic flags
     * @param distribution                The taxon distribution
     * @paarm vernacularNames             The taxon vernacular names
     */
    public TaxonConceptInstance(
            String taxonID,
            NomenclaturalClassifier code,
            String verbatimNomenclaturalCode,
            NameProvider provider,
            String scientificName,
            String scientificNameAuthorship,
            @Nullable String nameComplete,
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
            @Nullable List<String> taxonRemarks,
            @Nullable String verbatimTaxonRemarks,
            @Nullable List<String> provenance,
            @Nullable Map<Term, Optional<String>> classification,
            @Nullable Set<TaxonFlag> flags,
            @Nullable List<Distribution> distribution,
            @Nullable List<VernacularName> vernacularNames
    ) {
        this.taxonID = taxonID;
        this.code = code;
        this.verbatimNomenclaturalCode = verbatimNomenclaturalCode;
        this.provider = Objects.requireNonNull(provider);
        this.scientificName = scientificName;
        this.scientificNameAuthorship = scientificNameAuthorship;
        this.nameComplete = nameComplete;
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
        this.taxonRemarks = taxonRemarks == null ? null : new ArrayList<>(taxonRemarks);
        this.verbatimTaxonRemarks = verbatimTaxonRemarks;
        this.provenance = provenance == null ? null : new ArrayList<>(provenance);
        this.classification = classification;
        this.flags = flags;
        this.distribution = distribution;
        this.vernacularNames = vernacularNames;
    }

    /**
     * Get the nomenclatural code for this taxon
     *
     * @return The nomenclatural code
     */
    public NomenclaturalClassifier getCode() {
        return code;
    }

    /**
     * Get the taxon identifier, as specified by the source.
     *
     * @return The taxon identifier
     */
    @Override
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
     * Get the originating authority for the data
     *
     * @return The authority source
     */
    public NameProvider getAuthority() {
        return provider == null ? null : provider.getAuthority();
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
     * Get the correctly formatted complete name, if it exists
     *
     * @return The complete name
     */
    @Nullable
    public String getNameComplete() {
        return nameComplete;
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
    public void reallocate(TaxonConceptInstance element, Taxonomy taxonomy, String reason) {
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
    public String getVerbatimNomenclaturalClassifier() {
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
     * Get the list of taxon remarks.
     * <p>
     * A list to make it easy to add additional remarks.
     * </p>
     *
     * @return The taxon remarks.
     */
    public List<String> getTaxonRemarks() {
        return this.taxonRemarks;
    }

    /**
     * Get a string form of the taxon remarks.
     * <p>
     * Currently, all taxon remarks are separated by a vertical bar, to avoid CSV problems with newlines.
     * </p>
     *
     * @return The taxon remark string
     */
    public String getTaxonRemarkString() {
        return this.taxonRemarks == null || this.taxonRemarks.isEmpty() ? null : this.taxonRemarks.stream().reduce(null, (a, b) -> a == null ? b : a + " | " + b);
    }

    /**
     * Add a taxon remark to the list of taxon remarks.
     *
     * @param remark The remark
     */
    public void addTaxonRemark(String remark) {
        if (this.taxonRemarks == null)
            this.taxonRemarks = new ArrayList<>();
        this.taxonRemarks.add(remark);
    }

    /**
     * Get the original taxon remarks.
     *
     * @return The verbatim taxon remarks
     */
    public String getVerbatimTaxonRemarks() {
        return this.verbatimTaxonRemarks;
    }

    /**
     * Get the list of provenance statements.
     * <p>
     * A list to make it easy to add additional provenance.
     * </p>
     *
     * @return The taxon remarks.
     */
    public List<String> getProvenance() {
        return this.provenance;
    }

    /**
     * Get a string form of the provenance.
     * <p>
     * Currently, all provenance statements are separated by a vertical bar, to avoid CSV problems with newlines.
     * </p>
     *
     * @return The taxon remark string
     */
    public String getProvenanceString() {
        return this.provenance == null || this.provenance.isEmpty() ? null : this.provenance.stream().reduce(null, (a, b) -> a == null ? b : a + " | " + b);
    }

    /**
     * Add a provenance statement to the provenance list.
     *
     * @param statement The statement
     */
    public void addProvenance(String statement) {
        if (this.provenance == null)
            this.provenance = new ArrayList<>();
        this.provenance.add(statement);
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
        if (this.baseScore == null) {
            synchronized (this) {
                if (this.baseScore == null) {
                    this.baseScore = this.provider.computeBaseScore(this, this);
                }
            }
        }
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
            throw new IllegalStateException("Uncaught loop in score computation from " + original);
        if (this.baseScore == null) {
            synchronized (this) {
                if (this.baseScore == null) {
                    this.baseScore = this.provider.computeBaseScore(original, this);
                }
            }
        }
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
            synchronized (this) {
                if (this.score == null) {
                    this.score = this.provider.computeScore(this);
                }
            }
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
     * Get the flags associated with the taxon
     *
     * @return The taxon flags (null for none)
     */
    public Set<TaxonFlag> getFlags() {
        return this.flags;
    }

    /**
     * Test to see if a flag is set
     *
     * @param flag The flag to find
     * @return True if the flag is present
     */
    public boolean hasFlag(TaxonFlag flag) {
        return this.flags != null && this.flags.contains(flag);
    }

    /**
     * Get the distribution set
     *
     * @return The distribution
     */
    public List<Distribution> getDistribution() {
        return distribution;
    }

    /**
     * Get the list of assigned vernacular names
     *
     * @return The list of vernacular names
     */
    public List<VernacularName> getVernacularNames() {
        return vernacularNames;
    }

    /**
     * Assign a vernacular name to this instance.
     *
     * @param name The name
     */
    public void assignVernacularName(VernacularName name) {
        if (this.vernacularNames == null)
            this.vernacularNames = new ArrayList<>();
        this.vernacularNames.add(name);
        name.setInstance(this);
    }

    /**
     * Set the forbidden flag
     * <p>
     * Note that, if you set something as forbidden, increase <code>count.load.forbidden</code>
     * so people can add up the numbers
     * </p>
     *
     * @param forbidden
     */
    public void setForbidden(boolean forbidden) {
        this.forbidden = forbidden;
    }

    /**
     * Should we output this instance?
     *
     * @return True if not forbidden and of outputtable taxonomic status
     */
    public boolean isOutput() {
        return !this.isForbidden() && this.getTaxonomicStatus().isOutput();
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
     * Add a classification hint.
     * <p>
     * If the hint is null or there is already a classification value, don't bother.
     * Otherwise, add the hint to the classification.
     * </p>
     *
     * @param term  The classifcation term
     * @param value The hint value
     */
    public void addClassificationHint(Term term, @Nullable String value) {
        if (value == null || (this.classification != null && this.classification.containsKey(term) && this.classification.get(term).isPresent()))
            return;
        if (this.classification == null)
            this.classification = new HashMap<>();
        this.classification.put(term, Optional.of(value));
    }

    /**
     * A provider/id pair to help locate this taxon.
     *
     * @return The locator
     */
    public String getLocator() {
        if (this.provider == null)
            return this.taxonID;
        return this.provider.getId() + ":" + this.taxonID;
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
     * Test to see if this instance has a simple synonym loop.
     * <p>
     * A simple loop is one provided by the source taxonomy and uses {@link #getAccepted()} to navigate links.
     * This returns the lowest scored element from the loop. So <code>A(50) -> B(100) -> C(60) -> B(100)</code> will return C.
     * The first detected loop element is where we can insert a break.
     * </p>
     *
     * @return The first instance where there is a look
     */
    public TaxonConceptInstance findSimpleSynonymLoop() {
        List<TaxonConceptInstance> trace = new ArrayList<>();
        TaxonConceptInstance tci = this;
        int steps = MAX_RESOLUTION_STEPS;

        while (tci != null && steps > 0) {
            if (trace.contains(tci)) {
                int index = trace.indexOf(tci);
                trace = trace.subList(index, trace.size());
                trace.sort((tci1, tci2) -> tci1.getTaxonID().compareTo(tci2.getTaxonID()));
                trace.sort((tci1, tci2) -> tci1.getScore() - tci2.getScore());
                return trace.isEmpty() ? tci : trace.get(0);
            }
            trace.add(tci);
            steps--;
            TaxonomicElement a = tci.getAccepted();
            tci = a != null && (a instanceof TaxonConceptInstance) ? (TaxonConceptInstance) a : null;
        }
        return tci;
    }

    /**
     * Break a synonym loop by makling this instance inferred unplaced.
     * <p>
     * If a parent hasn't been supplied, then make it the unknown taxon.
     * </p>
     *
     * @param taxonomy The taxonomy to use
     */
    public void resolveSynonymLoop(Taxonomy taxonomy) {
        TaxonConceptInstance breakPoint = this.findSimpleSynonymLoop();
        if (breakPoint == null) // Already corrected by something else
            return;
        if (breakPoint != this) {
            breakPoint.resolveSynonymLoop(taxonomy);
            return;
        }
        taxonomy.report(IssueType.PROBLEM, "instance.accepted.resolve.loop", this, this.traceAccepted());
        this.taxonomicStatus = TaxonomicType.INFERRED_UNPLACED;
        this.accepted = null;
        this.acceptedNameUsage = null;
        this.acceptedNameUsageID = null;
        this.score = null;
        if (this.parent == null) {
            String unknownTaxonID = this.getProvider().getUnknownTaxonID();
            TaxonConceptInstance unknownTaxon = taxonomy.getInstance(unknownTaxonID);
            this.parentNameUsage = null;
            this.parentNameUsageID = unknownTaxonID;
            this.parent = unknownTaxon;
        }
        String provenance = taxonomy.getResources().getString("instance.accepted.resolve.loop.provenance");
        this.addProvenance(MessageFormat.format(provenance, this.taxonID));
    }
    /**
     * Test to see if this instance is a synonym whose accepted name
     * calls this instance its parent
     * <p>
     * A synonym/parent loop is one provided by the source taxonomy. This checks for a close loop
     * and doesn't look for a loop more than one step away. If found, change synonym to inferred accepted and
     * make its parent the unknown taxon.
     *
     * @return TaxonConcept if there is a loop, otherwise return null
     * </p>
     */
    public TaxonConceptInstance findSimpleSynonymParentLoop(){
        TaxonConceptInstance tci = this;
        if (tci.acceptedNameUsageID == null) // not a synonym
        {
            return null;
        }
        TaxonConceptInstance acceptedInstance =  tci.getResolvedAccepted();
        if (acceptedInstance != null && acceptedInstance.parentNameUsageID!= null) {
            if (acceptedInstance.parentNameUsageID.equals(tci.taxonID)) {
                return tci;
            }
        }
        return null;
    }

    public void resolveSimpleSynonymParentLoop(Taxonomy taxonomy){

                taxonomy.report(IssueType.PROBLEM, "instance.accepted.synonym.loop", this.taxonID, this.acceptedNameUsageID );
                this.taxonomicStatus = TaxonomicType.INFERRED_UNPLACED;
                this.accepted = null;
                this.acceptedNameUsage = null;
                this.acceptedNameUsageID = null;
                this.score = null;

                String unknownTaxonID = this.getProvider().getUnknownTaxonID();
                TaxonConceptInstance unknownTaxon = taxonomy.getInstance(unknownTaxonID);
                this.parentNameUsage = null;
                this.parentNameUsageID = unknownTaxonID;
                this.parent = unknownTaxon;

                String provenance = taxonomy.getResources().getString("instance.accepted.synonym.loop.provenance");
                this.addProvenance(provenance);



    }


    /**
     * Test to see if this instance has a simple parent loop.
     * <p>
     * A simple loop is one provided by the source taxonomy and uses {@link #getParent()} to navigate links.
     * This returns the highest ranking loop element. So <code>species -> genus -> family -> genus</code> will return family.
     * The highest ranking loop element is where we can insert a break.
     * </p>
     *
     * @return True if there is a parent loop
     */
    public TaxonConceptInstance findSimpleParentLoop() {
        List<TaxonConceptInstance> trace = new ArrayList<>();
        TaxonConceptInstance tci = this;
        int steps = MAX_RESOLUTION_STEPS;

        while (tci != null && steps > 0) {
            if (trace.contains(tci))
                return trace.stream().min((tci1, tci2) -> tci1.getRank().getId() - tci2.getRank().getId()).orElse(tci);
            trace.add(tci);
            steps--;
            TaxonomicElement p = tci.getParent();
            tci = p != null && (p instanceof TaxonConceptInstance) ? (TaxonConceptInstance) p : null;
        }
        return tci;
    }

    /**
     * Break a parent loop by making the parent the unknown taxon.
     *
     * @param taxonomy The taxonomy to use
     */
    public void resolveParentLoop(Taxonomy taxonomy) {
        TaxonConceptInstance breakPoint = this.findSimpleParentLoop();
        if (breakPoint == null) // Already corrected by something else
            return;
        if (breakPoint != this) {
            breakPoint.resolveParentLoop(taxonomy);
            return;
        }
        this.taxonomicStatus = TaxonomicType.INFERRED_UNPLACED;
        String unknownTaxonID = this.getProvider().getUnknownTaxonID();
        TaxonConceptInstance unknownTaxon = taxonomy.getInstance(unknownTaxonID);
        taxonomy.report(IssueType.PROBLEM, "instance.parent.resolve.loop", this, this.traceParent());
        this.parent = unknownTaxon;
        this.parentNameUsage = null;
        this.parentNameUsageID = unknownTaxonID;
        this.score = null;
        String provenance = taxonomy.getResources().getString("instance.parent.resolve.loop.provenance");
        this.addProvenance(provenance);
    }


    /**
     * Break an invalid parent by making the parent the unknown taxon.
     *
     * @param taxonomy The taxonomy to use
     */
    public void resolveInvalidParent(Taxonomy taxonomy) {
        this.taxonomicStatus = TaxonomicType.INFERRED_UNPLACED;
        String unknownTaxonID = this.getProvider().getUnknownTaxonID();
        TaxonConceptInstance unknownTaxon = taxonomy.getInstance(unknownTaxonID);
        taxonomy.report(IssueType.PROBLEM, "instance.parent.resolve.invalid", this, this.traceParent());
        this.parent = unknownTaxon;
        this.parentNameUsage = null;
        this.parentNameUsageID = unknownTaxonID;
        this.score = null;
        String provenance = taxonomy.getResources().getString("instance.parent.resolve.invalid.provenance");
        this.addProvenance(provenance);
    }


    /**
     * Trace all parents, making sure that there isn't a loop.
     *
     * @param taxonomy The source taxonomy
     * @return True if the parent chain is valid
     */
    private boolean validateParent(Taxonomy taxonomy) {
        if (this.parent == null)
            return true;
        TaxonomicElement loop = this.findSimpleParentLoop();
        if (loop != null) {
            taxonomy.report(IssueType.VALIDATION, "instance.validation.parent.loop", this, this.traceParent());
            return false;
        }
        return true;
    }

    /**
     * Find the resolved parent, one step at a time
     *
     * @param original  The original instance
     * @param steps     The number of steps allowed in resolution
     * @param trace     The trace of steps (null not to keep a record of the resolved parents)
     * @param exception Throw an exception if a loop is detected, otherwise return null
     * @return The resolved parent or null for none
     * @throws ResolutionException if parent resolution fails (usually via a loop of parents)
     */
    private TaxonConceptInstance getResolvedParent(TaxonConceptInstance original, int steps, @Nullable List<TaxonomicElement> trace, boolean exception) {
        if (steps <= 0) {
            if (original.isForbidden() || this.isForbidden())
                return null;
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
        if (parent == null)
            return null;
        parent = parent.getResolvedAccepted(original, steps - 1, trace, exception);
        if (parent == null)
            return null;
        if (trace != null && trace.contains(parent)) {
            trace.add(parent);
            if (exception)
                throw new ResolutionException("Detected possible loop resolving parent " + original, trace);
            return null;
        }
        if (trace != null)
            trace.add(parent);
        if (parent == null || !parent.isForbidden()) // Forbidden parents are still in the taxonomy but we want to avoid them and bump up to the next level
            return parent;
        return parent.getResolvedParent(original, steps - 1, trace, exception);
    }

    /**
     * Get the completely resolved, accepted taxon concept instance for this instance.
     *
     * @return The final accepted instance (this should not be null, as an instance should at least resolve to itself)
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
     * @param original  The initial instance
     * @param steps     The number of resolution steps allowed
     * @param trace     An accumulating trace of steps (may be null in which case the trace is not collected)
     * @param exception Throw an exceptioon if a loop is detected, otherwise the original is returned
     * @return The accepted taxon concept instance
     * @throws ResolutionException if parent resolution fails (usually via a resolution loop)
     */
    private TaxonConceptInstance getResolvedAccepted(TaxonConceptInstance original, int steps, @Nullable List<TaxonomicElement> trace, boolean exception) {
        if (steps <= 0) {
            if (original.isForbidden() || this.isForbidden()) {
                return null;
            }
            if (exception) {
                throw new ResolutionException("Detected possible loop resolving accepted " + original, trace);
            }
            return original;
        }
        TaxonConceptInstance resolved = this.getResolved(original, steps - 1);
        if (resolved == null) {
            if (original.isForbidden() || this.isForbidden()) {
                return null;
            }
            if (exception) {
                throw new ResolutionException("Detected dangling resolution resolving accepted " + original, trace);
            }
            return original;
        }
        TaxonomicElement ae = resolved.getAccepted();
        if (ae == null || ae == resolved) {
            return resolved;
        }
        if (trace != null && trace.contains(ae)) {
            trace.add(ae);
            if (exception) {
                throw new ResolutionException("Detected possible loop resolving accepted " + original, trace);
            }
            return original;
        }
        if (trace != null) {
            trace.add(ae);
        }
        TaxonConceptInstance accepted = ae.getRepresentative();
        if (accepted == null) {
            logger.warn("Null representative instance for " + ae + " when resolving " + this);
            return resolved;
        }
        accepted = accepted.getResolvedAccepted(original, steps - 1, trace, exception);
//        Null being returned from this - but can't see why this function should ever return null.
        if (accepted == null){
            logger.warn("Inexplicable Null representative instance for " + ae + " when resolving " + this);
            return resolved;
        }
        if (!accepted.isForbidden()) {
            return accepted;
        }
        return accepted.getResolvedParent(original, steps - 1, trace, exception);
    }

    private TaxonConceptInstance getResolved(TaxonConceptInstance original, int steps) {
        if (steps <= 0) {
            if (this.isForbidden()) {
                return null;
            }
            throw new ResolutionException("Detected possible loop resolving " + original);
        }
        TaxonConceptInstance resolved = this.getResolved();
        if (resolved != null && this != resolved) {
            return resolved.getResolved(original, steps - 1);
        }
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
        return this.taxonomicStatus != null && this.taxonomicStatus.isPrimary() && !this.hasFlag(TaxonFlag.SYNTHETIC);
    }

    /**
     * Is this a geographic instance?
     *
     * @return True if the tanomomic status is geographic
     */
    public boolean isGeographic() {
        return this.taxonomicStatus != null && this.taxonomicStatus.isGeographic() && !this.hasFlag(TaxonFlag.SYNTHETIC);
    }

    /**
     * Clean up common messinesses with the instance caused by different conventions:
     * <ul>
     *     <li>If the acceptedNameUsageID is the same as the taxonID then it is set to null</li>
     *     <li>If the scientificName has the scientificNameAuthorship in it then the authorship is removed</li>
     * </ul>
     *
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
     * @return True if successfully resolved
     * @throws IndexBuilderException If unable to make a link, usually due to a broken reference
     */
    // If you plan to change this, it is called by a parallel stream, so consisder thread safety
    // At the moment, this fills out inferred information only
    public boolean resolveLinks(Taxonomy taxonomy) throws IndexBuilderException {
        if (this.parentNameUsageID != null) {
            this.parent = taxonomy.getInstance(this.parentNameUsageID);
        }
        if (this.parentNameUsage != null && this.parent == null) {
            this.parent = taxonomy.findElement(this.code, this.parentNameUsage, this.provider, null);
        }
        if (this.parent == null && (this.parentNameUsage != null || this.parentNameUsageID != null)) {
            StringBuilder name = new StringBuilder();
            if (this.parentNameUsageID != null)
                name.append(this.parentNameUsageID);
            if (this.parentNameUsage != null) {
                if (name.length() > 0)
                    name.append(" - ");
                name.append(this.parentNameUsage);
            }
            taxonomy.report(IssueType.ERROR, "instance.parent.invalidLink", this.taxonID, this.scientificName, "Unable to find parent taxon for " + this + " from " + name);
            if (this.acceptedNameUsageID == null && this.acceptedNameUsage == null && this.classification == null)
                return false;
            this.addProvenance("Unable to find supplied parent taxon " + name);
        }
        if (this.acceptedNameUsageID != null) {
            this.accepted = taxonomy.getInstance(this.acceptedNameUsageID);
        }
        if (this.acceptedNameUsage != null && this.accepted == null) {
            this.accepted = taxonomy.findElement(this.code, this.acceptedNameUsage, this.provider, null);
        }
        if (this.accepted == null && (this.acceptedNameUsage != null || this.acceptedNameUsageID != null)) {
            StringBuilder name = new StringBuilder();
            if (this.acceptedNameUsageID != null)
                name.append(this.acceptedNameUsageID);
            if (this.acceptedNameUsage != null) {
                if (name.length() > 0)
                    name.append(" - ");
                name.append(this.acceptedNameUsage);
            }
            taxonomy.report(IssueType.ERROR, "instance.accepted.invalidLink", this.taxonID, this.scientificName, "Unable to find accepted taxon for " + this + " from " + name);
            if (this.classification == null)
                return false;
            this.addProvenance("Unable to find accepted taxon " + name);
        }
        // No parent or accepted taxon but has a classification, so see if we can deduce a parent
        if (this.parent == null && this.accepted == null && this.classification != null) {
            String genus = "";
            String specificEpithet = "sp.";
            for (int i = 0; i < CLASSIFICATION_FIELDS.size(); i++) {
                Term cls = CLASSIFICATION_FIELDS.get(i);
                RankType clr = CLASSIFICATION_RANKS.get(i);
                if (!this.rank.isLoose() && !clr.isHigherThan(this.rank)) {
                    continue;
                }
                Optional<String> name = this.classification.get(cls);
                if (name != null && name.isPresent()) {
                    String n = name.get();
                    if (cls.equals(DwcTerm.genus))
                        genus = n;
                    if (cls.equals(DwcTerm.specificEpithet)) {
                        specificEpithet = n;
                        n = (genus + " " + n).trim();
                    }
                    if (cls.equals(DwcTerm.infraspecificEpithet)) {
                        n = (genus + " " + specificEpithet + " " + n).trim();
                    }
                    if (!n.equalsIgnoreCase(this.scientificName)) {
                        TaxonomicElement p = taxonomy.findElement(this.code, n, this.provider, clr);
                        RankType pr = p != null ? p.getRank() : null;
                        if (p != null && p != this && (this.rank.isLoose() || pr.isLoose() || pr == null || pr.isHigherThan(this.rank)))
                            this.parent = p;
                    }
                }
            }
        }
        if (this.parent == null && this.accepted == null && !this.rank.isHigherThan(RankType.PHYLUM)) {
            taxonomy.count("count.resolve.instance.defaultParent");
            this.parent = this.provider.findDefaultParent(taxonomy, this);
            this.addProvenance("Assigned to default parent taxon");
        }
        if (this.parent != null && this.accepted == null && this.isSynonym()) {
            this.taxonomicStatus = TaxonomicType.INFERRED_ACCEPTED;
            this.addProvenance("Synonym without accepted taxon treated as accepted");
        }
        taxonomy.count("count.resolve.instance.links");
        return true;
    }

    /**
     * Detect concepts that should be discarded.
     * <p>
     * Check for synthetic, not (yet) forbidden instances that have no legitimate children any more.
     * </p>
     *
     * @param taxonomy The base taxonomy
     * @throws IndexBuilderException if unable to manage the forbidden taxon instance
     */
    // Note that this is not thread-safe due to index writing
    public void detectDiscard(Taxonomy taxonomy, Collection<TaxonConceptInstance> allInstances) throws IndexBuilderException {
        if (!this.hasFlag(TaxonFlag.SYNTHETIC) || this.isForbidden())
            return;
        boolean required = allInstances.stream().anyMatch(tci -> !tci.isForbidden() && tci.getResolvedParent() == this);
        if (!required) {
            this.setForbidden(true);
            taxonomy.count("count.resolve.synthetic.discarded");
            taxonomy.report(IssueType.NOTE, "instance.discarded.synthetic", this, null);
        }
    }

    /**
     * Work out twhat to do with this instance if it is forbidden.
     *
     * @param taxonomy The base taxonomy
     * @throws IndexBuilderException if unable to manage the forbidden taxon instance
     */
    // Note that this is not thread-safe due to index writing
    public void resolveDiscarded(Taxonomy taxonomy) throws IndexBuilderException {
        if (!this.isForbidden())
            return;
        TaxonConceptInstance parent = this.getResolvedParent();
        if (parent == null)
            return; // Ignore and leave forbidden
        String provenance;
        switch (this.getProvider().getDiscardStrategy()) {
            case IDENTIFIER_TO_PARENT:
                // Add an additional identifier to the parent
                Document doc = new Document();
                doc.add(new StringField("type", GbifTerm.Identifier.qualifiedName(), Field.Store.YES));
                doc.add(new StringField("id", UUID.randomUUID().toString(), Field.Store.YES));
                doc.add(new StringField(Taxonomy.fieldName(DwcTerm.taxonID), parent.getTaxonID(), Field.Store.YES));
                doc.add(new StringField(Taxonomy.fieldName(DcTerm.identifier), this.getTaxonID(), Field.Store.YES));
                doc.add(new StringField(Taxonomy.fieldName(DwcTerm.datasetID), taxonomy.getInferenceProvider().getId(), Field.Store.YES));
                doc.add(new StringField(Taxonomy.fieldName(DcTerm.title), taxonomy.getResources().getString("instance.discarded.identifier.title"), Field.Store.YES));
                doc.add(new StringField(Taxonomy.fieldName(ALATerm.status), "discarded", Field.Store.YES));
                if (this.taxonRemarks != null) {
                    doc.add(new StringField(Taxonomy.fieldName(DcTerm.description), this.getTaxonRemarkString(), Field.Store.YES));
                }
                provenance = taxonomy.getResources().getString("instance.discarded.identifier.provenance");
                provenance = MessageFormat.format(provenance, this.getScientificName());
                doc.add(new StringField(Taxonomy.fieldName(DcTerm.provenance), provenance, Field.Store.YES));
                taxonomy.addProvenanceToOutput();
                try {
                    taxonomy.addRecords(Collections.singletonList(doc));
                } catch (IOException ex) {
                    throw new IndexBuilderException("Unable to process discard", ex);
                }
                break;
            case SYNONYMISE_TO_PARENT:
                this.provider = taxonomy.getInferenceProvider();
                this.forbidden = false;
                this.taxonomicStatus = TaxonomicType.INFERRED_SYNONYM;
                this.parent = null;
                this.parentNameUsage = null;
                this.parentNameUsageID = null;
                this.accepted = parent;
                this.acceptedNameUsage = null;
                this.acceptedNameUsageID = parent.getTaxonID();
                this.score = this.provider.getDefaultScore();
                provenance = taxonomy.getResources().getString("instance.discarded.synonym.provenance");
                provenance = MessageFormat.format(provenance, this.getTaxonID());
                this.addProvenance(provenance);
                taxonomy.addProvenanceToOutput();
                this.getContainer().resolveTaxon(taxonomy, true);
                break;
            default:
                // Ignore and leave forbidden
                break;
        }

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
     * @param strict Strictly observe parent/accepted division
     * @return The taxon map
     * @throws IOException if unable to retrieve source documents
     */
    public Map<Term, String> getTaxonMap(Taxonomy taxonomy, boolean strict) throws IOException {
        List<Map<Term, String>> valuesList = taxonomy.getIndexValues(DwcTerm.Taxon, this.taxonID);
        final Map<Term, String> values;
        if (valuesList.isEmpty()) {
            if (this.provider != taxonomy.getInferenceProvider())
                taxonomy.report(IssueType.NOTE, "instance.noIndex", this, null);
            values = new HashMap<>();
        } else {
            if (valuesList.size() > 1)
                taxonomy.report(IssueType.ERROR, "instance.multiIndex", this, null);
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
        if (this.taxonRemarks != null)
            values.put(DwcTerm.taxonRemarks, this.getTaxonRemarkString());
        if (this.verbatimTaxonRemarks != null)
            values.put(ALATerm.verbatimTaxonRemarks, this.verbatimTaxonRemarks);
        if (this.provenance != null)
            values.put(DcTerm.provenance, this.getProvenanceString());
        if (this.parent == null || (strict && !this.getAuthority().allowParentOutput(this))) {
            values.remove(DwcTerm.parentNameUsageID); // If instance has become a synonym
            values.remove(DwcTerm.parentNameUsage);
        } else {
            String pid = null;
            try {
                TaxonConceptInstance rp = this.getResolvedParent();
                if (rp == this) {
                    throw new ResolutionException("Hidden loop in parent");
                } else {
                    pid = rp == null ? null : rp.getTaxonID();
                }
            } catch (ResolutionException ex) {
                pid = this.provider.getUnknownTaxonID();
                taxonomy.report(IssueType.ERROR, "instance.parent.resolve.loop", this, this.traceParent());
            }
            values.put(DwcTerm.parentNameUsageID, pid);
        }
        if (this.accepted == null || (strict && !this.getAuthority().allowAcceptedOutput(this))) {
            values.remove(DwcTerm.acceptedNameUsageID); // If instance has become accepted
            values.remove(DwcTerm.acceptedNameUsage);
        } else {
            String aid = null;
            try {
                TaxonConceptInstance ra = this.getResolvedAccepted();
                if (ra == this) {
                    throw new ResolutionException("Hidden loop in accepted");
                } else {
                    aid = ra == null || ra == this ? null : ra.getTaxonID();
                }
            } catch (ResolutionException ex) {
                aid = this.provider.getUnknownTaxonID();
                taxonomy.report(IssueType.ERROR, "instance.accepted.resolve.loop", this, this.traceAccepted());
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
     * @return The identifier list
     * @throws IOException if unable to retrive source documents
     */
    public List<Map<Term, String>> getIdentifierMaps(Taxonomy taxonomy) throws IOException {
        final Map<Term, String> taxon = this.getTaxonMap(taxonomy, true);
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
     * @return The vernacular name list
     * @throws IOException if unable to retrive source documents
     */
    public List<Map<Term, String>> getVernacularMaps(Taxonomy taxonomy) throws IOException {
        List<Map<Term, String>> valuesList = taxonomy.getIndexValues(GbifTerm.VernacularName, this.taxonID);
        return valuesList;
    }


    /**
     * Get a list of references associated with this taxon instance.
     *
     * @param taxonomy The taxonomy to collect additional information from
     * @return The vernacular name list
     * @throws IOException if unable to retrive source documents
     */
    public List<Map<Term, String>> getReferenceMaps(Taxonomy taxonomy) throws IOException {
        List<Map<Term, String>> valuesList = taxonomy.getIndexValues(GbifTerm.Reference, this.taxonID);
        return valuesList;
    }

    /**
     * Get a list of distribution maps associated with this taxon instance.
     *
     * @param taxonomy The taxonomy to collect additional information from
     * @return The distribution list
     * @throws IOException if unable to retrive source documents
     */
    public List<Map<Term, String>> getDistributionMaps(Taxonomy taxonomy) throws IOException {
        List<Map<Term, String>> valuesList = taxonomy.getIndexValues(GbifTerm.Distribution, this.taxonID);
        return valuesList;
    }

    /**
     * A human readbale label for the concept instance
     *
     * @return The label
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(64);
        builder.append("TCI[");
        builder.append(this.getProvider().getId());
        builder.append(":");
        builder.append(this.getTaxonID());
        builder.append(", ");
        builder.append(this.getCode());
        builder.append(", ");
        if (this.nameComplete != null) {
            builder.append(this.nameComplete);
        } else {
            builder.append(this.getScientificName());
            builder.append(", ");
            builder.append(this.getScientificNameAuthorship());
        }
        builder.append(", ");
        builder.append(this.getRank());
        builder.append(", ");
        builder.append(this.getTaxonomicStatus());
        builder.append("]");
        return builder.toString();
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
     *
     * @param concept                  The parent taxon concept
     * @param scientificName           The source name of the synonym
     * @param scientificNameAuthorship The authorship of the synonym
     * @param year                     The year of authorship
     * @param taxonomy                 The base taxonomy
     * @return A synonym that points a name towards this instance
     */
    public TaxonConceptInstance createInferredSynonym(TaxonConcept concept, String scientificName, String scientificNameAuthorship, String nameComplete, String year, Taxonomy taxonomy) {
        TaxonConceptInstance synonym = new TaxonConceptInstance(
                UUID.randomUUID().toString(),
                this.code,
                this.verbatimNomenclaturalCode,
                taxonomy.getInferenceProvider(),
                scientificName,
                scientificNameAuthorship,
                nameComplete,
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
                this.taxonRemarks == null ? null : new ArrayList<>(this.taxonRemarks),
                this.verbatimTaxonRemarks,
                this.provenance == null ? null : new ArrayList<>(this.provenance),
                this.classification,
                this.flags,
                this.distribution,
                this.vernacularNames == null ? null : new ArrayList<>(this.vernacularNames)
        );
        synonym.setContainer(concept);
        synonym.accepted = this;
        synonym.baseScore = null;
        synonym.score = null;
        synonym.forbidden = false;
        String provenance = taxonomy.getResources().getString("instance.inferredSynonym.provenance");
        provenance = MessageFormat.format(provenance, this.getTaxonID(), this.provider.getId());
        synonym.addProvenance(provenance);
        taxonomy.addProvenanceToOutput();
        taxonomy.addInferredInstance(synonym);
        return synonym;
    }

    /**
     * Create an ranked instance of this taxon.
     *
     * @param newRank
     * @param taxonomy The base taxonomy
     * @return A ranked copy of this instance
     */
    public TaxonConceptInstance createRankedInstance(RankType newRank, Taxonomy taxonomy) {
        TaxonConceptInstance instance = new TaxonConceptInstance(
                this.taxonID,
                this.code,
                this.verbatimNomenclaturalCode,
                this.provider,
                this.scientificName,
                this.scientificNameAuthorship,
                this.nameComplete,
                this.year,
                this.taxonomicStatus,
                this.verbatimTaxonomicStatus,
                newRank,
                this.verbatimTaxonRank,
                this.status,
                this.verbatimNomenclaturalStatus,
                this.parentNameUsage,
                this.parentNameUsageID,
                this.acceptedNameUsage,
                this.acceptedNameUsageID,
                this.taxonRemarks == null ? null : new ArrayList<>(this.taxonRemarks),
                this.verbatimTaxonRemarks,
                this.provenance == null ? null : new ArrayList<>(this.provenance),
                this.classification,
                this.flags,
                this.distribution,
                this.vernacularNames == null ? null : new ArrayList<>(this.vernacularNames)
                );
        instance.setContainer(null);
        instance.accepted = this.accepted;
        instance.parent = this.parent;
        instance.baseScore = null;
        instance.score = null;
        instance.forbidden = false;
        String provenance = taxonomy.getResources().getString("taxonConcept.unranked.reallocate.provenance");
        provenance = MessageFormat.format(provenance, newRank.getRank());
        instance.addProvenance(provenance);
        taxonomy.report(IssueType.NOTE, "taxonConcept.unranked.reallocate", this, Arrays.asList(instance));
        taxonomy.count("count.resolve.unrankedTaxonConcept");
        taxonomy.addProvenanceToOutput();
        taxonomy.insertInstance(instance.getTaxonID(), this.getContainer().getKey().toRankedNameKey(newRank), instance);
        return instance;
    }

    /**
     * Convert this instance into a synonym of a newer instance.
     * <p>
     * This instance is forbidden but remains to allow accepted taxon resolution.
     * </p>
     *
     * @param other    The other instance
     * @param taxonomy
     */
    public void forwardTo(TaxonConceptInstance other, Taxonomy taxonomy) {
        if (other.getTaxonID().equals(this.taxonID)) {
            this.taxonID = UUID.randomUUID().toString();
            taxonomy.addInferredInstance(this);
        }
        this.setForbidden(true);
        this.provider = taxonomy.getInferenceProvider();
        this.taxonomicStatus = TaxonomicType.INFERRED_SYNONYM;
        this.acceptedNameUsage = null;
        this.acceptedNameUsageID = other.getTaxonID();
        this.accepted = other;
        this.parentNameUsage = null;
        this.parentNameUsageID = null;
        this.parent = null;
        this.classification = null;
    }


    /**
     * Test to see if the parent is invalid.
     *
     * @return True if there is information suggesting that there should be a parent but no parent has been found.
     */
    public boolean hasInvalidParent() {
        if (this.parent != null)
            return false;
        boolean check = this.parentNameUsageID != null;
        check = check || this.parentNameUsage != null;
        if (this.isAccepted() && this.classification != null && !this.rank.isHigherThan(RankType.PHYLUM)) {
            check = check || this.classification.values().stream().anyMatch(v -> v.isPresent() && !v.get().equals(this.scientificName));
        }
        return check;
    }

    /**
     * Validate this taxon concept instance.
     *
     * @param taxonomy The taxonomy to validate against and report to
     * @return True if the scientific name is valid
     */
    // If you plan to change this, it is called by a parallel stream, so consider thread safety
    @Override
    public boolean validate(Taxonomy taxonomy) {
        boolean valid = true;
        if (this.hasInvalidParent()) {
            if (this.provider.isLoose())
                taxonomy.report(IssueType.NOTE, "instance.validation.noParent.loose", this, null);
            else {
                taxonomy.report(IssueType.VALIDATION, "instance.validation.noParent", this, null);
                valid = false;
            }
        }
        if ((this.acceptedNameUsageID != null || this.acceptedNameUsage != null) && this.accepted == null) {
            if (this.provider.isLoose())
                taxonomy.report(IssueType.NOTE, "instance.validation.noAccepted.loose", this, null);
            else {
                taxonomy.report(IssueType.VALIDATION, "instance.validation.noAccepted", this, null);
                valid = false;
            }

        }
        if (this.parent != null && this.isSynonym()) {
            taxonomy.report(IssueType.VALIDATION, "instance.validation.synonymWithParent", this, null);
            valid = false;
        }
        if (this.accepted != null && this.isAccepted()) {
            taxonomy.report(IssueType.VALIDATION, "instance.validation.acceptedWithAccepted", this, null);
            valid = false;
        }
        if (this.getContainer() == null) {
            taxonomy.report(IssueType.VALIDATION, "instance.validation.noTaxonConcept", this, null);
            valid = false;

        } else if (this.getContainer().getContainer() == null) {
            taxonomy.report(IssueType.VALIDATION, "instance.validation.noScientificName", this, null);
            valid = false;
        }
        valid = valid && this.validateParent(taxonomy);
        return valid;
    }

}
