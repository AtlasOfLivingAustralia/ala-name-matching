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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ala.biocache.dao.SearchDAO;
import org.ala.biocache.dto.OccurrencePoint;
import org.ala.biocache.dto.PointType;
import org.ala.biocache.dto.SearchQuery;
import org.ala.biocache.util.SearchUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author "Ajay Ranipeta <Ajay.Ranipeta@csiro.au>"
 */
@Controller
public class WMSController {

    /** Logger initialisation */
    private final static Logger logger = Logger.getLogger(WMSController.class);
    /** Fulltext search DAO */
    @Inject
    protected SearchDAO searchDAO;
    /** Search Utils helper class */
    @Inject
    protected SearchUtils searchUtils;

    @RequestMapping(value = "/occurrences/wms", method = RequestMethod.GET)
    public void pointsWmsImage(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "fq", required = false) String[] filterQuery,
            @RequestParam(value = "width", required = false, defaultValue = "256") Integer widthObj,
            @RequestParam(value = "height", required = false, defaultValue = "256") Integer heightObj,
            @RequestParam(value = "zoom", required = false, defaultValue = "0") Integer zoomLevel,
            @RequestParam(value = "bbox", required = false, defaultValue = "110,-45,157,-9") String bboxString,
            @RequestParam(value = "type", required = false, defaultValue = "normal") String type,
            //Model model,
            HttpServletResponse response)
            throws Exception {


        int width = widthObj.intValue();
        int height = heightObj.intValue();


        if (StringUtils.isBlank(query)) {
            displayBlankImage(width, height, false, response);
            return;
        }

        // let's force it to PNG's for now 
        response.setContentType("image/png");

        // Convert array to list so we append more values onto it
        ArrayList<String> fqList = null;
        if (filterQuery != null) {
            fqList = new ArrayList<String>(Arrays.asList(filterQuery));
        } else {
            fqList = new ArrayList<String>();
        }

        // the bounding box
        double[] bbox = new double[4];
        int i;
        i = 0;
        for (String s : bboxString.split(",")) {
            try {
                bbox[i] = Double.parseDouble(s);
                i++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        bbox[0] = convertMetersToLng(bbox[0]);
        bbox[1] = convertMetersToLat(bbox[1]);
        bbox[2] = convertMetersToLng(bbox[2]);
        bbox[3] = convertMetersToLat(bbox[3]);


        String bboxString2 = bbox[0] + "," + bbox[1] + "," + bbox[2] + "," + bbox[3];
        System.out.println("bboxS1: " + bboxString + "\nbboxS2:" + bboxString2);
        bboxToQuery(bboxString2, fqList);

        PointType pointType = PointType.POINT_RAW;
        //pointType = getPointTypeForZoomLevel(zoomLevel);

        String[] newFilterQuery = (String[]) fqList.toArray(new String[fqList.size()]); // convert back to array
        //get the new query details
        System.out.println("getting query details");
        SearchQuery searchQuery = new SearchQuery(query, type, newFilterQuery);
        System.out.println("updating query details");
        searchUtils.updateQueryDetails(searchQuery);
        System.out.println("searching index");
        List<OccurrencePoint> points = searchDAO.getFacetPoints(searchQuery.getQuery(), searchQuery.getFilterQuery(), pointType);
        //logger.debug("Points search for " + pointType.getLabel() + " - found: " + points.size());
        System.out.println("Points search for " + pointType.getLabel() + " - found: " + points.size());
        //model.addAttribute("points", points);

        if (points.size() == 0) {
            displayBlankImage(width, height, false, response);
            return;
        }

        //BufferedImage baseImage = ImageIO.read(new File("/data/tmp/mapaus1_white.png"));
        //width = baseImage.getWidth();
        //height = baseImage.getHeight();

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
        //g.drawImage(baseImage, 0, 0, null);
        g.setColor(Color.RED);

        // size of the circles
        int size = 4;

        double pixelWidth = (bbox[2] - bbox[0]) / width;
        double pixelHeight = (bbox[3] - bbox[1]) / height;
        bbox[0] += pixelWidth / 2;
        bbox[2] -= pixelWidth / 2;
        bbox[1] += pixelHeight / 2;
        bbox[3] -= pixelHeight / 2;

        double[] pbbox = new double[4]; //pixel bounding box
        System.out.println("converting meters to pixels");
        pbbox[0] = convertLngToPixel(bbox[0]);
        pbbox[1] = convertLatToPixel(bbox[1]);
        pbbox[2] = convertLngToPixel(bbox[2]);
        pbbox[3] = convertLatToPixel(bbox[3]);

        System.out.println("pbbox: " + pbbox);

        int x, y;
        int pointWidth = size * 2;
        double width_mult = (width / (pbbox[2] - pbbox[0]));
        double height_mult = (height / (pbbox[1] - pbbox[3]));

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float radius = 10f;

        for (i = 0; i < points.size(); i++) {
            OccurrencePoint pt = points.get(i);
            float lng = pt.getCoordinates().get(0).floatValue();
            float lat = pt.getCoordinates().get(1).floatValue();

            x = (int) ((convertLngToPixel(lng) - pbbox[0]) * width_mult);
            y = (int) ((convertLatToPixel(lat) - pbbox[3]) * height_mult);

            System.out.println("Writing " + lng + "," + lat + " to " + x + "," + y);


            Shape circle = new Ellipse2D.Float(x - (radius / 2), y - (radius / 2), radius, radius);
            g.setPaint(Color.blue);
            g.draw(circle);
            g.fill(circle);
            g.setPaint(Color.RED);
            g.fillOval(x - (size / 2), y - (size / 2), pointWidth, pointWidth);
        }

        g.dispose();

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(img, "png", outputStream);
            ServletOutputStream outStream = response.getOutputStream();
            outStream.write(outputStream.toByteArray());
            outStream.flush();
            outStream.close();

        } catch (Exception e) {
            System.out.println("Unable to write image: ");
            e.printStackTrace(System.out);
        }

    }

    @RequestMapping(value = "/occurrences/static", method = RequestMethod.GET)
    public void pointsStaticImage(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "fq", required = false) String[] filterQuery,
            @RequestParam(value = "width", required = false, defaultValue = "256") Integer widthObj,
            @RequestParam(value = "height", required = false, defaultValue = "256") Integer heightObj,
            @RequestParam(value = "type", required = false, defaultValue = "normal") String type,
            //Model model,
            HttpServletResponse response)
            throws Exception {

        int width = widthObj.intValue();
        int height = heightObj.intValue();

        if (StringUtils.isBlank(query)) {
            displayBlankImage(width, height, true, response);
            return;
        }

        // let's force it to PNG's for now
        response.setContentType("image/png");



        // Convert array to list so we append more values onto it
        ArrayList<String> fqList = null;
        if (filterQuery != null) {
            fqList = new ArrayList<String>(Arrays.asList(filterQuery));
        } else {
            fqList = new ArrayList<String>();
        }

        PointType pointType = PointType.POINT_RAW;

        String[] newFilterQuery = (String[]) fqList.toArray(new String[fqList.size()]); // convert back to array
        //get the new query details
        SearchQuery searchQuery = new SearchQuery(query, type, newFilterQuery);
        searchUtils.updateQueryDetails(searchQuery);
        List<OccurrencePoint> points = searchDAO.getFacetPoints(searchQuery.getQuery(), searchQuery.getFilterQuery(), pointType);
        logger.debug("Points search for " + pointType.getLabel() + " - found: " + points.size());
        //model.addAttribute("points", points);

        if (points.size() == 0) {
            displayBlankImage(width, height, true, response);
            return;
        }


        BufferedImage baseImage = ImageIO.read(new File("/data/tmp/mapaus1_white.png"));
        width = baseImage.getWidth();
        height = baseImage.getHeight();

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.drawImage(baseImage, 0, 0, null);
        g.setColor(Color.RED);

        // size of the circles
        int size = 4;

        // the bounding box
        String bboxString = "110,-45,157,-9";
        double[] bbox = new double[4];
        int i;
        i = 0;
        for (String s : bboxString.split(",")) {
            try {
                bbox[i] = Double.parseDouble(s);
                i++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        double pixelWidth = (bbox[2] - bbox[0]) / width;
        double pixelHeight = (bbox[3] - bbox[1]) / height;
        bbox[0] += pixelWidth / 2;
        bbox[2] -= pixelWidth / 2;
        bbox[1] += pixelHeight / 2;
        bbox[3] -= pixelHeight / 2;

        double[] pbbox = new double[4]; //pixel bounding box
        pbbox[0] = convertLngToPixel(bbox[0]);
        pbbox[1] = convertLatToPixel(bbox[1]);
        pbbox[2] = convertLngToPixel(bbox[2]);
        pbbox[3] = convertLatToPixel(bbox[3]);

        int x, y;
        int pointWidth = size * 2;
        double width_mult = (width / (pbbox[2] - pbbox[0]));
        double height_mult = (height / (pbbox[1] - pbbox[3]));

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float radius = 10f;

        for (i = 0; i < points.size(); i++) {
            OccurrencePoint pt = points.get(i);
            float lng = pt.getCoordinates().get(0).floatValue();
            float lat = pt.getCoordinates().get(1).floatValue();

            x = (int) ((convertLngToPixel(lng) - pbbox[0]) * width_mult);
            y = (int) ((convertLatToPixel(lat) - pbbox[3]) * height_mult);


            Shape circle = new Ellipse2D.Float(x - (radius / 2), y - (radius / 2), radius, radius);
            g.setPaint(Color.blue);
            g.draw(circle);
            g.fill(circle);
            g.setPaint(Color.RED);
            g.fillOval(x - (size / 2), y - (size / 2), pointWidth, pointWidth);
        }

        g.dispose();

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(img, "png", outputStream);
            ServletOutputStream outStream = response.getOutputStream();
            outStream.write(outputStream.toByteArray());
            outStream.flush();
            outStream.close();

        } catch (Exception e) {
            System.out.println("Unable to write image: ");
            e.printStackTrace(System.out);
        }

    }

    @RequestMapping(value = "/occurrences/info", method = RequestMethod.GET)
    public String getOccurrencesInformation(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "fq", required = false) String[] filterQuery,
            @RequestParam(value="callback", required=false) String callback,
            @RequestParam(value = "lat", required = true) Float latitude,
            @RequestParam(value = "lng", required = true) Float longitude,
            @RequestParam(value="radius", required=false, defaultValue = "0.1") Float radius,
            @RequestParam(value = "zoom", required = false, defaultValue = "0") Integer zoomLevel,
            @RequestParam(value="taxa", required=false, defaultValue="*") String taxa,
            @RequestParam(value="rank", required=false, defaultValue="*") String rank,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {

        System.out.println("Got request.getQueryString: " + request.getQueryString());

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

        // Convert array to list so we append more values onto it
        ArrayList<String> fqList = null;
        if (filterQuery != null) {
            fqList = new ArrayList<String>(Arrays.asList(filterQuery));
        } else {
            fqList = new ArrayList<String>();
        }

        PointType pointType = PointType.POINT_00001; // default value for when zoom is null
        pointType = getPointTypeForZoomLevel(zoomLevel);
        logger.info("PointType for zoomLevel ("+zoomLevel+") = "+pointType.getLabel());

        String[] newFilterQuery = (String[]) fqList.toArray (new String[fqList.size()]); // convert back to array
        //get the new query details
        SearchQuery searchQuery = new SearchQuery(query, "normal", newFilterQuery);
        searchUtils.updateQueryDetails(searchQuery);
        System.out.println("searchQuery: " + searchQuery.getDisplayString()); 
        //List<OccurrencePoint> points = searchDAO.findRecordsForLocation(taxaList, rank, latitude, longitude, radius, pointType);
        List<OccurrencePoint> points = searchDAO.getFacetPoints(searchQuery.getQuery(), searchQuery.getFilterQuery(), pointType);
        logger.info("Click Points search for "+pointType.getLabel()+" - found: "+points.size());
        model.addAttribute("points", points);

        return "json/infoPointGeojson"; //POINTS_GEOJSON;
    }

    private void displayBlankImage(int width, int height, boolean useBase, HttpServletResponse response) {
        try {
            response.setContentType("image/png");

            BufferedImage baseImage = null;

            //BufferedImage baseImage = ImageIO.read(new File("/data/tmp/mapaus1_white.png"));
            //BufferedImage baseImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            if (useBase) {
                baseImage = ImageIO.read(new File("/data/tmp/mapaus1_white.png"));
            } else {
                baseImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                //baseImage.getGraphics().fillRect(0, 0, width, height);
            }


            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(baseImage, "png", outputStream);
            ServletOutputStream outStream = response.getOutputStream();
            outStream.write(outputStream.toByteArray());
            outStream.flush();
            outStream.close();

        } catch (Exception e) {
            System.out.println("Unable to write image: ");
            e.printStackTrace(System.out);
        }
    }
    private int map_offset = 268435456; // half the Earth's circumference at zoom level 21
    private double map_radius = map_offset / Math.PI;

    public int convertLatToPixel(double lat) {
        return (int) Math.round(map_offset - map_radius
                * Math.log((1 + Math.sin(lat * Math.PI / 180))
                / (1 - Math.sin(lat * Math.PI / 180))) / 2);
    }

    public int convertLngToPixel(double lng) {
        return (int) Math.round(map_offset + map_radius * lng * Math.PI / 180);
    }

    private double convertMetersToLng(double meters) {
        return meters / 20037508.342789244 * 180;
    }

    private double convertMetersToLat(double meters) {
        return 180.0 / Math.PI * (2 * Math.atan(Math.exp(meters / 20037508.342789244 * Math.PI)) - Math.PI / 2.0);
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
    protected void radiusToQuery(Float latitude, Float longitude, Float radius, ArrayList<String> fqList) {
        // e.g. bbox=122.013671875,-53.015625,172.990234375,-10.828125
        //if (bbox != null && !bbox.isEmpty()) {
                String fq1 = "longitude:" + longitude.doubleValue();
                fqList.add(fq1);
                String fq2 = "latitude:" + latitude.doubleValue();
                fqList.add(fq2);
                String fq3 = "radius:" + radius.doubleValue();
                fqList.add(fq3);
        //}
    }

    /**
     * Format the search input query for a full-text search
     *
     * @param query
     * @return
     */
    protected String formatSearchQuery(String query) {
        // set the query
        StringBuilder queryString = new StringBuilder();
        if (query.equals("*:*") || query.contains(" AND ") || query.contains(" OR ") || query.startsWith("(")
                || query.endsWith("*") || query.startsWith("{")) {
            queryString.append(query);
        } else if (query.contains(":") && !query.startsWith("urn")) {
            // search with a field name specified (other than an LSID guid)
            String[] bits = StringUtils.split(query, ":", 2);
            queryString.append(ClientUtils.escapeQueryChars(bits[0]));
            queryString.append(":");
            queryString.append(ClientUtils.escapeQueryChars(bits[1]));
        } else {
            // regular search
            queryString.append(ClientUtils.escapeQueryChars(query));
        }
        return queryString.toString();
    }

    /**
     * Build the query string for a spatial query (using Spatial-Solr plugin syntax)
     *
     * TODO change param type to SearchRequestParams
     *
     * @param fullTextQuery
     * @param latitude
     * @param longitude
     * @param radius
     * @return
     */
    protected String buildSpatialQueryString(String fullTextQuery, Float latitude, Float longitude, Float radius) {
        String queryString = "{!spatial lat=" + latitude.toString() + " long=" + longitude.toString() +
                " radius=" + radius.toString() + " unit=km calc=arc threadCount=2}" + fullTextQuery; // calc=arc|plane
        return queryString;
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

    public void setSearchDAO(SearchDAO searchDAO) {
        this.searchDAO = searchDAO;
    }

    public void setSearchUtils(SearchUtils searchUtils) {
        this.searchUtils = searchUtils;
    }
}
