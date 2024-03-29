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

import au.org.ala.names.index.provider.*;
import au.org.ala.names.model.RankType;
import com.fasterxml.jackson.annotation.*;
import org.gbif.api.model.registry.Citation;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;

import javax.annotation.Nullable;
import java.util.*;

/**
 * A provider of name information.
 * <p>
 * Providers can be used to prioritise clashing names.
 * Providers can have a parent that provides default behaviours.
 * This is useful when computing forbidden instances or adjustments.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 *
 * Copyright (c) 2016 CSIRO
 */

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NameProvider {
    /** The default score for a name provider, giving room for lower scores */
    public static final int DEFAULT_SCORE = 100;
    /** The default uknown taxon identifier */
    public static final String DEFAULT_UNKNOWN_TAXON_ID = "ALA_The_Unknown_Taxon";


    /** The provider identifier */
    @JsonProperty
    private String id;
    /** The provider name */
    @JsonProperty
    private String name;
    /** The provider description */
    @JsonProperty
    private String description;
    /** The rights holder */
    @JsonProperty
    private String rightsHolder;
    /** The licence */
    @JsonProperty
    private String licence;
    /** The parent provider */
    @JsonProperty
    private NameProvider parent;
    /** The default source score */
    @JsonProperty
    private Integer defaultScore;
    /** Any additional priorities for this provider, based on name */
    @JsonProperty
    private Map<String, Integer> scores;
    /** Assert ownership of a particular scientific name */
    @JsonProperty
    private Set<String> owner;
    /** Score adjustments */
    @JsonProperty
    private ScoreAdjuster adjuster;
    /** Key adjustments */
    @JsonProperty
    private KeyAdjuster keyAdjuster;
    /** The default nomenclatural code */
    @JsonProperty
    private NomenclaturalClassifier defaultNomenclaturalCode;
    /** Is this a "loose" taxonomy, where we can expect fragments of taxonomy and we shouldn't worry too much about consistency */
    @JsonProperty
    private boolean loose;
    /** Is this an "external" name provider - something that can be referenced */
    @JsonProperty
    private boolean external;
    /** Is this an authority - meaning that sub-providers come from this authority */
    @JsonProperty
    private boolean authority;
    /** The method of discarding forbidden taxa */
    @JsonProperty
    private DiscardStrategy discardStrategy;
    /** Assign unranked elements to ranked elements */
    @JsonProperty
    private UnrankedStrategy unrankedStrategy;
    /** How to handle concept conflicts */
    @JsonProperty
    private ConceptResolutionPriority conceptResolutionPriority;
    /** The identifier of the unknown taxon */
    @JsonProperty
    private String unknownTaxonID;
    /** A default parent taxon, if one is not specified. This can be used in combination with the nomenclatural code to provide a parent if one is absent. */
    @JsonProperty
    private String defaultParentTaxon;
    /** Any spelling corrections needed on names. */
    @JsonProperty
    private Map<String, String> scientificNameChanges;
    /** Any spelling corrections needed on authors. */
    @JsonProperty
    private Map<String, String> scientificNameAuthorshipChanges;
    @JsonProperty
    private RankType distributionCutoff;
    /** Reporter for any problems */
    @JsonIgnore
    private Reporter reporter;

    /**
     * Default constructor
     */
    public NameProvider() {
        this.id = UUID.randomUUID().toString();
        this.name = this.id;
        this.description = null;
        this.rightsHolder = null;
        this.licence = null;
        this.parent = null;
        this.defaultScore = null;
        this.defaultNomenclaturalCode = null;
        this.scores = new HashMap<>();
        this.owner = new HashSet<>();
        this.adjuster = new ScoreAdjuster();
        this.keyAdjuster = new KeyAdjuster();
        this.loose = false;
        this.external = true;
        this.authority = true;
        this.scientificNameChanges = new HashMap<>();
        this.scientificNameAuthorshipChanges = new HashMap<>();
        this.distributionCutoff = null;
        this.reporter = new LogReporter();
    }
    
     public NameProvider(String id, Integer defaultScore, String unknownTaxonID, Map<String, Integer> scores) {
        this.id = id;
        this.name = this.id;
        this.description = null;
        this.rightsHolder = null;
        this.licence = null;
        this.parent = null;
        this.defaultScore= defaultScore;
        this.defaultNomenclaturalCode = null;
        this.scores = scores;
        this.owner = new HashSet<>();
        this.adjuster = new ScoreAdjuster();
        this.keyAdjuster = new KeyAdjuster();
        this.loose = false;
        this.external = true;
        this.authority = true;
        this.unknownTaxonID = unknownTaxonID;
        this.scientificNameChanges = new HashMap<>();
        this.scientificNameAuthorshipChanges = new HashMap<>();
        this.distributionCutoff = null;
        this.reporter = new LogReporter();
    }

    /**
     * Create an ad-hoc name source with a parent.
     *
     * @param id The source identifier
     * @param name The source name
     * @param parent The default source priority
     * @param loose Is this a loose provider?
     */
    public NameProvider(String id, String name, NameProvider parent, boolean loose) {
        if (parent == this)
            throw new IllegalArgumentException("Parent same as child for " + id);
        this.id = id;
        this.name = name;
        this.description = null;
        this.rightsHolder = null;
        this.licence = null;
        this.parent = parent;
        this.defaultScore = parent.getDefaultScore();
        this.defaultNomenclaturalCode = null;
        this.scores = new HashMap<>();
        this.owner = new HashSet<>();
        this.adjuster = new ScoreAdjuster();
        this.keyAdjuster = new KeyAdjuster();
        this.loose = loose;
        this.external = true;
        this.authority = true;
        this.scientificNameChanges = new HashMap<>();
        this.scientificNameAuthorshipChanges = new HashMap<>();
        this.distributionCutoff = null;
        this.reporter = new LogReporter();
    }

    /**
     * Create a simple name source.
     *
     * @param id The source identifier
     * @param defaultScore The default source priority
     */
    public NameProvider(String id, int defaultScore) {
        this(id, defaultScore, DEFAULT_UNKNOWN_TAXON_ID, Collections.EMPTY_MAP);
    }

    /**
     * Get the source name.
     *
     * @return The source name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the source identifier.
     *
     * @return The source ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the source description.
     * <p>
     * Intended as a place to put useful human-readable descriptions
     * </p>
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the rights holder.
     * <p>
     * If one is not explicitly set, get the parent rights holder
     * </p>
     *
     * @return The rights holder
     */
    public String getRightsHolder() {
        if (rightsHolder == null && this.parent != null)
            return this.parent.getRightsHolder();
        return rightsHolder;
    }

    /**
     * Get the licence.
     * <p>
     * If one is not explicitly set, get the parent licence
     * </p>
     *
     * @return The licence
     */
    public String getLicence() {
        if (licence == null && this.parent != null)
            return this.parent.getLicence();
        return licence;
    }

    /**
     * Get the parent name provider
     *
     * @return
     */
    public NameProvider getParent() {
        return parent;
    }

    /**
     * Is this a "loose" provider, meaning that it may not have a consisent taxonomy?
     * <p>
     * Note that this is not inherited from the parent taxonmy
     * </p>
     *
     * @return True if this is a loose provider
     */
    public boolean isLoose() {
        return loose;
    }

    /**
     * Is this an external provider, meaning that the data has a connection with an outside source.
     *
     * @return True if external
     */
    public boolean isExternal() {
        return external;
    }

    /**
     * Is this an "authority" provider, meaning that it's a single organisation that can provide
     * multiple, semi-related data sources.
     *
     * @return True if this is an institution
     */
    public boolean isAuthority() {
        return authority;
    }

    /**
     * Return the institutional provider for this provider.
     * <p>
     * If not institutional, then
     * </p>
     *
     * @return The provider that represents the institution this comes from.
     */
    public NameProvider getAuthority() {
        if (this.authority)
            return this;
        if (this.parent != null)
            return this.parent.getAuthority();
        return null;
    }

    /**
     * Get the discard strategy.
     * <p>
     * This determines how forbidden or otherwise discarded taxa should be treated.
     * If one is not explicitly set, get the parent strategy.
     * By default, this is {@link DiscardStrategy#IGNORE}.
     * </p>
     *
     * @return The discard strategy.
     */
    public DiscardStrategy getDiscardStrategy() {
        if (this.discardStrategy != null)
            return this.discardStrategy;
        DiscardStrategy ds =  this.parent != null ? this.parent.getDiscardStrategy() : null;
        return ds == null ? DiscardStrategy.IGNORE : ds;
    }

    /**
     * Get the unranked strategy.
     * <p>
     * This determines how unranked taxa should be treated.
     * If one is not explicitly set, get the parent strategy.
     * By default, this is {@link UnrankedStrategy#NONE}.
     * </p>
     *
     * @return The unranked strategy.
     */
    public UnrankedStrategy getUnrankedStrategy() {
        if (this.unrankedStrategy != null)
            return this.unrankedStrategy;
        UnrankedStrategy us =  this.parent != null ? this.parent.getUnrankedStrategy() : null;
        return us == null ? UnrankedStrategy.NONE : us;
    }

    /**
     * Get the concept resolution priority.
     * <p>
     * This determines how differences in opinion about concepts for a scientific name should be treated.
     * If one is not explicitly set, get the parent strategy.
     * By default, this is {@link ConceptResolutionPriority#AUTHORATATIVE}.
     * </p>
     *
     * @return The concept resolution priority.
     */
    public ConceptResolutionPriority getConceptResolutionPriority() {
        if (this.conceptResolutionPriority != null)
            return this.conceptResolutionPriority;
        ConceptResolutionPriority cs =  this.parent != null ? this.parent.getConceptResolutionPriority() : null;
        return cs == null ? ConceptResolutionPriority.AUTHORATATIVE : cs;
    }

    /**
     * Get the identifier of The Unknown Taxon
     * <p>
     * If a synonym loop or bad parent is detected, the taxon id is mapped onto The Unknown Taxon.
     * This allows the BIE and name matching to not explode in tragedy from dangling synonyms.
     * If one is not explicitly set, get the parent identifier.
     * If there is no identifier, an exception is thrown.
     * </p>
     *
     * @return The unranked strategy.
     *
     * @throws IndexBuilderException if there is no unknown taxon identifier
     */
    public String getUnknownTaxonID() throws IndexBuilderException {
        if (this.unknownTaxonID != null)
            return this.unknownTaxonID;
        String utid =  this.parent != null ? this.parent.getUnknownTaxonID() : null;
        if (utid == null)
            throw new IndexBuilderException("Unable to find unknown taxon identifier for " + this.getId());
        return utid;
    }

    /**
     * Get the name of the default parent taxon.
     * <p>
     * If, during resolution, a taxon from this provider does not have a parent, then a reference
     * to this name (and default nomenclatural code, if one is available) is used.
     * If one is not explicitly set, get the parent.
     * </p>
     *
     * @return The name of the parent taxon, or null for none.
     *
     */
    @Nullable
    public String getDefaultParentTaxon() {
        if (this.defaultParentTaxon == null && this.parent != null)
            return this.parent.getDefaultParentTaxon();
        return this.defaultParentTaxon;
    }

    /**
     * Get the current reporter for issues when sorting out names
     *
     * @return The reporter
     */
    public Reporter getReporter() {
        return reporter;
    }

    /**
     * Set the reporter to use when sorting out names
     *
     * @param reporter The reporter to use
     */
    public void setReporter(Reporter reporter) {
        this.reporter = reporter;
    }

    /**
     * Decide whether to forbid an instance.
     * <p>
     * If there is a parent provider, then check the parent, as well.
     * </p>
     *
     * @param instance The instance
     * @param key The associated name key
     *
     * @return True if the instance is forbidden
     */
    public String forbid(TaxonConceptInstance instance, NameKey key) {
        String explain;
        if (this.parent != null && ((explain = this.parent.forbid(instance, key)) != null))
            return explain;
        return this.adjuster.forbid(instance, key);
    }

    /**
     * Do we "own" a particular scientific name?
     * <p>
     * Ownership is used to manually break collisions between taxon concepts
     * supplied by different providers where there needs to be an inferred synonym
     * created.
     * </p>
     *
     * @param name The scientific name to assert ownership of
     *
     * @return True if this data provider owns the name
     */
    public boolean owns(String name) {
        return this.owner.contains(name);
    }

    /**
     * Get the default score.
     * <p>
     * If not set and there is a parent, get the parent default.
     * If nothing has been set, then return the {@link #DEFAULT_SCORE}
     * </p>
     *
     * @return The default score
     */
    @JsonIgnore
    public int getDefaultScore() {
        if (this.defaultScore != null)
            return this.defaultScore;
        if (this.parent != null)
            return this.parent.getDefaultScore();
        return DEFAULT_SCORE;
    }

    /**
     * Get the default nomeclatural code.
     * <p>
     * If not set and there is a parent, get the parent default.
     * If nothing has been set, then return null.
     * </p>
     */
    @JsonIgnore
    public NomenclaturalClassifier getDefaultNomenclaturalCode() {
        if (this.defaultNomenclaturalCode != null)
            return this.defaultNomenclaturalCode;
        if (this.parent != null)
            return this.parent.getDefaultNomenclaturalCode();
        return null;
    }

    /**
     * Get a specific score for a name.
     * <p>
     * If there is a parent provider, then that parent is tried if there is no specific default.
     * </p>
     *
     * @param name The scientific name
     *
     * @return The specific score or null for none.
     */
    public Integer getSpecificScore(String name) {
        Integer specific = this.scores.get(name);
        if (specific != null)
            return specific;
        if (this.parent != null)
            specific = this.parent.getSpecificScore(name);
        return specific == null ? null : Math.max(TaxonomicElement.MIN_SCORE, Math.min(TaxonomicElement.MAX_SCORE, specific));
    }

    /**
     * Compute a base score for an instance.
     * <p>
     * Only accepted instances have a score, synonyms and the like have a score of 0.
     * </p>
     *
     * @param original The original instance, for loop detection
     * @param instance The instance
     *
     * @return The instance score
     */
    public int computeBaseScore(TaxonConceptInstance original, TaxonConceptInstance instance) {
        Integer specific = this.getSpecificScore(instance.getScientificName());
        if (specific != null)
            return specific;
        TaxonConceptInstance p = instance.getParent() == null ? null : instance.getParent().getRepresentative();
        if (p == original || p == instance) {
            this.reporter.report(IssueType.PROBLEM, "instance.parent.resolve.loop", original, Collections.singletonList(instance));
            p = instance.getResolvedParent();
            p = p == null ? null : p.getRepresentative();
        }
        int score = p != null && p.getProvider() == this ? p.getBaseScore(original) : this.getDefaultScore();
        return Math.max(TaxonomicElement.MIN_SCORE, Math.min(TaxonomicElement.MAX_SCORE, score));

    }

    /**
     * Compute a score for an instance.
     * <p>
     * We start with the base score and apply adjustments based on the taxonomic status,
     * nomenclatural status, etc.
     * If the provider "owns" the instance, then it gets the maximum possible score, ensuring that it is selected.
     * Similarly, if the instance is forbidden then it gets the mimimum possible score, ensuring that anything else will be preferred.
     * </p>
     *
     * @param instance The instance
     *
     * @return The instance score
     */
    public int computeScore(TaxonConceptInstance instance) {
        if (this.owns(instance.getScientificName()))
            return TaxonomicElement.MAX_SCORE;
        if (instance.isForbidden())
            return TaxonomicElement.MIN_SCORE;
        int score = instance.getBaseScore();
        score = this.adjustScore(score, instance);
        return Math.max(TaxonomicElement.MIN_SCORE, Math.min(TaxonomicElement.MAX_SCORE, score));
    }

    /**
     * Adjust a score.
     * <p>
     * Any parent provider adjustments are applied, followed
     * by any local adjustments.
     * </p>
     *
     * @param score The current score
     * @param instance The taxon instances
     *
     * @return The adjusted score
     */
    public int adjustScore(int score, TaxonConceptInstance instance) {
        if (this.parent != null)
            score = this.parent.adjustScore(score, instance);
        return this.adjuster == null ? score : this.adjuster.score(score, instance, instance.getContainer() == null ? null : instance.getContainer().getKey());
    }

    /**
     * Adjust a name key.
     * <p>
     * Any parent provider adjustments are applied, followed by any
     * local adjustments.
     * </p>
     *
     * @param key The key to adjust
     * @param instance The taxon instance
     *
     * @return The adjusted key
     */
    public NameKey adjustKey(NameKey key, TaxonConceptInstance instance) {
        if (this.parent != null)
            key = this.parent.adjustKey(key, instance);
        return this.keyAdjuster == null ? key : this.keyAdjuster.adjustKey(key, instance);
    }

    /**
     * Correct the spelling of scientific names that have clashes with other names in other providers for key management.
     * <p>
     * This is a short way of encoding key adjustments, since fixing mispellings etc. is often onerous.
     * As always parent corrections are also tried if there isn't a local correction.
     * Only exact matches will be corrected.
     * </p>
     * @param scientificName The source name
     *                       
     * @return The corrected name or the original name if no corrections are needed.
     */
    public String correctScientificName(String scientificName) {
        String name = this.scientificNameChanges.get(scientificName);
        if (name == null && this.parent != null)
            name = this.parent.correctScientificName(scientificName);
        return name == null ? scientificName : name;
    }

    /**
     * Correct the spelling of scientific names that have clashes with other names in other providers for key management.
     * <p>
     * This is a short way of encoding key adjustments, since fixing mispellings etc. is often difficult.
     * As always parent corrections are also tried if there isn't a local correction.
     * Only exact matches will be corrected.
     * </p>
     * @param scientificNameAuthorship The source author
     *
     * @return The corrected name or the original name if no corrections are needed.
     */
    public String correctScientificNameAuthorship(String scientificNameAuthorship) {
        String author = this.scientificNameAuthorshipChanges.get(scientificNameAuthorship);
        if (author == null && this.parent != null)
            author = this.parent.correctScientificName(scientificNameAuthorship);
        return author == null ? scientificNameAuthorship : author;
    }

    /**
     * Validate the name provider.
     *
     * @param taxonomy The taxonomy
     *
     * @return True if the provider is valid, false otherwise.
     */
    public boolean validate(Taxonomy taxonomy) {
        try {
            String utid = this.getUnknownTaxonID();
            if (taxonomy != null && taxonomy.getInstance(utid) == null) {
                taxonomy.report(IssueType.ERROR, "provider.validation.unknownTaxonID.notFound", utid, null);
                return false;
            }
        } catch (IndexBuilderException ex) {
            taxonomy.report(IssueType.ERROR, "provider.validation.unknownTaxonID.noID", null, null);
            return false;
        }
        return true;
    }

    /**
     * Get a citation for this provider.
     *
     * @return The citation
     */
    @JsonIgnore
    public Citation getCitation() throws IndexBuilderException {
        StringBuilder sb = new StringBuilder();
        if (this.getName() != null) {
            sb.append(this.getName());
        }
        if (this.getRightsHolder() != null) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(this.getRightsHolder());
        }
        return new Citation(sb.toString(), this.getId());
    }


    /**
     * Get a map of terms suitable for writing to an attribution extension.
     *
     * @return The map of terms.
     */
    @JsonIgnore
    public Map<Term,String> getProviderMap() {
        Map<Term, String> map = new LinkedHashMap<>();
        map.put(DwcTerm.datasetID, this.getId());
        map.put(DwcTerm.datasetName, this.getName());
        map.put(DcTerm.description, this.getDescription());
        map.put(DcTerm.rightsHolder, this.getRightsHolder());
        map.put(DcTerm.license, this.getLicence());
        return map;
    }

    /**
     * Get the cutoff level for distribution information.
     * <p>
     * Distributions for any rank above this one are ignored.
     * </p>
     */
    @JsonIgnore
    public RankType getDistributionCutoff() {
        if (this.distributionCutoff != null)
            return this.distributionCutoff;
        if (this.parent != null)
            return this.parent.getDistributionCutoff();
        return null;
    }

    /**
     * Find the default parent for an instance.
     *
     * @param taxonomy The base taxonomy
     *
     * @return The default parent, or null if not present or found.
     *
     * @throws IndexBuilderException if unable to detect a parent
     */
    public TaxonomicElement findDefaultParent(Taxonomy taxonomy, TaxonConceptInstance instance) throws IndexBuilderException {
        NomenclaturalClassifier code = instance.getCode();
        String dp = this.getDefaultParentTaxon();

        if (code == null || dp == null)
            return null;

        return taxonomy.findElement(code, dp, this, null);
    }
}
