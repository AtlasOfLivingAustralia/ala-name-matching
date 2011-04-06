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
package au.org.ala.sds.dto;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitiveSpeciesDto {
    private int id;
    private String scientificName;
    private String commonName;
    private String family;
    private String sensitivityZone;
    private String authority;
    private String fromDate;
    private String toDate;
    private String locationGeneralisation;
    private String sensitivityCategory;

    public SensitiveSpeciesDto() {
        super();
        // TODO Auto-generated constructor stub
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getSensitivityZone() {
        return sensitivityZone;
    }

    public void setSensitivityZone(String sensitivityZone) {
        this.sensitivityZone = sensitivityZone;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public String getLocationGeneralisation() {
        return locationGeneralisation;
    }

    public void setLocationGeneralisation(String locationGeneralisation) {
        this.locationGeneralisation = locationGeneralisation;
    }

    public String getSensitivityCategory() {
        return sensitivityCategory;
    }

    public void setSensitivityCategory(String sensitivityCategory) {
        this.sensitivityCategory = sensitivityCategory;
    }

}
