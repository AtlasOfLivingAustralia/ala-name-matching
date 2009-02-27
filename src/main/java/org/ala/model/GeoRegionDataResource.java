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
 * (DTO) POJO representing a data resource region
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class GeoRegionDataResource {

    protected String geoRegionName;

    protected Long geoRegionId;
    
    protected String dataResourceName;

    protected Long dataResourceId;

    protected Long occurrenceCount;

    protected Long OccurrenceCoordinateCount;

    protected BasisOfRecord basisOfRecord;

    /*
     * Getter & Setters
     */

    public Long getOccurrenceCoordinateCount() {
        return OccurrenceCoordinateCount;
    }

    public void setOccurrenceCoordinateCount(Long OccurrenceCoordinateCount) {
        this.OccurrenceCoordinateCount = OccurrenceCoordinateCount;
    }

    public BasisOfRecord getBasisOfRecord() {
        return basisOfRecord;
    }

    public void setBasisOfRecord(BasisOfRecord basisOfRecord) {
        this.basisOfRecord = basisOfRecord;
    }

    public Long getDataResourceId() {
        return dataResourceId;
    }

    public void setDataResourceId(Long dataResourceId) {
        this.dataResourceId = dataResourceId;
    }

    public String getDataResourceName() {
        return dataResourceName;
    }

    public void setDataResourceName(String dataResourceName) {
        this.dataResourceName = dataResourceName;
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

    public Long getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(Long occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }
    
}


