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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Test cases for {@link MatchTaxonCondition}
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class MatchTaxonConditionTest extends TestUtils {
    private NameProvider provider;
    private ALANameAnalyser analyser;

    @Before
    public void setup() {
        this.provider = new NameProvider();
        this.analyser = new ALANameAnalyser();
    }

    @Test
    public void testMatch1() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setNomenclaturalCode(NomenclaturalCode.BOTANICAL);
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia dealbata", this.provider, TaxonomicType.HOMOTYPIC_SYNONYM);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch2() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setNomenclaturalCode(NomenclaturalCode.ZOOLOGICAL);
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia dealbata", this.provider, TaxonomicType.HOMOTYPIC_SYNONYM);
        NameKey key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }

    @Test
    public void testMatch3() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setTaxonomicStatus(TaxonomicType.HOMOTYPIC_SYNONYM);
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL,  "Acacia dealbata", this.provider, TaxonomicType.HOMOTYPIC_SYNONYM);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch4() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setTaxonomicStatus(TaxonomicType.ACCEPTED);
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia dealbata", this.provider, TaxonomicType.HOMOTYPIC_SYNONYM);
        NameKey key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }

    @Test
    public void testMatch5() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setNomenclaturalStatus(NomenclaturalStatus.DOUBTFUL);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia dealbata", null, null,null, TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), Collections.singleton(NomenclaturalStatus.DOUBTFUL), "doubtful", null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch6() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setNomenclaturalStatus(NomenclaturalStatus.DOUBTFUL);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia dealbata", null, null, null, TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(),null, null,null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }

    @Test
    public void testMatch7() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setNomenclaturalStatus(NomenclaturalStatus.DOUBTFUL);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia dealbata", null, null, null, TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), Collections.singleton(NomenclaturalStatus.ALTERNATIVE), "alternative", null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }

    @Test
    public void testMatch8() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setNameType(NameType.SCIENTIFIC);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia dealbata", null, null,null, TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch9() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setNameType(NameType.SCIENTIFIC);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia unplaced", null, null,null, TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }

    @Test
    public void testMatch10() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setTaxonRank(RankType.SPECIES);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia dealbata", null, null,null, TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch11() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setTaxonRank(RankType.CLASS);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia dealbata", null, null, null, TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }

    @Test
    public void testMatch12() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setYear("1974");
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia dealbata", null, null, "1974", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch13() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setYear("1974");
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia dealbata", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }

    @Test
    public void testMatch14() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificName("Acacia dealbata");
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia dealbata", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch15() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificName(" Acacia dealbata ");
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia dealbata", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch16() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificName("Acacia other");
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia dealbata", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }

    @Test
    public void testMatch17() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificName("Acacia dealbata");
        condition.setMatchType(NameMatchType.INSENSITIVE);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia dealbata", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch18() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificName(" ACACIA    dealbata ");
        condition.setMatchType(NameMatchType.INSENSITIVE);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia dealbata", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch19() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificName(" ACACIA    dealbata ");
        condition.setMatchType(NameMatchType.INSENSITIVE);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, " ACACIA   Dealbata", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch20() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificName("Acacia other");
        condition.setMatchType(NameMatchType.INSENSITIVE);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia dealbata", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }

    @Test
    public void testMatch21() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificName("Mycofalcella calcarata");
        condition.setMatchType(NameMatchType.NORMALISED);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Mycofalcela calcarata", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch22() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificName("Aphanesia greyi");
        condition.setMatchType(NameMatchType.NORMALISED);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Aphanesia grei", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch23() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificName("Aphanesia greyi");
        condition.setMatchType(NameMatchType.NORMALISED);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, " Aphanesia  greyi  ", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch24() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificName("Aphanesia greyi");
        condition.setMatchType(NameMatchType.NORMALISED);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia dealbata", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }


    @Test
    public void testMatch25() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificName("Mycofalcel+a calca?rata");
        condition.setMatchType(NameMatchType.REGEX);
        TaxonConceptInstance instance1 = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Mycofalcella calcarata", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key1 = this.analyser.analyse(instance1);
        assertTrue(condition.match(instance1, key1));
        TaxonConceptInstance instance2 = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Mycofalcela calcrata", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key2 = this.analyser.analyse(instance2);
        assertTrue(condition.match(instance2, key2));
    }

    @Test
    public void testMatch26() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificName("Aphanesia gr[eyi]+");
        condition.setMatchType(NameMatchType.REGEX);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Aphanesia greyi", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch27() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificName("Aphanesia gr[eyi]+");
        condition.setMatchType(NameMatchType.REGEX);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia dealbata", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }


    @Test
    public void testMatch28() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificNameAuthorship("Benth.");
        condition.setMatchType(NameMatchType.EXACT);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Lepidosperma leptophyllum", "Benth.", null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch29() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificNameAuthorship("Benth.");
        condition.setMatchType(NameMatchType.EXACT);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Lepidosperma leptophyllum", " Benth.", null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch30() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificNameAuthorship("Benth.");
        condition.setMatchType(NameMatchType.EXACT);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Lepidosperma leptophyllum", "Bentham", null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }


    @Test
    public void testMatch31() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificNameAuthorship("Benth.");
        condition.setMatchType(NameMatchType.INSENSITIVE);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Lepidosperma leptophyllum", "Benth.", null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch32() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificNameAuthorship("Benth.");
        condition.setMatchType(NameMatchType.INSENSITIVE);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Lepidosperma leptophyllum", " BENTh.", null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch33() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificNameAuthorship("Benth.");
        condition.setMatchType(NameMatchType.INSENSITIVE);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Lepidosperma leptophyllum", "Bentham", null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }

    @Test
    public void testMatch34() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificNameAuthorship("Benth.");
        condition.setMatchType(NameMatchType.NORMALISED);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Lepidosperma leptophyllum", "Benth.", null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch35() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificNameAuthorship("Benth.");
        condition.setMatchType(NameMatchType.NORMALISED);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Lepidosperma leptophyllum", "Bentham", null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch36() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificNameAuthorship("Benth.");
        condition.setMatchType(NameMatchType.NORMALISED);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Lepidosperma leptophyllum", "L.", null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }

    @Test
    public void testMatch37() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificNameAuthorship("Benth\\.?");
        condition.setMatchType(NameMatchType.REGEX);
        TaxonConceptInstance instance1 = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Lepidosperma leptophyllum", "Benth.", null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key1 = this.analyser.analyse(instance1);
        assertTrue(condition.match(instance1, key1));
        TaxonConceptInstance instance2 = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Lepidosperma leptophyllum", "Benth", null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key2 = this.analyser.analyse(instance2);
        assertTrue(condition.match(instance2, key2));
    }

    @Test
    public void testMatch38() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificNameAuthorship("[Bb]enth(am|\\.)?");
        condition.setMatchType(NameMatchType.REGEX);
        TaxonConceptInstance instance1 = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Lepidosperma leptophyllum", "Benth", null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key1 = this.analyser.analyse(instance1);
        assertTrue(condition.match(instance1, key1));
        TaxonConceptInstance instance2 = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Lepidosperma leptophyllum", "bentham", null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key2 = this.analyser.analyse(instance2);
        assertTrue(condition.match(instance2, key2));
        TaxonConceptInstance instance3 = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Lepidosperma leptophyllum", "Benth.", null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key3 = this.analyser.analyse(instance3);
        assertTrue(condition.match(instance3, key3));
    }

    @Test
    public void testMatch39() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificNameAuthorship("[Bb]enth(am|\\.)?");
        condition.setMatchType(NameMatchType.REGEX);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Lepidosperma leptophyllum", "Benthos", null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }

    @Test
    public void testMatch40() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setDatasetID(this.provider.getId());
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Lepidosperma leptophyllum", "Benth.", null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch41() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setDatasetID("other");
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Lepidosperma leptophyllum", "Benth.", null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }


    @Test
    public void testMatch42() {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setScientificName("Unknown(\\s.*|)");
        condition.setMatchType(NameMatchType.REGEX);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "unknown", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
        instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Unknown sp.", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
        instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Unknownsp.", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }

    @Test
    public void testWrite1() throws Exception {
        MatchTaxonCondition condition = new MatchTaxonCondition();
        condition.setNomenclaturalCode(NomenclaturalCode.BOTANICAL);
        condition.setTaxonomicStatus(TaxonomicType.EXCLUDED);
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, condition);
        assertEquals(this.loadResource("match-condition-1.json"), writer.toString());

    }
    @Test
    public void testRead1() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        MatchTaxonCondition condition = mapper.readValue(this.resourceReader("match-condition-1.json"), MatchTaxonCondition.class);
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia dealbata", null, null, "1975", TaxonomicType.EXCLUDED, TaxonomicType.EXCLUDED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
        instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider, "Acacia dealbata", null, null, "1975", TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
        key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }


}
