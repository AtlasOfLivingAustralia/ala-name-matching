/***************************************************************************
 * Copyright (C) 2009 Atlas of Living Australia
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
package org.ala.web.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.dao.GeoRegionDAO;
import org.ala.dao.InstitutionDAO;
import org.ala.model.Institution;
import org.ala.model.GeoRegion;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.gbif.portal.dto.geospatial.CountryDTO;
import org.gbif.portal.dto.occurrence.IdentifierRecordDTO;
import org.gbif.portal.dto.occurrence.ImageRecordDTO;
import org.gbif.portal.dto.occurrence.LinkRecordDTO;
import org.gbif.portal.dto.occurrence.OccurrenceRecordDTO;
import org.gbif.portal.dto.occurrence.RawOccurrenceRecordDTO;
import org.gbif.portal.dto.occurrence.TypificationRecordDTO;
import org.gbif.portal.dto.resources.DataProviderDTO;
import org.gbif.portal.dto.resources.DataResourceDTO;
import org.gbif.portal.dto.resources.ResourceAccessPointDTO;
import org.gbif.portal.dto.taxonomy.BriefTaxonConceptDTO;
import org.gbif.portal.dto.taxonomy.TaxonConceptDTO;
import org.gbif.portal.service.DataResourceManager;
import org.gbif.portal.service.GeospatialManager;
import org.gbif.portal.service.OccurrenceManager;
import org.gbif.portal.service.ServiceException;
import org.gbif.portal.service.TaxonomyManager;
import org.gbif.portal.service.provider.DataProviderServices;
import org.gbif.portal.util.log.GbifLogMessage;
import org.gbif.portal.util.log.LogEvent;
import org.gbif.portal.web.controller.RestController;
import org.gbif.portal.web.filter.CriteriaDTO;
import org.gbif.portal.web.filter.CriteriaUtil;
import org.gbif.portal.web.filter.CriterionDTO;
import org.gbif.portal.web.filter.FilterDTO;
import org.gbif.portal.web.util.UserUtils;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.RedirectView;

/**
 * The dispatcher controller for all occurrence record related screens.
 * 
 * Url patterns supported:
 * 1) occurrences/<occurrence-record-key>
 * 2) occurrences/<occurrence-record-key>/view
 * 3) occurrences/<institution-code>/<collection-code>/<catalogue-number>/
 * 4) occurrences/<institution-code>/<collection-code>/<catalogue-number>/view
 * 
 * @author dmartin
 */
public class OccurrenceController extends RestController {

	/** The occurrence manager to use to make the service calls */
	protected OccurrenceManager occurrenceManager;
	/** The taxonomy manager to use to make the service calls */
	protected DataResourceManager dataResourceManager;	
	/** The taxonomy manager to use to make the service calls */
	protected TaxonomyManager taxonomyManager;
	/** The geospatial manager for service calls */
	protected GeospatialManager geospatialManager;
	/** The data provider services for retrieving raw messages */
	protected DataProviderServices dataProviderServices;
	/** The georegion DAO for retrieving regions associated with an occurrence record */
	protected GeoRegionDAO geoRegionDAO;
	/** The Institution DAO for retrieving the institution associated with an occurrence record */
	protected InstitutionDAO institutionDAO;	
	
	/** The id of the scientific name occurrence search filter */
	protected FilterDTO scientificNameFilter;
	/** The url for the occurrence filter search. */
	protected String queryViewUrl; 
	/** the cell width for the bounding box links */
	protected int cellWidth;
	/** Raw XML message view */
	protected String providerXmlSubview = "providerMessage";
	/** Raw XML message view */
	protected String requestXmlSubview = "providerRequest";
	/** Raw XML message view from cached record */
	protected String cachedRecordSubview = "cachedRecord";
	/** large map view */
	protected String largeMapSubview = "largeMap";
	/** Raw XML message view */
	protected String rawXmlStylesheet = "rawXml.jsp";
	
	/** The results limit for a distinct query */
	protected String messageSourceKey = "messageSource";
	
	/** Message source for i18n lookups */
	protected MessageSource messageSource;

	/** Utilities for user related actions */
	protected UserUtils userUtils;
	
	/** Whether or not this controller is looking at the staging area */
	protected boolean isStagingArea = false;
	
	/** The occurrence record key */
	protected String keyRequestKey = "key";
	
	protected String institutionCodeFilterId = "12";
	protected String collectionCodeFilterId = "13";
	protected String catalogueNumberFilterId = "14";
	protected String equalsPredicateId = "0";
	protected boolean doubleEncodeCachedUrls = true;
	protected int httpTimeOut = 2000;
	protected String charEnc = "UTF-8";
	protected boolean cachedRecordIsGzipped =  true;
	
