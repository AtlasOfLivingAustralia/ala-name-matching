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

/**
 * Groupings of taxonomic types
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public enum TaxonomicTypeGroup {
    ACCEPTED,
    SYNONYM,
    MISAPPLIED,
    EXCLUDED,
    MISCELLANEOUS,
    INCERTAE_SEDIS,
    SPECIES_INQUIRENDA,
    UNPLACED,
    DOUBTFUL,
    INVALID
}
