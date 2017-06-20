

package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonomicType;
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
        NameKey key = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla F.Muell.", null);
        assertEquals(NomenclaturalCode.BOTANICAL, key.getCode());
        assertEquals("HEMIGENIA BRACHIPHILA", key.getScientificName());
        assertEquals("F.Muell.", key.getScientificNameAuthorship());

    }

    @Test
    public void testKey2() throws Exception {
        NameKey key = this.analyser.analyse("ICZN", "Abantiades ocellatus Tindale, 1932", null);
        assertEquals(NomenclaturalCode.ZOOLOGICAL, key.getCode());
        assertEquals("ABANTIADES OCELATA", key.getScientificName());
        assertEquals("Tindale, 1932", key.getScientificNameAuthorship());
    }

    @Test
    public void testKeyEquals1() throws Exception {
        NameKey key1 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla F.Muell.", null);
        NameKey key2 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla F.Muell.", null);
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testKeyEquals2() throws Exception {
        NameKey key1 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphila F.Muell.", null);
        NameKey key2 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla F.Muell.", null);
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testKeyEquals3() throws Exception {
        NameKey key1 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla F.Muell.", null);
        NameKey key2 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla F von Mueller", null);
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testkeyEquals4() throws Exception {
        NameKey key1 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla F.Muell.", null);
        NameKey key2 = this.analyser.analyse("ICZN", "Abantiades ocellatus Tindale, 1932", null);
        assertFalse(key1.equals(key2));
    }

    @Test
    public void testkeyEquals5() throws Exception {
        NameKey key1 = this.analyser.analyse("ICNAFP", "Bryidae Engl.", null);
        NameKey key2 = this.analyser.analyse("ICNAFP", "Bryidae Engler", null);
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testkeyEquals6() throws Exception {
        NameKey key1 = this.analyser.analyse("ICNAFP", "Bryidae", "Engl.");
        NameKey key2 = this.analyser.analyse("ICNAFP", "Bryidae", "Engler");
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testkeyEquals7() throws Exception {
        NameKey key1 = this.analyser.analyse("ICNAFP", "Bryidae", "Engl.");
        NameKey key2 = this.analyser.analyse("ICNAFP", "Bryidae", "H.G.A.Engler");
        assertTrue(key1.equals(key2));
    }


    @Test
    public void testCanonicaliseCode1() throws Exception {
        NomenclaturalCode code = this.analyser.canonicaliseCode("ICZN");
        assertEquals(NomenclaturalCode.ZOOLOGICAL, code);
    }

    @Test
    public void testCanonicaliseCode2() throws Exception {
        try {
            NomenclaturalCode code = this.analyser.canonicaliseCode("FLUFFY");
            fail("Expecting exception");
        } catch (IllegalArgumentException ex) {
        }
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
        assertNull(status);
    }

    @Test
    public void testCanonicaliseTaxonomicType4() throws Exception {
        try {
            TaxonomicType status = this.analyser.canonicaliseTaxonomicType("unknown");
            fail("Expecting exception");
        } catch (IllegalArgumentException ex) {
        }
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
        assertNull(status);
    }

    @Test
    public void testCanonicaliseTaxonomicType8() throws Exception {
        try {
            TaxonomicType status = this.analyser.canonicaliseTaxonomicType("something else");
            fail("Expecting exception");
        } catch (IllegalArgumentException ex) {
        }
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
        assertNull(rank);
    }

    @Test
    public void testCanonicaliseRankType4() throws Exception {
        try {
            RankType rank = this.analyser.canonicaliseRank("something else");
            fail("Expecting exception");
        } catch (IllegalArgumentException ex) {
        }
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
        try {
            NomenclaturalStatus status = this.analyser.canonicaliseNomenclaturalStatus("unknown");
            fail("Expecting exception");
        } catch (IllegalArgumentException ex) {
        }
    }

}