	/**
	 * Resolve an id to an occurrence record and send to view
	 * 
	 * @see org.gbif.portal.web.controller.RestController#handleRequest(java.util.List, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public ModelAndView handleRequest(Map<String, String> properties, HttpServletRequest request, HttpServletResponse response) throws Exception {

		String occurrenceRecordKey = properties.get(keyRequestKey);
		String view = properties.get(subViewNameAttribute);
		String institutionCode = properties.get("institutionCode");
		String collectionCode = properties.get("collectionCode");
		String catalogueNumber = properties.get("catalogueNumber");
		
		if(StringUtils.isNotEmpty(institutionCode) && StringUtils.isNotEmpty(collectionCode) && StringUtils.isNotEmpty(catalogueNumber)){
			List<OccurrenceRecordDTO> occurrenceRecords = occurrenceManager.getOccurrenceRecordByCodes(institutionCode, collectionCode, catalogueNumber);
			if(occurrenceRecords==null || occurrenceRecords.size()>1 || occurrenceRecords.isEmpty()){
				CriteriaDTO cDTO = new CriteriaDTO();  
				cDTO.add(new CriterionDTO(institutionCodeFilterId,equalsPredicateId,institutionCode));
				cDTO.add(new CriterionDTO(collectionCodeFilterId,equalsPredicateId,collectionCode));
				cDTO.add(new CriterionDTO(catalogueNumber,equalsPredicateId,catalogueNumber));
				String queryString = CriteriaUtil.getUrl(cDTO); 
				return new ModelAndView(new RedirectView(request.getContextPath()+"/occurrences/search.htm?"+queryString));
			} 
			
			OccurrenceRecordDTO orDTO = occurrenceRecords.get(0);
			return view(orDTO, properties, request, response);
		} else if(StringUtils.isNotEmpty(occurrenceRecordKey)){
			//remove the file extension
			if(StringUtils.isNotEmpty(view))
				view = FilenameUtils.removeExtension(view);	
			//if it is an id send to view
			if(occurrenceManager.isValidOccurrenceRecordKey(occurrenceRecordKey)){
				if(view!=null){
					if(view.equals(providerXmlSubview)){
						return retrieveProviderMessage(occurrenceRecordKey, properties, request, response);
					}
					if(view.equals(requestXmlSubview)){
						return retrieveProviderRequest(occurrenceRecordKey, properties, request, response);
					}
					if(view.equals(largeMapSubview)){
						return showMapView(occurrenceRecordKey, properties, request, response);
					}
					if(view.equals(cachedRecordSubview)){
						return retrieveCachedRecord(occurrenceRecordKey, properties, request, response);
					}
				}
				return view(occurrenceRecordKey, properties, request, response);
			} else {
				//send to query view
				String criteriaUrl= CriteriaUtil.getUrl(scientificNameFilter.getId(), scientificNameFilter.getDefaultPredicateId(), occurrenceRecordKey, 0);
				String contextPath = request.getContextPath();
				return new ModelAndView(new RedirectView(contextPath+queryViewUrl+criteriaUrl));
			}
		}
		return redirectToDefaultView();
	}

	/**
	 * Show full map view
	 * 
	 * @param occurrenceRecordKey
	 * @param properties
	 * @param request
	 * @param response
	 * @return
	 */
	private ModelAndView showMapView(String occurrenceRecordKey, Map<String, String> properties, HttpServletRequest request, HttpServletResponse response) {
		try {
			OccurrenceRecordDTO or = occurrenceManager.getOccurrenceRecordFor(occurrenceRecordKey);
			ModelAndView mav = resolveAndCreateView(properties, request, false);
			mav.addObject("occurrenceRecord", or);
			//add points for map representations - e.g. googlemaps
			List<OccurrenceRecordDTO> points = new ArrayList<OccurrenceRecordDTO>();
			points.add(or);
			mav.addObject("points", points);			
			return mav;
		} catch (ServiceException e) {
			logger.error(e.getMessage(), e);
		}
		return redirectToDefaultView();
	}

	/**
	 * Retrieve and render the original provider message.
	 * 
	 * @param occurrenceRecordKey
	 * @param request
	 * @param response
	 * @return
	 */
	public ModelAndView retrieveProviderMessage(String occurrenceRecordKey, Map<String, String>properties, HttpServletRequest request, HttpServletResponse response) {
		try {
			String rawMessage = dataProviderServices.getOccurrence(occurrenceRecordKey);
			formatAndOutputMessage(request, response, rawMessage);
		} catch (Exception e) {
			logger.debug(e.getMessage(), e);
		}
		return null;
	}
	
	/**
	 * Retrieve and render the original provider message.
	 * 
	 * @param occurrenceRecordKey
	 * @param request
	 * @param response
	 * @return
	 */
	public ModelAndView retrieveProviderRequest(String occurrenceRecordKey, Map<String, String>properties, HttpServletRequest request, HttpServletResponse response) {
		try {
			String rawMessage = dataProviderServices.getOccurrenceRecordRequest(occurrenceRecordKey);
			formatAndOutputMessage(request, response, rawMessage);
		} catch (Exception e) {
			logger.debug(e.getMessage(), e);
		}
		return null;
	}	
	
