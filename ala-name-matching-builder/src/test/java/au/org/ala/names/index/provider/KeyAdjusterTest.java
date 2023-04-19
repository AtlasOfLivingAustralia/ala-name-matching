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

package au.org.ala.names.index.provider;

import au.org.ala.names.index.*;
import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonomicType;
import au.org.ala.names.util.TestUtils;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test cases for {@link KeyAdjuster}
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class KeyAdjusterTest extends TestUtils {
    private NameAnalyser analyser;
    private KeyAdjuster adjuster;
    private NameProvider provider;

    @Before
    public void setup() {
        this.analyser = new ALANameAnalyser();
        this.provider = new NameProvider();
        this.adjuster = new KeyAdjuster();
        MatchTaxonCondition condition1 = new MatchTaxonCondition();
        condition1.setScientificName("Viruses");
        this.adjuster.addAdjustment(new KeyAdjustment(condition1, null, "VIRUS", null, null, null));
        MatchTaxonCondition condition2 = new MatchTaxonCondition();
        condition2.setNomenclaturalCode(NomenclaturalClassifier.find(NomenclaturalCode.PHYLOCODE));
        this.adjuster.addAdjustment(new KeyAdjustment(condition2, NomenclaturalClassifier.BACTERIAL, null, "", null, null));
        MatchTaxonCondition condition3 = new MatchTaxonCondition();
        condition3.setTaxonRank(RankType.DOMAIN);
        this.adjuster.addAdjustment(new KeyAdjustment(condition3, NomenclaturalClassifier.CULTIVARS, "PLACEHOLDER", "Nurke", NameType.CULTIVAR, RankType.CULTIVAR));
    }


    @Test
    public void testAdjust1() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalClassifier.ZOOLOGICAL, "Osphranter rufus", this.provider, TaxonomicType.ACCEPTED );
        NameKey key = this.analyser.analyse(instance.getCode(), instance.getScientificName(), instance.getScientificNameAuthorship(), instance.getRank(), null, null, false).getNameKey();
        NameKey key2 = this.adjuster.adjustKey(key, instance, this.provider);
        assertSame(key, key2);
    }

    @Test
    public void testAdjust2() {
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalClassifier.BOTANICAL, NomenclaturalClassifier.BOTANICAL.getAcronym(), this.provider,"Acacia dealbata", "Link.", null,null, TaxonomicType.MISAPPLIED, TaxonomicType.MISAPPLIED.getTerm(), RankType.SPECIES,  RankType.SPECIES.getRank(), null, null,null, null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance.getCode(), instance.getScientificName(), instance.getScientificNameAuthorship(), instance.getRank(), null, null, false).getNameKey();
        NameKey key2 = this.adjuster.adjustKey(key, instance, this.provider);
        assertSame(key, key2);
    }

    @Test
    public void testAdjust3() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalClassifier.VIRUS, "Viruses", this.provider, TaxonomicType.ACCEPTED );
        NameKey key = this.analyser.analyse(instance.getCode(), instance.getScientificName(), instance.getScientificNameAuthorship(), instance.getRank(), null, null, false).getNameKey();
        NameKey key2 = this.adjuster.adjustKey(key, instance, this.provider);
        assertNotSame(key, key2);
        assertEquals(key.getCode(), key2.getCode());
        assertEquals("VIRUS", key2.getScientificName());
        assertEquals(key.getScientificNameAuthorship(), key2.getScientificNameAuthorship());
        assertEquals(key.getType(), key2.getType());
        assertEquals(key.getRank(), key2.getRank());
    }

    @Test
    public void testAdjust4() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalClassifier.find(NomenclaturalCode.PHYLOCODE), "Viruses", this.provider, TaxonomicType.MISAPPLIED );
        NameKey key = this.analyser.analyse(instance.getCode(), instance.getScientificName(), instance.getScientificNameAuthorship(), instance.getRank(), null, null, false).getNameKey();
        NameKey key2 = this.adjuster.adjustKey(key, instance, this.provider);
        assertNotSame(key, key2);
        assertEquals(NomenclaturalClassifier.BACTERIAL, key2.getCode());
        assertEquals("VIRUS", key2.getScientificName());
        assertEquals(key.getScientificNameAuthorship(), key2.getScientificNameAuthorship());
        assertEquals(key.getType(), key2.getType());
        assertEquals(key.getRank(), key2.getRank());
    }

    @Test
    public void testAdjust5() {
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalClassifier.find(NomenclaturalCode.PHYLOCODE), NomenclaturalCode.PHYLOCODE.getAcronym(), this.provider,"Acacia dealbata", "Link.", null,null, TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES,  RankType.SPECIES.getRank(), null, null,null, null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance.getCode(), instance.getScientificName(), instance.getScientificNameAuthorship(), instance.getRank(), null, null, false).getNameKey();
        NameKey key2 = this.adjuster.adjustKey(key, instance, this.provider);
        assertNotSame(key, key2);
        assertEquals(NomenclaturalClassifier.BACTERIAL, key2.getCode());
        assertEquals(key.getScientificName(), key2.getScientificName());
        assertNull(key2.getScientificNameAuthorship());
        assertEquals(key.getType(), key2.getType());
        assertEquals(key.getRank(), key2.getRank());
    }


    @Test
    public void testAdjust6() {
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalClassifier.BOTANICAL, NomenclaturalClassifier.BOTANICAL.getAcronym(), this.provider,"Acacia dealbata", "Link.", null,null, TaxonomicType.MISAPPLIED, TaxonomicType.MISAPPLIED.getTerm(), RankType.DOMAIN,  RankType.DOMAIN.getRank(), null, null,null, null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance.getCode(), instance.getScientificName(), instance.getScientificNameAuthorship(), instance.getRank(), null, null, false).getNameKey();
        NameKey key2 = this.adjuster.adjustKey(key, instance, this.provider);
        assertNotSame(key, key2);
        assertEquals(NomenclaturalClassifier.CULTIVARS, key2.getCode());
        assertEquals("PLACEHOLDER", key2.getScientificName());
        assertEquals("Nurke", key2.getScientificNameAuthorship());
        assertEquals(NameType.CULTIVAR, key2.getType());
        assertEquals(RankType.CULTIVAR, key2.getRank());
    }
}
