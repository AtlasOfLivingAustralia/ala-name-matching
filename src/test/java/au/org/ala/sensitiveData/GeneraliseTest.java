/***************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia
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
package au.org.ala.sensitiveData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import au.org.ala.sensitiveData.model.SensitiveSpecies;
import au.org.ala.sensitiveData.util.GeneralisedLocation;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class GeneraliseTest {

	ApplicationContext context;
	SensitiveSpeciesFinder finder;
	
	@Before
	public void setUp() throws Exception {
		context = new ClassPathXmlApplicationContext("spring-config.xml");
		finder = context.getBean("searchImpl", SensitiveSpeciesFinder.class);
	}

	@Test
	public void testGeneralisation() {
		SensitiveSpecies ss = finder.findSensitiveSpecies("Crex crex");
		assertNotNull(ss);
		
		String latitude = "-67.2883";
		String longitude = "71.1993";
		GeneralisedLocation lg = new GeneralisedLocation(latitude, longitude, ss.getCategory());
		assertEquals("Latitude", "-67.3", lg.getGeneralisedLatitude());
		assertEquals("Longitude", "71.2", lg.getGeneralisedLongitude());
		assertEquals("InMetres", "10000", lg.getGeneralisationInMetres());
		
		ss = finder.findSensitiveSpecies("Wollemia nobilis");
		assertNotNull(ss);
		
        lg = new GeneralisedLocation(latitude, longitude, ss.getCategory());
		assertEquals("Latitude", "", lg.getGeneralisedLatitude());
		assertEquals("Longitude", "", lg.getGeneralisedLongitude());
        assertEquals("InMetres", "", lg.getGeneralisationInMetres());
	}
}
