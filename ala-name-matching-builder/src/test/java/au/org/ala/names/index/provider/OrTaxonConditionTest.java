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
import au.org.ala.names.model.TaxonomicType;
import au.org.ala.names.util.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.*;

/**
 * Test cases for {@link OrTaxonCondition}
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class OrTaxonConditionTest extends TestUtils {
    private NameProvider provider;
    private ALANameAnalyser analyser;

    @Before
    public void setup() {
        this.provider = new NameProvider();
        this.analyser = new ALANameAnalyser();
    }

    @Test
    public void testMatch1() {
        OrTaxonCondition condition = new OrTaxonCondition();
        MatchTaxonCondition condition1 = new MatchTaxonCondition();
        condition1.setNomenclaturalCode(NomenclaturalClassifier.BOTANICAL);
        condition.add(condition1);
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalClassifier.BOTANICAL, "Acacia dealbata", this.provider, TaxonomicType.HOMOTYPIC_SYNONYM);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch2() {
        OrTaxonCondition condition = new OrTaxonCondition();
        MatchTaxonCondition condition1 = new MatchTaxonCondition();
        condition1.setNomenclaturalCode(NomenclaturalClassifier.ZOOLOGICAL);
        condition.add(condition1);
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalClassifier.BOTANICAL, "Acacia dealbata", this.provider, TaxonomicType.HOMOTYPIC_SYNONYM);
        NameKey key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }


    @Test
    public void testMatch3() {
        OrTaxonCondition condition = new OrTaxonCondition();
        MatchTaxonCondition condition1 = new MatchTaxonCondition();
        condition1.setNomenclaturalCode(NomenclaturalClassifier.BOTANICAL);
        MatchTaxonCondition condition2 = new MatchTaxonCondition();
        condition2.setTaxonomicStatus(TaxonomicType.HOMOTYPIC_SYNONYM);
        condition.add(condition1);
        condition.add(condition2);
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalClassifier.BOTANICAL, "Acacia dealbata", this.provider, TaxonomicType.HOMOTYPIC_SYNONYM);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch4() {
        OrTaxonCondition condition = new OrTaxonCondition();
        MatchTaxonCondition condition1 = new MatchTaxonCondition();
        condition1.setNomenclaturalCode(NomenclaturalClassifier.BOTANICAL);
        MatchTaxonCondition condition2 = new MatchTaxonCondition();
        condition2.setTaxonomicStatus(TaxonomicType.HOMOTYPIC_SYNONYM);
        condition.add(condition1);
        condition.add(condition2);
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalClassifier.ZOOLOGICAL, "Acacia dealbata", this.provider, TaxonomicType.HOMOTYPIC_SYNONYM);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch5() {
        OrTaxonCondition condition = new OrTaxonCondition();
        MatchTaxonCondition condition1 = new MatchTaxonCondition();
        condition1.setNomenclaturalCode(NomenclaturalClassifier.BOTANICAL);
        MatchTaxonCondition condition2 = new MatchTaxonCondition();
        condition2.setTaxonomicStatus(TaxonomicType.HOMOTYPIC_SYNONYM);
        condition.add(condition1);
        condition.add(condition2);
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalClassifier.BOTANICAL, "Acacia dealbata", this.provider, TaxonomicType.ACCEPTED);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
    }

    @Test
    public void testMatch6() {
        OrTaxonCondition condition = new OrTaxonCondition();
        MatchTaxonCondition condition1 = new MatchTaxonCondition();
        condition1.setNomenclaturalCode(NomenclaturalClassifier.BOTANICAL);
        MatchTaxonCondition condition2 = new MatchTaxonCondition();
        condition2.setTaxonomicStatus(TaxonomicType.HOMOTYPIC_SYNONYM);
        condition.add(condition1);
        condition.add(condition2);
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalClassifier.ZOOLOGICAL, "Acacia dealbata", this.provider, TaxonomicType.ACCEPTED);
        NameKey key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }

    @Test
    public void testWrite1() throws Exception {
        OrTaxonCondition condition = new OrTaxonCondition();
        MatchTaxonCondition condition1 = new MatchTaxonCondition();
        condition1.setNomenclaturalCode(NomenclaturalClassifier.BOTANICAL);
        MatchTaxonCondition condition2 = new MatchTaxonCondition();
        condition2.setTaxonomicStatus(TaxonomicType.HOMOTYPIC_SYNONYM);
        condition.add(condition1);
        condition.add(condition2);
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, condition);
        assertEquals(this.loadResource("or-condition-1.json"), writer.toString());

    }
    @Test
    public void testRead1() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        OrTaxonCondition condition = mapper.readValue(this.resourceReader("or-condition-1.json"), OrTaxonCondition.class);
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalClassifier.BOTANICAL, "Acacia dealbata", this.provider, TaxonomicType.HOMOTYPIC_SYNONYM);
        NameKey key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
        instance = this.createInstance("ID-1", NomenclaturalClassifier.BOTANICAL, "Acacia dealbata", this.provider, TaxonomicType.ACCEPTED);
        key = this.analyser.analyse(instance);
        assertTrue(condition.match(instance, key));
        instance = this.createInstance("ID-1", NomenclaturalClassifier.ZOOLOGICAL, "Acacia dealbata", this.provider, TaxonomicType.ACCEPTED);
        key = this.analyser.analyse(instance);
        assertFalse(condition.match(instance, key));
    }

}
