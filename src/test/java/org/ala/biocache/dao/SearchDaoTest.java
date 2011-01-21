package org.ala.biocache.dao;

import java.util.List;

import junit.framework.TestCase;

import org.ala.biocache.dto.DataProviderCountDTO;
import org.ala.biocache.dto.FieldResultDTO;
import org.ala.biocache.dto.SearchQuery;
import org.ala.biocache.util.SearchUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SearchDaoTest extends TestCase {

	public void testSearchDataProviders() throws Exception {
		
		ApplicationContext ac = new ClassPathXmlApplicationContext(new String[]{"classpath:applicationContext-webapp.xml"});
		SearchDAO searchDao = (SearchDAO) ac.getBean("searchDao");
		List<DataProviderCountDTO> countDTOs = searchDao.getDataProviderCounts();
		
		for(DataProviderCountDTO countDTO: countDTOs){
			System.out.println(countDTO);
		}
	}
	
	public void testCollectionByDecade() throws Exception {
		
		ApplicationContext ac = new ClassPathXmlApplicationContext(new String[]{"classpath:applicationContext-webapp.xml"});
		SearchDAO searchDao = (SearchDAO) ac.getBean("searchDao");
		
		SearchUtils s = new SearchUtils();
		SearchQuery searchQuery = new SearchQuery("urn:lsid:biocol.org:col:32981", "collection");
		s.updateCollectionSearchString(searchQuery);
		
		List<FieldResultDTO> countDTOs = searchDao.findRecordByDecadeFor(searchQuery.getQuery());
		
		for(FieldResultDTO countDTO: countDTOs){
			System.out.println(countDTO);
		}
	}
	
}
