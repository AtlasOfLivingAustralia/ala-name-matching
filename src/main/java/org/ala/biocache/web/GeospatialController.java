package org.ala.biocache.web;

import javax.inject.Inject;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.ala.biocache.*;
import org.ala.biocache.dao.SearchDAO;

import org.ala.biocache.dto.SearchResultDTO;

import org.apache.log4j.Logger;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import org.ala.biocache.dto.SpatialSearchRequestParams;
import org.springframework.web.bind.annotation.ResponseBody;
import org.w3c.dom.Document;

import net.sf.json.JSONObject;
import org.ala.biocache.dto.OccurrencePoint;
import org.ala.biocache.dto.PointType;
import org.apache.commons.lang.StringUtils;


/**
 * Geospatial controller for the biocache services.
 *
 *
 * @author "Natasha Carter <Natasha.Carter@csiro.au>"
 *
 *
 */
@Controller
public class GeospatialController {

    /** Logger initialisation */
    private final static Logger logger = Logger.getLogger(GeospatialController.class);
    /** Fulltext search DAO */
    @Inject
    protected SearchDAO searchDAO;

    private final String POINTS_GEOJSON = "json/pointsGeoJson";


    /**
     * Performs an occurrence search based on wkt. Use the &facets parameter to
     * limit the facets to the ones required. eg &facets=species_guid
     * @param requestParams
     * @param url
     * @param model
     * @return
     * @throws Exception
     */
    @RequestMapping(value = {"/occurrences/spatial*"}, method = RequestMethod.GET)
    public @ResponseBody SearchResultDTO listWktOccurrences(SpatialSearchRequestParams requestParams,
                                    @RequestParam(value = "url", required = false) String url,
                                    Model model) throws Exception {
        
        requestParams.setQ("*:*");       
        //don't limit the factes
        requestParams.setFlimit(-1);
        //check to see if a url has been provided
        if(url != null && url.length()>0){
            logger.info("Loading the WKT from " + url);
            //get the info from the url it should be XML data
            URL xmlUrl = new URL(url);
            InputStream in = xmlUrl.openStream();
            Document doc = parse(in);
            try{
                
                String geoJson =doc.getElementsByTagName("entry").item(0).getChildNodes().item(0).getNodeValue();
                requestParams.setWkt(wktFromJSON(geoJson));
            }
            catch(Exception e){
                logger.warn("Unable to parse the supplied URL for a WKT", e);
            }
            
        }
        return searchDAO.findByFulltextSpatialQuery(requestParams);
    }

    private static String wktFromJSON(String json) {
        try {
            logger.info("The json:" + json);
            JSONObject obj = JSONObject.fromObject(json);

            //String coords = obj.getJSONArray("geometries").getJSONObject(0).getString("coordinates");
            String coords = obj.getString("coordinates");

            if (obj.getString("type").equalsIgnoreCase("multipolygon")) {
                String wkt = coords.replace("]]],[[[", "))*((").replace("]],[[", "))*((").replace("],[", "*").replace(",", " ").replace("*", ",").replace("[[[[", "MULTIPOLYGON(((").replace("]]]]", ")))");
                return wkt;
            } else {
                String wkt = coords.replace("],[", "*").replace(",", " ").replace("*", ",").replace("[[[[", "POLYGON((").replace("]]]]", "))").replace("],[", "),(");
                return wkt;
            }
        } catch (Exception e) {
            logger.warn("Unable to get JSON", e);
            return "none";
        }
    }

    /**
* Constructs a Document object by reading from an input stream.
*/
    public static Document parse (InputStream is) {
        Document ret = null;
        DocumentBuilderFactory domFactory;
        DocumentBuilder builder;

        try {
            domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setValidating(false);
            domFactory.setNamespaceAware(false);
            builder = domFactory.newDocumentBuilder();

            ret = builder.parse(is);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ret;
    }


    /**
     * GeoJSON view of records as clusters of points within a specified radius of a given location
     *
     * This service will be used by explore your area.
     *
     * TODO: Is this the correct place for this service? 
     *
     */
    @RequestMapping(value = "/geojson/radius-points", method = RequestMethod.GET)
        public String radiusPointsGeoJson(SpatialSearchRequestParams requestParams,


            @RequestParam(value="zoom", required=false, defaultValue="0") Integer zoomLevel,
            @RequestParam(value="bbox", required=false) String bbox,
            @RequestParam(value="group", required=false, defaultValue="ALL_SPECIES") String speciesGroup,
            Model model)
            throws Exception
    {

        //only interested in applying the group if no value for the query has been provided
        if(StringUtils.isEmpty(requestParams.getQ())){
            // Convert array to list so we append more values onto it
            String query = speciesGroup.equals("ALL_SPECIES")?"*:*" : "species_group:"+speciesGroup;
            requestParams.setQ(query);
        }
        PointType pointType = PointType.POINT_00001; // default value for when zoom is null
        pointType = getPointTypeForZoomLevel(zoomLevel);
        logger.info("PointType for zoomLevel ("+zoomLevel+") = "+pointType.getLabel());
        List<OccurrencePoint> points = searchDAO.findRecordsForLocation(requestParams, pointType);
        logger.info("Points search for "+pointType.getLabel()+" - found: "+points.size());
        model.addAttribute("points", points);
        return POINTS_GEOJSON;

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


    public static void main(String[] args) throws Exception{
//        String url = "http://spatial.ala.org.au/gazetteer/ibra/Australian_Alps.xml";
//        URL xmlUrl = new URL(url);
//            InputStream in = xmlUrl.openStream();
//            Document doc = parse(in);
//            System.out.println(doc.getElementsByTagName("entry").getLength());
//            System.out.println(doc.getElementsByTagName("entry").item(0).getChildNodes().item(0).getNodeValue());
        String json = "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[145.648936693066,-37.661942598869],[145.650608044561,-37.6620222269492],[145.652055599241,-37.661858444561]]]]}";
        System.out.println(wktFromJSON(json));
    }
}
