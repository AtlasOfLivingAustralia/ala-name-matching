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

import org.junit.BeforeClass;
import org.junit.Test;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.validation.FactCollection;
import au.org.ala.sds.validation.PlantPestOutcome;
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

        cbIndexSearch = new CBIndexSearch("/data/lucene/namematching");
        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder("file:///data/sds/sensitive-species.xml", cbIndexSearch);
    }

    @Test
    public void phylloxeraInPIZDuringInfestation() {
        System.out.println("phylloxeraInPIZDuringInfestation");
        SensitiveTaxon ss = finder.findSensitiveSpecies("Daktulosphaira vitifoliae");
        assertNotNull(ss);

        String latitude = "-35.998337";   // Albury, NSW
        String longitude = "147.014848";
        String date = "2011-03-21";

        FactCollection facts = new FactCollection();
        facts.add(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.add(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        facts.add(FactCollection.EVENT_DATE_KEY, date);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertTrue(outcome instanceof PlantPestOutcome);
        assertTrue(((PlantPestOutcome) outcome).isLoadable());
    }

    @Test
    public void phylloxeraInPIZBeforeInfestation() {
        System.out.println("phylloxeraInPIZBeforeInfestation");
        SensitiveTaxon ss = finder.findSensitiveSpecies("Daktulosphaira vitifoliae");
        assertNotNull(ss);

        String latitude = "-35.998337";   // Albury, NSW
        String longitude = "147.014848";
        String date = "2001-02-15";

        FactCollection facts = new FactCollection();
        facts.add(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.add(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        facts.add(FactCollection.EVENT_DATE_KEY, date);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertTrue(outcome instanceof PlantPestOutcome);
        assertTrue(((PlantPestOutcome) outcome).isLoadable());
    }

    @Test
    public void phylloxeraOutsidePIZ() {
        System.out.println("phylloxeraOutsidePIZ");
        SensitiveTaxon ss = finder.findSensitiveSpecies("Daktulosphaira vitifoliae");
        assertNotNull(ss);

        String latitude = "-35.974255";   // Wahgunyah, Vic
        String longitude = "146.427294";
        String date = "2011-01-29";

        FactCollection facts = new FactCollection();
        facts.add(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.add(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        facts.add(FactCollection.EVENT_DATE_KEY, date);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertTrue(outcome instanceof PlantPestOutcome);
        assertFalse(((PlantPestOutcome) outcome).isLoadable());
    }

}
