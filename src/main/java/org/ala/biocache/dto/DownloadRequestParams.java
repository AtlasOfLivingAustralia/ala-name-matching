/* *************************************************************************
 *  Copyright (C) 2011 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/

package org.ala.biocache.dto;

import org.apache.commons.lang.StringUtils;

/**
 * Data Transfer Object to represent the request parameters required to download
 * the results of a search.
 *
 * @author "Natasha Carter <Natasha.Carter@csiro.au>"
 */
public class DownloadRequestParams extends SearchRequestParams {
    protected String email="";
    protected String reason ="";
    protected String file="data";
    /** CSV list of fields that should be downloaded.  If el or cl will need to map to appropriate column name */
    protected String fields="uuid,catalogNumber,taxonConceptID.p,scientificName,vernacularName,scientificName.p,taxonRank.p,vernacularName.p,kingdom.p,phylum.p,classs.p,order.p,family.p,genus.p,species.p,subspecies.p,institutionCode,collectionCode,decimalLatitude.p,decimalLongitude.p,coordinatePrecision,country.p,ibra.p,imcra.p,stateProvince.p,lga.p,minimumElevationInMeters,maximumElevationInMeters,minimumDepthInMeters,maximumDepthInMeters,year.p,month.p,day.p,eventDate.p,eventTime.p,basisOfRecord,sex,preparations";
    /** CSV list of extra fields to be added to the download - useful if wish to make use of default list */
    protected String extra="";

    /**
     * Custom toString method to produce a String to be used as the request parameters
     * for the Biocache Service webservices
     *
     * @return request parameters string
     */
    @Override
    public String toString() {
        StringBuilder req = new StringBuilder(super.toString());
        req.append("&email=").append(email);
        req.append("&reason=").append(reason);
        req.append("&file=").append(file);
        req.append("&fields=").append(fields);
        req.append("&extra=").append(extra);

        return req.toString();
    }

       public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * @return the fields
     */
    public String getFields() {
        return fields;
    }

    /**
     * @param fields the fields to set
     */
    public void setFields(String fields) {
        this.fields = fields;
    }

    /**
     * @return the extra
     */
    public String getExtra() {
        return extra;
    }

    /**
     * @param extra the extra to set
     */
    public void setExtra(String extra) {
        this.extra = extra;
    }
    

}
