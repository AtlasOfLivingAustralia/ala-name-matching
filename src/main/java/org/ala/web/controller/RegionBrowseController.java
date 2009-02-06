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

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.dao.GeoRegionDAO;
import org.apache.commons.lang.StringUtils;
import org.gbif.portal.model.geospatial.GeoRegion;
import org.gbif.portal.web.controller.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 * A Rest controller for browsing geographic regions.
 * 
 * @author "Nick dos Remedios (Nick.dosRemedios@csiro.au)"
 *
 */
public class RegionBrowseController extends RestController {

	protected GeoRegionDAO geoRegionDAO;
	
	public ModelAndView handleRequest(Map<String, String> properties, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		String regionTypeKey = properties.get("regionType");
		
		if(StringUtils.isBlank(regionTypeKey)){
			regionTypeKey = "states";
		}
			
		RegionType geoRegionType = new RegionType(regionTypeKey);
		Long minGeoRegionTypeId = geoRegionType.getMinTypeId();
		Long maxGeoRegionTypeId = geoRegionType.getMaxTypeId();
		List<GeoRegion> geoRegions = geoRegionDAO.getGeoRegionsForGeoRegionType(minGeoRegionTypeId, maxGeoRegionTypeId);
		
		ModelAndView mav = new ModelAndView("geoRegionBrowse");
		mav.addObject("geoRegions", geoRegions);
		mav.addObject("geoRegionType", geoRegionType);
		return mav;
	}
	
	/**
	 * @param geoRegionDAO the geoRegionDAO to set
	 */
	public void setGeoRegionDAO(GeoRegionDAO geoRegionDAO) {
		this.geoRegionDAO = geoRegionDAO;
	}
}
