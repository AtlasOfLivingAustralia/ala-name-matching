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
package org.ala.biocache.dao;


import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;

import org.ala.biocache.dto.DataProviderCountDTO;
import org.ala.biocache.dto.FacetResultDTO;
import org.ala.biocache.dto.FieldResultDTO;
import org.ala.biocache.dto.OccurrenceDTO;
import org.ala.biocache.dto.OccurrencePoint;
import org.ala.biocache.dto.PointType;
import org.ala.biocache.dto.SearchResultDTO;
import org.ala.biocache.dto.TaxaCountDTO;
import org.ala.biocache.dto.TaxaRankCountDTO;
import org.ala.biocache.util.CollectionsCache;
import org.ala.biocache.util.RangeBasedFacets;
import org.ala.biocache.util.SearchUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.core.CoreContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;


import au.org.ala.biocache.OccurrenceIndex;

import au.com.bytecode.opencsv.CSVWriter;

import com.ibm.icu.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ala.biocache.dto.SearchRequestParams;
import org.ala.biocache.dto.SpatialSearchRequestParams;
import org.ala.biocache.util.DownloadFields;
import org.apache.solr.common.SolrDocument;
import javax.inject.Inject;
import org.apache.commons.lang.ArrayUtils;
import au.org.ala.biocache.IndexDAO;
import au.org.ala.biocache.SolrIndexDAO;
import org.apache.solr.client.solrj.SolrServer;
/**
 * SOLR implementation of SearchDao. Uses embedded SOLR server (can be a memory hog).
 *
 * @see org.ala.biocache.dao.SearchDAO
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
@Component("searchDao")
public class SearchDAOImpl implements SearchDAO {

    /** log4 j logger */
    private static final Logger logger = Logger.getLogger(SearchDAOImpl.class);
    /** SOLR home directory - injected by Spring from properties file */
    protected String solrHome = "/data/solr/bio-proto";
    /** SOLR server instance */
    protected SolrServer server;
    /** Limit search results - for performance reasons */
    protected static final Integer MAX_DOWNLOAD_SIZE = 15000;
    protected static final String POINT = "point-0.1";
    protected static final String KINGDOM = "kingdom";
    protected static final String KINGDOM_LSID = "kingdom_lsid";
    protected static final String SPECIES = "species";
    protected static final String SPECIES_LSID = "species_lsid";
    protected static final String NAMES_AND_LSID = "names_and_lsid";
    protected static final String TAXON_CONCEPT_LSID = "taxon_concept_lsid";
    

    //Patterns that are used to prepares a SOLR query for execution
    protected Pattern lsidPattern= Pattern.compile("lsid:[a-zA-Z0-9\\.:-]*");
    protected Pattern urnPattern = Pattern.compile("urn:[a-zA-Z0-9\\.:-]*");
    protected Pattern spacesPattern =Pattern.compile("[^\\s\"()\\[\\]']+|\"[^\"]*\"|'[^']*'");
    protected Pattern uidPattern = Pattern.compile("([a-z_]*_uid:)([a-z0-9]*)");
    protected Pattern spatialPattern = Pattern.compile("\\{!spatial[a-z=\\-\\s0-9\\.\\,]*\\}");
    
    protected String bieUri ="http://bie.ala.org.au";

    /** Download properties */
    protected DownloadFields downloadFields;

    @Inject
    protected SearchUtils searchUtils;
    
    @Inject
    private CollectionsCache collectionCache;
    
    @Inject
    private RestOperations restTemplate;

    /**
     * Initialise the SOLR server instance
     */
    public SearchDAOImpl() {
        if (this.server == null & solrHome != null) {
            try {
//                System.setProperty("solr.solr.home", solrHome);
//                logger.info("Initialising SOLR HOME: "+ solrHome);
//                CoreContainer.Initializer initializer = new CoreContainer.Initializer();
//                CoreContainer coreContainer = initializer.initialize();
//                server = new EmbeddedSolrServer(coreContainer, "");
                
                //use the solr server that has been in the biocache-store...
                SolrIndexDAO dao = (SolrIndexDAO)au.org.ala.biocache.Config.getInstance(IndexDAO.class);
                dao.init();
                server = dao.solrServer();
            } catch (Exception ex) {
                logger.error("Error initialising embedded SOLR server: " + ex.getMessage(), ex);
            }
        }
        downloadFields = new DownloadFields();
    }
  

  
    /**
     * @see org.ala.biocache.dao.SearchDAO#findByFulltextQuery(java.lang.String, java.lang.String,
     *         java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String)
     */
    @Override
    public SearchResultDTO findByFulltextQuery(SearchRequestParams requestParams) throws Exception {
        SearchResultDTO searchResults = new SearchResultDTO();

        try {
            formatSearchQuery(requestParams);
            //add the context information
            updateQueryContext(requestParams);
            SolrQuery solrQuery = initSolrQuery(requestParams); // general search settings
            solrQuery.setQuery(requestParams.getQ());
            QueryResponse qr = runSolrQuery(solrQuery, requestParams);
            searchResults = processSolrResponse(qr, solrQuery);
            //set the title for the results
            searchResults.setQueryTitle(requestParams.getDisplayString());

            logger.info("search query: " + requestParams.getQ());
        } catch (SolrServerException ex) {
            logger.error("Problem communicating with SOLR server. " + ex.getMessage(), ex);
            searchResults.setStatus("ERROR"); // TODO also set a message field on this bean with the error message(?)
        }

        return searchResults;
    }


    /**
     * @see org.ala.biocache.dao.SearchDAO#findByFulltextSpatialQuery(java.lang.String, java.lang.String,
     *         java.lang.Float, java.lang.Float, java.lang.Integer, java.lang.Integer,
     *         java.lang.Integer, java.lang.String, java.lang.String)
     */
    @Override
    public SearchResultDTO findByFulltextSpatialQuery(SpatialSearchRequestParams searchParams) throws Exception {
        SearchResultDTO searchResults = new SearchResultDTO();

        try {
            //String queryString = formatSearchQuery(query);
            formatSearchQuery(searchParams);
            //add context information
            updateQueryContext(searchParams);
            String queryString = buildSpatialQueryString(searchParams);
            //logger.debug("The spatial query " + queryString);
            SolrQuery solrQuery = initSolrQuery(searchParams); // general search settings
            solrQuery.setQuery(queryString);

            QueryResponse qr = runSolrQuery(solrQuery, searchParams);
            searchResults = processSolrResponse(qr, solrQuery);

            logger.info("spatial search query: " + queryString);
        } catch (SolrServerException ex) {
            logger.error("Problem communicating with SOLR server. " + ex.getMessage(), ex);
            searchResults.setStatus("ERROR"); // TODO also set a message field on this bean with the error message(?)
        }

        return searchResults;
    }

    /**
     * @see org.ala.biocache.dao.SearchDAO#writeSpeciesCountByCircleToStream(java.lang.Float, java.lang.Float, java.lang.Integer, java.lang.String, java.util.List, javax.servlet.ServletOutputStream)
     * 
     */
    public int writeSpeciesCountByCircleToStream(SpatialSearchRequestParams searchParams, String speciesGroup, ServletOutputStream out) throws Exception {

        //get the species counts:
        logger.debug("Writing CSV file for species count by circle");
        searchParams.setPageSize(-1);
        List<TaxaCountDTO> species = findAllSpeciesByCircleAreaAndHigherTaxa(searchParams, speciesGroup);
        logger.debug("There are " + species.size() + "records being downloaded");
        CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(out), '\t', '"');
        csvWriter.writeNext(new String[]{
                    "Taxon ID",
                    "Kingdom",
                    "Family",
                    "Scientific name",
                    "Common name",
                    "Record count",});
        int count = 0;
        for (TaxaCountDTO item : species) {

            String[] record = new String[]{
                item.getGuid(),
                item.getKingdom(),
                item.getFamily(),
                item.getName(),
                item.getCommonName(),
                item.getCount().toString(),};


            csvWriter.writeNext(record);
            csvWriter.flush();
            count++;
        }
        return count;
    }
    /**
     * Writes the values for the first supplied facet to output stream
     */
    public void writeFacetToStream(SearchRequestParams searchParams, OutputStream out) throws Exception{
        //set to unlimited facets
        searchParams.setFlimit(-1);
        formatSearchQuery(searchParams);
        //add the context information
        updateQueryContext(searchParams);
        SolrQuery solrQuery = initSolrQuery(searchParams);
        solrQuery.setQuery(searchParams.getQ());
        //don't want any results returned
        solrQuery.setRows(0);
        QueryResponse qr = runSolrQuery(solrQuery, searchParams);
        if (qr.getResults().size() > 0) {
            FacetField ff = qr.getFacetField(searchParams.getFacets()[0]);
            
            if(ff != null && ff.getValueCount() >0){
              //process the "species_guid_ facet by looking up the list of guids                
                if(ff.getName().equals("species_guid")){
                    out.write((ff.getName() +",species name\n").getBytes());
                    List<String> guids = new ArrayList<String>();
                    logger.debug("Downloading " +  ff.getValueCount() + " species guids");
                    for(FacetField.Count value : ff.getValues()){
                        guids.add(value.getName());
                        //Only want to send a sub set of the list so that the URI is not too long for BIE
                        if(guids.size()==30){
                          //now get the list of species from the web service TODO may need to move this code
                            String jsonUri = bieUri + "/species/namesFromGuids.json?guid=" + StringUtils.join(guids, "&guid=");
                            List<String> entities = restTemplate.getForObject(jsonUri, List.class);
                            for(int j = 0 ; j<guids.size();j++){
                                out.write((guids.get(j) + ",").getBytes());
                                out.write((entities.get(j) +"\n").getBytes());
                            }
                            guids.clear();
                        }
                    }
                    //now get the list of species from the web service TODO may need to move this code
                    String jsonUri = bieUri + "/species/namesFromGuids.json?guid=" + StringUtils.join(guids, "&guid=");
                    List<String> entities = restTemplate.getForObject(jsonUri, List.class);
                    for(int i = 0 ; i<guids.size();i++){
                        out.write((guids.get(i) + ",").getBytes());
                        out.write((entities.get(i) +"\n").getBytes());
                    }
                }
                else{
                    //default processing of facets
                    out.write((ff.getName() +"\n").getBytes());
                    for(FacetField.Count value : ff.getValues()){
                        out.write(value.getName().getBytes());
                        out.write("\n".getBytes());
                    }
                }
            }
        }
    }
    
    /**
     * Writes all the distinct latitude and longitude in the index to the supplied
     * output stream.
     * 
     * @param out
     * @throws Exception
     */
    public void writeCoordinatesToStream(SearchRequestParams searchParams,OutputStream out) throws Exception{
        //generate the query to obtain the lat,long as a facet
        
        SearchRequestParams srp = new SearchRequestParams();
        searchUtils.setDefaultParams(srp);
        srp.setFacets(searchParams.getFacets());
        
        SolrQuery solrQuery = initSolrQuery(srp);
        //We want all the facets so we candump all the coordinates
        solrQuery.setFacetLimit(-1);
        solrQuery.setFacetSort("count");
        solrQuery.setRows(0);
        solrQuery.setQuery(searchParams.getQ());

        QueryResponse qr = runSolrQuery(solrQuery, srp);
         if (qr.getResults().size() > 0) {
                FacetField ff = qr.getFacetField(searchParams.getFacets()[0]);
                if(ff != null && ff.getValueCount() >0){
                    out.write("latitude,longitude\n".getBytes());
                    //write the facets to file
                    for(FacetField.Count value : ff.getValues()){
                        //String[] slatlon = value.getName().split(",");
                        out.write(value.getName().getBytes());
                        out.write("\n".getBytes());
                        
                    }
                }
        }
    }


    /**
     * @see org.ala.biocache.dao.SearchDAO#writeResultsToStream(java.lang.String, java.lang.String[], java.io.OutputStream, int)
     *
     * 
     */
    public Map<String, Integer> writeResultsToStream(SearchRequestParams searchParams, OutputStream out, int i) throws Exception {

        int resultsCount = 0;
        Map<String, Integer> uidStats = new HashMap<String, Integer>();
        //stores the remaining limit for data resources that have a download limit
        Map<String, Integer> downloadLimit = new HashMap<String,Integer>();
        try {
            logger.info("search query: " + searchParams.getQ());
            SolrQuery solrQuery = initSolrQuery(searchParams);
            solrQuery.setRows(MAX_DOWNLOAD_SIZE);
            solrQuery.setQuery(searchParams.getQ());
            //Only the fields specified below will be included in the results from the SOLR Query
            solrQuery.setFields("row_key", "institution_uid", "collection_uid", "data_resource_uid", "data_provider_uid");

            int startIndex = 0;
            int pageSize = 1000;
            StringBuilder  sb = new StringBuilder(downloadFields.getFields());
            StringBuilder qasb = new StringBuilder();
            QueryResponse qr = runSolrQuery(solrQuery, searchParams.getFq(), pageSize, startIndex, "score", "asc");
            //get the assertion facets to add them to the download fields
            List<FacetField> facets = qr.getFacetFields();
            for(FacetField facet : facets){
                if(facet.getName().equals("assertions")){
            		
	               for(FacetField.Count facetEntry : facet.getValues()){
	                   //System.out.println("facet: " + facetEntry.getName());
	                   if(qasb.length()>0)
	                       qasb.append(",");
	                   qasb.append(facetEntry.getName());
	               }
                }else if(facet.getName().equals("data_resource_uid")){
                    //populate the download limit
                    initDownloadLimits(downloadLimit, facet);
            	}
            }
            String qas = qasb.toString();            
            String[] fields = sb.toString().split(",");            
            String[]qaFields = qas.split(",");            
            String[] titles = downloadFields.getHeader(fields);
            out.write(StringUtils.join(titles, ",").getBytes());
            if(qaFields.length >0){
                out.write("\t".getBytes());
                out.write(StringUtils.join(qaFields, ",").getBytes());
            }
            out.write("\n".getBytes());
            List<String> uuids = new ArrayList<String>();
           
            while (qr.getResults().size() > 0 && resultsCount < MAX_DOWNLOAD_SIZE) {
                logger.debug("Start index: " + startIndex);
                //cycle through the results adding them to the list that will be sent to cassandra
                for (SolrDocument sd : qr.getResults()) {
                    String druid = sd.getFieldValue("data_resource_uid").toString();
                    if(shouldDownload(druid,downloadLimit)){
	                    resultsCount++;
	                    uuids.add(sd.getFieldValue("row_key").toString());
	
	                    //increment the counters....
	                    incrementCount(uidStats, sd.getFieldValue("institution_uid"));
	                    incrementCount(uidStats, sd.getFieldValue("collection_uid"));
	                    incrementCount(uidStats, sd.getFieldValue("data_provider_uid"));
	                    incrementCount(uidStats, druid);
                    }
                }
                //logger.debug("Downloading " + uuids.size() + " records");
                au.org.ala.biocache.Store.writeToStream(out, ",", "\n", uuids.toArray(new String[]{}),
                        fields, qaFields);
                startIndex += pageSize;
                uuids.clear();
                if (resultsCount < MAX_DOWNLOAD_SIZE) {
                    qr = runSolrQuery(solrQuery, searchParams.getFq(), pageSize, startIndex, "score", "asc");
                   
                }
            }

        } catch (SolrServerException ex) {
            logger.error("Problem communicating with SOLR server. " + ex.getMessage(), ex);
            //searchResults.setStatus("ERROR"); // TODO also set a message field on this bean with the error message(?)
        }
        return uidStats;
    }
    /**
     * Indicates whether or not a records from the supplied data resource should be included 
     * in the download. (based on download limits)
     * @param druid
     * @param limits
     * @return
     */
    private boolean shouldDownload(String druid, Map<String, Integer>limits){
        if(limits.size()>0 && limits.containsKey(druid)){
            Integer remainingLimit = limits.get(druid);
            if(remainingLimit==0){
                return false;
            }
            limits.put(druid, remainingLimit-1);
    	}
        return true;
    }
    /**
     * Initialises the download limit tracking
     * @param map
     * @param facet
     */
    private void initDownloadLimits(Map<String,Integer> map,FacetField facet){
        //get the download limits from the cache
        Map<String, Integer>limits = collectionCache.getDownloadLimits();
        for(FacetField.Count facetEntry :facet.getValues()){
            Integer limit = limits.get(facetEntry.getName());
            if(limit != null && limit >0){
                //check to see if the number of records returned from the query execeeds the limit
                if(limit < facetEntry.getCount())
                    map.put(facetEntry.getName(), limit);
            }
        }
        if(map.size()>0)
            logger.debug("Downloading with the following limits: " + map);
    }

   
    private void incrementCount(Map<String, Integer> values, Object uid) {
        if (uid != null) {
            Integer count = values.containsKey(uid) ? values.get(uid) : 0;
            count++;
            values.put(uid.toString(), count);
        }
    }

    

    /**
     * @see org.ala.biocache.dao.SearchDao#getFacetPoints(java.lang.String, java.lang.String[], PointType pointType)
     */
    @Override
    public List<OccurrencePoint> getFacetPoints(SpatialSearchRequestParams searchParams, PointType pointType) throws Exception {
        List<OccurrencePoint> points = new ArrayList<OccurrencePoint>(); // new OccurrencePoint(PointType.POINT);
         formatSearchQuery(searchParams);
        logger.info("search query: " + searchParams.getQ());
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQueryType("standard");
        solrQuery.setQuery(searchParams.getQ());
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        solrQuery.addFacetField(pointType.getLabel());
        solrQuery.setFacetMinCount(1);
        solrQuery.setFacetLimit(MAX_DOWNLOAD_SIZE);  // unlimited = -1

        //add the context information
        updateQueryContext(searchParams);

        QueryResponse qr = runSolrQuery(solrQuery, searchParams.getFq(), 1, 0, "score", "asc");
        List<FacetField> facets = qr.getFacetFields();

        if (facets != null) {
            for (FacetField facet : facets) {
                List<FacetField.Count> facetEntries = facet.getValues();
                if (facet.getName().contains(pointType.getLabel()) && (facetEntries != null) && (facetEntries.size() > 0)) {

                    for (FacetField.Count fcount : facetEntries) {
                        OccurrencePoint point = new OccurrencePoint(pointType);
                        point.setCount(fcount.getCount());
                        String[] pointsDelimited = StringUtils.split(fcount.getName(), ',');
                        List<Float> coords = new ArrayList<Float>();

                        for (String coord : pointsDelimited) {
                            try {
                                Float decimalCoord = Float.parseFloat(coord);
                                coords.add(decimalCoord);
                            } catch (NumberFormatException numberFormatException) {
                                logger.warn("Error parsing Float for Lat/Long: " + numberFormatException.getMessage(), numberFormatException);
                            }
                        }

                        if (!coords.isEmpty()) {
                            Collections.reverse(coords); // must be long, lat order
                            point.setCoordinates(coords);
                            points.add(point);
                        }
                    }
                }
            }
        }

        return points;
    }

    /**
     * @see org.ala.biocache.dao.SearchDao#getOccurrences(java.lang.String, java.lang.String[], PointType pointType)
     */
    @Override
    public List<OccurrencePoint> getOccurrences(SpatialSearchRequestParams searchParams, PointType pointType, String colourBy, int searchType) throws Exception {


        List<OccurrencePoint> points = new ArrayList<OccurrencePoint>();
        List<String> colours = new ArrayList<String>();

        searchParams.setPageSize(100);

        String queryString = "";
        formatSearchQuery(searchParams);
        if (searchType == 0) {
            queryString = searchParams.getQ();
        } else if (searchType == 1) {
            queryString = buildSpatialQueryString(searchParams.getQ(), searchParams.getLat(), searchParams.getLon(), searchParams.getRadius());
        }

        logger.info("search query: " + queryString);
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQueryType("standard");
        solrQuery.setQuery(queryString);
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        solrQuery.addFacetField(pointType.getLabel());
        solrQuery.setFacetMinCount(1);
        solrQuery.setFacetLimit(MAX_DOWNLOAD_SIZE);  // unlimited = -1

        //add the context information
        updateQueryContext(searchParams);

        QueryResponse qr = runSolrQuery(solrQuery, searchParams);
        SearchResultDTO searchResults = processSolrResponse(qr, solrQuery);
        List<OccurrenceIndex> ocs = searchResults.getOccurrences();


        if (!ocs.isEmpty() && ocs.size() > 0) {

            for (OccurrenceIndex oc : ocs) {

                List<Float> coords = new ArrayList<Float>();
                coords.add(oc.getDecimalLongitude().floatValue());
                coords.add(oc.getDecimalLatitude().floatValue());

                OccurrencePoint point = new OccurrencePoint();
                point.setCoordinates(coords);

                if (searchType == 0) {
                    // for now, let's set the colour in this one.

                    String value = "Not available";
                    if (StringUtils.isNotBlank(colourBy)) {

                        try {
                            if (oc.getMap() != null) {
                                java.util.Map map = oc.getMap();
                                if (map != null) {
                                    
                                    //check to see if it is empty otherwise a NPE is thrown when option.get is called
                                    if (map.containsKey(colourBy)) {
                                        value = (String) map.get(colourBy);
                                    }
                                    point.setOccurrenceUid(value);
                                }

                            } // check if oc is null
                        } catch (Exception e) {
                            //System.out.println("Error with getOccurrences:");
                            //e.printStackTrace(System.out);
                        }
                    }

                } else if (searchType == 1) {
                    point.setOccurrenceUid(oc.getUuid());
                }

                points.add(point);
            }
        }

        return points;

    }


    /*
    public List<Term> query(String q, int limit) {
    List<Term> items = null;
    initSolrServer();

    // escape special characters
    SolrQuery query = new SolrQuery();
    query.addTermsField("spell");
    query.setTerms(true);
    query.setTermsLimit(limit);
    query.setTermsLower(q);
    query.setTermsPrefix(q);
    query.setQueryType("/terms");

    try {
    QueryResponse qr = server.query(query);
    TermsResponse resp = qr.getTermsResponse();
    items = resp.getTerms("spell");
    } catch (SolrServerException e) {
    items = null;
    }

    return items;
    }
     */
    /**
     * http://ala-biocache1.vm.csiro.au:8080/solr/select?q=*:*&rows=0&facet=true&facet.field=data_provider_id&facet.field=data_provider&facet.sort=data_provider_id
     * 
     * @see org.ala.biocache.dao.SearchDAO#getDataProviderCounts()
     */
    //IS THIS BEING USED BY ANYTHING??
    @Override
    public List<DataProviderCountDTO> getDataProviderCounts() throws Exception {

        List<DataProviderCountDTO> dpDTOs = new ArrayList<DataProviderCountDTO>(); // new OccurrencePoint(PointType.POINT);
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQueryType("standard");
        solrQuery.setQuery("*:*");
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        solrQuery.addFacetField("data_provider_uid");
        solrQuery.addFacetField("data_provider");
        solrQuery.setFacetMinCount(1);
        QueryResponse qr = runSolrQuery(solrQuery, null, 1, 0, "data_provider", "asc");
        List<FacetField> facets = qr.getFacetFields();

        if (facets != null && facets.size() == 2) {

            FacetField dataProviderIdFacet = facets.get(0);
            FacetField dataProviderNameFacet = facets.get(1);

            List<FacetField.Count> dpIdEntries = dataProviderIdFacet.getValues();
            List<FacetField.Count> dpNameEntries = dataProviderNameFacet.getValues();

            for (int i = 0; i < dpIdEntries.size(); i++) {

                FacetField.Count dpIdEntry = dpIdEntries.get(i);
                FacetField.Count dpNameEntry = dpNameEntries.get(i);

                String dataProviderId = dpIdEntry.getName();
                String dataProviderName = dpNameEntry.getName();
                long count = dpIdEntry.getCount();

                DataProviderCountDTO dto = new DataProviderCountDTO(dataProviderId, dataProviderName, count);
                dpDTOs.add(dto);
            }
        }
        logger.info("Find data providers = " + dpDTOs.size());
        return dpDTOs;
    }

    /**
     * @see org.ala.biocache.dao.SearchDAO#findRecordsForLocation(Float, Float, Integer)
     * This is used by explore your area
     */
    @Override
    public List<OccurrencePoint> findRecordsForLocation(SpatialSearchRequestParams requestParams, PointType pointType) throws Exception {
        List<OccurrencePoint> points = new ArrayList<OccurrencePoint>(); // new OccurrencePoint(PointType.POINT);
        String queryString = buildSpatialQueryString(requestParams);
        //String queryString = formatSearchQuery(query);
        logger.info("location search query: " + queryString + "; pointType: " + pointType.getLabel());
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQueryType("standard");
        solrQuery.setQuery(queryString);

       
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        solrQuery.addFacetField(pointType.getLabel());
        solrQuery.setFacetMinCount(1);
        solrQuery.setFacetLimit(MAX_DOWNLOAD_SIZE);  // unlimited = -1

        QueryResponse qr = runSolrQuery(solrQuery, null, 1, 0, "score", "asc");
        logger.info("qr number found: " + qr.getResults().getNumFound());
        List<FacetField> facets = qr.getFacetFields();

        if (facets != null) {
            for (FacetField facet : facets) {
                List<FacetField.Count> facetEntries = facet.getValues();
                if (facet.getName().contains(pointType.getLabel()) && (facetEntries != null) && (facetEntries.size() > 0)) {

                    for (FacetField.Count fcount : facetEntries) {
                        OccurrencePoint point = new OccurrencePoint(pointType);
                        point.setCount(fcount.getCount());
                        String[] pointsDelimited = StringUtils.split(fcount.getName(), ',');
                        List<Float> coords = new ArrayList<Float>();

                        for (String coord : pointsDelimited) {
                            try {
                                Float decimalCoord = Float.parseFloat(coord);
                                coords.add(decimalCoord);
                            } catch (NumberFormatException numberFormatException) {
                                logger.warn("Error parsing Float for Lat/Long: " + numberFormatException.getMessage(), numberFormatException);
                            }
                        }

                        if (!coords.isEmpty()) {
                            Collections.reverse(coords); // must be long, lat order
                            point.setCoordinates(coords);
                            points.add(point);
                        }
                    }
                }
            }
        }
        logger.info("findRecordsForLocation: number of points = " + points.size());

        return points;
    }

    @Override
    //TODO: Not storing/indexing a user id
    //IS this being used
    public List<TaxaCountDTO> findTaxaByUserId(String userId) throws Exception {

        String queryString = "user_id:" + ClientUtils.escapeQueryChars(userId);
        List<String> facetFields = new ArrayList<String>();
        facetFields.add(TAXON_CONCEPT_LSID);
        List<TaxaCountDTO> speciesWithCounts = getSpeciesCounts(queryString, null, facetFields, 1000, 0, "taxon_name", "asc");

        return speciesWithCounts;
    }

    @Override
    //TODO Not storing/indexing a user
    //IS this being used
    public List<OccurrenceIndex> findPointsForUserId(String userId) throws Exception {
        String query = "user_id:" + ClientUtils.escapeQueryChars(userId);
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQueryType("standard");
        solrQuery.setQuery(query);
        QueryResponse qr = runSolrQuery(solrQuery, null, 1000000, 0, "score", "asc");
        SearchResultDTO searchResults = processSolrResponse(qr, solrQuery);
        logger.debug("solr result (size): " + searchResults.getOccurrences().size());

        return searchResults.getOccurrences();
    }



    /**
     * @see org.ala.biocache.dao.SearchDAO#findAllSpeciesByCircleAreaAndHigherTaxa(Float, Float,
     *     Integer, String, String, String, Integer, Integer, String, String)
     */
    @Override
    public List<TaxaCountDTO> findAllSpeciesByCircleAreaAndHigherTaxa(SpatialSearchRequestParams requestParams, String speciesGroup) throws Exception {
        String q = speciesGroup.equals("ALL_SPECIES")?"*:*":"species_group:"+speciesGroup;
        //add the context information
        updateQueryContext(requestParams);
        requestParams.setQ(q);
        String queryString = buildSpatialQueryString(requestParams);
        List<String> facetFields = new ArrayList<String>();
        facetFields.add(NAMES_AND_LSID);
        logger.debug("The species count query " + queryString);
        List<String> fqList = new ArrayList<String>();
        //only add the FQ's if they are not the default values
        if(requestParams.getFq().length>0 && (requestParams.getFq()[0]).length()>0)
            org.apache.commons.collections.CollectionUtils.addAll(fqList, requestParams.getFq());
        List<TaxaCountDTO> speciesWithCounts = getSpeciesCounts(queryString, fqList, facetFields, requestParams.getPageSize(), requestParams.getStart(), requestParams.getSort(), requestParams.getDir());

        return speciesWithCounts;
    }

    /**
     * @see org.ala.biocache.dao.SearchDAO#findRecordByDecadeFor(java.lang.String)
     * IS THIS BEIGN USED OR NECESSARY?
     */
    @Override
    public List<FieldResultDTO> findRecordByDecadeFor(String query) throws Exception {
        List<FieldResultDTO> fDTOs = new ArrayList<FieldResultDTO>(); // new OccurrencePoint(PointType.POINT);
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQueryType("standard");
        solrQuery.add("facet.date", "occurrence_date");
        solrQuery.add("facet.date.start", "1850-01-01T12:00:00Z"); // facet date range starts from 1850
        solrQuery.add("facet.date.end", "NOW/DAY"); // facet date range ends for current date (gap period)
        solrQuery.add("facet.date.gap", "+10YEAR"); // gap interval of 10 years
        solrQuery.setQuery(query);
//        solrQuery.setQuery(query);
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        QueryResponse qr = runSolrQuery(solrQuery, null, 1, 0, "score", "asc");

        //get date fields
        List<FacetField> facetDateFields = qr.getFacetDates();
        FacetField ff = facetDateFields.get(0);

        System.out.println(ff.getName());
        boolean addCounts = false;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");

        for (Count count : ff.getValues()) {
            //only start adding counts when we hit a decade with some results.
            if (addCounts || count.getCount() > 0) {
                addCounts = true;
                Date date = DateUtils.parseDate(count.getName(), new String[]{"yyyy-MM-dd'T'HH:mm:ss'Z'"});
                String year = sdf.format(date);
                FieldResultDTO f = new FieldResultDTO(year, count.getCount());
                fDTOs.add(f);
                System.out.println(f.getLabel() + " " + f.getCount());
            }
        }
        return fDTOs;
    }

    /**
     * @see org.ala.biocache.dao.SearchDAO#findRecordByStateFor(java.lang.String)
     * IS THIS BEGIN USED OR NECESSARY
     */
    @Override
    public List<FieldResultDTO> findRecordByStateFor(String query)
            throws Exception {
        List<FieldResultDTO> fDTOs = new ArrayList<FieldResultDTO>(); // new OccurrencePoint(PointType.POINT);
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQueryType("standard");
        solrQuery.setQuery(query);
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        solrQuery.addFacetField("state");
        solrQuery.setFacetMinCount(1);
        QueryResponse qr = runSolrQuery(solrQuery, null, 1, 0, "data_provider", "asc");
        List<FacetField> facets = qr.getFacetFields();
        FacetField ff = qr.getFacetField("state");
        if (ff != null) {
            for (Count count : ff.getValues()) {
                //only start adding counts when we hit a decade with some results.
                FieldResultDTO f = new FieldResultDTO(count.getName(), count.getCount());
                fDTOs.add(f);
            }
        }
        return fDTOs;
    }

    /**
     * Finds the counts for the taxa starting at the family level.
     *
     * If the number of distinct taxa is higher than maximumFacets then we
     * move up to the next level of the taxonomic hierarchy.
     *
     * @param query
     * @param maximumFacets
     * @return
     * @throws Exception
     */
    @Override
    public TaxaRankCountDTO findTaxonCountForUid(String query, String queryContext, int maximumFacets) throws Exception {
        logger.info("Attempting to find the counts for " + query);
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQueryType("standard");
        solrQuery.setQuery(query);
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.setFacetSort("count");
        solrQuery.setFacetLimit(-1);
        TaxaRankCountDTO trDTO = null;
        for (int i = 5; i > 0 && trDTO == null; i--) {
            String ffname = searchUtils.getRankFacetName(i);
            solrQuery.addFacetField(ffname);
            solrQuery.setFacetMinCount(1);
            QueryResponse qr = runSolrQuery(solrQuery, getQueryContextAsArray(queryContext), 1, 0, ffname, "asc");
            if (qr.getResults().size() > 0) {
                FacetField ff = qr.getFacetField(ffname);
                if (ff.getValues().size() <= maximumFacets) {
                    trDTO = new TaxaRankCountDTO(ffname);
                    List<FieldResultDTO> fDTOs = new ArrayList<FieldResultDTO>();
                    for (Count count : ff.getValues()) {
                        FieldResultDTO f = new FieldResultDTO(count.getName(), count.getCount());
                        fDTOs.add(f);
                    }
                    trDTO.setTaxa(fDTOs);
                }
            } else {
                return null;
            }
        }
        return trDTO;
    }

    /**
     * @see org.ala.biocache.dao.SearchDAO#findTaxonCountForUid(java.lang.String, java.lang.String)
     */
    public TaxaRankCountDTO findTaxonCountForUid(String query, String rank, String returnRank, String queryContext, boolean includeSuppliedRank) throws Exception {
        TaxaRankCountDTO trDTO = null;
        List<String> ranks = returnRank== null?searchUtils.getNextRanks(rank, includeSuppliedRank) : new ArrayList<String>();
        if(returnRank != null)
            ranks.add(returnRank);
        if (ranks != null && ranks.size() > 0) {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQueryType("standard");
            solrQuery.setQuery(query);
            solrQuery.setRows(0);
            solrQuery.setFacet(true);
            solrQuery.setFacetMinCount(1);
            solrQuery.setFacetSort("count");
            solrQuery.setFacetLimit(-1); //we want all facets
            for (String r : ranks) {
                solrQuery.addFacetField(r);
            }
            QueryResponse qr = runSolrQuery(solrQuery, getQueryContextAsArray(queryContext), 1, 0, rank, "asc");
            if (qr.getResults().size() > 0) {
                for (String r : ranks) {
                    trDTO = new TaxaRankCountDTO(r);
                    FacetField ff = qr.getFacetField(r);
                    if (ff != null && ff.getValues() != null) {
                        List<Count> counts = ff.getValues();
                        if (counts.size() > 0) {
                            List<FieldResultDTO> fDTOs = new ArrayList<FieldResultDTO>();
                            for (Count count : counts) {
                                FieldResultDTO f = new FieldResultDTO(count.getName(), count.getCount());
                                fDTOs.add(f);
                            }
                            trDTO.setTaxa(fDTOs);
                            break;
                        }
                    }
                }
            }
        }
        return trDTO;
    }

    //TODO delete me just temporary until all methods are modified to use SearchRequestParams requestParams
    private QueryResponse runSolrQuery(SolrQuery solrQuery, String filterQuery[], Integer pageSize,
            Integer startIndex, String sortField, String sortDirection) throws SolrServerException {

        SearchRequestParams requestParams = new SearchRequestParams();
        requestParams.setFq(filterQuery);
        requestParams.setPageSize(pageSize);
        requestParams.setStart(startIndex);
        requestParams.setSort(sortField);
        requestParams.setDir(sortDirection);
        return runSolrQuery(solrQuery, requestParams);

    }

    /**
     * Perform SOLR query - takes a SolrQuery and search params
     *
     * @param solrQuery
     * @param filterQuery
     * @param pageSize
     * @param startIndex
     * @param sortField
     * @param sortDirection
     * @return
     * @throws SolrServerException
     */
    private QueryResponse runSolrQuery(SolrQuery solrQuery, SearchRequestParams requestParams) throws SolrServerException {

        if (requestParams.getFq() != null) {
            for (String fq : requestParams.getFq()) {
                // pull apart fq. E.g. Rank:species and then sanitize the string parts
                // so that special characters are escaped apporpriately
                if (fq.isEmpty()) {
                    continue;
                }
                String[] parts = fq.split(":", 2); // separate query field from query text
                logger.debug("fq split into: " + parts.length + " parts: " + parts[0] + " & " + parts[1]);
                String prefix = null;
                String suffix = null;
                // don't escape range queries
                if (parts[1].contains(" TO ")) {
                    prefix = parts[0];
                    suffix = parts[1];
                } else {
                    prefix = ClientUtils.escapeQueryChars(parts[0]);
                    suffix = ClientUtils.escapeQueryChars(parts[1]);
                }

                //FIXME check for blank value and replace with constant
                if(StringUtils.isEmpty(suffix)){
                    suffix = "Unknown";
                }
                solrQuery.addFilterQuery(prefix + ":" + suffix); // solrQuery.addFacetQuery(facetQuery)
                logger.info("adding filter query: " + prefix + ":" + suffix);
            }
        }

        solrQuery.setRows(requestParams.getPageSize());
        solrQuery.setStart(requestParams.getStart());
        solrQuery.setSortField(requestParams.getSort(), ORDER.valueOf(requestParams.getDir()));
        logger.info("runSolrQuery: " + solrQuery.toString());
        return server.query(solrQuery); // can throw exception
    }

    /**
     * Process the {@see org.​apache.​solr.​client.​solrj.​response.QueryResponse} from a SOLR search and return
     * a {@link org.ala.biocache.dto.SearchResultDTO}
     *
     * @param qr
     * @param solrQuery
     * @return
     */
    private SearchResultDTO processSolrResponse(QueryResponse qr, SolrQuery solrQuery) {
        SearchResultDTO searchResult = new SearchResultDTO();
        SolrDocumentList sdl = qr.getResults();
        // Iterator it = qr.getResults().iterator() // Use for download 
        List<FacetField> facets = qr.getFacetFields();
        List<FacetField> facetDates = qr.getFacetDates();
        Map<String, Integer> facetQueries = qr.getFacetQuery();
        if (facetDates != null) {
            logger.debug("Facet dates size: " + facetDates.size());
            facets.addAll(facetDates);
        }
        //Map<String, Map<String, List<String>>> highlights = qr.getHighlighting();
        List<OccurrenceIndex> results = qr.getBeans(OccurrenceIndex.class);
        List<FacetResultDTO> facetResults = new ArrayList<FacetResultDTO>();
        searchResult.setTotalRecords(sdl.getNumFound());
        searchResult.setStartIndex(sdl.getStart());
        searchResult.setPageSize(solrQuery.getRows()); //pageSize
        searchResult.setStatus("OK");
        String[] solrSort = StringUtils.split(solrQuery.getSortField(), " "); // e.g. "taxon_name asc"
        logger.debug("sortField post-split: " + StringUtils.join(solrSort, "|"));
        searchResult.setSort(solrSort[0]); // sortField
        searchResult.setDir(solrSort[1]); // sortDirection
        searchResult.setQuery(solrQuery.getQuery());
        searchResult.setOccurrences(results);
        // populate SOLR facet results
        if (facets != null) {
            for (FacetField facet : facets) {
                List<FacetField.Count> facetEntries = facet.getValues();                
                if ((facetEntries != null) && (facetEntries.size() > 0)) {
                    ArrayList<FieldResultDTO> r = new ArrayList<FieldResultDTO>();
                    for (FacetField.Count fcount : facetEntries) {
//                        String msg = fcount.getName() + ": " + fcount.getCount();                       
                        //logger.trace(fcount.getName() + ": " + fcount.getCount());        	
                    		r.add(new FieldResultDTO(fcount.getName(), fcount.getCount()));
                    }
                    // only add facets if there are more than one facet result
                    if (r.size() > 0) {
                        FacetResultDTO fr = new FacetResultDTO(facet.getName(), r);
                        facetResults.add(fr);
                    }
                }
            }
        }
        //all belong to uncertainty range for now
        Map<String, String> rangeMap = RangeBasedFacets.getRangeMap("uncertainty");
        List<FieldResultDTO> fqr = new ArrayList<FieldResultDTO>();
        for(String value: facetQueries.keySet()){
            if(facetQueries.get(value)>0)
                fqr.add(new FieldResultDTO(rangeMap.get(value), facetQueries.get(value)));
        }
        facetResults.add(new FacetResultDTO("uncertainty", fqr));

        //handle the confidence facets in the facetQueries
        //TODO Work out whether or not we need the confidence facets ie the confidence ratign indexed???
//       List<FieldResultDTO> fqr = new ArrayList<FieldResultDTO>();
//        if(facetQueries != null){
//            for(String range: facetQueries.keySet()){
//                //get the OccurrenceSource
//                OccurrenceSource os = OccurrenceSource.getForRange(range.substring(range.indexOf(":")+1));
//                if(os != null && facetQueries.get(range)>0){
//                    fqr.add(new FieldResultDTO(os.getDisplayName(), facetQueries.get(range)));
//                }
//
//            }
//        }
//        //Only add the confidence facetResult if there is more than 1 facet
//        if(fqr.size()>1){
//                FacetResultDTO fr = new FacetResultDTO(OccurrenceSource.FACET_NAME, fqr);
//                facetResults.add(fr);
//            }

        searchResult.setFacetResults(facetResults);
        // The query result is stored in its original format so that all the information
        // returned is available later on if needed
        searchResult.setQr(qr);

        //add the URL information necessary for the maps and downloads
        String params = "?q=" + solrQuery.getQuery();
        if (solrQuery.getFilterQueries() != null && solrQuery.getFacetFields().length > 0) {
            params += "&fq=" + StringUtils.join(solrQuery.getFilterQueries(), "&fq=");
        }
        logger.info("The download params: " + params);
        searchResult.setUrlParameters(params);
        return searchResult;
    }

    /**
     * Build the query string for a spatial query (using Spatial-Solr plugin syntax)
     *
     * TODO change param type to SearchRequestParams
     *
     * New plugin syntax
     * {!spatial circles=52.347,4.453,10}
     *
     * TODO different types of spatial queries...
     *
     * @param fullTextQuery
     * @param latitude
     * @param longitude
     * @param radius
     * @return
     */
    protected String buildSpatialQueryString(String fullTextQuery, Float latitude, Float longitude, Float radius) {
        String queryString = "{!spatial circles=" + latitude.toString() + "," + longitude.toString()
                + "," + radius.toString() + "}" +  fullTextQuery;
        return queryString;
    }
    
    protected String buildSpatialQueryString(SpatialSearchRequestParams searchParams){
        if(searchParams != null){
            StringBuilder sb = new StringBuilder("{!spatial ");
            if(StringUtils.isEmpty(searchParams.getWkt())){
                sb.append("circles=").append(searchParams.getLat().toString()).append(",");
                sb.append(searchParams.getLon().toString()).append(",");
                sb.append(searchParams.getRadius().toString()).append("}");
            }
            else{
                //format the wkt
                sb.append("wkt=").append(searchParams.getWkt().replaceAll(" ", ":")).append("}");
            }

            sb.append(searchParams.getQ());
            return sb.toString();
        }
        return null;
    }

    /**
     * Format the search input query for a full-text search.
     *
     * This includes constructing a user friendly version of the query to
     * be used for display purposes.
     *
     * @param query
     * @return
     */
    protected void formatSearchQuery(SearchRequestParams searchParams) {
        // set the query
        String query =searchParams.getQ();
        StringBuffer queryString = new StringBuffer();
        StringBuffer displaySb = new StringBuffer();
        String displayString = query;
        //if the query string contains lsid: we will need to replace it with the corresponding lft range
        int last =0;
        if (query.contains("lsid:")) {
            Matcher matcher = lsidPattern.matcher(query);
            while (matcher.find()) {
                String value = matcher.group();
                logger.debug("preprocessing " + value);
                String[] values = searchUtils.getTaxonSearch(value.substring(5, value.length()));
                matcher.appendReplacement(queryString, values[0]);
                displaySb.append(query.substring(last, matcher.start()));
                if(!values[1].startsWith("lsid:"))
                    displaySb.append("<span>").append(values[1]).append("</span>");
                else
                    displaySb.append(values[1]);
                last = matcher.end();
                //matcher.appendReplacement(displayString, values[1]);
            }
            matcher.appendTail(queryString);
            displaySb.append(query.substring(last, query.length()));

           
            query = queryString.toString();
            displayString = displaySb.toString();
        }
        if (query.contains("urn")) {
            //esacape the URN strings before escaping the rest this aviods the issue with attempting to search on a urn field
            Matcher matcher = urnPattern.matcher(query);
            queryString.setLength(0);
            while (matcher.find()) {
                String value = matcher.group();
                
                logger.debug("escaping lsid urns  " + value );
                matcher.appendReplacement(queryString,prepareSolrStringForReplacement(value));
            }
            matcher.appendTail(queryString);
            query = queryString.toString();
        }
        if(query.contains("{!spatial")){           
            Matcher matcher = spatialPattern.matcher(query);
            if(matcher.find()){
                String spatial = matcher.group();            
                //format the search query of the remaining text only            
                searchParams.setQ(query.substring(matcher.regionStart() + spatial.length(), query.length()));
                //format the remaining query
                formatSearchQuery(searchParams);
                //now append Q's together
                queryString.setLength(0);
                queryString.append(spatial);
                queryString.append(searchParams.getQ());
                searchParams.setQ(queryString.toString());
            }
            
                
        }
        else{
            //escape reserved characters unless the colon represnts a field name colon
            queryString.setLength(0);

            Matcher matcher = spacesPattern.matcher(query);
            while(matcher.find()){
                String value = matcher.group();

                //special cases to ignore from character escaping
                //if the value is a single - or * it means that we don't want to escape it as it is likely to have occurred in the following situation -(occurrence_date:[* TO *]) or *:*
                if(!value.equals("-") && !value.equals("*")  && !value.equals("*:*")){

                    //split on the colon
                    String[] bits = StringUtils.split(value, ":", 2);
                    if(bits.length == 2){
                        if(!bits[0].contains("urn"))
                            matcher.appendReplacement(queryString, bits[0] +":"+ prepareSolrStringForReplacement(bits[1]));

                    }
                    //need to ignore field names where the : is at the end because the pattern matching will return field_name: as a match when it has a double quoted value
                    else if(!value.endsWith(":")){
                        //default behaviour is to escape all 
                        matcher.appendReplacement(queryString, prepareSolrStringForReplacement(value));
                    }
                }

            }
            matcher.appendTail(queryString);

            //substitute better display strings for collection/inst etc searches
            if(displayString.contains("_uid")){
                displaySb.setLength(0);
                matcher = uidPattern.matcher(displayString);
                while(matcher.find()){
                    String newVal = "<span>"+searchUtils.getUidDisplayString(matcher.group(2)) +"</span>";
                    if(newVal != null)
                        matcher.appendReplacement(displaySb, newVal);
                }
                matcher.appendTail(displaySb);
                displayString = displaySb.toString();
            }
            searchParams.setQ(queryString.toString());
            searchParams.setDisplayString(displayString);
            //return queryString.toString();
        }
        
        //format the fq's for facets that need ranges substituted
        for(int i=0; i<searchParams.getFq().length;i++){
            String fq = searchParams.getFq()[i];
            String[] parts = fq.split(":", 2);
            //check to see if the first part is a range based query and update if necessary
            Map<String, String> titleMap = RangeBasedFacets.getTitleMap(parts[0]);
            if(titleMap != null){
                searchParams.getFq()[i]= titleMap.get(parts[1]);
            }
        }
    }

    /**
     * Creates a SOLR escaped string the can be used in a StringBuffer.appendReplacement
     * The appendReplacement needs an extra delimiting on the backslashes
     * @param value
     * @return
     */
    private String prepareSolrStringForReplacement(String value){
        //if starts and ends with quotes just escape the inside
        boolean quoted = false;
        StringBuffer sb = new StringBuffer();
        if(value.startsWith("\"") && value.endsWith("\"")){
            quoted = true;
            value = value.substring(1, value.length()-1);
            sb.append("\"");
        }
        sb.append(ClientUtils.escapeQueryChars(value).replaceAll("\\\\", "\\\\\\\\"));
        if(quoted) sb.append("\"");
        return sb.toString();
    }

    /**
     * Updates the supplied search params to cater for the query context
     * @param searchParams
     */
    protected void updateQueryContext(SearchRequestParams searchParams){
        //TODO better method of getting the mappings between qc on solr fields names
        String qc = searchParams.getQc();
        if(StringUtils.isNotEmpty(qc)){
//            String[] values = qc.split(",");
//            for(int i =0; i<values.length;i++){
//                String field = values[i];
//                values[i]= field.replace("hub:", "data_hub_uid:");
//
//            }
            
            //add the query context to the filter query
            searchParams.setFq((String[])ArrayUtils.addAll(searchParams.getFq(), getQueryContextAsArray(qc)));
        }
    }

    protected String[] getQueryContextAsArray(String queryContext){
        if(StringUtils.isNotEmpty(queryContext)){
            String[] values = queryContext.split(",");
            for(int i =0; i<values.length;i++){
                String field = values[i];
                values[i]= field.replace("hub:", "data_hub_uid:");

            }

            //add the query context to the filter query
            return values;
        }
        return new String[]{};
    }
   

    /**
     * Helper method to create SolrQuery object and add facet settings
     *
     * @return solrQuery the SolrQuery
     */
    protected SolrQuery initSolrQuery(SearchRequestParams searchParams) {

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQueryType("standard");
        // Facets
        solrQuery.setFacet(true);
        for (String facet : searchParams.getFacets()) {
            if (facet.equals("month")) {
                solrQuery.addFacetField("month");
                solrQuery.add("f.month.facet.sort", "index"); // sort by Jan-Dec
            } else if (facet.equals("date") || facet.equals("year")) {
                solrQuery.add("facet.date", "occurrence_" +facet);
                solrQuery.add("facet.date.start", "1850-01-01T12:00:00Z"); // facet date range starts from 1850
                solrQuery.add("facet.date.end", "NOW/DAY"); // facet date range ends for current date (gap period)
                solrQuery.add("facet.date.gap", "+10YEAR"); // gap interval of 10 years
                solrQuery.add("facet.date.other", "before"); // include counts before the facet start date ("before" label)
                solrQuery.add("facet.date.include", "lower"); // counts will be included for dates on the starting date but not ending date
            } else if(facet.equals("uncertainty")){
                Map<String, String> rangeMap = RangeBasedFacets.getRangeMap("uncertainty");
                for(String range: rangeMap.keySet()){
                    solrQuery.add("facet.query", range);
                }
            }
            else {
                solrQuery.addFacetField(facet);
            }
        }
//        solrQuery.addFacetField("basis_of_record");
//        solrQuery.addFacetField("type_status");
//        solrQuery.addFacetField("institution_code_name");
//        //solrQuery.addFacetField("data_resource");
//        solrQuery.addFacetField("state");
//        solrQuery.addFacetField("biogeographic_region");
//        solrQuery.addFacetField("rank");
//        solrQuery.addFacetField("kingdom");
//        solrQuery.addFacetField("family");
//        solrQuery.addFacetField("assertions");
//        //solrQuery.addFacetField("data_provider");
//        solrQuery.addFacetField("month");
//        solrQuery.add("f.month.facet.sort","index"); // sort by Jan-Dec
//        // Date Facet Params
//        // facet.date=occurrence_date&facet.date.start=1900-01-01T12:00:00Z&facet.date.end=2010-01-01T12:00:00Z&facet.date.gap=%2B1YEAR
//        solrQuery.add("facet.date","occurrence_date");
//        solrQuery.add("facet.date.start", "1850-01-01T12:00:00Z"); // facet date range starts from 1850
//        solrQuery.add("facet.date.end", "NOW/DAY"); // facet date range ends for current date (gap period)
//        solrQuery.add("facet.date.gap", "+10YEAR"); // gap interval of 10 years
//        solrQuery.add("facet.date.other", "before"); // include counts before the facet start date ("before" label)
//        solrQuery.add("facet.date.include", "lower"); // counts will be included for dates on the starting date but not ending date
        //Manually add the integer ranges for the facet query required on the confidence field
        //TODO DO we need confidence/ Indivdual Sightings etc
//        for(OccurrenceSource os : OccurrenceSource.values()){
//            solrQuery.add("facet.query", "confidence:" + os.getRange());
//        }

        //solrQuery.add("facet.date.other", "after");

        solrQuery.setFacetMinCount(1);
        solrQuery.setFacetLimit(searchParams.getFlimit());
        if(searchParams.getFlimit() == -1)
            solrQuery.setFacetSort("count");
        solrQuery.setRows(10);
        solrQuery.setStart(0);

//        add highlights
//        solrQuery.setHighlight(true);
//        solrQuery.setHighlightFragsize(40);
//        solrQuery.setHighlightSnippets(1);
//        solrQuery.setHighlightSimplePre("<b>");
//        solrQuery.setHighlightSimplePost("</b>");
//        solrQuery.addHighlightField("commonName");

        return solrQuery;
    }

    /**
     * Get a distinct list of species and their counts using a facet search
     *
     * @param queryString
     * @param pageSize
     * @param sortField
     * @param sortDirection
     * @return
     * @throws SolrServerException
     */
    protected List<TaxaCountDTO> getSpeciesCounts(String queryString, List<String> filterQueries, List<String> facetFields, Integer pageSize,
            Integer startIndex, String sortField, String sortDirection) throws SolrServerException {

        List<TaxaCountDTO> speciesCounts = new ArrayList<TaxaCountDTO>();
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQueryType("standard");
        solrQuery.setQuery(queryString);
        
        if (filterQueries != null && filterQueries.size()>0) {
            solrQuery.addFilterQuery("(" + StringUtils.join(filterQueries, " OR ") + ")");
        }
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        solrQuery.setFacetSort(sortField);
        for (String facet : facetFields) {
            solrQuery.addFacetField(facet);
            logger.debug("adding facetField: " + facet);
        }
        solrQuery.setFacetMinCount(1);
        solrQuery.setFacetLimit(-1); // unlimited = -1 | pageSize
        logger.debug("getSpeciesCount query :" + solrQuery.getQuery());
        QueryResponse qr = runSolrQuery(solrQuery, null, 1, 0, "score", sortDirection);
        logger.info("SOLR query: " + solrQuery.getQuery() + "; total hits: " + qr.getResults().getNumFound());
        List<FacetField> facets = qr.getFacetFields();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\|");

        if (facets != null && facets.size() > 0) {
            logger.debug("Facets: " + facets.size() + "; facet #1: " + facets.get(0).getName());
            for (FacetField facet : facets) {
                List<FacetField.Count> facetEntries = facet.getValues();
                if ((facetEntries != null) && (facetEntries.size() > 0)) {
                    //for (int i = 0; i < facetEntries.size(); i++) {
                    int highestEntry = (pageSize < 0) ? facetEntries.size() : startIndex + pageSize;
                    int lastEntry = (highestEntry > facetEntries.size()) ? facetEntries.size() : highestEntry;
                    logger.debug("highestEntry = " + highestEntry + ", facetEntries.size = " + facetEntries.size() + ", lastEntry = " + lastEntry);
                    for (int i = startIndex; i < lastEntry; i++) {
                        FacetField.Count fcount = facetEntries.get(i);
                        //speciesCounts.add(i, new TaxaCountDTO(fcount.getName(), fcount.getCount()));
                        TaxaCountDTO tcDTO = null;
                        if (fcount.getFacetField().getName().equals(NAMES_AND_LSID)) {
                            String[] values = p.split(fcount.getName());
                            tcDTO = new TaxaCountDTO(values[0], fcount.getCount());
                            if (values.length >= 5) {
                                tcDTO.setGuid(StringUtils.trimToNull(values[1]));
                                tcDTO.setCommonName(values[2]);
                                tcDTO.setKingdom(values[3]);
                                tcDTO.setFamily(values[4]);
                            }
                            //speciesCounts.add(i, tcDTO);
                            speciesCounts.add(tcDTO);
                        }
                        //I think that the code below is obsolete.
                       /* else if (fcount.getFacetField().getName().equals(TAXON_CONCEPT_LSID)) {
                        tcDTO = new TaxaCountDTO();
                        tcDTO.setGuid(StringUtils.trimToNull(fcount.getName()));
                        tcDTO.setCount(fcount.getCount());
                        speciesCounts.add(tcDTO);
                        }
                        else{
                        //leave the original code for findAllKingdomsByCircleArea method
                        try {
                        tcDTO = speciesCounts.get(i);
                        tcDTO.setGuid(StringUtils.trimToNull(fcount.getName()));
                        //speciesCounts.set(i, tcDTO);
                        speciesCounts.add(tcDTO);
                        } catch (Exception e) {
                        tcDTO = new TaxaCountDTO(fcount.getName(), fcount.getCount());
                        //speciesCounts.add(i, tcDTO);
                        speciesCounts.add(tcDTO);
                        }
                        }*/
                    }
                }
            }
        }

        return speciesCounts;
    }

    /**
     * Obtains a list and facet count of the source uids for the supplied query.
     *
     * IS THIS NECESSARY
     *
     * @param query
     * @param filterQuery
     * @return
     * @throws Exception
     */
    public Map<String, Integer> getSourcesForQuery(String query, String[] filterQuery) throws Exception {

        Map<String, Integer> uidStats = new HashMap<String, Integer>();
//        SolrQuery solrQuery = new SolrQuery();
//        solrQuery.setQuery(formatSearchQuery(query));
//        solrQuery.setQueryType("standard");
//        solrQuery.setRows(0);
//        solrQuery.setFacet(true);
//        solrQuery.setFacetMinCount(1);
//        solrQuery.addFacetField("data_provider_uid");
//        solrQuery.addFacetField("data_resource_uid");
//        solrQuery.addFacetField("collection_uid");
//        solrQuery.addFacetField("institution_uid");
//        QueryResponse qr = runSolrQuery(solrQuery, filterQuery, 1, 0, "score", "asc");
//        //now cycle through and get all the facets
//        List<FacetField> facets = qr.getFacetFields();
//        for (FacetField facet : facets) {
//            if (facet.getValues() != null) {
//                for (FacetField.Count ffc : facet.getValues()) {
//                    uidStats.put(ffc.getName(), new Integer((int) ffc.getCount()));
//                }
//            }
//        }
        return uidStats;
    }

    public String getSolrHome() {
        return solrHome;
    }

    public void setSolrHome(String solrHome) {
        this.solrHome = solrHome;
    }
}
