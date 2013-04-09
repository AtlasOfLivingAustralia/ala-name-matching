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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.util.Configuration;
import au.org.ala.sds.validation.FactCollection;
import au.org.ala.sds.validation.ServiceFactory;
import au.org.ala.sds.validation.ValidationOutcome;
import au.org.ala.sds.validation.ValidationService;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */

public class PlantPestNotKnownInAustraliaTest {

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
        System.out.println(Configuration.getInstance().getNameMatchingIndex());
        cbIndexSearch = new CBIndexSearch(Configuration.getInstance().getNameMatchingIndex());
        String uri = cbIndexSearch.getClass().getClassLoader().getResource("sensitive-species.xml").toURI().toString();
        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(uri, cbIndexSearch, true);
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
        System.out.println(outcome.getReport());

    }

}
