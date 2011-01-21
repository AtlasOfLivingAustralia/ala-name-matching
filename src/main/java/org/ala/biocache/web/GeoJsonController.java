/* *************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
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

package org.ala.biocache.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.ala.biocache.dao.SearchDAO;
import org.ala.biocache.dto.OccurrenceCell;
import org.ala.biocache.dto.OccurrencePoint;
import org.ala.biocache.dto.PointType;
import org.ala.biocache.dto.SearchQuery;
import org.ala.biocache.util.SearchUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
@Controller
public class GeoJsonController {
    /** Logger initialisation */
	private final static Logger logger = Logger.getLogger(GeoJsonController.class);

    /** Fulltext search DAO */
    @Inject
    protected SearchDAO searchDAO;
    /** Name of view for points GeoJSON service */
	private final String POINTS_GEOJSON = "json/pointsGeoJson";
    /** Name of view for square cells GeoJSON service */
	private final String CELLS_GEOJSON = "json/cellsGeoJson";
    /** Search Utils helper class */
    @Inject
    protected SearchUtils searchUtils;

    /**
     * GeoJSON view of records as clusters of points
     *
     * @param query
     * @param filterQuery
     * @param callback
     * @param zoomLevel
     * @param bbox
     * @param model
     * @param response
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/geojson/points", method = RequestMethod.GET)
	public String pointsGeoJson(
            @RequestParam(value="q", required=true) String query,
            @RequestParam(value="fq", required=false) String[] filterQuery,
            @RequestParam(value="callback", required=false) String callback,
            @RequestParam(value="zoom", required=false, defaultValue="0") Integer zoomLevel,
            @RequestParam(value="bbox", required=false) String bbox,
            @RequestParam(value="type", required=false, defaultValue="normal") String type,
            Model model,
            HttpServletResponse response)
            throws Exception {

        if (callback != null && !callback.isEmpty()) {
            response.setContentType("text/javascript");
        } else {
            response.setContentType("application/json");
        }

        // Convert array to list so we append more values onto it
        ArrayList<String> fqList = null;
        if (filterQuery != null) {
            fqList = new ArrayList<String>(Arrays.asList(filterQuery));
        } else {
            fqList = new ArrayList<String>();
        }

        PointType pointType = PointType.POINT_1;
        pointType = getPointTypeForZoomLevel(zoomLevel);

        String[] newFilterQuery = (String[]) fqList.toArray (new String[fqList.size()]); // convert back to array
        //get the new query details
        SearchQuery searchQuery = new SearchQuery(query, type, newFilterQuery);
        searchUtils.updateQueryDetails(searchQuery);
        List<OccurrencePoint> points = searchDAO.getFacetPoints(searchQuery.getQuery(), searchQuery.getFilterQuery(), pointType);
        logger.debug("Points search for "+pointType.getLabel()+" - found: "+points.size());
        model.addAttribute("points", points);

        return POINTS_GEOJSON;
    }

    /**
     * GeoJSON view of records as clusters of points within a specified radius of a given location
     *
     * @param latitude
     * @param longitude
     * @param radius
     * @param callback
     * @param zoomLevel
     * @param bbox
     * @param taxa
     * @param rank
     * @param model
     * @param response
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/geojson/radius-points", method = RequestMethod.GET)
	public String radiusPointsGeoJson(
            @RequestParam(value="lat", required=true) Float latitude,
            @RequestParam(value="long", required=true) Float longitude,
            @RequestParam(value="radius", required=true) Float radius,
            @RequestParam(value="callback", required=false) String callback,
            @RequestParam(value="zoom", required=false, defaultValue="0") Integer zoomLevel,
            @RequestParam(value="bbox", required=false) String bbox,
            @RequestParam(value="taxa", required=false, defaultValue="*") String taxa,
            @RequestParam(value="rank", required=false, defaultValue="*") String rank,
            Model model,
            HttpServletResponse response)
            throws Exception {

        if (callback != null && !callback.isEmpty()) {
            response.setContentType("text/javascript");
        } else {
            response.setContentType("application/json");
        }

        // Convert array to list so we append more values onto it
        String[] taxaArray = StringUtils.split(taxa, "|");
        ArrayList<String> taxaList = null;
        if (taxaArray != null) {
            taxaList = new ArrayList<String>(Arrays.asList(taxaArray));
        } else {
            taxaList = new ArrayList<String>();
        }

        PointType pointType = PointType.POINT_00001; // default value for when zoom is null
        pointType = getPointTypeForZoomLevel(zoomLevel);
        logger.info("PointType for zoomLevel ("+zoomLevel+") = "+pointType.getLabel());
        List<OccurrencePoint> points = searchDAO.findRecordsForLocation(taxaList, rank, latitude, longitude, radius, pointType);
        logger.info("Points search for "+pointType.getLabel()+" - found: "+points.size());
        model.addAttribute("points", points);

        return POINTS_GEOJSON;
    }

    /**
     * GeoJSON view of records as square (cell) polygons
     *
     * @param query
     * @param filterQuery
     * @param callback
     * @param zoomLevel
     * @param bbox
     * @param model
     * @param response
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/geojson/cells", method = RequestMethod.GET)
	public String cellsGeoJson(
            @RequestParam(value="q", required=true) String query,
            @RequestParam(value="fq", required=false) String[] filterQuery,
            @RequestParam(value="callback", required=false) String callback,
            @RequestParam(value="zoom", required=false, defaultValue="0") Integer zoomLevel,
            @RequestParam(value="bbox", required=false) String bbox,
            @RequestParam(value="type", required=false, defaultValue="normal") String type,
            Model model,
            HttpServletResponse response)
            throws Exception {

        if (callback != null && !callback.isEmpty()) {
            response.setContentType("text/javascript");
        } else {
            response.setContentType("application/json");
        }

        // Convert array to list so we append more values onto it
        ArrayList<String> fqList = null;
        if (filterQuery != null) {
            fqList = new ArrayList<String>(Arrays.asList(filterQuery));
        } else {
            fqList = new ArrayList<String>();
        }

        bboxToQuery(bbox, fqList);

        PointType pointType = PointType.POINT_1;
        pointType = getPointTypeForZoomLevel(zoomLevel);

        String[] newFilterQuery = (String[]) fqList.toArray (new String[fqList.size()]); // convert back to array
         //get the new query details
        SearchQuery searchQuery = new SearchQuery(query, type, newFilterQuery);
        searchUtils.updateQueryDetails(searchQuery);
        logger.debug("Searching cells for query: "+ searchQuery.getQuery());
        List<OccurrencePoint> points = searchDAO.getFacetPoints(searchQuery.getQuery(), searchQuery.getFilterQuery(), pointType);

        logger.debug("Cells search for "+pointType.getLabel()+" - found: "+points.size());
        List<OccurrenceCell> cells = new ArrayList<OccurrenceCell>();

        // Convert points to cells
        for (OccurrencePoint point : points) {
            OccurrenceCell cell = new OccurrenceCell(point);
            cells.add(cell);
        }

        model.addAttribute("cells", cells);

        return CELLS_GEOJSON;
    }

    /**
     * Map a zoom level to a coordinate accuracy level
     *
     * @param zoomLevel
     * @return
     */
    protected PointType getPointTypeForZoomLevel(Integer zoomLevel) {
        PointType pointType = null;
        // Map zoom levels to lat/long accuracy levels
        if (zoomLevel != null) {
            if (zoomLevel >= 0 && zoomLevel <= 6) {
                // 0-6 levels
                pointType = PointType.POINT_1;
            } else if (zoomLevel > 6 && zoomLevel <= 8) {
                // 6-7 levels
                pointType = PointType.POINT_01;
            } else if (zoomLevel > 8 && zoomLevel <= 10) {
                // 8-9 levels
                pointType = PointType.POINT_001;
            } else if (zoomLevel > 10 && zoomLevel <= 13) {
                // 10-12 levels
                pointType = PointType.POINT_0001;
            } else if (zoomLevel > 13 && zoomLevel <= 15) {
                // 12-n levels
                pointType = PointType.POINT_00001;
            } else {
                // raw levels
                pointType = PointType.POINT_RAW;
            }
        }
        return pointType;
    }

    /**
     * Reformat bbox param to SOLR spatial query and add to fq list
     *
     * @param bbox
     * @param fqList
     */
    protected void bboxToQuery(String bbox, ArrayList<String> fqList) {
        // e.g. bbox=122.013671875,-53.015625,172.990234375,-10.828125
        if (bbox != null && !bbox.isEmpty()) {
            String[] bounds = StringUtils.split(bbox, ",");
            if (bounds.length == 4) {
                String fq1 = "longitude:[" + bounds[0] + " TO " + bounds[2] + "]";
                fqList.add(fq1);
                String fq2 = "latitude:[" + bounds[1] + " TO " + bounds[3] + "]";
                fqList.add(fq2);
            } else {
                logger.warn("BBOX does not contain the expected number of coords (4). Found: " + bounds.length);
            }
        }
    }

    public void setSearchDAO(SearchDAO searchDAO) {
        this.searchDAO = searchDAO;
    }

    public void setSearchUtils(SearchUtils searchUtils) {
        this.searchUtils = searchUtils;
    }

}
