/*
 * Copyright (C) 2013 Atlas of Living Australia
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
package au.org.ala.sds.model;

import au.org.ala.sds.util.DateHelper;

import java.util.Date;

/**
 * A DTO that represents a species list.  It includes the common information for all items relating to the same list.
 *
 * @author Natasha Carter (natasha.carter@csiro.au)
 */
public class SDSSpeciesListDTO {
    private String dataResourceUid;
    private String title;
    private String region;
    private String authority;
    private String category;
    private String generalisation;
    private String sdsType;
    private Date dateUpdated;

    public SDSSpeciesListDTO(){

    }
    public SDSSpeciesListDTO(String dataResourceUid, String title, String region, String authority,String category, String generalisation, String sdsType, String lastUpdated){
        this.dataResourceUid = dataResourceUid;
        this.title = title;
        this.region = region;
        this.authority = authority;
        this.category = category;
        this.generalisation = generalisation;
        this.sdsType = sdsType;
        this.dateUpdated = DateHelper.parseDate(lastUpdated);
    }

    @Override
    public String toString() {
        return "SDSSpeciesListDTO{" +
                "dataResourceUid='" + dataResourceUid + '\'' +
                ", title='" + title + '\'' +
                ", region='" + region + '\'' +
                ", authority='" + authority + '\'' +
                ", category='" + category + '\'' +
                ", generalisation='" + generalisation + '\'' +
                ", sdsType='" + sdsType + '\'' +
                '}';
    }

    public String getDataResourceUid() {
        return dataResourceUid;
    }

    public String getTitle() {
        return title;
    }

    public String getRegion() {
        return region;
    }

    public String getAuthority() {
        return authority;
    }

    public String getCategory() {
        return category;
    }

    public String getGeneralisation() {
        return generalisation;
    }

    public String getSdsType() {
        return sdsType;
    }

    public Date getDateUpdated() {
        return dateUpdated;
    }
}
