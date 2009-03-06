/* **************************************************************************
 * Copyright (C) 2009 Atlas of Living Australia
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
package org.ala.model;

import org.gbif.portal.model.occurrence.BasisOfRecord;

/**
 * (DTO) POJO representing a data resource taxon concept
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class GeoRegionTaxonConcept {

    protected String geoRegionName;

    protected Long geoRegionId;
    
    protected Long taxonConceptId;

    protected String taxonConceptName;

    protected String commonName;

    protected Integer rankId;

    protected String rankName;

    protected Long occurrenceCount;

    protected Long occurrenceCoordinateCount;

    protected BasisOfRecord basisOfRecord;

    /*
     * Getter & Setters
     */

    public BasisOfRecord getBasisOfRecord() {
        return basisOfRecord;
    }

    public void setBasisOfRecord(BasisOfRecord basisOfRecord) {
        this.basisOfRecord = basisOfRecord;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public Long getGeoRegionId() {
        return geoRegionId;
    }

    public void setGeoRegionId(Long geoRegionId) {
        this.geoRegionId = geoRegionId;
    }

    public String getGeoRegionName() {
        return geoRegionName;
    }

    public void setGeoRegionName(String geoRegionName) {
        this.geoRegionName = geoRegionName;
    }

    public Long getOccurrenceCoordinateCount() {
        return occurrenceCoordinateCount;
    }

    public void setOccurrenceCoordinateCount(Long occurrenceCoordinateCount) {
        this.occurrenceCoordinateCount = occurrenceCoordinateCount;
    }

    public Long getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(Long occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    public Integer getRankId() {
        return rankId;
    }

    public void setRankId(Integer rankId) {
        this.rankId = rankId;
    }

    public String getRankName() {
        return rankName;
    }

    public void setRankName(String rankName) {
        this.rankName = rankName;
    }

    public Long getTaxonConceptId() {
        return taxonConceptId;
    }

    public void setTaxonConceptId(Long taxonConceptId) {
        this.taxonConceptId = taxonConceptId;
    }

    public String getTaxonConceptName() {
        return taxonConceptName;
    }

    public void setTaxonConceptName(String taxonConceptName) {
        this.taxonConceptName = taxonConceptName;
    }
    
}


