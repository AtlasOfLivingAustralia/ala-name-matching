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
package org.ala.biocache.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.org.ala.biocache.*;
import org.ala.biocache.*;
import org.ala.biocache.dao.SearchDAO;
import org.ala.biocache.dto.DownloadRequestParams;
import org.ala.biocache.dto.store.OccurrenceDTO;
import org.ala.biocache.dto.SearchQuery;
import org.ala.biocache.dto.SearchResultDTO;
import org.ala.biocache.util.SearchUtils;
import org.ala.client.appender.RestLevel;
import org.ala.client.model.LogEventType;
import org.ala.client.model.LogEventVO;
import org.ala.client.util.RestfulClient;
import org.apache.commons.collections.iterators.ArrayListIterator;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Field;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.ala.biocache.dto.SearchRequestParams;
import org.ala.biocache.dto.SpatialSearchRequestParams;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Occurrences controller for the BIE biocache site.
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 * @author "Natasha Carter <Natasha.Carter@csiro.au>" 
 * 
 * History:
 * 1 Sept 10 (MOK011): added restfulClient to retrieve citation information into citation.txt
 * [private void getCitations(Set<String> keys, OutputStream out) throws HttpException, IOException]
 * 
 * 14 Dept 10 (MOK011): modified getCitations function to get csv format data from Citation Service.
 * 
 */
@Controller
public class OccurrenceController {

	/** Logger initialisation */
	private final static Logger logger = Logger.getLogger(OccurrenceController.class);

	/** Fulltext search DAO */
	@Inject
	protected SearchDAO searchDAO;
	/** Data Resource DAO */
	@Inject
	protected SearchUtils searchUtils;
	@Inject
	protected RestfulClient restfulClient;
	
	/** Name of view for site home page */
	private String HOME = "homePage";
	/** Name of view for list of taxa */
	private final String LIST = "occurrences/list";
	/** Name of view for a single taxon */
	private final String SHOW = "occurrences/show";

	protected String hostUrl = "http://localhost:8888/biocache-service";
	protected String bieBaseUrl = "http://bie.ala.org.au/";
	protected String collectoryBaseUrl = "http://collections.ala.org.au";
	protected String citationServiceUrl = collectoryBaseUrl + "/lookup/citation";
	protected String summaryServiceUrl  = collectoryBaseUrl + "/lookup/summary";
	
	/**
	 * Custom handler for the welcome view.
	 * <p>
	 * Note that this handler relies on the RequestToViewNameTranslator to
	 * determine the logical view name based on the request URL: "/welcome.do"
	 * -&gt; "welcome".
	 *
	 * @return viewname to render
	 */
	@RequestMapping("/")
	public String homePageHandler() {
		return HOME;
	}

	/**
	 * Returns the complete list of Occurrences
	 */
	@RequestMapping(value = {"/occurrences", "/occurrences/collection", "/occurrences/institution", "/occurrences/data-resource", "/occurrences/data-provider", "/occurrences/taxon"}, method = RequestMethod.GET)
	public @ResponseBody SearchResultDTO listOccurrences(Model model) throws Exception {
            SearchRequestParams srp = new SearchRequestParams();
            srp.setQ("*:*");
            return occurrenceSearch(srp, model);
	}

	/**
	 * Occurrence search page uses SOLR JSON to display results
	 * 
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/occurrences/taxon/{guid:.+}*", method = RequestMethod.GET)
	public @ResponseBody SearchResultDTO occurrenceSearchByTaxon(
            SearchRequestParams requestParams,
            @PathVariable("guid") String guid,
            Model model) throws Exception {

        SearchResultDTO searchResult = new SearchResultDTO();
        SearchUtils.setDefaultParams(requestParams);
        logger.debug("requestParams: " + requestParams);
        //Change the method call so that the filter query can be updated
        boolean taxonFound = searchUtils.updateTaxonConceptSearchString(requestParams, guid);

		if (taxonFound) {
	        searchResult = searchDAO.findByFulltextQuery(requestParams);
			model.addAttribute("searchResult", searchResult);
			logger.debug("query = "+requestParams);
			Long totalRecords = searchResult.getTotalRecords();

			if (logger.isDebugEnabled()) {
				logger.debug("Returning results set with: "+totalRecords);
			}
		}

        logger.info("Taxon not found...." +guid);

		return searchResult;
	}

    /**
     * Obtains a list of the sources for the supplied guid.
     *
     * I don't think that this should be necessary. We should be able to
     * configure the requestParams facets to contain the collectino_uid, institution_uid
     * data_resource_uid and data_provider_uid
     *
     * It also handle's the logging for the BIE.
     * //TODO Work out what to do with this
     * @param query
     * @param request
     * @param model
     * @throws Exception
     */
    @RequestMapping(value = "/occurrences/sourceByTaxon/{guid}.json*", method = RequestMethod.GET)
    public void sourceByTaxon(
            @PathVariable(value = "guid") String query,
            @RequestParam(value = "fq", required = false) String[] filterQuery,
            HttpServletRequest request,
            Model model)
            throws Exception {
        String email = null;
        String reason = "Viewing BIE species map";
        String ip = request.getLocalAddr();
        SearchQuery searchQuery = new SearchQuery(query, "taxon", filterQuery);
        searchUtils.updateTaxonConceptSearchString(searchQuery);
        Map<String, Integer> sources = searchDAO.getSourcesForQuery(searchQuery.getQuery(), searchQuery.getFilterQuery());
        logger.debug("The sources and counts.... " + sources);
        model.addAttribute("occurrenceSources", searchUtils.getSourceInformation(sources));
        //log the usages statistic to the logger
        LogEventVO vo = new LogEventVO(LogEventType.OCCURRENCE_RECORDS_VIEWED_ON_MAP, email, reason, ip, sources);
        logger.log(RestLevel.REMOTE, vo);

    }

