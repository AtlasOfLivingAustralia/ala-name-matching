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
import org.gbif.portal.web.controller.RestController;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.springframework.web.servlet.ModelAndView;
import org.ala.web.util.SearchType;
import org.ala.web.util.SearchUtils;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.springframework.web.bind.ServletRequestUtils;

/**
 * Paging Hibernte (Lucene) Search
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class PagingSearchController extends RestController {
    /** SessionFactory  */
    protected SessionFactory sessionFactory;
    /** HTML view name (Tile) */
    protected String htmlView;
    /** JSON view name (Tile) */
    protected String jsonView;
    /** SearchType Enum class */
    SearchType searchType;
    /** Map of field name in JSON table to qualified bean field name */
    protected Map<String, String> sortFieldMap;

    /**
     * Override the super class method 
     *
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
        
        // set the sort field and sort order
        Boolean reverse = ("desc".equals(dir)) ? true : false; 
        if (!"score".equals(sort) && sortFieldMap.containsKey(sort))
            sort = sortFieldMap.get(sort); // map the YUI table column name to bean field names

        /* View name is determined by the (optional rest param "view" which is either
         * blank for HTML view or "json" for the json output. The tile names which are used
         * are set in the dispatcher XML file */
        String view = ("json".equals(viewString)) ? jsonView : htmlView; 
        // Create the mav and add some initial elements to it
        ModelAndView mav = new ModelAndView(view);
        mav.addObject("searchType", searchType.getName());
        mav.addObject("searchString", searchString);
        mav.addObject("pageSize", 10); // sets the number of page "boxes" to display
        mav.addObject("recordsReturned", results);
        mav.addObject("startIndex", startIndex);
        mav.addObject("sort", sort);
        mav.addObject("dir", dir);

        // Build the Hibernate Search query and search
        Session session = sessionFactory.openSession();
        FullTextSession fullTextSession = Search.getFullTextSession(session);
        SearchUtils searchUtils = new SearchUtils();
        FullTextQuery fullTextQuery = searchUtils.buildQuery(fullTextSession, searchType.getSearchFields(), searchType.getBean(), searchString);
        
        if (!"score".equals(sort)) {
            // Optionally set sort field (if not the default score sort field)
            Sort sortField = new Sort(new SortField(sort, reverse));
            fullTextQuery.setSort(sortField);
        }

        // combine two String[] arrays into a 3rd array using System.arraycopy
        String[] projectionArgs = {FullTextQuery.SCORE, FullTextQuery.THIS};
        // use a Hibernate search projection (so we can get the score value back)
        fullTextQuery.setProjection(projectionArgs);
        searchUtils.doQuery(searchType.getResultsParam(), searchType.getResultTotalParam(), mav, fullTextQuery, results, startIndex);
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
     * @param searchType the searchType to set
     */
    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }

}




