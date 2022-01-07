/*
 * Copyright (c) 2021 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 */

package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonomicType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 * Test cases for {@link TaxonConcept}
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class TaxonConceptTest {
    private static final String PROVIDER_ID = "P-1";
    private static final String TAXON_ID = "ID-1";
    private static final String NAME_1 = "Petrogale rothschildi";
    private static final String AUTHOR_1 = "Thomas, 1904";
    private static final String YEAR_1 = "1904";

    private ALANameAnalyser analyser;
    private NameProvider provider;

    @Before
    public void setup() {
        this.analyser = new ALANameAnalyser();
        this.provider = new NameProvider(PROVIDER_ID, 150);
    }

    @Test
    public void testAddInstance1() throws Exception {
        TaxonConceptInstance instance = new TaxonConceptInstance(
                TAXON_ID,
                NomenclaturalClassifier.ZOOLOGICAL,
                NomenclaturalClassifier.ZOOLOGICAL.getAcronym(),
                provider,
                NAME_1,
                AUTHOR_1,
                null,
                YEAR_1,
                TaxonomicType.ACCEPTED,
                TaxonomicType.ACCEPTED.getTerm(),
                RankType.SPECIES,
                RankType.SPECIES.getRank(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        NameKey instanceKey = this.analyser.analyse(NomenclaturalClassifier.ZOOLOGICAL, NAME_1, AUTHOR_1, RankType.SPECIES, null, null, false);
        NameKey nameKey = instanceKey.toNameKey();
        ScientificName name = new ScientificName(null, nameKey);
        TaxonConcept concept = new TaxonConcept(name, nameKey);
        concept.addInstance(instanceKey, instance);
        assertSame(concept, instance.getContainer());
    }
}