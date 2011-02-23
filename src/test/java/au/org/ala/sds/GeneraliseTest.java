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

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.junit.BeforeClass;
import org.junit.Test;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.sds.model.SensitiveSpecies;
import au.org.ala.sds.util.GeneralisedLocation;
import au.org.ala.sds.util.GeneralisedLocationFactory;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class GeneraliseTest {

    static DataSource dataSource;
    static CBIndexSearch cbIndexSearch;
    static SensitiveSpeciesFinder finder;

    @BeforeClass
    public static void runOnce() throws Exception {
        dataSource = new BasicDataSource();
        ((BasicDataSource) dataSource).setDriverClassName("com.mysql.jdbc.Driver");
        ((BasicDataSource) dataSource).setUrl("jdbc:mysql://localhost/portal");
        ((BasicDataSource) dataSource).setUsername("root");
        ((BasicDataSource) dataSource).setPassword("password");

        cbIndexSearch = new CBIndexSearch("/data/lucene/namematching");
        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(dataSource, cbIndexSearch);
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
        GeneralisedLocation gl = GeneralisedLocationFactory.getGeneralisedLocation(latitude, longitude, ss.getConservationCategory(latitude, longitude));
        assertEquals("Latitude", "-35.3", gl.getGeneralisedLatitude());
        assertEquals("Longitude", "149.1", gl.getGeneralisedLongitude());
        assertEquals("InMetres", "10000", gl.getGeneralisationInMetres());

        //
        // Critically endangered NSW species in ACT - not generalised
        //
        ss = finder.findSensitiveSpecies("Wollemia nobilis");
        assertNotNull(ss);
        gl = GeneralisedLocationFactory.getGeneralisedLocation(latitude, longitude, ss.getConservationCategory(latitude, longitude));
        assertEquals("Latitude", "-35.276771", gl.getGeneralisedLatitude());
        assertEquals("Longitude", "149.112539", gl.getGeneralisedLongitude());
        assertEquals("InMetres", "", gl.getGeneralisationInMetres());

        //
        // Critically endangered NSW species in NZ - not generalised
        //
        latitude = "-41.538137";    // NZ
        longitude = "173.968817";
        gl = GeneralisedLocationFactory.getGeneralisedLocation(latitude, longitude, ss.getConservationCategory(latitude, longitude));
        assertEquals("Latitude", "-41.538137", gl.getGeneralisedLatitude());
        assertEquals("Longitude", "173.968817", gl.getGeneralisedLongitude());
        assertEquals("InMetres", "", gl.getGeneralisationInMetres());

        //
        // Critically endangered NSW species in NSW - position not published
        //
        latitude = "-33.630629";    // NSW
        longitude = "150.441284";
        gl = GeneralisedLocationFactory.getGeneralisedLocation(latitude, longitude, ss.getConservationCategory(latitude, longitude));
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
        gl = GeneralisedLocationFactory.getGeneralisedLocation(latitude, longitude, ss.getConservationCategory(latitude, longitude));
        assertEquals("Latitude", "-40.1", gl.getGeneralisedLatitude());
        assertEquals("Longitude", "148.1", gl.getGeneralisedLongitude());
        assertEquals("InMetres", "10000", gl.getGeneralisationInMetres());

        //
        // Find sensitive species by LSID
        //
        latitude = "-33.630629";    // NSW
        longitude = "150.441284";
        ss = finder.findSensitiveSpeciesByLsid("urn:lsid:biodiversity.org.au:afd.taxon:fb2de285-c58c-4c63-9268-9beef7c61c16");
        gl = GeneralisedLocationFactory.getGeneralisedLocation(latitude, longitude, ss, "NSW");
        assertEquals("Latitude", "-33.6", gl.getGeneralisedLatitude());
        assertEquals("Longitude", "150.4", gl.getGeneralisedLongitude());
        assertEquals("InMetres", "10000", gl.getGeneralisationInMetres());
    }
}
