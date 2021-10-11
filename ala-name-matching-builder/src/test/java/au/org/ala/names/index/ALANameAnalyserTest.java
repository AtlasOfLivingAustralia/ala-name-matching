

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
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test cases for cleaned scientific names
 */
public class ALANameAnalyserTest {
    private ALANameAnalyser analyser;

    @Before
    public void setup() {
        this.analyser = new ALANameAnalyser();
    }

    @Test
    public void testKey1() throws Exception {
        NameKey key = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "F.Muell.", "species");
        assertEquals(NomenclaturalCode.BOTANICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("HEMIGENIA BRACHIPHILA", key.getScientificName());
        assertEquals("F.Muell.", key.getScientificNameAuthorship());
    }

    @Test
    public void testKey2() throws Exception {
        NameKey key = this.analyser.analyse("ICZN", "Abantiades ocellatus", "Tindale, 1932", "species");
        assertEquals(NomenclaturalCode.ZOOLOGICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("ABANTIADES OCELATA", key.getScientificName());
        assertEquals("Tindale, 1932", key.getScientificNameAuthorship());
    }

    @Test
    public void testKey3() throws Exception {
        NameKey key = this.analyser.analyse("ICZN", "Abantiades ocellatus", "Tindale, 1932", "species");
        assertEquals(NomenclaturalCode.ZOOLOGICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("ABANTIADES OCELATA", key.getScientificName());
        assertEquals("Tindale, 1932", key.getScientificNameAuthorship());
    }


    @Test
    public void testKey4() throws Exception {
        NameKey key = this.analyser.analyse("ICZN", "Chezala Subgroup 4", null, "genus");
        assertEquals(NameType.PLACEHOLDER, key.getType());
        assertEquals("CHEZALA SUBGROUP 4", key.getScientificName());
    }

    @Test
    public void testKey5() throws Exception {
        NameKey key = this.analyser.analyse("ICNAFP", "Convolvulus sect. Brewera", null, "genus");
        assertEquals(NomenclaturalCode.BOTANICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("CONVOLVULUS BREWERA", key.getScientificName());
    }

    @Test
    public void testKey6() throws Exception {
        NameKey key = this.analyser.analyse("ICNAFP", "Convolvulus sect. Brewera", null, "genus");
        assertEquals(NomenclaturalCode.BOTANICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("CONVOLVULUS BREWERA", key.getScientificName());
    }


    @Test
    public void testKey7() throws Exception {
        NameKey key = this.analyser.analyse("ICZN", "Incertae sedis", null, "species");
        assertEquals(NomenclaturalCode.ZOOLOGICAL, key.getCode());
        assertEquals(NameType.PLACEHOLDER, key.getType());
    }

    @Test
    public void testKey8() throws Exception {
        NameKey key = this.analyser.analyse("ICZN", "Unplaced acacia", null, "species");
        assertEquals(NomenclaturalCode.ZOOLOGICAL, key.getCode());
        assertEquals(NameType.PLACEHOLDER, key.getType());
    }

    @Test
    public void testKey9() throws Exception {
        NameKey key = this.analyser.analyse("ICBN", "Entoloma sp. (C)", null, "species");
        assertEquals(NomenclaturalCode.BOTANICAL, key.getCode());
        assertEquals(NameType.INFORMAL, key.getType());
        assertEquals("ENTOLOMA C", key.getScientificName());
    }

    @Test
    public void testKey10() throws Exception {
        NameKey key = this.analyser.analyse("ICNAFP", "Atriplex ser. Stipitata", null, "series botany");
        assertEquals(NomenclaturalCode.BOTANICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("ATRIPLEX STIPITATA", key.getScientificName());
    }

    @Test
    public void testKey11() throws Exception {
        NameKey key = this.analyser.analyse("ICNAFP", "Atriplex stipitatum", null, "species");
        assertEquals(NomenclaturalCode.BOTANICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("ATRIPLEX STIPITATA", key.getScientificName());
    }

    @Test
    public void testKey12() throws Exception {
        NameKey key = this.analyser.analyse("ICNAFP", "Brachyscome 'Pilliga Posy'", null, "species");
        assertEquals(NomenclaturalCode.BOTANICAL, key.getCode());
        assertEquals(NameType.CULTIVAR, key.getType());
        assertEquals("BRACHYSCOME 'PILLIGA POSY'", key.getScientificName());
        assertNull(key.getScientificNameAuthorship());
    }

    @Test
    public void testKey13() throws Exception {
        NameKey key = this.analyser.analyse("ICNAFP", "Eucalyptus caesia 'Silver Princess'", null, "species");
        assertEquals(NomenclaturalCode.BOTANICAL, key.getCode());
        assertEquals(NameType.CULTIVAR, key.getType());
        assertEquals("EUCALYPTUS CAESIA 'SILVER PRINCESS'", key.getScientificName());
        assertNull(key.getScientificNameAuthorship());
    }

    @Test
    public void testKey14() throws Exception {
        NameKey key = this.analyser.analyse("ICNAFP", "Munida aff. amathea", null, "species");
        assertEquals(NomenclaturalCode.BOTANICAL, key.getCode());
        assertEquals(NameType.DOUBTFUL, key.getType());
        assertEquals("MUNIDA AFF AMATHEA", key.getScientificName());
        assertNull(key.getScientificNameAuthorship());
    }

    @Test
    public void testKey15() throws Exception {
        NameKey key = this.analyser.analyse("ICNAFP", "Munida aff amathea", null, "species");
        assertEquals(NomenclaturalCode.BOTANICAL, key.getCode());
        assertEquals(NameType.DOUBTFUL, key.getType());
        assertEquals("MUNIDA AFF AMATHEA", key.getScientificName());
        assertNull(key.getScientificNameAuthorship());
    }

    @Test
    public void testKey16() throws Exception {
        NameKey key = this.analyser.analyse("ICNAFP", "Waminoa cf. brickneri", null, "species");
        assertEquals(NomenclaturalCode.BOTANICAL, key.getCode());
        assertEquals(NameType.DOUBTFUL, key.getType());
        assertEquals("WAMINOA CF BRICKNERI", key.getScientificName());
        assertNull(key.getScientificNameAuthorship());
    }

    @Test
    public void testKey17() throws Exception {
        NameKey key = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Waminoa cf. brickneri", null, null, null, true);
        assertEquals(NomenclaturalCode.BOTANICAL, key.getCode());
        assertEquals(NameType.DOUBTFUL, key.getType());
        assertEquals("WAMINOA CF BRICKNERI", key.getScientificName());
        assertNull(key.getScientificNameAuthorship());
    }

    // Autonym test
    @Test
    public void testKey18() throws Exception {
        NameKey key = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Gonocarpus micranthus Thunb. subsp. micranthus", null, null, null, false);
        assertEquals(NomenclaturalCode.BOTANICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("GONOCARPUS MICRANTHUS MICRANTHUS", key.getScientificName());
        assertNull(key.getScientificNameAuthorship());
        assertTrue(key.isAutonym());
    }

    @Test
    public void testKey19() throws Exception {
        NameKey key = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Gonocarpus micranthus subsp. ramosissimus", "Orchard", null, null, false);
        assertEquals(NomenclaturalCode.BOTANICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("GONOCARPUS MICRANTUS RAMOSISIMA", key.getScientificName());
        assertEquals("Orchard", key.getScientificNameAuthorship());
        assertFalse(key.isAutonym());
    }

    @Test
    public void testKey20() throws Exception {
        NameKey key = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Gonocarpus micranthus subsp. ramosissimus Orchard", null, null, null, true);
        assertEquals(NomenclaturalCode.BOTANICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("GONOCARPUS MICRANTUS RAMOSISIMA", key.getScientificName());
        assertEquals("Orchard", key.getScientificNameAuthorship());
        assertFalse(key.isAutonym());
    }

    @Test
    public void testKey21() throws Exception {
        NameKey key = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Gonocarpus micranthus Thunb. subsp. micranthus", "Thunb.", null, null, false);
        assertEquals(NomenclaturalCode.BOTANICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("GONOCARPUS MICRANTHUS MICRANTHUS", key.getScientificName());
        assertNull(key.getScientificNameAuthorship());
        assertTrue(key.isAutonym());
    }

    // Author without trailing year marker
    @Test
    public void testKey22() throws Exception {
        NameKey key = this.analyser.analyse(NomenclaturalCode.BACTERIAL, "Gemmatimonas aurantiaca Zhang et al. amerlia ", "Zhang et al.", null, null, false);
        assertEquals(NomenclaturalCode.BACTERIAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("GEMMATIMONAS AURANTIACA AMERLIA", key.getScientificName());
        assertEquals("Zhang et al.", key.getScientificNameAuthorship());
        assertFalse(key.isAutonym());
    }

    // Author with trailing year marker
    @Test
    public void testKey23() throws Exception {
        NameKey key = this.analyser.analyse(NomenclaturalCode.BACTERIAL, "Gemmatimonas aurantiaca Zhang et al., 1995", "Zhang et al.", null, null, false);
        assertEquals(NomenclaturalCode.BACTERIAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("GEMMATIMONAS AURANTIACA", key.getScientificName());
        assertEquals("Zhang et al.", key.getScientificNameAuthorship());
        assertFalse(key.isAutonym());
    }

    // Author with trailing year marker
    @Test
    public void testKey24() throws Exception {
        NameKey key = this.analyser.analyse(NomenclaturalCode.BACTERIAL, "Gemmatimonas aurantiaca Zhang et al., 1995 amerlia", "Zhang et al.", null, null, false);
        assertEquals(NomenclaturalCode.BACTERIAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("GEMMATIMONAS AURANTIACA AMERLIA", key.getScientificName());
        assertEquals("Zhang et al.", key.getScientificNameAuthorship());
        assertEquals(RankType.INFRASPECIFICNAME, key.getRank());
        assertFalse(key.isAutonym());
    }

    // Test quoted genus
    @Test
    public void testKey25() throws Exception {
        NameKey key = this.analyser.analyse(NomenclaturalCode.ZOOLOGICAL, "\"Hypomecis\" catephes", "(Turner, 1947)", null, null, false);
        assertEquals(NomenclaturalCode.ZOOLOGICAL, key.getCode());
        assertEquals(NameType.DOUBTFUL, key.getType());
        assertEquals("\"HYPOMECIS\" CATEPHES", key.getScientificName());
        assertEquals("(Turner, 1947)", key.getScientificNameAuthorship());
        assertEquals(RankType.UNRANKED, key.getRank());
     }


    // Test aff. name looks like an author
    @Test
    public void testKey26() throws Exception {
        // With authot
        NameKey key1 = this.analyser.analyse(null, "Carex aff. tereticaulis (Lake Omeo)", "sensu G.W. Carr", RankType.UNRANKED, TaxonomicType.INFERRED_UNPLACED, true);
        assertEquals(null, key1.getCode());
        assertEquals(NameType.DOUBTFUL, key1.getType());
        assertEquals("CAREX AFF TERETICAULIS LAKE OMEO", key1.getScientificName());
        assertEquals("sensu G.W. Carr", key1.getScientificNameAuthorship());
        assertEquals(RankType.UNRANKED, key1.getRank());

        // Without author
        NameKey key2 = this.analyser.analyse(null, "Carex aff. tereticaulis (Lake Omeo)", null, RankType.UNRANKED, TaxonomicType.INFERRED_UNPLACED, true);
        assertEquals(null, key2.getCode());
        assertEquals(NameType.DOUBTFUL, key2.getType());
        assertEquals("CAREX AFF TERETICAULIS LAKE OMEO", key2.getScientificName());
        assertEquals(null, key2.getScientificNameAuthorship());
        assertEquals(RankType.UNRANKED, key2.getRank());
    }

    @Test
    public void testAuthorEquals1() throws Exception {
        assertEquals(0, this.analyser.compareAuthor(null, null));
        assertTrue( this.analyser.compareAuthor("L.", null) > 0);
        assertTrue(this.analyser.compareAuthor(null, "L.") < 0);
    }

    @Test
    public void testAuthorEquals2() throws Exception {
        assertEquals(0, this.analyser.compareAuthor("Lindel", "Lindel"));
        assertTrue( this.analyser.compareAuthor("Alphose", "Lindel") < 0);
        assertTrue(this.analyser.compareAuthor("Lindel", "Alphonse") > 0);
    }

    @Test
    public void testAuthorEquals3() throws Exception {
        assertEquals(0, this.analyser.compareAuthor("L.", "Linnaeus"));
        assertEquals(0, this.analyser.compareAuthor("L.", "Lin."));
     }

    // Ensure ex authors are treated properly
    @Test
    public void testAuthorEquals4() throws Exception {
        assertEquals(0, this.analyser.compareAuthor("Desf. ex Poir.", "Poir."));
        assertEquals(0, this.analyser.compareAuthor("Poir.", "Desf. ex Poir."));
    }


    @Test
    public void testKeyEquals1() throws Exception {
        NameKey key1 = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Hemigenia brachyphylla F.Muell.", null, RankType.SPECIES, null, true);
        NameKey key2 = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Hemigenia brachyphylla F.Mueller", null, RankType.SPECIES, null, true);
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testKeyEquals2() throws Exception {
        NameKey key1 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphila", "F.Muell.", "species");
        NameKey key2 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "F.Muell.", "species");
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testKeyEquals3() throws Exception {
        NameKey key1 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "F.Muell.", "species");
        NameKey key2 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "F von Mueller", "species");
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testkeyEquals4() throws Exception {
        NameKey key1 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "F.Muell.", "species");
        NameKey key2 = this.analyser.analyse("ICZN", "Abantiades ocellatus", "Tindale, 1932", "species");
        assertFalse(key1.equals(key2));
    }

    @Test
    public void testkeyEquals5() throws Exception {
        NameKey key1 = this.analyser.analyse("ICNAFP", "Bryidae", "Engl.", "subclass");
        NameKey key2 = this.analyser.analyse("ICNAFP", "Bryidae", "Engler", "subclass");
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testkeyEquals6() throws Exception {
        NameKey key1 = this.analyser.analyse("ICNAFP", "Bryidae", "Engl.", "subclass");
        NameKey key2 = this.analyser.analyse("ICNAFP", "Bryidae", "Engler", "subclass");
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testkeyEquals7() throws Exception {
        NameKey key1 = this.analyser.analyse("ICNAFP", "Bryidae", "Engl.", "subclass");
        NameKey key2 = this.analyser.analyse("ICNAFP", "Bryidae", "H.G.A.Engler", "subclass");
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testkeyEquals8() throws Exception {
        NameKey key1 = this.analyser.analyse("ICZN", "Typhinae", null, "subfamily");
        NameKey key2 = this.analyser.analyse("ICZN", "Tiphiinae", null, "subfamily");
        assertFalse(key1.equals(key2));
    }

    @Test
    public void testkeyEquals9() throws Exception {
        NameKey key1 = this.analyser.analyse("ICZN", "Amenia (imperialis group)", null, "species group");
        NameKey key2 = this.analyser.analyse("ICZN", "Amenia (leonina group)", null, "species group");
        assertFalse(key1.equals(key2));
    }

    @Test
    public void testkeyEquals10() throws Exception {
        NameKey key1 = this.analyser.analyse("ICNAFP", "Atriplex ser. Stipitata", null, "series botany");
        NameKey key2 = this.analyser.analyse("ICNAFP", "Atriplex stipitatum", null, "species");
        assertFalse(key1.equals(key2));
    }

    // Loose names
    @Test
    public void testkeyEquals11() throws Exception {
        NameKey key1 = this.analyser.analyse(null, "Acaena rorida", "B.H.Macmill.", null, null, false);
        NameKey key2 = this.analyser.analyse(null, "Acaena rorida B.H.Macmill.", null, null, null, true);
        assertTrue(key1.equals(key2));
    }


    // Cultivar names
    @Test
    public void testkeyEquals12() throws Exception {
        NameKey key1 = this.analyser.analyse(null, "Acacia dealbata 'Morning Glory'", null, "species");
        NameKey key2 = this.analyser.analyse(null, "Acacia dealbata Morning Glory", null, "species");
        assertFalse(key1.equals(key2));
    }

    @Test
    public void testKeyEquals13() throws Exception {
        NameKey key1 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "F.Muell.", "species");
        NameKey key2 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "F. Muell.", "species");
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testKeyEquals14() throws Exception {
        NameKey key1 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "A.F. Nurke", "species");
        NameKey key2 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "A.F.Nurke", "species");
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testKeyEquals15() throws Exception {
        NameKey key1 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "A.F. Nûrke", "species");
        NameKey key2 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "A.F. Nurke", "species");
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testKeyEquals16() throws Exception {
        NameKey key1 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "A. Nurke", "species");
        NameKey key2 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "A Nurke", "species");
        assertTrue(key1.equals(key2));
    }

    // Placeholder names
    @Test
    public void testKeyEquals17() throws Exception {
        NameKey key1 = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Incertae sedis", "F.Muell.", RankType.SPECIES, TaxonomicType.ACCEPTED, false);
        NameKey key2 = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Incertae sedis", "F.Muell.", RankType.SPECIES, TaxonomicType.ACCEPTED, false);
        assertFalse(key1.equals(key2));
    }

    @Test
    public void testKeyEquals18() throws Exception {
        NameKey key1 = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Hemigenia brachyphylla", "F.Muell.", RankType.SPECIES, TaxonomicType.INCERTAE_SEDIS, false);
        NameKey key2 = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Hemigenia brachyphylla", "F.Muell.", RankType.SPECIES, TaxonomicType.INCERTAE_SEDIS, false);
        assertFalse(key1.equals(key2));
    }

    // Autonyms
    @Test
    public void testKeyEquals19() throws Exception {
        NameKey key1 = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Senecio glomeratus Desf. ex Poir. subsp. glomeratus", null, null, null, false);
        NameKey key2 = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Senecio glomeratus Poir. subsp. glomeratus", "Poir.", null, null, false);
        assertTrue(key1.isAutonym());
        assertTrue(key2.isAutonym());
        assertEquals(key1, key2);
    }

    // Escaped letters
    @Test
    public void testKeyEquals20() throws Exception {
        NameKey key1 = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Senecio \\glomeratus", "Poir.", null, null, false);
        NameKey key2 = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Senecio glomeratus", "Poir.", null, null, false);
        assertEquals(key1, key2);
    }

    @Test
    public void testKeyEquals21() throws Exception {
        NameKey key1 = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Senecio glomeratus", "\\(Poir\\.\\)", null, null, false);
        NameKey key2 = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Senecio glomeratus", "(Poir.)", null, null, false);
        assertEquals(key1, key2);
    }

    // Ampersands
    @Test
    public void testKeyEquals22() throws Exception {
        NameKey key1 = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Senecio glomeratus", "Poir.  and Labil", null, null, false);
        NameKey key2 = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Senecio glomeratus", "Poir. &  Labil", null, null, false);
        assertEquals(key1, key2);
    }

    // Changed combination marker
    @Test
    public void testKeyEquals23() throws Exception {
        NameKey key1 = this.analyser.analyse(NomenclaturalCode.ZOOLOGICAL, "Osphranter rufus", "Desmarest, 1822", null, null, false);
        NameKey key2 = this.analyser.analyse(NomenclaturalCode.ZOOLOGICAL, "Osphranter rufus", "(Desmarest, 1822)", null, null, false);
        assertEquals(key1, key2);
    }

    // Placeholder names
    @Test
    public void testKeyEquals24() throws Exception {
        NameKey key1 = this.analyser.analyse(NomenclaturalCode.ZOOLOGICAL, "Galaxias sp. 3", null, null, null, false);
        NameKey key2 = this.analyser.analyse(NomenclaturalCode.ZOOLOGICAL, "Galaxias sp 3", null, null, null, false);
        assertEquals(key1, key2);
    }

    @Test
    public void testKeyEquals25() throws Exception {
        NameKey key1 = this.analyser.analyse(NomenclaturalCode.ZOOLOGICAL, "Galaxias sp. 3", null, null, null, false);
        NameKey key2 = this.analyser.analyse(NomenclaturalCode.ZOOLOGICAL, "Galaxias sp 3", null, null, null, true);
        assertEquals(key1, key2);
    }

    // Initially quoted names
    @Test
    public void testKeyEquals26() throws Exception {
        NameKey key1 = this.analyser.analyse(NomenclaturalCode.ZOOLOGICAL, "\"Hypomecis\" catephes", null, RankType.SPECIES, null, false);
        NameKey key2 = this.analyser.analyse(NomenclaturalCode.ZOOLOGICAL, "Hypomecis catephes", null, RankType.SPECIES, null, false);
        assertNotEquals(key1, key2);
    }


    @Test
    public void testCanonicaliseCode1() throws Exception {
        NomenclaturalCode code = this.analyser.canonicaliseCode("ICZN");
        assertEquals(NomenclaturalCode.ZOOLOGICAL, code);
    }

    @Test
    public void testCanonicaliseCode2() throws Exception {
        NomenclaturalCode code = this.analyser.canonicaliseCode("FLUFFY");
        assertNull(code);
    }

    @Test
    public void testCanonicaliseTaxonomicType1() throws Exception {
        TaxonomicType status = this.analyser.canonicaliseTaxonomicType("accepted");
        assertEquals(TaxonomicType.ACCEPTED, status);
    }

    @Test
    public void testCanonicaliseTaxonomicType2() throws Exception {
        TaxonomicType status = this.analyser.canonicaliseTaxonomicType("synonym");
        assertEquals(TaxonomicType.SYNONYM, status);
    }
    
    @Test
    public void testCanonicaliseTaxonomicType3() throws Exception {
        TaxonomicType status = this.analyser.canonicaliseTaxonomicType("");
        assertEquals(TaxonomicType.INFERRED_UNPLACED, status);
    }

    @Test
    public void testCanonicaliseTaxonomicType4() throws Exception {
        TaxonomicType status = this.analyser.canonicaliseTaxonomicType("unknown");
        assertEquals(TaxonomicType.UNPLACED, status);
    }

    @Test
    public void testCanonicaliseTaxonomicType5() throws Exception {
        TaxonomicType status = this.analyser.canonicaliseTaxonomicType("heterotypicSynonym");
        assertEquals(TaxonomicType.HETEROTYPIC_SYNONYM, status);
    }

    @Test
    public void testCanonicaliseTaxonomicType6() throws Exception {
        TaxonomicType status = this.analyser.canonicaliseTaxonomicType("synonym");
        assertEquals(TaxonomicType.SYNONYM, status);
    }

    @Test
    public void testCanonicaliseTaxonomicType7() throws Exception {
        TaxonomicType status = this.analyser.canonicaliseTaxonomicType("");
        assertEquals(TaxonomicType.INFERRED_UNPLACED, status);
    }

    @Test
    public void testCanonicaliseTaxonomicType8() throws Exception {
        TaxonomicType status = this.analyser.canonicaliseTaxonomicType("something else");
        assertEquals(TaxonomicType.INFERRED_UNPLACED, status);
    }

    @Test
    public void testCanonicaliseRankType1() throws Exception {
        RankType rank = this.analyser.canonicaliseRank("family");
        assertEquals(RankType.FAMILY, rank);
    }

    @Test
    public void testCanonicaliseRankType2() throws Exception {
        RankType rank = this.analyser.canonicaliseRank("Section zoology");
        assertEquals(RankType.SECTION_ZOOLOGY, rank);
    }

    @Test
    public void testCanonicaliseRankType3() throws Exception {
        RankType rank = this.analyser.canonicaliseRank("");
        assertEquals(RankType.UNRANKED, rank);
    }

    @Test
    public void testCanonicaliseRankType4() throws Exception {
        RankType rank = this.analyser.canonicaliseRank("something else");
        assertEquals(RankType.UNRANKED, rank);
     }

    @Test
    public void testCanonicaliseNomenclaturalStatus1() throws Exception {
        NomenclaturalStatus status = this.analyser.canonicaliseNomenclaturalStatus("doubtful");
        assertEquals(NomenclaturalStatus.DOUBTFUL, status);
    }

    @Test
    public void testCanonicaliseNomenclaturalStatus2() throws Exception {
        NomenclaturalStatus status = this.analyser.canonicaliseNomenclaturalStatus("nomen dubium");
        assertEquals(NomenclaturalStatus.DOUBTFUL, status);
    }

    @Test
    public void testCanonicaliseNomenclaturalStatus3() throws Exception {
        NomenclaturalStatus status = this.analyser.canonicaliseNomenclaturalStatus("nom. dub.");
        assertEquals(NomenclaturalStatus.DOUBTFUL, status);
    }

    @Test
    public void testCanonicaliseNomenclaturalStatus4() throws Exception {
        NomenclaturalStatus status = this.analyser.canonicaliseNomenclaturalStatus("nom dub");
        assertEquals(NomenclaturalStatus.DOUBTFUL, status);
    }

    @Test
    public void testCanonicaliseNomenclaturalStatus5() throws Exception {
        NomenclaturalStatus status = this.analyser.canonicaliseNomenclaturalStatus("");
        assertNull(status);
    }

    @Test
    public void testCanonicaliseNomenclaturalStatus6() throws Exception {
        NomenclaturalStatus status = this.analyser.canonicaliseNomenclaturalStatus("unknown");
        assertNull(status);
    }

}