	   /**
     * Occurrence search for a given collection, institution, data_resource or data_provider.
     *
     * @param requestParams The search parameters
     * @param  uid The uid for collection, institution, data_resource or data_provider
     * @param model
     * @return
     * @throws Exception
     */
    @RequestMapping(value = {"/occurrences/collection/{uid}", "/occurrences/institution/{uid}", "/occurrences/data-resource/{uid}", "/occurrences/data-provider/{uid}"}, method = RequestMethod.GET)
    public @ResponseBody SearchResultDTO occurrenceSearchForCollection(
            SearchRequestParams requestParams,
            @PathVariable("uid") String uid,
            Model model)
            throws Exception {
        SearchResultDTO searchResult = new SearchResultDTO();
        // no query so exit method
        if (StringUtils.isEmpty(uid)) {
            return searchResult;
        }

        SearchUtils.setDefaultParams(requestParams);
		//update the request params so the search caters for the supplied uid
		searchUtils.updateCollectionSearchString(requestParams, uid);
		logger.debug("solr query: " + requestParams);
		searchResult = searchDAO.findByFulltextQuery(requestParams);
		model.addAttribute("searchResult", searchResult);

		return searchResult;
	}

    /**
     * Spatial search for either a taxon name or full text text search
     *
     * OLD URI Tested with: /occurrences/searchByArea.json?q=taxon_name:Lasioglossum|-31.2|138.4|800
     * NEW URI Tested with: /occurrences/area/-31.2/138.4/800?q=Lasioglossum
     * @param model
     * @return
     * @throws Exception
     */
    @RequestMapping(value =  "/occurrences/searchByArea*", method = RequestMethod.GET)
	public @ResponseBody SearchResultDTO occurrenceSearchByArea(
			SpatialSearchRequestParams requestParams,
			Model model)
            throws Exception {
        SearchResultDTO searchResult = new SearchResultDTO();

        if (StringUtils.isEmpty(requestParams.getQ())) {
			return searchResult;
		}

        searchUtils.updateSpatial(requestParams);
        searchResult = searchDAO.findByFulltextQuery(requestParams);
		model.addAttribute("searchResult", searchResult);

		if(logger.isDebugEnabled()){
			logger.debug("Returning results set with: "+searchResult.getTotalRecords());
		}

		return searchResult;
	}

