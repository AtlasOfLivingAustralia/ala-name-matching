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

public class PlantPestSubjectToOfficialControlTest {

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
        //finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder("file:///data/sds/sensitive-species-test.xml", cbIndexSearch);
        String uri = cbIndexSearch.getClass().getClassLoader().getResource("sensitive-species.xml").toURI().toString();
        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(uri, cbIndexSearch, true);
    }

    @Test
    public void phylloxeraInPIZBeforeInfestation() {
        System.out.println("phylloxeraInPIZBeforeInfestation");
        SensitiveTaxon ss = finder.findSensitiveSpecies("Daktulosphaira vitifoliae");
        assertNotNull(ss);

        String latitude = "-35.998337";   // Albury, NSW
        String longitude = "147.014848";
        String date = "2001-02-15";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        facts.put(FactCollection.EVENT_DATE_KEY, date);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertTrue(outcome.isLoadable());
    }

    @Test
    public void phylloxeraInPIZDuringInfestation() {
        System.out.println("phylloxeraInPIZDuringInfestation");
        SensitiveTaxon ss = finder.findSensitiveSpecies("Daktulosphaira vitifoliae");
        assertNotNull(ss);

        String latitude = "-35.998337";   // Albury, NSW
        String longitude = "147.014848";
        String date = "2011-03-21";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        facts.put(FactCollection.EVENT_DATE_KEY, date);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertTrue(outcome.isLoadable());
    }

    @Test
    public void phylloxeraOutsidePIZ() {
        System.out.println("phylloxeraOutsidePIZ");
        SensitiveTaxon ss = finder.findSensitiveSpecies("Daktulosphaira vitifoliae");
        assertNotNull(ss);

        //String latitude = "-35.974255";   // Wahgunyah, Vic
        //String longitude = "146.427294";
        String latitude="-38.3827659";  // Warnambool Vic
        String longitude ="142.48449949";
        String date = "2011-01-29";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        facts.put(FactCollection.EVENT_DATE_KEY, date);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);
        //VIC doesn't need this specie notifiable if it is outside the PIZ
        assertTrue(outcome.isValid());
        assertTrue(outcome.isLoadable());
        //NSW does need it notifiable
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY,"-35.12577");  //Wagga Wagga
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY,"147.35374");
        outcome = service.validate(facts);
        assertTrue(outcome.isValid());
        assertFalse(outcome.isLoadable());
    }

}
