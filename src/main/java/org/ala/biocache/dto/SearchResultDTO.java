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
package org.ala.biocache.dto;

import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.response.QueryResponse;

import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * DTO to represents the results from a Lucene search
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class SearchResultDTO {

    /** Maximum number of results returned from a query */
    private long pageSize = 10;
    /** Current page of results (not currently used) */
    private long startIndex = 0;
    /** Total number of results for the match (indept of resultsPerPage) */
    private long totalRecords = 0;
    /** Field to sort results by */
    private String sort;
    /** Direction to sort results by (asc || desc) */
    private String dir = "asc";
    /** Status code to be set by Controller (e.g. OK) */
    private String status;
    /** List of results from search */
    private List<OccurrenceDTO> occurrences;
    /** List of facet results from search */
    private List<FacetResultDTO> facetResults;
    /** SOLR query response following search */
    private QueryResponse qr;
    private String query;

    /**
     * Constructor with 2 args
     *
     * @param searchResults
     * @param facetResults
     */
    public SearchResultDTO(List<OccurrenceDTO> searchResults) {
        this.occurrences = searchResults;
        //this.facetResults = facetResults;
    }

    /**
     * Contructor with no args
     */
    public SearchResultDTO() {}

    /*
     * Getters & Setters
     */
    
    public long getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(long start) {
        this.startIndex = start;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }
    
    @JsonIgnore
    public long getCurrentPage() {
        return this.startIndex/pageSize;
    }
   
    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<OccurrenceDTO> getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(List<OccurrenceDTO> values) {
        this.occurrences = values;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    //@JsonIgnore
    public Collection<FacetResultDTO> getFacetResults() {
        return facetResults;
    }

    public void setFacetResults(List<FacetResultDTO> facetResults) {
        this.facetResults = facetResults;
    }

    @JsonIgnore
    public QueryResponse getQr() {
        return qr;
    }

    public void setQr(QueryResponse qr) {
        this.qr = qr;
    }
}
