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

import java.util.Date;

import org.junit.BeforeClass;
import org.junit.Test;

import au.org.ala.sds.model.SensitiveSpecies;
import au.org.ala.sds.model.SensitivityCategory;
import au.org.ala.sds.model.SensitivityZone;
import au.org.ala.sds.validation.ServiceFactory;
import au.org.ala.sds.validation.ValidationOutcome;
import au.org.ala.sds.validation.ValidationService;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */

public class PlantPestNotKnownInAustraliaTest {

    static ValidationService validationService;
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        validationService = ServiceFactory.createValidationService(SensitivityCategory.PLANT_PEST_NOT_KNOWN_IN_AUSTRALIA);
    }

    @Test
    public void testInAustralia() {
        SensitiveSpecies ss = new SensitiveSpecies("Felis catus");
        ValidationOutcome outcome = validationService.validate(ss, SensitivityZone.ACT, new Date());
    }
    
    @Test
    public void testExternalTerritory() {
        SensitiveSpecies ss = new SensitiveSpecies("Felis catus");
        ValidationOutcome outcome = validationService.validate(ss, SensitivityZone.CX, new Date());
        
    }

    @Test
    public void testInTSPZ() {
        SensitiveSpecies ss = new SensitiveSpecies("Felis catus");
        ValidationOutcome outcome = validationService.validate(ss, SensitivityZone.TSPZ, new Date());
    }
    
    @Test
    public void testNotInAustralia() {
        SensitiveSpecies ss = new SensitiveSpecies("Felis catus");
        ValidationOutcome outcome = validationService.validate(ss, null, new Date());
    }

}