	/**
	 * Retrieve and render the original cached record.
	 * 
	 * @param occurrenceRecordKey
	 * @param properties
	 * @param request
	 * @param response
	 * @return
	 */
	private ModelAndView retrieveCachedRecord(String occurrenceRecordKey,
			Map<String, String> properties, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			String rawMessage = getCachedRecordMessage(occurrenceRecordKey);
			
			if (rawMessage != null && rawMessage != "") {
				formatAndOutputMessage(request, response, rawMessage);
			} else {
				logger.error("rawMessage string was null or empty.");
			}
		} catch (IOException ioe) {
			logger.error(ioe.getMessage(), ioe);
			try {
				response.sendError(404, "Connection to remote server timed out. Please try again later.");
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}	
		return null;
	}
	
	/**
	 * Formats a raw XML message by adding style sheet and running through
	 * dom4j prettyprint.
	 * 
	 * @param request
	 * @param response
	 * @param rawMessage
	 * @throws IOException
	 */
	private void formatAndOutputMessage(HttpServletRequest request,
			HttpServletResponse response, String rawMessage)
			throws IOException {
		Writer writer = response.getWriter();
		try {
			rawMessage = addXmlOutputHeaders(rawMessage, request, response);
			//response.getWriter().write(rawMessage);
			Document doc = DocumentHelper.parseText(rawMessage);
			OutputFormat format = OutputFormat.createPrettyPrint();
			XMLWriter xmlWriter = new XMLWriter( writer, format );
			xmlWriter.write( doc );
			xmlWriter.flush();
			xmlWriter.close();
		} catch (DocumentException de) {
			logger.error("Problem parsing cached message...", de);
			writer.write(rawMessage);
		} finally {
			writer.close();
		}
	}
	
	/**
	 * Add XML headers to raw XML message output. Includes link to style sheet.
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	private String addXmlOutputHeaders(String rawMessage, HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/xml");
		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("<?xml-stylesheet type=\"text/xsl\" href=\"");
		sb.append(request.getContextPath());
		sb.append("/");
		sb.append(rawXmlStylesheet);
		sb.append("\"?>");
		sb.append(rawMessage);
		return sb.toString();
	}
	
	/**
	 * View an Occurrence Record in full.
	 * 
	 * @param request
	 * @param response
	 * @return view for displaying this Occurrence Record
	 * @throws Exception
	 */
	public ModelAndView view(String occurrenceRecordKey, Map<String, String> properties, HttpServletRequest request, HttpServletResponse response) {

		OccurrenceRecordDTO occurrenceRecord = null;
		RawOccurrenceRecordDTO rawOccurrenceRecord = null;
		ModelAndView mav = resolveAndCreateView(properties, request, false);
		mav.addObject("rawOnly", isStagingArea);
		if(isStagingArea && logger.isDebugEnabled()){
			logger.debug("Viewing record in staging area with key:"+occurrenceRecordKey);
		}
		
		try {
			occurrenceRecord = occurrenceManager.getOccurrenceRecordFor(occurrenceRecordKey);
			rawOccurrenceRecord = occurrenceManager.getRawOccurrenceRecordFor(occurrenceRecordKey);
		} catch (ServiceException e){
			logger.debug(e.getMessage(), e);
		}			

		if(rawOccurrenceRecord==null){
			return redirectToDefaultView();			
		}
		
		try {
			mav.addObject("rawOccurrenceRecord", rawOccurrenceRecord);
			//currently we dont add the parsed data for the staging area
			if(occurrenceRecord!=null && !isStagingArea){
				addParsedData(request, mav, occurrenceRecord, rawOccurrenceRecord);
			}
		} catch (ServiceException e){
			// redirect to another view???
			logger.error(e.getMessage(), e);
			redirectToDefaultView();
		}
		
		logUsage(request, occurrenceRecord, rawOccurrenceRecord);		
		
		//add points for map representations - e.g. google maps
		List<OccurrenceRecordDTO> points = new ArrayList<OccurrenceRecordDTO>();
		points.add(occurrenceRecord);
		mav.addObject("points", points);
		
		// Add a list of georegions for the occurrence record id
		addGeoRegions(occurrenceRecordKey, mav);
		
		// Add the institution associated with this occurrence record
		Institution institution = institutionDAO.getInstitutionForCode(occurrenceRecord.getInstitutionCode());
		mav.addObject("institution", institution);
		
		// Generate link to raw record XML file
		addCachedRecordLink(mav, rawOccurrenceRecord);
		return mav;
	}

