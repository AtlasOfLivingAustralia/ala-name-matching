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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.checklist.lucene.SearchResultException;
import au.org.ala.checklist.lucene.model.NameSearchResult;
import au.org.ala.data.model.LinnaeanRankClassification;
import au.org.ala.data.util.RankType;
import au.org.ala.sds.dao.SensitiveSpeciesDao;
import au.org.ala.sds.model.SensitiveTaxon.Rank;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */

public class SensitiveTaxonStore {

    protected static final Logger logger = Logger.getLogger(SensitiveTaxonStore.class);

    private final List<SensitiveTaxon> taxon;

    private final Map<String, Integer> lsidMap;
    private final Map<String, Integer> nameMap;

    private final CBIndexSearch cbIndexSearcher;

    public SensitiveTaxonStore(SensitiveSpeciesDao dao, CBIndexSearch cbIndexSearcher) throws Exception {
        this.cbIndexSearcher = cbIndexSearcher;
        this.lsidMap = new HashMap<String, Integer>();
        this.nameMap = new HashMap<String, Integer>();
        this.taxon = dao.getAll();
        verifyAndInitialiseSpeciesList();
    }

    private void verifyAndInitialiseSpeciesList() {
        for (int index = 0; index < taxon.size(); index++) {
            SensitiveTaxon st = taxon.get(index);
            NameSearchResult match = getAcceptedName(st);
            if (match != null) {
                String acceptedName = match.getRankClassification().getScientificName();
                String lsid = match.getLsid();
                if (!st.getTaxonName().equalsIgnoreCase(acceptedName)) {
                    logger.info("Sensitive species '" + st.getTaxonName() + "' is not accepted name - using '" + acceptedName + "'");
                    st.setAcceptedName(acceptedName);
                }
                logger.debug("'" + st.getTaxonName() + "' ('" + acceptedName + "')\t'" + lsid + "'");
                nameMap.put(acceptedName, index);
                st.setLsid(lsid);
                lsidMap.put(lsid, index);
            } else {
                logger.warn("Sensitive species '" + st.getTaxonName() + "' not found in NameMatching index");
            }
        }
    }

    public SensitiveTaxon findByName(String name) {
        String acceptedName = name;
        NameSearchResult result = getAcceptedName(name);
        if (result != null) {
            acceptedName = result.getRankClassification().getScientificName();
        }

        Integer index = nameMap.get(acceptedName);
        if (index != null) {
            return taxon.get(index);
        } else {
            // Try binary search
            int idx = Collections.binarySearch(taxon, new SensitiveTaxon(name, StringUtils.contains(name, ' ') ? Rank.SPECIES : Rank.GENUS));
            if (idx >= 0 && taxon.get(idx).getTaxonName().equalsIgnoreCase(name)) {
                return taxon.get(idx);
            } else {
                return null;
            }
        }
    }

    public SensitiveTaxon findByAcceptedName(String acceptedName) {
        Integer index = nameMap.get(acceptedName);
        if (index != null) {
            return taxon.get(index);
        } else {
            return null;
        }
    }

    public SensitiveTaxon findByLsid(String lsid) {
        Integer index = lsidMap.get(lsid);
        if (index != null) {
            return taxon.get(index);
        } else {
            return null;
        }
    }

    private NameSearchResult getAcceptedName(String name) {
        NameSearchResult match = null;
        try {
            match = cbIndexSearcher.searchForRecord(stripTaxonTokens(name), null);
            if (match != null) {
                if (match.isSynonym()) {
                    match = cbIndexSearcher.searchForRecordByID(Long.toString(match.getAcceptedId()));
                }
            }
        } catch (SearchResultException e) {
            logger.debug("'" + name + "' - " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("'" + name + "'", e);
        }

        return match;
    }

    private NameSearchResult getAcceptedName(SensitiveTaxon st) {
        String name = null;
        NameSearchResult match = null;
        try {
            name = stripTaxonTokens(st.getTaxonName());
            LinnaeanRankClassification cl = new LinnaeanRankClassification(null, null, null, null, st.getFamily().equals("") ? null : st.getFamily() , null, name);
            match = cbIndexSearcher.searchForRecord(name, cl, StringUtils.contains(name, ' ') ? null : RankType.GENUS);
            if (match != null) {
                if (match.isSynonym()) {
                    match = cbIndexSearcher.searchForRecordByID(Long.toString(match.getAcceptedId()));
                }
            }
        } catch (SearchResultException e) {
            logger.debug("'" + name + "' - " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("'" + name + "'", e);
        }

        return match;
    }

    protected static String stripTaxonTokens(String name) {
//        String stripped  = name.replaceAll(" subsp\\. ", " ");
//        stripped = stripped.replaceAll(" var\\. ", " ");
        String stripped = name.replaceAll(" ms$", "");
        return stripped;
    }
}
