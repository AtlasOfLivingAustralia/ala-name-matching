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

import org.junit.BeforeClass;
import org.junit.Test;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.sds.util.Configuration;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */

public class PlantPestUnderEradicationTest {

//  static DataSource dataSource;
    static CBIndexSearch cbIndexSearch;
    static SensitiveSpeciesFinder finder;

    @BeforeClass
    public static void runOnce() throws Exception {
//        dataSource = new BasicDataSource();
//        ((BasicDataSource) dataSource).setDriverClassName("com.mysql.jdbc.Driver");
//        ((BasicDataSource) dataSource).setUrl("jdbc:mysql://localhost/portal");
//        ((BasicDataSource) dataSource).setUsername("root");
//        ((BasicDataSource) dataSource).setPassword("password");

        cbIndexSearch = new CBIndexSearch(Configuration.getInstance().getNameMatchingIndex());
        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder("file:///data/sds/sensitive-species.xml", cbIndexSearch);
    }

    @Test
    public void redImportedFireAntInPQADuringEradication() {
//        System.out.println("redImportedFireAntInPQADuringEradication");
//        SensitiveTaxon ss = finder.findSensitiveSpecies("Solenopsis invicta");
//        assertNotNull(ss);
//
//        String latitude = "-27.58333";   // Wacol, Qld
//        String longitude = "152.9167";
//        String date = "2011-03-21";
//
//        Map<String, String> facts = new HashMap<String, String>();
//        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
//        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
//        facts.put(FactCollection.EVENT_DATE_KEY, date);
//
//        ValidationService service = ServiceFactory.createValidationService(ss);
//        ValidationOutcome outcome = service.validate(facts);
//
//        assertTrue(outcome.isValid());
//        assertTrue(outcome.isLoadable());
    }

    @Test
    public void redImportedFireAntInPQABeforeEradication() {
//        System.out.println("redImportedFireAntInPQABeforeEradication");
//        SensitiveTaxon ss = finder.findSensitiveSpecies("Solenopsis invicta");
//        assertNotNull(ss);
//
//        String latitude = "-27.58333";   // Wacol, Qld
//        String longitude = "152.9167";
//        String date = "2001-02-15";
//
//        Map<String, String> facts = new HashMap<String, String>();
//        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
//        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
//        facts.put(FactCollection.EVENT_DATE_KEY, date);
//
//        ValidationService service = ServiceFactory.createValidationService(ss);
//        ValidationOutcome outcome = service.validate(facts);
//
//        assertTrue(outcome.isValid());
//        assertTrue(outcome.isLoadable());
    }

    @Test
    public void redImportedFireAntOutsidePQA() {
//        System.out.println("redImportedFireAntOutsidePQA");
//        SensitiveTaxon ss = finder.findSensitiveSpecies("Solenopsis invicta");
//        assertNotNull(ss);
//
//        String latitude = "-27.560406";   // Toowoomba, Qld
//        String longitude = "151.961625";
//        String date = "2004-01-29";
//
//        Map<String, String> facts = new HashMap<String, String>();
//        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
//        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
//        facts.put(FactCollection.EVENT_DATE_KEY, date);
//
//        ValidationService service = ServiceFactory.createValidationService(ss);
//        ValidationOutcome outcome = service.validate(facts);
//
//        assertTrue(outcome.isValid());
//        assertFalse(outcome.isLoadable());
    }

}
