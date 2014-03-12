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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import au.org.ala.names.search.ALANameSearcher;
import au.org.ala.names.search.SearchResultException;
import au.org.ala.names.model.NameSearchResult;
import au.org.ala.names.model.LinnaeanRankClassification;
import au.org.ala.names.model.RankType;
import au.org.ala.sds.dao.SensitiveSpeciesDao;
import au.org.ala.sds.model.SensitiveTaxon.Rank;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */

public class SensitiveTaxonStore implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final Logger logger = Logger.getLogger(SensitiveTaxonStore.class);

    private final List<SensitiveTaxon> taxonList;

    private final Map<String, Integer> lsidMap;
    private final Map<String, Integer> nameMap;

    private transient final ALANameSearcher namesSearcher;

    public SensitiveTaxonStore(SensitiveSpeciesDao dao, ALANameSearcher nameSearcher) throws Exception {
        this.namesSearcher = nameSearcher;
        this.lsidMap = new HashMap<String, Integer>();
        this.nameMap = new HashMap<String, Integer>();
        this.taxonList = dao.getAll();
        verifyAndInitialiseSpeciesList();
    }

    private void verifyAndInitialiseSpeciesList() {
        List<SensitiveTaxon> additionalAcceptedTaxons = new ArrayList<SensitiveTaxon>();

        for (SensitiveTaxon st : taxonList) {
            NameSearchResult match = lookupName(st);
            if (match != null) {
                st.setLsid(match.getLsid());
                if (match.isSynonym()) {
                    NameSearchResult accepted = getAcceptedNameFromSynonym(match);
                    if (accepted != null) {
                        String acceptedName = accepted.getRankClassification().getScientificName();
                        logger.info("Sensitive species '" + st.getName() + "' is not accepted name - using '" + acceptedName + "'");
                        SensitiveTaxon acceptedTaxon = findByExactMatch(acceptedName);
                        if (acceptedTaxon == null) {
                            acceptedTaxon = new SensitiveTaxon(acceptedName, StringUtils.contains(acceptedName, ' ') ? Rank.SPECIES : Rank.GENUS);
                            acceptedTaxon.setLsid(accepted.getLsid());
                            if (!additionalAcceptedTaxons.contains(acceptedTaxon)) {
                                additionalAcceptedTaxons.add(acceptedTaxon);
                                logger.info("Accepted name '" + acceptedName + "' (" + acceptedTaxon.getLsid() + ") added to sensitive taxon list");
                            }
                        }
                        st.setAcceptedName(acceptedName);
                    }
                }
                logger.debug(st.getName() + (st.getAcceptedName() == null ? "" : " (" + st.getAcceptedName() + ")") + "\t" + st.getLsid());
            } else {
                logger.warn("Sensitive species '" + st.getName() + "' not found in NameMatching index");
            }
        }

        // Add additional accepted sensitive taxons
        taxonList.addAll(additionalAcceptedTaxons);
        Collections.sort(taxonList);

        // Construct lookup maps and deal with synonym sensitivity instances
        for (int i = 0; i < this.taxonList.size(); i++) {
            SensitiveTaxon st = taxonList.get(i);
            String lsid = st.getLsid();
            if (StringUtils.isNotBlank(lsid)) {
                lsidMap.put(st.getLsid(), i);
            }
            if (st.getAcceptedName() != null) {
                SensitiveTaxon acceptedTaxon = findByExactMatch(st.getAcceptedName());
                if (acceptedTaxon != null) {
                    for (SensitivityInstance si : st.getInstances()) {
                        if (!acceptedTaxon.getInstances().contains(si)) {
                            acceptedTaxon.getInstances().add(si);
                        }
                    }
                    st.setAcceptedTaxon(acceptedTaxon);
                } else {
                    logger.error("Accepted taxon '" + st.getAcceptedName() + "' not found in taxon list");
                }
            } else {
                if (StringUtils.isNotBlank(lsid)) {
                    nameMap.put(st.getName(), i);
                    logger.debug("Added '" + st.getName() + "' to nameMap");
                }
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
            return taxonList.get(index);
        } else {
            // Try binary search
            return findByExactMatch(name);
        }
    }

    public SensitiveTaxon findByAcceptedName(String acceptedName) {
        Integer index = nameMap.get(acceptedName);
        if (index != null) {
            return taxonList.get(index);
        } else {
            return null;
        }
    }

    public SensitiveTaxon findByLsid(String lsid) {
        Integer index = lsidMap.get(lsid);
        if (index != null) {
            return taxonList.get(index);
        } else {
            return null;
        }
    }

    public SensitiveTaxon findByExactMatch(String name) {
        // Do binary search
        int idx = Collections.binarySearch(taxonList, new SensitiveTaxon(name, StringUtils.contains(name, ' ') ? Rank.SPECIES : Rank.GENUS));
        if (idx >= 0 && taxonList.get(idx).getName().equalsIgnoreCase(name)) {
            return taxonList.get(idx);
        } else {
            return null;
        }
    }

    private NameSearchResult getAcceptedName(String name) {
        NameSearchResult match = null;
        if (namesSearcher != null) {
            try {
                match = namesSearcher.searchForRecord(name, null);
                if (match != null && match.isSynonym()) {
                    match = getAcceptedNameFromSynonym(match);
                }
            } catch (SearchResultException e) {
                logger.debug("'" + name + "' - " + e.getMessage());
            } catch (RuntimeException e) {
                logger.error("'" + name + "'", e);
            }
        }

        return match;
    }

    private NameSearchResult lookupName(SensitiveTaxon st) {
        String name = null;
        NameSearchResult match = null;
        if (namesSearcher != null) {
            try {
                name = st.getTaxonName();
                LinnaeanRankClassification lrc = new LinnaeanRankClassification(null, null, null, null, st.getFamily().equals("") ? null : st.getFamily() , null, name);
                match = namesSearcher.searchForRecord(name, lrc, null);
            } catch (SearchResultException e) {
                logger.debug("'" + name + "' - " + e.getMessage());
            } catch (RuntimeException e) {
                logger.error("'" + name + "'", e);
            }
        }

        return match;
    }

    private NameSearchResult getAcceptedNameFromSynonym(NameSearchResult match) {
        NameSearchResult accepted;
        if (match.isSynonym()) {
            accepted = namesSearcher.searchForRecordByLsid(match.getAcceptedLsid());
            if (accepted == null) {
                logger.error("Could not find accepted name for synonym '" + match.getCleanName() + "'");
            }
            return accepted;
        } else {
            return match;
        }
    }

}
