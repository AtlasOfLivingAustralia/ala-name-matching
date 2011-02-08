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
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;

import org.ala.biocache.dto.DataProviderCountDTO;
import org.ala.biocache.dto.FacetResultDTO;
import org.ala.biocache.dto.FieldResultDTO;
import org.ala.biocache.dto.OccurrenceDTO;
import org.ala.biocache.dto.OccurrencePoint;
import org.ala.biocache.dto.OccurrenceSource;
import org.ala.biocache.dto.PointType;
import org.ala.biocache.dto.SearchResultDTO;
import org.ala.biocache.dto.TaxaCountDTO;
import org.ala.biocache.dto.TaxaRankCountDTO;
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
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.springframework.stereotype.Component;
import au.org.ala.biocache.OccurrenceIndex;

import au.com.bytecode.opencsv.CSVWriter;

import com.ibm.icu.text.SimpleDateFormat;

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
    protected EmbeddedSolrServer server;
    /** Limit search results - for performance reasons */
    protected static final Integer MAX_DOWNLOAD_SIZE = 15000;
    protected static final String POINT = "point-0.1";
    protected static final String KINGDOM = "kingdom";
    protected static final String KINGDOM_LSID = "kingdom_lsid";
    protected static final String SPECIES = "species";
    protected static final String SPECIES_LSID = "species_lsid";
    protected static final String NAMES_AND_LSID = "names_and_lsid";
    protected static final String TAXON_CONCEPT_LSID = "taxon_concept_lsid";
    
    /**
     * Initialise the SOLR server instance
     */
    public SearchDAOImpl() {
        if (this.server == null & solrHome != null) {
            try {
                System.setProperty("solr.solr.home", solrHome);
                CoreContainer.Initializer initializer = new CoreContainer.Initializer();
                CoreContainer coreContainer = initializer.initialize();
                server = new EmbeddedSolrServer(coreContainer, "");
            } catch (Exception ex) {
                logger.error("Error initialising embedded SOLR server: "+ex.getMessage(), ex);
            }
        }
    }
    
    /**
     * @see org.ala.biocache.dao.SearchDAO#addOccurrence(org.ala.biocache.model.RawOccurrenceRecord, org.ala.biocache.model.OccurrenceRecord)
     * IS THIS BEIGN USED
     */
	public boolean addOccurrence(OccurrenceDTO oc) throws Exception {
//		//TODO Do we need to add a
//		SolrInputDocument doc = new SolrInputDocument();
//		doc.addField("id", oc.getId());
//		doc.addField("taxon_concept_lsid", oc.getTaxonConceptLsid());
//		doc.addField("taxon_name", oc.getRawTaxonName());
//		doc.addField("common_name", oc.getCommonName());
//		if(oc.getBasisOfRecord()!=null) doc.addField("basis_of_record", oc.getBasisOfRecord());
//		if(oc.getLatitude()!=null) doc.addField("latitude", oc.getLatitude());
//		if(oc.getLongitude()!=null) doc.addField("longitude", oc.getLongitude());
//		if(oc.getPoint1()!=null) doc.addField("point-1", oc.getPoint1());
//		if(oc.getPoint01()!=null) doc.addField("point-0.1", oc.getPoint01());
//		if(oc.getPoint001()!=null) doc.addField("point-0.01", oc.getPoint001());
//		if(oc.getPoint0001()!=null) doc.addField("point-0.001", oc.getPoint0001());
//		if(oc.getPoint00001()!=null) doc.addField("point-0.0001", oc.getPoint00001());
//		if(oc.getDataProviderUid()!=null) doc.addField("data_provider_uid", oc.getDataProviderUid());
//		if(oc.getDataProviderUid()!=null) doc.addField("data_resource_uid", oc.getDataResourceUid());
//		if(oc.getDataProviderUid()!=null) doc.addField("data_provider", oc.getDataProvider());
//		if(oc.getDataProviderUid()!=null) doc.addField("data_resource", oc.getDataResource());
//		if(oc.getOccurrenceDate()!=null) doc.addField("occurrence_date", oc.getOccurrenceDate());
//		if(oc.getYear()!=null) doc.addField("year", oc.getYear());
//		if(oc.getMonth()!=null) doc.addField("month", oc.getMonth());
//		if(oc.getCoordinatePrecision()!=null) doc.addField("lat_long_precision", oc.getCoordinatePrecision());
//		if(oc.getState()!=null) doc.addField("state", oc.getState());
//		if(oc.getRank()!=null) doc.addField("rank", oc.getRank());
//		if(oc.getRankId()!=null) doc.addField("rank_id", oc.getRankId());
//		if(oc.getNamesLsid()!=null) doc.addField("names_and_lsid", oc.getNamesLsid());
//		if(oc.getLeft()!=null) doc.addField("lft", oc.getLeft());
//		if(oc.getRight()!=null) doc.addField("rgt", oc.getRight());
//
//		if(oc.getKingdom()!=null) doc.addField("kingdom", oc.getKingdom());
//		if(oc.getPhylum()!=null) doc.addField("phylum", oc.getPhylum());
//		if(oc.getClass()!=null) doc.addField("class", oc.getClazz());
//		if(oc.getOrder()!=null) doc.addField("order", oc.getOrder());
//		if(oc.getFamily()!=null) doc.addField("family", oc.getFamily());
//		if(oc.getGenus()!=null) doc.addField("genus", oc.getGenus());
//
//		if(oc.getUserId()!=null) doc.addField("user_id", oc.getUserId());
//		if(oc.getCollector()!=null) doc.addField("collector", oc.getCollector());
//		doc.addField("confidence", oc.getConfidence());
//
//		//FIXME we need to lookup the states, ibra regions
//		server.add(doc);
//		server.commit();
		return true;
	}

    /**
     * @see org.ala.biocache.dao.SearchDAO#deleteOccurrence(String)
     * //Probably don't want to delete from here because we need to handle updates to cassandra
     * IS THIS BEING USED
     */
    @Override
	public boolean deleteOccurrence(String occurrenceId) throws Exception {
        // will throw exception if it fails...
//        server.deleteById(occurrenceId);
//        server.commit();
        return true;
    }
    
    /**
     * @see org.ala.biocache.dao.SearchDAO#findByFulltextQuery(java.lang.String, java.lang.String,
     *         java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String)
     */
    @Override
    public SearchResultDTO findByFulltextQuery(String query, String[] filterQuery, Integer startIndex,
            Integer pageSize, String sortField, String sortDirection) throws Exception {
        SearchResultDTO searchResults = new SearchResultDTO();

        try {
            String queryString = formatSearchQuery(query);
            SolrQuery solrQuery = initSolrQuery(); // general search settings
            solrQuery.setQuery(queryString);
            QueryResponse qr = runSolrQuery(solrQuery, filterQuery, pageSize, startIndex, sortField, sortDirection);
            searchResults = processSolrResponse(qr, solrQuery);

            logger.info("search query: "+queryString);
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
    public SearchResultDTO findByFulltextSpatialQuery(String query, String[] filterQuery, Float lat, Float lon,
            Float radius, Integer startIndex, Integer pageSize, String sortField, String sortDirection) throws Exception {
        SearchResultDTO searchResults = new SearchResultDTO();

        try {
            //String queryString = formatSearchQuery(query);
            String queryString = buildSpatialQueryString(formatSearchQuery(query), lat, lon, radius);

            SolrQuery solrQuery = initSolrQuery(); // general search settings
            solrQuery.setQuery(queryString);

            QueryResponse qr = runSolrQuery(solrQuery, filterQuery, pageSize, startIndex, sortField, sortDirection);
            searchResults = processSolrResponse(qr, solrQuery);

            logger.info("spatial search query: "+queryString);
        } catch (SolrServerException ex) {
            logger.error("Problem communicating with SOLR server. " + ex.getMessage(), ex);
            searchResults.setStatus("ERROR"); // TODO also set a message field on this bean with the error message(?)
        }

        return searchResults;
    }
    
    /**
     * @see org.ala.biocache.dao.SearchDAO#writeSpeciesCountByCircleToStream(java.lang.Float, java.lang.Float, java.lang.Integer, java.lang.String, java.util.List, javax.servlet.ServletOutputStream)
     */
    public int writeSpeciesCountByCircleToStream(Float latitude, Float longitude,
            Float radius, String rank, List<String> higherTaxa, ServletOutputStream out) throws Exception
    {

        //get the species counts:
        logger.debug("Writing CSV file for species count by circle");
        List<TaxaCountDTO> species = findAllSpeciesByCircleAreaAndHigherTaxa(latitude, longitude, radius, rank, higherTaxa, null, 0, -1, "count", "asc");
        logger.debug("There are " + species.size() + "records being downloaded");
        CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(out), '\t', '"');
        csvWriter.writeNext(new String[]{
            		"Taxon ID",
                    "Kingdom",
                    "Family",
            		"Scientific name",
            		"Common name",
                    "Record count",
        });
        int count = 0;
        for (TaxaCountDTO item : species) {

            String[] record = new String[]{
                item.getGuid(),
                item.getKingdom(),
                item.getFamily(),
                item.getName(),
                item.getCommonName(),
                item.getCount().toString(),
            };


            csvWriter.writeNext(record);
            csvWriter.flush();
            count++;
        }
        return count;
    }

    /**
     * @see org.ala.biocache.dao.SearchDAO#writeResultsToStream(java.lang.String, java.lang.String[], java.io.OutputStream, int)
     *
     * TODO Handle differently get a list of UUID's to be sent to the biocache-store
     */
    public Map<String,Integer> writeResultsToStream(String query, String[] filterQuery, OutputStream out, int i) throws Exception {

        int resultsCount = 0;
        Map<String,Integer> uidStats = new HashMap<String, Integer>();
        try {
            String queryString = formatSearchQuery(query);
            logger.info("search query: "+queryString);
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQueryType("standard");
            solrQuery.setRows(MAX_DOWNLOAD_SIZE);
            solrQuery.setQuery(queryString);

            int startIndex = 0;
            int pageSize = 1000;
            
            QueryResponse qr = runSolrQuery(solrQuery, filterQuery, pageSize, startIndex, "score", "asc");
//            CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(out), ',', '"');
//
//            //should these be darwin core terms??
//            csvWriter.writeNext(new String[]{
//            		"Record ID",
//            		"Taxon ID",
//            		"Original taxon name",
//                    "Supplied common name",
//            		"Recognised taxon name",
//            		"Taxon rank",
//                    "Common name",
//            		"Family",
//            		"Latitude",
//            		"Longitude",
//            		"Coordinate Precision (metres)",
//            		"Locality",
//            		"Bio region",
//            		"Basis of record",
//            		"State/Territory",
//            		"Event date",
//            		"Event time",
//            		"Collection code",
//            		"Institution code",
//            		"Collector/observer",
//            		"Catalogue number",
//            		"Data provider",
//            		"Data resource",
//            		"Identifier name",
//            		"Citation",
//            });
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            List<String> uuids = new ArrayList<String>();
            while(qr.getResults().size()>0 && resultsCount<MAX_DOWNLOAD_SIZE){
            	logger.debug("Start index: "+startIndex);
	            List<OccurrenceIndex> results = qr.getBeans(OccurrenceIndex.class);
	            for(OccurrenceIndex result : results){
	            	resultsCount++;
                        uuids.add(result.getUuid());
                        //au.org.ala.biocache.FullRecord[] fullResult = au.org.ala.biocache.Store.getAllVersionsByUuid(result.getUuid());
                        //Get the record from the biocache-store
	            	
//	            	String latitude = null, longitude = null;
//
//	            	if(result.getLatitude()!=null){
//	            		latitude = result.getLatitude().toString();
//	            	}
//	            	if(result.getLatitude()!=null){
//	            		longitude = result.getLongitude().toString();
//	            	}
//
//	            	String eventDate = "";
//	            	String eventTime = "";
//	            	if(result.getOccurrenceDate()!=null){
//	            		eventDate = dateFormat.format(result.getOccurrenceDate());
//	            		eventTime = timeFormat.format(result.getOccurrenceDate());
//	            		if("00:00".equals(eventTime)){
//	            			eventTime = "";
//	            		}
//	            	}
	            	
//	            	String[] record = new String[]{
//            			result.getUuid(),
//            			result.getTaxonConceptLsid(),
//            			result.getRawTaxonName(),
//                        result.getRawCommonName(),
//            			result.getTaxonName(),
//            			result.getRank(),
//            			result.getCommonName(),
//            			result.getFamily(),
//            			latitude,
//            			longitude,
//            			result.getCoordinatePrecision(),
//            			result.getPlace(),
//            			result.getBiogeographicRegion(),
//            			result.getBasisOfRecord(),
//            			result.getState(),
//            			eventDate,
//            			eventTime,
//            			result.getCollectionCode(),
//            			result.getInstitutionCode(),
//            			result.getCollector(),
//            			result.getCatalogueNumber(),
//            			result.getDataProvider(),
//            			result.getDataResource(),
//            			result.getIdentifierName(),
//            			result.getCitation(),
//	            	};
	            	
                     //increment the counters....
                    incrementCount(uidStats, result.getInstitutionUid());
                    incrementCount(uidStats, result.getCollectionUid());
                    incrementCount(uidStats, result.getDataProviderUid());
                    incrementCount(uidStats, result.getDataResourceUid());
                        
	            	//csvWriter.writeNext(record);
	            	//csvWriter.flush();
	            }
                    au.org.ala.biocache.Store.writeToStream(out, "\t", "\n", uuids.toArray(new String[]{}),
                                            new String[]{"uuid",
                                                         "taxonConceptID.p",
                                                         "scientificName",
                                                         "vernacularName",
                                                         "scientificName.p" ,
                                                         "taxonRank.p", "vernacularName.p", "family.p", "latitude.p", "longitude.p", "coordinatePrecision" });
	            startIndex += pageSize;
	            if(resultsCount<MAX_DOWNLOAD_SIZE){
	            	qr = runSolrQuery(solrQuery, filterQuery, pageSize, startIndex, "score", "asc");
	            }
            }
               
        } catch (SolrServerException ex) {
            logger.error("Problem communicating with SOLR server. " + ex.getMessage(), ex);
            //searchResults.setStatus("ERROR"); // TODO also set a message field on this bean with the error message(?)
        }
        return uidStats;
    }

    /**
     * @see org.ala.biocache.dao.SearchDAO#writeResultsToStream(java.lang.String, java.lang.String[], java.io.OutputStream, int, Float, Float, Integer)
     */
    @Override
    public Map<String,Integer> writeResultsToStream(String query, String[] filterQuery, OutputStream out, int i, Float lat, Float lon, Integer rad) throws Exception {
        Float radius = Float.parseFloat(rad + "f");
        String queryString = buildSpatialQueryString(formatSearchQuery(query), lat, lon, radius);
        return writeResultsToStream(queryString, filterQuery, out, i);
    }
    
    private void incrementCount(Map<String,Integer> values, String uid){
        if(uid != null){
            Integer count = values.containsKey(uid)?values.get(uid):0;
            count++;
            values.put(uid, count);
        }
    }
    
    /**
     * @see org.ala.biocache.dao.SearchDAO#getById(java.lang.String)
     * REMOVE THIS METHOD All references to this should be replaced byStore.getByUUID
     */
    @Override
    public OccurrenceDTO getById(String id) throws Exception {
        OccurrenceDTO oc = null;
        
//        String query = "id:"+ClientUtils.escapeQueryChars(id);
//        SolrQuery solrQuery = new SolrQuery();
//        solrQuery.setQueryType("standard");
//        solrQuery.setQuery(query);
//        QueryResponse qr = runSolrQuery(solrQuery, null, 1, 0, "score", "asc");
//        SearchResultDTO searchResults = processSolrResponse(qr, solrQuery);
//        List<OccurrenceIndex> ocs = searchResults.getOccurrences();
//
//        if (!ocs.isEmpty() && ocs.size() == 1) {
//            oc = ocs.get(0);
//        } else if (!ocs.isEmpty()) {
//            logger.warn("Get by id returned more than ONE result: "+ocs.size()+" for id: "+id);
//        }

        return oc;
    }
   
    /**
     * @see org.ala.biocache.dao.SearchDao#getFacetPoints(java.lang.String, java.lang.String[], PointType pointType)
     */
    @Override
    public List<OccurrencePoint> getFacetPoints(String query, String[] filterQuery, PointType pointType) throws Exception {
        List<OccurrencePoint> points = new ArrayList<OccurrencePoint>(); // new OccurrencePoint(PointType.POINT);
        String queryString = formatSearchQuery(query);
        logger.info("search query: "+queryString);
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQueryType("standard");
        solrQuery.setQuery(queryString);
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        solrQuery.addFacetField(pointType.getLabel());
        solrQuery.setFacetMinCount(1);
        solrQuery.setFacetLimit(MAX_DOWNLOAD_SIZE);  // unlimited = -1

        QueryResponse qr = runSolrQuery(solrQuery, filterQuery, 1, 0, "score", "asc");
        List<FacetField> facets = qr.getFacetFields();

        if (facets != null) {
            for (FacetField facet : facets) {
                List<FacetField.Count> facetEntries = facet.getValues();
                if (facet.getName().contains(pointType.getLabel()) && (facetEntries != null) && (facetEntries.size() > 0)) {

                    for (FacetField.Count fcount : facetEntries) {
                        OccurrencePoint point = new OccurrencePoint(pointType);
                        point.setCount(fcount.getCount());
                        String[] pointsDelimited = StringUtils.split(fcount.getName(),',');
                        List<Float> coords = new ArrayList<Float>();

                        for (String coord : pointsDelimited) {
                            try {
                                Float decimalCoord = Float.parseFloat(coord);
                                coords.add(decimalCoord);
                            } catch (NumberFormatException numberFormatException) {
                                logger.warn("Error parsing Float for Lat/Long: "+numberFormatException.getMessage(), numberFormatException);
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

        if (facets != null && facets.size()==2) {
        	
        	FacetField dataProviderIdFacet = facets.get(0);
        	FacetField dataProviderNameFacet = facets.get(1);
        	
        	List<FacetField.Count> dpIdEntries = dataProviderIdFacet.getValues();
        	List<FacetField.Count> dpNameEntries = dataProviderNameFacet.getValues();
        	
            for (int i=0; i<dpIdEntries.size(); i++) {
            	
            	FacetField.Count dpIdEntry = dpIdEntries.get(i);
            	FacetField.Count dpNameEntry = dpNameEntries.get(i);
            	
            	String dataProviderId = dpIdEntry.getName();
            	String dataProviderName = dpNameEntry.getName();
            	long count = dpIdEntry.getCount();
            	
            	DataProviderCountDTO dto = new DataProviderCountDTO(dataProviderId, dataProviderName, count);
            	dpDTOs.add(dto);
            }
        }
        logger.info("Find data providers = "+dpDTOs.size());
        return dpDTOs;
	}
	
	/**
     * @see org.ala.biocache.dao.SearchDAO#findRecordsForLocation(Float, Float, Integer)
     */
    @Override
    public List<OccurrencePoint> findRecordsForLocation(List<String> taxa, String rank, Float latitude, Float longitude, Float radius, PointType pointType) throws Exception {
        List<OccurrencePoint> points = new ArrayList<OccurrencePoint>(); // new OccurrencePoint(PointType.POINT);
        String queryString = buildSpatialQueryString("*:*", latitude, longitude, radius);
        //String queryString = formatSearchQuery(query);
        logger.info("location search query: "+queryString+"; pointType: "+pointType.getLabel());
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQueryType("standard");
        solrQuery.setQuery(queryString);

        ArrayList<String> filterQueries = new ArrayList<String>();
        for (String taxon : taxa) {
            // Don't escape taxon when it is wildcard (*) value as it breaks
            String taxonName = ("*".equals(taxon)) ? taxon : ClientUtils.escapeQueryChars(taxon);
            filterQueries.add(rank + ":" + taxonName);
        }
        
        solrQuery.setFilterQueries("(" + StringUtils.join(filterQueries, " OR ") + ")");
        logger.info("filterQueries: "+solrQuery.getFilterQueries()[0]);
        
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        solrQuery.addFacetField(pointType.getLabel());
        solrQuery.setFacetMinCount(1);
        solrQuery.setFacetLimit(MAX_DOWNLOAD_SIZE);  // unlimited = -1

        QueryResponse qr = runSolrQuery(solrQuery, null, 1, 0, "score", "asc");
        logger.info("qr number found: "+qr.getResults().getNumFound());
        List<FacetField> facets = qr.getFacetFields();

        if (facets != null) {
            for (FacetField facet : facets) {
                List<FacetField.Count> facetEntries = facet.getValues();
                if (facet.getName().contains(pointType.getLabel()) && (facetEntries != null) && (facetEntries.size() > 0)) {

                    for (FacetField.Count fcount : facetEntries) {
                        OccurrencePoint point = new OccurrencePoint(pointType);
                        point.setCount(fcount.getCount());
                        String[] pointsDelimited = StringUtils.split(fcount.getName(),',');
                        List<Float> coords = new ArrayList<Float>();

                        for (String coord : pointsDelimited) {
                            try {
                                Float decimalCoord = Float.parseFloat(coord);
                                coords.add(decimalCoord);
                            } catch (NumberFormatException numberFormatException) {
                                logger.warn("Error parsing Float for Lat/Long: "+numberFormatException.getMessage(), numberFormatException);
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
        logger.info("findRecordsForLocation: number of points = "+points.size());

        return points;
    }

    @Override
    //TODO: Not storing/indexing a user id
    //IS this being used
    public List<TaxaCountDTO> findTaxaByUserId(String userId) throws Exception {
        
        String queryString = "user_id:"+ClientUtils.escapeQueryChars(userId);
        List<String> facetFields = new ArrayList<String>();
        facetFields.add(TAXON_CONCEPT_LSID);
        List<TaxaCountDTO> speciesWithCounts = getSpeciesCounts(queryString, null, facetFields, 1000, 0, "taxon_name", "asc");

        return speciesWithCounts;
    }
    
    @Override
    //TODO Not storing/indexing a user
    //IS this being used
    public List<OccurrenceIndex> findPointsForUserId(String userId) throws Exception {
        String query = "user_id:"+ClientUtils.escapeQueryChars(userId);
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQueryType("standard");
        solrQuery.setQuery(query);
        QueryResponse qr = runSolrQuery(solrQuery, null, 1000000, 0, "score", "asc");
        SearchResultDTO searchResults = processSolrResponse(qr, solrQuery);
        logger.debug("solr result (size): "+searchResults.getOccurrences().size());

        return searchResults.getOccurrences();
    }

    /**
     * @see org.ala.biocache.dao.SearchDAO#findAllSpeciesByCircleAreaAndHigherTaxa(Float, Float,
     *     Integer, String, String, String, Integer, Integer, String, String)
     * IS THIS BEING USED??
     */
    @Override
    public List<TaxaCountDTO> findAllSpeciesByCircleAreaAndHigherTaxon(Float latitude, Float longitude,
            Float radius, String rank, String higherTaxon, String filterQuery, Integer startIndex,
            Integer pageSize, String sortField, String sortDirection) throws Exception {

        String queryString = buildSpatialQueryString("*:*", latitude, longitude, radius);
        List<String> filterQueries = Arrays.asList(rank + ":" + higherTaxon);
        List<String> facetFields = new ArrayList<String>();
        
        facetFields.add(NAMES_AND_LSID);
        List<TaxaCountDTO> speciesWithCounts = getSpeciesCounts(queryString, filterQueries, facetFields, pageSize, startIndex, sortField, sortDirection);

        return speciesWithCounts;
    }

    /**
     * @see org.ala.biocache.dao.SearchDAO#findAllSpeciesByCircleAreaAndHigherTaxa(Float, Float,
     *     Integer, String, String, String, Integer, Integer, String, String)
     */
    @Override
    public List<TaxaCountDTO> findAllSpeciesByCircleAreaAndHigherTaxa(Float latitude, Float longitude,
            Float radius, String rank, List<String> higherTaxa, String filterQuery, Integer startIndex,
            Integer pageSize, String sortField, String sortDirection) throws Exception {

        ArrayList<String> filterQueries = new ArrayList<String>();
        
        for (String higherTaxon : higherTaxa) {
            filterQueries.add(rank + ":" + higherTaxon);
        }
        
        String queryString = buildSpatialQueryString("*:*", latitude, longitude, radius);
        List<String> facetFields = new ArrayList<String>();
        facetFields.add(NAMES_AND_LSID);
        List<TaxaCountDTO> speciesWithCounts = getSpeciesCounts(queryString, filterQueries, facetFields, pageSize, startIndex, sortField, sortDirection);

        return speciesWithCounts;
    }


    
    /**
     * @see org.ala.biocache.dao.SearchDAO#findRecordByDecadeFor(java.lang.String)
     */
	@Override
	public List<FieldResultDTO> findRecordByDecadeFor(String query) throws Exception {
        List<FieldResultDTO> fDTOs = new ArrayList<FieldResultDTO>(); // new OccurrencePoint(PointType.POINT);
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQueryType("standard");
        solrQuery.add("facet.date","occurrence_date");
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
        
        for(Count count : ff.getValues()){
        	//only start adding counts when we hit a decade with some results.
        	if(addCounts || count.getCount()>0){
        		addCounts = true;
        		Date date = DateUtils.parseDate(count.getName(), new String[]{"yyyy-MM-dd'T'HH:mm:ss'Z'"});
        		String year = sdf.format(date);
        		FieldResultDTO f = new FieldResultDTO(year, count.getCount());
        		fDTOs.add(f);
        		System.out.println(f.getLabel()+" "+f.getCount() );
        	}
        }
        return fDTOs;
	}

	/**
	 * @see org.ala.biocache.dao.SearchDAO#findRecordByStateFor(java.lang.String)
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
        if(ff!=null){
	        for(Count count : ff.getValues()){
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
    public TaxaRankCountDTO findTaxonCountForUid(String query, int maximumFacets) throws Exception {
        logger.info("Attempting to find the counts for " + query);
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQueryType("standard");
        solrQuery.setQuery(query);
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        TaxaRankCountDTO trDTO = null;
        for (int i = 5; i > 0 && trDTO == null; i--) {
            String ffname = SearchUtils.getRankFacetName(i);
            solrQuery.addFacetField(ffname);
            solrQuery.setFacetMinCount(1);
            QueryResponse qr = runSolrQuery(solrQuery, null, 1, 0, ffname, "asc");
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
    public TaxaRankCountDTO findTaxonCountForUid(String query, String rank, boolean includeSuppliedRank) throws Exception{
        TaxaRankCountDTO trDTO = null;
        List<String> ranks = SearchUtils.getNextRanks(rank, includeSuppliedRank);
        if(ranks != null && ranks.size()>0){
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQueryType("standard");
            solrQuery.setQuery(query);
            solrQuery.setRows(0);
            solrQuery.setFacet(true);
            solrQuery.setFacetMinCount(1);
            for(String r :ranks){
                solrQuery.addFacetField(r);
            }
            QueryResponse qr = runSolrQuery(solrQuery, null, 1, 0, rank, "asc");
            if(qr.getResults().size()>0){
                for(String r : ranks){
                    trDTO = new TaxaRankCountDTO(r);
                    FacetField ff = qr.getFacetField(r);
                    if(ff != null){
                        List<Count> counts = ff.getValues();
                        if(counts.size()>0){
                            List<FieldResultDTO> fDTOs = new ArrayList<FieldResultDTO>();
                            for(Count count: counts){
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
    private QueryResponse runSolrQuery(SolrQuery solrQuery, String filterQuery[], Integer pageSize,
            Integer startIndex, String sortField, String sortDirection) throws SolrServerException {

        if (filterQuery != null) {
            for (String fq : filterQuery) {
                // pull apart fq. E.g. Rank:species and then sanitize the string parts
                // so that special characters are escaped apporpriately
                if (fq.isEmpty()) continue;
                String[] parts = fq.split(":", 2); // separate query field from query text
                logger.debug("fq split into: "+parts.length+" parts: "+parts[0]+" & "+parts[1]);
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

                solrQuery.addFilterQuery(prefix + ":" + suffix); // solrQuery.addFacetQuery(facetQuery)
                logger.info("adding filter query: " + prefix + ":" + suffix);
            }
        }

        solrQuery.setRows(pageSize);
        solrQuery.setStart(startIndex);
        solrQuery.setSortField(sortField, ORDER.valueOf(sortDirection));
        logger.info("runSolrQuery: "+solrQuery.toString());
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
            logger.debug("Facet dates size: "+facetDates.size());
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
        logger.debug("sortField post-split: "+StringUtils.join(solrSort, "|"));
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
        return searchResult;
    }

    /**
     * Build the query string for a spatial query (using Spatial-Solr plugin syntax)
     *
     * @param fullTextQuery
     * @param latitude
     * @param longitude
     * @param radius
     * @return
     */
    protected String buildSpatialQueryString(String fullTextQuery, Float latitude, Float longitude, Float radius) {
        String queryString = "{!spatial lat=" + latitude.toString() + " long=" + longitude.toString() +
                " radius=" + radius.toString() + " unit=km calc=arc threadCount=2}" + fullTextQuery; // calc=arc|plane
        return queryString;
    }

    /**
     * Format the search input query for a full-text search
     *
     * @param query
     * @return
     */
    protected String formatSearchQuery(String query) {
        // set the query
        StringBuilder queryString = new StringBuilder();
        if (query.equals("*:*") || query.contains(" AND ") || query.contains(" OR ") || query.startsWith("(") 
                || query.endsWith("*") || query.startsWith("{")) {
            queryString.append(query);
        } else if (query.contains(":") && !query.startsWith("urn")) {
            // search with a field name specified (other than an LSID guid)
            String[] bits = StringUtils.split(query, ":", 2);
            queryString.append(ClientUtils.escapeQueryChars(bits[0]));
            queryString.append(":");
            queryString.append(ClientUtils.escapeQueryChars(bits[1]));
        } else {
            // regular search
            queryString.append(ClientUtils.escapeQueryChars(query));
        }
        return queryString.toString();
    }
   
    /**
     * Helper method to create SolrQuery object and add facet settings
     *
     * @return solrQuery the SolrQuery
     */
    protected SolrQuery initSolrQuery() {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQueryType("standard");
        // Facets
        solrQuery.setFacet(true);
        solrQuery.addFacetField("basis_of_record");
        solrQuery.addFacetField("type_status");
        solrQuery.addFacetField("data_resource");
        solrQuery.addFacetField("state");
        solrQuery.addFacetField("biogeographic_region");
        solrQuery.addFacetField("rank");
        solrQuery.addFacetField("kingdom");
        solrQuery.addFacetField("family");
        solrQuery.addFacetField("assertions");
        //solrQuery.addFacetField("data_provider");
        solrQuery.addFacetField("month");
        solrQuery.add("f.month.facet.sort","index"); // sort by Jan-Dec
        // Date Facet Params
        // facet.date=occurrence_date&facet.date.start=1900-01-01T12:00:00Z&facet.date.end=2010-01-01T12:00:00Z&facet.date.gap=%2B1YEAR
        solrQuery.add("facet.date","occurrence_date");
        solrQuery.add("facet.date.start", "1850-01-01T12:00:00Z"); // facet date range starts from 1850
        solrQuery.add("facet.date.end", "NOW/DAY"); // facet date range ends for current date (gap period)
        solrQuery.add("facet.date.gap", "+10YEAR"); // gap interval of 10 years
        solrQuery.add("facet.date.other", "before"); // include counts before the facet start date ("before" label)
        solrQuery.add("facet.date.include", "lower"); // counts will be included for dates on the starting date but not ending date
        //Manually add the integer ranges for the facet query required on the confidence field
        //TODO DO we need confidence/ Indivdual Sightings etc
//        for(OccurrenceSource os : OccurrenceSource.values()){
//            solrQuery.add("facet.query", "confidence:" + os.getRange());
//        }
        
        //solrQuery.add("facet.date.other", "after");

        solrQuery.setFacetMinCount(1);
        solrQuery.setFacetLimit(30);
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
        if (filterQueries != null) solrQuery.addFilterQuery("(" + StringUtils.join(filterQueries, " OR ") + ")");
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        solrQuery.setFacetSort(sortField);
        for (String facet : facetFields) {
            solrQuery.addFacetField(facet);
            logger.debug("adding facetField: "+facet);
        }
        solrQuery.setFacetMinCount(1);
        solrQuery.setFacetLimit(-1); // unlimited = -1 | pageSize
        QueryResponse qr = runSolrQuery(solrQuery, null, 1, 0, "score", sortDirection);
        logger.info("SOLR query: " + solrQuery.getQuery() + "; total hits: " + qr.getResults().getNumFound());
        List<FacetField> facets = qr.getFacetFields();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\|");
        
        if (facets != null && facets.size() > 0) {
            logger.debug("Facets: " +facets.size()+"; facet #1: "+facets.get(0).getName());
            for (FacetField facet : facets) {
                List<FacetField.Count> facetEntries = facet.getValues();
                if ((facetEntries != null) && (facetEntries.size() > 0)) {
                    //for (int i = 0; i < facetEntries.size(); i++) {
                    int highestEntry = (pageSize < 0) ? facetEntries.size() : startIndex + pageSize;
                    int lastEntry = (highestEntry > facetEntries.size()) ? facetEntries.size() : highestEntry;
                    logger.debug("highestEntry = "+highestEntry+", facetEntries.size = "+facetEntries.size()+", lastEntry = "+lastEntry);
                    for (int i = startIndex; i < lastEntry; i++) {
                        FacetField.Count fcount = facetEntries.get(i);
                        //speciesCounts.add(i, new TaxaCountDTO(fcount.getName(), fcount.getCount()));
                        TaxaCountDTO tcDTO = null;
                        if(fcount.getFacetField().getName().equals(NAMES_AND_LSID)){
                            String[] values =p.split(fcount.getName());
                            tcDTO = new TaxaCountDTO(values[0], fcount.getCount());
                            if(values.length>=5){
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
     * @param query
     * @param filterQuery
     * @return
     * @throws Exception
     */
    public Map<String,Integer> getSourcesForQuery(String query, String[] filterQuery) throws Exception{
       
        Map<String,Integer> uidStats = new HashMap<String, Integer>();
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(formatSearchQuery(query));
        solrQuery.setQueryType("standard");
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        solrQuery.addFacetField("data_provider_uid");
        solrQuery.addFacetField("data_resource_uid");
        solrQuery.addFacetField("collection_code_uid");
        solrQuery.addFacetField("institution_code_uid");
        QueryResponse qr = runSolrQuery(solrQuery, filterQuery, 1, 0, "score", "asc");
        //now cycle through and get all the facets
        List<FacetField> facets = qr.getFacetFields();
        for(FacetField facet : facets){
            if(facet.getValues() != null){
                for(FacetField.Count ffc : facet.getValues()){
                    uidStats.put(ffc.getName(), new Integer((int)ffc.getCount()));
                }
            }
        }
        return uidStats;
    }


    public String getSolrHome() {
        return solrHome;
    }

    public void setSolrHome(String solrHome) {
        this.solrHome = solrHome;
    }
}
