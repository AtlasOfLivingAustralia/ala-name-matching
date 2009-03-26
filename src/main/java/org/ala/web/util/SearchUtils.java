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

package org.ala.web.util;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.springframework.web.servlet.ModelAndView;


/**
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class SearchUtils {

    protected Logger logger = Logger.getLogger(this.getClass());
    private Integer MAX_RESULTS = 10;
    private Integer FIRST_RESULT = 0;
    
	/**
     *
     * @param fullTextSession
     * @param searchString
     * @param clazz
     * @param fieldsToSearch
     * @param resultsParam
     * @param resultTotalParam
     * @param mav
     * @throws ParseException
     */
    public void query(FullTextSession fullTextSession, String searchString, Class clazz, String[] fieldsToSearch,
			String resultsParam, String resultTotalParam, ModelAndView mav) throws ParseException {
		FullTextQuery fullTextQuery = buildQuery(fullTextSession, fieldsToSearch, clazz, searchString);
		doQuery(resultsParam, resultTotalParam, mav, fullTextQuery, MAX_RESULTS, FIRST_RESULT);
	}

	/**
     *
     * @param resultsParam
     * @param resultTotalParam
     * @param mav
     * @param fullTextQuery
     */
    public void doQuery(String resultsParam, String resultTotalParam,
			ModelAndView mav, FullTextQuery fullTextQuery, Integer maxResults, Integer firstResult) {
		fullTextQuery.setMaxResults(maxResults);
		fullTextQuery.setFirstResult(firstResult);
        List results = null;
        int resultsSize = 0;
		try {
            results = fullTextQuery.list();
            resultsSize = fullTextQuery.getResultSize();
        }
        catch (Exception e) {
            mav.addObject(resultsParam+"Error", e.toString());
            logger.error("FullTextQuery returned an exception: " + e);
            //return;
        }

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
    public FullTextQuery buildQuery(FullTextSession fullTextSession,  String[] fieldsToSearch,
            Class modelClass, String queryString) throws ParseException {
    	fullTextSession.getSearchFactory().getAnalyzer(modelClass);
		Analyzer analyzer = fullTextSession.getSearchFactory().getAnalyzer(modelClass);
		MultiFieldQueryParser parser = new MultiFieldQueryParser(fieldsToSearch,analyzer);
		org.apache.lucene.search.Query luceneQuery = parser.parse(queryString);
		org.hibernate.search.FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(luceneQuery, modelClass);
    	return fullTextQuery;
    }

}
