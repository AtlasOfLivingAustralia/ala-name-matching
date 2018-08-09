package au.org.ala.names.index;

import au.org.ala.names.util.TestUtils;
import org.gbif.dwc.terms.DwcTerm;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

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
    private Taxonomy taxonomy = null;

    @After
    public void cleanup() throws Exception {
        if (this.taxonomy != null) {
            this.taxonomy.close();
            this.taxonomy.clean();
        }
    }

    @Test
    public void testLub1() throws Exception {
        this.taxonomy = new Taxonomy();
        this.taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-3.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolveLinks();
        TaxonConceptInstance i1 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044711");
        TaxonConceptInstance i2 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044715");
        ALATaxonResolver resolver = new ALATaxonResolver(this.taxonomy);
        TaxonConceptInstance lub = resolver.lub(i1, i2);
        assertNotNull(lub);
        assertEquals("http://id.biodiversity.org.au/node/ausmoss/10044710", lub.getTaxonID());
    }

    @Test
    public void testLub2() throws Exception {
        this.taxonomy = new Taxonomy();
        this.taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-3.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolveLinks();
        TaxonConceptInstance i1 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044715");
        TaxonConceptInstance i2 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044716");
        ALATaxonResolver resolver = new ALATaxonResolver(this.taxonomy);
        TaxonConceptInstance lub = resolver.lub(i1, i2);
        assertNotNull(lub);
        assertEquals("http://id.biodiversity.org.au/node/ausmoss/10044715", lub.getTaxonID());
    }

    @Test
    public void testLub3() throws Exception {
        this.taxonomy = new Taxonomy();
        this.taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-3.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolveLinks();
        TaxonConceptInstance i1 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044711");
        TaxonConceptInstance i2 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044715");
        ALATaxonResolver resolver = new ALATaxonResolver(this.taxonomy);
        TaxonConceptInstance lub = resolver.lub(Arrays.asList(i1, i2));
        assertNotNull(lub);
        assertEquals("http://id.biodiversity.org.au/node/ausmoss/10044710", lub.getTaxonID());
    }

    @Test
    public void testLub4() throws Exception {
        this.taxonomy = new Taxonomy();
        this.taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-3.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolveLinks();
        TaxonConceptInstance i1 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044711");
        TaxonConceptInstance i2 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044715");
        TaxonConceptInstance i3 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044716");
        ALATaxonResolver resolver = new ALATaxonResolver(taxonomy);
        TaxonConceptInstance lub = resolver.lub(Arrays.asList(i1, i2, i3));
        assertNotNull(lub);
        assertEquals("http://id.biodiversity.org.au/node/ausmoss/10044710", lub.getTaxonID());
    }
    
    @Test
    public void testPrincipals1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-18.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolveLinks();
        TaxonConcept tc1 = this.taxonomy.getInstance("Accepted-1-1").getContainer();
        assertEquals(2, tc1.getInstances().size());
        TaxonConcept tc2 = this.taxonomy.getInstance("Accepted-1-2").getContainer();
        assertEquals(2, tc2.getInstances().size());
        ALATaxonResolver resolver = new ALATaxonResolver(taxonomy);
        List<TaxonConceptInstance> principals = resolver.principals(tc1, tc1.getInstances());
        assertNotNull(principals);
        assertEquals(1, principals.size());
        assertEquals("Accepted-1-1", principals.get(0).getTaxonID());
        principals = resolver.principals(tc2, tc2.getInstances());
        assertNotNull(principals);
        assertEquals(1, principals.size());
        assertEquals("Accepted-1-2", principals.get(0).getTaxonID());
    }

    @Test
    public void testPrincipals2() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-19.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolveLinks();
        TaxonConcept tc1 = this.taxonomy.getInstance("Accepted-1-1").getContainer();
        assertEquals(2, tc1.getInstances().size());
        TaxonConcept tc2 = this.taxonomy.getInstance("Synonym-1-2").getContainer();
        assertEquals(2, tc2.getInstances().size());
        ALATaxonResolver resolver = new ALATaxonResolver(taxonomy);
        List<TaxonConceptInstance> principals = resolver.principals(tc1, tc1.getInstances());
        assertNotNull(principals);
        assertEquals(1, principals.size());
        assertEquals("Accepted-1-1", principals.get(0).getTaxonID());
        principals = resolver.principals(tc2, tc2.getInstances());
        assertNotNull(principals);
        assertEquals(1, principals.size());
        assertEquals("Synonym-1-2", principals.get(0).getTaxonID());
    }

    @Test
    public void testPrincipals3() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-20.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolveLinks();
        TaxonConcept tc1 = this.taxonomy.getInstance("Falcata_APNI").getContainer();
        assertEquals(2, tc1.getInstances().size());
        TaxonConcept tc2 = this.taxonomy.getInstance("Furcata_APNI").getContainer();
        assertEquals(2, tc2.getInstances().size());
        ALATaxonResolver resolver = new ALATaxonResolver(taxonomy);
        List<TaxonConceptInstance> principals = resolver.principals(tc1, tc1.getInstances());
        assertNotNull(principals);
        assertEquals(1, principals.size());
        assertEquals("Falcata_NZOR", principals.get(0).getTaxonID());
        principals = resolver.principals(tc2, tc2.getInstances());
        assertNotNull(principals);
        assertEquals(1, principals.size());
        assertEquals("Furcata_NZOR", principals.get(0).getTaxonID());
    }

    @Test
    public void testResolution1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-18.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolveLinks();
        TaxonConceptInstance tci11 = this.taxonomy.getInstance("Accepted-1-1");
        TaxonConceptInstance tci12 = this.taxonomy.getInstance("Accepted-1-2");
        TaxonConceptInstance tci21 = this.taxonomy.getInstance("Accepted-2-1");
        TaxonConceptInstance tci22 = this.taxonomy.getInstance("Accepted-2-2");
        TaxonConcept tc1 = tci11.getContainer();
        assertEquals(2, tc1.getInstances().size());
        TaxonConcept tc2 = tci12.getContainer();
        assertEquals(2, tc2.getInstances().size());
        ALATaxonResolver resolver = new ALATaxonResolver(taxonomy);
        TaxonResolution resolution = resolver.resolve(tc1, resolver.principals(tc1, tc1.getInstances()), tc1.getInstances());
        assertEquals(1, resolution.getUsed().size());
        assertEquals(tci11, resolution.getResolved(tci11));
        assertEquals(tci11, resolution.getResolved(tci21));
        resolution = resolver.resolve(tc2, resolver.principals(tc2, tc2.getInstances()), tc2.getInstances());
        assertEquals(1, resolution.getUsed().size());
        assertEquals(tci12, resolution.getResolved(tci12));
        assertEquals(tci12, resolution.getResolved(tci22));
    }

    @Test
    public void testResolution2() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-19.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolveLinks();
        TaxonConceptInstance tci11 = this.taxonomy.getInstance("Accepted-1-1");
        TaxonConceptInstance tci12 = this.taxonomy.getInstance("Synonym-1-2");
        TaxonConceptInstance tci21 = this.taxonomy.getInstance("Accepted-2-1");
        TaxonConceptInstance tci22 = this.taxonomy.getInstance("Accepted-2-2");
        TaxonConcept tc1 = tci11.getContainer();
        assertEquals(2, tc1.getInstances().size());
        TaxonConcept tc2 = tci12.getContainer();
        assertEquals(2, tc2.getInstances().size());
        ALATaxonResolver resolver = new ALATaxonResolver(taxonomy);
        TaxonResolution resolution = resolver.resolve(tc1, resolver.principals(tc1, tc1.getInstances()), tc1.getInstances());
        assertEquals(1, resolution.getUsed().size());
        assertEquals(tci11, resolution.getResolved(tci11));
        assertEquals(tci11, resolution.getResolved(tci21));
        resolution = resolver.resolve(tc2, resolver.principals(tc2, tc2.getInstances()), tc2.getInstances());
        assertEquals(1, resolution.getUsed().size());
        assertEquals(tci12, resolution.getResolved(tci12));
        assertEquals(tci12, resolution.getResolved(tci22));
    }


    @Test
    public void testResolution3() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-20.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolveLinks();
        TaxonConceptInstance tci11 = this.taxonomy.getInstance("Falcata_APNI");
        TaxonConceptInstance tci12 = this.taxonomy.getInstance("Furcata_APNI");
        TaxonConceptInstance tci21 = this.taxonomy.getInstance("Falcata_NZOR");
        TaxonConceptInstance tci22 = this.taxonomy.getInstance("Furcata_NZOR");
        TaxonConcept tc1 = tci11.getContainer();
        assertEquals(2, tc1.getInstances().size());
        TaxonConcept tc2 = tci12.getContainer();
        assertEquals(2, tc2.getInstances().size());
        ALATaxonResolver resolver = new ALATaxonResolver(taxonomy);
        TaxonResolution resolution = resolver.resolve(tc1, resolver.principals(tc1, tc1.getInstances()), tc1.getInstances());
        assertEquals(1, resolution.getUsed().size());
        assertEquals(tci21, resolution.getResolved(tci11));
        assertEquals(tci21, resolution.getResolved(tci21));
        resolution = resolver.resolve(tc2, resolver.principals(tc2, tc2.getInstances()), tc2.getInstances());
        assertEquals(1, resolution.getUsed().size());
        assertEquals(tci22, resolution.getResolved(tci12));
        assertEquals(tci22, resolution.getResolved(tci22));
    }


}
