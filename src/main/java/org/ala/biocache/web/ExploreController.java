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
import java.util.Map;

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
import org.ala.biocache.dto.*;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for the "explore your area" page
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 * @author "Natasha Carter <natasha.carter@csiro.au>"
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


    /**
     *
     * Returns a list of species groups and counts that will need to be displayed.
     *
     * TODO: MOVE all the IP lat long lookup to the the client webapp.  The purposes
     * of biocache-service is to provide a service layer over the biocache.
     *
     */
    @RequestMapping(value = "/explore/groups*", method = RequestMethod.GET)
	public @ResponseBody List<SpeciesGroupDTO> yourAreaView(
            SpatialSearchRequestParams requestParams,
            @RequestParam(value="address", required=false, defaultValue=DEFAULT_LOCATION) String address,
            @RequestParam(value="location", required=false, defaultValue="") String location,
            HttpServletRequest request,
            Model model) throws Exception {
        
        // Determine lat/long for client's IP address
        //LookupService lookup = new LookupService(geoIpDatabase, LookupService.GEOIP_INDEX_CACHE );
//        String clientIP = request.getLocalAddr();
//        logger.info("client IP address = "+request.getRemoteAddr());
//
//        if (lookupService != null && location == null) {
//            Location loc = lookupService.getLocation(clientIP);
//            if (loc != null) {
//                logger.info(clientIP + " has location: " + loc.postalCode + ", " + loc.city + ", " + loc.region + ". Coords: " + loc.latitude + ", " + loc.longitude);
//                latitude = loc.latitude;
//                longitude = loc.longitude;
//                address = ""; // blank out address so Google Maps API can reverse geocode it
//            }
//        }

        //set the query to all
        requestParams.setQ("*:*");
        //only want the species groups in the facet
        requestParams.setFacets(new String[]{"species_group"});
        //we want all the facets
        requestParams.setFlimit(-1);
        requestParams.setPageSize(0);//don't actually want any of the results returned
        SearchResultDTO results =searchDao.findByFulltextSpatialQuery(requestParams);
        java.util.Collection<FacetResultDTO> facets =results.getFacetResults();
        java.util.List<FieldResultDTO> fieldResults = facets.size()>0?facets.iterator().next().getFieldResult():java.util.Collections.EMPTY_LIST;
        java.util.Collections.sort(fieldResults);
        //now we want to grab all the facets to get the counts associated with the species groups
        List<au.org.ala.biocache.SpeciesGroup> sgs =au.org.ala.biocache.Store.retrieveSpeciesGroups();
        List<SpeciesGroupDTO> speciesGroups = new java.util.ArrayList<SpeciesGroupDTO>();
        SpeciesGroupDTO all = new SpeciesGroupDTO();
        all.setName("ALL_SPECIES");
        all.setLevel(0);
        all.setCount(results.getTotalRecords());
        all.setSpeciesCount(getYourAreaCount(requestParams, "ALL_SPECIES")[1]);
        speciesGroups.add(all);

        String oldName = null;
        String kingdom =null;
        //set the counts an indent levels for all the species groups
        for(au.org.ala.biocache.SpeciesGroup sg : sgs){
            logger.debug("name: " + sg.name() + " parent: " +sg.parent());
            int level =3;
            SpeciesGroupDTO sdto = new SpeciesGroupDTO();
            sdto.setName(sg.name());

            if(oldName!= null && sg.parent()!= null && sg.parent().equals(kingdom))
                level = 2;
            
            oldName = sg.name();
            if(sg.parent() == null){
                level = 1;
                kingdom = sg.name();
            }
            sdto.setLevel(level);
            Integer[] counts = getYourAreaCount(requestParams, sg.name());
            sdto.setCount(counts[0]);
            sdto.setSpeciesCount(counts[1]);
            speciesGroups.add(sdto);
        }
        return speciesGroups;


        
        //model.addAttribute("results",speciesGroups);

//        model.addAttribute("latitude", latitude);
//        model.addAttribute("longitude", longitude);
//        model.addAttribute("location", location); // TDOD delete if not used in JSP
//        //model.addAttribute("address", address); // TDOD delete if not used in JSP
//        model.addAttribute("radius", radius);
//        model.addAttribute("zoom", radiusToZoomLevelMap.get(radius));
//        model.addAttribute("taxaGroups", TaxaGroup.values());
//
//        // TODO: get from properties file or load via Spring
//        model.addAttribute("speciesPageUrl", speciesPageUrl);

		//return YOUR_AREA;
	}
    /**
     * Returns the number of records and distinct species in a particular species group
     * 
     * @param requestParams
     * @param group
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/explore/counts/group/{group}*", method = RequestMethod.GET)
    public @ResponseBody Integer[] getYourAreaCount(SpatialSearchRequestParams requestParams,
            @PathVariable(value="group") String group) throws Exception{
        String query = group.equals("ALL_SPECIES")? "*:*" : "species_group:" + group;
        requestParams.setQ(query);
        requestParams.setPageSize(0);
        requestParams.setFacets(new String[]{"taxon_name"});
        requestParams.setFlimit(-1);
        SearchResultDTO results = searchDao.findByFulltextSpatialQuery(requestParams);
        Integer speciesCount =0;
        if(results.getFacetResults().size() >0){
            speciesCount = results.getFacetResults().iterator().next().getFieldResult().size();
        }
        
        return new Integer[]{(int)results.getTotalRecords() ,speciesCount};
    }
    /**
     * Returns the number of species in all the groups.
     * @param requestParams
     * @return
     * @throws Exception
     */
