/* *************************************************************************
 * Copyright (C) 2005 Global Biodiversity Information Facility Secretariat.  
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

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.dao.OccurrenceFacetDAO;
import org.ala.util.IndexingIssue;
import org.ala.util.IndexingIssueTypes;
import org.ala.web.util.WebUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.gbif.portal.dao.tag.SimpleTagDAO;
import org.gbif.portal.dao.tag.TagDAO;
import org.gbif.portal.dto.CountDTO;
import org.gbif.portal.dto.SearchResultsDTO;
import org.gbif.portal.dto.resources.DataProviderDTO;
import org.gbif.portal.dto.resources.DataResourceAgentDTO;
import org.gbif.portal.dto.resources.DataResourceDTO;
import org.gbif.portal.dto.resources.ResourceAccessPointDTO;
import org.gbif.portal.dto.resources.ResourceNetworkDTO;
import org.gbif.portal.dto.tag.BiRelationTagDTO;
import org.gbif.portal.dto.tag.GeographicalCoverageTag;
import org.gbif.portal.dto.tag.NumberTag;
import org.gbif.portal.dto.tag.StringTag;
import org.gbif.portal.dto.tag.TemporalCoverageTag;
import org.gbif.portal.dto.util.EntityType;
import org.gbif.portal.dto.util.SearchConstraints;
import org.gbif.portal.service.DataResourceManager;
import org.gbif.portal.service.GeospatialManager;
import org.gbif.portal.service.TaxonomyManager;
import org.gbif.portal.util.log.GbifLogMessage;
import org.gbif.portal.util.log.LogEvent;
import org.gbif.portal.web.content.map.MapContentProvider;
import org.gbif.portal.web.controller.RestController;
import org.gbif.portal.web.util.UserUtils;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

/**
 * A Controller that resolves and sets up a detailed view of a Data Resource.
 *
 * @author Dave Martin
 */
public class DataResourceController extends RestController {
	
	/** The Taxonomy Manager */
	protected TaxonomyManager taxonomyManager;
	/** The Data Resource Manager */
	protected DataResourceManager dataResourceManager;
	/** The Geospatial Manager */
	protected GeospatialManager geospatialManager;
	/** The MapContentProvider */
	protected MapContentProvider mapContentProvider;
    /** The OccurrenceFacetDAO */
    protected OccurrenceFacetDAO occurrenceFacetDAO;
	/** The MappingFactory */
	//protected GbifMappingFactory gbifMappingFactory;	
	/** The data resource request key */ 
	protected String resourceRequestKey = "resource";
	/** The data provider request key */ 
	protected String providerRequestKey = "provider";
	/** The data provider model key */ 
	protected String dataResourceModelKey = "dataResource";	
	/** The data provider model key */ 
	protected String dataProviderModelKey = "dataProvider";
	/** The nub data provider model key */ 
	protected String nubDataProviderModelKey = "nubDataProvider";	
	/** The data resources model key */ 
	protected String dataResourcesModelKey = "dataResources";	
	/** The root concepts model key */ 
	protected String rootConceptsModelKey = "rootConcepts";		
	/** The citation text model key */ 
	protected String citationTextModelKey = "citationText";		
	
	protected String dataResourceAgentsModelKey = "agents";
	
	protected String resourceNetworksModelKey = "networks";

	protected String resourceAccessPointsKey = "resourceAccessPoints";
	
	/** the basis of record value for a taxonomic data resource */
	protected String taxonomyBasisOfRecord = "taxonomy";
	
	/** User Utils */
	protected UserUtils userUtils;
	
	protected SimpleTagDAO dataResourceTagDAO;
	
	protected Map<String, View> schema2View;
	
	protected MessageSource messageSource;
	
