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

package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonomicType;
import au.org.ala.names.util.FileUtils;
import au.org.ala.names.util.TestUtils;
import au.org.ala.vocab.ALATerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Test cases for {@link Taxonomy}
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class TaxonomyTest extends TestUtils {
    private Taxonomy taxonomy = null;
    private File output = null;

    @After
    public void cleanup() throws Exception {
        if (this.taxonomy != null) {
            this.taxonomy.close();
            this.taxonomy.clean();
        }
        if (this.output != null && this.output.exists()) {
            FileUtils.clear(this.output, true);
        }
    }
    
    @Test
    public void testResolveLinks1() throws Exception {
        this.taxonomy = new Taxonomy();
        this.taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-1.csv"), DwcTerm.Taxon);
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
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-29.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolveLinks();
        TaxonConceptInstance i1 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/apni/10044707");
        assertNotNull(i1);
        TaxonConceptInstance i2 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044710");
        assertNotNull(i2);
        TaxonConceptInstance i3 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044711");
        assertNotNull(i3);
        TaxonConceptInstance i4 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044712");
        assertNotNull(i4);
        assertSame(i1.getContainer().getContainer().getContainer(), i2.getParent());
        assertSame(i2, i3.getParent());
        assertSame(i3, i4.getParent());
    }

    @Test
    public void testResolveLinks3() throws Exception {
        this.taxonomy = new Taxonomy();
        this.taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-2.csv"), DwcTerm.Taxon);
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
    public void testResolveLinks4() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-36.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolveLinks();
        TaxonConceptInstance i1 = this.taxonomy.getInstance("Kingdom-1-1");
        assertNotNull(i1);
        TaxonConceptInstance i2 = this.taxonomy.getInstance("Phylum-1-1");
        assertNotNull(i2);
        assertSame(i1, i2.getParent());
        TaxonConceptInstance i3 = this.taxonomy.getInstance("Class-1-1");
        assertNotNull(i3);
        assertSame(i2, i3.getParent());
        TaxonConceptInstance i4 = this.taxonomy.getInstance("Order-1-1");
        assertNotNull(i4);
        assertSame(i3, i4.getParent());
        TaxonomicElement i4r = this.taxonomy.findElement(null, i4.getScientificName(), i4.getProvider(), i4.getRank());
        assertNotNull(i4r);
        TaxonConceptInstance i5 = this.taxonomy.getInstance("Family-2-1");
        assertNotNull(i5);
        assertSame(i4r, i5.getParent());
        TaxonomicElement i5r = this.taxonomy.findElement(null, i5.getScientificName(), i5.getProvider(), i5.getRank());
        assertNotNull(i5r);
        TaxonConceptInstance i6 = this.taxonomy.getInstance("Genus-2-1");
        assertNotNull(i6);
        assertSame(i5r, i6.getParent());
        TaxonomicElement i6r = this.taxonomy.findElement(null, i6.getScientificName(), i6.getProvider(), i6.getRank());
        assertNotNull(i4r);
        TaxonConceptInstance i7 = this.taxonomy.getInstance("Species-2-1");
        assertNotNull(i7);
        assertSame(i5r, i7.getParent());
        TaxonConceptInstance i8 = this.taxonomy.getInstance("Species-3-1");
        assertNotNull(i8);
        assertSame(i5r, i8.getParent());
    }


    @Test
    public void testResolveTaxon1() throws Exception {
        this.taxonomy = new Taxonomy();
        this.taxonomy.begin();
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-1.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolveLinks();
        this.taxonomy.resolveTaxon();
        TaxonConceptInstance i1 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044710");
        assertNotNull(i1);
        assertEquals(NameProvider.DEFAULT_SCORE, i1.getScore());
        TaxonConcept tc1 = i1.getContainer();
        assertSame(i1, tc1.getRepresentative());
        TaxonConceptInstance i2 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044709");
        assertNotNull(i2);
        assertEquals(NameProvider.DEFAULT_SCORE, i2.getScore());
        TaxonConcept tc2 = i2.getContainer();
        assertSame(i2, tc2.getRepresentative());
    }

    @Test
    public void testResolveTaxon2() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-3.csv"), DwcTerm.Taxon);
        CSVNameSource source2 = new CSVNameSource(this.resourceReader("taxonomy-4.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1, source2));
        this.taxonomy.resolveLinks();
        this.taxonomy.resolveTaxon();
        TaxonConceptInstance i11 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044710");
        TaxonConceptInstance i12 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/xmoss/10044710");
        assertNotNull(i11);
        assertNotNull(i12);
        TaxonConcept tc1 = i11.getContainer();
        assertSame(i11, tc1.getRepresentative());
        assertNotSame(i12, tc1.getRepresentative());
        TaxonConceptInstance i21 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044709");
        TaxonConceptInstance i22 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/xmoss/10044709");
        assertNotNull(i21);
        assertNotNull(i21);
        TaxonConcept tc2 = i21.getContainer();
        assertSame(i21, tc2.getRepresentative());
        assertNotSame(i22, tc2.getRepresentative());
        TaxonConceptInstance i31 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044712");
        TaxonConceptInstance i32 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/xmoss/10044712");
        assertNotNull(i31);
        assertNotNull(i31);
        TaxonConcept tc3 = i31.getContainer();
        assertNotSame(i31, tc3.getRepresentative());
        assertSame(i32, tc3.getRepresentative());
    }

    // Test different spelling of authors
    @Test
    public void testResolveTaxon3() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-3.csv"), DwcTerm.Taxon);
        CSVNameSource source2 = new CSVNameSource(this.resourceReader("taxonomy-5.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1, source2));
        this.taxonomy.resolveLinks();
        this.taxonomy.resolveTaxon();
        TaxonConceptInstance i11 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044710");
        TaxonConceptInstance i12 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/xmoss/10044710");
        assertNotNull(i11);
        assertNotNull(i12);
        TaxonConcept tc1 = i11.getContainer();
        assertSame(i11, tc1.getRepresentative());
        assertNotSame(i12, tc1.getRepresentative());
        TaxonConceptInstance i21 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044709");
        TaxonConceptInstance i22 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/xmoss/10044709");
        assertNotNull(i21);
        assertNotNull(i21);
        TaxonConcept tc2 = i21.getContainer();
        assertSame(i21, tc2.getRepresentative());
        assertNotSame(i22, tc2.getRepresentative());
        TaxonConceptInstance i31 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044712");
        TaxonConceptInstance i32 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/xmoss/10044712");
        assertNotNull(i31);
        assertNotNull(i31);
        TaxonConcept tc3 = i31.getContainer();
        assertNotSame(i31, tc3.getRepresentative());
        assertSame(i32, tc3.getRepresentative());
    }

    // Test resolution to a preferred taxon
    @Test
    public void testResolveMultiple1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-7.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance i11 = this.taxonomy.getInstance("NZOR-4-28207");
        TaxonConceptInstance i12 = this.taxonomy.getInstance("53095000");
        assertNotNull(i11);
        assertNotNull(i12);
        TaxonConcept tc1 = i11.getContainer();
        assertSame(i11, tc1.getRepresentative());
        assertSame(tc1, i12.getContainer());
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
    public void testResolveMultiple2() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-9.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance i11 = this.taxonomy.getInstance("NZOR-4-10536");
        TaxonConceptInstance i12 = this.taxonomy.getInstance("NZOR-4-34433");
        TaxonConceptInstance i13 = this.taxonomy.getInstance("CoL:29944185");
        assertNotNull(i11);
        assertNotNull(i12);
        assertNotNull(i13);
        TaxonConcept tc1 = i11.getContainer();
        TaxonConcept tc2 = i12.getContainer();
        assertSame(i11, tc1.getRepresentative());
        assertSame(i12, tc2.getRepresentative());
        assertSame(i12, i12.getResolved());
        assertSame(i12, i12.getResolvedAccepted());
        assertSame(tc2, i13.getContainer());
        assertSame(i12, i13.getResolved());
        assertSame(i12, i13.getResolvedAccepted());
    }



    // Test resolution to a preferred taxon with an unranked instance.
    @Test
    public void testResolveUnranked1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-10.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance i0 = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/apni/7178434");
        TaxonConceptInstance i11 = this.taxonomy.getInstance("http://id.biodiversity.org.au/instance/apni/7178429");
        TaxonConceptInstance i12 = this.taxonomy.getInstance("http://id.biodiversity.org.au/instance/apni/7178430");
        TaxonConceptInstance i13 = this.taxonomy.getInstance("ALA_Astroloma_sp_Cataby_E_A_Griffin_1022");
        assertNotNull(i0);
        assertNotNull(i11);
        assertNotNull(i12);
        assertNotNull(i13);
        TaxonConcept tc1 = i11.getContainer();
        TaxonConcept tc2 = i12.getContainer();
        TaxonConcept tc3 = i13.getContainer();
        assertSame(tc1, tc3);
        assertSame(i11, tc1.getRepresentative());
        assertSame(i12, tc2.getRepresentative());
        assertSame(i11, i11.getResolved());
        assertSame(i0, i11.getResolvedAccepted());
        assertSame(i12, i12.getResolved());
        assertSame(i0, i12.getResolvedAccepted());
        assertSame(i11, i13.getResolved());
        assertSame(i0, i13.getResolvedAccepted());
    }

    // Test resolution to a preferred taxon for an unranked synonym
    @Test
    public void testResolveUnranked2() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-14.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance acc1 = this.taxonomy.getInstance("Accepted-1");
        TaxonConceptInstance acc2 = this.taxonomy.getInstance("Accepted-2");
        TaxonConceptInstance syn1 = this.taxonomy.getInstance("Synonym-1");
        assertNotNull(acc1);
        assertNotNull(acc2);
        assertNotNull(syn1);
        TaxonConcept tcAcc1 = acc1.getContainer();
        TaxonConcept tcAcc2 = acc2.getContainer();
        TaxonConcept tcSyn1 = syn1.getContainer();
        assertNotSame(tcAcc1, tcAcc2);
        assertSame(tcAcc2, tcSyn1);
    }

    // Test resolution to a preferred taxon for an unranked synonym
    @Test
    public void testResolveUnranked3() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-15.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance acc1 = this.taxonomy.getInstance("Accepted-1");
        TaxonConceptInstance acc2 = this.taxonomy.getInstance("Accepted-2");
        TaxonConceptInstance syn1 = this.taxonomy.getInstance("Synonym-1");
        TaxonConceptInstance syn2 = this.taxonomy.getInstance("Synonym-2");
        assertNotNull(acc1);
        assertNotNull(acc2);
        assertNotNull(syn1);
        assertNotNull(syn2);
        TaxonConcept tcAcc1 = acc1.getContainer();
        TaxonConcept tcAcc2 = acc2.getContainer();
        TaxonConcept tcSyn1 = syn1.getContainer();
        TaxonConcept tcSyn2 = syn2.getContainer();
        assertSame(tcAcc1, tcAcc2);
        assertSame(tcSyn1, tcSyn2);
    }

    // Test resolution to a preferred taxon for an unranked synonym
    @Test
    public void testResolveUnranked4() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-16.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance acc1 = this.taxonomy.getInstance("Accepted-1");
        TaxonConceptInstance acc2 = this.taxonomy.getInstance("Accepted-2");
        TaxonConceptInstance syn1 = this.taxonomy.getInstance("Synonym-1");
        TaxonConceptInstance syn2 = this.taxonomy.getInstance("Synonym-2");
        assertNotNull(acc1);
        assertNotNull(acc2);
        assertNotNull(syn1);
        assertNotNull(syn2);
        TaxonConcept tcAcc1 = acc1.getContainer();
        TaxonConcept tcAcc2 = acc2.getContainer();
        TaxonConcept tcSyn1 = syn1.getContainer();
        TaxonConcept tcSyn2 = syn2.getContainer();
        assertSame(tcAcc1, tcAcc2);
        assertSame(tcSyn1, tcSyn2);
    }


    // Test resolution to a preferred taxon for an unranked synonym
    @Test
    public void testResolveUnranked5() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-17.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance acc1 = this.taxonomy.getInstance("Accepted-1");
        TaxonConceptInstance acc2 = this.taxonomy.getInstance("Accepted-2");
        TaxonConceptInstance syn1 = this.taxonomy.getInstance("Synonym-1");
        TaxonConceptInstance syn2 = this.taxonomy.getInstance("Synonym-2");
        assertNotNull(acc1);
        assertNotNull(acc2);
        assertNotNull(syn1);
        assertNotNull(syn2);
        TaxonConcept tcAcc1 = acc1.getContainer();
        TaxonConcept tcAcc2 = acc2.getContainer();
        TaxonConcept tcSyn1 = syn1.getContainer();
        TaxonConcept tcSyn2 = syn2.getContainer();
        assertSame(tcAcc1, tcAcc2);
        assertSame(tcSyn1, tcSyn2);
        assertEquals(RankType.UNRANKED, syn1.getRank());
        assertEquals(RankType.UNRANKED, syn2.getRank());
        assertNull(syn1.getProvenance());
        assertNull(syn2.getProvenance());
    }

    // Test resolution to a preferred taxon for an unranked synonym which should stay unranked
    @Test
    public void testResolveUnranked6() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-24.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance acc1 = this.taxonomy.getInstance("Accepted-1");
        TaxonConceptInstance acc2 = this.taxonomy.getInstance("Accepted-2");
        TaxonConceptInstance syn1 = this.taxonomy.getInstance("Synonym-1");
        assertNotNull(acc1);
        assertNotNull(acc2);
        assertNotNull(syn1);
        TaxonConcept tcAcc1 = acc1.getContainer();
        TaxonConcept tcAcc2 = acc2.getContainer();
        TaxonConcept tcSyn1 = syn1.getContainer();
        assertSame(tcAcc1, tcAcc2);
        assertSame(acc1, syn1.getAccepted());
        assertEquals(RankType.SUBSPECIES, acc1.getRank());
        assertEquals(RankType.SUBSPECIES, acc2.getRank());
        assertEquals(RankType.UNRANKED, syn1.getRank());
        assertNull(syn1.getProvenance());
    }

    @Test
    public void testResolveUnranked7() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-25.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance acc1 = this.taxonomy.getInstance("Accepted-1");
        TaxonConceptInstance acc2 = this.taxonomy.getInstance("Accepted-2");
        TaxonConceptInstance syn1 = this.taxonomy.getInstance("Synonym-1");
        TaxonConceptInstance syn2 = this.taxonomy.getInstance("Synonym-2");
        assertNotNull(acc1);
        assertNotNull(acc2);
        assertNotNull(syn1);
        assertNotNull(syn2);
        TaxonConcept tcAcc1 = acc1.getContainer();
        TaxonConcept tcAcc2 = acc2.getContainer();
        TaxonConcept tcSyn1 = syn1.getContainer();
        TaxonConcept tcSyn2 = syn2.getContainer();
        assertSame(tcAcc1, tcAcc2);
        assertSame(tcSyn1, tcSyn2);
        assertEquals(RankType.VARIETY, syn1.getRank());
        assertEquals(RankType.UNRANKED, syn2.getRank());
        assertFalse(syn1.getProvenance() == null || syn1.getProvenance().isEmpty());
    }

    // Issue #126 Do not merge X y and X aff. y
    @Test
    public void testResolveUnranked8() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-32.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance acc1 = this.taxonomy.getInstance("Concept-1-1");
        TaxonConceptInstance acc2 = this.taxonomy.getInstance("Concept-2-1");
        TaxonConceptInstance acc3 = this.taxonomy.getInstance("Concept-3-1");
        assertNotNull(acc1);
        assertNotNull(acc2);
        assertNotNull(acc3);
        TaxonConcept tcAcc1 = acc1.getContainer();
        TaxonConcept tcAcc2 = acc2.getContainer();
        TaxonConcept tcAcc3 = acc3.getContainer();
        assertNotSame(tcAcc1, tcAcc2);
        assertNotSame(tcAcc1, tcAcc3);
        assertNotSame(tcAcc2, tcAcc3);
        ScientificName sn1 = tcAcc1.getContainer();
        ScientificName sn2 = tcAcc2.getContainer();
        ScientificName sn3 = tcAcc2.getContainer();
        assertNotSame(sn1, sn2);
        assertSame(sn2, sn3);
        UnrankedScientificName usn1 = sn1.getContainer();
        UnrankedScientificName usn2 = sn2.getContainer();
        assertNotSame(usn1, usn2);
        BareName bn1 = usn1.getContainer();
        BareName bn2 = usn2.getContainer();
        assertNotSame(bn1, bn2);
    }

    // Test placement on an uncoded name
    @Test
    public void testPlaceUncoded1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-8.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance i11 = this.taxonomy.getInstance("NZOR-4-118018");
        TaxonConceptInstance i12 = this.taxonomy.getInstance("urn:lsid:catalogueoflife.org:taxon:e5c9a6a6-e319-11e5-86e7-bc764e092680:col20161028");
        TaxonConceptInstance i13 = this.taxonomy.getInstance("ALA_Closteroviridae");
        assertNotNull(i11);
        assertNotNull(i12);
        assertNotNull(i13);
        TaxonConcept tc1 = i11.getContainer();
        assertSame(i11, tc1.getRepresentative());
        assertSame(tc1, i12.getContainer());
        assertSame(tc1, i13.getContainer());
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
    public void testWrite1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-3.csv"), DwcTerm.Taxon);
        CSVNameSource source2 = new CSVNameSource(this.resourceReader("taxonomy-4.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1, source2));
        this.taxonomy.resolve();
        File dir = new File(this.taxonomy.getWork(), "output");
        dir.mkdir();
        this.taxonomy.createDwCA(dir);
        assertTrue(new File(dir, "meta.xml").exists());
        assertTrue(new File(dir, "eml.xml").exists());
        assertEquals(12, this.rowCount(new File(dir, "taxon.txt")));
        assertEquals(22, this.rowCount(new File(dir, "taxonvariant.txt")));
        assertEquals(52, this.rowCount(new File(dir, "identifier.txt")));
        assertEquals(23, this.rowCount(new File(dir, "rightsholder.txt")));

    }


    @Test
    public void testWrite2() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-10.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        File dir = new File(this.taxonomy.getWork(), "output");
        dir.mkdir();
        this.taxonomy.createDwCA(dir);
        assertTrue(new File(dir, "meta.xml").exists());
        assertTrue(new File(dir, "eml.xml").exists());
        assertEquals(5, this.rowCount(new File(dir, "taxon.txt")));
        assertEquals(6, this.rowCount(new File(dir, "taxonvariant.txt")));
        assertEquals(6, this.rowCount(new File(dir, "identifier.txt")));
        assertEquals(24, this.rowCount(new File(dir, "rightsholder.txt")));
    }


    @Test
    public void testResolveUnplacedVernacular1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-5.csv"), DwcTerm.Taxon);
        CSVNameSource source2 = new CSVNameSource(this.resourceReader("vernacular-1.csv"), GbifTerm.VernacularName);
        this.taxonomy.load(Arrays.asList(source1, source2));
        this.taxonomy.resolve();
        this.taxonomy.createWorkingIndex();
        this.taxonomy.resolveUnplacedVernacular();
        File dir = new File(this.taxonomy.getWork(), "output");
        dir.mkdir();
        this.taxonomy.createDwCA(dir);
        assertTrue(new File(dir, "meta.xml").exists());
        assertTrue(new File(dir, "eml.xml").exists());
        assertEquals(12, this.rowCount(new File(dir, "taxon.txt")));
        assertEquals(2, this.rowCount(new File(dir, "vernacularname.txt")));
    }

    // Test disarding an identifier
    @Test
    public void testDiscardIdentifier1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-11.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance i11 = this.taxonomy.getInstance("7178434");
        TaxonConceptInstance i12 = this.taxonomy.getInstance("7178429");
        TaxonConceptInstance i13 = this.taxonomy.getInstance("7178430");
        assertNotNull(i11);
        assertNotNull(i12);
        assertNotNull(i13);
        assertFalse(i11.isForbidden());
        assertTrue(i12.isForbidden());
        assertFalse(i13.isForbidden());
        File dir = new File(this.taxonomy.getWork(), "output");
        dir.mkdir();
        this.taxonomy.createDwCA(dir);
        assertEquals(4, this.rowCount(new File(dir, "taxon.txt")));
        assertEquals(4, this.rowCount(new File(dir, "taxonvariant.txt")));
        File identifier = new File(dir, "identifier.txt");
        assertEquals(5, this.rowCount(identifier));
        this.dumpFile(identifier);
        assertTrue(this.fileContains(identifier, Pattern.compile("^7178434\t7178429.*discarded.*")));
    }

    // Test disarding an synonym
    @Test
    public void testDiscardSynonym1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-12.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance i11 = this.taxonomy.getInstance("7178434");
        TaxonConceptInstance i12 = this.taxonomy.getInstance("7178429");
        TaxonConceptInstance i13 = this.taxonomy.getInstance("7178430");
        assertNotNull(i11);
        assertNotNull(i12);
        assertNotNull(i13);
        assertFalse(i11.isForbidden());
        assertFalse(i12.isForbidden());
        assertFalse(i13.isForbidden());
        assertEquals(i11, i12.getResolvedAccepted());
        File dir = new File(this.taxonomy.getWork(), "output");
        dir.mkdir();
        this.taxonomy.createDwCA(dir);
        File taxon = new File(dir, "taxon.txt");
        assertEquals(5, this.rowCount(taxon));
        assertTrue(this.fileContains(taxon, Pattern.compile("^7178429\t\t7178434.*inferredSynonym.*")));
        assertEquals(5, this.rowCount(new File(dir, "taxonvariant.txt")));
        assertEquals(5, this.rowCount(new File(dir, "identifier.txt")));
    }

    // Test resolution to a preferred rank with key rewriting
    @Test
    public void testRankAdjust1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-13.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance i11 = this.taxonomy.getInstance("NZOR-4-118018");
        TaxonConceptInstance i12 = this.taxonomy.getInstance("urn:lsid:catalogueoflife.org:taxon:e5c9a6a6-e319-11e5-86e7-bc764e092680:col20161028");
        TaxonConceptInstance i13 = this.taxonomy.getInstance("ALA_Closteroviridae");
        assertNotNull(i11);
        assertNotNull(i12);
        assertNotNull(i13);
        TaxonConcept tc1 = i11.getContainer();
        TaxonConcept tc2 = i12.getContainer();
        TaxonConcept tc3 = i13.getContainer();
        assertSame(tc1, tc2);
        assertSame(tc1, tc3);
        assertEquals(RankType.FAMILY, tc3.getKey().getRank());
    }

    // Test placement with a disagreement about taxonomic status
    @Test
    public void testResolveDisagreement1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-20.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance fa_a = this.taxonomy.getInstance("Falcata_APNI");
        TaxonConceptInstance fa_n = this.taxonomy.getInstance("Falcata_NZOR");
        TaxonConceptInstance fa_o = this.taxonomy.getInstance("Falcata_Other");
        TaxonConceptInstance fu_a = this.taxonomy.getInstance("Furcata_APNI");
        TaxonConceptInstance fu_n = this.taxonomy.getInstance("Furcata_NZOR");
        TaxonConceptInstance fu_o = this.taxonomy.getInstance("Furcata_Other");
        assertNotNull(fa_a);
        assertNotNull(fa_n);
        assertNotNull(fa_o);
        assertNotNull(fu_a);
        assertNotNull(fu_n);
        assertNotNull(fu_o);
        TaxonConcept fa = fa_a.getContainer();
        assertEquals(fa, fa_a.getContainer());
        assertEquals(fa, fa_n.getContainer());
        assertEquals(fa, fa_o.getContainer());
        assertEquals(fa_n, fa.getRepresentative());
        assertEquals(fa_n, fa.getResolved(fa_a));
        assertEquals(fa_n, fa.getResolved(fa_n));
        assertEquals(fa_n, fa.getResolved(fa_o));
        TaxonConcept fu = fu_a.getContainer();
        assertEquals(fu, fu_a.getContainer());
        assertEquals(fu, fu_n.getContainer());
        assertEquals(fu, fu_o.getContainer());
        assertEquals(fu_n, fu.getRepresentative());
        assertEquals(fu_n, fu.getResolved(fu_a));
        assertEquals(fu_n, fu.getResolved(fu_n));
        assertEquals(fu_n, fu.getResolved(fu_o));
        File dir = new File(this.taxonomy.getWork(), "output");
        dir.mkdir();
        this.taxonomy.createDwCA(dir);
        assertTrue(new File(dir, "meta.xml").exists());
        assertTrue(new File(dir, "eml.xml").exists());
        assertEquals(6, this.rowCount(new File(dir, "taxon.txt")));
        assertEquals(11, this.rowCount(new File(dir, "taxonvariant.txt")));
        assertEquals(11, this.rowCount(new File(dir, "identifier.txt")));
        assertEquals(23, this.rowCount(new File(dir, "rightsholder.txt")));
    }

    // Test the presence of a taxon loop
    @Test
    public void testUnknownTaxon1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-21.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance tci1 = this.taxonomy.getInstance("Falcata");
        TaxonConceptInstance tci2 = this.taxonomy.getInstance("Furcata");
        TaxonConceptInstance tci3 = this.taxonomy.getInstance("Other");
        assertEquals(TaxonomicType.INFERRED_UNPLACED ,tci1.getTaxonomicStatus());
        assertNull(tci1.getAcceptedNameUsageID());
        assertNull(tci1.getAcceptedNameUsage());
        assertNull(tci1.getAccepted());
        assertNotNull(tci1.getProvenanceString());
        assertEquals(TaxonomicType.SYNONYM, tci2.getTaxonomicStatus());
        assertEquals(tci1.getTaxonID(), tci2.getAcceptedNameUsageID());
        assertSame(tci1, tci2.getAccepted());
        assertNull(tci2.getProvenanceString());
        assertEquals(TaxonomicType.SYNONYM, tci3.getTaxonomicStatus());
        assertEquals(tci1.getTaxonID(), tci3.getAcceptedNameUsageID());
        assertSame(tci1, tci3.getAccepted());
        assertNull(tci3.getProvenanceString());
      }

    // Test the presence of a taxon loop in s parent
    @Test
    public void testUnknownTaxon2() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-22.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance tci1 = this.taxonomy.getInstance("Pterostylis");
        TaxonConceptInstance tci2 = this.taxonomy.getInstance("Furcata");
        TaxonConceptInstance tci3 = this.taxonomy.getInstance("Furcata_furcata");
        TaxonConceptInstance unknown = taxonomy.getInstance(tci1.getProvider().getUnknownTaxonID());
        assertSame(unknown, tci1.getResolvedParent());
        assertNotNull(tci1.getProvenanceString());
        assertSame(tci1, tci2.getResolvedParent());
        assertSame(tci2, tci3.getResolvedParent());
    }

    // Test forbidden parents don't do to the unknown taxon
    @Test
    public void testUnknownTaxon3() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-23.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance tci1 = this.taxonomy.getInstance("ROOT");
        TaxonConceptInstance tci2 = this.taxonomy.getInstance("Plantae");
        TaxonConceptInstance tci3 = this.taxonomy.getInstance("Fungi");
        assertTrue(tci1.isForbidden());
        assertNull(tci1.getResolvedParent());
        assertNull(tci2.getResolvedParent());
        assertNull(tci3.getResolvedParent());
    }

    // Test misapplied taxa do not override accepted taxa
    @Test
    public void testMisappliedAccepted1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-27.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance tci1 = this.taxonomy.getInstance("Accepted-1");
        TaxonConceptInstance tci2 = this.taxonomy.getInstance("Accepted-2");
        TaxonConceptInstance tci3 = this.taxonomy.getInstance("Synonym-1");
        TaxonConcept tc1 = tci1.getContainer();
        TaxonConcept tc2 = tci2.getContainer();
        TaxonConcept tc3 = tci3.getContainer();
        assertSame(tc1, tc3);
        assertSame(tci2, tci3.getResolvedAccepted());
        assertSame(tci1, tci1.getResolvedAccepted());
    }

    // Test autonym handling
    @Test
    public void testAutonym1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-28.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance tci1 = this.taxonomy.getInstance("Autonym-1-1");
        TaxonConceptInstance tci2 = this.taxonomy.getInstance("Autonym-1-2");
        TaxonConcept tc1 = tci1.getContainer();
        TaxonConcept tc2 = tci2.getContainer();
        assertSame(tc1, tc2);
        assertSame(tci1, tc1.getResolved(tci1));
        assertSame(tci1, tc1.getResolved(tci2));
    }

    @Test
    public void testAutonym2() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-28.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance tci1 = this.taxonomy.getInstance("Autonym-2-1");
        TaxonConceptInstance tci2 = this.taxonomy.getInstance("Autonym-2-2");
        TaxonConceptInstance tci3 = this.taxonomy.getInstance("Autonym-2-3");
        TaxonConcept tc1 = tci1.getContainer();
        TaxonConcept tc2 = tci2.getContainer();
        TaxonConcept tc3 = tci3.getContainer();
        assertSame(tc1, tc2);
        assertSame(tc1, tc3);
        assertSame(tci1, tc1.getResolved(tci1));
        assertSame(tci1, tc1.getResolved(tci2));
        assertSame(tci1, tc1.getResolved(tci3));
    }

    @Test
    public void testConceptPriority1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-30.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance tci1 = this.taxonomy.getInstance("Concept-1-1");
        TaxonConceptInstance tci2 = this.taxonomy.getInstance("Concept-1-2");
        TaxonConcept tc1 = tci1.getContainer();
        TaxonConcept tc2 = tci2.getContainer();
        assertSame(tc1, tc2);
        assertSame(tci1, tc1.getResolved(tci1));
        assertSame(tci1, tc1.getResolved(tci2));
    }

    @Test
    public void testConceptPriority2() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-30.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance tci1 = this.taxonomy.getInstance("Concept-2-1");
        TaxonConceptInstance tci2 = this.taxonomy.getInstance("Concept-2-2");
        TaxonConcept tc1 = tci1.getContainer();
        TaxonConcept tc2 = tci2.getContainer();
        assertSame(tc1, tc2);
        assertSame(tci1, tc1.getResolved(tci1));
        assertSame(tci1, tc1.getResolved(tci2));
    }

    @Test
    public void testConceptPriority3() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-30.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance tci1 = this.taxonomy.getInstance("Concept-3-1");
        TaxonConceptInstance tci2 = this.taxonomy.getInstance("Concept-3-2");
        TaxonConceptInstance tci3 = this.taxonomy.getInstance("Concept-3-3");
        TaxonConcept tc1 = tci1.getContainer();
        TaxonConcept tc2 = tci2.getContainer();
        TaxonConcept tc3 = tci3.getContainer();
        assertNotSame(tc1, tc2);
        assertSame(tc2, tc3);
        assertSame(tci1, tc1.getResolved(tci1));
        assertSame(tci2, tc2.getResolved(tci2));
        assertSame(tci3, tc2.getResolved(tci3));
        assertSame(tci3, tc2.getRepresentative());
    }

    @Test
    public void testSharedAuthority1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-31.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance tci1 = this.taxonomy.getInstance("Concept-1-1");
        TaxonConceptInstance tci2 = this.taxonomy.getInstance("Concept-1-2");
        TaxonConceptInstance tci3 = this.taxonomy.getInstance("Concept-1-3");
        TaxonConceptInstance tci4 = this.taxonomy.getInstance("Concept-2-1");
        TaxonConceptInstance tci5 = this.taxonomy.getInstance("Concept-3-1");
        TaxonConcept tc1 = tci1.getContainer();
        TaxonConcept tc2 = tci2.getContainer();
        TaxonConcept tc3 = tci3.getContainer();
        TaxonConcept tc4 = tci4.getContainer();
        TaxonConcept tc5 = tci5.getContainer();
        assertSame(tc1, tc2);
        assertSame(tc2, tc3);
        assertSame(tci1, tc1.getResolved(tci1));
        assertSame(tci2, tc1.getResolved(tci2));
        assertSame(tci2, tc1.getResolved(tci3));
        assertSame(tci1, tc1.getRepresentative());
        assertSame(tci4, tc4.getResolved(tci4));
        assertSame(tci4, tc4.getRepresentative());
        assertSame(tci5, tc5.getResolved(tci5));
        assertSame(tci5, tc5.getRepresentative());
    }


    @Test
    public void testFuzzyCode1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-33.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance tci1 = this.taxonomy.getInstance("Concept-1-1");
        TaxonConceptInstance tci2 = this.taxonomy.getInstance("Concept-1-2");
        TaxonConcept tc1 = tci1.getContainer();
        TaxonConcept tc2 = tci2.getContainer();
        assertSame(tc1, tc2);
        assertSame(tci2, tc1.getRepresentative());
    }

    @Test
    public void testSyntheticTaxon1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-34.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance tci1 = this.taxonomy.getInstance("Concept-1-1");
        TaxonConceptInstance tci2 = this.taxonomy.getInstance("Concept-1-2");
        TaxonConcept tc1 = tci1.getContainer();
        TaxonConcept tc2 = tci2.getContainer();
        assertSame(tc1, tc2);
        assertSame(tci1, tc1.getRepresentative());
        assertFalse(tci2.isOutput());
    }

    @Test
    public void testSyntheticTaxon2() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-35.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance tci1 = this.taxonomy.getInstance("Concept-1-1");
        TaxonConceptInstance tci2 = this.taxonomy.getInstance("Concept-1-2");
        TaxonConceptInstance tci3 = this.taxonomy.getInstance("Parent-1-1");
        TaxonConceptInstance tci4 = this.taxonomy.getInstance("Parent-1-2");
        TaxonConcept tc1 = tci1.getContainer();
        TaxonConcept tc2 = tci2.getContainer();
        TaxonConcept tc3 = tci3.getContainer();
        TaxonConcept tc4 = tci4.getContainer();
        assertSame(tc1, tc2);
        assertNotSame(tc3, tc4);
        assertSame(tci1, tc1.getRepresentative());
        assertFalse(tci2.isOutput());
        assertFalse(tci4.isOutput());
    }

    // Issue 171
    @Test
    public void testSyntheticTaxon3() throws Exception {
        this.output = FileUtils.mkTempDir("merged", "", null);
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-40.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        this.taxonomy.createDwCA(this.output);
        File identifier = new File(this.output, "identifier.txt");
        assertEquals(13, this.rowCount(identifier));
    }

    @Test
    public void testParent1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("taxonomy-37.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        TaxonConceptInstance tci1 = this.taxonomy.getInstance("Species-1-1");
        TaxonConceptInstance tci2 = this.taxonomy.getInstance("Genus-1-1");
        TaxonConceptInstance tci3 = this.taxonomy.getInstance("Family-1-1");
        assertSame(tci2, tci1.getResolvedParent());
        assertSame(tci3, tci2.getResolvedParent());
        assertSame(taxonomy.getInferenceProvider().getUnknownTaxonID(), tci3.getResolvedParent().getTaxonID());
    }

    @Test
    public void testDistribution1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource locations1 = new CSVNameSource(this.resourceReader("locations/Location.csv"), ALATerm.Location);
        DwcaNameSource source1 = new DwcaNameSource(new File(this.getClass().getResource("dwca-2").getFile()));
        this.taxonomy.load(Arrays.asList(locations1, source1));
        this.taxonomy.resolve();
        TaxonConceptInstance tci1 = this.taxonomy.getInstance("https://biodiversity.org.au/afd/taxa/e9d6fbbd-1505-4073-990a-dc66c930dad6");
        assertNotNull(tci1.getDistribution());
        assertEquals(3, tci1.getDistribution().size());
        assertEquals("http://vocab.getty.edu/tgn/7001828", tci1.getDistribution().get(0).getLocation().getLocationID());
        assertEquals("AU", tci1.getDistribution().get(0).getAdditional().get(DwcTerm.countryCode));
    }

    @Test
    public void testDistribution2() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource locations1 = new CSVNameSource(this.resourceReader("locations/Location.csv"), ALATerm.Location);
        DwcaNameSource source1 = new DwcaNameSource(new File(this.getClass().getResource("dwca-2").getFile()));
        this.taxonomy.load(Arrays.asList(locations1, source1));
        this.taxonomy.resolve();
        this.output = FileUtils.mkTempDir("merged", "", null);
        this.taxonomy.createDwCA(this.output);
        File taxon = new File(this.output, "taxon.txt");
        assertTrue(taxon.exists());
        //this.dumpFile(taxon);
        assertEquals(19, this.rowCount(taxon));
        File distribution = new File(this.output, "distribution.txt");
        assertTrue(distribution.exists());
        //this.dumpFile(distribution);
        assertEquals(4, this.rowCount(distribution));
    }

    @Test
    public void testReferences1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        CSVNameSource locations1 = new CSVNameSource(this.resourceReader("locations/Location.csv"), ALATerm.Location);
        DwcaNameSource references1 = new DwcaNameSource(new File(this.getClass().getResource("references").getFile()));
        DwcaNameSource source1 = new DwcaNameSource(new File(this.getClass().getResource("dwca-2").getFile()));
        this.taxonomy.load(Arrays.asList(locations1, references1, source1));
        this.taxonomy.resolve();
        this.taxonomy.createWorkingIndex();
        this.taxonomy.resolveUnplacedReferences();
        this.output = FileUtils.mkTempDir("merged", "", null);
        this.taxonomy.createDwCA(this.output);
        File taxon = new File(this.output, "taxon.txt");
        assertTrue(taxon.exists());
        //this.dumpFile(taxon);
        assertEquals(19, this.rowCount(taxon));
        File reference = new File(this.output, "reference.txt");
        assertTrue(reference.exists());
        //this.dumpFile(reference);
        assertEquals(3, this.rowCount(reference));
    }

    @Test
    public void testPreferredVernacular1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        DwcaNameSource source1 = new DwcaNameSource(new File(this.getClass().getResource("dwca-3").getFile()));
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        this.taxonomy.buildPreferredVernacular();
        TaxonConceptInstance tci1 = this.taxonomy.getInstance("https://biodiversity.org.au/afd/taxa/340484bd-33f6-4b46-a63c-751f0b159ed1");
        assertNotNull(tci1);
        TaxonConcept tc1 = tci1.getContainer();
        assertNotNull(tc1);
        VernacularName vn1 = tc1.getPreferred();
        assertNotNull(vn1);
        assertEquals("Orange Roughy", vn1.getVernacularName());
        assertEquals("name:0003", vn1.getNameID());
        assertEquals(300, vn1.getScore().intValue());
        TaxonConceptInstance tci2 = this.taxonomy.getInstance("https://biodiversity.org.au/afd/taxa/23f4784e-1116-482c-8e3e-b6b12733f588");
        assertNotNull(tci2);
        TaxonConcept tc2 = tci2.getContainer();
        assertNotNull(tc2);
        VernacularName vn2 = tc2.getPreferred();
        assertNotNull(vn2);
        assertEquals("Eastern Grey Kangaroo", vn2.getVernacularName());
        assertEquals("name:0005", vn2.getNameID());
        assertEquals(150, vn2.getScore().intValue());
     }


    @Test
    public void testPreferredVernacular2() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        DwcaNameSource source1 = new DwcaNameSource(new File(this.getClass().getResource("dwca-3").getFile()));
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        this.taxonomy.buildPreferredVernacular();
        this.output = FileUtils.mkTempDir("merged", "", null);
        this.taxonomy.createDwCA(this.output);
        File taxon = new File(this.output, "taxon.txt");
        assertTrue(taxon.exists());
        //this.dumpFile(taxon);
        assertEquals(4, this.rowCount(taxon));
        File vernacular = new File(this.output, "vernacularname.txt");
        assertTrue(vernacular.exists());
        // this.dumpFile(vernacular);
        assertEquals(6, this.rowCount(vernacular));
    }


    @Test
    public void testPreferredVernacular3() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        DwcaNameSource locations1 = new DwcaNameSource(new File(this.getClass().getResource("locations").getFile()));
        CSVNameSource vernacular1 = new CSVNameSource(this.resourceReader("vernacular-2.csv"), GbifTerm.VernacularName);
        DwcaNameSource source1 = new DwcaNameSource(new File(this.getClass().getResource("dwca-3").getFile()));
        this.taxonomy.load(Arrays.asList(locations1, vernacular1, source1));
        this.taxonomy.resolve();
        this.taxonomy.createWorkingIndex();
        this.taxonomy.resolveUnplacedVernacular();
        this.taxonomy.buildPreferredVernacular();
        this.output = FileUtils.mkTempDir("merged", "", null);
        this.taxonomy.createDwCA(this.output);
        File taxon = new File(this.output, "taxon.txt");
        assertTrue(taxon.exists());
        //this.dumpFile(taxon);
        assertEquals(4, this.rowCount(taxon));
        File vernacular = new File(this.output, "vernacularname.txt");
        assertTrue(vernacular.exists());
        // this.dumpFile(vernacular);
        assertEquals(8, this.rowCount(vernacular));
    }
    @Test
    public void testPreferredVernacular4() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        this.taxonomy = new Taxonomy(config, null);
        this.taxonomy.begin();
        DwcaNameSource source1 = new DwcaNameSource(new File(this.getClass().getResource("dwca-3").getFile()));
        this.taxonomy.load(Arrays.asList(source1));
        this.taxonomy.resolve();
        this.taxonomy.createWorkingIndex();
        this.taxonomy.resolveUnplacedVernacular();
        this.taxonomy.buildPreferredVernacular();
        this.output = FileUtils.mkTempDir("merged", "", null);
        this.taxonomy.createDwCA(this.output);
        File taxon = new File(this.output, "taxon.txt");
        assertTrue(taxon.exists());
        //this.dumpFile(taxon);
        assertEquals(4, this.rowCount(taxon));
        File vernacular = new File(this.output, "vernacularname.txt");
        assertTrue(vernacular.exists());
        // this.dumpFile(vernacular);
        assertEquals(6, this.rowCount(vernacular));
        TaxonConceptInstance tci1 = this.taxonomy.getInstance("https://biodiversity.org.au/afd/taxa/23f4784e-1116-482c-8e3e-b6b12733f588");
        assertNotNull(tci1);
        assertNotNull(tci1.getVernacularNames());
        assertEquals(3, tci1.getVernacularNames().size());
        Optional<VernacularName> vn1 = tci1.getVernacularNames().stream().filter(v -> v.isForbidden()).findFirst();
        assertTrue(vn1.isPresent());
        assertEquals("name:0007", vn1.get().getNameID());
    }


}