//    @RequestMapping(value="/explore/counts*", method = RequestMethod.GET)
//    public @ResponseBody Map<String,String> getYourAreaCounts(SpatialSearchRequestParams requestParams) throws Exception{
//        Map<String,String> values = new HashMap<String, String>();
//        List<au.org.ala.biocache.SpeciesGroup> sgs =au.org.ala.biocache.Store.retrieveSpeciesGroups();
//        values.put("ALL_SPECIES", getYourAreaCount(requestParams, "ALL_SPECIES"));
//        for(au.org.ala.biocache.SpeciesGroup sg : sgs){
//            values.put(sg.name(), getYourAreaCount(requestParams,sg.name()));
//        }
//        return values;
//    }
    
    
    /**
     * The first value is the count of the value
     * The second value is the number of distinct species
     */
//    private long[] getCounts(String field){
//        
//    }
    

        /**
	 * Occurrence search page uses SOLR JSON to display results
	 *
     * @param query
     * @param model
     * @return
     * @throws Exception
     */
	@RequestMapping(value = "/explore/group/{group}/download*", method = RequestMethod.GET)
	public void yourAreaDownload(
            SpatialSearchRequestParams requestParams,
            @PathVariable(value="group") String group,
            HttpServletResponse response)
            throws Exception {

        logger.debug("Downloading the species in your area... ");
        response.setHeader("Cache-Control", "must-revalidate");
        response.setHeader("Pragma", "must-revalidate");
        response.setHeader("Content-Disposition", "attachment;filename=data");
        response.setContentType("application/vnd.ms-excel");
       
        ServletOutputStream out = response.getOutputStream();
        int count = searchDao.writeSpeciesCountByCircleToStream(requestParams,group, out);
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
    @RequestMapping(value = "/explore/group/{group}*", method = RequestMethod.GET)
	public @ResponseBody List<TaxaCountDTO> listSpeciesForHigherTaxa(
            SpatialSearchRequestParams requestParams,
            @PathVariable(value="group") String group,
                       
            Model model) throws Exception {

//        String[] taxaArray = StringUtils.split(taxa, "|");
//        ArrayList<String> taxaList = null;
//        if (taxaArray != null) {
//            taxaList = new ArrayList<String>(Arrays.asList(taxaArray));
//        } else {
//            taxaList = new ArrayList<String>();
//        }
//
//        model.addAttribute("taxa", taxa);
//        model.addAttribute("rank", rank);
       

        return searchDao.findAllSpeciesByCircleAreaAndHigherTaxa(requestParams, group);
        //model.addAttribute("species", species);
        //model.addAttribute("speciesCount", species.size());
    }

    /**
     * AJAX service to return number of species for given taxa group in a given location
     *
     * Tested with: /explore/taxaGroupCount?latitude=-31.2&longitude=138.4&group=ANIMALS
     *
     * @param taxaGroupLabel
     * @param radius
     * @param latitude
     * @param longitude
     * @param model
     * @return speciesCount
     * @throws Exception
     */
//    @RequestMapping(value = "/explore/taxaGroupCount", method = RequestMethod.GET)
//	public void listSpeciesForHigherTaxa(
//            @RequestParam(value="group", required=true, defaultValue ="") String taxaGroupLabel,
//            @RequestParam(value="radius", required=true, defaultValue="10f") Float radius,
//            @RequestParam(value="latitude", required=true, defaultValue="0f") Float latitude,
//            @RequestParam(value="longitude", required=true, defaultValue="0f") Float longitude,
//            HttpServletResponse response) throws Exception {
//
//        TaxaGroup group = TaxaGroup.getForLabel(taxaGroupLabel);
//
//        if (group != null) {
//            List<TaxaCountDTO> taxaCounts = searchDao.findAllSpeciesByCircleAreaAndHigherTaxa(latitude,
//                longitude, radius, group.getRank(), new ArrayList<String>(Arrays.asList(group.getTaxa())),
//                null, 0, -1, "species", "asc");
//            //Long speciesCount = calculateSpeciesCount(taxaCounts);
//            Integer speciesCount = taxaCounts.size();
//            logger.info("Species count for "+group.getLabel()+" = "+speciesCount);
//            OutputStreamWriter os = new OutputStreamWriter(response.getOutputStream());
//            response.setContentType("text/plain");
//            os.write(speciesCount.toString());
//            os.close();
//        }
//    }

    /**
     * Calculate the number of records for a given taxa group
     *
     * @param taxa
     * @return
     */
//    protected Long calculateSpeciesCount(List<TaxaCountDTO> taxa) {
//        // Get full count of records in area from facet breakdowns
//        Long totalRecords = 0l;
//        for (TaxaCountDTO taxon : taxa) {
//            totalRecords = totalRecords + taxon.getCount();
//        }
//        return totalRecords;
//    }

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
