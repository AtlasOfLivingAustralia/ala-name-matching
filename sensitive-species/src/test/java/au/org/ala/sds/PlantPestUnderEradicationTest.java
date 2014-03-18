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

import au.org.ala.sds.model.Message;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.util.GeoLocationHelper;
import au.org.ala.sds.validation.*;
import org.junit.BeforeClass;
import org.junit.Test;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.sds.util.Configuration;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        //finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder("file:///data/sds/sensitive-species-new.xml", cbIndexSearch);
        String uri = cbIndexSearch.getClass().getClassLoader().getResource("sensitive-species.xml").toURI().toString();
        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(uri, cbIndexSearch, true);
    }
    @Test
    public void testTemporaryCatchAllRuleForCategory3(){
        SensitiveTaxon ss = finder.findSensitiveSpecies("Solenopsis invicta");
        String latitude = "-23.546678";   // Emerald, Qld
        String longitude = "148.151751";


        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);

        facts.put(GeoLocationHelper.LGA_BOUNDARIES_LAYER,"Emerald");
        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertTrue(outcome.isLoadable());
        assertTrue(outcome.isControlledAccess());
        assertEquals(MessageFactory.getMessageText(MessageFactory.PLANT_PEST_MSG_CAT3_ALL3, "Solenopsis invicta"),outcome.getReport().getAssertion());
        assertTrue(outcome.getReport().getMessagesByType(Message.Type.ALERT).get(0).getMessageText().contains("This record may represent a new outbreak of the pest. The submitter has been requested to check the status with local biosecurity authorities"));
        assertTrue(outcome.getReport().getMessagesByType(Message.Type.WARNING).get(0).getMessageText().contains("is the subject of an ongoing eradication program. Your record"));

    }

//    @Test
//    public void redImportedFireAntInPQADuringEradication() {
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
//    }
//
//    @Test
//    public void redImportedFireAntInPQABeforeEradication() {
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
//    }
//
//    @Test
//    public void redImportedFireAntOutsidePQA() {
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
//    }

}
