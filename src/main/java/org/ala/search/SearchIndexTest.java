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
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

import org.ala.model.CommonName;
import org.apache.lucene.queryParser.QueryParser;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;

/**
 * Command line tool for testing the indexes using the correct analyzer.
 * 
 * @author "Dave Martin (David.Martin@csiro.au)"
 */
public class SearchIndexTest {

	private static void search(String queryString) throws Exception{
		AnnotationConfiguration cfg = new AnnotationConfiguration();
		File file = new File("/Users/davejmartin2/dev/ala-portal-search/src/main/resources/hibernate.cfg.xml");
		cfg.configure(file);
		SessionFactory sf = cfg.buildSessionFactory();
		Session session = sf.openSession();
		FullTextSession fullTextSession = Search.getFullTextSession(session);
		
		
		//Query Parser
		org.apache.lucene.queryParser.QueryParser parser = new QueryParser(
		    "name", 
		    fullTextSession.getSearchFactory().getAnalyzer( CommonName.class )
		);
		
//		queryString = "name:\""+queryString+"\"";
		
		//Parse the query
		org.apache.lucene.search.Query luceneQuery = parser.parse(queryString);
		
		//Create hibernate query
		org.hibernate.search.FullTextQuery fullTextQuery = 
		    fullTextSession.createFullTextQuery( luceneQuery, CommonName.class );
		
		//sort?
//		org.apache.lucene.search.Sort sort = new org.apache.lucene.search.Sort(
//				new SortField("name"));
//		fullTextQuery.setSort(sort);
		
		
		
		
		List<CommonName> commonNames = fullTextQuery.list();
		
		for(int i=0; i<commonNames.size() && i<10; i++){
			CommonName commonName = commonNames.get(i);
			System.out.println(commonName.getId()+" "+commonName.getName()+" ("
					+ commonName.getTaxonConcept().getId()+" - "
					+commonName.getTaxonConcept().getTaxonName().getCanonical()+")");
		}
		System.out.println("Number of results: "+fullTextQuery.getResultSize());
	}
	
	public static void main(String[] args) throws Exception{
		InputStreamReader converter = new InputStreamReader(System.in);
		BufferedReader in = new BufferedReader(converter);
		String queryString = null;
		System.out.print("Enter a search term >> ");
		while(!"quit".equals(queryString = in.readLine())){
			queryString = queryString.trim();
			if(queryString.length()>0){
				//System.out.println("Searching with "+queryString);
				search(queryString);
				System.out.print("\n");
			}
			System.out.print("Enter a search term >> ");
		}
		
		System.out.print("Bye bye");
		System.exit(1);
	}
}
