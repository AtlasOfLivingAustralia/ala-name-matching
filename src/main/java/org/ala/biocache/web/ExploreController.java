/**************************************************************************
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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.biocache.dao.SearchDAO;
import org.ala.biocache.dto.TaxaCountDTO;
import org.ala.biocache.util.TaxaGroup;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;

/**
 * Controller for the "explore your area" page
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
@Controller("exploreController")
public class ExploreController {
    /** Logger initialisation */
	private final static Logger logger = Logger.getLogger(ExploreController.class);

    /** Fulltext search DAO */
    @Inject
    protected SearchDAO searchDao;
    /** Name of view for site home page */
	private String YOUR_AREA = "explore/yourArea";
    private String speciesPageUrl = "http://bie.ala.org.au/species/";
    private static final String GEOIP_DATABASE = "/data/geoip/GeoLiteCity.dat"; // get from http://www.maxmind.com/app/geolitecity
//    private HashMap<String, List<Float>> addressCache = new HashMap<String, List<Float>>();
    private final String DEFAULT_LOCATION = "Clunies Ross St, Black Mountain, ACT";
    private static LookupService lookupService = null;
    /** Mapping of radius in km to OpenLayers zoom level */
    public final static HashMap<Float, Integer> radiusToZoomLevelMap = new HashMap<Float, Integer>();
	static {
		radiusToZoomLevelMap.put(1f, 14);
        radiusToZoomLevelMap.put(5f, 12);
		radiusToZoomLevelMap.put(10f, 11);
		radiusToZoomLevelMap.put(50f, 9);

        try {
            lookupService = new LookupService(GEOIP_DATABASE, LookupService.GEOIP_INDEX_CACHE);
        } catch (IOException ex) {
            logger.error("Failed to load GeoIP database: " + ex.getMessage(), ex);
        }
	}

    @RequestMapping(value = "/explore/your-area*", method = RequestMethod.GET)
	public String yourAreaView(
            @RequestParam(value="radius", required=false, defaultValue="5f") Float radius,
            @RequestParam(value="latitude", required=false, defaultValue="-35.27412f") Float latitude,
            @RequestParam(value="longitude", required=false, defaultValue="149.11288f") Float longitude,
            @RequestParam(value="address", required=false, defaultValue=DEFAULT_LOCATION) String address,
            @RequestParam(value="location", required=false, defaultValue="") String location,
            HttpServletRequest request,
            Model model) throws Exception {
        
        // Determine lat/long for client's IP address
        //LookupService lookup = new LookupService(geoIpDatabase, LookupService.GEOIP_INDEX_CACHE );
        String clientIP = request.getLocalAddr();
        logger.info("client IP address = "+request.getRemoteAddr());

        if (lookupService != null && location == null) {
            Location loc = lookupService.getLocation(clientIP);
            if (loc != null) {
                logger.info(clientIP + " has location: " + loc.postalCode + ", " + loc.city + ", " + loc.region + ". Coords: " + loc.latitude + ", " + loc.longitude);
                latitude = loc.latitude;
                longitude = loc.longitude;
                address = ""; // blank out address so Google Maps API can reverse geocode it
            }
        }
        
        model.addAttribute("latitude", latitude);
        model.addAttribute("longitude", longitude);
        model.addAttribute("location", location); // TDOD delete if not used in JSP
        //model.addAttribute("address", address); // TDOD delete if not used in JSP
        model.addAttribute("radius", radius);
        model.addAttribute("zoom", radiusToZoomLevelMap.get(radius));
        model.addAttribute("taxaGroups", TaxaGroup.values()); 

        // TODO: get from properties file or load via Spring
        model.addAttribute("speciesPageUrl", speciesPageUrl);

		return YOUR_AREA;
	}
        /**
	 * Occurrence search page uses SOLR JSON to display results
	 *
     * @param query
     * @param model
     * @return
     * @throws Exception
     */
	@RequestMapping(value = "/explore/download*", method = RequestMethod.GET)
	public void yourAreaDownload(
            @RequestParam(value="radius", required=false, defaultValue="10f") Float radius,
            @RequestParam(value="latitude", required=false, defaultValue="0f") Float latitude,
            @RequestParam(value="longitude", required=false, defaultValue="0f") Float longitude,
            @RequestParam(value="taxa", required=false, defaultValue="") String taxa, // comma separated list
            @RequestParam(value="rank", required=false, defaultValue="") String rank,
            HttpServletResponse response)
            throws Exception {

        logger.debug("Downloading the species in your area... ");
        response.setHeader("Cache-Control", "must-revalidate");
        response.setHeader("Pragma", "must-revalidate");
        response.setHeader("Content-Disposition", "attachment;filename=data");
        response.setContentType("application/vnd.ms-excel");
        String[] taxaArray = StringUtils.split(taxa, ",");
        ArrayList<String> taxaList = null;
        if (taxaArray != null) {
            taxaList = new ArrayList<String>(Arrays.asList(taxaArray));
        } else {
            taxaList = new ArrayList<String>();
        }
        ServletOutputStream out = response.getOutputStream();
        int count = searchDao.writeSpeciesCountByCircleToStream(latitude, longitude, radius, rank, taxaList, out);
        logger.debug("Exported " + count + " species records in the requested area");
        
	}

    /**
     * JSON web service that returns a list of species and record counts for a given location search
     * and a higher taxa with rank. 
     *
     * @param radius
     * @param latitude
     * @param longitude
     * @param taxa
     * @param rank
     * @param model
     * @throws Exception
     */
    @RequestMapping(value = "/explore/species.json", method = RequestMethod.GET)
	public void listSpeciesForHigherTaxa(
            @RequestParam(value="radius", required=false, defaultValue="10f") Float radius,
            @RequestParam(value="latitude", required=false, defaultValue="0f") Float latitude,
            @RequestParam(value="longitude", required=false, defaultValue="0f") Float longitude,
            @RequestParam(value="taxa", required=false, defaultValue="") String taxa, // comma separated list
            @RequestParam(value="rank", required=false, defaultValue="") String rank,
            @RequestParam(value="start", required=false, defaultValue="0") Integer startIndex,
			@RequestParam(value="pageSize", required=false, defaultValue ="50") Integer pageSize,
            @RequestParam(value="sort", required=false, defaultValue ="taxon_name") String sort,
            Model model) throws Exception {

        String[] taxaArray = StringUtils.split(taxa, "|");
        ArrayList<String> taxaList = null;
        if (taxaArray != null) {
            taxaList = new ArrayList<String>(Arrays.asList(taxaArray));
        } else {
            taxaList = new ArrayList<String>();
        }

        model.addAttribute("taxa", taxa);
        model.addAttribute("rank", rank);
        List<TaxaCountDTO> species = searchDao.findAllSpeciesByCircleAreaAndHigherTaxa(latitude, longitude, radius, rank, taxaList, null, startIndex, pageSize, sort, "asc");
        model.addAttribute("species", species);
        model.addAttribute("speciesCount", species.size());
    }

    /**
     * AJAX service to return number of species for given taxa group in a given location
     *
     * @param taxaGroupLabel
     * @param radius
     * @param latitude
     * @param longitude
     * @param model
     * @return speciesCount
     * @throws Exception
     */
    @RequestMapping(value = "/explore/taxaGroupCount", method = RequestMethod.GET)
	public void listSpeciesForHigherTaxa(
            @RequestParam(value="group", required=true, defaultValue ="") String taxaGroupLabel,
            @RequestParam(value="radius", required=true, defaultValue="10f") Float radius,
            @RequestParam(value="latitude", required=true, defaultValue="0f") Float latitude,
            @RequestParam(value="longitude", required=true, defaultValue="0f") Float longitude,
            HttpServletResponse response) throws Exception {

        TaxaGroup group = TaxaGroup.getForLabel(taxaGroupLabel);

        if (group != null) {
            List<TaxaCountDTO> taxaCounts = searchDao.findAllSpeciesByCircleAreaAndHigherTaxa(latitude,
                longitude, radius, group.getRank(), new ArrayList<String>(Arrays.asList(group.getTaxa())),
                null, 0, -1, "species", "asc");
            //Long speciesCount = calculateSpeciesCount(taxaCounts);
            Integer speciesCount = taxaCounts.size();
            logger.info("Species count for "+group.getLabel()+" = "+speciesCount);
            OutputStreamWriter os = new OutputStreamWriter(response.getOutputStream());
            response.setContentType("text/plain");
            os.write(speciesCount.toString());
            os.close();
        }
    }

    /**
     * Calculate the number of records for a given taxa group
     *
     * @param taxa
     * @return
     */
    protected Long calculateSpeciesCount(List<TaxaCountDTO> taxa) {
        // Get full count of records in area from facet breakdowns
        Long totalRecords = 0l;
        for (TaxaCountDTO taxon : taxa) {
            totalRecords = totalRecords + taxon.getCount();
        }
        return totalRecords;
    }

	/**
	 * @param searchDao the searchDao to set
	 */
	public void setSearchDao(SearchDAO searchDao) {
		this.searchDao = searchDao;
	}
	/**
	 * @param speciesPageUrl the speciesPageUrl to set
	 */
	public void setSpeciesPageUrl(String speciesPageUrl) {
		this.speciesPageUrl = speciesPageUrl;
	}
//	/**
//	 * @param addressCache the addressCache to set
//	 */
//	public void setAddressCache(HashMap<String, List<Float>> addressCache) {
//		this.addressCache = addressCache;
//	}
	
}
