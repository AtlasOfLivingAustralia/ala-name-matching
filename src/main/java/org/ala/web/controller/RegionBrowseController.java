/**
 * 
 */
package org.ala.web.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gbif.portal.dao.geospatial.GeoRegionDAO;
import org.gbif.portal.model.geospatial.GeoRegion;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;


/**
 * @author nick
 *
 */
public class RegionBrowseController implements Controller {

	protected GeoRegionDAO geoRegionDAO;	
	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		List<GeoRegion> geoRegions = geoRegionDAO.getGeoRegions();
		ModelAndView mav = new ModelAndView("geoRegionBrowse");
		mav.addObject("geoRegions", geoRegions);
		return mav;
	}

	/**
	 * @param geoRegionDAO the geoRegionDAO to set
	 */
	public void setGeoRegionDAO(GeoRegionDAO geoRegionDAO) {
		this.geoRegionDAO = geoRegionDAO;
	}	
}
