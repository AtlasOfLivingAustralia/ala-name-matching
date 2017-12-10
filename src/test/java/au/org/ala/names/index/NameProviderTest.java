package au.org.ala.names.index;

import au.org.ala.names.model.TaxonomicType;
import au.org.ala.names.util.TestUtils;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Test cases for {@link NameProvider}
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class NameProviderTest extends TestUtils {
    private NameProvider[] providers;

    @Before
    public void setup() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        this.providers = mapper.readValue(this.resourceReader("name-provider-1.json"), NameProvider[].class);
    }

    private NameProvider getProvider(String id) {
        for (NameProvider p: this.providers)
            if (p.getId().equals(id))
                return p;
        throw new IllegalArgumentException("Can't find provider " + id);
    }

    @Test
    public void testDefault1() {
        NameProvider provider = this.getProvider("ID-1");
        assertEquals(200, provider.getDefaultScore());
    }

    @Test
    public void testDefault2() {
        NameProvider provider = this.getProvider("ID-2");
        assertEquals(200, provider.getDefaultScore());
    }

    @Test
    public void testSpecificScore1() {
        NameProvider provider = this.getProvider("ID-3");
        assertNull(provider.getSpecificScore("Acacia dealbata"));
        assertEquals(150, (int) provider.getSpecificScore("Acacia"));
        assertEquals(90, (int) provider.getSpecificScore("Macropus"));
    }

    @Test
    public void testSpecificScore2() {
        NameProvider provider = this.getProvider("ID-4");
        assertNull(provider.getSpecificScore("Acacia dealbata"));
        assertEquals(100, (int) provider.getSpecificScore("Acacia"));
        assertEquals(90, (int) provider.getSpecificScore("Macropus"));
    }

    @Test
    public void testBaseScore1() {
        NameProvider provider = this.getProvider("ID-3");
        TaxonConceptInstance instance1 = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia dealbata", provider);
        TaxonConceptInstance instance2 = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia", provider);
        TaxonConceptInstance instance3 = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Macropus", provider);
        assertEquals(100, provider.computeBaseScore(instance1, instance1));
        assertEquals(150, provider.computeBaseScore(instance2, instance2));
        assertEquals(90, provider.computeBaseScore(instance3, instance3));
    }

    @Test
    public void testBaseScore2() {
        NameProvider provider = this.getProvider("ID-4");
        TaxonConceptInstance instance1 = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia dealbata", provider);
        TaxonConceptInstance instance2 = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia", provider);
        TaxonConceptInstance instance3 = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Macropus", provider);
        assertEquals(100, provider.computeBaseScore(instance1, instance1));
        assertEquals(100, provider.computeBaseScore(instance2, instance2));
        assertEquals(90, provider.computeBaseScore(instance3, instance3));
    }

    @Test
    public void testScore1() {
        NameProvider provider = this.getProvider("ID-5");
        TaxonConceptInstance instance1 = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia dealbata", provider);
        TaxonConceptInstance instance2 = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia", provider);
        TaxonConceptInstance instance3 = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Macropus", provider);
        TaxonConceptInstance instance4 = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Macropus", provider, TaxonomicType.EXCLUDED);
        assertEquals(110, provider.computeScore(instance1));
        assertEquals(160, provider.computeScore(instance2));
        assertEquals(90, provider.computeScore(instance3));
        assertEquals(70, provider.computeScore(instance4));
    }


    @Test
    public void testScore2() {
        NameProvider provider = this.getProvider("ID-6");
        TaxonConceptInstance instance1 = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia dealbata", provider);
        TaxonConceptInstance instance2 = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia", provider);
        TaxonConceptInstance instance3 = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Macropus", provider, TaxonomicType.INFERRED_ACCEPTED);
        TaxonConceptInstance instance4 = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Macropus", provider, TaxonomicType.EXCLUDED);
        assertEquals(110, provider.computeScore(instance1));
        assertEquals(160, provider.computeScore(instance2));
        assertEquals(70, provider.computeScore(instance3));
        assertEquals(70, provider.computeScore(instance4));
    }

    @Test
    public void testForbid1() {
        NameProvider provider = this.getProvider("ID-5");
        TaxonConceptInstance instance1 = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia dealbata", provider);
        TaxonConceptInstance instance2 = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia", provider);
        TaxonConceptInstance instance3 = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Macropus", provider);
        TaxonConceptInstance instance4 = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Macropus", provider, TaxonomicType.INCERTAE_SEDIS);
        assertFalse(provider.forbid(instance1));
        assertFalse(provider.forbid(instance2));
        assertFalse(provider.forbid(instance3));
        assertTrue(provider.forbid(instance4));

    }

    @Test
    public void testForbid2() {
        NameProvider provider = this.getProvider("ID-6");
        TaxonConceptInstance instance1 = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia dealbata", provider);
        TaxonConceptInstance instance2 = this.createInstance("ID-1", NomenclaturalCode.BOTANICAL, "Acacia", provider, TaxonomicType.EXCLUDED);
        TaxonConceptInstance instance3 = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Macropus", provider);
        TaxonConceptInstance instance4 = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Macropus", provider, TaxonomicType.INCERTAE_SEDIS);
        assertFalse(provider.forbid(instance1));
        assertTrue(provider.forbid(instance2));
        assertFalse(provider.forbid(instance3));
        assertTrue(provider.forbid(instance4));

    }

}
