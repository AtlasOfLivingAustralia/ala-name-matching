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
 * POJO representing a data resource region
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class GeoRegionDataResource {

    private GeoRegion geoRegion;
    
    private DataResource  dataResource;

    private Long occurrenceCount;

    private Long OccurrenceCoordinateCount;

    private BasisOfRecord basisOfRecord;

    /*
     * Getter & Setters
     */

    public DataResource  getDataResource() {
        return dataResource;
    }

    public void setDataResource(DataResource dataResource) {
        this.dataResource = dataResource;
    }

    public GeoRegion getGeoRegion() {
        return geoRegion;
    }

    public void setGeoRegion(GeoRegion geoRegion) {
        this.geoRegion = geoRegion;
    }

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

    public Long getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(Long occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }
    
}


