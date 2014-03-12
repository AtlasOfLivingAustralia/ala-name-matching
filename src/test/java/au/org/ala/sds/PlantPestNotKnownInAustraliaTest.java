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

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import au.org.ala.sds.model.Message;
import au.org.ala.sds.validation.*;
import org.junit.BeforeClass;
import org.junit.Test;

import au.org.ala.names.search.ALANameSearcher;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.util.Configuration;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */

public class PlantPestNotKnownInAustraliaTest {

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
        System.out.println(Configuration.getInstance().getNameMatchingIndex());
        nameSearcher = new ALANameSearcher(Configuration.getInstance().getNameMatchingIndex());
        String uri = nameSearcher.getClass().getClassLoader().getResource("sensitive-species.xml").toURI().toString();
        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(uri, nameSearcher, true);
        //finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder("file:///data/sds/sensitive-species-test.xml", cbIndexSearch);
    }

    @Test
    public void powderPostBeetle() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Heterobostrychus aequalis");
        assertNotNull(ss);

        // No location
        Map<String, String> facts = new HashMap<String, String>();

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertFalse(outcome.isLoadable());
        System.out.println("MESSAGES::" +outcome.getReport().getMessages());
        assertTrue(outcome.getReport().getMessages().contains(MessageFactory.createInfoMessage(MessageFactory.PLANT_PEST_MSG_CAT1_A0, "Heterobostrychus aequalis")));
    }

    @Test
    public void giantAfricanSnailInAustralia() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Achatina fulica");
        assertNotNull(ss);

        String latitude = "-35.276771";   // Black Mountain (Epicorp)
        String longitude = "149.112539";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertFalse(outcome.isLoadable());
        assertTrue(outcome.getReport().getMessagesByType(Message.Type.WARNING).get(0).getMessageText().contains("This determination has triggered an alert message to Office of the Chief Plant Protection"));
        assertTrue(outcome.getReport().getMessagesByType(Message.Type.ALERT).get(0).getMessageText().contains(" The record was rejected and a message set to the submitter advising them to phone the Exotic Plant Pest Hotline"));
    }

    @Test
    public void asianGyspyMothInAustralia() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Lymantria dispar");
        assertNotNull(ss);

        String latitude = "-35.276771";   // Black Mountain (Epicorp)
        String longitude = "149.112539";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertFalse(outcome.isLoadable());
    }

    @Test
    public void asianGyspyMothInExternalTerritory() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Lymantria dispar");
        assertNotNull(ss);

        String latitude = "-16.286858";   // Coral Sea Islands
        String longitude = "149.964066";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertTrue(outcome.isLoadable());
        assertTrue(outcome.getReport().getCategory().equals("PBC1"));
        assertTrue(outcome.getReport().getAssertion().equals(MessageFactory.getMessageText(MessageFactory.PLANT_PEST_MSG_CAT1_B1,"Coral Sea Islands" ,"Lymantria dispar")));
    }

    @Test
    public void bactroceraPhilippinensisInTSPZ() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Bactrocera philippinensis");
        assertNotNull(ss);

        String latitude = "-10.11";   // Badu (Queensland)
        String longitude = "142.11";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        facts.put(FactCollection.EVENT_DATE_KEY, "1977-06-20");

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertTrue(outcome.isLoadable());
    }

    @Test
    public void asianGyspyMothNotInAustralia() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Lymantria dispar");
        assertNotNull(ss);

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.COUNTRY_KEY, "Indonesia");

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertTrue(outcome.isLoadable());
        assertTrue(outcome.getReport().getCategory().equals("PBC1"));
        assertTrue(outcome.getReport().getAssertion().equals(MessageFactory.getMessageText(MessageFactory.PLANT_PEST_MSG_CAT1_D1, "Lymantria dispar")));
        //assertTrue(outcome.getReport().getMessages().contains(MessageFactory.createInfoMessage(MessageFactory.PLANT_PEST_MSG_CAT1_D1, "Lymantria dispar")));//"This record of a plant biosecurity sensitive species is based on a specimen or specimens collected from a locality outside Australia and now deposited in an Australian reference collection that contributes records to the ALA. The record does not imply that Lymantria dispar has been recorded in Australia. (PPC1-D1)")));
   }

    @Test
    public void failingSpecies(){
        SensitiveTaxon ss = finder.findSensitiveSpecies("Mucor mucedo");
        Map<String,String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY,"-32.4");
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY,"149.4667");

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);
        System.out.println(outcome);
        assertTrue(outcome.isValid());
        assertFalse(outcome.isLoadable());

    }

    @Test
    public void category8HeterobostrychusAequalis() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Heterobostrychus aequalis");
        assertNotNull(ss);
        Map<String,String> facts = new HashMap<String, String>();
        facts.put(FactCollection.STATE_PROVINCE_KEY, "NT");
        facts.put(FactCollection.ZONES_KEY, "NT");
        facts.put(FactCollection.EVENT_DATE_KEY, "1977-06-01");

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertTrue(outcome.isLoadable());
        assertEquals("PBC8", outcome.getReport().getCategory());
        assertEquals(MessageFactory.getMessageText("PBC8", "Heterobostrychus aequalis"), outcome.getReport().getAssertion());
        System.out.println(outcome.getReport());

    }

    @Test
    public void testCat8WithoutDate(){
        SensitiveTaxon ss = finder.findSensitiveSpecies("Heterobostrychus aequalis");
        assertNotNull(ss);
        Map<String,String> facts = new HashMap<String, String>();
        facts.put(FactCollection.STATE_PROVINCE_KEY, "NT");
        facts.put(FactCollection.ZONES_KEY, "NT");

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertFalse(outcome.isLoadable());
        assertEquals("PBC1", outcome.getReport().getCategory());
        System.out.println(outcome.getReport());
    }

    //@Test
    /* NQ:2014-03-12 This test is designed to align with new requirements obtained in the Feb 2014 meeting about APPD
    At the moment it fails because Category1 is consulted before all others. NOT used as a "catch all" as was suggested
    at the meeting. */
    public void testCat6TakesPrecindent(){
        SensitiveTaxon ss = finder.findSensitiveSpecies("Acarapis woodi");
        assertNotNull(ss);
        Map<String,String> facts = new HashMap<String, String>();
        facts.put(FactCollection.STATE_PROVINCE_KEY, "NSW");
        facts.put(FactCollection.ZONES_KEY, "NSW");

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);
        assertTrue(outcome.isValid());
        assertFalse(outcome.isLoadable());
        assertEquals("PBC6", outcome.getReport().getCategory());
        System.out.println(outcome.getReport());

    }

}
