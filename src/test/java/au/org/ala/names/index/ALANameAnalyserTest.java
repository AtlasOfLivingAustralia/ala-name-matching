

package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import au.org.ala.names.model.SynonymType;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.TaxonomicStatus;
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
    public void testCanonicaliseTaxonomicStatus1() throws Exception {
        TaxonomicStatus status = this.analyser.canonicaliseTaxonomicStatus("accepted");
        assertEquals(TaxonomicStatus.ACCEPTED, status);
    }

    @Test
    public void testCanonicaliseTaxonomicStatus2() throws Exception {
        TaxonomicStatus status = this.analyser.canonicaliseTaxonomicStatus("synonym");
        assertEquals(TaxonomicStatus.SYNONYM, status);
    }
    
    @Test
    public void testCanonicaliseTaxonomicStatus3() throws Exception {
        TaxonomicStatus status = this.analyser.canonicaliseTaxonomicStatus("");
        assertNull(status);
    }

    @Test
    public void testCanonicaliseTaxonomicStatus4() throws Exception {
        try {
            TaxonomicStatus status = this.analyser.canonicaliseTaxonomicStatus("unknown");
            fail("Expecting exception");
        } catch (IllegalArgumentException ex) {
        }
    }

    @Test
    public void testCanonicaliseSynonymType1() throws Exception {
        SynonymType status = this.analyser.canonicaliseSynonymType("heterotypicSynonym");
        assertEquals(SynonymType.SUBJECTIVE_SYNONYM, status);
    }

    @Test
    public void testCanonicaliseSynonymType2() throws Exception {
        SynonymType status = this.analyser.canonicaliseSynonymType("synonym");
        assertEquals(SynonymType.SYNONYM, status);
    }

    @Test
    public void testCanonicaliseSynonymType3() throws Exception {
        SynonymType status = this.analyser.canonicaliseSynonymType("");
        assertNull(status);
    }

    @Test
    public void testCanonicaliseSynonymType4() throws Exception {
        try {
            SynonymType status = this.analyser.canonicaliseSynonymType("something else");
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
