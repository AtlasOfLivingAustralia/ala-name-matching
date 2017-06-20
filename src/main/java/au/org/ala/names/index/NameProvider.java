package au.org.ala.names.index;

import au.org.ala.names.index.provider.ScoreAdjuster;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

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
    /** The provider description */
    @JsonProperty
    private String description;
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

    /**
     * Default constructor
     */
    public NameProvider() {
        this.id = UUID.randomUUID().toString();
        this.description = null;
        this.parent = null;
        this.defaultScore = null;
        this.scores = new HashMap<>();
        this.owner = new HashSet<>();
        this.adjuster = new ScoreAdjuster();
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
        this.description = null;
        this.parent = null;
        this.defaultScore= defaultScore;
        this.scores = scores;
        this.owner = new HashSet<>();
        this.adjuster = new ScoreAdjuster();
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
     * Get the parent name provider
     *
     * @return
     */
    public NameProvider getParent() {
        return parent;
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
    public int getDefaultScore() {
        if (this.defaultScore != null)
            return this.defaultScore;
        if (this.parent != null)
            return this.parent.getDefaultScore();
        return DEFAULT_SCORE;
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
        return specific;
    }

    /**
     * Compute a base score for an instance.
     * <p>
     * Only accepted instances have a score, synonyms and the like have a score of 0.
     * </p>
     *
     * @param instance The instance
     *
     * @return The instance score
     */
    public int computeBaseScore(TaxonConceptInstance instance) {
        Integer specific = this.getSpecificScore(instance.getScientificName());
        if (specific != null)
            return specific;
        return instance.getParent() != null ? instance.getParent().getBaseScore() : this.getDefaultScore();
    }

    /**
     * Compute a score for an instance.
     * <p>
     * We start with the base score and apply adjustments based on the taxonomic status,
     * nomenclatural status, etc.
     * </p>
     *
     * @param instance The instance
     *
     * @return The instance score
     */
    public int computeScore(TaxonConceptInstance instance) {
        if (this.owns(instance.getScientificName()))
            return Integer.MAX_VALUE;
        if (instance.isForbidden())
            return Integer.MIN_VALUE;
        int score = instance.getBaseScore();
        if (this.parent != null)
            score = this.parent.adjuster.score(score, instance);
        score = this.adjuster.score(score, instance);
        return score;
    }
}