	private void addGeoRegions(String occurrenceRecordKey, ModelAndView mav) {
		Long OccurrenceRecordId = Long.parseLong(occurrenceRecordKey);
		List<GeoRegion> geoRegions = geoRegionDAO.getGeoRegionsForOccurrenceRecord(OccurrenceRecordId);
		mav.addObject("geoRegions", geoRegions);

		//FIX ME!!!
		for(GeoRegion geoRegion: geoRegions){
			if(geoRegion.getGeoRegionType().getId()<3) mav.addObject("state", geoRegion);
			if(geoRegion.getGeoRegionType().getId()>=3 && geoRegion.getGeoRegionType().getId()<5) mav.addObject("city", geoRegion);
			if(geoRegion.getGeoRegionType().getId()==9) mav.addObject("shire", geoRegion);
			if(geoRegion.getGeoRegionType().getId()>=10 && geoRegion.getGeoRegionType().getId()<12) mav.addObject("town", geoRegion);
			if(geoRegion.getGeoRegionType().getId()>=2000 && geoRegion.getGeoRegionType().getId()<3000) mav.addObject("ibra", geoRegion);
			if(geoRegion.getGeoRegionType().getId()>=3000 && geoRegion.getGeoRegionType().getId()<4000) mav.addObject("imcra", geoRegion);
			if(geoRegion.getGeoRegionType().getId()>=5000 && geoRegion.getGeoRegionType().getId()<5999) mav.addObject("riverbasin", geoRegion);
		}
	}
	
	/**
	 * View an Occurrence Record in full.
	 * 
	 * @param request
	 * @param response
	 * @return view for displaying this Occurrence Record
	 * @throws Exception
	 */
	public ModelAndView view(OccurrenceRecordDTO occurrenceRecord, Map<String, String> properties, HttpServletRequest request, HttpServletResponse response) {

		RawOccurrenceRecordDTO rawOccurrenceRecord = null;
		ModelAndView mav = resolveAndCreateView(properties, request, false);
		
		try {
			rawOccurrenceRecord = occurrenceManager.getRawOccurrenceRecordFor(occurrenceRecord.getKey());
		} catch (ServiceException e){
			logger.debug(e.getMessage(), e);
		}			

		if(rawOccurrenceRecord==null){
			return redirectToDefaultView();			
		}
		try{
			addParsedData(request, mav, occurrenceRecord, rawOccurrenceRecord);
		} catch(Exception e){
			logger.error(e.getMessage(), e);
		}
		
		mav.addObject("rawOccurrenceRecord", rawOccurrenceRecord);
		logUsage(request, occurrenceRecord, rawOccurrenceRecord);	
		//add points for map representations - e.g. google maps
		List<OccurrenceRecordDTO> points = new ArrayList<OccurrenceRecordDTO>();
		points.add(occurrenceRecord);
		mav.addObject("points", points);	
		// Add a list of georegions for the occurrence record id
		Long OccurrenceRecordId = Long.parseLong(occurrenceRecord.getKey());
		List<GeoRegion> geoRegions = geoRegionDAO.getGeoRegionsForOccurrenceRecord(OccurrenceRecordId);
		mav.addObject("geoRegions", geoRegions);
		return mav;
	}	

