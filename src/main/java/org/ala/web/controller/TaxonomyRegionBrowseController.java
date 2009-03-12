/* *************************************************************************
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

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.dao.OccurrenceFacetDAO;
import org.ala.model.GeoRegionTaxonConcept;
import org.ala.web.util.RankFacet;
import org.apache.commons.lang.StringUtils;
import org.gbif.portal.dao.geospatial.GeoRegionDAO;
import org.gbif.portal.dto.taxonomy.TaxonConceptDTO;
import org.gbif.portal.model.geospatial.GeoRegion;
import org.gbif.portal.service.TaxonomyManager;
import org.gbif.portal.web.controller.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 * Species/Taxa for a given region
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class TaxonomyRegionBrowseController extends RestController {
    
    /** The GeoRegionDAO */
	protected GeoRegionDAO geoRegionDAO;
    /** The OccurrenceFacetDAO */
    protected OccurrenceFacetDAO occurrenceFacetDAO;
    /** The TaxonomyManager */
    protected TaxonomyManager taxonomyManager;

    /**
	 * @see org.ala.web.controller.RestController#handleRequest(java.util.Map, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
    @Override
	public ModelAndView handleRequest(Map<String, String> properties, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String regionKey = properties.get("region");
        String taxonKey = properties.get("taxon");
        logger.debug("TaxonomyRegionBrowseController: region="+regionKey+"; taxon="+taxonKey);
        
        if (regionKey==null) return redirectToDefaultView();
        
        Long regionId = Long.parseLong(regionKey);
		GeoRegion geoRegion = geoRegionDAO.getGeoRegionFor(regionId);
        //ModelAndView mav = new ModelAndView("taxonomyRegionBrowse");
        ModelAndView mav = resolveAndCreateView(properties, request, removeExtensions);
		mav.addObject("geoRegion", geoRegion);
        
        List<GeoRegionTaxonConcept> regionConcepts;
        String dataTableParam = "";
                
		if (taxonKey != null) {
            regionConcepts = occurrenceFacetDAO.getTaxonConceptsForGeoRegion(regionId, taxonKey);
            TaxonConceptDTO requestedTaxonConceptDTO = taxonomyManager.getTaxonConceptFor(taxonKey);
            //requestedTaxonConceptDTO.getAcceptedConceptKey()
            dataTableParam = geoRegion.getId() + "/taxon/" + taxonKey + "/json/";
            mav.addObject("requestedTaxonConceptDTO", requestedTaxonConceptDTO);
        } else {
			regionConcepts = occurrenceFacetDAO.getTaxonConceptsForGeoRegion(regionId, null);
            dataTableParam = geoRegion.getId() + "/json";
		}

        mav.addObject("regionConcepts", regionConcepts);
        mav.addObject("dataTableParam", dataTableParam);
        return mav;
	}

	/**
	 * @param geoRegionDAO the geoRegionDAO to set
	 */
	public void setGeoRegionDAO(GeoRegionDAO geoRegionDAO) {
		this.geoRegionDAO = geoRegionDAO;
	}

    /**
     * @param occurrenceFacetDAO the occurrenceFacetDAO to set
     */
    public void setOccurrenceFacetDAO(OccurrenceFacetDAO occurrenceFacetDAO) {
        this.occurrenceFacetDAO = occurrenceFacetDAO;
    }

    /**
     * @param taxonomyManager the taxonomyManager to set
     */
    public void setTaxonomyManager(TaxonomyManager taxonomyManager) {
        this.taxonomyManager = taxonomyManager;
    }

}