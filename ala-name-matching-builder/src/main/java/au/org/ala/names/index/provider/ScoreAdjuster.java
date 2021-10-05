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
import au.org.ala.names.index.TaxonConceptInstance;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * A score adjustment for applying to a specific
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
     *
     * @return An explanation for forbdding this instance or null for not forbidden
     */
    public String forbid(TaxonConceptInstance instance, NameKey key) {
        for (TaxonCondition condition: this.forbidden) {
            if (condition.match(instance, key))
                return condition.explain();
        }
        return null;
    }

    public int score(int base, TaxonConceptInstance instance, NameKey key) {
        return this.adjustments.stream().reduce(base, (score, adjuster) -> adjuster.adjust(score, instance, key), (a, b) -> a);
    }
}
