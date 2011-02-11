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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.org.ala.biocache.FullRecord;
import au.org.ala.biocache.OccurrenceDAO;
import org.ala.biocache.*;
import org.ala.biocache.dao.SearchDAO;
import org.ala.biocache.dto.store.OccurrenceDTO;
import org.ala.biocache.dto.SearchQuery;
import org.ala.biocache.dto.SearchResultDTO;
import org.ala.biocache.util.SearchUtils;
import org.ala.client.appender.RestLevel;
import org.ala.client.model.LogEventType;
import org.ala.client.model.LogEventVO;
import org.ala.client.util.RestfulClient;
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
import au.org.ala.biocache.Store;
import org.ala.biocache.dto.SearchRequestParams;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Occurrences controller for the BIE biocache site
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
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

	protected String hostUrl = "http://localhost:8888/biocache-webapp";
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
	 * Default method for Controller
	 *
	 * @return mav
	 */
	@RequestMapping(value = "/occurrences", method = RequestMethod.GET)
	public ModelAndView listOccurrences() {
		ModelAndView mav = new ModelAndView();
		mav.setViewName(LIST);
		mav.addObject("message", "Results list for search goes here. (TODO)");
		return mav;
	}

	/**
	 * Occurrence search page uses SOLR JSON to display results
	 * 
	 * @param query
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
//		if (StringUtils.isEmpty(requestParams.getQ())) {
//                    logger.info("The values :" + requestParams);
//                    return searchResult;
//		}

		//temporarily set the guid as the q
                requestParams.setQ(guid);
           // String guid = requestParams.getQ();
                
                logger.info("requestParams: " + requestParams);
                //Change the method call so that the filter query can be updated
                boolean taxonFound = searchUtils.updateTaxonConceptSearchString(requestParams);

		if(taxonFound){

			
			
//			String queryJsEscaped = StringEscapeUtils.escapeJavaScript(query);
//			model.addAttribute("entityQuery", searchQuery.getEntityQuery());
//			model.addAttribute("query", query);
//			model.addAttribute("queryJsEscaped", queryJsEscaped);
//			model.addAttribute("facetQuery", filterQuery);

	        searchResult = searchDAO.findByFulltextQuery(requestParams);
                

			model.addAttribute("searchResult", searchResult);
			logger.debug("query = "+requestParams);
			Long totalRecords = searchResult.getTotalRecords();
			//model.addAttribute("totalRecords", totalRecords);
			//type of search
			//model.addAttribute("type", "taxon");
        //    model.addAttribute("facetMap", addFacetMap(filterQuery));

			if(logger.isDebugEnabled()){
				logger.debug("Returning results set with: "+totalRecords);
			}

           // model.addAttribute("lastPage", calculateLastPage(totalRecords, pageSize));
		}
                logger.info("Taxon not found...." +guid);

		return searchResult;
	}
        /**
         * Obtains a list of the sources for the supplied guid.
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
			@PathVariable(value="guid") String query,
                        @RequestParam(value="fq", required=false) String[] filterQuery,
                        HttpServletRequest request,
                        Model model
                        )
        throws Exception{
            String email = null;
                String reason = "Viewing BIE species map";
                String ip = request.getLocalAddr();
            SearchQuery searchQuery = new SearchQuery(query, "taxon", filterQuery);
		searchUtils.updateTaxonConceptSearchString(searchQuery);
                Map<String, Integer> sources =searchDAO.getSourcesForQuery(searchQuery.getQuery(), searchQuery.getFilterQuery());
                logger.debug("The sources and counts.... " + sources);
                model.addAttribute("occurrenceSources", searchUtils.getSourceInformation(sources));
                //log the usages statistic to the logger
                LogEventVO vo = new LogEventVO(LogEventType.OCCURRENCE_RECORDS_VIEWED_ON_MAP, email, reason, ip,sources);
	    	logger.log(RestLevel.REMOTE, vo);
            
        }

	/**
	 * Occurrence search for a given collection. Takes zero or more collectionCode and institutionCode
	 * parameters (but at least one must be set).
	 *
	 * @param query  This should be the institute's collectory database id, LSID or acronym. By making use of the query
	 * parameter we didn't need try and keep track of another variable in the URL
	 * @param filterQuery
	 * @param startIndex
	 * @param pageSize
	 * @param sortField
	 * @param sortDirection
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

		// one of collectionCode or institutionCode must be set
		//		if ((query == null || query.isEmpty()) && (collectionCode==null || collectionCode.length==0) && (institutionCode==null || institutionCode.length==0)) {
//			return LIST;
//		}

		
                requestParams.setQ(uid);
		//SearchQuery searchQuery = new SearchQuery(query, "collection", filterQuery);
		searchUtils.updateCollectionSearchString(requestParams);//changed to this method so that the filter query has the correct updates applied
               

		logger.info("solr query: " + requestParams);
                //TODO work out which extra attributes are required by the hubs web app
//		String queryJsEscaped = StringEscapeUtils.escapeJavaScript(query);
//		model.addAttribute("entityQuery", searchQuery.getDisplayString());
//
//		model.addAttribute("query", query);
//		model.addAttribute("queryJsEscaped", queryJsEscaped);
//		model.addAttribute("facetQuery", filterQuery);

		searchResult = searchDAO.findByFulltextQuery(requestParams);
                

		model.addAttribute("searchResult", searchResult);
		
//		Long totalRecords = searchResult.getTotalRecords();
//		model.addAttribute("totalRecords", totalRecords);
//        model.addAttribute("facetMap", addFacetMap(filterQuery));
		//type of serach
//		model.addAttribute("type", "collection");
//		model.addAttribute("lastPage", calculateLastPage(totalRecords, pageSize));

		return searchResult;

	}

    /**
     * Spatial search for either a taxon name or full text text search
     *
     * OLD URI Tested with: /occurrences/searchByArea.json?q=taxon_name:Lasioglossum|-31.2|138.4|800
     * NEW URI Tested with: /occurrences/area/-31.2/138.4/800?q=Lasioglossum
     *
     * @param query
     * @param filterQuery
     * @param startIndex
     * @param pageSize
     * @param sortField
     * @param sortDirection
     * @param radius
     * @param latitude
     * @param longitude
     * @param model
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/occurrences/area/{lat}/{lon}/{rad}*", method = RequestMethod.GET)
	public @ResponseBody SearchResultDTO occurrenceSearchByArea(
			SearchRequestParams requestParams,
                        @PathVariable("lat") Float latitude,
                        @PathVariable("lon") Float longitude,
                        @PathVariable("rad") Float radius,
                        
			Model model)
	throws Exception {
            SearchResultDTO searchResult = new SearchResultDTO();
		if (StringUtils.isEmpty(requestParams.getQ())) {
			return searchResult;
		}

               
                requestParams.setLat(latitude);
                requestParams.setLon(longitude);
                requestParams.setRadius(radius);

		//SearchQuery searchQuery = new SearchQuery(query, "spatial", filterQuery);
                searchUtils.updateSpatial(requestParams);
		//searchUtils.updateTaxonConceptSearchString(searchQuery);

                //TODO work out if we need to support something similar to below

//        if (latitude == null && longitude ==  null && radius == null && query.contains("|")) {
//            // check for lat/long/rad encoded in q param, delimited by |
//            // order is query, latitude, longitude, radius
//            String[] queryParts = StringUtils.split(query, "|", 4);
//            query = queryParts[0];
//            logger.info("(spatial) query: "+query);
//
//            if (query.contains("%%")) {
//                // mulitple parts (%% separated) need to be OR'ed (yes a hack for now)
//                String prefix = StringUtils.substringBefore(query, ":");
//                String suffix = StringUtils.substringAfter(query, ":");
//                String[] chunks = StringUtils.split(suffix, "%%");
//                ArrayList<String> formatted = new ArrayList<String>();
//
//                for (String s : chunks) {
//                    formatted.add(prefix+":"+s);
//                }
//
//                query = StringUtils.join(formatted, " OR ");
//                logger.info("new query: "+query);
//            }
//
//            latitude = Float.parseFloat(queryParts[1]);
//            longitude = Float.parseFloat(queryParts[2]);
//            radius = Float.parseFloat(queryParts[3]);
//        }

		
//	String queryJsEscaped = StringEscapeUtils.escapeJavaScript(query);
//		model.addAttribute("entityQuery", displayQuery.toString());
//		model.addAttribute("query", query);
//		model.addAttribute("queryJsEscaped", queryJsEscaped);
//		model.addAttribute("facetQuery", filterQuery);
//        model.addAttribute("facetMap", addFacetMap(filterQuery));
//        model.addAttribute("latitude", latitude);
//        model.addAttribute("longitude", longitude);
//        model.addAttribute("radius", radius);

        searchResult = searchDAO.findByFulltextSpatialQuery(requestParams);
        
		model.addAttribute("searchResult", searchResult);
//		Long totalRecords = searchResult.getTotalRecords();
//		model.addAttribute("totalRecords", totalRecords);
//		//type of search
//		model.addAttribute("type", "spatial");

		if(logger.isDebugEnabled()){
			logger.debug("Returning results set with: "+searchResult.getTotalRecords());
		}

//		model.addAttribute("lastPage", calculateLastPage(totalRecords, pageSize));

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
            SearchResultDTO searchResult = new SearchResultDTO();
        
        //TODO work out which of these attributes are necessary for the hubs web app
       //String queryJsEscaped = StringEscapeUtils.escapeJavaScript(query);
       // model.addAttribute("query", query);
        //model.addAttribute("queryJsEscaped", queryJsEscaped);
       // model.addAttribute("facetQuery", filterQuery);

        
        searchUtils.updateNormal(requestParams);

        searchResult = searchDAO.findByFulltextQuery(requestParams);
        model.addAttribute("searchResult", searchResult);
        logger.debug("query = " + requestParams.getQ());
       // Long totalRecords = searchResult.getTotalRecords();
        //model.addAttribute("totalRecords", totalRecords);
       // model.addAttribute("facetMap", addFacetMap(filterQuery));
        //type of serach
        //model.addAttribute("type", "normal");
        
       // model.addAttribute("lastPage", calculateLastPage(totalRecords, pageSize));

		return searchResult;
	}

	/**
	 * Occurrence search page uses SOLR JSON to display results
	 * 
	 * @param query
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/occurrences/download*", method = RequestMethod.GET)
	public String occurrenceDownload(
			@RequestParam(value="q", required=false) String query,
			@RequestParam(value="fq", required=false) String[] filterQuery,
			@RequestParam(value="type", required=false, defaultValue="normal") String type,
            @RequestParam(value="email", required=false) String email,
            @RequestParam(value="reason", required=false) String reason,
            @RequestParam(value="file", required=false, defaultValue="data") String filename,
			@RequestParam(value="rad", required=false) Integer radius,
			@RequestParam(value="lat", required=false) Float latitude,
			@RequestParam(value="lon", required=false) Float longitude,
			HttpServletResponse response,
            HttpServletRequest request) throws Exception {
       
        String ip = request.getLocalAddr();
        if (query == null || query.isEmpty()) {
            return LIST;
        }
        if (StringUtils.trimToNull(filename) == null) {
            filename = "data";
        }
        // if params are set but empty (e.g. foo=&bar=) then provide sensible defaults
        if (filterQuery != null && filterQuery.length == 0) {
            filterQuery = null;
        }

        if (filename != null && !filename.toLowerCase().endsWith(".zip")) {
            filename = filename + ".zip";
        }

        response.setHeader("Cache-Control", "must-revalidate");
        response.setHeader("Pragma", "must-revalidate");
        response.setHeader("Content-Disposition", "attachment;filename=" + filename);
        response.setContentType("application/zip");

        ServletOutputStream out = response.getOutputStream();
        //get the new query details
        SearchQuery searchQuery = new SearchQuery(query, type, filterQuery);
        searchUtils.updateQueryDetails(searchQuery);

        //Use a zip output stream to include the data and citation together in the download
        ZipOutputStream zop = new ZipOutputStream(out);
        zop.putNextEntry(new java.util.zip.ZipEntry(filename.substring(0, filename.length()-4) + ".csv"));
        Map<String, Integer> uidStats = null;
        
        if (checkValidSpatialParams(latitude, longitude, radius)) {
            // spatial search
            uidStats = searchDAO.writeResultsToStream(searchQuery.getQuery(), searchQuery.getFilterQuery(), zop, 100, latitude, longitude, radius);
        } else {
            uidStats = searchDAO.writeResultsToStream(searchQuery.getQuery(), searchQuery.getFilterQuery(), zop, 100);
        }
        zop.closeEntry();

        if (!uidStats.isEmpty()) {
            //add the citations for the supplied uids
            zop.putNextEntry(new java.util.zip.ZipEntry("citation.csv"));
            try {
                getCitations(uidStats.keySet(), zop);
//                    citationUtils.addCitation(uidStats.keySet(), zop);
            } catch (Exception e) {
                logger.error(e);
            }
            zop.closeEntry();
        }
        zop.flush();
        zop.close();

        //logger.debug("UID stats : " + uidStats);
        //log the stats to ala logger

        LogEventVO vo = new LogEventVO(LogEventType.OCCURRENCE_RECORDS_DOWNLOADED, email, reason, ip, uidStats);
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
	 * Occurrence record page
	 *
     * TODO Log viewing stats for all uids associated with this record.
     *
	 * @param uuid
	 * @param model
	 * @throws Exception
	 */
	@RequestMapping(value = {"/occurrences/{uuid}", "/occurrences/{uuid}.json"}, method = RequestMethod.GET)
	public @ResponseBody OccurrenceDTO showOccurrence(@PathVariable("uuid") String uuid,
        HttpServletRequest request, Model model) throws Exception {

		logger.debug("Retrieving occurrence record with guid: '"+uuid+"'");

        FullRecord[] fullRecord = Store.getAllVersionsByUuid(uuid);

        OccurrenceDTO occ = new OccurrenceDTO(fullRecord);
        occ.setSystemAssertions(Store.getSystemAssertions(uuid));
        occ.setUserAssertions(Store.getUserAssertions(uuid));

//        //We only want to log the stats if a non-json request was made <- NO LONGER TRUE!!
//        if (request.getRequestURL() != null && !request.getRequestURL().toString().endsWith("json")) {
//            String email = null;
//            String reason = "Viewing Occurrence Record " + id;
//            String ip = request.getLocalAddr();
//            Map<String, Integer> uidStats = new HashMap<String, Integer>();
//            if (fullRecord[1].getAttribution().getCollectionUid() != null) {
//                uidStats.put(fullRecord[1].getAttribution().getCollectionUid(), 1);
//            }
//            if (fullRecord[1].getAttribution().getInstitutionUid() != null) {
//                uidStats.put(fullRecord[1].getAttribution().getInstitutionUid(), 1);
//            }
//            if(fullRecord[1].getAttribution().getDataProviderUid() != null)
//                uidStats.put(fullRecord[1].getAttribution().getDataProviderUid(), 1);
//            if(fullRecord[1].getAttribution().getDataResourceUid() != null)
//            uidStats.put(fullRecord[1].getAttribution().getDataResourceUid(), 1);
//            LogEventVO vo = new LogEventVO(LogEventType.OCCURRENCE_RECORDS_VIEWED, email, reason, ip, uidStats);
//            logger.log(RestLevel.REMOTE, vo);
//        }
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