	/**
	 * @see org.gbif.portal.web.controller.RestController#handleRequest(java.util.Map, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ModelAndView handleRequest(Map<String, String> propertiesMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String dataProviderRequestParam = propertiesMap.get(providerRequestKey);
		String dataProviderKey = null;
		logger.debug("Inside DataResourceController - handleRequest");

		if (StringUtils.isNotEmpty(dataProviderRequestParam)){		
			if(!dataResourceManager.isValidDataProviderKey(dataProviderKey)){
				SearchResultsDTO searchResultsDTO = dataResourceManager.findDataProviders(dataProviderRequestParam, false, null, null, new SearchConstraints(0,2));
				if(!searchResultsDTO.isEmpty() && searchResultsDTO.getResults().size()==1){
					DataProviderDTO dataProvider = (DataProviderDTO) searchResultsDTO.getResults().get(0);
					dataProviderKey = dataProvider.getKey();
				} 
			} else {
				dataProviderKey = dataProviderRequestParam;
			}
		}
		
		String dataResourceKey = propertiesMap.get(resourceRequestKey);	
		if (StringUtils.isNotEmpty(dataResourceKey)){
			if(dataResourceManager.isValidDataResourceKey(dataResourceKey)){
				return view(dataResourceKey, propertiesMap, request, response);
			} else {
				String resourceName = decodeParameter(dataResourceKey);
				SearchConstraints searchConstraints = new SearchConstraints();
				SearchResultsDTO searchResultsDTO = dataResourceManager.findDataResources(resourceName, false, dataProviderKey, null, null, searchConstraints);
				List<DataResourceDTO> dataResources = searchResultsDTO.getResults();
				if (!dataResources.isEmpty() && dataResources.size() == 1){
					DataResourceDTO dataResource = dataResources.get(0);
					return view(dataResource.getKey(), propertiesMap, request, response);	
				}
			}
		}
		return redirectToDefaultView();
	}
	
	/**
	 * Generate EML profile for this Datasource.
	 * 
	 * @param dataResourceKey
	 * @param propertiesMap
	 * @param request
	 * @param response
	 * @return
	 */
	public ModelAndView viewProfile(String dataResourceKey, String schema, Map<String, String> propertiesMap, 
			HttpServletRequest request, HttpServletResponse response) throws Exception{
		
		if(schema2View.get(schema)==null){
			return null;
		}
		
		ModelAndView mav = new ModelAndView(schema2View.get(schema));
		mav.addObject("messageSource", messageSource);
		
		DataResourceDTO dataResource = dataResourceManager.getDataResourceFor(dataResourceKey);
		if(dataResource==null)
			return null;
		
		Long id = Long.parseLong(dataResourceKey);
		
		List<CountDTO> countryCounts = geospatialManager.getCountriesForDataResource(dataResourceKey, false);
		mav.addObject("countries", countryCounts);
		
		//add regions
		Set<String> regions = new HashSet<String>();
		for (CountDTO count: countryCounts){
			regions.add(count.getProperties().get(0));
		}
		mav.addObject("regions", regions);
		
		mav.addObject("dataResource", dataResource);
		mav.addObject("pageTitle", dataResource.getName());
		
		//add agents
		List<DataResourceAgentDTO> agents = dataResourceManager.getAgentsForDataResource(dataResourceKey);
		mav.addObject("agents", agents);

		//add raps
		List<ResourceAccessPointDTO> raps = dataResourceManager.getResourceAccessPointsForDataResource(dataResourceKey);
		mav.addObject("raps", raps);
		
		//retrieve bounding box
		logger.debug("Adding geographic coverage tags");
		List<GeographicalCoverageTag> gcts = dataResourceTagDAO.retrieveGeographicalCoverageTagsForEntity(TagDAO.DATA_RESOURCE_OCCURRENCES_BOUNDING_BOX, id);
		if(gcts!=null && !gcts.isEmpty()){
			mav.addObject("geographicCoverageTags", gcts);
		}
		
		//retrieve all associated countries		
		//FIXME replace with resource_country query - same thing
		logger.debug("Adding country tags");
		List<BiRelationTagDTO> countryTags = dataResourceTagDAO.retrieveBiRelationTagsForEntity(TagDAO.DATA_RESOURCE_OCCURRENCES_COUNTRY, id);
		if(countryTags!=null && !countryTags.isEmpty()){
			mav.addObject("countryTags", countryTags);
		}
		
		//retrieve all associated temporal coverage	
		logger.debug("Adding temporal coverage tags");
		List<TemporalCoverageTag> tempTags = dataResourceTagDAO.retrieveTemporalCoverageTagsForEntity(TagDAO.DATA_RESOURCE_OCCURRENCES_DATE_RANGE, id);
		if(tempTags!=null && !tempTags.isEmpty()){
			mav.addObject("temporalCoverageTags", tempTags);			
		}
		
		//retrieve all associated taxonomic scope	
		logger.debug("Adding taxonomic scope tags");
		List<BiRelationTagDTO> taxonomicScopeTags = dataResourceTagDAO.retrieveBiRelationTagsForEntity(TagDAO.DATA_RESOURCE_TAXONOMIC_SCOPE, id);
		if(taxonomicScopeTags!=null && !taxonomicScopeTags.isEmpty()){
			mav.addObject("taxonomicScopeTags", taxonomicScopeTags);
		}	
		
		//retrieve all associated kingdom scope		
		logger.debug("Adding kingdom coverage tags");
		List<BiRelationTagDTO> kingdomCoverageTags = dataResourceTagDAO.retrieveBiRelationTagsForEntity(TagDAO.DATA_RESOURCE_ASSOCIATED_KINGDOM, id);
		if(kingdomCoverageTags!=null && !kingdomCoverageTags.isEmpty()){
			mav.addObject("kingdomCoverageTags", kingdomCoverageTags);
		}		
		
		//retrieve all associated families
		logger.debug("Adding family tags");
		List<BiRelationTagDTO> familyTags = dataResourceTagDAO.retrieveBiRelationTagsForEntity(TagDAO.DATA_RESOURCE_OCCURRENCES_FAMILY, id);
		if(familyTags!=null && !familyTags.isEmpty()){
			mav.addObject("familyTags", familyTags);
		}			

		//retrieve all associated genera	
//		logger.debug("Adding genera tags");
//		List<BiRelationTagDTO> generaTags = dataResourceTagDAO.retrieveBiRelationTagsForEntity(TagDAO.DATA_RESOURCE_OCCURRENCES_GENUS, id);
//		if(generaTags!=null && !generaTags.isEmpty()){
//			mav.addObject("generaTags", generaTags);
//		}			
//		
//		//retrieve all associated species		
//		logger.debug("Adding species tags");
//		List<BiRelationTagDTO> speciesTags = dataResourceTagDAO.retrieveBiRelationTagsForEntity(TagDAO.DATA_RESOURCE_OCCURRENCES_SPECIES, id);
//		if(speciesTags!=null && !speciesTags.isEmpty()){
//			mav.addObject("speciesTags", speciesTags);
//		}				
		
		//retrieve all associated common names
		logger.debug("Adding common name tags");
		List<BiRelationTagDTO> commonNamesTags = dataResourceTagDAO.retrieveBiRelationTagsForEntity(TagDAO.DATA_RESOURCE_COMMON_NAMES, id);
		if(commonNamesTags!=null && !commonNamesTags.isEmpty()){
			mav.addObject("commonNamesTags", commonNamesTags);
		}	
		
		//retrieve contains type status tags
		logger.debug("Adding type specimen tags");
		List<NumberTag> containsTypeSpecimenTags = dataResourceTagDAO.retrieveNumberTagsForEntity(TagDAO.DATA_RESOURCE_CONTAINS_TYPE_SPECIMENS, id);
		if(containsTypeSpecimenTags!=null && !containsTypeSpecimenTags.isEmpty()){
			mav.addObject("containsTypeSpecimenTags", containsTypeSpecimenTags);
		}
		
		//retrieve all associated collectors
		logger.debug("Adding collector tags");
		List<StringTag> collectorTags = dataResourceTagDAO.retrieveStringTagsForEntity(TagDAO.DATA_RESOURCE_COLLECTOR, id);
		if(collectorTags!=null && !collectorTags.isEmpty()){
			mav.addObject("collectorTags", collectorTags);
		}			

		//retrieve all associated keywords
		logger.debug("Adding keyword tags");
		List<StringTag> keywordTags = dataResourceTagDAO.retrieveStringTagsForEntity(TagDAO.DATA_RESOURCE_KEYWORDS, id);
		if(keywordTags!=null && !keywordTags.isEmpty()){
			mav.addObject("keywordTags", keywordTags);
		}			
		
		//retrieve all associated months
		logger.debug("Adding month tags");
		List<NumberTag> monthTags = dataResourceTagDAO.retrieveNumberTagsForEntity(TagDAO.DATA_RESOURCE_OCCURRENCES_MONTH, id);
		if(monthTags!=null && !monthTags.isEmpty()){
			mav.addObject("monthTags", monthTags);
		}					
		
		//retrieve all associated months
		logger.debug("Adding polygon tags");
		List<StringTag> polygonTags = dataResourceTagDAO.retrieveStringTagsForEntity(TagDAO.DATA_RESOURCE_OCCURRENCES_WKT_POLYGON, id);
		if(polygonTags!=null && !polygonTags.isEmpty()){
			mav.addObject("polygonTags", polygonTags);
		}				
		
		return mav;
	}
	
