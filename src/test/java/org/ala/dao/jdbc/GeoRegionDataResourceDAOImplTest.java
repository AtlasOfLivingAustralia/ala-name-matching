/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.dao.jdbc;

import java.util.List;
import org.ala.dao.GeoRegionDataResourceDAO;
import org.ala.model.GeoRegionDataResource;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 *
 * @author nick
 */
public class GeoRegionDataResourceDAOImplTest extends AbstractDependencyInjectionSpringContextTests {

    /**
	 * @see org.springframework.test.AbstractDependencyInjectionSpringContextTests#getConfigLocations()
	 */
	protected String[] getConfigLocations() {
		return new String [] {
				"classpath*:/**/applicationContext-*.xml",
				"classpath*:**/applicationContext-*.xml",
				"classpath*:org/gbif/portal/**/applicationContext-*.xml",
				"classpath*:/org/gbif/portal/**/impl/applicationContext-*-test.xml",
				"classpath*:org/gbif/portal/dao/applicationContext-dao-ro.xml",
				"classpath*:org/gbif/portal/dao/applicationContext-factories.xml",
				"classpath*:/org/gbif/portal/service/impl/applicationContext-service-test.xml",
                "classpath*:org/ala/dao/applicationContext-*.xml"
		};
	}

    /**
     * Test of getDataResourcesForGeoRegion method, of class GeoRegionDataResourceDAOImpl.
     */
    public void testGetDataResourcesForGeoRegion() {
        System.out.println("getDataResourcesForGeoRegion");
        Long geoRegionId = 863L;
        //GeoRegionDataResourceDAO instance = new GeoRegionDataResourceDAOImpl();
        GeoRegionDataResourceDAO grdrDAO = (GeoRegionDataResourceDAO) this.applicationContext.getBean("geoRegionDataResourceDAORO");
        List<GeoRegionDataResource> expResult = null;
        List<GeoRegionDataResource> result = grdrDAO.getDataResourcesForGeoRegion(geoRegionId);
        System.out.println("DataResourcesForGeoRegion list size = " + result.size());

        for (GeoRegionDataResource grdr : result) {
            System.out.println("Data Resource: " + grdr.getDataResourceName() + " " + grdr.getDataResourceId());
            System.out.println("Basis of record: " + grdr.getBasisOfRecord().getName());
            System.out.println("Occurrence counts: " + grdr.getOccurrenceCount());
        }
        //assertEquals(5,result.size());
        //assertEquals(expResult, result);
    }

}