package org.ala.web.controller.maplayer;

import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.dao.geo.CellDensityDAO;
import org.ala.io.CellDensityOutputStream;
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
	
	protected boolean includeHeader = true;

	/**
	 * @see org.springframework.web.servlet.mvc.Controller#handleRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public ModelAndView handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		logger.info("Retrieving map layer...."+request.getQueryString());
		float unit = ServletRequestUtils.getFloatParameter(request, "unit", 1f);
		final long id = ServletRequestUtils.getLongParameter(request, "id", 0);
		final int type = ServletRequestUtils.getIntParameter(request, "type", 0);
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
		
		OutputStream output = response.getOutputStream();
		if(gzipped){
			logger.info("Sending gzipped data....");
			response.setContentType("application/gzip");
			output = new GZIPOutputStream(output);
		}
		CellDensityOutputStream routput = new CellDensityOutputStream(output);
		
		if(includeHeader){
			output.write("MINX\tMINY\tMAXX\tMAXY\tDENSITY\n".getBytes());
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
			logger.info("Retrieving centi cells....");
			if(!ignoreBoundaries && maxX!=null && maxY!=null && minX!=null && minY!=null){
				cellDensityDAO.outputCentiCellDensities(id, type, minCellId, maxCellId, routput);
			} else {
				cellDensityDAO.outputCentiCellDensities(id, type, routput);
			}
		} else if(unit==0.01f){
			logger.info("Retrieving ten milli cells....");
			cellDensityDAO.outputTenMilliCellDensities(id, type, routput);
		} else {
			logger.info("Retrieving 1 deg cells....");
			if(!ignoreBoundaries && maxX!=null && maxY!=null && minX!=null && minY!=null){			
				cellDensityDAO.outputCellDensities(id, type, minCellId, maxCellId, routput);
			} else {
				cellDensityDAO.outputCellDensities(id, type, routput);
			}
		}
		output.close();
		return null;
	}

	/**
	 * @param cellDensityDAO
	 */
	public void setCellDensityDAO(CellDensityDAO cellDensityDAO) {
		this.cellDensityDAO = cellDensityDAO;
	}
}