	/**
	 * Retrieve content as String.
	 * 
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public static String getUrlContentAsString(String url) throws Exception {
		HttpClient httpClient = new HttpClient();
		GetMethod gm = new GetMethod(url);
		gm.setFollowRedirects(true);
		httpClient.executeMethod(gm);
		// String requestCharset = gm.getRequestCharSet();
		String content = gm.getResponseBodyAsString();
		// content = new String(content.getBytes(requestCharset), "UTF-8");
		return content;
	}

	/**
	 * Occurrence search page uses SOLR JSON to display results
	 *
         * Tested with :/occurrences/search.json?q=Victoria
         *
	 * @param query
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/occurrences/search*", method = RequestMethod.GET)
	public @ResponseBody SearchResultDTO occurrenceSearch(SearchRequestParams requestParams,
            Model model) throws Exception {
        // handle empty param values, e.g. &sort=&dir=
        SearchUtils.setDefaultParams(requestParams); 
        SearchResultDTO searchResult = searchDAO.findByFulltextQuery(requestParams);
        model.addAttribute("searchResult", searchResult);
        logger.debug("query = " + requestParams.getQ());

		return searchResult;
	}

	/**
	 * Occurrence search page uses SOLR JSON to display results
         *
         * Please NOTE that the q and fq provided to this URL should be obtained
         * from SearchResultDTO.urlParameters
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/occurrences/download*", method = RequestMethod.GET)
	public String occurrenceDownload(
			DownloadRequestParams requestParams,
			HttpServletResponse response,
            HttpServletRequest request) throws Exception {
       
        String ip = request.getLocalAddr();
        if (requestParams.getQ().isEmpty()) {
            return LIST;
        }

        String filename = requestParams.getFile();
        
        response.setHeader("Cache-Control", "must-revalidate");
        response.setHeader("Pragma", "must-revalidate");
        response.setHeader("Content-Disposition", "attachment;filename=" + filename +".zip");
        response.setContentType("application/zip");

        ServletOutputStream out = response.getOutputStream();
        
        //Use a zip output stream to include the data and citation together in the download
        ZipOutputStream zop = new ZipOutputStream(out);
        zop.putNextEntry(new java.util.zip.ZipEntry(filename + ".csv"));
        Map<String, Integer> uidStats = null;
        //put the factes
        requestParams.setFacets(new String[]{"assertions"});
        uidStats = searchDAO.writeResultsToStream(requestParams, zop, 100);
        zop.closeEntry();

        if (!uidStats.isEmpty()) {
            //add the citations for the supplied uids
            zop.putNextEntry(new java.util.zip.ZipEntry("citation.csv"));
            try {
                getCitations(uidStats.keySet(), zop);
            } catch (Exception e) {
                logger.error(e);
            }
            zop.closeEntry();
        }
        zop.flush();
        zop.close();

        //logger.debug("UID stats : " + uidStats);
        //log the stats to ala logger

        LogEventVO vo = new LogEventVO(LogEventType.OCCURRENCE_RECORDS_DOWNLOADED, requestParams.getEmail(), requestParams.getReason(), ip, uidStats);
        logger.log(RestLevel.REMOTE, vo);
        return null;
	}

	/**
	 * get citation info from citation web service and write it into citation.txt file.
	 * 
	 * @param keys
	 * @param out
	 * @throws HttpException
	 * @throws IOException
	 */
	private void getCitations(Set<String> keys, OutputStream out) throws HttpException, IOException{
		if(keys == null || out == null){
			throw new NullPointerException("keys and/or out is null!!");
		}
		
        Object[] citations = restfulClient.restPost(citationServiceUrl, "text/plain", keys);
        if((Integer)citations[0] == HttpStatus.SC_OK){
        	out.write(((String)citations[1]).getBytes());
    	}
	}

