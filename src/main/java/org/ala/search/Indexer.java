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
package org.ala.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Class for indexing entities using hibernate search (Lucene).
 *
 * @author "Dave Martin (David.Martin@csiro.au)"
 */
public class Indexer {

	protected Logger logger = Logger.getLogger(this.getClass());
	protected int pageSize=5000;
	protected SessionFactory sessionFactory = null;
	protected String hqlQuery;

	public void init() {
		String[] locations = {
			"classpath*:org/gbif/portal/**/applicationContext-*.xml",
			"classpath*:org/ala/**/applicationContext-*.xml"
		};
		ApplicationContext context = new ClassPathXmlApplicationContext(locations);
		sessionFactory = (SessionFactory) context.getBean("hsSessionFactory");
	}
	
	/**
	 * @param args
	 */
	public void index() throws Exception{
		if(hqlQuery==null){
			logger.error("No query specified");
			return;
		}
		Session session = sessionFactory.openSession();
		FullTextSession fullTextSession = Search.getFullTextSession(session);
		int pageNumber = 0;
		while(indexPage(session, fullTextSession, pageNumber)>0){
			pageNumber++;
			logger.debug("Starting page number: "+pageNumber+", total indexed: "+(pageNumber*pageSize));
		}
		fullTextSession.close();
	}

	/**
	 * Checks if index is available
	 * @param fullTextSession
	 * @return
	 * @throws ParseException
	 */
	public boolean indexAvailable(Class clazz, String fieldName) throws ParseException {
		Session session = sessionFactory.openSession();
		FullTextSession fullTextSession = Search.getFullTextSession(session);
		org.apache.lucene.queryParser.QueryParser parser = new QueryParser(
				fieldName, 
			    fullTextSession.getSearchFactory().getAnalyzer(clazz)
			);
			org.apache.lucene.search.Query luceneQuery = parser.parse("*:*");
		
		Query query = fullTextSession.createFullTextQuery(luceneQuery, clazz);
		query.setMaxResults(1);
		List results = query.list();
		boolean indexAvailable = results.size()>0;
		fullTextSession.close();
		return indexAvailable;
	}

	/**
	 * Index a page of results.
	 * 
	 * @param session
	 * @param fullTextSession
	 * @param pageNumber
	 * @return
	 */
	private int indexPage(Session session, FullTextSession fullTextSession,
			int pageNumber) {
		Transaction tx = fullTextSession.beginTransaction();
		Query query = fullTextSession.createQuery(hqlQuery);
		int firstResults = pageNumber * pageSize;
		query.setFirstResult(firstResults);
		query.setMaxResults(pageSize);
		query.setReadOnly(true);
		query.setFetchSize(Integer.MIN_VALUE);
	    query.setCacheable(false);
	    
	    ScrollableResults results = query.scroll(ScrollMode.FORWARD_ONLY);
	    int noIndexed=0;
	    long startTime = System.currentTimeMillis();
	    long increStartTime = System.currentTimeMillis();
	    long increFinishTime = -1;
		while(results.next()){
			Object objectToIndex = results.get(0);
			fullTextSession.index(objectToIndex);
			noIndexed++;
			if(noIndexed%1000==0){
				increFinishTime = System.currentTimeMillis();
				logger.debug("Page number:"+pageNumber+", time taken to index last "+pageSize+" (milliseconds): "+((increFinishTime-increStartTime)));
				increStartTime = System.currentTimeMillis();
			}
		}
		fullTextSession.flushToIndexes();
		fullTextSession.clear();
		session.flush();
		session.clear();

	    long finishTime = System.currentTimeMillis();
	    logger.debug("Total time taken to index a page (millis): "+((finishTime-startTime)));
		results.close();
		tx.commit(); //index is written at commit time 
		return noIndexed;
	}
	
	/**
	 * Kicks off a HQL Query
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws Exception {
		//get a query
		System.out.print("Enter a HQL Query and hit return >> ");
		InputStreamReader converter = new InputStreamReader(System.in);
		BufferedReader in = new BufferedReader(converter);
		String entityName = in.readLine();
		
		//load indexer from spring
		Indexer indexer = new Indexer();
		indexer.init();
		indexer.hqlQuery = entityName;
		
		//start indexing
	    long startTime = System.currentTimeMillis();
		indexer.index();
		long finishTime = System.currentTimeMillis();
		
		//log time taken
		System.out.println("Total time taken to index: "+(finishTime-startTime)/60000+" mins");
		System.exit(1);
	}

	/**
	 * @param pageSize the pageSize to set
	 */
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * @param hqlQuery the hqlQuery to set
	 */
	public void setHqlQuery(String hqlQuery) {
		this.hqlQuery = hqlQuery;
	}

	/**
	 * @param sessionFactory the sessionFactory to set
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
}