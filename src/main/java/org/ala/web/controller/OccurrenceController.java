/***************************************************************************
 * Copyright (C) 2005 Global Biodiversity Information Facility Secretariat.
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.dao.geo.GeoRegionDAO;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.gbif.portal.dto.geospatial.CountryDTO;
import org.gbif.portal.dto.occurrence.IdentifierRecordDTO;
import org.gbif.portal.dto.occurrence.ImageRecordDTO;
import org.gbif.portal.dto.occurrence.LinkRecordDTO;
import org.gbif.portal.dto.occurrence.OccurrenceRecordDTO;
import org.gbif.portal.dto.occurrence.RawOccurrenceRecordDTO;
import org.gbif.portal.dto.occurrence.TypificationRecordDTO;
import org.gbif.portal.dto.resources.DataProviderDTO;
import org.gbif.portal.dto.resources.DataResourceDTO;
import org.gbif.portal.dto.taxonomy.BriefTaxonConceptDTO;
import org.gbif.portal.dto.taxonomy.TaxonConceptDTO;
import org.gbif.portal.model.geospatial.GeoRegion;
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
	 * @param occurrenceRecordKey
	 * @param request
	 * @param response
	 * @return
	 */
	public ModelAndView retrieveProviderMessage(String occurrenceRecordKey, Map<String, String>properties, HttpServletRequest request, HttpServletResponse response) {
		try {
			String rawMessage = dataProviderServices.getOccurrence(occurrenceRecordKey);
			response.setContentType("text/xml");
			response.getWriter().write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			response.getWriter().write("<?xml-stylesheet type=\"text/xsl\" href=\"");
			response.getWriter().write(request.getContextPath());
			response.getWriter().write("/");
			response.getWriter().write(rawXmlStylesheet);
			response.getWriter().write("\"?>");
			response.getWriter().write(rawMessage);
		} catch (Exception e) {
			logger.debug(e.getMessage(), e);
		}
		return null;
	}
	
	/**
	 * Retrieve and render the original provider message.
	 * @param occurrenceRecordKey
	 * @param request
	 * @param response
	 * @return
	 */
	public ModelAndView retrieveProviderRequest(String occurrenceRecordKey, Map<String, String>properties, HttpServletRequest request, HttpServletResponse response) {
		try {
			String rawMessage = dataProviderServices.getOccurrenceRecordRequest(occurrenceRecordKey);
			response.setContentType("text/xml");
//			response.getWriter().write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
//			response.getWriter().write("<?xml-stylesheet type=\"text/xsl\" href=\"");
//			response.getWriter().write(request.getContextPath());
//			response.getWriter().write("/");
//			response.getWriter().write(rawXmlStylesheet);
//			response.getWriter().write("\"?>");
			response.getWriter().write(rawMessage);
		} catch (Exception e) {
			logger.debug(e.getMessage(), e);
		}
		return null;
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
		Long OccurrenceRecordId = Long.parseLong(occurrenceRecordKey);
		List<GeoRegion> geoRegions = geoRegionDAO.getGeoRegionsForOccurrenceRecord(OccurrenceRecordId);
		mav.addObject("geoRegions", geoRegions);
		return mav;
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
			List<BriefTaxonConceptDTO> concepts = taxonomyManager.getClassificationFor(taxonConceptDTO.getKey(), false, null, true);
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
}