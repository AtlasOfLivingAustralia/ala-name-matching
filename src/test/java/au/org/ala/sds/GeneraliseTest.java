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
package au.org.ala.sds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import au.org.ala.sds.SensitiveSpeciesFinder;
import au.org.ala.sds.model.SensitiveSpecies;
import au.org.ala.sds.util.GeneralisedLocation;

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
	    
		//
	    // Endangered Birds Australia species in ACT - position generalised
	    //
	    SensitiveSpecies ss = finder.findSensitiveSpecies("Crex crex");
		assertNotNull(ss);
		String latitude = "-35.276771";   // Epicorp
		String longitude = "149.112539";
		GeneralisedLocation gl = new GeneralisedLocation(latitude, longitude, ss.getConservationCategory(latitude, longitude));
		assertEquals("Latitude", "-35.3", gl.getGeneralisedLatitude());
		assertEquals("Longitude", "149.1", gl.getGeneralisedLongitude());
		assertEquals("InMetres", "10000", gl.getGeneralisationInMetres());
		
		//
		// Critically endangered NSW species in ACT - not generalised
		//
		ss = finder.findSensitiveSpecies("Wollemia nobilis");
		assertNotNull(ss);
        gl = new GeneralisedLocation(latitude, longitude, ss.getConservationCategory(latitude, longitude));
		assertEquals("Latitude", "-35.276771", gl.getGeneralisedLatitude());
		assertEquals("Longitude", "149.112539", gl.getGeneralisedLongitude());
        assertEquals("InMetres", "", gl.getGeneralisationInMetres());
        
        //
        // Critically endangered NSW species in NZ - not generalised
        //
        latitude = "-41.538137";    // NZ
        longitude = "173.968817";
        gl = new GeneralisedLocation(latitude, longitude, ss.getConservationCategory(latitude, longitude));
        assertEquals("Latitude", "-41.538137", gl.getGeneralisedLatitude());
        assertEquals("Longitude", "173.968817", gl.getGeneralisedLongitude());
        assertEquals("InMetres", "", gl.getGeneralisationInMetres());
         
        //
        // Critically endangered NSW species in NSW - position not published
        //
        latitude = "-33.630629";    // NSW
        longitude = "150.441284";
        gl = new GeneralisedLocation(latitude, longitude, ss.getConservationCategory(latitude, longitude));
        assertEquals("Latitude", "", gl.getGeneralisedLatitude());
        assertEquals("Longitude", "", gl.getGeneralisedLongitude());
        assertEquals("InMetres", "", gl.getGeneralisationInMetres());
         
        //
        // Endangered TAS species in TAS - generalised
        //
        ss = finder.findSensitiveSpecies("Galaxias fontanus");
        assertNotNull(ss);
        latitude = "-40.111689";    // TAS
        longitude = "148.095703";
        gl = new GeneralisedLocation(latitude, longitude, ss.getConservationCategory(latitude, longitude));
        assertEquals("Latitude", "-40.1", gl.getGeneralisedLatitude());
        assertEquals("Longitude", "148.1", gl.getGeneralisedLongitude());
        assertEquals("InMetres", "10000", gl.getGeneralisationInMetres());
        
	}
}
