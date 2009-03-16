/* *************************************************************************
 *  Copyright (C) 2009 Atlas of Living Australia
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

package org.ala.dao.solr;

import org.ala.dao.OccurrenceFacetDAO;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.ala.model.GeoRegionTaxonConcept;
import org.ala.util.IndexingIssue;
import org.ala.util.IndexingIssueTypes;
import org.ala.web.util.RankFacet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.gbif.portal.dto.taxonomy.TaxonConceptDTO;
import org.gbif.portal.model.occurrence.BasisOfRecord;
import org.gbif.portal.service.TaxonomyManager;
import org.springframework.context.MessageSource;

/**
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class OccurrenceFacetDAOImpl implements OccurrenceFacetDAO {

    protected static Log logger = LogFactory.getLog(OccurrenceFacetDAOImpl.class);

    protected MessageSource messageSource;

    protected TaxonomyManager taxonomyManager;
    
    protected SolrDataHelper solrDataHelper;

    /** Breakdown of indexing data for display as charts (uses Solr) */
    protected String solrUrl = "http://localhost:8080/solr";

    protected SolrServer solrServer = null;

    protected List<String> resourceFacetFields = Arrays.asList(
            "species_concept_id", "taxonomic_issue", "geospatial_issue", "other_issue", "month", "year");

    protected List<String> speciesFacetFields = Arrays.asList(
            "basis_of_record", "month", "year");

    protected List<String> monthName = Arrays.asList("January", "February", "March", "April", "May",
            "June", "July", "August", "September", "October", "November", "December");

    protected Integer startingYear = 1800;

    /**
	 * Produce a CSV String for the requested chart type (AMCharts)
	 *
	 * If there is a problem retrieving aggregated data from SOLR Server, return null
	 *
	 * @param data resource key
	 * @return data to populate the the AM Chart (csv)
	 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
	 *
	 * TODO format data as XML instead of CSV so we can add url parameter for links
	 *
	 * Example Solr URL:
	 * http://localhost:8080/solr/select/?q=data_resource_id:56&facet=true&facet.limit=-1&facet.mincount=0
	 *   &facet.missing=true&facet.field=basisofrecord&facet.field=year&facet=true&facet.sort=false
	 */
	public Map<String, String> getChartFacetsForResource(String dataResourceKey) {
		try{
		    Map<String, String> csvDataMap = new HashMap<String, String>();  // model passed through to JSP
		    // Setup the Solr query, etc
		    String queryString = "data_resource_id:" + dataResourceKey;
		    solrServer = initialiseSolrServer();
	        SolrQuery searchQuery = new SolrQuery();  // handle better?
	        searchQuery.setQuery(queryString);
	        searchQuery.setRows(0); // we don't want any search results only the facet results
	        searchQuery.setFacetLimit(100);  // TODO make -1 (no limit) and cull list after 20 but add to "other" facet
	        searchQuery.setFacetMinCount(1);
	        // temporal charts require special facet parameters
	        searchQuery.setParam("f.year.facet.sort", false);
	        searchQuery.setParam("f.month.facet.sort", false);
	        searchQuery.setParam("f.year.facet.limit", "-1");
	        
	        // add each facet field to the query
	        for (String facet : resourceFacetFields) searchQuery.addFacetField(facet);

	        // do the Solr search
	        QueryResponse rsp = null;
	        try {
	            rsp = solrServer.query( searchQuery );
	        } catch (SolrServerException e) {
	        	logger.warn("Problem communicating with SOLR server. Unable to generate chart data.");
	            logger.debug(e.getMessage(), e);
	            return null;
	        }

	        //Long resultsSize = rsp.getResults().getNumFound();
		    // getForField the facet results (list)
	        Calendar calendar = Calendar.getInstance();
	        int currentYear = calendar.get(Calendar.YEAR);
	        List<FacetField> facetResults = rsp.getFacetFields();

		    for (FacetField facetData : facetResults) {
		        // Iterate over each of the facet result objects
		        String facetName = facetData.getName();
		        StringBuffer csvValues = new StringBuffer(); // CSV format: 'foo;25\nbar;33\ndoh;1\n'
		        Map<String, Long> issueFacetMap = new HashMap<String, Long>(); // needed to redistribute the issue counts after splitting bit values

		        if(facetData==null){
		        	return null;
		        }

		        for (Count count : facetData.getValues()) {
		            // Iterate over the facet counts
		            // TODO evaluate using percentage values for issue types only ?
		            String label = count.getName();
		            long facetCount = count.getCount();

		            if ("year".equals(facetName)) {
		                // skip bad year data
		                Integer numLabel = Integer.parseInt(label);
		                if (numLabel != null && (numLabel < startingYear  || numLabel > currentYear)) continue;
		            }
		            else if ("month".equals(facetName)) {
		                // convert month int to string values (1 -> January)
		                int monthInt = Integer.parseInt(label);
		                label = monthName.get(monthInt-1);
		            }
                    else if (facetName.contains("concept_id")) {
                        // Get a taxon name from taxonConceptId
                        logger.debug("Getting taxon name from taxon concept id: id: "+label);
                        TaxonConceptDTO taxonConceptDTO = taxonomyManager.getTaxonConceptFor(label);
                        label = taxonConceptDTO.getTaxonName();
                    }

		            if (facetName.contains("_issue")) {
		                // Issue types breakdown -> getForField i18n keys for labels
		                int issueTypeValue = Integer.parseInt(label);
		                String issueTypeStr = facetName.replace("_issue", "");  // knock the "_issue" part off the string (other_issue -> other)

		                if (issueTypeValue == 0) {
		                    // special case of "no issue"

		                	String message = messageSource.getMessage(IndexingIssue.NO_ISSUES.getI18nKey(), null, "No issues", Locale.ENGLISH);
		                    csvValues.append(message + ';' + facetCount + "\\n");
		                }
		                else {
		                    List<IndexingIssue> indexingIssues =
		                        IndexingIssue.splitIssuesForCompositeIssueValue(issueTypeValue, IndexingIssueTypes.get(issueTypeStr));

		                    for (IndexingIssue issue : indexingIssues) {
		                        // Distribute the issue counts into the issueFacetMap map
		                        String issueKey = issue.getI18nKey();
	//	                        String message = messageSource.getMessage(issueKey, null, Locale.ENGLISH);
		                        Long previousCount = (issueFacetMap.get(issueKey) != null) ? issueFacetMap.get(issueKey) : 0;
		                        Long newCount = previousCount + facetCount;
		                        issueFacetMap.put(issue.getDescrption(), newCount);
		                    }
		                }
		            }
		            else {
		                // all "other types" data breakdown
		                csvValues.append(label + ';' + facetCount + "\\n");
		            }
		        }

		        // Add each key/value pair from issueFacetMap to csvValues string buffer
		        Set<String> keys = issueFacetMap.keySet();
		        Iterator<String> it = keys.iterator();
		        while (it.hasNext()) {
		            String key = it.next();
		            csvValues.append(key + ';' + issueFacetMap.get(key) + "\\n");
		        }

		        // Add each facet CSV data string to the csvDataMap map
		        csvDataMap.put(facetName, csvValues.toString());
		        logger.debug("getChartData: " + facetName + "->" + csvValues);
		    }
		    return csvDataMap;
		} catch(Exception e){
        	logger.warn("Problem generating chart data. Charts will not be rendered");
            logger.debug(e.getMessage(), e);
		}
		return null;
	}

    /**
	 * Produce a CSV String for the requested chart type (AMCharts) for Species Page
	 *
	 * If there is a problem retrieving aggregated data from SOLR Server, return null
	 *
	 * @param species taxon concept key
	 * @return data Map to populate the the AM Chart (csv)
	 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
	 *
	 * TODO format data as XML instead of CSV so we can add url parameter for links
	 *
	 * Example Solr URL:
	 * http://localhost:8080/solr/select/?q=species_concept_id:2557787&facet=true&facet.limit=-1&facet.mincount=0
	 *   &facet.missing=true&facet.field=basisofrecord&facet.field=year&facet.field=month&facet=true&facet.sort=false
	 */
	public Map<String, String> getChartFacetsForSpecies(String taxonConceptKey) {
		try{
		    Map<String, String> csvDataMap = new HashMap<String, String>();  // model passed through to JSP
		    // Setup the Solr query, etc
		    String queryString = "species_concept_id:" + taxonConceptKey;
		    solrServer = initialiseSolrServer();
	        SolrQuery searchQuery = new SolrQuery();  // handle better?
	        searchQuery.setQuery(queryString);
	        searchQuery.setRows(0); // we don't want any search results only the facet results
	        searchQuery.setFacetLimit(100);  // TODO make -1 (no limit) and cull list after 20 but add to "other" facet
	        searchQuery.setFacetMinCount(1);
	        // temporal charts require special facet parameters
	        searchQuery.setParam("f.year.facet.sort", false);
	        searchQuery.setParam("f.month.facet.sort", false);
	        searchQuery.setParam("f.year.facet.limit", "-1");

	        // add each facet field to the query
	        for (String facet : speciesFacetFields) searchQuery.addFacetField(facet);

	        // do the Solr search
	        QueryResponse rsp = null;
	        try {
	            rsp = solrServer.query( searchQuery );
	        } catch (SolrServerException e) {
	        	logger.warn("Problem communicating with SOLR server. Unable to generate chart data.");
	            logger.debug(e.getMessage(), e);
	            return null;
	        }

	        //Long resultsSize = rsp.getResults().getNumFound();
		    // getForField the facet results (list)
	        Calendar calendar = Calendar.getInstance();
	        int currentYear = calendar.get(Calendar.YEAR);
	        List<FacetField> facetResults = rsp.getFacetFields();

		    for (FacetField facetData : facetResults) {
		        // Iterate over each of the facet result objects
		        String facetName = facetData.getName();
		        StringBuffer csvValues = new StringBuffer(); // CSV format: 'foo;25\nbar;33\ndoh;1\n'
		        Map<String, Long> issueFacetMap = new HashMap<String, Long>(); // needed to redistribute the issue counts after splitting bit values

		        if(facetData==null){
		        	return null;
		        }

		        for (Count count : facetData.getValues()) {
		            // Iterate over the facet counts
		            // TODO evaluate using percentage values for issue types only ?
		            String label = count.getName();
		            long facetCount = count.getCount();

		            if ("year".equals(facetName)) {
		                // skip bad year data
		                Integer numLabel = Integer.parseInt(label);
		                if (numLabel != null && (numLabel < startingYear  || numLabel > currentYear)) continue;
		            }
		            else if ("month".equals(facetName)) {
		                // convert month int to string values (1 -> January)
		                int monthInt = Integer.parseInt(label);
		                label = monthName.get(monthInt-1);
		            }
                    else if ("basis_of_record".equals(facetName)) {
                        // substitute the BoR string value for int value
                        int borInt = Integer.parseInt(label);
                        label = BasisOfRecord.getBasisOfRecord(borInt).getName();
                    }
                    
		            csvValues.append(label + ';' + facetCount + "\\n");
		            
		        }

		        // Add each key/value pair from issueFacetMap to csvValues string buffer
		        Set<String> keys = issueFacetMap.keySet();
		        Iterator<String> it = keys.iterator();
		        while (it.hasNext()) {
		            String key = it.next();
		            csvValues.append(key + ';' + issueFacetMap.get(key) + "\\n");
		        }

		        // Add each facet CSV data string to the csvDataMap map
		        csvDataMap.put(facetName, csvValues.toString());
		        logger.debug("getChartData: " + facetName + "->" + csvValues);
		    }
		    return csvDataMap;
		} catch(Exception e){
        	logger.warn("Problem generating chart data. Charts will not be rendered");
            logger.debug(e.getMessage(), e);
		}
        
		return null;
	}

    /**
     * Return a list of taxon_concept ids for the given geo region and rank
     * @param regionId
     * @param startingRank
     * @return
     */
    public List<GeoRegionTaxonConcept> getTaxonConceptsForGeoRegion(Long regionId, String taxonConceptKey) {
        List<GeoRegionTaxonConcept> geoRegionTaxonConcept = new ArrayList<GeoRegionTaxonConcept> ();

        try {
		    RankFacet rank = null;
            String facetQuery =  null;

            if (taxonConceptKey != null) {
                TaxonConceptDTO taxonConceptDTO = taxonomyManager.getTaxonConceptFor(taxonConceptKey);
                rank = RankFacet.getForField(taxonConceptDTO.getRank());
                facetQuery = rank.getFacetField() + ":" + taxonConceptKey;
                // set rank to the next level for the facets
                rank = RankFacet.getForId(rank.getId() + 1000);
            }
            else {
                // Default starting rank to browse
                rank = RankFacet.FAMILY;
            }
		    // Setup the Solr query, etc
		    String queryString = "geo_region_id:" + regionId;
		    solrServer = initialiseSolrServer();
	        SolrQuery searchQuery = new SolrQuery(); 
            // Load up search params
	        searchQuery.setQuery(queryString);
	        searchQuery.setRows(0); // we don't want any search results only the facet results
	        searchQuery.setFacetLimit(100);  // TODO make -1 (no limit) and cull list after 20 but add to "other" facet
	        searchQuery.setFacetMinCount(1);
            // searchQuery.setFacetSort(false); // sorts by XX_concept_id field 
	        
            searchQuery.addFacetField(rank.getFacetField());

            if (facetQuery != null)
                searchQuery.setFilterQueries(facetQuery);
	        // add each facet field to the query
	        //for (String facet : resourceFacetFields) searchQuery.addFacetField(facet);

	        // do the Solr search
	        QueryResponse rsp = null;
	        try {
	            rsp = solrServer.query( searchQuery );
	        } catch (SolrServerException e) {
	        	logger.warn("Problem communicating with SOLR server. Unable to generate chart data.");
	            logger.debug(e.getMessage(), e);
	            return null;
	        }

	        List<FacetField> facetResults = rsp.getFacetFields();

            for (FacetField facetData : facetResults) {
		        // Iterate over each of the facet result objects
		        String facetName = facetData.getName();

		        if (facetData == null) {
		        	return geoRegionTaxonConcept; // will be empty list
		        }
		        
		        List<Long> conceptIds = new ArrayList<Long>();
		        for (Count count : facetData.getValues()) {
		        	String taxonConcept = count.getName();
                    Long taxonConceptId = Long.parseLong(taxonConcept);
		        	conceptIds.add(taxonConceptId);
		        }
		        
		        List<Map<String, Object>> conceptNames = solrDataHelper.getScientificNamesForConceptsIds(conceptIds);
		        List<Count> counts = facetData.getValues();
		        for (int j=0; j<counts.size(); j++) {
		            // Iterate over the facet counts
		        	Count count = counts.get(j);
		            String taxonConcept = count.getName();
                    Long taxonConceptId = Long.parseLong(taxonConcept);
		            Long facetCount = count.getCount();
//                    TaxonConceptDTO taxonConceptDTO = taxonomyManager.getTaxonConceptFor(taxonConcept);
                    GeoRegionTaxonConcept grtc = new GeoRegionTaxonConcept();
                    grtc.setGeoRegionId(regionId);
                    grtc.setTaxonConceptId(taxonConceptId);
                    
                    Map<String, Object> conceptName = conceptNames.get(j);
                    
                    grtc.setTaxonConceptName((String) conceptName.get("scientific_name"));
                    grtc.setCommonName((String) conceptName.get("common_name"));
                    
                    grtc.setOccurrenceCount(facetCount);
                    grtc.setRankName(rank.getRank());
                    geoRegionTaxonConcept.add(grtc);
                }
            }

        } catch(Exception e){
        	logger.warn("Problem performing Solr query");
            logger.debug(e.getMessage(), e);
		}

        
        return geoRegionTaxonConcept;
    }

	/**
     * Re-use the Solr server object
     * @return the server
     */
    protected SolrServer initialiseSolrServer() {
        if (this.solrServer == null & this.solrUrl  != null) {
            // Solr running in seperate webapp/war
            try {
                this.solrServer = new CommonsHttpSolrServer( this.solrUrl );
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        return solrServer;
    }

    public void setResourceFacetFields(List<String> resourceFacetFields) {
        this.resourceFacetFields = resourceFacetFields;
    }

    public void setSpeciesFacetFields(List<String> speciesFacetFields) {
        this.speciesFacetFields = speciesFacetFields;
    }

    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public void setMonthName(List<String> monthName) {
        this.monthName = monthName;
    }

    public void setSolrUrl(String solrUrl) {
        this.solrUrl = solrUrl;
    }

    public void setStartingYear(Integer startingYear) {
        this.startingYear = startingYear;
    }

    public void setTaxonomyManager(TaxonomyManager taxonomyManager) {
        this.taxonomyManager = taxonomyManager;
    }

	/**
	 * @param solDataHelper the solDataHelper to set
	 */
	public void setSolrDataHelper(SolrDataHelper solrDataHelper) {
		this.solrDataHelper = solrDataHelper;
	}
}
