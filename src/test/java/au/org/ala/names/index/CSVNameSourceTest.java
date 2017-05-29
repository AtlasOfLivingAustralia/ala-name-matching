package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import au.org.ala.names.util.TestUtils;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test cases for {@link CSVNameSource}
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class CSVNameSourceTest extends TestUtils {
    private Taxonomy taxonomy;

    @Before
    public void setup() {
        this.taxonomy = new Taxonomy();
    }

    @Test
    public void testValidate1() throws Exception {
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-1.csv"));
        source.validate();
    }

    @Test
    public void testValidate2() throws Exception {
        try {
            CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-bad-1.csv"));
            source.validate();
            fail("Expected IndexBuilderException");
        } catch (IndexBuilderException ex) {
        }
    }

    @Test
    public void testLoadIntoTaxonomy1() throws Exception {
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-1.csv"));
        source.loadIntoTaxonomy(this.taxonomy);
        TaxonConceptInstance instance = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044710");
        assertNotNull(instance);
        assertEquals("Bryidae", instance.getScientificName());
        assertEquals("Engl.", instance.getScientificNameAuthorship());
        assertNull(instance.getAcceptedNameUsageID());
        assertEquals("http://id.biodiversity.org.au/node/ausmoss/10044709", instance.getParentNameUsageID());
        assertEquals("http://id.biodiversity.org.au/node/ausmoss/10044710", instance.getTaxonID());
        assertEquals(NomenclaturalCode.BOTANICAL, instance.getCode());
        assertNotNull(instance.getProvider());
        assertEquals("default", instance.getProvider().getId());
        assertEquals(RankType.SUBCLASS, instance.getRank());
        assertNull(instance.getStatus());
        assertEquals(TaxonomicStatus.ACCEPTED, instance.getTaxonomicStatus());
        assertNull(instance.getSynonymType());
        assertNull(instance.getYear());
        TaxonConcept concept = instance.getTaxonConcept();
        assertNotNull(concept);
        ScientificName name = concept.getScientificName();
        assertNotNull(name);
        assertTrue(this.taxonomy.getNames().containsValue(name));
    }

}
