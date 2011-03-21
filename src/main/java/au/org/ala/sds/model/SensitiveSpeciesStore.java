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
package au.org.ala.sds.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.checklist.lucene.SearchResultException;
import au.org.ala.checklist.lucene.model.NameSearchResult;
import au.org.ala.data.util.RankType;
import au.org.ala.sds.dao.SensitiveSpeciesDao;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */

public class SensitiveSpeciesStore {

    protected static final Logger logger = Logger.getLogger(SensitiveSpeciesStore.class);

    private final List<SensitiveSpecies> species;

    private final Map<String, Integer> lsidMap;
    private final Map<String, Integer> nameMap;

    private final CBIndexSearch cbIndexSearcher;

    public SensitiveSpeciesStore(SensitiveSpeciesDao dao, CBIndexSearch cbIndexSearcher) throws Exception {
        this.cbIndexSearcher = cbIndexSearcher;
        this.lsidMap = new HashMap<String, Integer>();
        this.nameMap = new HashMap<String, Integer>();
        this.species = dao.getAll();
        verifyAndInitialiseSpeciesList();
    }

    private void verifyAndInitialiseSpeciesList() {
        for (int index = 0; index < species.size(); index++) {
            SensitiveSpecies ss = species.get(index);
            NameSearchResult match = getAcceptedName(ss.getScientificName());
            if (match != null) {
                String acceptedName = match.getRankClassification().getScientificName();
                String lsid = match.getLsid();
                if (!ss.getScientificName().equalsIgnoreCase(acceptedName)) {
                    logger.info("Sensitive species '" + ss.getScientificName() + "' is not accepted name - using '" + acceptedName + "'");
                    ss.setAcceptedName(acceptedName);
                }
                logger.debug("'" + ss.getScientificName() + "' ('" + acceptedName + "')\t'" + lsid + "'");
                nameMap.put(acceptedName, index);
                ss.setLsid(lsid);
                lsidMap.put(lsid, index);
            } else {
                logger.warn("Sensitive species '" + ss.getScientificName() + "' not found in NameMatching index");
            }
        }
    }


    public SensitiveSpecies findByName(String scientificName) {
        String acceptedName = scientificName;
        NameSearchResult result = getAcceptedName(scientificName);
        if (result != null) {
            acceptedName = result.getRankClassification().getScientificName();
        }

        Integer index = nameMap.get(acceptedName);
        if (index != null) {
            return species.get(index);
        } else {
            // Try binary search
            int idx = Collections.binarySearch(species, new SensitiveSpecies(scientificName));

            return null;
        }
    }

    public SensitiveSpecies findByAcceptedName(String acceptedName) {
        Integer index = nameMap.get(acceptedName);
        if (index != null) {
            return species.get(index);
        } else {
            return null;
        }
    }

    public SensitiveSpecies findByLsid(String lsid) {
        Integer index = lsidMap.get(lsid);
        if (index != null) {
            return species.get(index);
        } else {
            return null;
        }
    }

    private NameSearchResult getAcceptedName(String name) {
        NameSearchResult match = null;
        try {
            name = stripTaxonTokens(name);
            match = cbIndexSearcher.searchForRecord(name, null);
            if (match != null) {
                if (match.isSynonym()) {
                    match = cbIndexSearcher.searchForRecordByID(Long.toString(match.getAcceptedId()));
                }
                if (match != null) {
                    Set<RankType> speciesTypes = EnumSet.of(RankType.GENUS);
                    if (speciesTypes.contains(match.getRank())) {
                        logger.warn("Sensitive species '" + name + "' returns '" + match.getRankClassification().getScientificName() + "' rank " + match.getRank().name());
                        match = null;
                    }
                }
            }
        } catch (SearchResultException e) {
            logger.warn("'" + name + "' - " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("'" + name + "'", e);
        }

        return match;
    }

    protected static String stripTaxonTokens(String name) {
        String stripped  = name.replaceAll(" subsp\\. ", " ");
        stripped = stripped.replaceAll(" var\\. ", " ");
        stripped = stripped.replaceAll(" ms$", "");
        return stripped;
    }
}
