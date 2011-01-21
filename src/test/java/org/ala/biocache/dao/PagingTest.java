package org.ala.biocache.dao;

import junit.framework.TestCase;
import au.org.ala.biocache.FullRecord;
import au.org.ala.biocache.OccurrenceConsumer;
import au.org.ala.biocache.OccurrenceDAO;
import au.org.ala.biocache.Versions;

public class PagingTest extends TestCase {

	/**
	 * A junit test that test paging functionality.
	 */
	public void testPaging(){
		
		OccurrenceDAO.pageOverAll(Versions.RAW(), new OccurrenceConsumer(){
			@Override
			public void consume(FullRecord fullrecord) {
				System.out.println("GUID: "+ fullrecord.getO().getUuid());
			}
		});
	}
}