	/**
	 * View the dataset with the supplied id.
	 * @param id
	 * @param request
	 * @param response
	 * @return a ModelAndView for this dataset
	 * @throws Exception
	 */
	public ModelAndView view(String dataResourceKey, Map<String, String> propertiesMap, HttpServletRequest request, HttpServletResponse response) throws Exception {

		String schema = ServletRequestUtils.getStringParameter(request, "schema");
		if(StringUtils.isNotBlank(schema)){
			return viewProfile(dataResourceKey, schema, propertiesMap, request, response);
		}
		
		DataResourceDTO dataResource = dataResourceManager.getDataResourceFor(dataResourceKey);
		if(dataResource==null)
			return redirectToDefaultView();
		ModelAndView mav = resolveAndCreateView(propertiesMap, request, false);
		DataProviderDTO dataProvider = dataResourceManager.getDataProviderFor(dataResource.getDataProviderKey());
		mav.addObject(dataResourceModelKey, dataResource);
		mav.addObject(dataProviderModelKey, dataProvider);
		mav.addObject(nubDataProviderModelKey, dataResourceManager.getNubDataProvider());
		/* Removed for ALA portal v1 by NdR 9/2/09
		String url = request.getRequestURL().toString();
		String servletPath = request.getServletPath();
		String urlBase = url.substring(0, url.indexOf(servletPath));
		mav.addObject(citationTextModelKey, gbifMappingFactory.getCitationText(dataResource, urlBase));
		*/
		mav.addObject(citationTextModelKey, "To do -  add citation text");
		mav.addObject(resourceAccessPointsKey, dataResourceManager.getResourceAccessPointsForDataResource(dataResourceKey));
//		Scope has been removed for now		
//		List<BriefTaxonConceptDTO> rootConcepts = taxonomyManager.getRootTaxonConceptsForTaxonomy(null, dataResource.getKey());
//		if(logger.isDebugEnabled() && rootConcepts!=null)
//			logger.debug("rootConcepts: "+rootConcepts.size());
//		if(rootConcepts!=null)
//			mav.addObject("rootConcepts", rootConcepts);
		
		if(!taxonomyBasisOfRecord.equals(dataResource.getBasisOfRecord())){
			mapContentProvider.addMapContentForEntity(request, EntityType.TYPE_DATA_RESOURCE, dataResource.getKey());
		}
		
		List<DataResourceAgentDTO> dataResourceAgents = dataResourceManager.getAgentsForDataResource(dataResource.getKey());
		mav.addObject(dataResourceAgentsModelKey, dataResourceAgents);
		
		List<ResourceNetworkDTO> resourceNetworks = dataResourceManager.getResourceNetworksForDataResource(dataResource.getKey());
		mav.addObject(resourceNetworksModelKey, resourceNetworks);
		
		// Add charts showing resource statistics/breakdown using Solr facetted search
		// TODO Add a check for the Solr server running?
		Map<String, String> chartData = occurrenceFacetDAO.getChartFacetsForResource(dataResourceKey);
		if(chartData!=null){
			mav.addObject("chartData", chartData);
		}
		
		mav.addObject("hostUrl", WebUtils.getHostUrl(request));
		logUsage(request, dataResource);
		return mav;
	}
	
