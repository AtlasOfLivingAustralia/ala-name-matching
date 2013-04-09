/*
 * Copyright (C) 2012 Atlas of Living Australia
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
 */
package au.org.ala.sds;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.sds.util.Configuration;
import au.org.ala.sds.validation.FactCollection;
import au.org.ala.sds.validation.ValidationOutcome;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * Test the complete SDS workflow as interfaced by the SensitiveDataService class
 * @author Natasha Carter (natasha.carter@csiro.au)
 */
public class SensitiveDataServiceTest {

    static CBIndexSearch cbIndexSearch;
    static SensitiveSpeciesFinder finder;
    static SensitiveDataService sds;

    @BeforeClass
    public static void runOnce() throws Exception {

        System.out.println(Configuration.getInstance().getNameMatchingIndex());
        cbIndexSearch = new CBIndexSearch(Configuration.getInstance().getNameMatchingIndex());
        //The URI to the test list - only contains entries that are used in one or more the the tests
        String uri = cbIndexSearch.getClass().getClassLoader().getResource("sensitive-species.xml").toURI().toString();
        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(uri, cbIndexSearch, true);
        sds = new SensitiveDataService();
    }

    @Test
    public void testFlagCases(){
        //PBC7
        Map<String,String> props = new HashMap<String,String>();
        props.put("scientificName", "Aus bus");
        props.put("eventDate","2013-04-09");
        props.put("recordedBy", "Natasha Carter");
        props.put("decimalLatitude", "123.123");
        props.put("PBC7", "T");
        ValidationOutcome vo =sds.testMapDetails(finder, props,"Aus,bus");
        assertNotNull(vo);
        assertTrue(vo.isLoadable());
        assertTrue(vo.getReport().getMessages().get(0).getMessageText().contains("has been intercepted during a quarantine"));
        assertTrue(vo.getResult().get("decimalLatitude").equals(""));
        System.out.println(vo.getResult());

        //PBC8
        props.put("PBC7","");
        props.put("PBC8", "T");
        vo =sds.testMapDetails(finder, props,"Aus,bus");
        assertNotNull(vo);
        assertTrue(vo.isLoadable());
        assertTrue(vo.getReport().getMessages().get(0).getMessageText().contains("but transient populations have occurred from time to time"));
        assertTrue(vo.getResult().get("decimalLatitude").equals(""));
        System.out.println(vo.getResult());
    }

    @Test
    public void testConservationSpecies (){
        String latitude = "-35.276771";   // Epicorp
        String longitude = "149.112539";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put("dataResourceUid", "dr359");
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        facts.put("minimumElevationInMeters", "Do nothing");
        facts.put("eventID", "1234");
        facts.put("stateProvince", "Australian Capital Territory");
        facts.put("scientificName", "Crex crex");
        facts.put("taxonConceptID", "urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca");
        facts.put("locationRemarks", "remarks");
        facts.put("day", "10");
        facts.put("month", "10");
        facts.put("year", "2010");
        facts.put("verbatimCoordinates", "These need to be withheld");

        ValidationOutcome outcome = sds.testMapDetails(finder,facts,"Crex crex", "urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca");

        assertTrue(outcome.isValid());
        assertTrue(outcome.isSensitive());
        Map<String, Object> result = outcome.getResult();
        assertNotNull(result);

        assertEquals("Latitude", "-35.3", result.get("decimalLatitude"));
        assertEquals("Longitude", "149.1", result.get("decimalLongitude"));
        assertEquals("InMetres", "10000", result.get("generalisationInMetres"));
        assertEquals("eventID", "", result.get("eventID"));
        assertEquals("locationRemarks", "", result.get("locationRemarks"));
        assertEquals("day", "", result.get("day"));
        assertEquals("informationWithheld", "The eventID and day information has been withheld in accordance with Birds Australia data policy", result.get("informationWithheld"));
        assertEquals("dataGeneralizations", "Location in ACT generalised to 0.1 degrees. \nSensitive in AUS [Endangered, Birds Australia]", result.get("dataGeneralizations"));

        Map<String, String> originalSenstiveValues = (Map<String, String>) outcome.getResult().get("originalSensitiveValues");
        assertNotNull(originalSenstiveValues);

        assertTrue(outcome.getResult().get("verbatimCoordinates").toString().length()==0);

        assertEquals("Original latitude", "-35.276771", originalSenstiveValues.get("decimalLatitude"));
        assertEquals("Original longitude", "149.112539", originalSenstiveValues.get("decimalLongitude"));
        assertEquals("Original eventID", "1234", originalSenstiveValues.get("eventID"));
        assertEquals("Original locationRemarks", "remarks", originalSenstiveValues.get("locationRemarks"));
        assertEquals("Original day", "10", originalSenstiveValues.get("day"));
    }

    @Test
    public void testPestSpecies(){

        // No location
        Map<String, String> facts = new HashMap<String, String>();

        ValidationOutcome outcome = sds.testMapDetails(finder, facts, "Heterobostrychus aequalis");

        assertTrue(outcome.isValid());
        assertFalse(outcome.isLoadable());
    }
}