	/**
	 * Adds the parsed details for this record.
	 * 
	 * @param request
	 * @param mav
	 * @param occurrenceRecord
	 * @param rawOccurrenceRecord
	 * @throws ServiceException
	 */
	private void addParsedData(HttpServletRequest request, ModelAndView mav, OccurrenceRecordDTO occurrenceRecord,
			RawOccurrenceRecordDTO rawOccurrenceRecord) throws ServiceException {
		
		logger.debug("Adding parsed data");
		//TODO optimise for performance - reduce the number of selects		
		TaxonConceptDTO taxonConceptDTO = taxonomyManager.getTaxonConceptFor(occurrenceRecord.getTaxonConceptKey());
		if (taxonConceptDTO != null) {
			List<BriefTaxonConceptDTO> concepts = taxonomyManager.getClassificationFor(occurrenceRecord.getNubTaxonConceptKey(), false, null, true);
			mav.addObject("concepts", concepts);
		}
		DataResourceDTO dataResource = dataResourceManager.getDataResourceFor(occurrenceRecord.getDataResourceKey());
		DataProviderDTO dataProvider = dataResourceManager.getDataProviderFor(dataResource.getDataProviderKey());
		Locale locale = RequestContextUtils.getLocale(request);
		CountryDTO country = geospatialManager.getCountryForIsoCountryCode(occurrenceRecord.getIsoCountryCode(), locale);
		mav.addObject("occurrenceRecord", occurrenceRecord);
		mav.addObject("taxonConcept", taxonConceptDTO);			
		mav.addObject("dataResource", dataResource);			
		mav.addObject("dataProvider", dataProvider);				
		mav.addObject("country", country);
		mav.addObject(messageSourceKey, messageSource);
		
		//add an image to this request - with image dimension for scaling
		logger.debug("Adding image records");
		List<ImageRecordDTO> imageRecords = occurrenceManager.getImageRecordsForOccurrenceRecord(occurrenceRecord.getKey());
		if(!imageRecords.isEmpty()){
			mav.addObject("imageRecords", imageRecords);
		}	
		
		//Typification details
		logger.debug("Adding typification records");
		List<TypificationRecordDTO> typifications = occurrenceManager.getTypificationRecordsForOccurrenceRecord(occurrenceRecord.getKey());
		mav.addObject("typifications", typifications);
		
		//link records
		logger.debug("Adding link records");
		List<LinkRecordDTO> linkRecords = occurrenceManager.getLinkRecordsForOccurrenceRecord(occurrenceRecord.getKey());
		mav.addObject("linkRecords", linkRecords);
		
		//identifier records			
		logger.debug("Adding identifier records");
		List<IdentifierRecordDTO> identifierRecords = occurrenceManager.getIdentifierRecordsForOccurrenceRecord(occurrenceRecord.getKey());
		mav.addObject("identifierRecords", identifierRecords);			
		
		//include brief concepts for major ranks - needed for back links to nub
		if(StringUtils.isNotBlank(rawOccurrenceRecord.getKingdom()) && taxonConceptDTO.getKingdomConceptKey()!=null)
			mav.addObject("kingdomConcept", taxonomyManager.getBriefTaxonConceptFor(taxonConceptDTO.getKingdomConceptKey()));
		
		if(StringUtils.isNotBlank(rawOccurrenceRecord.getPhylum()) && taxonConceptDTO.getPhylumConceptKey()!=null)
			mav.addObject("phylumConcept", taxonomyManager.getBriefTaxonConceptFor(taxonConceptDTO.getPhylumConceptKey()));
		
		if(StringUtils.isNotBlank(rawOccurrenceRecord.getBioClass()) && taxonConceptDTO.getClassConceptKey()!=null)
			mav.addObject("classConcept", taxonomyManager.getBriefTaxonConceptFor(taxonConceptDTO.getClassConceptKey()));
		
		if(StringUtils.isNotBlank(rawOccurrenceRecord.getOrder()) && taxonConceptDTO.getOrderConceptKey()!=null)
			mav.addObject("orderConcept", taxonomyManager.getBriefTaxonConceptFor(taxonConceptDTO.getOrderConceptKey()));
		
		if(StringUtils.isNotBlank(rawOccurrenceRecord.getFamily()) && taxonConceptDTO.getFamilyConceptKey()!=null)
			mav.addObject("familyConcept", taxonomyManager.getBriefTaxonConceptFor(taxonConceptDTO.getFamilyConceptKey()));
		
		if(StringUtils.isNotBlank(rawOccurrenceRecord.getGenus()) && taxonConceptDTO.getGenusConceptKey()!=null)
			mav.addObject("genusConcept", taxonomyManager.getBriefTaxonConceptFor(taxonConceptDTO.getGenusConceptKey()));
		
		if(StringUtils.isNotBlank(rawOccurrenceRecord.getSpecies()) && taxonConceptDTO.getSpeciesConceptKey()!=null)
			mav.addObject("speciesConcept", taxonomyManager.getBriefTaxonConceptFor(taxonConceptDTO.getSpeciesConceptKey()));
		
		if(StringUtils.isNotBlank(rawOccurrenceRecord.getSubspecies()))
			mav.addObject("subspeciesConcept", taxonConceptDTO);			

		//add the nub concept if available	
		if(taxonConceptDTO.getPartnerConceptKey()!=null){
			logger.debug("Adding partner concept");
			TaxonConceptDTO partnerConcept = taxonomyManager.getTaxonConceptFor(taxonConceptDTO.getPartnerConceptKey());
			mav.addObject("partnerConcept", partnerConcept);
			
			
			
		}
		
		//add one degree cell bounding box
		if(occurrenceRecord.getLatitude()!=null && occurrenceRecord.getLongitude()!=null){
			logger.debug("Adding one degree cell bounding box");
			float latitude = occurrenceRecord.getLatitude();
			float longitude = occurrenceRecord.getLongitude();
			float minLong = longitude-(cellWidth/2);
			float minLat = latitude-(cellWidth/2);
			float maxLong = longitude+(cellWidth/2);
			float maxLat = latitude+(cellWidth/2);
			
			//sanity checks
			if(minLat<-90){
				minLat = -90;
				maxLat = minLat + cellWidth;
			}
			if(maxLat>90){
				maxLat = 90;
				minLat = maxLat - cellWidth;
			}			
			if(minLong<-180){
				minLong = -180;
				maxLong = minLong + cellWidth;
			}
			if(maxLat>90){
				maxLat = 90;
				minLat = maxLat - cellWidth;
			}			
			
			//for region link
			mav.addObject("minX", minLong);
			mav.addObject("minY", minLat);
			mav.addObject("maxX", maxLong);
			mav.addObject("maxY", maxLat);
		}
	}	

