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

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import au.org.ala.sensitiveData.model.SensitiveSpecies;

import junit.framework.TestCase;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class GeneraliseTest extends TestCase {

	ApplicationContext context;
	SensitiveSpeciesFinder finder;
	
	protected void setUp() throws Exception {
		super.setUp();
		context = new ClassPathXmlApplicationContext("spring-config.xml");
		finder = context.getBean("searchImpl", SensitiveSpeciesFinder.class);
	}

	public void testGeneralisation() {
		SensitiveSpecies ss = finder.findSensitiveSpecies("Crex crex");
		assertNotNull(ss);
		
		String latitude = "-67.2883";
		String longitude = "71.1993";
		String[] coords = finder.generaliseLocation(ss, latitude, longitude);
		assertEquals("Latitude", "-67.29", coords[0]);
		assertEquals("Longitude", "71.20", coords[1]);

		ss = finder.findSensitiveSpecies("Dasyornis brachypterus");
		assertNotNull(ss);
		
		latitude = "-67.2883";
		longitude = "71.1993";
		coords = finder.generaliseLocation(ss, latitude, longitude);
		assertEquals("Latitude", "-67.3", coords[0]);
		assertEquals("Longitude", "71.2", coords[1]);
	}
}
