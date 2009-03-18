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

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import org.ala.web.util.SearchType;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.springframework.web.bind.ServletRequestUtils;

/**
 * Paging Hibernte (Lucene) Search
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class PagingSearchController extends RestController {

    protected SessionFactory sessionFactory;
    protected String htmlView = "commonNamesSearchView";
    protected String jsonView = "commonNamesJsonView";
    //protected String searchType;
    SearchType searchType;
    protected Map<String, String> sortFieldMap;

    //protected Map<String, Class> beanNameMap;
    /**
     * @see org.ala.web.controller.RestController#handleRequest(java.util.Map, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public ModelAndView handleRequest(Map<String, String> properties, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String searchString = properties.get("searchString");
        String viewString   = properties.get("view");  // optional param for json view
        Integer results     = ServletRequestUtils.getIntParameter(request, "results", 10);
        Integer startIndex  = ServletRequestUtils.getIntParameter(request, "startIndex", 0);
        String sort         = ServletRequestUtils.getStringParameter(request, "sort", "score");
        String dir          = ServletRequestUtils.getStringParameter(request, "dir", "asc");

        Boolean reverse = ("desc".equals(dir)) ? true : false; // sets the sort order
        if (!"score".equals(sort) && sortFieldMap.containsKey(sort))
            sort = sortFieldMap.get(sort); // map the YUI table column name to bean field names

        String view = ("json".equals(viewString)) ? jsonView : htmlView;
        ModelAndView mav = new ModelAndView(view);
        mav.addObject("searchString", searchString);
        mav.addObject("pageSize", 10); // sets the number of page "boxes" to display
        mav.addObject("recordsReturned", results);
        mav.addObject("startIndex", startIndex);
        mav.addObject("sort", sort);
        mav.addObject("dir", dir);

        Session session = sessionFactory.openSession();
        FullTextSession fullTextSession = Search.getFullTextSession(session);
        //FullTextQuery fullTextQuery = buildQuery(fullTextSession, "name", CommonName.class, searchString);
        FullTextQuery fullTextQuery = buildQuery(fullTextSession, searchType.getSearchFields(), searchType.getBean(), searchString);
        fullTextQuery.setMaxResults(results);
        fullTextQuery.setFirstResult(startIndex);
        
        if (!"score".equals(sort)) {
            Sort sortField = new Sort(new SortField(sort, reverse));
            fullTextQuery.setSort(sortField);
        }

        // combine two String[] arrays into a 3rd array using System.arraycopy
        String[] defaultFields = {FullTextQuery.SCORE, FullTextQuery.THIS};
        String[] typeFields = searchType.getDisplayFields();
        String[] projectionArgs = new String[defaultFields.length + typeFields.length];
        System.arraycopy(defaultFields, 0, projectionArgs, 0, defaultFields.length);
		System.arraycopy(typeFields, 0, projectionArgs, defaultFields.length, typeFields.length);

        //fullTextQuery.setProjection(FullTextQuery.SCORE, FullTextQuery.THIS,
                //"taxonConcept.taxonName.canonical", "taxonConcept.kingdomConcept.taxonName.canonical");
                //searchType.getDisplayFields()[0], searchType.getDisplayFields()[1]);
        fullTextQuery.setProjection(projectionArgs);
        //doQuery( "commonNames", "commonNamesTotal", mav, fullTextQuery);
        doQuery(searchType.getResultsParam(), searchType.getResultTotalParam(), mav, fullTextQuery);
        
        //close the session
        fullTextSession.close();

        return mav;
    }

    private void doQuery(String resultsParam, String resultTotalParam,
            ModelAndView mav, FullTextQuery fullTextQuery) {

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
    public FullTextQuery buildQuery(FullTextSession fullTextSession, String[] fieldsToSearch, Class modelClass, String queryString) throws ParseException {
        fullTextSession.getSearchFactory().getAnalyzer(modelClass);
        Analyzer analyzer = fullTextSession.getSearchFactory().getAnalyzer(modelClass);
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fieldsToSearch, analyzer);
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
                fullTextSession.getSearchFactory().getAnalyzer(modelClass));
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
    
    /**
     * @param sortFieldMap the sortFieldMap to set 
     */
    public void setSortFieldMap(Map<String, String> sortFieldMap) {
        this.sortFieldMap = sortFieldMap;
    }

    /**
     * @param htmlView the htmlView to set
     */
    public void setHtmlView(String htmlView) {
        this.htmlView = htmlView;
    }

    /**
     * @param jsonView the jsonView to set
     */
    public void setJsonView(String jsonView) {
        this.jsonView = jsonView;
    }

    /**
     *
     * @param searchType the searchType to set
     */
    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }
    
}




