package au.org.ala.names.index.provider;

import au.org.ala.names.index.NameProvider;
import au.org.ala.names.index.TaxonConceptInstance;
import au.org.ala.names.model.TaxonomicType;
import au.org.ala.names.util.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.gbif.api.vocabulary.NomenclaturalCode;
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

    @Before
    public void setup() {
        this.provider = new NameProvider();
    }

    @Test
    public void testMatch1() {
        OrTaxonCondition condition = new OrTaxonCondition();
        MatchTaxonCondition condition1 = new MatchTaxonCondition();
        condition1.setNomenclaturalCode(NomenclaturalCode.BOTANICAL);
        condition.add(condition1);
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia dealbata", this.provider, TaxonomicType.HOMOTYPIC_SYNONYM);
        assertTrue(condition.match(instance));
    }

    @Test
    public void testMatch2() {
        OrTaxonCondition condition = new OrTaxonCondition();
        MatchTaxonCondition condition1 = new MatchTaxonCondition();
        condition1.setNomenclaturalCode(NomenclaturalCode.ZOOLOGICAL);
        condition.add(condition1);
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia dealbata", this.provider, TaxonomicType.HOMOTYPIC_SYNONYM);
        assertFalse(condition.match(instance));
    }


    @Test
    public void testMatch3() {
        OrTaxonCondition condition = new OrTaxonCondition();
        MatchTaxonCondition condition1 = new MatchTaxonCondition();
        condition1.setNomenclaturalCode(NomenclaturalCode.BOTANICAL);
        MatchTaxonCondition condition2 = new MatchTaxonCondition();
        condition2.setTaxonomicStatus(TaxonomicType.HOMOTYPIC_SYNONYM);
        condition.add(condition1);
        condition.add(condition2);
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia dealbata", this.provider, TaxonomicType.HOMOTYPIC_SYNONYM);
        assertTrue(condition.match(instance));
    }

    @Test
    public void testMatch4() {
        OrTaxonCondition condition = new OrTaxonCondition();
        MatchTaxonCondition condition1 = new MatchTaxonCondition();
        condition1.setNomenclaturalCode(NomenclaturalCode.BOTANICAL);
        MatchTaxonCondition condition2 = new MatchTaxonCondition();
        condition2.setTaxonomicStatus(TaxonomicType.HOMOTYPIC_SYNONYM);
        condition.add(condition1);
        condition.add(condition2);
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Acacia dealbata", this.provider, TaxonomicType.HOMOTYPIC_SYNONYM);
        assertTrue(condition.match(instance));
    }

    @Test
    public void testMatch5() {
        OrTaxonCondition condition = new OrTaxonCondition();
        MatchTaxonCondition condition1 = new MatchTaxonCondition();
        condition1.setNomenclaturalCode(NomenclaturalCode.BOTANICAL);
        MatchTaxonCondition condition2 = new MatchTaxonCondition();
        condition2.setTaxonomicStatus(TaxonomicType.HOMOTYPIC_SYNONYM);
        condition.add(condition1);
        condition.add(condition2);
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia dealbata", this.provider, TaxonomicType.ACCEPTED);
        assertTrue(condition.match(instance));
    }

    @Test
    public void testMatch6() {
        OrTaxonCondition condition = new OrTaxonCondition();
        MatchTaxonCondition condition1 = new MatchTaxonCondition();
        condition1.setNomenclaturalCode(NomenclaturalCode.BOTANICAL);
        MatchTaxonCondition condition2 = new MatchTaxonCondition();
        condition2.setTaxonomicStatus(TaxonomicType.HOMOTYPIC_SYNONYM);
        condition.add(condition1);
        condition.add(condition2);
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Acacia dealbata", this.provider, TaxonomicType.ACCEPTED);
        assertFalse(condition.match(instance));
    }

    @Test
    public void testWrite1() throws Exception {
        OrTaxonCondition condition = new OrTaxonCondition();
        MatchTaxonCondition condition1 = new MatchTaxonCondition();
        condition1.setNomenclaturalCode(NomenclaturalCode.BOTANICAL);
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
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia dealbata", this.provider, TaxonomicType.HOMOTYPIC_SYNONYM);
        assertTrue(condition.match(instance));
        instance = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia dealbata", this.provider, TaxonomicType.ACCEPTED);
        assertTrue(condition.match(instance));
        instance = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Acacia dealbata", this.provider, TaxonomicType.ACCEPTED);
        assertFalse(condition.match(instance));
    }

}
