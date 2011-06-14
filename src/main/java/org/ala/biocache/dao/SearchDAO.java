package org.ala.biocache.dao;
/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
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

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;

import org.ala.biocache.dto.DataProviderCountDTO;
import org.ala.biocache.dto.FieldResultDTO;
import au.org.ala.biocache.OccurrenceIndex;
import org.ala.biocache.dto.OccurrenceDTO;
import org.ala.biocache.dto.OccurrencePoint;
import org.ala.biocache.dto.PointType;
import org.ala.biocache.dto.SearchRequestParams;
import org.ala.biocache.dto.SearchResultDTO;
import org.ala.biocache.dto.SpatialSearchRequestParams;
import org.ala.biocache.dto.TaxaCountDTO;
import org.ala.biocache.dto.TaxaRankCountDTO;

/**
 * DAO for searching occurrence records held in the biocache.
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>" .
 */
public interface SearchDAO {
	

	
    /**
     * Find all occurrences for a given (full text) query
     *
     * @param query
     * @param filterQuery
     * @param startIndex
     * @param pageSize
     * @param sortField
     * @param sortDirection
     * @return
     * @throws Exception
     */
    SearchResultDTO findByFulltextQuery(SearchRequestParams requestParams) throws Exception;

    /**
     * Find all occurrences for a given (full text) query, latitude, longitude & radius (km). I.e.
     * a full-text spatial query.
     *
     * @param query
     * @param filterQuery
     * @param lat
     * @param lon
     * @param radius
     * @param startIndex
     * @param pageSize
     * @param sortField
     * @param sortDirection
     * @return
     * @throws Exception
     */
    SearchResultDTO findByFulltextSpatialQuery(SpatialSearchRequestParams requestParams) throws Exception;



    /**
     * Writes the species count in the specified circle to the output stream.
     * @param latitude
     * @param longitude
     * @param radius
     * @param rank
     * @param higherTaxa
     * @param out
     * @return
     * @throws Exception
     */
    int writeSpeciesCountByCircleToStream(SpatialSearchRequestParams requestParams, String speciesGroup, ServletOutputStream out) throws Exception;


    /**
     * Write out the results of this query to the output stream
     * 
     * @param query
     * @param filterQuery
     * @param out
     * @param maxNoOfRecords
     * @return A map of uids and counts that needs to be logged to the ala-logger
     * @throws Exception
     */
	Map<String,Integer> writeResultsToStream(SearchRequestParams searchParams, OutputStream out, int maxNoOfRecords) throws Exception;


    void writeCoordinatesToStream(OutputStream out) throws Exception;

    /**
     * Retrieve an OccurrencePoint (distinct list of points - lat-long to 4 decimal places) for a given search
     *
     * @param query
     * @param filterQuery
     * @return
     * @throws Exception
     */
    List<OccurrencePoint> getFacetPoints(SpatialSearchRequestParams searchParams, PointType pointType) throws Exception;

    /**
     * Retrieve a list of occurrence uid's for a given search
     *
     * @param query
     * @param filterQuery
     * @param pointType
     * @return
     * @throws Exception
     */
    List<OccurrencePoint> getOccurrences(SpatialSearchRequestParams requestParams, PointType pointType, String colourBy, int searchType) throws Exception;

    /**
     * Get a list of occurrence points for a given lat/long and distance (radius)
     *
     * @param latitude
     * @param longitude
     * @param radius
     * @return
     * @throws Exception
     */
    List<OccurrencePoint> findRecordsForLocation(List<String> taxa, String rank,Float latitude, Float longitude, Float radius, PointType pointType) throws Exception;

    

    /**
     * Find all species (and counts) for a given location search (lat/long and radius) and a higher taxon (with rank)
     *
     * @param latitude
     * @param longitude
     * @param radius
     * @param rank
     * @param higherTaxa
     * @param filterQuery
     * @param startIndex
     * @param pageSize
     * @param sortField
     * @param sortDirection
     * @return
     * @throws Exception
     */
    List<TaxaCountDTO> findAllSpeciesByCircleAreaAndHigherTaxa(SpatialSearchRequestParams requestParams, String speciesGroup) throws Exception;

    

    /**
     * Find all the data providers with records.
     * 
     * @return
     */
    List<DataProviderCountDTO> getDataProviderCounts() throws Exception;
    
    List<FieldResultDTO> findRecordByDecadeFor(String query) throws Exception;
    
    List<FieldResultDTO> findRecordByStateFor(String query) throws Exception;

    List<TaxaCountDTO> findTaxaByUserId(String userId) throws Exception;

    List<OccurrenceIndex> findPointsForUserId(String userId) throws Exception;
    /**
     * Find all the sources for the supplied query
     *
     * @param query
     * @return
     * @throws Exception
     */
    Map<String,Integer> getSourcesForQuery(String query, String[] filterQuery) throws Exception;


    TaxaRankCountDTO findTaxonCountForUid(String query, int maximumFacets) throws Exception;
    /**
     * Returns the scientific name and counts for the taxon rank that proceed or include the supplied rank.
     * @param query
     * @param rank
     * @param includeSuppliedRank true when the supplied rank should be included, false when proceeding ranks should be included
     * @return
     * @throws Exception
     */
    TaxaRankCountDTO findTaxonCountForUid(String query, String rank,String returnRank, boolean includeSuppliedRank) throws Exception;
}