	/**
	 * Logs the user event of viewing an occurrence record.
	 * 
	 * @param dataProvider
	 */
	protected void logUsage(HttpServletRequest request, OccurrenceRecordDTO occurrenceRecordDTO, RawOccurrenceRecordDTO rawOccurrenceRecordDTO) {
		GbifLogMessage gbifMessage = new GbifLogMessage();
		gbifMessage.setEvent(LogEvent.USAGE_OCCURRENCE_VIEW);
		StringBuffer sb = new StringBuffer("Occurrence record viewed");
		if(occurrenceRecordDTO!=null){
			gbifMessage.setDataProviderId(parseKey(occurrenceRecordDTO.getDataProviderKey()));
			gbifMessage.setDataResourceId(parseKey(occurrenceRecordDTO.getDataResourceKey()));
			gbifMessage.setOccurrenceId(parseKey(occurrenceRecordDTO.getKey()));
			gbifMessage.setTaxonConceptId(parseKey(occurrenceRecordDTO.getTaxonConceptKey()));
		} else {
			gbifMessage.setOccurrenceId(parseKey(rawOccurrenceRecordDTO.getKey()));
			sb.append(" (Raw only)");
		}
		gbifMessage.setTimestamp(new Date());
		gbifMessage.setRestricted(false);
		gbifMessage.setMessage(sb.toString());
		gbifMessage.setCount(1);
		userUtils.logUsage(logger, gbifMessage, request);
	}		
		
	/**
	 * Parses the supplied key. Returns null if supplied string invalid
	 * @param key
	 * @return a concept key. Returns null if supplied string invalid key	
	 */
	protected static Long parseKey(String key){
		Long parsedKey = null;
		try {
			parsedKey = Long.parseLong(key);
		} catch (NumberFormatException e){
			//expected behaviour for invalid keys
		}
		return parsedKey;
	}
	
	/**
	 * Checks if a cached raw record is available and sets a flag for the view.
	 * Uses simple webservice to indexing server (property portalcache.url)
	 * 
	 * @see org.ala.harvest.workflow.activity.MessageCachingActivity
	 * for method that generates the webservice directory hierarchy
	 * 
	 * @param mav
	 * @param rawOccurrenceRecord
	 * @return
	 * 
	 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
	 */
	private void addCachedRecordLink(ModelAndView mav,RawOccurrenceRecordDTO rawOccurrenceRecord) {
		List<String> cachedRecordUrls = generateCachedRecordUrls(rawOccurrenceRecord);
		boolean cachedRecordExists = false;
		int cachedRecordsFound = cachedRecordUrls.size();
		
		if (cachedRecordsFound > 1) {
			logger.error("Occurrence record " + keyRequestKey + " has more than 1 cached record (" + 
					cachedRecordsFound + " records found).");
		}
		
		for (String url : cachedRecordUrls) {
			//Check that the cached record exists
			HttpClient client = new HttpClient();
	        //establish a connection within 5 seconds
	        client.getHttpConnectionManager().getParams().setConnectionTimeout(httpTimeOut);
	        GetMethod getMethod = new GetMethod(url);
	        getMethod.setFollowRedirects(true);
	        Integer HttpStatusCode = null;
	        
	        try {
	            client.executeMethod(getMethod);
	            HttpStatusCode = getMethod.getStatusCode();
	        } catch (HttpException he) {
	        	logger.error("Http error connecting to '" + url + "'", he);
				return;
	        } catch (IOException ioe){
	        	logger.error("Unable to connect to '" + url + "'", ioe);
				return;
	        }
			
	        logger.debug("HTTP status code is: "+HttpStatusCode);
	        
	        if (HttpStatusCode == 200) {
	        	// Cached record was found
	        	cachedRecordExists = true;
	        	mav.addObject("cachedRecordExists", cachedRecordExists);
	        } else {
	        	logger.error("Cached record for occurrence record " + keyRequestKey + 
	        			"was not found via HTTP get. Server returned HTTP status code: " + 
	        			HttpStatusCode + "for the URL: " + url);
	        }
		}
	}
	
