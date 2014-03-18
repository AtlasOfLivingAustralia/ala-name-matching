package au.org.ala.sds;
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

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.util.Configuration;
import au.org.ala.sds.validation.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExoticBiologicalControlTest {
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
    public void testEueupitheciaCisplatensis() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Eueupithecia cisplatensis");
        assertNotNull(ss);


        String latitude = "-35.276771";   // Black Mountain (Epicorp)
        String longitude = "149.112539";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isLoadable());
        assertTrue(outcome.isValid());
        assertEquals("PBC9", outcome.getReport().getCategory());
        assertEquals(MessageFactory.getMessageText(MessageFactory.PLANT_PEST_MSG_CAT9, "Eueupithecia cisplatensis"), outcome.getReport().getAssertion());
    }
}
