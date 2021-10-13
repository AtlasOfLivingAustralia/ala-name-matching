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
 * Adjust name keys, based on specifiec properties.
 *
 * @see KeyAdjustment
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class KeyAdjuster {
    @JsonProperty
    private List<KeyAdjustment> adjustments;

    public KeyAdjuster() {
        this.adjustments = new ArrayList<>();
    }

    public void addAdjustment(KeyAdjustment adjustment) {
        this.adjustments.add(adjustment);
    }

    public NameKey adjustKey(NameKey base, TaxonConceptInstance instance) {
        return this.adjustments.stream().reduce(base, (key, adjuster) -> adjuster.adjust(key, instance), (a, b) -> a);
    }
}
