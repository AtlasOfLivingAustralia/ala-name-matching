/*
 * Copyright (C) 2014 Atlas of Living Australia
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
 */

package au.org.ala.names.model;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A model to store the required information in a search result
 *
 * This includes the type of match that was used to get the result
 *
 * @author Natasha
 */
public class NameSearchResult {
    private String id;
    private String lsid;
    private String cleanName;
    private boolean isHomonym;
    private String acceptedLsid;
    //private long acceptedId = -1;
    private String kingdom;
    private String left, right;
    private LinnaeanRankClassification rankClass;
    private RankType rank;
    /** The type of match that was performed */
    private MatchType matchType;
    private SynonymType synonymType; //store that type of synonym that this name is
    /** The match quality */
    private MatchMetrics matchMetrics;

    public NameSearchResult(String id, String lsid, MatchType type) {
        this.id = id;//Long.parseLong(id);
        this.lsid = StringUtils.trimToNull(lsid) == null ? id : StringUtils.trimToNull(lsid);
        matchType = type;
        isHomonym = false;
        this.matchMetrics = new MatchMetrics();
    }

    /**
     * Construct a fully filled out result
     *
     * @param id The result identifier
     * @param lsid The lsid of the concept
     * @param acceptedLsid The lsid of the accepted concept
     * @param left The left-value
     * @param right The right-value
     * @param rankClass The linnaean classification
     * @param rank The rank
     * @param type The match type
     * @param synonymType The synonym type
     * @param priority An optional match priority
     */
    public NameSearchResult(String id, String lsid, String acceptedLsid, String left, String right, LinnaeanRankClassification rankClass, RankType rank, MatchType type, SynonymType synonymType, Integer priority) {
        this(id, lsid, type);
        this.acceptedLsid = acceptedLsid;
        this.left = left;
        this.right = right;
        this.rankClass = rankClass;
        this.rank = rank;
        this.synonymType = synonymType;
        if (priority != null)
            this.matchMetrics.setPriority(priority);
    }

    public SynonymType getSynonymType() {
        return synonymType;
    }

    /**
     *
     * @return The classification for the match
     */
    public LinnaeanRankClassification getRankClassification() {
        return rankClass;
    }

    /**
     *
     * @return
     * @deprecated Use the kingdom from the "getRankClassification"
     *
     */
    @Deprecated
    public String getKingdom() {
        return kingdom;
    }

    /**
     * Return the LSID for the result if it is not null otherwise return the id.
     *
     * @return
     */
    public String getLsid() {
        //if(lsid !=null || id <1)
        return lsid;
        //return Long.toString(id);
    }

    public String getId() {
        return id;
    }

    /**
     *
     * @return The match type used to get this result
     */
    public MatchType getMatchType() {
        return matchType;
    }

    /**
     * Set the match type that was used to get this result
     * @param type
     */
    public void setMatchType(MatchType type) {
        matchType = type;
    }

    @Deprecated
    public String getCleanName() {
        return cleanName;
    }
    @Deprecated
    public void setCleanName(String name) {
        cleanName = name;
    }
    @Deprecated
    public boolean hasBeenCleaned() {
        return cleanName != null;
    }

    public boolean isHomonym() {
        return isHomonym;
    }

    public boolean isSynonym() {
        return acceptedLsid != null;// || acceptedId >0;
    }

    /**
     * When the LSID for the synonym is null return the ID for the synonym
     *
     * @return
     * @deprecated Use {@link #getAcceptedLsid()} instead
     */
    @Deprecated
    public String getSynonymLsid() {
        return getAcceptedLsid();
    }

    /**
     * @return The accepted LSID for this name.  When the
     * name is not a synonym null is returned
     */
    public String getAcceptedLsid() {
        //if(acceptedLsid != null || acceptedId<1)
        return acceptedLsid;
//        else
//            return Long.toString(acceptedId);
    }

    @Override
    public String toString() {
        return "Match: " + matchType + " id: " + id + " lsid: " + lsid + " classification: " + rankClass + " synonym: " + acceptedLsid + " rank: " + rank;
    }

    public Map<String,String> toMap() {
        Map<String,String> map = new LinkedHashMap<String, String>();
        map.put("ID", id);
        map.put("GUID", lsid);
        if(rankClass !=null) {
            map.put("Classification", rankClass.toCSV(','));
            map.put("Scientific name", rankClass.getScientificName());
            map.put("Authorship", rankClass.getAuthorship());
        }
        if(rank !=null) {
            map.put("Rank", rank.toString());
        }
        map.put("Synonym", acceptedLsid);
        if(matchType !=null) {
            map.put("Match type", matchType.toString());
        }
        return map;
    }

    public RankType getRank() {
        return rank;
    }

    public void setRank(RankType rank) {
        this.rank = rank;
    }

    public String getLeft() {
        return left;
    }

    public void setLeft(String left) {
        this.left = left;
    }

    public String getRight() {
        return right;
    }

    public void setRight(String right) {
        this.right = right;
    }

    /**
     * Get the match metrics for this result
     *
     * @return The metrics
     */
    public MatchMetrics getMatchMetrics() {
        return matchMetrics;
    }

    /**
     * Compute the match metrics for this result against the query.
     *
     * @param query The query. If null, no metrics are computed
     */
    public void computeMatch(LinnaeanRankClassification query) {
        if (query == null)
            return;
        this.matchMetrics.computeMatch(query, this.rankClass, this.synonymType != null);
    }
}
