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

package au.org.ala.names.model;

import org.apache.commons.lang3.StringUtils;
import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.SmithWatermanGotoh;

/**
 * Measures of how well a result matches a set of criteria.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2018 Atlas of Living Australia
 */
public class MatchMetrics implements Comparable<MatchMetrics> {
    /** How close two matches need to be to decide that they're roughly equivalent matches */
    public static final float MATCH_PROXIMITY = 0.1f;
    /** The default priority score to use when one is not available */
    public static final int DEFAULT_PRIORITY = 1000;
    /** The default match level */
    public static final float DEFAULT_MATCH = 1.0f;
    /** Weights for match terms for things we expect to have a higher taxonomy */
    final float[] WEIGHTS = new float[] { 4.0f, 1.0f, 1.0f, 1.0f, 1.5f, 2.0f, 1.0f, 1.0f, 5.0f, 0.5f };
    /** Weights for match terms for things we don't expect to have a higher taxonomy or, possibly, a rank */
    final float[] SYNONYM_WEIGHTS = new float[] { 2.0f, 1.0f, 1.0f, 1.0f, 1.5f, 0.5f, 1.0f, 1.0f, 5.0f, 0.5f };


    /** The taxon priority */
    private int priority;
    /** The match metric. 1.0 is a perfect match */
    private float match;

    /**
     * Construct an initial match metric
      */
    public MatchMetrics() {
        this.priority = DEFAULT_PRIORITY;
        this.match = DEFAULT_MATCH;
    }

    /**
     * Get the taxon priority.
     * <p>
     * This is the score given during index merging and can be used to choose between
     * equivalent matches.
     * The priority is usually from 1000-6000 but is not limited to those values.
     * </p>
     *
     * @return The priority
     *
     * @see au.org.ala.names.index.TaxonConceptInstance#getScore()
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Set the match priority
     *
     * @param priority The new priority
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * The match level.
     * <p>
     * This is a measure of how well a taxon matches the supplied information.
     * 0.0 means no match.
     * 1.0 is a perfect match.
     * </p>
     * @return
     */
    public float getMatch() {
        return match;
    }

    /**
     * Set the match level.
     *
     * @param match The new match level
     */
    public void setMatch(float match) {
        this.match = match;
    }

    /**
     * Compute a match of how close a classification matches a query.
     * <p>
     * The match level is set to the resulting calculation
     * </p>
     *
     * @param query The query classificartion
     * @param result The result classification
     * @param synonym The result is a synonym, which makes higher order matches difficult
     *
     * @return A measure of closeness of match.
     */
    public void computeMatch(LinnaeanRankClassification query, LinnaeanRankClassification result, boolean synonym) {
        float[] weightVector = synonym ? SYNONYM_WEIGHTS : WEIGHTS;
        Float[] matchVector = new Float[10];
        AbstractStringMetric similarity = new SmithWatermanGotoh();
        float weight = synonym ? 1.0f : 0.1f; // Inital scoo
        float score = weight;

        matchVector[0] = this.compareTerm(query.kingdom, result.kingdom, true, similarity);
        matchVector[1] = this.compareTerm(query.phylum, result.phylum, false, similarity);
        matchVector[2] = this.compareTerm(query.klass, result.klass, false, similarity);
        matchVector[3] = this.compareTerm(query.order, result.order, false, similarity);
        matchVector[4] = this.compareTerm(query.family, result.family, false, similarity);
        matchVector[5] = this.compareTerm(query.genus, result.genus, false, similarity);
        matchVector[6] = this.compareTerm(query.specificEpithet, result.specificEpithet, false, similarity);
        matchVector[7] = this.compareTerm(query.infraspecificEpithet, result.infraspecificEpithet, false, similarity);
        // We assume that scientificName matches, otherwise, why bother?
        matchVector[8] = this.compareTerm(query.authorship, result.authorship, false, similarity);
        if (StringUtils.isNotEmpty(query.rank) && StringUtils.isNotEmpty(result.rank)) {
            RankType r1 = RankType.getForStrRank(query.rank);
            RankType r2 = RankType.getForStrRank(result.rank);
            if (!r1.isLoose() && !r2.isLoose()) {
                // Allow some slop-over
                matchVector[9] = Math.max(0.01f, 1.0f - (0.8f * Math.abs(r1.getId() - r2.getId())) / (RankType.PHYLUM.getId() - RankType.KINGDOM.getId()));
            }
        }
        for (int i = 0; i < matchVector.length; i++) {
            if (matchVector[i] != null) {
                weight += weightVector[i];
                score += matchVector[i] * weightVector[i];
            }
        }
        this.match = score / weight;
    }

    /**
     * Compare an expected term against an actual term.
     *
     * @param expected The expected term
     * @param actual The actual term
     * @param required This term is required
     * @param similarity The similarity calculator to use
     *
     * @return Either null for a non-matchable term or a similarity metric from 0.0-1.0
     */
    private Float compareTerm(String expected, String actual, boolean required, AbstractStringMetric similarity) {
        if (StringUtils.isEmpty(expected))
            return null;
        if (StringUtils.isEmpty(actual))
            return required ? 0.01f : 0.5f;
        if (expected.equalsIgnoreCase(actual))
            return 1.0f;
        return similarity.getSimilarity(expected.toUpperCase(), actual.toUpperCase());
    }


    /**
     * Equality test.
     *
     * @param o The other object
     *
     * @return True if the priority and match are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MatchMetrics that = (MatchMetrics) o;

        if (priority != that.priority) return false;
        return Float.compare(that.match, match) == 0;
    }

    /**
     * Compute a hash code.
     *
     * @return The hash code computed from the priority and match level.
     */
    @Override
    public int hashCode() {
        int result = priority;
        result = 31 * result + (match != +0.0f ? Float.floatToIntBits(match) : 0);
        return result;
    }

    /**
     * Compare two metrics.
     * <p>
     * The the match scores are 'close enough', meaning within {@link #MATCH_PROXIMITY}
     * then the priorities are compared.
     * Otherwise, compare match levels.
     * </p>
     *
     * @param metric The other metric
     * @return
     */
    @Override
    public int compareTo(MatchMetrics metric) {
        if (Math.abs(this.match - metric.match) > MATCH_PROXIMITY) {
            return this.match < metric.match ? -10000 : 10000;
        }
        return this.priority - metric.priority;
    }
}
