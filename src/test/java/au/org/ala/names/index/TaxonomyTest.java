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
        if (this.taxonomy != null) {
            this.taxonomy.close();
            this.taxonomy.clean();
        }
    }
    
    @Test
    public void testResolveLinks1() throws Exception {
        this.taxonomy = new Taxonomy();
        this.taxonomy.begin();
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
        this.taxonomy.begin();
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
        this.taxonomy.begin();
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
        this.taxonomy.begin();
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
        this.taxonomy.begin();
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

    // Test resolution to a preferred taxon
    @Test
    public void testResolveMultipe1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-7.csv"));
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance i11 = this.taxonomy.getInstance("NZOR-4-28207");
        TaxonConceptInstance i12 = this.taxonomy.getInstance("53095000");
        assertNotNull(i11);
        assertNotNull(i12);
        TaxonConcept tc1 = i11.getTaxonConcept();
        assertSame(i11, tc1.getRepresentative());
        assertSame(tc1, i12.getTaxonConcept());
        assertSame(i11, i12.getResolved());
        assertSame(i11, i12.getResolvedAccepted());
        assertNull(i11.getResolvedParent());
        assertNull(i12.getResolvedParent());
        // Accepted
        TaxonConceptInstance i21 = this.taxonomy.getInstance("53095000-1");
        assertNotNull(i21);
        assertSame(i21, i21.getResolved());
        assertSame(i11, i21.getResolvedAccepted());
        assertNull(i21.getResolvedParent());
        // Parent
        TaxonConceptInstance i31 = this.taxonomy.getInstance("53095002");
        assertNotNull(i31);
        assertSame(i31, i31.getResolved());
        assertSame(i31, i31.getResolvedAccepted());
        assertSame(i11, i31.getResolvedParent());
    }


    // Test resolution to a preferred taxon with key rewriting
    @Test
    public void testResolveMultipe2() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-9.csv"));
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance i11 = this.taxonomy.getInstance("NZOR-4-10536");
        TaxonConceptInstance i12 = this.taxonomy.getInstance("NZOR-4-34433");
        TaxonConceptInstance i13 = this.taxonomy.getInstance("CoL:29944185");
        assertNotNull(i11);
        assertNotNull(i12);
        assertNotNull(i13);
        TaxonConcept tc1 = i11.getTaxonConcept();
        TaxonConcept tc2 = i12.getTaxonConcept();
        assertSame(i11, tc1.getRepresentative());
        assertSame(i12, tc2.getRepresentative());
        assertSame(i12, i12.getResolved());
        assertSame(i12, i12.getResolvedAccepted());
        assertSame(tc2, i13.getTaxonConcept());
        assertSame(i12, i13.getResolved());
        assertSame(i12, i13.getResolvedAccepted());
    }

    // Test placement on an uncoded name
    @Test
    public void testPlaceUncoded1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-8.csv"));
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance i11 = this.taxonomy.getInstance("NZOR-4-118018");
        TaxonConceptInstance i12 = this.taxonomy.getInstance("urn:lsid:catalogueoflife.org:taxon:e5c9a6a6-e319-11e5-86e7-bc764e092680:col20161028");
        TaxonConceptInstance i13 = this.taxonomy.getInstance("ALA_Closteroviridae");
        assertNotNull(i11);
        assertNotNull(i12);
        assertNotNull(i13);
        TaxonConcept tc1 = i11.getTaxonConcept();
        assertSame(i11, tc1.getRepresentative());
        assertSame(tc1, i12.getTaxonConcept());
        assertSame(tc1, i13.getTaxonConcept());
        assertSame(i11, i11.getResolved());
        assertSame(i11, i11.getResolvedAccepted());
        assertSame(i11, i12.getResolved());
        assertSame(i11, i12.getResolvedAccepted());
        assertSame(i11, i13.getResolved());
        assertSame(i11, i13.getResolvedAccepted());
        assertNull(i11.getResolvedParent());
        assertNull(i12.getResolvedParent());
        assertNull(i13.getResolvedParent());
    }

    @Test
    public void testWrite() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-3.csv"));
        CSVNameSource source2 = new CSVNameSource(this.resourceReader("taxonomy-4.csv"));
        this.taxonomy.load(Arrays.asList(source1, source2));
        this.taxonomy.resolve();
        File dir = new File(this.taxonomy.getWork(), "output");
        dir.mkdir();
        this.taxonomy.createDwCA(dir);
        assertTrue(new File(dir, "meta.xml").exists());
        assertTrue(new File(dir, "eml.xml").exists());
        assertTrue(new File(dir, "taxon.txt").exists());
        assertTrue(new File(dir, "taxonvariant.txt").exists());
        assertTrue(new File(dir, "identifier.txt").exists());
        assertTrue(new File(dir, "rightsholder.txt").exists());

    }

}
