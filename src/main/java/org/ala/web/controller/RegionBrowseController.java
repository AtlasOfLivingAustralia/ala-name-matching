/**
 * 
 */
package org.ala.web.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.ala.dao.geo.GeoRegionDAO;
import org.gbif.portal.model.geospatial.GeoRegion;
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
