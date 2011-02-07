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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * Data Transfer Object to represent the request parameters required to search
 * for occurrence records against biocache-service.
 * 
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class SearchRequestParams {

    protected String query;
    protected String[] filterQuery;
    protected Integer startIndex = 0;
    protected Integer pageSize = 10;
    protected String sortField = "score";
    protected String sortDirection = "asc";
    protected List<UselessBean> beans = new ArrayList<UselessBean>();
    
    /**
     * Custom toString method to produce a String for the request parameters
     * for the Biocache Service webservice
     * 
     * @return 
     */
    @Override
    public String toString() {
        StringBuilder req = new StringBuilder();
        req.append("q=").append(query);
        req.append("&fq=").append(StringUtils.join(filterQuery, "&fq="));
        req.append("&start=").append(startIndex);
        req.append("&pageSize=").append(pageSize);
        req.append("&sort=").append(sortField);
        req.append("&dir=").append(sortDirection);
        req.append("&beans.size=").append(beans.size());
//        req.append("&beans.name=").append(beans.getName());
//        req.append("&beans.age=").append(beans.getAge());
//        req.append("&beans.ceationDate=").append(beans.getCreationDate());
        return req.toString();
    }

    /**
     * Get the value of query
     *
     * @return the value of query
     */
    public String getQuery() {
        return query;
    }

    /**
     * Set the value of query
     *
     * @param query new value of query
     */
    public void setQuery(String query) {
        this.query = query;
    }
    
    /**
     * Get the value of filterQuery
     *
     * @return the value of filterQuery
     */
    public String[] getFilterQuery() {
        return filterQuery;
    }

    /**
     * Set the value of filterQuery
     *
     * @param filterQuery new value of filterQuery
     */
    public void setFilterQuery(String[] filterQuery) {
        this.filterQuery = filterQuery;
    }
    
    /**
     * Get the value of startIndex
     *
     * @return the value of startIndex
     */
    public Integer getStartIndex() {
        return startIndex;
    }

    /**
     * Set the value of startIndex
     *
     * @param startIndex new value of startIndex
     */
    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
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
     * Get the value of sortField
     *
     * @return the value of sortField
     */
    public String getSortField() {
        return sortField;
    }

    /**
     * Set the value of sortField
     *
     * @param sortField new value of sortField
     */
    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    /**
     * Get the value of sortDirection
     *
     * @return the value of sortDirection
     */
    public String getSortDirection() {
        return sortDirection;
    }

    /**
     * Set the value of sortDirection
     *
     * @param sortDirection new value of sortDirection
     */
    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    public List<UselessBean> getBeans() {
        return beans;
    }

    public void setBeans(List<UselessBean> bean) {
        this.beans = bean;
    }

}
