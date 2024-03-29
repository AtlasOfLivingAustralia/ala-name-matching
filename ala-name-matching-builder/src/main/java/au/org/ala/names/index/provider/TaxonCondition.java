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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A possible condition that a provider can apply to a taxon instance.
 * <p>
 * Conditions follow a polymorphic composition rule,
 * so JSON serialisation uses a @class property to identify the condition type.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
@JsonInclude(JsonInclude.Include.NON_NULL)
abstract public class TaxonCondition {
    /**
     * Does this condition match an instance?
     *
     * @param instance The instance to match
     * @param key The associated name key
     *
     * @return True on a match
     */
    abstract public boolean match(TaxonConceptInstance instance, NameKey key);

    /**
     * Provide a string explanation of the condition.
     *
     * @return A string describing the condition
     */
    abstract public String explain();
}
