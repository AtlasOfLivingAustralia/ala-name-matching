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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.gbif.portal.dao.geospatial.GeoRegionDAO;
import org.gbif.portal.model.geospatial.GeoRegion;
import org.gbif.portal.web.controller.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 * Initial stab at a region page.
 * 
 * TODO Add data breakdowns etc
 * 
 * @author "Dave Martin (David.Martin@csiro.au)"
 */
public class RegionController extends RestController {

	protected GeoRegionDAO geoRegionDAO;
	
	/**
	 * @see org.ala.web.controller.RestController#handleRequest(java.util.Map, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public ModelAndView handleRequest(Map<String, String> properties, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String regionKey = properties.get("region");
		if(StringUtils.isNotBlank(regionKey)){
			Long regionId = Long.parseLong(regionKey);
			GeoRegion geoRegion = geoRegionDAO.getGeoRegionFor(regionId);
			if(geoRegion!=null){
				ModelAndView mav = new ModelAndView("geoRegionView");
				mav.addObject("geoRegion", geoRegion);
				return mav;
			}
		}
		return redirectToDefaultView();
	}

	/**
	 * @param geoRegionDAO the geoRegionDAO to set
	 */
	public void setGeoRegionDAO(GeoRegionDAO geoRegionDAO) {
		this.geoRegionDAO = geoRegionDAO;
	}
}