	/**
	 * Generate URL to retrieve cached record from indexing server via simple HTTP get.
	 * Note the URLs are not checked.
	 * 
	 * @param rawOccurrenceRecord
	 * @return a list of URLs (String)
	 * 
	 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
	 */
	private List<String> generateCachedRecordUrls(RawOccurrenceRecordDTO rawOccurrenceRecord) {
		List<String> cachedRecordLinks = new ArrayList<String>();
		ResourceBundle rb = ResourceBundle.getBundle("portal");
		String baseUrl = rb.getString("portalcache.url");
		String dataResourceKey = rawOccurrenceRecord.getDataResourceKey();
		List<ResourceAccessPointDTO> raps;
		
		try {
			raps = dataResourceManager.getResourceAccessPointsForDataResource(dataResourceKey);
		} catch (ServiceException e1) {
			logger.error("Unable to retieve resource access point for data resource key.",e1);
			return cachedRecordLinks;
		}
		
		for (ResourceAccessPointDTO rap : raps) {
			String url = null; 
			String rapUrl = rap.getUrl();
			String remoteId = rap.getRemoteIdAtUrl();
			// Retrieve occurrence record details to build web service path
			String instCode = rawOccurrenceRecord.getInstitutionCode();
			String collCode = rawOccurrenceRecord.getCollectionCode();
			String catNumber = rawOccurrenceRecord.getCatalogueNumber();
			Long recordId = Long.parseLong(rawOccurrenceRecord.getKey());
			String recordIdDir = Long.toString(recordId/1000);

			try {
				Integer max = (doubleEncodeCachedUrls == true) ? 2 : 1;
				// URL Encode Strings either once or twice depending on doubleEncodeCachedUrls
				for (int i=1; i<=max; i++) {
					// URL encode the strings
					rapUrl = URLEncoder.encode(rapUrl,charEnc);
					remoteId = URLEncoder.encode(remoteId,charEnc);
					instCode = URLEncoder.encode(instCode,charEnc);
					collCode = URLEncoder.encode(collCode,charEnc);
					catNumber = URLEncoder.encode(catNumber,charEnc);
					recordIdDir = URLEncoder.encode(recordIdDir,charEnc);
					logger.debug("URL encoding strings, pass " + i);
		        }
			} catch (UnsupportedEncodingException e) {
				logger.error("Unable to URL encode string with "+charEnc,e);
				return cachedRecordLinks;
			}
			
			url = baseUrl + "/" + rapUrl + "/" + remoteId + "/" 
				  + instCode + "/"  + recordIdDir + "/" + collCode + "/"
				  + catNumber + ".txt.gz";
			
			logger.debug("Cached record URL is "+url);
			cachedRecordLinks.add(url);
		}
		
		return cachedRecordLinks;
	}
	
	/**
	 * 
	 * @param occurrenceRecordKey
	 * @return raw cached message as XML string
	 * @throws IOException 
	 */
	private String getCachedRecordMessage(String occurrenceRecordKey) throws IOException {
		String rawMessage = null;
		RawOccurrenceRecordDTO ror;
		try {
			ror = occurrenceManager.getRawOccurrenceRecordFor(occurrenceRecordKey);
		} catch (ServiceException e) {
			logger.debug(e.getMessage(), e);
			return null;
		}
		List<String> cachedRecordUrls = generateCachedRecordUrls(ror);
		int cachedRecordsFound = cachedRecordUrls.size();
		
		if (cachedRecordsFound > 1) {
			logger.error("Occurrence record " + keyRequestKey + " has more than 1 cached record (" + 
					cachedRecordsFound + " records found).");
		}
		
		for (String url : cachedRecordUrls) {
			//Check that the cached record exists
			HttpClient client = new HttpClient();
	        client.getHttpConnectionManager().getParams().setConnectionTimeout(httpTimeOut);
	        GetMethod getMethod = new GetMethod(url);
	        getMethod.setFollowRedirects(true);
	        InputStream input = null;
	        
	        try {
	            client.executeMethod(getMethod);
	            input = getMethod.getResponseBodyAsStream();
				
	            if(cachedRecordIsGzipped){
					input = new GZIPInputStream(input);
				}
				
				InputStreamReader inR = new InputStreamReader(input);
				BufferedReader buf = new BufferedReader(inR);
				String line;
				StringBuffer sb = new StringBuffer();
				
				while ((line = buf.readLine()) != null) {
					sb.append(line);
				}
				
				rawMessage = sb.toString();
	        } catch (HttpException he) {
	        	logger.warn("Unable to retrieve cached version of the response. Cache server maybe unavailable.");
	        	logger.debug("Http error connecting to '" + url + "'", he);
				return null;
	        } 
		}
		return rawMessage;
	}


	
	/**
	 * @param cellWidth the cellWidth to set
	 */
	public void setCellWidth(int cellWidth) {
		this.cellWidth = cellWidth;
	}

	/**
	 * @param occurrenceManager the occurrenceManager to set
	 */
	public void setOccurrenceManager(OccurrenceManager occurrenceManager) {
		this.occurrenceManager = occurrenceManager;
	}

	/**
	 * @param queryViewUrl the queryViewUrl to set
	 */
	public void setQueryViewUrl(String queryViewUrl) {
		this.queryViewUrl = queryViewUrl;
	}

	/**
	 * @param taxonomyManager the taxonomyManager to set
	 */
	public void setTaxonomyManager(TaxonomyManager taxonomyManager) {
		this.taxonomyManager = taxonomyManager;
	}

	/**
	 * @param dataResourceManager the dataResourceManager to set
	 */
	public void setDataResourceManager(DataResourceManager dataResourceManager) {
		this.dataResourceManager = dataResourceManager;
	}

	/**
	 * @param geospatialManager the geospatialManager to set
	 */
	public void setGeospatialManager(GeospatialManager geospatialManager) {
		this.geospatialManager = geospatialManager;
	}

