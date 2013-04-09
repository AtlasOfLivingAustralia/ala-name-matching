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

package au.org.ala.sds.util;

import org.junit.Test;
import org.junit.BeforeClass;

import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Test cases for the ValidationUtil
 * @author Natasha Carter (natasha.carter@csiro.au)
 */
public class ValidationUtilTest {
    static Map<String,String> props;
    @BeforeClass
    public static void runOnce() throws Exception {
        props =new HashMap<String,String>();
        props.put("scientificName","Aus bus");
        props.put("genus","Aus");
        props.put("recordedBy","Natasha Carter");
        props.put("eventDate","2013-03-09");
        props.put("dataResourceUid","drTest");
        props.put("institutionName", "My Institution");
    }

    @Test
    public void testRestrictToTaxon(){

        Map<String,Object> results = ValidationUtils.restrictToTypes(props, null, "Taxon");
        //System.out.println(results);
        //System.out.println(results.keySet());
        assertTrue(results.get("scientificName").toString().length()>0);
        assertTrue(results.get("eventDate").toString().length()==0);
        assertTrue(results.get("originalSensitiveValues").toString().length()>0);
        Map osv =  (Map)results.get("originalSensitiveValues");
        assertTrue(osv.containsKey("recordedBy"));
        assertTrue(osv.containsKey("institutionName"));

    }

    @Test
    public void testRestrictToTaxonAndExtra(){
        Map<String,Object> results = ValidationUtils.restrictToTypes(props, new String[]{"institutionName", "dataResourceUid"}, "Taxon");
        assertTrue(results.get("scientificName").toString().length()>0);
        assertTrue(results.get("eventDate").toString().length()==0);
        assertTrue(results.get("institutionName").toString().length()>0);
        Map osv =  (Map)results.get("originalSensitiveValues");
        assertTrue(osv.containsKey("recordedBy"));
        assertFalse(osv.containsKey("institutionName"));
    }
}
