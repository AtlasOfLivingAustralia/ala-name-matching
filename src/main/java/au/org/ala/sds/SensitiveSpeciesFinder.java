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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.checklist.lucene.SearchResultException;
import au.org.ala.checklist.lucene.model.NameSearchResult;
import au.org.ala.data.util.RankType;
import au.org.ala.sds.dao.SensitiveSpeciesDao;
import au.org.ala.sds.model.SensitiveSpecies;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitiveSpeciesFinder implements Lookup {

    protected static final Logger logger = Logger.getLogger(SensitiveSpeciesFinder.class);
    private SensitiveSpeciesDao dao;

    public SensitiveSpeciesFinder(SensitiveSpeciesDao dao) {
        this.dao = dao;
    }

    public void setDao(SensitiveSpeciesDao dao ) {
        this.dao = dao;
    }

    @Override
    public SensitiveSpecies findSensitiveSpecies(String scientificName) {
        return dao.findByName(scientificName);
    }

    @Override
    public SensitiveSpecies findSensitiveSpeciesByAcceptedName(String acceptedName) {
        return dao.findByAcceptedName(acceptedName);
    }

    @Override
    public SensitiveSpecies findSensitiveSpeciesByLsid(String lsid) {
        return dao.findByLsid(lsid);
    }

    @Override
    public boolean isSensitive(String scientificName) {
        return dao.findByName(scientificName) != null;
    }

    public void verifySensitiveSpecies(CBIndexSearch cbIdxSearcher) throws SearchResultException {
        List<SensitiveSpecies> speciesList = dao.getAll();
        Map<String, Integer> lsidMap = new HashMap<String, Integer>();
        for (int index = 0; index < speciesList.size(); index++) {
            SensitiveSpecies ss = speciesList.get(index);
            NameSearchResult match = cbIdxSearcher.searchForRecord(ss.getScientificName(), RankType.SPECIES);
            if (match != null) {
                String acceptedName = match.getRankClassification().getSpecies();
                String lsid = match.getLsid();
                if (!ss.getScientificName().equalsIgnoreCase(acceptedName)) {
                    logger.info("Sensitive species '" + ss.getScientificName() + "' is not accepted name - using '" + acceptedName + "'");
                } else {
                    logger.debug("'" + acceptedName + "'\t'" + lsid + "'");
                    ss.setLsid(lsid);
                    lsidMap.put(lsid, index);
                }
            } else {
                logger.warn("Sensitive species '" + ss.getScientificName() + "' not found in NameMatching index");
            }
        }
    }

}