	/**
	 * Log the usage.
	 * 
	 * @param dataProvider
	 */
	protected void logUsage(HttpServletRequest request, DataResourceDTO dataResource) {
			GbifLogMessage gbifMessage = new GbifLogMessage();
			gbifMessage.setEvent(LogEvent.USAGE_DATASET_METADATA_VIEW);
			gbifMessage.setDataResourceId(parseKey(dataResource.getKey()));
			gbifMessage.setDataProviderId(parseKey(dataResource.getDataProviderKey()));
			gbifMessage.setTimestamp(new Date());
			gbifMessage.setRestricted(false);
			gbifMessage.setMessage("Dataset metadata viewed");
			userUtils.logUsage(logger, gbifMessage, request);
	}	
	
	/**
	 * Parses the supplied key. Returns null if supplied string invalid
	 * @param key
	 * @return a concept key. Returns null if supplied string invalid key
	 */
	protected static Long parseKey(String key){
		Long parsedKey = null;
		try {
			parsedKey = Long.parseLong(key);
		} catch (NumberFormatException e){
			//expected behaviour for invalid keys
		}
		return parsedKey;
	}

	
	/**
	 * @param dataResourceManager the dataResourceManager to set
	 */
	public void setDataResourceManager(DataResourceManager dataResourceManager) {
		this.dataResourceManager = dataResourceManager;
	}

	/**
	 * @param taxonomyManager the taxonomyManager to set
	 */
	public void setTaxonomyManager(TaxonomyManager taxonomyManager) {
		this.taxonomyManager = taxonomyManager;
	}		

