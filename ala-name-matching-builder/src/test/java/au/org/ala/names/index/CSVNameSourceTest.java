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
import au.org.ala.names.util.TestUtils;
import au.org.ala.vocab.ALATerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        this.taxonomy.begin();
    }

    @After
    public void cleanup() throws Exception {
        this.taxonomy.close();
        this.taxonomy.clean();
    }

    @Test
    public void testValidate1() throws Exception {
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-1.csv"), DwcTerm.Taxon);
        source.validate();
    }

    @Test
    public void testValidate2() throws Exception {
        try {
            CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-bad-1.csv"), DwcTerm.Taxon);
            source.validate();
            fail("Expected IndexBuilderException");
        } catch (IndexBuilderException ex) {
        }
    }

    @Test
    public void testValidate3() throws Exception {
        CSVNameSource source = new CSVNameSource(this.resourceReader("locations/Location.csv"), ALATerm.Location);
        source.validate();
    }

    @Test
    public void testLoadLocations1() throws Exception {
        CSVNameSource source = new CSVNameSource(this.resourceReader("locations/Location.csv"), ALATerm.Location);
        this.taxonomy.load(Arrays.asList(source));
        this.taxonomy.resolveLocations();
        Location location1 = this.taxonomy.resolveLocation("http://vocab.getty.edu/tgn/7000490");
        assertNotNull(location1);
        assertEquals("Australia", location1.getLocality());
        assertEquals("country", location1.getGeographyType());
        assertEquals("http://vocab.getty.edu/tgn/1000006", location1.getParentLocationID());
        Location parent1 = location1.getParent();
        assertNotNull(parent1);
        assertEquals("http://vocab.getty.edu/tgn/1000006", parent1.getLocationID());
        assertEquals("Oceania", parent1.getLocality());
        Location location2 = this.taxonomy.resolveLocation("http://vocab.getty.edu/tgn/7002758");
        assertEquals("Andalusia", location2.getLocality());
        assertEquals("stateProvince", location2.getGeographyType());
        assertEquals("http://vocab.getty.edu/tgn/1000095", location2.getParentLocationID());
    }


    @Test
    public void testLoadReferences1() throws Exception {
        CSVNameSource source1 = new CSVNameSource(this.resourceReader("references/reference.csv"), GbifTerm.Reference);
        CSVNameSource source2 = new CSVNameSource(this.resourceReader("dwca-2/taxon.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source1, source2));
    }

    @Test
    public void testLoadIntoTaxonomy1() throws Exception {
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-1.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source));
        TaxonConceptInstance instance = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044710");
        assertNotNull(instance);
        assertEquals("Bryidae", instance.getScientificName());
        assertEquals("Engl.", instance.getScientificNameAuthorship());
        assertNull(instance.getAcceptedNameUsageID());
        assertEquals("http://id.biodiversity.org.au/node/ausmoss/10044709", instance.getParentNameUsageID());
        assertEquals("http://id.biodiversity.org.au/node/ausmoss/10044710", instance.getTaxonID());
        assertEquals(NomenclaturalClassifier.BOTANICAL, instance.getCode());
        assertNotNull(instance.getProvider());
        assertEquals("dr100", instance.getProvider().getId());
        assertEquals(RankType.SUBCLASS, instance.getRank());
        assertNull(instance.getStatus());
        assertEquals(TaxonomicType.ACCEPTED, instance.getTaxonomicStatus());
        assertNull(instance.getYear());
        Map<Term, Optional<String>> classification = instance.getClassification();
        assertNotNull(classification);
        assertEquals("Plantae", classification.get(DwcTerm.kingdom).get());
        assertEquals("Equisetopsida", classification.get(DwcTerm.class_).get());
        TaxonConcept concept = instance.getContainer();
        assertNotNull(concept);
        ScientificName name = concept.getContainer();
        assertNotNull(name);
        assertTrue(this.taxonomy.getNames().containsValue(name));
    }


    @Test
    public void testLoadIntoTaxonomy2() throws Exception {
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-2.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source));
        TaxonConceptInstance instance = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10044710");
        assertNotNull(instance);
        assertEquals("Bryidae", instance.getScientificName());
        assertEquals("Engl.", instance.getScientificNameAuthorship());
        assertNull(instance.getAcceptedNameUsageID());
        assertNull(instance.getParentNameUsageID());
        assertEquals("http://id.biodiversity.org.au/node/ausmoss/10044710", instance.getTaxonID());
        assertEquals(NomenclaturalClassifier.BOTANICAL, instance.getCode());
        assertNotNull(instance.getProvider());
        assertEquals("default", instance.getProvider().getId());
        assertEquals(RankType.SUBCLASS, instance.getRank());
        assertNull(instance.getStatus());
        assertEquals(TaxonomicType.ACCEPTED, instance.getTaxonomicStatus());
        assertNull(instance.getYear());
        Map<Term, Optional<String>> classification = instance.getClassification();
        assertNotNull(classification);
        assertEquals("Plantae", classification.get(DwcTerm.kingdom).get());
        assertEquals("Equisetopsida", classification.get(DwcTerm.class_).get());
        TaxonConcept concept = instance.getContainer();
        assertNotNull(concept);
        ScientificName name = concept.getContainer();
        assertNotNull(name);
        assertTrue(this.taxonomy.getNames().containsValue(name));
    }
    @Test
    public void testLoadIntoTaxonomy3() throws Exception {
        CSVNameSource locations = new CSVNameSource(this.resourceReader("locations/Location.csv"), ALATerm.Location);
        CSVNameSource source = new CSVNameSource(this.resourceReader("taxonomy-38.csv"), DwcTerm.Taxon);
        this.taxonomy.load(Arrays.asList(source, locations));
        TaxonConceptInstance instance = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10005930");
        assertNotNull(instance);
        assertEquals("Bryoerythrophyllum dubium", instance.getScientificName());
        assertEquals("(Schwägr.) P.Sollman", instance.getScientificNameAuthorship());
        assertNull(instance.getAcceptedNameUsageID());
        assertEquals("http://id.biodiversity.org.au/node/ausmoss/10044716", instance.getParentNameUsageID());
        assertNotNull(instance.getProvider());
        assertEquals("dr100", instance.getProvider().getId());
        assertEquals(RankType.SPECIES, instance.getRank());
        List<Distribution> distribution = instance.getDistribution();
        assertNotNull(distribution);
        assertEquals(1, distribution.size());
        Distribution dist = distribution.get(0);
        assertNotNull(dist.getLocation());
        assertEquals("http://vocab.getty.edu/tgn/7001834", dist.getLocation().getLocationID());
        assertEquals("Western Australia", dist.getLocation().getLocality());
        assertNull(dist.getLifeStage());
        assertNull(dist.getOccurrenceStatus());
        assertNull(dist.getAdditional());
        assertSame(instance.getProvider(), dist.getProvider());

        instance = this.taxonomy.getInstance("http://id.biodiversity.org.au/node/ausmoss/10001788");
        assertNotNull(instance);
        assertEquals("Bryoerythrophyllum recurvirostre", instance.getScientificName());
        distribution = instance.getDistribution();
        assertNotNull(distribution);
        assertEquals(2, distribution.size());
        dist = distribution.get(0);
        assertNotNull(dist.getLocation());
        assertEquals("http://vocab.getty.edu/tgn/1007961", dist.getLocation().getLocationID());
        assertEquals("Macquarrie Island", dist.getLocation().getLocality());
        assertNull(dist.getLifeStage());
        assertNull(dist.getOccurrenceStatus());
        assertNull(dist.getAdditional());
        assertSame(instance.getProvider(), dist.getProvider());
        dist = distribution.get(1);
        assertNotNull(dist.getLocation());
        assertEquals("http://vocab.getty.edu/tgn/1007365", dist.getLocation().getLocationID());
        assertEquals("Heard Island", dist.getLocation().getLocality());
        assertNull(dist.getLifeStage());
        assertNull(dist.getOccurrenceStatus());
        assertNull(dist.getAdditional());
        assertSame(instance.getProvider(), dist.getProvider());

    }

}
