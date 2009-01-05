/**
 * 
 */
package org.ala.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gbif.portal.dao.geospatial.GeoRegionDAO;
import org.gbif.portal.model.geospatial.GeoRegion;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

/**
 * Initial stab at a region page.
 *
 * @author "Dave Martin (David.Martin@csiro.au)"
 */
public class RegionController implements Controller {

	protected GeoRegionDAO geoRegionDAO;
	
	public ModelAndView handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		Long geoRegionId = ServletRequestUtils.getLongParameter(request,"id", 1);
		GeoRegion geoRegion = geoRegionDAO.getGeoRegionFor(geoRegionId);
		ModelAndView mav = new ModelAndView("geoRegionView");
		mav.addObject("geoRegion", geoRegion);
		return mav;
	}

	/**
	 * @param geoRegionDAO the geoRegionDAO to set
	 */
	public void setGeoRegionDAO(GeoRegionDAO geoRegionDAO) {
		this.geoRegionDAO = geoRegionDAO;
	}
}