	/**
	 * Utility method for retrieving a list of occurrences. Mainly added to help debug
     * webservices for that a developer can retrieve example UUIDs.
     *
	 * @throws Exception
	 */
	@RequestMapping(value = {"/occurrences/page"}, method = RequestMethod.GET)
	public @ResponseBody List<FullRecord> pageOccurrences(
            @RequestParam(value="pageSize", required=false, defaultValue="10") int pageSize) throws Exception {
        final List<FullRecord> records = new ArrayList<FullRecord>();
        Store.pageOverAll(Versions.RAW(), new OccurrenceConsumer(){
            public boolean consume(FullRecord record) {
                records.add(record);
                return false;
            }
        }, pageSize);
        return records;
	}

	
	/**
	 * Occurrence record page
	 *
         * When user supplies a uuid that is not found search for a unique record
         * with the supplied occurrenc_id
         *
         * Returns a SearchResultDTO when there is more than 1 record with the supplied UUID
         *
	 * @param uuid
	 * @param model
	 * @throws Exception
	 */
	@RequestMapping(value = {"/occurrence/{uuid}","/occurrences/{uuid}", "/occurrence/{uuid}.json", "/occurrences/{uuid}.json"}, method = RequestMethod.GET)
	public @ResponseBody Object showOccurrence(@PathVariable("uuid") String uuid,
        HttpServletRequest request, Model model) throws Exception {

		logger.debug("Retrieving occurrence record with guid: '"+uuid+"'");

        FullRecord[] fullRecord = Store.getAllVersionsByUuid(uuid);
        if(fullRecord == null){
            //check to see if we have an occurrence id
            SearchRequestParams srp = new SearchRequestParams();
            srp.setQ("occurrence_id:" + uuid);
            SearchResultDTO result = occurrenceSearch(srp, model);
            if(result.getTotalRecords()>1)
                return result;
            else if(result.getTotalRecords()==0)
                return new OccurrenceDTO();
            else
                fullRecord = Store.getAllVersionsByUuid(result.getOccurrences().get(0).getUuid());

        }

        OccurrenceDTO occ = new OccurrenceDTO(fullRecord);
        occ.setSystemAssertions(Store.getSystemAssertions(uuid));
        occ.setUserAssertions(Store.getUserAssertions(uuid));

        //log the statistics for viewing the record
            String email = null;
            String reason = "Viewing Occurrence Record " + uuid;
            String ip = request.getLocalAddr();
            Map<String, Integer> uidStats = new HashMap<String, Integer>();
            if(occ.getProcessed() != null && occ.getProcessed().getAttribution()!=null){
                if (occ.getProcessed().getAttribution().getCollectionUid() != null) {
                    uidStats.put(occ.getProcessed().getAttribution().getCollectionUid(), 1);
                }
                if (occ.getProcessed().getAttribution().getInstitutionUid() != null) {
                    uidStats.put(occ.getProcessed().getAttribution().getInstitutionUid(), 1);
                }
                if(occ.getProcessed().getAttribution().getDataProviderUid() != null)
                    uidStats.put(occ.getProcessed().getAttribution().getDataProviderUid(), 1);
                if(occ.getProcessed().getAttribution().getDataResourceUid() != null)
                    uidStats.put(occ.getProcessed().getAttribution().getDataResourceUid(), 1);
            }

            
        LogEventVO vo = new LogEventVO(LogEventType.OCCURRENCE_RECORDS_VIEWED, email, reason, ip, uidStats);
        logger.log(RestLevel.REMOTE, vo);

        return occ;
	}

    /**
     * Create a HashMap for the filter queries
     *
     * @param filterQuery
     * @return
     */
    private HashMap<String, String> addFacetMap(String[] filterQuery) {
               HashMap<String, String> facetMap = new HashMap<String, String>();

        if (filterQuery != null && filterQuery.length > 0) {
            logger.debug("filterQuery = "+StringUtils.join(filterQuery, "|"));
            for (String fq : filterQuery) {
                if (fq != null && !fq.isEmpty()) {
                    String[] fqBits = StringUtils.split(fq, ":", 2);
                    facetMap.put(fqBits[0], fqBits[1]);
                }
            }
        }
        return facetMap;
    }
    
    /**
     * Calculate the last page number for pagination
     * 
     * @param totalRecords
     * @param pageSize
     * @return
     */
    private Integer calculateLastPage(Long totalRecords, Integer pageSize) {
        Integer lastPage = 0;
        Integer lastRecordNum = totalRecords.intValue();
        
        if (pageSize > 0) {
            lastPage = (lastRecordNum / pageSize) + ((lastRecordNum % pageSize > 0) ? 1 : 0);
        }
        
        return lastPage;
    }

    /**
     * Simple check for valid latitude, longitude & radius values
     *
     * @param latitude
     * @param longitude
     * @param radius
     * @return
     */
    private boolean checkValidSpatialParams(Float latitude, Float longitude, Integer radius) {
        if (latitude != null && !latitude.isNaN() && longitude != null && !longitude.isNaN() && radius != null && radius > 0) {
            return true;
        } else {
            return false;
        }
    }

	/**
	 * @param hostUrl the hostUrl to set
	 */
	public void setHostUrl(String hostUrl) {
		this.hostUrl = hostUrl;
	}

	/**
	 * @param searchDAO the searchDAO to set
	 */
	public void setSearchDAO(SearchDAO searchDAO) {
		this.searchDAO = searchDAO;
	}

	/**
	 * @param bieBaseUrl the bieBaseUrl to set
	 */
	public void setBieBaseUrl(String bieBaseUrl) {
		this.bieBaseUrl = bieBaseUrl;
	}

	/**
	 * @param collectoryBaseUrl the collectoryBaseUrl to set
	 */
	public void setCollectoryBaseUrl(String collectoryBaseUrl) {
		this.collectoryBaseUrl = collectoryBaseUrl;
	}

	/**
	 * @param searchUtils the searchUtils to set
	 */
	public void setSearchUtils(SearchUtils searchUtils) {
		this.searchUtils = searchUtils;
	}


    public String getCitationServiceUrl() {
		return citationServiceUrl;
	}

	public void setCitationServiceUrl(String citationServiceUrl) {
		this.citationServiceUrl = citationServiceUrl;
	}
}
