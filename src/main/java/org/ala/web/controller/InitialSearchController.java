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

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.web.util.SearchType;
import org.ala.web.util.SearchUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.gbif.portal.web.controller.RestController;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for the full text searching across multiple entities.
 * This support the blanket search for the web application.
 * 
 * @author "Dave Martin (David.Martin@csiro.au)"
 */
public class InitialSearchController extends RestController {

	protected SessionFactory sessionFactory;
	protected String view = "fullSearchView";
	protected String searchStringRequestParam = "searchString";

	/**
	 * @see org.gbif.portal.web.controller.RestController#handleRequest(java.util.Map, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public ModelAndView handleRequest(Map<String, String> propertiesMap,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		String searchString = propertiesMap.get(searchStringRequestParam);
		if(logger.isDebugEnabled()){
			logger.debug("Searching with: "+searchString);
		}
		
		ModelAndView mav = new ModelAndView(view);
		mav.addObject(searchStringRequestParam, searchString);

		Session session = sessionFactory.openSession();
		FullTextSession fullTextSession = Search.getFullTextSession(session);

		//add results
//		query(fullTextSession, searchString, TaxonConcept.class, new String[]{"taxonName.canonical", "taxonName.author"}, "taxonConcepts", "taxonConceptsTotal", mav);
//    	query(fullTextSession, searchString, CommonName.class, "name", "commonNames", "commonNamesTotal", mav);
//    	query(fullTextSession, searchString, DataResource.class, new String[]{"name","description"}, "dataResources", "dataResourcesTotal", mav);
//    	query(fullTextSession, searchString, DataProvider.class, new String[]{"name","description"}, "dataProviders", "dataProvidersTotal", mav);
//    	query(fullTextSession, searchString, GeoRegion.class, new String[]{"name","acronym","geoRegionType.name"}, "geoRegions", "geoRegionsTotal", mav);
//    	query(fullTextSession, searchString, Locality.class, new String[]{"name","state","postcode"}, "localities", "localitiesTotal", mav);

        SearchUtils searchUtils = new SearchUtils();

        for (SearchType st : SearchType.values()) {
            searchUtils.query(fullTextSession, searchString, st.getBean(), st.getSearchFields(), st.getResultsParam(), st.getResultTotalParam(), mav);
        }
        
    	//close the session
		fullTextSession.close();
        return mav;
    }
    
	/**
	 * @param sessionFactory the sessionFactory to set
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
}
