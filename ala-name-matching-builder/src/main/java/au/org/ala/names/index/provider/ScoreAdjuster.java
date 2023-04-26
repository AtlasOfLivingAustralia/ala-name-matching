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

package au.org.ala.names.index.provider;

import au.org.ala.names.index.NameKey;
import au.org.ala.names.index.NameProvider;
import au.org.ala.names.index.TaxonConceptInstance;
import au.org.ala.names.index.VernacularName;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * A score adjustment for applying to a specific taxonomic element.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class ScoreAdjuster {
    @JsonProperty
    private List<TaxonCondition> forbidden;
    @JsonProperty
    private List<ScoreAdjustment> adjustments;

    public ScoreAdjuster() {
        this.forbidden = new ArrayList<>();
        this.adjustments = new ArrayList<>();
    }

    public void addForbidden(TaxonCondition condition) {
        this.forbidden.add(condition);
    }

    public void addAdjustment(ScoreAdjustment adjustment) {
        this.adjustments.add(adjustment);
    }

    /**
     * Is this instance forbidden?
     *
     * @param instance The instance
     * @param key The associated name key
     * @param provider The provider for match context
     *
     * @return An explanation for forbdding this instance or null for not forbidden
     */
    public String forbid(TaxonConceptInstance instance, NameKey key, NameProvider provider) {
        for (TaxonCondition condition: this.forbidden) {
            if (condition.match(instance, key, provider))
                return condition.explain();
        }
        return null;
    }

    /**
     * Compute an adjuested score for an instance.
     *
     * @param base The base score
     * @param instance The taxon instance
     * @param key The name key
     * @param provider The provider for match context
     *
     * @return The adjusted score
     */
    public int score(int base, TaxonConceptInstance instance, NameKey key, NameProvider provider) {
        return this.adjustments.stream().reduce(base, (score, adjuster) -> adjuster.adjust(score, instance, key, provider), (a, b) -> a);
    }


    /**
     * Is this vernacular name forbidden?
     *
     * @param name The vernacular name
     * @param provider The provider for match context
     *
     * @return An explanation for forbdding this instance or null for not forbidden
     */
    public String forbid(VernacularName name, NameProvider provider) {
        for (TaxonCondition condition: this.forbidden) {
            if (condition.match(name, provider))
                return condition.explain();
        }
        return null;
    }

    /**
     * Compute an adjuested score for an instance.
     *
     * @param base The base score
     * @param name The vernacular name
     * @param provider The provider for match context
     *
     * @return The adjusted score
     */
    public int score(int base, VernacularName name, NameProvider provider) {
        return this.adjustments.stream().reduce(base, (score, adjuster) -> adjuster.adjust(score, name, provider), (a, b) -> a);
    }

}
