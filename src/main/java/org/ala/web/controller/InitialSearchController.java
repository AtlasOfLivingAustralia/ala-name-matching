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

import org.ala.model.CommonName;
import org.ala.model.DataProvider;
import org.ala.model.DataResource;
import org.ala.model.GeoRegion;
import org.ala.model.Locality;
import org.ala.model.TaxonConcept;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
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
		query(fullTextSession, searchString, TaxonConcept.class, new String[]{"taxonName.canonical", "taxonName.author"}, "taxonConcepts", "taxonConceptsTotal", mav);
    	query(fullTextSession, searchString, CommonName.class, "name", "commonNames", "commonNamesTotal", mav);
    	query(fullTextSession, searchString, DataResource.class, new String[]{"name","description"}, "dataResources", "dataResourcesTotal", mav);
    	query(fullTextSession, searchString, DataProvider.class, new String[]{"name","description"}, "dataProviders", "dataProvidersTotal", mav);
    	query(fullTextSession, searchString, GeoRegion.class, new String[]{"name","acronym"}, "geoRegions", "geoRegionsTotal", mav);
    	query(fullTextSession, searchString, Locality.class, new String[]{"name","state","postcode"}, "localities", "localitiesTotal", mav);
    	
    	//close the session
		fullTextSession.close();
        return mav;
    }
	
	private void query(FullTextSession fullTextSession, String searchString, Class clazz, String fieldToSearch, 
			String resultsParam, String resultTotalParam, ModelAndView mav) throws ParseException {
		FullTextQuery fullTextQuery = buildQuery(fullTextSession, fieldToSearch, clazz, searchString);
		doQuery(resultsParam, resultTotalParam, mav, fullTextQuery);
	}

	private void query(FullTextSession fullTextSession, String searchString, Class clazz, String[] fieldsToSearch, 
			String resultsParam, String resultTotalParam, ModelAndView mav) throws ParseException {
		FullTextQuery fullTextQuery = buildQuery(fullTextSession, fieldsToSearch, clazz, searchString);
		doQuery(resultsParam, resultTotalParam, mav, fullTextQuery);
	}
	
	private void doQuery(String resultsParam, String resultTotalParam,
			ModelAndView mav, FullTextQuery fullTextQuery) {
		fullTextQuery.setMaxResults(10);
		fullTextQuery.setFirstResult(0);
		List results = fullTextQuery.list();
		int resultsSize = fullTextQuery.getResultSize();
		mav.addObject(resultsParam, results);
		mav.addObject(resultTotalParam, resultsSize);
	}
	
    /**
     * Build a query, using the correct analyzer.
     * 
     * @param clazz
     * @param queryString
     * @return
     * @throws ParseException
     */
    public FullTextQuery buildQuery(FullTextSession fullTextSession,  String[] fieldsToSearch, Class modelClass, String queryString) throws ParseException {
    	
    	fullTextSession.getSearchFactory().getAnalyzer(modelClass);
		Analyzer analyzer = fullTextSession.getSearchFactory().getAnalyzer(modelClass);
		MultiFieldQueryParser parser = new MultiFieldQueryParser(fieldsToSearch,analyzer);
		org.apache.lucene.search.Query luceneQuery = parser.parse(queryString);
		org.hibernate.search.FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(luceneQuery, modelClass);	
    	return fullTextQuery;
    }
    
    /**
     * Build a query, using the correct analyzer.
     * 
     * @param clazz
     * @param queryString
     * @return
     * @throws ParseException
     */
    public FullTextQuery buildQuery(FullTextSession fullTextSession, String fieldToSearch, Class modelClass, String queryString) throws ParseException {
    	fullTextSession.getSearchFactory().getAnalyzer(modelClass);
		org.apache.lucene.queryParser.QueryParser parser = new QueryParser(
			fieldToSearch, 
		    fullTextSession.getSearchFactory().getAnalyzer(modelClass)
		);
		org.apache.lucene.search.Query luceneQuery = parser.parse(queryString);
		org.hibernate.search.FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(luceneQuery, modelClass);	
    	return fullTextQuery;
    }

	/**
	 * @param sessionFactory the sessionFactory to set
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
}