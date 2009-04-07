/* *************************************************************************
 *  Copyright (C) 2009 Atlas of Living Australia
 *  All Rights Reserved.
 *  
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *  
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/

package org.ala.web.controller;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.dao.OccurrenceFacetDAO;
import org.ala.gis.GisUtils;
import org.ala.model.OccurrenceSearchCounts;
import org.apache.log4j.Logger;
import org.gbif.portal.util.geospatial.CellIdUtils;
import org.gbif.portal.util.geospatial.LatLongBoundingBox;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

/**
 * Controller for AJAX request to display popup info  of counts
 * for a given cell/centi cell/ten milli cell.
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class OccurrenceCellInfoController implements Controller {
    
    protected Logger logger = Logger.getLogger(this.getClass());
	
    private OccurrenceFacetDAO occurrenceFacetDAO;

    private String view = "cellInfo";

    /**
	 * @see org.springframework.web.servlet.mvc.Controller#handleRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public ModelAndView handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
        Float unit      = ServletRequestUtils.getFloatParameter(request, "unit", 1f);
        Float latitude  = ServletRequestUtils.getFloatParameter(request, "lat", 0f);
        Float longitude = ServletRequestUtils.getFloatParameter(request, "lon", 0f);
        String extraParams = ServletRequestUtils.getStringParameter(request, "extraParams");
        String entityPath  = ServletRequestUtils.getStringParameter(request, "entityPath");
        String entityId    = ServletRequestUtils.getStringParameter(request, "entityId");

        Integer cellId = null;
        Integer centiCellId = null;
        Integer tenMilliCellId = null;
        LatLongBoundingBox bbox = null;

        if (unit == null) {
            return null;
        } else if (unit == 1f) {
            // cell requested
            cellId = CellIdUtils.toCellId(latitude, longitude);
            bbox = CellIdUtils.toBoundingBox(cellId);
        } else if (unit == 0.1f) {
            // centi cell requested
            cellId = CellIdUtils.toCellId(latitude, longitude);
            centiCellId = CellIdUtils.toCentiCellId(latitude, longitude);
            bbox = CellIdUtils.toBoundingBox(cellId, centiCellId);
        } else if (unit == 0.01f) {
            // ten milli cell requested
            cellId = CellIdUtils.toCellId(latitude, longitude);
            centiCellId = CellIdUtils.toCentiCellId(latitude, longitude);
            tenMilliCellId = GisUtils.toTenMilliCellId(latitude, longitude);
            bbox = CellIdUtils.toBoundingBox(cellId, centiCellId);  // TODO create method for ten milli cells
        }
        
        String constraint = null;
        if (entityPath != null && entityId != null) {
            constraint = entityPath + ":" + entityId;
        }
        
        OccurrenceSearchCounts occurrenceSearchCounts = occurrenceFacetDAO.getChartFacetsForMapCell(cellId, centiCellId, tenMilliCellId, entityPath, entityId);

        ModelAndView mav = new ModelAndView(view);
        mav.addObject("occurrenceSearchCounts", occurrenceSearchCounts);
        mav.addObject("bbox", bbox);
        mav.addObject("extraParams", extraParams);
        //map.put("occurrenceSearchCounts", occurrenceSearchCounts);
        return mav;
    }

    /**
     * @param occurrenceFacetDAO the occurrenceFacetDAO to set
     */
    public void setOccurrenceFacetDAO(OccurrenceFacetDAO occurrenceFacetDAO) {
        this.occurrenceFacetDAO = occurrenceFacetDAO;
    }

    /**
     * @param view the view to set
     */
    public void setView(String view) {
        this.view = view;
    }
}
