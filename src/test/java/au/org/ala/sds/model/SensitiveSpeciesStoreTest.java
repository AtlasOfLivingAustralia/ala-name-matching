/***************************************************************************
 * Copyright (C) 2011 Atlas of Living Australia
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
package au.org.ala.sds.model;

import org.junit.Test;

public class SensitiveSpeciesStoreTest {

    @Test
    public void testStripTaxonTokens() {

        final String SPECIES_1  = "Acacia acanthoclada subsp. glaucescens";
        final String SPECIES_1E = "Acacia acanthoclada glaucescens";
        final String SPECIES_2  = "Banksia conferta var. parva";
        final String SPECIES_2E = "Banksia conferta parva";
        final String SPECIES_3  = "Abutilon uncinatum ms";
        final String SPECIES_3E = "Abutilon uncinatum";

//        assertEquals(SPECIES_1E, SensitiveTaxonStore.stripTaxonTokens(SPECIES_1));
//        assertEquals(SPECIES_2E, SensitiveTaxonStore.stripTaxonTokens(SPECIES_2));
//        assertEquals(SPECIES_3E, SensitiveTaxonStore.stripTaxonTokens(SPECIES_3));
    }

}
