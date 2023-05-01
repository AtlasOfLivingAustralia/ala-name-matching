

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

import au.org.ala.names.index.provider.KeyAdjustment;
import au.org.ala.names.index.provider.MatchTaxonCondition;
import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonFlag;
import au.org.ala.names.model.TaxonomicType;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

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
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "F.Muell.", "species");
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("HEMIGENIA BRACHIPHILA", key.getScientificName());
        assertEquals("F.Muell.", key.getScientificNameAuthorship());
    }

    @Test
    public void testKey2() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICZN", "Abantiades ocellatus", "Tindale, 1932", "species");
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.ZOOLOGICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("ABANTIADES OCELATA", key.getScientificName());
        assertEquals("Tindale, 1932", key.getScientificNameAuthorship());
    }

    @Test
    public void testKey3() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICZN", "Abantiades ocellatus", "Tindale, 1932", "species");
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.ZOOLOGICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("ABANTIADES OCELATA", key.getScientificName());
        assertEquals("Tindale, 1932", key.getScientificNameAuthorship());
    }


    @Test
    public void testKey4() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICZN", "Chezala Subgroup 4", null, "genus");
        NameKey key = result.getNameKey();
        assertEquals(NameType.PLACEHOLDER, key.getType());
        assertEquals("CHEZALA SUBGROUP 4", key.getScientificName());
    }

    @Test
    public void testKey5() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICNAFP", "Convolvulus sect. Brewera", null, "genus");
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("CONVOLVULUS BREWERA", key.getScientificName());
    }

    @Test
    public void testKey6() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICNAFP", "Convolvulus sect. Brewera", null, "genus");
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("CONVOLVULUS BREWERA", key.getScientificName());
    }


    @Test
    public void testKey7() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICZN", "Incertae sedis", null, "species");
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.ZOOLOGICAL, key.getCode());
        assertEquals(NameType.PLACEHOLDER, key.getType());
    }

    @Test
    public void testKey8() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICZN", "Unplaced acacia", null, "species");
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.ZOOLOGICAL, key.getCode());
        assertEquals(NameType.PLACEHOLDER, key.getType());
    }

    @Test
    public void testKey9() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICBN", "Entoloma sp. (C)", null, "species");
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key.getCode());
        assertEquals(NameType.INFORMAL, key.getType());
        assertEquals("ENTOLOMA C", key.getScientificName());
    }

    @Test
    public void testKey10() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICNAFP", "Atriplex ser. Stipitata", null, "series botany");
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("ATRIPLEX STIPITATA", key.getScientificName());
    }

    @Test
    public void testKey11() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICNAFP", "Atriplex stipitatum", null, "species");
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("ATRIPLEX STIPITATA", key.getScientificName());
    }

    @Test
    public void testKey12() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICNAFP", "Brachyscome 'Pilliga Posy'", null, "species");
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key.getCode());
        assertEquals(NameType.CULTIVAR, key.getType());
        assertEquals("BRACHYSCOME 'PILLIGA POSY'", key.getScientificName());
        assertNull(key.getScientificNameAuthorship());
    }

    @Test
    public void testKey13() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICNAFP", "Eucalyptus caesia 'Silver Princess'", null, "species");
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key.getCode());
        assertEquals(NameType.CULTIVAR, key.getType());
        assertEquals("EUCALYPTUS CAESIA 'SILVER PRINCESS'", key.getScientificName());
        assertNull(key.getScientificNameAuthorship());
    }

    @Test
    public void testKey14() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICNAFP", "Munida aff. amathea", null, "species");
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key.getCode());
        assertEquals(NameType.DOUBTFUL, key.getType());
        assertEquals("MUNIDA AFF AMATHEA", key.getScientificName());
        assertNull(key.getScientificNameAuthorship());
    }

    @Test
    public void testKey15() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICNAFP", "Munida aff amathea", null, "species");
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key.getCode());
        assertEquals(NameType.DOUBTFUL, key.getType());
        assertEquals("MUNIDA AFF AMATHEA", key.getScientificName());
        assertNull(key.getScientificNameAuthorship());
    }

    @Test
    public void testKey16() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICNAFP", "Waminoa cf. brickneri", null, "species");
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key.getCode());
        assertEquals(NameType.DOUBTFUL, key.getType());
        assertEquals("WAMINOA CF BRICKNERI", key.getScientificName());
        assertNull(key.getScientificNameAuthorship());
    }

    @Test
    public void testKey17() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Waminoa cf. brickneri", null, null, null, null, true);
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key.getCode());
        assertEquals(NameType.DOUBTFUL, key.getType());
        assertEquals("WAMINOA CF BRICKNERI", key.getScientificName());
        assertNull(key.getScientificNameAuthorship());
    }

    // Autonym test
    @Test
    public void testKey18() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Gonocarpus micranthus Thunb. subsp. micranthus", null, null, null, null, false);
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("GONOCARPUS MICRANTHUS MICRANTHUS", key.getScientificName());
        assertNull(key.getScientificNameAuthorship());
        assertTrue(key.isAutonym());
    }

    @Test
    public void testKey19() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Gonocarpus micranthus subsp. ramosissimus", "Orchard", null, null, null, false);
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("GONOCARPUS MICRANTUS RAMOSISIMA", key.getScientificName());
        assertEquals("Orchard", key.getScientificNameAuthorship());
        assertFalse(key.isAutonym());
    }

    @Test
    public void testKey20() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Gonocarpus micranthus subsp. ramosissimus Orchard", null, null, null, null, true);
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("GONOCARPUS MICRANTUS RAMOSISIMA", key.getScientificName());
        assertEquals("Orchard", key.getScientificNameAuthorship());
        assertFalse(key.isAutonym());
    }

    @Test
    public void testKey21() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Gonocarpus micranthus Thunb. subsp. micranthus", "Thunb.", null, null, null, false);
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("GONOCARPUS MICRANTHUS MICRANTHUS", key.getScientificName());
        assertNull(key.getScientificNameAuthorship());
        assertTrue(key.isAutonym());
    }

    // Author without trailing year marker
    @Test
    public void testKey22() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse(NomenclaturalClassifier.BACTERIAL, "Gemmatimonas aurantiaca Zhang et al. amerlia ", "Zhang et al.", null, null, null, false);
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BACTERIAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("GEMMATIMONAS AURANTIACA AMERLIA", key.getScientificName());
        assertEquals("Zhang et al.", key.getScientificNameAuthorship());
        assertFalse(key.isAutonym());
    }

    // Author with trailing year marker
    @Test
    public void testKey23() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse(NomenclaturalClassifier.BACTERIAL, "Gemmatimonas aurantiaca Zhang et al., 1995", "Zhang et al.", null, null, null, false);
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BACTERIAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("GEMMATIMONAS AURANTIACA", key.getScientificName());
        assertEquals("Zhang et al.", key.getScientificNameAuthorship());
        assertFalse(key.isAutonym());
    }

    // Author with trailing year marker
    @Test
    public void testKey24() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse(NomenclaturalClassifier.BACTERIAL, "Gemmatimonas aurantiaca Zhang et al., 1995 amerlia", "Zhang et al.", null, null, null, false);
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.BACTERIAL, key.getCode());
        assertEquals(NameType.SCIENTIFIC, key.getType());
        assertEquals("GEMMATIMONAS AURANTIACA AMERLIA", key.getScientificName());
        assertEquals("Zhang et al.", key.getScientificNameAuthorship());
        assertEquals(RankType.INFRASPECIFICNAME, key.getRank());
        assertFalse(key.isAutonym());
    }

    // Test quoted genus
    @Test
    public void testKey25() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse(NomenclaturalClassifier.ZOOLOGICAL, "\"Hypomecis\" catephes", "(Turner, 1947)", null, null, null, false);
        NameKey key = result.getNameKey();
        assertEquals(NomenclaturalClassifier.ZOOLOGICAL, key.getCode());
        assertEquals(NameType.DOUBTFUL, key.getType());
        assertEquals("\"HYPOMECIS\" CATEPHES", key.getScientificName());
        assertEquals("(Turner, 1947)", key.getScientificNameAuthorship());
        assertEquals(RankType.UNRANKED, key.getRank());
    }


    // Test aff. name looks like an author
    @Test
    public void testKey26() throws Exception {
        // With authot
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(null, "Carex aff. tereticaulis (Lake Omeo)", "sensu G.W. Carr", RankType.UNRANKED, TaxonomicType.INFERRED_UNPLACED, null, true);
        NameKey key1 = result1.getNameKey();
        assertEquals(null, key1.getCode());
        assertEquals(NameType.DOUBTFUL, key1.getType());
        assertEquals("CAREX AFF TERETICAULIS LAKE OMEO", key1.getScientificName());
        assertEquals("sensu G.W. Carr", key1.getScientificNameAuthorship());
        assertEquals(RankType.UNRANKED, key1.getRank());

        // Without author
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse(null, "Carex aff. tereticaulis (Lake Omeo)", null, RankType.UNRANKED, TaxonomicType.INFERRED_UNPLACED, null, true);
        NameKey key2 = result2.getNameKey();
        assertEquals(null, key2.getCode());
        assertEquals(NameType.DOUBTFUL, key2.getType());
        assertEquals("CAREX AFF TERETICAULIS LAKE OMEO", key2.getScientificName());
        assertEquals(null, key2.getScientificNameAuthorship());
        assertEquals(RankType.UNRANKED, key2.getRank());
    }

    // Test flags
    @Test
    public void testKey27() throws Exception {
        Set<TaxonFlag> flags = Collections.singleton(TaxonFlag.AMBIGUOUS_NOMENCLATURAL_CODE);
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Aulacoseira ambigua", "(Grunov) Simonsen", RankType.SPECIES, TaxonomicType.INFERRED_ACCEPTED, flags, true);
        NameKey key1 = result1.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key1.getCode());
        assertEquals(NameType.SCIENTIFIC, key1.getType());
        assertEquals("AULACOSEIRA AMBIGUA", key1.getScientificName());
        assertEquals("(Grunov) Simonsen", key1.getScientificNameAuthorship());
        assertEquals(RankType.SPECIES, key1.getRank());
        assertSame(flags, key1.getFlags());
    }

    // Test flags
    @Test
    public void testKey28() throws Exception {
        Set<TaxonFlag> flags = Collections.singleton(TaxonFlag.AMBIGUOUS_NOMENCLATURAL_CODE);
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Aulacoseira ambigua ambigua", "(Grunov) Simonsen", RankType.SPECIES, TaxonomicType.INFERRED_ACCEPTED, flags, true);
        NameKey key1 = result1.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key1.getCode());
        assertEquals(NameType.SCIENTIFIC, key1.getType());
        assertEquals("AULACOSEIRA AMBIGUA AMBIGUA", key1.getScientificName());
        assertNull(key1.getScientificNameAuthorship());
        assertEquals(RankType.SPECIES, key1.getRank());
        assertNotSame(flags, key1.getFlags());
        assertTrue(key1.getFlags().contains(TaxonFlag.AUTONYM));
        assertTrue(key1.getFlags().contains(TaxonFlag.AMBIGUOUS_NOMENCLATURAL_CODE));
    }

    // Test misc literature
    @Test
    public void testKey29() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(NomenclaturalClassifier.ZOOLOGICAL, "Genus B", null, RankType.UNRANKED, TaxonomicType.MISCELLANEOUS_LITERATURE, null, false);
        NameKey key1 = result1.getNameKey();
        assertEquals(NomenclaturalClassifier.ZOOLOGICAL, key1.getCode());
        assertEquals(NameType.INFORMAL, key1.getType());
        assertEquals("GENUS B", key1.getScientificName());
        assertNull(key1.getScientificNameAuthorship());
        assertEquals(RankType.UNRANKED, key1.getRank());
        assertNull(key1.getFlags());
        assertNull(result1.getMononomial());
        assertNull(result1.getGenus());
        assertNull(result1.getSpecificEpithet());
        assertNull(result1.getInfraspecificEpithet());
    }


    // Test obvious placeholder
    @Test
    public void testKey30() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(NomenclaturalClassifier.ZOOLOGICAL, "Genus B sp.", null, null, null, null, false);
        NameKey key1 = result1.getNameKey();
        assertEquals(NomenclaturalClassifier.ZOOLOGICAL, key1.getCode());
        assertEquals(NameType.INFORMAL, key1.getType());
        assertEquals("GENUS B SP", key1.getScientificName());
        assertNull(key1.getScientificNameAuthorship());
        assertEquals(RankType.SPECIES, key1.getRank());
        assertNull(key1.getFlags());
        assertNull(result1.getMononomial());
        assertNull(result1.getGenus());
        assertNull(result1.getSpecificEpithet());
        assertNull(result1.getInfraspecificEpithet());
    }


    // Test phrase names
    @Test
    public void testKey31() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Grevillea sp. Gillingarra (R.J.Cranfield 4087)", null, null, null, null, false);
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Grevillea sp. Gillingarra (R.J. Cranfield 4087)", null, null, null, null, false);
        NameAnalyser.AnalysisResult result3 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Grevillea sp. Gillingarra (R.J. Cranfield 4087) PN", null, null, null, null, false);
        NameKey key1 = result1.getNameKey();
        NameKey key2 = result2.getNameKey();
        NameKey key3 = result3.getNameKey();
        assertEquals(key1, key2);
        assertEquals(key1, key3);
        assertEquals(NomenclaturalClassifier.BOTANICAL, key1.getCode());
        assertEquals(NameType.PLACEHOLDER, key1.getType());
        assertEquals("GREVILLEA SP GILLINGARRA CRANFIELD4087", key1.getScientificName());
        assertNull(key1.getScientificNameAuthorship());
        assertEquals(RankType.SPECIES, key1.getRank());
        assertNull(key1.getFlags());
        assertEquals("Grevillea", result1.getMononomial());
        assertEquals("Grevillea", result1.getGenus());
        assertNull(result1.getSpecificEpithet());
        assertNull(result1.getInfraspecificEpithet());
    }
    @Test
    public void testKey32() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Symplocos sp. aff. S.thwaitesii", null, null, null, null, false);
        NameKey key1 = result1.getNameKey();
        assertEquals(NomenclaturalClassifier.BOTANICAL, key1.getCode());
        assertEquals(NameType.PLACEHOLDER, key1.getType());
        assertEquals("SYMPLOCOS S THWAITESII", key1.getScientificName());
        assertNull(key1.getScientificNameAuthorship());
        assertEquals(RankType.UNRANKED, key1.getRank());
        assertNull(key1.getFlags());
        assertEquals("Symplocos", result1.getMononomial());
        assertEquals("Symplocos", result1.getGenus());
        assertNull(result1.getSpecificEpithet());
        assertNull(result1.getInfraspecificEpithet());
    }

    // Isseu 194
    @Test
    public void testKey33() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Pterostylis cucullata subsp. sylvicola", "D.L.Jones", RankType.SUBSPECIES, TaxonomicType.HETEROTYPIC_SYNONYM, null, false);
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse(null, "Pterostylis cucullata subsp. sylvicola", "Pterostylis cucullata R.Br. ssp. sylvicola D.L.Jones", RankType.UNRANKED, TaxonomicType.INFERRED_ACCEPTED, null, false);
        assertEquals(result1.getMononomial(), result2.getMononomial());
        assertEquals(result1.getGenus(), result2.getGenus());
        assertEquals(result1.getSpecificEpithet(), result2.getSpecificEpithet());
        assertEquals(result1.getInfraspecificEpithet(), result2.getInfraspecificEpithet());
        assertEquals(result1.getCultivarEpithet(), result2.getCultivarEpithet());
        NameKey key1 = result1.getNameKey();
        NameKey key2 = result2.getNameKey();
        assertNotEquals(key1, key2);
        key1 = result1.getNameKey().toNameKey();
        key2 = result2.getNameKey().toNameKey();
        assertNotEquals(key1, key2);
        key1 = result1.getNameKey().toUnrankedNameKey();
        key2 = result2.getNameKey().toUnrankedNameKey();
        assertNotEquals(key1, key2);
        key1 = result1.getNameKey().toUncodedNameKey();
        key2 = result2.getNameKey().toUncodedNameKey();
        assertEquals(key1, key2);
   }

    @Test
    public void testAuthorEquals1() throws Exception {
        assertEquals(0, this.analyser.compareAuthor(null, null));
        assertTrue(this.analyser.compareAuthor("L.", null) > 0);
        assertTrue(this.analyser.compareAuthor(null, "L.") < 0);
    }

    @Test
    public void testAuthorEquals2() throws Exception {
        assertEquals(0, this.analyser.compareAuthor("Lindel", "Lindel"));
        assertTrue(this.analyser.compareAuthor("Alphose", "Lindel") < 0);
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
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Hemigenia brachyphylla F.Muell.", null, RankType.SPECIES, null, null, true);
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Hemigenia brachyphylla F.Mueller", null, RankType.SPECIES, null, null, true);
        NameKey key2 = result2.getNameKey();
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testKeyEquals2() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphila", "F.Muell.", "species");
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "F.Muell.", "species");
        NameKey key2 = result2.getNameKey();
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testKeyEquals3() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "F.Muell.", "species");
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "F von Mueller", "species");
        NameKey key2 = result2.getNameKey();
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testkeyEquals4() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "F.Muell.", "species");
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse("ICZN", "Abantiades ocellatus", "Tindale, 1932", "species");
        NameKey key2 = result2.getNameKey();
        assertFalse(key1.equals(key2));
    }

    @Test
    public void testkeyEquals5() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse("ICNAFP", "Bryidae", "Engl.", "subclass");
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse("ICNAFP", "Bryidae", "Engler", "subclass");
        NameKey key2 = result2.getNameKey();
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testkeyEquals6() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse("ICNAFP", "Bryidae", "Engl.", "subclass");
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse("ICNAFP", "Bryidae", "Engler", "subclass");
        NameKey key2 = result2.getNameKey();
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testkeyEquals7() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse("ICNAFP", "Bryidae", "Engl.", "subclass");
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse("ICNAFP", "Bryidae", "H.G.A.Engler", "subclass");
        NameKey key2 = result2.getNameKey();
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testkeyEquals8() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse("ICZN", "Typhinae", null, "subfamily");
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse("ICZN", "Tiphiinae", null, "subfamily");
        NameKey key2 = result2.getNameKey();
        assertFalse(key1.equals(key2));
    }

    @Test
    public void testkeyEquals9() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse("ICZN", "Amenia (imperialis group)", null, "species group");
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse("ICZN", "Amenia (leonina group)", null, "species group");
        NameKey key2 = result2.getNameKey();
        assertFalse(key1.equals(key2));
    }

    @Test
    public void testkeyEquals10() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse("ICNAFP", "Atriplex ser. Stipitata", null, "series botany");
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse("ICNAFP", "Atriplex stipitatum", null, "species");
        NameKey key2 = result2.getNameKey();
        assertFalse(key1.equals(key2));
    }

    // Loose names
    @Test
    public void testkeyEquals11() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(null, "Acaena rorida", "B.H.Macmill.", null, null, null, false);
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse(null, "Acaena rorida B.H.Macmill.", null, null, null, null, true);
        NameKey key2 = result2.getNameKey();
        assertTrue(key1.equals(key2));
    }


    // Cultivar names
    @Test
    public void testkeyEquals12() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(null, "Acacia dealbata 'Morning Glory'", null, "species");
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse(null, "Acacia dealbata Morning Glory", null, "species");
        NameKey key2 = result2.getNameKey();
        assertFalse(key1.equals(key2));
    }

    @Test
    public void testKeyEquals13() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "F.Muell.", "species");
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "F. Muell.", "species");
        NameKey key2 = result2.getNameKey();
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testKeyEquals14() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "A.F. Nurke", "species");
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "A.F.Nurke", "species");
        NameKey key2 = result2.getNameKey();
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testKeyEquals15() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "A.F. Nûrke", "species");
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "A.F. Nurke", "species");
        NameKey key2 = result2.getNameKey();
        assertTrue(key1.equals(key2));
    }

    @Test
    public void testKeyEquals16() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "A. Nurke", "species");
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla", "A Nurke", "species");
        NameKey key2 = result2.getNameKey();
        assertTrue(key1.equals(key2));
    }

    // Placeholder names
    @Test
    public void testKeyEquals17() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Incertae sedis", "F.Muell.", RankType.SPECIES, TaxonomicType.ACCEPTED, null, false);
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Incertae sedis", "F.Muell.", RankType.SPECIES, TaxonomicType.ACCEPTED, null, false);
        NameKey key2 = result2.getNameKey();
        assertFalse(key1.equals(key2));
    }

    @Test
    public void testKeyEquals18() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Hemigenia brachyphylla", "F.Muell.", RankType.SPECIES, TaxonomicType.INCERTAE_SEDIS, null, false);
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Hemigenia brachyphylla", "F.Muell.", RankType.SPECIES, TaxonomicType.INCERTAE_SEDIS, null, false);
        NameKey key2 = result2.getNameKey();
        assertFalse(key1.equals(key2));
    }

    // Autonyms
    @Test
    public void testKeyEquals19() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Senecio glomeratus Desf. ex Poir. subsp. glomeratus", null, null, null, null, false);
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Senecio glomeratus Poir. subsp. glomeratus", "Poir.", null, null, null, false);
        NameKey key2 = result2.getNameKey();
        assertTrue(key1.isAutonym());
        assertTrue(key2.isAutonym());
        assertEquals(key1, key2);
    }

    // Escaped letters
    @Test
    public void testKeyEquals20() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Senecio \\glomeratus", "Poir.", null, null, null, false);
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Senecio glomeratus", "Poir.", null, null, null, false);
        NameKey key2 = result2.getNameKey();
        assertEquals(key1, key2);
    }

    @Test
    public void testKeyEquals21() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Senecio glomeratus", "\\(Poir\\.\\)", null, null, null, false);
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Senecio glomeratus", "(Poir.)", null, null, null, false);
        NameKey key2 = result2.getNameKey();
        assertEquals(key1, key2);
    }

    // Ampersands
    @Test
    public void testKeyEquals22() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Senecio glomeratus", "Poir.  and Labil", null, null, null, false);
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse(NomenclaturalClassifier.BOTANICAL, "Senecio glomeratus", "Poir. &  Labil", null, null, null, false);
        NameKey key2 = result2.getNameKey();
        assertEquals(key1, key2);
    }

    // Changed combination marker
    @Test
    public void testKeyEquals23() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(NomenclaturalClassifier.ZOOLOGICAL, "Osphranter rufus", "Desmarest, 1822", null, null, null, false);
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse(NomenclaturalClassifier.ZOOLOGICAL, "Osphranter rufus", "(Desmarest, 1822)", null, null, null, false);
        NameKey key2 = result2.getNameKey();
        assertEquals(key1, key2);
    }

    // Placeholder names
    @Test
    public void testKeyEquals24() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(NomenclaturalClassifier.ZOOLOGICAL, "Galaxias sp. 3", null, null, null, null, false);
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse(NomenclaturalClassifier.ZOOLOGICAL, "Galaxias sp 3", null, null, null, null, false);
        NameKey key2 = result2.getNameKey();
        assertEquals(key1, key2);
    }

    @Test
    public void testKeyEquals25() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(NomenclaturalClassifier.ZOOLOGICAL, "Galaxias sp. 3", null, null, null, null, false);
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse(NomenclaturalClassifier.ZOOLOGICAL, "Galaxias sp 3", null, null, null, null, true);
        NameKey key2 = result2.getNameKey();
        assertEquals(key1, key2);
    }

    // Initially quoted names
    @Test
    public void testKeyEquals26() throws Exception {
        NameAnalyser.AnalysisResult result1 = this.analyser.analyse(NomenclaturalClassifier.ZOOLOGICAL, "\"Hypomecis\" catephes", null, RankType.SPECIES, null, null, false);
        NameKey key1 = result1.getNameKey();
        NameAnalyser.AnalysisResult result2 = this.analyser.analyse(NomenclaturalClassifier.ZOOLOGICAL, "Hypomecis catephes", null, RankType.SPECIES, null, null, false);
        NameKey key2 = result2.getNameKey();
        assertNotEquals(key1, key2);
    }

    @Test
    public void testNames1() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICZN", "Abantiades ocellatus", "Tindale, 1932", "species");
        assertEquals("Abantiades", result.getMononomial());
        assertEquals("Abantiades", result.getGenus());
        assertEquals("ocellatus", result.getSpecificEpithet());
        assertNull(result.getInfraspecificEpithet());
        assertNull(result.getCultivarEpithet());
    }

    @Test
    public void testNames2() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICBN", "Plantae", "Haekel", "kingdom");
        assertEquals("Plantae", result.getMononomial());
        assertNull(result.getGenus());
        assertNull(result.getSpecificEpithet());
        assertNull(result.getInfraspecificEpithet());
        assertNull(result.getCultivarEpithet());
    }

    @Test
    public void testNames3() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICZN", "Chezala Subgroup 4", null, "genus");
        assertEquals("Chezala", result.getMononomial());
        assertEquals("Chezala", result.getGenus());
        assertEquals("Subgroup-4", result.getSpecificEpithet());
        assertNull(result.getInfraspecificEpithet());
        assertNull(result.getCultivarEpithet());
    }

    @Test
    public void testNames4() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICNAFP", "Convolvulus sect. Brewera", null, "genus");
        assertEquals("Convolvulus", result.getMononomial());
        assertEquals("Convolvulus", result.getGenus());
        assertNull(result.getSpecificEpithet());
        assertNull(result.getInfraspecificEpithet());
        assertNull(result.getCultivarEpithet());
    }

    @Test
    public void testNames5() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICNAFP", "Brachyscome 'Pilliga Posy'", null, null);
        assertEquals("Brachyscome", result.getMononomial());
        assertEquals("Brachyscome", result.getGenus());
        assertNull(result.getSpecificEpithet());
        assertNull(result.getInfraspecificEpithet());
        assertEquals("Pilliga Posy", result.getCultivarEpithet());
    }

    @Test
    public void testNames6() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICNAFP", "Eucalyptus caesia 'Silver Princess'", null, null);
        assertEquals("Eucalyptus", result.getMononomial());
        assertEquals("Eucalyptus", result.getGenus());
        assertEquals("caesia", result.getSpecificEpithet());
        assertNull(result.getInfraspecificEpithet());
        assertEquals("Silver Princess", result.getCultivarEpithet());
    }

    @Test
    public void testNames7() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICNAFP", "Waminoa cf. brickneri", null, "species");
        assertEquals("Waminoa", result.getMononomial());
        assertEquals("Waminoa", result.getGenus());
        assertEquals("brickneri", result.getSpecificEpithet());
        assertNull(result.getInfraspecificEpithet());
        assertNull( result.getCultivarEpithet());
    }

    @Test
    public void testNames8() throws Exception {
        NameAnalyser.AnalysisResult result = this.analyser.analyse("ICNAFP", "Acacia dealbata subalpina", null, "subspecies");
        assertEquals("Acacia", result.getMononomial());
        assertEquals("Acacia", result.getGenus());
        assertEquals("dealbata", result.getSpecificEpithet());
        assertEquals("subalpina", result.getInfraspecificEpithet());
        assertNull(result.getCultivarEpithet());
    }

    @Test
    public void testCanonicaliseCode1() throws Exception {
        NomenclaturalClassifier code = this.analyser.canonicaliseCode("ICZN");
        assertEquals(NomenclaturalClassifier.ZOOLOGICAL, code);
    }

    @Test
    public void testCanonicaliseCode2() throws Exception {
        NomenclaturalClassifier code = this.analyser.canonicaliseCode("FLUFFY");
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
