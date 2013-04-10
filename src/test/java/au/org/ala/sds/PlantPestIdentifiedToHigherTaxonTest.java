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
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.util.Configuration;
import au.org.ala.sds.util.PlantPestUtils;
import au.org.ala.sds.validation.FactCollection;
import au.org.ala.sds.validation.ServiceFactory;
import au.org.ala.sds.validation.ValidationOutcome;
import au.org.ala.sds.validation.ValidationService;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the rules associated with category 10 of the Plant Pest
 *
 * @author Natasha Carter (natasha.carter@csiro.au)
 */
public class PlantPestIdentifiedToHigherTaxonTest {
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
    public void testPlantPestUtilExactMatch(){
        SensitiveTaxon ss = finder.findSensitiveSpecies("Bactrocera");
        HashMap<String,String> props = new HashMap<String, String>();
        props.put("genus", "Bactrocera");
        assertTrue(PlantPestUtils.isExactMatch(props,ss));
        props.put("specificEpithet", "bus");
        assertFalse(PlantPestUtils.isExactMatch(props,ss));

    }

    @Test
    public void testSpeciesMarker(){
        SensitiveTaxon ss = finder.findSensitiveSpecies("Bactrocera sp.");
        assertNotNull(ss);

        String latitude = "-35.276771";   // Black Mountain (Epicorp)
        String longitude = "149.112539";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        //Test with a null name
        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);
        System.out.println(outcome.getReport().getMessages().get(0));
        assertTrue(outcome.isValid());
        assertTrue(outcome.isLoadable());
        assertTrue(outcome.getReport().getMessages().get(0).getMessageText().contains("potentially of plant biosecurity concern, are held in Australian reference collections."));
        //Test with an exact match name
        facts.put("scientificName", "Bactrocera sp.");
        outcome = service.validate(facts);
        assertTrue(outcome.isValid());
        assertTrue(outcome.isLoadable());
        assertTrue(outcome.getReport().getMessages().get(0).getMessageText().contains("potentially of plant biosecurity concern, are held in Australian reference collections."));
        assertTrue(((Map)outcome.getResult().get("originalSensitiveValues")).size()>0);
        //Test a species that does NOT match the exact text in which case it should not have any messages are restrictions applied
        facts.put("scientificName", "Bactrocera sp. test");
        outcome = service.validate(facts);
        assertTrue(outcome.isValid());
        assertTrue(outcome.isLoadable());
        assertTrue(outcome.getReport().getMessages().size() == 0);
        assertNull(outcome.getResult());

    }
}
