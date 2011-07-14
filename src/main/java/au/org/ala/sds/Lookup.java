/***************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 ***************************************************************************/
package au.org.ala.sds;

import au.org.ala.sds.model.SensitiveTaxon;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public interface Lookup {

    SensitiveTaxon findSensitiveSpecies(String scientificName);

    SensitiveTaxon findSensitiveSpeciesByAcceptedName(String acceptedName);

    SensitiveTaxon findSensitiveSpeciesByLsid(String lsid);

    SensitiveTaxon findSensitiveSpeciesByExactMatch(String scientificName);

    boolean isSensitive(String scientificName);
}
