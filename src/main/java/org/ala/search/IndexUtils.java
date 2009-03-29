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

import org.ala.model.CommonName;
import org.ala.model.DataProvider;
import org.ala.model.DataResource;
import org.ala.model.GeoRegion;
import org.ala.model.Locality;
import org.ala.model.TaxonConcept;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

/**
 * Utility to kick off the Lucene/Hibernate search based indexing
 *
 * @author "Dave Martin (David.Martin@csiro.au)"
 */
public class IndexUtils implements InitializingBean{

	protected Logger logger = Logger.getLogger(this.getClass());
	protected Indexer indexer;
	
	/**
	 * Runnable for kicking off lucene index generation.
	 *
	 * @author "Dave Martin (David.Martin@csiro.au)"
	 */
	public class IndexerGenerator implements Runnable {
		
		public void run(){
			
			logger.info("Checking search indexes.....");
			
			try {
				if(!indexer.indexAvailable(CommonName.class, "name")){
					logger.info("Indexing common names...");
					indexer.setHqlQuery("from CommonName cn " +
							"inner join fetch cn.taxonConcept tc " +
							"inner join fetch tc.taxonName " +
							"left join fetch tc.kingdomConcept kc " +
							"left join fetch kc.taxonName " +
							"left join fetch tc.familyConcept fc " +
							"left join fetch fc.taxonName");
					indexer.index();
					logger.info("Finishing indexing common names.");
				}
				if(!indexer.indexAvailable(GeoRegion.class, "name")){
					logger.info("Indexing georegion...");
					indexer.setHqlQuery("from GeoRegion gr inner join gr.geoRegionType");
					indexer.index();
					logger.info("Finishing indexing geo regions.");
				}
				if(!indexer.indexAvailable(Locality.class, "name")){
					logger.info("Indexing localities...");
					indexer.setHqlQuery("from Locality l inner join l.geoRegion gr inner join gr.geoRegionType");
					indexer.index();
					logger.info("Finished indexing localities.");
				}
				if(!indexer.indexAvailable(DataResource.class, "name")){
					logger.info("Indexing data resources...");
					indexer.setHqlQuery("from DataResource dr where dr.occurrenceCount>0");
					indexer.index();
					logger.info("Finished data resources.");
				}
				if(!indexer.indexAvailable(DataProvider.class, "name")){
					logger.info("Indexing data providers...");
					indexer.setHqlQuery("from DataProvider dp where dp.occurrenceCount>0");
					indexer.index();
					logger.info("Finished data providers.");
				}
				if(!indexer.indexAvailable(TaxonConcept.class, "taxonName.canonical")){
					logger.info("Indexing taxon concepts...");
					indexer.setHqlQuery(
							"from TaxonConcept tc " +
							"inner join fetch tc.taxonName " +
							"left join fetch tc.kingdomConcept kc " +
							"left join fetch kc.taxonName " +
							"left join fetch tc.familyConcept fc " +
							"left join fetch fc.taxonName " +
							"where tc.dataResourceId=1");
					indexer.index();
					logger.info("Finished indexing taxon concepts.");
				}
				logger.info("Search indexes generated.");
			} catch (Exception e) {
				logger.error("Problem generating lucene indexes......");
				logger.error(e.getMessage(), e);
			}
		}
	}
	/**
	 * @param indexer the indexer to set
	 */
	public void setIndexer(Indexer indexer) {
		this.indexer = indexer;
	}

	/**
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		IndexerGenerator ig = new IndexerGenerator();
		Thread t = new Thread(ig);
		t.start();
	}
}