	/**
	 * @param mapContentProvider the mapContentProvider to set
	 */
	public void setMapContentProvider(MapContentProvider mapContentProvider) {
		this.mapContentProvider = mapContentProvider;
	}

	/**
	 * @param resourceRequestKey the resourceRequestKey to set
	 */
	public void setResourceRequestKey(String resourceRequestKey) {
		this.resourceRequestKey = resourceRequestKey;
	}

	/**
	 * @param gbifMappingFactory the gbifMappingFactory to set
	 */
	/* public void setGbifMappingFactory(GbifMappingFactory gbifMappingFactory) {
		this.gbifMappingFactory = gbifMappingFactory;
	}*/

	/**
	 * @param citationTextModelKey the citationTextModelKey to set
	 */
	public void setCitationTextModelKey(String citationTextModelKey) {
		this.citationTextModelKey = citationTextModelKey;
	}

	/**
	 * @param dataProviderModelKey the dataProviderModelKey to set
	 */
	public void setDataProviderModelKey(String dataProviderModelKey) {
		this.dataProviderModelKey = dataProviderModelKey;
	}

	/**
	 * @param dataResourceAgentsModelKey the dataResourceAgentsModelKey to set
	 */
	public void setDataResourceAgentsModelKey(String dataResourceAgentsModelKey) {
		this.dataResourceAgentsModelKey = dataResourceAgentsModelKey;
	}

	/**
	 * @param dataResourceModelKey the dataResourceModelKey to set
	 */
	public void setDataResourceModelKey(String dataResourceModelKey) {
		this.dataResourceModelKey = dataResourceModelKey;
	}

	/**
	 * @param dataResourcesModelKey the dataResourcesModelKey to set
	 */
	public void setDataResourcesModelKey(String dataResourcesModelKey) {
		this.dataResourcesModelKey = dataResourcesModelKey;
	}

	/**
	 * @param nubDataProviderModelKey the nubDataProviderModelKey to set
	 */
	public void setNubDataProviderModelKey(String nubDataProviderModelKey) {
		this.nubDataProviderModelKey = nubDataProviderModelKey;
	}

	/**
	 * @param providerRequestKey the providerRequestKey to set
	 */
	public void setProviderRequestKey(String providerRequestKey) {
		this.providerRequestKey = providerRequestKey;
	}

	/**
	 * @param resourceAccessPointsKey the resourceAccessPointsKey to set
	 */
	public void setResourceAccessPointsKey(String resourceAccessPointsKey) {
		this.resourceAccessPointsKey = resourceAccessPointsKey;
	}

	/**
	 * @param resourceNetworksModelKey the resourceNetworksModelKey to set
	 */
	public void setResourceNetworksModelKey(String resourceNetworksModelKey) {
		this.resourceNetworksModelKey = resourceNetworksModelKey;
	}

	/**
	 * @param rootConceptsModelKey the rootConceptsModelKey to set
	 */
	public void setRootConceptsModelKey(String rootConceptsModelKey) {
		this.rootConceptsModelKey = rootConceptsModelKey;
	}

	/**
	 * @param taxonomyBasisOfRecord the taxonomyBasisOfRecord to set
	 */
	public void setTaxonomyBasisOfRecord(String taxonomyBasisOfRecord) {
		this.taxonomyBasisOfRecord = taxonomyBasisOfRecord;
	}

	/**
	 * @param userUtils the userUtils to set
	 */
	public void setUserUtils(UserUtils userUtils) {
		this.userUtils = userUtils;
	}

	/**
    * @param schema2View the schema2View to set
    */
    public void setSchema2View(Map<String, View> schema2View) {
        this.schema2View = schema2View;
    }

    /**
    * @param simpleTagDAO the simpleTagDAO to set
    */
    public void setDataResourceTagDAO(SimpleTagDAO dataResourceTagDAO) {
        this.dataResourceTagDAO = dataResourceTagDAO;
    }

    /**
    * @param geospatialManager the geospatialManager to set
    */
    public void setGeospatialManager(GeospatialManager geospatialManager) {
        this.geospatialManager = geospatialManager;
    }

    /**
    * @param messageSource the messageSource to set
    */
    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
    * @param occurrenceFacetDAO
    */
    public void setOccurrenceFacetDAO(OccurrenceFacetDAO occurrenceFacetDAO) {
        this.occurrenceFacetDAO = occurrenceFacetDAO;
    }


}