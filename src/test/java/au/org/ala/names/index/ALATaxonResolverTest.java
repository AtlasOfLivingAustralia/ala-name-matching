package au.org.ala.names.index;

import au.org.ala.names.util.TestUtils;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * File description.
 * <p>
 * More description.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class ALATaxonResolverTest extends TestUtils {

    @Test
    public void testLub1() throws Exception {
        Taxonomy taxonomy = new Taxonomy();
        taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-3.csv"));
        taxonomy.load(Arrays.asList(source));
        taxonomy.resolveLinks();
        TaxonConceptInstance i1 = taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044711");
        TaxonConceptInstance i2 = taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044715");
        ALATaxonResolver resolver = new ALATaxonResolver(taxonomy);
        TaxonConceptInstance lub = resolver.lub(i1, i2);
        assertNotNull(lub);
        assertEquals("http://id.biodiversity.org.au/node/ausmoss/10044710", lub.getTaxonID());
    }

    @Test
    public void testLub2() throws Exception {
        Taxonomy taxonomy = new Taxonomy();
        taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-3.csv"));
        taxonomy.load(Arrays.asList(source));
        taxonomy.resolveLinks();
        TaxonConceptInstance i1 = taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044715");
        TaxonConceptInstance i2 = taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044716");
        ALATaxonResolver resolver = new ALATaxonResolver(taxonomy);
        TaxonConceptInstance lub = resolver.lub(i1, i2);
        assertNotNull(lub);
        assertEquals("http://id.biodiversity.org.au/node/ausmoss/10044715", lub.getTaxonID());
    }

    @Test
    public void testLub3() throws Exception {
        Taxonomy taxonomy = new Taxonomy();
        taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-3.csv"));
        taxonomy.load(Arrays.asList(source));
        taxonomy.resolveLinks();
        TaxonConceptInstance i1 = taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044711");
        TaxonConceptInstance i2 = taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044715");
        ALATaxonResolver resolver = new ALATaxonResolver(taxonomy);
        TaxonConceptInstance lub = resolver.lub(Arrays.asList(i1, i2));
        assertNotNull(lub);
        assertEquals("http://id.biodiversity.org.au/node/ausmoss/10044710", lub.getTaxonID());
    }

    @Test
    public void testLub4() throws Exception {
        Taxonomy taxonomy = new Taxonomy();
        taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-3.csv"));
        taxonomy.load(Arrays.asList(source));
        taxonomy.resolveLinks();
        TaxonConceptInstance i1 = taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044711");
        TaxonConceptInstance i2 = taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044715");
        TaxonConceptInstance i3 = taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044716");
        ALATaxonResolver resolver = new ALATaxonResolver(taxonomy);
        TaxonConceptInstance lub = resolver.lub(Arrays.asList(i1, i2, i3));
        assertNotNull(lub);
        assertEquals("http://id.biodiversity.org.au/node/ausmoss/10044710", lub.getTaxonID());
    }

}
