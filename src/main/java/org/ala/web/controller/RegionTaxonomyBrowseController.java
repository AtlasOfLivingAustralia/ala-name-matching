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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.dao.GeoRegionTaxonConceptDAO;
import org.ala.model.GeoRegionTaxonConcept;
import org.gbif.portal.dto.geospatial.GeoRegionDTO;
import org.gbif.portal.service.GeospatialManager;
import org.gbif.portal.service.TaxonomyManager;
import org.gbif.portal.web.controller.RestKeyValueController;
import org.gbif.portal.web.util.TaxonConceptUtils;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
/**
 * Country Taxonomy Browser.
 * 
 * TODO this class should be factored out and the TaxonomyBrowseController should handle both resource taxonomies and country taxonomies.
 * 
 * @author dmartin
 */
public class RegionTaxonomyBrowseController extends RestKeyValueController{

	/** Taxonomy Manager providing tree and taxonConcept lookup */
	protected TaxonomyManager taxonomyManager;
	/** Taxonomy Manager providing tree and taxonConcept lookup */
	protected TaxonConceptUtils taxonConceptUtils;	
	/** Geospatial Manager providing country lookup */
	protected GeospatialManager geospatialManager;	
	/** The request properties taxon concept key */	
	protected String regionPropertyKey = "region";
	/** The request properties taxon concept key */	
	protected String taxonConceptPropertyKey = "taxon";
	/** Threshold used to determining rendering */
	protected String taxonPriorityThresholdModelKey = "taxonPriorityThreshold";	
	/** Threshold used to determining rendering */
	protected int taxonPriorityThreshold = 20;	

	protected MessageSource messageSource;

    protected GeoRegionTaxonConceptDAO geoRegionTaxonConceptDAO;
	/**
	 * @see org.gbif.portal.web.controller.RestController#handleRequest(java.util.Map, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public ModelAndView handleRequest(Map<String, String> propertiesMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		logger.debug("Viewing region taxonomy");
		String regionCode = propertiesMap.get(regionPropertyKey);
        Long regionId = Long.parseLong(regionCode);
		String taxonConceptKey = propertiesMap.get(taxonConceptPropertyKey);
		logger.debug("region id = "+regionId + "|"+regionCode);
        //Create the view
		//ModelAndView mav = resolveAndCreateView(propertiesMap, request, false);
        ModelAndView mav = new ModelAndView("taxonomyRegionBrowse");

		if (regionCode==null) return redirectToDefaultView();

		Locale locale = RequestContextUtils.getLocale(request);
		//CountryDTO countryDTO = geospatialManager.getCountryFor(regionCode, locale);
        GeoRegionDTO geoRegionDTO = geospatialManager.getGeoRegionFor(regionCode);
        logger.debug("geoRegionDTO = "+geoRegionDTO+"; name = "+geoRegionDTO.getName());

        if (geoRegionDTO==null) redirectToDefaultView();
        
		mav.addObject("region", geoRegionDTO);
		mav.addObject("messageSource", messageSource);

         if (regionCode!=null) {
			//List<BriefTaxonConceptDTO> concepts = taxonomyManager.getRootTaxonConceptsForCountry(regionCode);
            List<GeoRegionTaxonConcept> regionConcepts = geoRegionTaxonConceptDAO.getOrderTaxonConceptsForGeoRegion(regionId);
			mav.addObject("regionConcepts", regionConcepts);
		} else {
			return redirectToDefaultView();
		}

//		if (taxonConceptKey!=null && regionCode!=null) {
//			TaxonConceptDTO selectedConcept = taxonomyManager.getTaxonConceptFor(taxonConceptKey);
//			mav.addObject("selectedConcept", selectedConcept);
//			if (logger.isDebugEnabled()){
//				logger.debug(selectedConcept);
//			}
//			if (selectedConcept==null) redirectToDefaultView();
//			List<BriefTaxonConceptDTO> concepts      = taxonomyManager.getClassificationFor(taxonConceptKey, false, regionCode, true);
//			List<BriefTaxonConceptDTO> childConcepts = taxonomyManager.getChildConceptsFor(taxonConceptKey, regionCode, true);
//			taxonConceptUtils.organiseUnconfirmedNames(request, selectedConcept, concepts, childConcepts, taxonPriorityThreshold);
//			mav.addObject("concepts", concepts);
//		} else if (regionCode!=null) {
//			List<BriefTaxonConceptDTO> concepts = taxonomyManager.getRootTaxonConceptsForCountry(regionCode);
//			mav.addObject("concepts", concepts);
//		} else {
//			return redirectToDefaultView();
//		}
		return mav;
	}

	/**
	 * @param regionPropertyKey the regionPropertyKey to set
	 */
	public void setRegionPropertyKey(String regionPropertyKey) {
        this.regionPropertyKey = regionPropertyKey;
    }

	/**
	 * @param geospatialManager the geospatialManager to set
	 */
	public void setGeospatialManager(GeospatialManager geospatialManager) {
		this.geospatialManager = geospatialManager;
	}

	/**
	 * @param taxonConceptPropertyKey the taxonConceptPropertyKey to set
	 */
	public void setTaxonConceptPropertyKey(String taxonConceptPropertyKey) {
		this.taxonConceptPropertyKey = taxonConceptPropertyKey;
	}

	/**
	 * @param taxonomyManager the taxonomyManager to set
	 */
	public void setTaxonomyManager(TaxonomyManager taxonomyManager) {
		this.taxonomyManager = taxonomyManager;
	}

	/**
	 * @param taxonConceptUtils the taxonConceptUtils to set
	 */
	public void setTaxonConceptUtils(TaxonConceptUtils taxonConceptUtils) {
		this.taxonConceptUtils = taxonConceptUtils;
	}

	/**
	 * @param taxonPriorityThreshold the taxonPriorityThreshold to set
	 */
	public void setTaxonPriorityThreshold(int taxonPriorityThreshold) {
		this.taxonPriorityThreshold = taxonPriorityThreshold;
	}

	/**
	 * @param taxonPriorityThresholdModelKey the taxonPriorityThresholdModelKey to set
	 */
	public void setTaxonPriorityThresholdModelKey(
			String taxonPriorityThresholdModelKey) {
		this.taxonPriorityThresholdModelKey = taxonPriorityThresholdModelKey;
	}

	/**
	 * @param messageSource the messageSource to set
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

    /**
	 * @param geoRegionTaxonConceptDAO the geoRegionTaxonConceptDAO to set
	 */
    public void setGeoRegionTaxonConceptDAO(GeoRegionTaxonConceptDAO geoRegionConceptDAO) {
        this.geoRegionTaxonConceptDAO = geoRegionConceptDAO;
    }


}