	/**
	 * @param dataProviderServices the dataProviderServices to set
	 */
	public void setDataProviderServices(DataProviderServices dataProviderServices) {
		this.dataProviderServices = dataProviderServices;
	}
	
	/**
	 * @param geoRegionDAO the geoRegionDAO to set
	 */
	public void setgeoRegionDAO(GeoRegionDAO geoRegionDAO) {
		this.geoRegionDAO = geoRegionDAO;
	}

	/**
	 * @param scientificNameFilter the scientificNameFilter to set
	 */
	public void setScientificNameFilter(FilterDTO scientificNameFilter) {
		this.scientificNameFilter = scientificNameFilter;
	}

	/**
	 * @param largeMapSubview the largeMapSubview to set
	 */
	public void setLargeMapSubview(String largeMapSubview) {
		this.largeMapSubview = largeMapSubview;
	}

	/**
	 * @param rawXmlStylesheet the rawXmlStylesheet to set
	 */
	public void setRawXmlStylesheet(String rawXmlStylesheet) {
		this.rawXmlStylesheet = rawXmlStylesheet;
	}

	/**
	 * @param messageSource the messageSource to set
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * @param messageSourceKey the messageSourceKey to set
	 */
	public void setMessageSourceKey(String messageSourceKey) {
		this.messageSourceKey = messageSourceKey;
	}

	/**
	 * @param userUtils the userUtils to set
	 */
	public void setUserUtils(UserUtils userUtils) {
		this.userUtils = userUtils;
	}

	/**
	 * @param keyRequestKey the keyRequestKey to set
	 */
	public void setKeyRequestKey(String keyRequestKey) {
		this.keyRequestKey = keyRequestKey;
	}

	/**
	 * @param isStagingArea the isStagingArea to set
	 */
	public void setIsStagingArea(boolean isStagingArea) {
		this.isStagingArea = isStagingArea;
	}

	/**
	 * @param providerXmlSubview the providerXmlSubview to set
	 */
	public void setProviderXmlSubview(String providerXmlSubview) {
		this.providerXmlSubview = providerXmlSubview;
	}

	/**
	 * @param requestXmlSubview the requestXmlSubview to set
	 */
	public void setRequestXmlSubview(String requestXmlSubview) {
		this.requestXmlSubview = requestXmlSubview;
	}

	/**
	 * @param cachedRecordSubview the cachedRecordSubview to set
	 */
	public void setCachedRecordSubview(String cachedRecordSubview) {
		this.cachedRecordSubview = cachedRecordSubview;
	}

	/**
	 * @param isStagingArea the isStagingArea to set
	 */
	public void setStagingArea(boolean isStagingArea) {
		this.isStagingArea = isStagingArea;
	}

	/**
	 * @param institutionCodeFilterId the institutionCodeFilterId to set
	 */
	public void setInstitutionCodeFilterId(String institutionCodeFilterId) {
		this.institutionCodeFilterId = institutionCodeFilterId;
	}

	/**
	 * @param collectionCodeFilterId the collectionCodeFilterId to set
	 */
	public void setCollectionCodeFilterId(String collectionCodeFilterId) {
		this.collectionCodeFilterId = collectionCodeFilterId;
	}

	/**
	 * @param catalogueNumberFilterId the catalogueNumberFilterId to set
	 */
	public void setCatalogueNumberFilterId(String catalogueNumberFilterId) {
		this.catalogueNumberFilterId = catalogueNumberFilterId;
	}

	/**
	 * @param equalsPredicateId the equalsPredicateId to set
	 */
	public void setEqualsPredicateId(String equalsPredicateId) {
		this.equalsPredicateId = equalsPredicateId;
	}

	/**
	 * @param doubleEncodeCachedUrls the doubleEncodeCachedUrls to set
	 */
	public void setDoubleEncodeCachedUrls(boolean doubleEncodeCachedUrls) {
		this.doubleEncodeCachedUrls = doubleEncodeCachedUrls;
	}

	/**
	 * @param httpTimeOut the httpTimeOut to set
	 */
	public void setHttpTimeOut(int httpTimeOut) {
		this.httpTimeOut = httpTimeOut;
	}

	/**
	 * @param charEnc the charEnc to set
	 */
	public void setCharEnc(String charEnc) {
		this.charEnc = charEnc;
	}

	/**
	 * @param cachedRecordIsGzipped the cachedRecordIsGzipped to set
	 */
	public void setCachedRecordIsGzipped(boolean cachedRecordIsGzipped) {
		this.cachedRecordIsGzipped = cachedRecordIsGzipped;
	}

	/**
	 * @param geoRegionDAO the geoRegionDAO to set
	 */
	public void setGeoRegionDAO(GeoRegionDAO geoRegionDAO) {
		this.geoRegionDAO = geoRegionDAO;
	}

	/**
	 * @param institutionDAO the institutionDAO to set
	 */
	public void setInstitutionDAO(InstitutionDAO institutionDAO) {
		this.institutionDAO = institutionDAO;
	}
}