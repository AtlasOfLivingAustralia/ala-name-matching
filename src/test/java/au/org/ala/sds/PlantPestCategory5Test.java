/*
 * Copyright (C) 2013 Atlas of Living Australia
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
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.util.Configuration;
import au.org.ala.sds.util.PlantPestUtils;
import au.org.ala.sds.validation.MessageFactory;
import au.org.ala.sds.validation.ServiceFactory;
import au.org.ala.sds.validation.ValidationOutcome;
import au.org.ala.sds.validation.ValidationService;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * Tests the rules associated with Category t5.  The Fruit Fly Exclusion zone and Torres Straight zones
 *
 * @author Natasha Carter (natasha.carter@csiro.au)
 */
public class PlantPestCategory5Test {

    static CBIndexSearch cbIndexSearch;
    static SensitiveSpeciesFinder finder;

    @BeforeClass
    public static void runOnce() throws Exception {

        System.out.println(Configuration.getInstance().getNameMatchingIndex());
        cbIndexSearch = new CBIndexSearch(Configuration.getInstance().getNameMatchingIndex());
        //The URI to the test list - only contains entries that are used in one or more the the tests
        String uri = cbIndexSearch.getClass().getClassLoader().getResource("sensitive-species.xml").toURI().toString();
        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(uri, cbIndexSearch, true);
    }

    @Test
    public void testFruitFlyInSA(){
        SensitiveTaxon ss = finder.findSensitiveSpecies("Bactrocera tryoni");
        HashMap<String,String> props = new HashMap<String, String>();
        props.put("stateProvince", "South Australia");
        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(props);
        assertFalse(outcome.isLoadable());
        assertTrue(outcome.getReport().getCategory().equals("PBC6"));

    }

    /**
     * This is testing a rule that is NOT covered in the documentation.  But is a legitimate condition that shoudl probably be
     * handled
     */
    @Test
    public void testFruitFlyInFFEZInSANoDate(){
        SensitiveTaxon ss = finder.findSensitiveSpecies("Bactrocera tryoni");
        HashMap<String,String> props = new HashMap<String, String>();
        //Renkmark SA
        props.put("decimalLatitude","-34.17484");
        props.put("decimalLongitude", "140.74619");
        //props.put("stateProvince", "South Australia");
        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(props);
        assertTrue(outcome.isLoadable());
        assertTrue(outcome.getReport().getCategory().equals("PBC5b"));
        assertTrue(outcome.getReport().getAssertion().equals(MessageFactory.getMessageText(MessageFactory.PLANT_PEST_MSG_CAT5B_A3)));
    }

    @Test
    public void testFruitFlyInFFEZInSABeforeManagementDate(){
        SensitiveTaxon ss = finder.findSensitiveSpecies("Bactrocera tryoni");
        HashMap<String,String> props = new HashMap<String, String>();
        //Renkmark SA
        props.put("decimalLatitude","-34.17484");
        props.put("decimalLongitude", "140.74619");
        props.put("eventDate", "1990-08-01");
        //props.put("stateProvince", "South Australia");
        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(props);
        assertTrue(outcome.isLoadable());
        assertTrue(outcome.getReport().getCategory().equals("PBC5b"));
        assertTrue(outcome.getReport().getAssertion().equals(MessageFactory.getMessageText(MessageFactory.PLANT_PEST_MSG_CAT5B_A2)));
    }


    @Test
    public void testFruitFlyInFFEZInSAAfterManagementDate(){
        SensitiveTaxon ss = finder.findSensitiveSpecies("Bactrocera tryoni");
        HashMap<String,String> props = new HashMap<String, String>();
        //Renkmark SA
        props.put("decimalLatitude","-34.17484");
        props.put("decimalLongitude", "140.74619");
        props.put("eventDate", "2000-08-01");
        //props.put("stateProvince", "South Australia");
        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(props);
        assertTrue(outcome.isLoadable());
        assertTrue(outcome.getReport().getCategory().equals("PBC5b"));
        assertTrue(outcome.getReport().getAssertion().equals(MessageFactory.getMessageText(MessageFactory.PLANT_PEST_MSG_CAT5B_A1)));
    }

    @Test
    public void testPestInTSQZBefore1996(){
        SensitiveTaxon ss = finder.findSensitiveSpecies("Bactrocera philippinensis");
        HashMap<String,String> props = new HashMap<String, String>();

        props.put("decimalLatitude","-10.645743");
        props.put("decimalLongitude", "142.12030");
        props.put("eventDate", "1990-08-01");
        //props.put("stateProvince", "South Australia");
        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(props);
        assertTrue(outcome.isLoadable());
        assertTrue(outcome.getReport().getCategory().equals("PBC5a"));
        assertNotNull(outcome.getReport().getAssertion());
        assertTrue(outcome.getReport().getMessages().get(0).getMessageText().contains("in the Torres Strait Quarantine zones prior to the commencement of the Long Term Containment Strategy"));
    }

    @Test
    public void testPestInTSQZAfter1996(){
        SensitiveTaxon ss = finder.findSensitiveSpecies("Bactrocera philippinensis");
        HashMap<String,String> props = new HashMap<String, String>();

        props.put("decimalLatitude","-10.645743");
        props.put("decimalLongitude", "142.12030");
        props.put("eventDate", "1999-08-01");
        //props.put("stateProvince", "South Australia");
        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(props);
        assertTrue(outcome.isLoadable());
        assertTrue(outcome.getReport().getCategory().equals("PBC5a"));
        assertNotNull(outcome.getReport().getAssertion());
        assertTrue(outcome.getReport().getAssertion().endsWith("Torres Strait provides an additional protection to Australian horticulture from the regular incursions of exotic fruit flies into these islands."));
        assertTrue(outcome.getReport().getMessages().get(0).getMessageText().contains("in the Torres Strait Quarantine zones after the commencement of the Long Term Containment Strategy"));
    }

    @Test
    public void testPestInTSQZNoDate(){
        SensitiveTaxon ss = finder.findSensitiveSpecies("Bactrocera philippinensis");
        HashMap<String,String> props = new HashMap<String, String>();

        props.put("decimalLatitude","-10.645743");
        props.put("decimalLongitude", "142.12030");

        //props.put("stateProvince", "South Australia");
        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(props);
        assertTrue(outcome.isLoadable());
        assertTrue(outcome.getReport().getCategory().equals("PBC5a"));
        assertNotNull(outcome.getReport().getAssertion());
        assertTrue(outcome.getReport().getMessages().get(0).getMessageText().contains("in the Torres Strait Quarantine zones with no date provided"));
    }

    @Test
    public void testOtherPestInTSQZ(){
        SensitiveTaxon ss = finder.findSensitiveSpecies("Lymantria dispar");
        HashMap<String,String> props = new HashMap<String, String>();

        props.put("decimalLatitude","-10.645743");
        props.put("decimalLongitude", "142.12030");

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(props);
        assertTrue(outcome.isLoadable());
        assertTrue(outcome.getReport().getCategory().equals("PBC5a"));
        assertNotNull(outcome.getReport().getAssertion());
        assertTrue(outcome.getReport().getMessages().get(0).getMessageText().contains("These records of a plant pest believed to be absent from Australia"));
    }

}
