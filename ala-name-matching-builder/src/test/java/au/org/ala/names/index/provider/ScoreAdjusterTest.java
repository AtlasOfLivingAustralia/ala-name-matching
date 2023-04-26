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
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test cases for {@link ScoreAdjuster}
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class ScoreAdjusterTest extends TestUtils {
    private ScoreAdjuster adjuster;
    private NameProvider provider;
    private ALANameAnalyser analyser;

    @Before
    public void setup() {
        this.provider = new NameProvider();
        this.adjuster = new ScoreAdjuster();
        this.analyser = new ALANameAnalyser();
        MatchTaxonCondition condition1 = new MatchTaxonCondition();
        condition1.setTaxonomicStatus(TaxonomicType.INCERTAE_SEDIS);
        this.adjuster.addForbidden(condition1);
        MatchTaxonCondition condition2 = new MatchTaxonCondition();
        condition2.setTaxonomicStatus(TaxonomicType.MISAPPLIED);
        this.adjuster.addAdjustment(new ScoreAdjustment(condition2, -10));

        MatchTaxonCondition condition3 = new MatchTaxonCondition();
        condition3.setTaxonRank(RankType.DOMAIN);
        this.adjuster.addAdjustment(new ScoreAdjustment(condition3, 15));
    }

    @Test
    public void testForbidden1() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalClassifier.ZOOLOGICAL, "Osphranter rufus", this.provider, TaxonomicType.ACCEPTED );
        NameKey key = this.analyser.analyse(instance).getNameKey();
        assertNull(adjuster.forbid(instance, key, this.provider));
    }


    @Test
    public void testForbidden2() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalClassifier.ZOOLOGICAL, "Osphranter rufus", this.provider, TaxonomicType.INCERTAE_SEDIS );
        NameKey key = this.analyser.analyse(instance).getNameKey();
        assertEquals("taxonomicStatus:INCERTAE_SEDIS", adjuster.forbid(instance, key, this.provider));
    }

    @Test
    public void testScore1() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalClassifier.ZOOLOGICAL, "Osphranter rufus", this.provider, TaxonomicType.ACCEPTED );
        NameKey key = this.analyser.analyse(instance).getNameKey();
        assertEquals(0, adjuster.score(0, instance, key, this.provider));
    }

    @Test
    public void testScore2() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalClassifier.ZOOLOGICAL, "Osphranter rufus", this.provider, TaxonomicType.ACCEPTED );
        NameKey key = this.analyser.analyse(instance).getNameKey();
        assertEquals(50, adjuster.score(50, instance, key, this.provider));
    }

    @Test
    public void testScore3() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalClassifier.ZOOLOGICAL, "Osphranter rufus", this.provider, TaxonomicType.MISAPPLIED );
        NameKey key = this.analyser.analyse(instance).getNameKey();
        assertEquals(-10, adjuster.score(0, instance, key, this.provider));
    }

    @Test
    public void testScore4() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalClassifier.ZOOLOGICAL, "Osphranter rufus", this.provider, TaxonomicType.MISAPPLIED );
        NameKey key = this.analyser.analyse(instance).getNameKey();
        assertEquals(90, adjuster.score(100, instance, key, this.provider));
    }

    @Test
    public void testScore5() {
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalClassifier.BOTANICAL, NomenclaturalClassifier.BOTANICAL.getAcronym(), this.provider,"Acacia dealbata", "Link.", null, null, TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.DOMAIN,  RankType.DOMAIN.getRank(), null, null,null, null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance).getNameKey();
        assertEquals(115, adjuster.score(100, instance, key, this.provider));
    }

    @Test
    public void testScore6() {
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalClassifier.BOTANICAL, NomenclaturalClassifier.BOTANICAL.getAcronym(), this.provider,"Acacia dealbata", "Link.", null, null, TaxonomicType.MISAPPLIED, TaxonomicType.MISAPPLIED.getTerm(), RankType.DOMAIN,  RankType.DOMAIN.getRank(), null, null,null, null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance).getNameKey();
        assertEquals(105, adjuster.score(100, instance, key, this.provider));
    }
}
