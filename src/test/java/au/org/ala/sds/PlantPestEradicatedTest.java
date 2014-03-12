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

public class PlantPestEradicatedTest {

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
        //finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder("file:///data/sds/sensitive-species-new.xml", cbIndexSearch);
        String uri = nameSearcher.getClass().getClassLoader().getResource("sensitive-species.xml").toURI().toString();
        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(uri, nameSearcher, true);
    }

    @Test
    public void papayaFruitFlyInPQADuringEradication() {
        System.out.println("papayaFruitFlyInPQADuringEradication");
        SensitiveTaxon ss = finder.findSensitiveSpecies("Bactrocera papayae");
        assertNotNull(ss);

        String latitude = "-16.902785";   // Cairns, Qld
        String longitude = "145.738106";
        String date = "1996-01-29";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        facts.put(FactCollection.EVENT_DATE_KEY, date);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertTrue(outcome.isLoadable());
        //assertNotNull(outcome.getAnnotation());
        assertNotNull(outcome.getReport().getAssertion());
        assertEquals(MessageFactory.getMessageText(MessageFactory.PLANT_PEST_MSG_CAT2_A1, "Bactrocera papayae"),outcome.getReport().getAssertion());
    }

    @Test
    public void citrusCankerInPQABeforeEradication() {
        System.out.println("citrusCankerInPQABeforeEradication");
        SensitiveTaxon ss = finder.findSensitiveSpecies("Xanthomonas axonopodis citri");
        assertNotNull(ss);

        String latitude = "-23.546678";   // Emerald
        String longitude = "148.151751";
        String date = "2004-01-29";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        facts.put(FactCollection.EVENT_DATE_KEY, date);
        facts.put(GeoLocationHelper.LGA_BOUNDARIES_LAYER,"Emerald");

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertTrue(outcome.isLoadable());
        assertTrue(outcome.isControlledAccess());
        //test for the correct messages
        assertEquals(MessageFactory.getMessageText(MessageFactory.PLANT_PEST_MSG_CAT2_B1, "Xanthomonas axonopodis citri","Emerald"),outcome.getReport().getAssertion());
        assertTrue(outcome.getReport().getMessages().get(0).getMessageText().contains("Your record Xanthomonas axonopodis citri,2004-01-29 and Emerald has been forwarded to a secure view with the Atlas of Living Australia"));

    }

    @Test
    public void papayaFruitFlyOutsidePQA() {
        System.out.println("papayaFruitFlyOutsidePQA");
        SensitiveTaxon ss = finder.findSensitiveSpecies("Bactrocera papayae");
        assertNotNull(ss);

        String latitude = "-23.546678";   // Emerald, Qld
        String longitude = "148.151751";
        String date = "2004-01-29";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        facts.put(FactCollection.EVENT_DATE_KEY, date);
        facts.put(GeoLocationHelper.LGA_BOUNDARIES_LAYER,"Emerald");

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertFalse(outcome.isLoadable());
        assertTrue(outcome.getReport().getMessagesByType(Message.Type.ALERT).get(0).getMessageText().contains("previously considered eradicated from Australia, has been  forwarded to Atlas of Living Australia from"));
        assertTrue(outcome.getReport().getMessagesByType(Message.Type.WARNING).get(0).getMessageText().contains("This record has been determined to have plant biosecurity sensitivity because the pest is believed absent from Australia having been the subject of a successful eradication campaign"));
    }

    @Test
    public void citrusCankerAfterEradication() {
        System.out.println("citrusCankerAfterEradication");
        SensitiveTaxon ss = finder.findSensitiveSpecies("Xanthomonas axonopodis citri");
        assertNotNull(ss);

        String latitude = "-23.546678";   // Emerald
        String longitude = "148.151751";
        String date = "2010-01-29";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        facts.put(FactCollection.EVENT_DATE_KEY, date);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertFalse(outcome.isLoadable());
        assertTrue(outcome.getReport().getMessagesByType(Message.Type.ALERT).get(0).getMessageText().contains("previously considered eradicated from Australia, has been  forwarded to Atlas of Living Australia from"));
        assertTrue(outcome.getReport().getMessagesByType(Message.Type.WARNING).get(0).getMessageText().contains("This record has been determined to have plant biosecurity sensitivity because the pest is believed absent from Australia having been the subject of a successful eradication campaign"));
    }

    @Test
    public void citrusCankerNoDate(){
        SensitiveTaxon ss = finder.findSensitiveSpecies("Xanthomonas axonopodis citri");
        assertNotNull(ss);

        String latitude = "-23.546678";   // Emerald
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
        assertTrue(outcome.getReport().getMessages().get(0).getMessageText().contains("and Emerald has been forwarded to a secure view with the Atlas of Living Australia"));
        assertEquals(MessageFactory.getMessageText(MessageFactory.PLANT_PEST_MSG_CAT2_A1, "Xanthomonas axonopodis citri"),outcome.getReport().getAssertion());
    }
}
