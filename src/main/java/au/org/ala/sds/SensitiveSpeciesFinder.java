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

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.model.SensitiveTaxonStore;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
@Service
public class SensitiveSpeciesFinder implements Lookup {

    protected static final Logger logger = Logger.getLogger(SensitiveSpeciesFinder.class);
    private final SensitiveTaxonStore store;

    public SensitiveSpeciesFinder(SensitiveTaxonStore store) {
        this.store = store;
    }

    @Override
    public SensitiveTaxon findSensitiveSpecies(String scientificName) {
        return store.findByName(scientificName);
    }

    @Override
    public SensitiveTaxon findSensitiveSpeciesByAcceptedName(String acceptedName) {
        return store.findByAcceptedName(acceptedName);
    }

    @Override
    public SensitiveTaxon findSensitiveSpeciesByLsid(String lsid) {
        return store.findByLsid(lsid);
    }

    @Override
    public boolean isSensitive(String scientificName) {
        return store.findByName(scientificName) != null;
    }

}
