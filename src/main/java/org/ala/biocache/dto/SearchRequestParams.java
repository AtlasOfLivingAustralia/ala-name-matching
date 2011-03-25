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
 * Data Transfer Object to represent the request parameters required to search
 * for occurrence records against biocache-service.
 * 
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class SearchRequestParams {

    protected String q = "";
    protected String[] fq = {""}; // must not be null
    /**
     * The facets to be included by the search
     * Initialised with the default facets to use
     */
    protected String[] facets = {"basis_of_record",
                                "type_status",
                                "institution_uid",
                                "collection_uid",
                                "state",
                                "biogeographic_region",
                                "rank",
                                "species_group",
                                "kingdom",
                                "family",
                                "assertions",
                                "month",
                                "year",
                                "multimedia"};
    protected Integer start = 0;
    protected Integer pageSize = 10;
    protected String sort = "score";
    protected String dir = "asc";
    private String displayString;
   
    
    /**
     * Custom toString method to produce a String to be used as the request parameters
     * for the Biocache Service webservices
     * 
     * @return request parameters string
     */
    @Override
    public String toString() {
        StringBuilder req = new StringBuilder();
        req.append("q=").append(q);
        req.append("&fq=").append(StringUtils.join(fq, "&fq="));
        req.append("&start=").append(start);
        req.append("&pageSize=").append(pageSize);
        req.append("&sort=").append(sort);
        req.append("&dir=").append(dir);
        return req.toString();
    }

    /**
     * Get the value of q
     *
     * @return the value of q
     */
    public String getQ() {
        return q;
    }

    /**
     * Set the value of q
     *
     * @param q new value of q
     */
    public void setQ(String query) {
        this.q = query;
    }
    
    /**
     * Get the value of fq
     *
     * @return the value of fq
     */
    public String[] getFq() {
        return fq;
    }

    /**
     * Set the value of fq
     *
     * @param fq new value of fq
     */
    public void setFq(String[] filterQuery) {
        this.fq = filterQuery;
    }
    
    /**
     * Get the value of start
     *
     * @return the value of start
     */
    public Integer getStart() {
        return start;
    }

    /**
     * Set the value of start
     *
     * @param start new value of start
     */
    public void setStart(Integer startIndex) {
        this.start = startIndex;
    }

    /**
     * Get the value of pageSize
     *
     * @return the value of pageSize
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Set the value of pageSize
     *
     * @param pageSize new value of pageSize
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Get the value of sort
     *
     * @return the value of sort
     */
    public String getSort() {
        return sort;
    }

    /**
     * Set the value of sort
     *
     * @param sort new value of sort
     */
    public void setSort(String sortField) {
        this.sort = sortField;
    }

    /**
     * Get the value of dir
     *
     * @return the value of dir
     */
    public String getDir() {
        return dir;
    }

    /**
     * Set the value of dir
     *
     * @param dir new value of dir
     */
    public void setDir(String sortDirection) {
        this.dir = sortDirection;
    }

    public String getDisplayString() {
        return displayString;
    }

    public void setDisplayString(String displayString) {
        this.displayString = displayString;
    }

    public String[] getFacets() {
        return facets;
    }

    public void setFacets(String[] facets) {
        this.facets = facets;
    }

 

}
