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
import org.ala.biocache.dto.OccurrenceDTO;
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
	@RequestMapping(value = "/occurrences/searchByTaxon*", method = RequestMethod.GET)
	public String occurrenceSearchByTaxon(
			@RequestParam(value="q", required=false) String query,
			@RequestParam(value="fq", required=false) String[] filterQuery,
			@RequestParam(value="start", required=false, defaultValue="0") Integer startIndex,
			@RequestParam(value="pageSize", required=false, defaultValue ="20") Integer pageSize,
			@RequestParam(value="sort", required=false, defaultValue="score") String sortField,
			@RequestParam(value="dir", required=false, defaultValue ="asc") String sortDirection,
			@RequestParam(value="rad", required=false, defaultValue="10") Float radius,
			@RequestParam(value="lat", required=false, defaultValue="-35.27412f") Float latitude,
			@RequestParam(value="lon", required=false, defaultValue="149.11288f") Float longitude,
			Model model) throws Exception {

		if (query == null || query.isEmpty()) {
			return LIST;
		}

		SearchQuery searchQuery = new SearchQuery(query, "taxon", filterQuery);
		//boolean taxonFound = searchUtils.updateTaxonConceptSearchString(searchQuery);
                //Change the method call so that the filter query can be updated
                boolean taxonFound = searchUtils.updateQueryDetails(searchQuery);

		if(taxonFound){
		
			if (startIndex == null) {
				startIndex = 0;
			}
			if (pageSize == null) {
				pageSize = 20;
			}
			if (sortField.isEmpty()) {
				sortField = "score";
			}
			if (sortDirection.isEmpty()) {
				sortDirection = "asc";
			}
	
			SearchResultDTO searchResult = new SearchResultDTO();
			String queryJsEscaped = StringEscapeUtils.escapeJavaScript(query);
			model.addAttribute("entityQuery", searchQuery.getEntityQuery());
			model.addAttribute("query", query);
			model.addAttribute("queryJsEscaped", queryJsEscaped);
			model.addAttribute("facetQuery", filterQuery);
	        
	        searchResult = searchDAO.findByFulltextQuery("*:*", searchQuery.getFilterQuery(), startIndex, pageSize, sortField, sortDirection);
	        
			model.addAttribute("searchResult", searchResult);
			logger.debug("query = "+query);
			Long totalRecords = searchResult.getTotalRecords();
			model.addAttribute("totalRecords", totalRecords);
			//type of search
			model.addAttribute("type", "taxon");
            model.addAttribute("facetMap", addFacetMap(filterQuery));
	
			if(logger.isDebugEnabled()){
				logger.debug("Returning results set with: "+totalRecords);
			}
            
            model.addAttribute("lastPage", calculateLastPage(totalRecords, pageSize));
		} else {
			model.addAttribute("totalRecords", 0);
			model.addAttribute("searchResult", new SearchResultDTO());
		}
		return LIST;
	}
        /**
         * Obtains a list of the sources for the supplied guid.
         *
         * It also handle's the logging for the BIE.
         * 
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
	@RequestMapping(value = {"/occurrences/searchForCollection*", "/occurrences/searchForUID*"}, method = RequestMethod.GET)
	public String occurrenceSearchForCollection(
			//@RequestParam(value="coll", required=false) String[] collectionCode,
			//@RequestParam(value="inst", required=false) String[] institutionCode,
			@RequestParam(value="q", required=false) String query,
			@RequestParam(value="fq", required=false) String[] filterQuery,
			@RequestParam(value="start", required=false, defaultValue="0") Integer startIndex,
			@RequestParam(value="pageSize", required=false, defaultValue ="20") Integer pageSize,
			@RequestParam(value="sort", required=false, defaultValue="score") String sortField,
			@RequestParam(value="dir", required=false, defaultValue ="asc") String sortDirection,
			Model model)
	throws Exception {

		// no query so exit method
		if (query == null || query.isEmpty()) {
			return LIST;
		}

		// one of collectionCode or institutionCode must be set
		//		if ((query == null || query.isEmpty()) && (collectionCode==null || collectionCode.length==0) && (institutionCode==null || institutionCode.length==0)) {
		//			return LIST;
		//		}

		// if params are set but empty (e.g. foo=&bar=) then provide sensible defaults
		if (filterQuery != null && filterQuery.length == 0) {
			filterQuery = null;
		}
		if (startIndex == null) {
			startIndex = 0;
		}
		if (pageSize == null) {
			pageSize = 20;
		}
		if (sortField.isEmpty()) {
			sortField = "score";
		}
		if (sortDirection.isEmpty()) {
			sortDirection = "asc";
		}


		SearchQuery searchQuery = new SearchQuery(query, "collection", filterQuery);
		searchUtils.updateQueryDetails(searchQuery);//changed to this method so that the filter query has the correct updates applied
                //searchUtils.updateCollectionSearchString(searchQuery);

		logger.info("solr query: " + searchQuery.getQuery());
		SearchResultDTO searchResult = new SearchResultDTO();
		String queryJsEscaped = StringEscapeUtils.escapeJavaScript(query);
		model.addAttribute("entityQuery", searchQuery.getDisplayString());

		model.addAttribute("query", query);
		model.addAttribute("queryJsEscaped", queryJsEscaped);
		model.addAttribute("facetQuery", filterQuery);

		searchResult = searchDAO.findByFulltextQuery(searchQuery.getQuery(), searchQuery.getFilterQuery(), startIndex, pageSize, sortField, sortDirection);

		model.addAttribute("searchResult", searchResult);
		logger.debug("query = " + query);
		Long totalRecords = searchResult.getTotalRecords();
		model.addAttribute("totalRecords", totalRecords);
        model.addAttribute("facetMap", addFacetMap(filterQuery));
		//type of serach
		model.addAttribute("type", "collection");
		model.addAttribute("lastPage", calculateLastPage(totalRecords, pageSize));

		return LIST;

	}

    /**
     * Spatial search for either a taxon name or full text text search
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
    @RequestMapping(value = "/occurrences/searchByArea*", method = RequestMethod.GET)
	public String occurrenceSearchByArea(
			@RequestParam(value="q", required=true) String query,
			@RequestParam(value="fq", required=false) String[] filterQuery,
			@RequestParam(value="start", required=false, defaultValue="0") Integer startIndex,
			@RequestParam(value="pageSize", required=false, defaultValue ="20") Integer pageSize,
			@RequestParam(value="sort", required=false, defaultValue="score") String sortField,
			@RequestParam(value="dir", required=false, defaultValue ="asc") String sortDirection,
			@RequestParam(value="rad", required=false) Float radius,
			@RequestParam(value="lat", required=false) Float latitude,
			@RequestParam(value="lon", required=false) Float longitude,
			Model model)
	throws Exception {

		if (query == null || query.isEmpty()) {
			return LIST;
		}

		SearchQuery searchQuery = new SearchQuery(query, "spatial", filterQuery);
                searchUtils.updateQueryDetails(searchQuery);
		//searchUtils.updateTaxonConceptSearchString(searchQuery);

		if (startIndex == null) {
			startIndex = 0;
		}
		if (pageSize == null) {
			pageSize = 20;
		}
		if (sortField.isEmpty()) {
			sortField = "score";
		}
		if (sortDirection.isEmpty()) {
			sortDirection = "asc";
		}

        if (latitude == null && longitude ==  null && radius == null && query.contains("|")) {
            // check for lat/long/rad encoded in q param, delimited by |
            // order is query, latitude, longitude, radius
            String[] queryParts = StringUtils.split(query, "|", 4);
            query = queryParts[0];
            logger.info("(spatial) query: "+query);

            if (query.contains("%%")) {
                // mulitple parts (%% separated) need to be OR'ed (yes a hack for now)
                String prefix = StringUtils.substringBefore(query, ":");
                String suffix = StringUtils.substringAfter(query, ":");
                String[] chunks = StringUtils.split(suffix, "%%");
                ArrayList<String> formatted = new ArrayList<String>();

                for (String s : chunks) {
                    formatted.add(prefix+":"+s);
                }

                query = StringUtils.join(formatted, " OR ");
                logger.info("new query: "+query);
            }

            latitude = Float.parseFloat(queryParts[1]);
            longitude = Float.parseFloat(queryParts[2]);
            radius = Float.parseFloat(queryParts[3]);
        }

		SearchResultDTO searchResult = new SearchResultDTO();
		String queryJsEscaped = StringEscapeUtils.escapeJavaScript(query);
        StringBuilder displayQuery = new StringBuilder(StringUtils.substringAfter(query, ":").replace("*", "(all taxa)"));
        displayQuery.append(" - within "+radius+" km of point ("+latitude+", "+longitude+")");
		model.addAttribute("entityQuery", displayQuery.toString());
		model.addAttribute("query", query);
		model.addAttribute("queryJsEscaped", queryJsEscaped);
		model.addAttribute("facetQuery", filterQuery);
        model.addAttribute("facetMap", addFacetMap(filterQuery));
        model.addAttribute("latitude", latitude);
        model.addAttribute("longitude", longitude);
        model.addAttribute("radius", radius);

        searchResult = searchDAO.findByFulltextSpatialQuery(query, searchQuery.getFilterQuery(), latitude, longitude, radius, startIndex, pageSize, sortField, sortDirection);
        
		model.addAttribute("searchResult", searchResult);
		Long totalRecords = searchResult.getTotalRecords();
		model.addAttribute("totalRecords", totalRecords);
		//type of search
		model.addAttribute("type", "spatial");

		if(logger.isDebugEnabled()){
			logger.debug("Returning results set with: "+totalRecords);
		}

		model.addAttribute("lastPage", calculateLastPage(totalRecords, pageSize));

		return LIST;
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
	 * @param query
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/occurrences/search*", method = RequestMethod.GET)
	public String occurrenceSearch(
			@RequestParam(value="q", required=false) String query,
			@RequestParam(value="fq", required=false) String[] filterQuery,
			@RequestParam(value="start", required=false, defaultValue="0") Integer startIndex,
			@RequestParam(value="pageSize", required=false, defaultValue ="20") Integer pageSize,
			@RequestParam(value="sort", required=false, defaultValue="score") String sortField,
			@RequestParam(value="dir", required=false, defaultValue ="asc") String sortDirection,
			@RequestParam(value="rad", required=false, defaultValue="10") Integer radius,
			@RequestParam(value="lat", required=false, defaultValue="-35.27412f") Float latitude,
			@RequestParam(value="lon", required=false, defaultValue="149.11288f") Float longitude,
			Model model)
	throws Exception {

		if (query == null || query.isEmpty()) {
			return LIST;
		}
		// if params are set but empty (e.g. foo=&bar=) then provide sensible defaults
		if (filterQuery != null && filterQuery.length == 0) {
			filterQuery = null;
		}
		if (startIndex == null) {
			startIndex = 0;
		}
		if (pageSize == null) {
			pageSize = 20;
		}
		if (sortField.isEmpty()) {
			sortField = "score";
		}
		if (sortDirection.isEmpty()) {
			sortDirection = "asc";
		}

		SearchResultDTO searchResult = new SearchResultDTO();
        String queryJsEscaped = StringEscapeUtils.escapeJavaScript(query);
        model.addAttribute("query", query);
        model.addAttribute("queryJsEscaped", queryJsEscaped);
        model.addAttribute("facetQuery", filterQuery);

        SearchQuery searchQuery = new SearchQuery(query, "normal", filterQuery);
        searchUtils.updateQueryDetails(searchQuery);

        searchResult = searchDAO.findByFulltextQuery(searchQuery.getQuery(), searchQuery.getFilterQuery(), startIndex, pageSize, sortField, sortDirection);
        model.addAttribute("searchResult", searchResult);
        logger.debug("query = " + query);
        Long totalRecords = searchResult.getTotalRecords();
        model.addAttribute("totalRecords", totalRecords);
        model.addAttribute("facetMap", addFacetMap(filterQuery));
        //type of serach
        model.addAttribute("type", "normal");
        model.addAttribute("lastPage", calculateLastPage(totalRecords, pageSize));
                
		return LIST;
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
            HttpServletRequest request)
	throws Exception {
       
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
        zop.putNextEntry(new java.util.zip.ZipEntry(filename + ".csv"));
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
	 * @param id
	 * @param model
     * @param log Optional supplied value to specify whether or not the log the statistics.  Statistics are logged by default
	 * @return view name
	 * @throws Exception
	 */
	@RequestMapping(value = {"/occurrences/{id}", "/occurrences/{id}.json"}, method = RequestMethod.GET)
	public String showOccurrence(@PathVariable("id") String id,
        HttpServletRequest request, Model model) throws Exception {
		logger.debug("Retrieving occurrence record with guid: "+id+".");
        model.addAttribute("id", id);
		OccurrenceDTO occurrence = searchDAO.getById(id);
		model.addAttribute("occurrence", occurrence);

        //au.org.ala.biocache.QualityAssertion.apply(au.org.ala.biocache.AssertionCodes.ALTITUDE_IN_FEET());

        FullRecord fullRecord = Store.getByUuid(id);

		if (occurrence != null && occurrence.getCollectionCodeUid() != null) {
			
			if(occurrence.getCollectionCodeUid()!=null){
	            Object[] resp = restfulClient.restGet(summaryServiceUrl + "/" + occurrence.getCollectionCodeUid());
	            if ((Integer) resp[0] == HttpStatus.SC_OK) {
	                String json = (String) resp[1];
	                ObjectMapper mapper = new ObjectMapper();
	                JsonNode rootNode;
	
	                try {
	                    rootNode = mapper.readValue(json, JsonNode.class);
	                    String name = rootNode.path("name").getTextValue();
	                    String logo = rootNode.path("institutionLogoUrl").getTextValue();
	                    String institution = rootNode.path("institution").getTextValue();
	                    model.addAttribute("collectionName", name);
	                    model.addAttribute("collectionLogo", logo);
	                    model.addAttribute("collectionInstitution", institution);
	                } catch (Exception e) {
	                    logger.error(e.toString());
	                }
	            }
			}
		}
            
        if (id != null) {
            Long occurrenceId = new Long(id);

            //
            
//            RawOccurrenceRecord rawOccurrence = rawOccurrenceRecordDAO.getById(occurrenceId);
//            model.addAttribute("rawOccurrence", rawOccurrence);
//
//            List<ImageRecord> images = imageRecordDAO.findByOccurrenceId(occurrenceId);
//            model.addAttribute("images", images);

        }
        model.addAttribute("hostUrl", hostUrl);

        //log the usage statistics to the ala logger if necessary
        //We only want to log the stats if a non-json request was made.
        if (request.getRequestURL() != null && !request.getRequestURL().toString().endsWith("json")) {
            String email = null;
            String reason = "Viewing Occurrence Record " + id;
            String ip = request.getLocalAddr();
            Map<String, Integer> uidStats = new HashMap<String, Integer>();
            if (occurrence.getCollectionCodeUid() != null) {
                uidStats.put(occurrence.getCollectionCodeUid(), 1);
            }
            if (occurrence.getInstitutionCodeUid() != null) {
                uidStats.put(occurrence.getInstitutionCodeUid(), 1);
            }
            //all occurrence records must have a dpuid and druid
            uidStats.put(occurrence.getDataProviderUid(), 1);
            uidStats.put(occurrence.getDataResourceUid(), 1);
            LogEventVO vo = new LogEventVO(LogEventType.OCCURRENCE_RECORDS_VIEWED, email, reason, ip, uidStats);
            logger.log(RestLevel.REMOTE, vo);
        }
        
		return SHOW;
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
}
