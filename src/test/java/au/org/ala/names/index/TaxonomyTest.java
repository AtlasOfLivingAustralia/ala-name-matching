package au.org.ala.names.index;

import au.org.ala.names.util.TestUtils;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Test cases for {@link Taxonomy}
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class TaxonomyTest extends TestUtils {
    private Taxonomy taxonomy = null;

    @After
    public void cleanup() throws Exception {
        if (this.taxonomy != null)
            this.taxonomy.clean();
    }
    
    @Test
    public void testResolveLinks1() throws Exception {
        this.taxonomy = new Taxonomy();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-1.csv"));
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolveLinks();
        TaxonConceptInstance i1 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044710");
        assertNotNull(i1);
        TaxonConceptInstance i2 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044709");
        assertNotNull(i2);
        assertEquals(i2.getTaxonID(), i1.getParentNameUsageID());
        assertSame(i2, i1.getParent());
        TaxonConceptInstance i3 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044708");
        assertNotNull(i3);
        assertEquals(i3.getTaxonID(), i2.getParentNameUsageID());
        assertSame(i3, i2.getParent());
        TaxonConceptInstance i4 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044707");
        assertNotNull(i4);
        assertEquals(i4.getTaxonID(), i3.getParentNameUsageID());
        assertSame(i4, i3.getParent());
    }

    @Test
    public void testResolveLinks2() throws Exception {
        this.taxonomy = new Taxonomy();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-2.csv"));
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolveLinks();
        TaxonConceptInstance i1 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044710");
        assertNotNull(i1);
        TaxonConceptInstance i2 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044709");
        assertNotNull(i2);
        assertSame(i2, i1.getParent());
        TaxonConceptInstance i3 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044708");
        assertNotNull(i3);
        TaxonConceptInstance i4 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044707");
        assertNotNull(i4);
        assertSame(i4, i2.getParent()); // No phylum in CSV
        assertSame(i4, i3.getParent());
    }


    @Test
    public void testResolveTaxon1() throws Exception {
        this.taxonomy = new Taxonomy();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-1.csv"));
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolveLinks();
        this.taxonomy.resolveTaxon();
        TaxonConceptInstance i1 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044710");
        assertNotNull(i1);
        assertEquals(NameProvider.DEFAULT_SCORE, i1.getScore());
        TaxonConcept tc1 = i1.getTaxonConcept();
        assertSame(i1, tc1.getRepresentative());
        TaxonConceptInstance i2 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044709");
        assertNotNull(i2);
        assertEquals(NameProvider.DEFAULT_SCORE, i2.getScore());
        TaxonConcept tc2 = i2.getTaxonConcept();
        assertSame(i2, tc2.getRepresentative());
    }

    @Test
    public void testResolveTaxon2() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-3.csv"));
        CSVNameSource source2 = new CSVNameSource(this.resourceReader("taxonomy-4.csv"));
        this.taxonomy.load(Arrays.asList(source1, source2));
        this.taxonomy.resolveLinks();
        this.taxonomy.resolveTaxon();
        TaxonConceptInstance i11 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044710");
        TaxonConceptInstance i12 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/xmoss/10044710");
        assertNotNull(i11);
        assertNotNull(i12);
        TaxonConcept tc1 = i11.getTaxonConcept();
        assertSame(i11, tc1.getRepresentative());
        assertNotSame(i12, tc1.getRepresentative());
        TaxonConceptInstance i21 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044709");
        TaxonConceptInstance i22 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/xmoss/10044709");
        assertNotNull(i21);
        assertNotNull(i21);
        TaxonConcept tc2 = i21.getTaxonConcept();
        assertSame(i21, tc2.getRepresentative());
        assertNotSame(i22, tc2.getRepresentative());
        TaxonConceptInstance i31 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044712");
        TaxonConceptInstance i32 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/xmoss/10044712");
        assertNotNull(i31);
        assertNotNull(i31);
        TaxonConcept tc3 = i31.getTaxonConcept();
        assertNotSame(i31, tc3.getRepresentative());
        assertSame(i32, tc3.getRepresentative());
    }

    // Test different spelling of authors
    @Test
    public void testResolveTaxon3() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-3.csv"));
        CSVNameSource source2 = new CSVNameSource(this.resourceReader("taxonomy-5.csv"));
        this.taxonomy.load(Arrays.asList(source1, source2));
        this.taxonomy.resolveLinks();
        this.taxonomy.resolveTaxon();
        TaxonConceptInstance i11 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044710");
        TaxonConceptInstance i12 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/xmoss/10044710");
        assertNotNull(i11);
        assertNotNull(i12);
        TaxonConcept tc1 = i11.getTaxonConcept();
        assertSame(i11, tc1.getRepresentative());
        assertNotSame(i12, tc1.getRepresentative());
        TaxonConceptInstance i21 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044709");
        TaxonConceptInstance i22 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/xmoss/10044709");
        assertNotNull(i21);
        assertNotNull(i21);
        TaxonConcept tc2 = i21.getTaxonConcept();
        assertSame(i21, tc2.getRepresentative());
        assertNotSame(i22, tc2.getRepresentative());
        TaxonConceptInstance i31 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044712");
        TaxonConceptInstance i32 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/xmoss/10044712");
        assertNotNull(i31);
        assertNotNull(i31);
        TaxonConcept tc3 = i31.getTaxonConcept();
        assertNotSame(i31, tc3.getRepresentative());
        assertSame(i32, tc3.getRepresentative());
    }

    @Test
    public void testLub1() throws Exception {
        this.taxonomy = new Taxonomy();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-3.csv"));
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolve();
        TaxonConceptInstance i1 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044711");
        TaxonConceptInstance i2 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044715");
        TaxonConceptInstance lub = this.taxonomy.lub(i1, i2);
        assertNotNull(lub);
        assertEquals("http://id.biodiversity.org.au/node/ausmoss/10044710", lub.getTaxonID());
    }

    @Test
    public void testLub2() throws Exception {
        this.taxonomy = new Taxonomy();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-3.csv"));
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolve();
        TaxonConceptInstance i1 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044715");
        TaxonConceptInstance i2 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044716");
        TaxonConceptInstance lub = this.taxonomy.lub(i1, i2);
        assertNotNull(lub);
        assertEquals("http://id.biodiversity.org.au/node/ausmoss/10044715", lub.getTaxonID());
    }

    @Test
    public void testLub3() throws Exception {
        this.taxonomy = new Taxonomy();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-3.csv"));
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolve();
        TaxonConceptInstance i1 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044711");
        TaxonConceptInstance i2 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044715");
        TaxonConceptInstance lub = this.taxonomy.lub(Arrays.asList(i1, i2));
        assertNotNull(lub);
        assertEquals("http://id.biodiversity.org.au/node/ausmoss/10044710", lub.getTaxonID());
    }

    @Test
    public void testLub4() throws Exception {
        this.taxonomy = new Taxonomy();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-3.csv"));
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolve();
        TaxonConceptInstance i1 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044711");
        TaxonConceptInstance i2 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044715");
        TaxonConceptInstance i3 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044716");
        TaxonConceptInstance lub = this.taxonomy.lub(Arrays.asList(i1, i2, i3));
        assertNotNull(lub);
        assertEquals("http://id.biodiversity.org.au/node/ausmoss/10044710", lub.getTaxonID());
    }

    @Test
    public void testWrite() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-3.csv"));
        CSVNameSource source2 = new CSVNameSource(this.resourceReader("taxonomy-4.csv"));
        this.taxonomy.load(Arrays.asList(source1, source2));
        this.taxonomy.resolve();
        File dir = new File(this.taxonomy.getWork(), "output");
        dir.mkdir();
        this.taxonomy.createDwCA(dir);
        assertTrue(new File(dir, "meta.xml").exists());
        assertTrue(new File(dir, "taxon.txt").exists());
        assertTrue(new File(dir, "taxonvariant.txt").exists());
        assertTrue(new File(dir, "identifier.txt").exists());

    }

}
