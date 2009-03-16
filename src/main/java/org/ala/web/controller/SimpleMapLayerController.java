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

import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.dao.CellDensityDAO;
import org.ala.io.CellDensityOutputStream;
import org.ala.io.KmlCellDensityOutputStream;
import org.ala.web.util.WebUtils;
import org.apache.log4j.Logger;
import org.gbif.portal.util.geospatial.CellIdUtils;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

/**
 * Simplified map layer controller. 2 tier badness at the minute.
 *
 * @author "Dave Martin (David.Martin@csiro.au)"
 */
public class SimpleMapLayerController implements Controller {

	protected Logger logger = Logger.getLogger(this.getClass());
	
	protected CellDensityDAO cellDensityDAO;

	/**
	 * @see org.springframework.web.servlet.mvc.Controller#handleRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public ModelAndView handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		logger.debug("Retrieving map layer...."+request.getQueryString());
		float unit = ServletRequestUtils.getFloatParameter(request, "unit", 1f);
		final long id = ServletRequestUtils.getLongParameter(request, "id", 0);
		final int type = ServletRequestUtils.getIntParameter(request, "type", 0);
		final String entityName = ServletRequestUtils.getStringParameter(request, "entityName", "entityName");
		final String entityTypeName = ServletRequestUtils.getStringParameter(request, "entityTypeName", "entityTypeName");
		final String format = ServletRequestUtils.getStringParameter(request, "format", "tab");
		final boolean ignoreBoundaries = ServletRequestUtils.getBooleanParameter(request, "ignoreBoundaries", false);
		final boolean gzipped = ServletRequestUtils.getBooleanParameter(request, "gz", false);
		
		Float minX = null;
		Float minY = null;
		Float maxX = null;
		Float maxY = null;

		if(!ignoreBoundaries){
			minX = ServletRequestUtils.getFloatParameter(request, "minx");
			minY = ServletRequestUtils.getFloatParameter(request, "miny");
			maxX = ServletRequestUtils.getFloatParameter(request, "maxx");
			maxY = ServletRequestUtils.getFloatParameter(request, "maxy");
		}
		
//		OutputStream output = response.getOutputStream();
//		File tempFile = DownloadUtils.createTempFileOutput("ala-region-", Long.toString(id), ".kml", false);
//		OutputStream output = new FileOutputStream(tempFile);
		
		CellDensityOutputStream routput = null;
		if(format.equalsIgnoreCase("kmz")){
			routput = new KmlCellDensityOutputStream(response.getOutputStream(), 
					Long.toString(id), entityName, Integer.toString(type), entityTypeName, 
					WebUtils.getHostUrl(request), 
					"ala-"+System.currentTimeMillis()+".kml", true);
			response.setContentType("application/vnd.google-earth.kmz");
		} else if(format.equalsIgnoreCase("kml")){
			routput = new KmlCellDensityOutputStream(response.getOutputStream(), 
					Long.toString(id), entityName, Integer.toString(type), entityTypeName, 
					WebUtils.getHostUrl(request), 
					"ala-"+System.currentTimeMillis()+".kml", false);
			response.setContentType("application/vnd.google-earth.kml+xml");
		} else {
			OutputStream output = null;
			if(gzipped){
				logger.debug("Sending gzipped data....");
				response.setContentType("application/gzip");
				output = new GZIPOutputStream(response.getOutputStream());
			} else {
				output = response.getOutputStream();
			}
			routput = new CellDensityOutputStream(output);
		}
		
		Integer minCellId = null;
		Integer maxCellId = null;
		
		//sanity checks
		if(!ignoreBoundaries && maxX!=null && maxY!=null && minX!=null && minY!=null){
			minX = minX>=-180 ? minX : -180;
			minY = minY>-90 ? minY : -90;
			maxX = maxX<=180 ? maxX : 180;
			maxY = maxY<=90 ? maxY : 90;
			
			minCellId = CellIdUtils.toCellId((float) Math.floor(minY), (float) Math.floor(minX));
			
			Float maxCellIdY =  (float) (Math.floor(maxY+1) >90f ? 90f : Math.floor(maxY+1));
			Float maxCellIdX =  (float) (Math.floor(maxX+1) >180f ? 180f : Math.floor(maxX+1));
			
			maxCellId = CellIdUtils.toCellId(maxCellIdY, maxCellIdX);
		}
		if(unit==0.1f){
			logger.debug("Retrieving centi cells....");
			if(!ignoreBoundaries && maxX!=null && maxY!=null && minX!=null && minY!=null){
				cellDensityDAO.outputCentiCellDensities(id, type, minCellId, maxCellId, routput);
			} else {
				cellDensityDAO.outputCentiCellDensities(id, type, routput);
			}
		} else if(unit==0.01f){
			logger.debug("Retrieving ten milli cells....");
			if(!ignoreBoundaries && maxX!=null && maxY!=null && minX!=null && minY!=null){
				cellDensityDAO.outputTenMilliCellDensities(id, type, minCellId, maxCellId, routput);
			} else {
				cellDensityDAO.outputTenMilliCellDensities(id, type, routput);
			}			
		} else {
			logger.debug("Retrieving 1 deg cells....");
			if(!ignoreBoundaries && maxX!=null && maxY!=null && minX!=null && minY!=null){
				cellDensityDAO.outputCellDensities(id, type, minCellId, maxCellId, routput);
			} else {
				cellDensityDAO.outputCellDensities(id, type, routput);
			}
		}
		
		//close the stream
		routput.close();
//		String fileName = tempFile.getName();
//		tempFile.renameTo(new File(tempFile.getParent()+File.separator+fileName.substring(0, fileName.length()-DownloadUtils.temporaryExtensionIndicator.length() )));
		return null;
	}

	/**
	 * @param cellDensityDAO
	 */
	public void setCellDensityDAO(CellDensityDAO cellDensityDAO) {
		this.cellDensityDAO = cellDensityDAO;
	}
}