package au.org.ala.names.index;

import au.org.ala.names.index.provider.KeyAdjuster;
import au.org.ala.names.index.provider.ScoreAdjuster;
import com.fasterxml.jackson.annotation.*;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;

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
    private NomenclaturalCode defaultNomenclaturalCode;
    /** Is this a "loose" taxonomy, where we can expect fragments of taxonomy and we shouldn't worry too much about consistency */
    @JsonProperty
    private boolean loose;
    /** Is this an "external" name provider - something that can be referenced */
    @JsonProperty
    private boolean external;
    /** The method of discarding forbidden taxa */
    @JsonProperty
    private DiscardStrategy discardStrategy;

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
    }

    /**
     * Create a name source.
     *
     * @param id The source identifier
     * @param defaultScore The default source priority
     * @param scores Additional priority mappings
     */
    public NameProvider(String id, Integer defaultScore, Map<String, Integer> scores) {
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
    }

    /**
     * Create a simple name source.
     *
     * @param id The source identifier
     * @param defaultScore The default source priority
     */
    public NameProvider(String id, int defaultScore) {
        this(id, defaultScore, Collections.EMPTY_MAP);
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
     * Intended as a place to put useful human-readable desctiptions
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
     * Decide whether to forbid an instance.
     * <p>
     * If there is a parent provider, then check the parent, as well.
     * </p>
     *
     * @param instance The instance
     *
     * @return True if the instance is forbidden
     */
    public boolean forbid(TaxonConceptInstance instance) {
        if (this.parent != null && this.parent.forbid(instance))
            return true;
        return this.adjuster.forbid(instance);
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
    public NomenclaturalCode getDefaultNomenclaturalCode() {
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
        int score = p != null ? p.getBaseScore(original) : this.getDefaultScore();
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
        return this.adjuster == null ? score : this.adjuster.score(score, instance);
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
}
