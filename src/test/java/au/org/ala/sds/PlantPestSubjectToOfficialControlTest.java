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

import java.util.HashMap;
import java.util.Map;

import au.org.ala.sds.model.GeoLocation;
import au.org.ala.sds.model.Message;
import au.org.ala.sds.util.GeoLocationHelper;
import au.org.ala.sds.validation.*;
import org.junit.BeforeClass;
import org.junit.Test;

import au.org.ala.names.search.ALANameSearcher;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.util.Configuration;

import static org.junit.Assert.*;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */

public class PlantPestSubjectToOfficialControlTest {

//  static DataSource dataSource;
    static ALANameSearcher nameSearcher;
    static SensitiveSpeciesFinder finder;

    @BeforeClass
    public static void runOnce() throws Exception {
//        dataSource = new BasicDataSource();
//        ((BasicDataSource) dataSource).setDriverClassName("com.mysql.jdbc.Driver");
//        ((BasicDataSource) dataSource).setUrl("jdbc:mysql://localhost/portal");
//        ((BasicDataSource) dataSource).setUsername("root");
//        ((BasicDataSource) dataSource).setPassword("password");

        nameSearcher = new ALANameSearcher(Configuration.getInstance().getNameMatchingIndex());
        //finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder("file:///data/sds/sensitive-species-test.xml", cbIndexSearch);
        String uri = nameSearcher.getClass().getClassLoader().getResource("sensitive-species.xml").toURI().toString();
        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(uri, nameSearcher, true);
    }

    @Test
    public void inOfficialControlShapeArea(){
        System.out.println("officialControlShapeArea");
        SensitiveTaxon ss = finder.findSensitiveSpecies("Daktulosphaira vitifoliae");
        assertNotNull(ss);
        String latitude = "-35.998337";   // Albury, NSW
        String longitude = "147.014848";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        facts.put(GeoLocationHelper.LGA_BOUNDARIES_LAYER,"Albury");

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertEquals("PBC4", outcome.getReport().getCategory());
        assertTrue(outcome.isValid());
        assertTrue(outcome.isLoadable());
        assertEquals(MessageFactory.getMessageText(MessageFactory.PLANT_PEST_MSG_CAT4_A1, "Daktulosphaira vitifoliae", "New South Wales"), outcome.getReport().getAssertion());

    }

    @Test
    public void inOfficialControlState(){
        System.out.println("officialControlStateArea");
        SensitiveTaxon ss = finder.findSensitiveSpecies("Idioscopus nitidulus");
        assertNotNull(ss);
        String latitude = "-10.11";   // Badu (Queensland)
        String longitude = "142.11";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        facts.put(GeoLocationHelper.LGA_BOUNDARIES_LAYER,"Badu");

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertTrue(outcome.isLoadable());
        assertTrue(outcome.isControlledAccess());
        assertEquals("PBC4", outcome.getReport().getCategory());
        assertEquals(MessageFactory.getMessageText(MessageFactory.PLANT_PEST_MSG_CAT4_B3, "Idioscopus nitidulus", "Queensland"), outcome.getReport().getAssertion());
        assertTrue(outcome.getReport().getMessagesByType(Message.Type.ALERT).size() ==1);
        assertTrue(outcome.getReport().getMessagesByType(Message.Type.WARNING).size() ==1);
    }

    @Test
    public void outsideOfficialControlNoCat6(){
        System.out.println("outsideOfficialControlNoCat6");
        SensitiveTaxon ss = finder.findSensitiveSpecies("Idioscopus nitidulus");
        assertNotNull(ss);
        String latitude = "-35.998337";   // Albury, NSW
        String longitude = "147.014848";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        facts.put(GeoLocationHelper.LGA_BOUNDARIES_LAYER,"Badu");

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);
        assertTrue(outcome.isValid());
        assertTrue(outcome.isLoadable());
        assertFalse(outcome.isControlledAccess());
        assertNull(outcome.getReport().getAssertion());
        assertTrue(outcome.getReport().getMessages().size()==0);
    }

    @Test
    public void outsideOfficialControlWithCat6(){
        System.out.println("officialControlShapeArea");
        SensitiveTaxon ss = finder.findSensitiveSpecies("Daktulosphaira vitifoliae");
        assertNotNull(ss);
        String latitude = "-35.276771";   // Black Mountain (Epicorp)
        String longitude = "149.112539";


        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);


        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertEquals("PBC6", outcome.getReport().getCategory());
        assertTrue(outcome.isValid());
        assertFalse(outcome.isLoadable());
        assertTrue(outcome.getReport().getMessages().size()>0);
    }


//    @Test
//    public void phylloxeraInPIZBeforeInfestation() {
//        System.out.println("phylloxeraInPIZBeforeInfestation");
//        SensitiveTaxon ss = finder.findSensitiveSpecies("Daktulosphaira vitifoliae");
//        assertNotNull(ss);
//
//        String latitude = "-35.998337";   // Albury, NSW
//        String longitude = "147.014848";
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
//    public void phylloxeraInPIZDuringInfestation() {
//        System.out.println("phylloxeraInPIZDuringInfestation");
//        SensitiveTaxon ss = finder.findSensitiveSpecies("Daktulosphaira vitifoliae");
//        assertNotNull(ss);
//
//        String latitude = "-35.998337";   // Albury, NSW
//        String longitude = "147.014848";
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
//    public void phylloxeraOutsidePIZ() {
//        System.out.println("phylloxeraOutsidePIZ");
//        SensitiveTaxon ss = finder.findSensitiveSpecies("Daktulosphaira vitifoliae");
//        assertNotNull(ss);
//
//        //String latitude = "-35.974255";   // Wahgunyah, Vic
//        //String longitude = "146.427294";
//        String latitude="-38.3827659";  // Warnambool Vic
//        String longitude ="142.48449949";
//        String date = "2011-01-29";
//
//        Map<String, String> facts = new HashMap<String, String>();
//        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
//        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
//        facts.put(FactCollection.EVENT_DATE_KEY, date);
//
//        ValidationService service = ServiceFactory.createValidationService(ss);
//        ValidationOutcome outcome = service.validate(facts);
//        //VIC doesn't need this specie notifiable if it is outside the PIZ
//        assertTrue(outcome.isValid());
//        assertTrue(outcome.isLoadable());
//        //NSW does need it notifiable
//        facts.put(FactCollection.DECIMAL_LATITUDE_KEY,"-35.12577");  //Wagga Wagga
//        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY,"147.35374");
//        outcome = service.validate(facts);
//        assertTrue(outcome.isValid());
//        assertFalse(outcome.isLoadable());
//    }

}
