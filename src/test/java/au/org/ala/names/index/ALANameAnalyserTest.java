

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
        assertEquals(NameType.INFORMAL, key.getType());
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


    @Test
    public void testKeyEquals1() throws Exception {
        NameKey key1 = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Hemigenia brachyphylla F.Muell.", null, RankType.SPECIES, true);
        NameKey key2 = this.analyser.analyse(NomenclaturalCode.BOTANICAL, "Hemigenia brachyphylla F.Mueller", null, RankType.SPECIES, true);
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
        NameKey key1 = this.analyser.analyse(null, "Acaena rorida", "B.H.Macmill.", RankType.SPECIES, false);
        NameKey key2 = this.analyser.analyse(null, "Acaena rorida B.H.Macmill.", null, null, true);
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
