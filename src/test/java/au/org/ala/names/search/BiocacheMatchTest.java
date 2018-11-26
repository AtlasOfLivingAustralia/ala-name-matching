package au.org.ala.names.search;

import au.org.ala.names.model.*;
import org.gbif.api.vocabulary.NameType;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * The set of test associated with performing correct matches of biocache names
 * - includes tests for error/issues types
 * - matches based on higher classification etc...
 * <p/>
 * TODO Need to add more test cases to this class
 *
 * @author Natasha Carter
 */
public class BiocacheMatchTest {

    private static ALANameSearcher searcher;

    @org.junit.BeforeClass
    public static void init() throws Exception {
        searcher = new ALANameSearcher("/data/lucene/namematching-20181120");
    }

    @Test
    @Ignore
    public void testMatchHybrid(){
        try{
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName("Eucalyptus globulus x Eucalyptus ovata");
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertEquals("hybrid", metrics.getNameType().toString());
            assertEquals(RankType.SPECIES, metrics.getResult().getRank());

        } catch(Exception e){
            e.printStackTrace();
            fail("Exception should not occur");
        }
    }

    @Test
    public void higherClassificationProvided(){
        try{
            LinnaeanRankClassification cl= new LinnaeanRankClassification();
            cl.setScientificName("Osphranter rufus");
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertEquals("Animalia", metrics.getResult().getRankClassification().getKingdom());
            assertEquals("Osphranter", metrics.getResult().getRankClassification().getGenus());

        } catch(Exception e){
            e.printStackTrace();
            fail("Exception should not occur");
        }
    }

    @Test
    public void synonymHomonymIssue(){
        try{
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName("Codium sp.");
            cl.setGenus("Codium");
            cl.setFamily("Alga");
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertTrue("Homonyms exception should have been detected", metrics.getErrors().contains(ErrorType.HOMONYM));

        } catch (Exception e){
            e.printStackTrace();
            fail("Exception should not occur");
        }
    }

    @Test
    public void testRecursiveAuthorshipIssue() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName("Graphis notreallyaname Mull.Arg.");
            cl.setAuthorship("Mull.Arg.");
            cl.setKingdom("Animalia");
            cl.setGenus("Graphis");
            cl.setSpecificEpithet("notreallyaname");
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:934c68e8-1a64-49ff-b89e-e275b93043af", metrics.getResult().getLsid()); // Graphis from AFD
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception should not occur");
        }
    }

    @Test
    public void testRecursiveAuthorshipIssue2() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName("Graphis notreallyaname Mull.Arg.");
        cl.setAuthorship("Mull.Arg.");
        cl.setFamily("Graphidaceae");
        cl.setGenus("Graphis");
        cl.setSpecificEpithet("notreallyaname");
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertEquals("NZOR-6-122770", metrics.getResult().getLsid()); // Can't find Graphis since not APC placed so gets Graphidaceae
    }

    @Test
    @Ignore // TODO FInd a suitable x-rank
    public void testCrossRankHomonym() throws Exception {
        //test unresolved
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName("Blattidae");
        MetricsResultDTO metrics =searcher.searchForRecordMetrics(cl, true);
        assertTrue("Failed the cross rank homonym test",metrics.getErrors().contains(ErrorType.HOMONYM));
        //test resolved based on rank
        cl.setRank("genus");
        metrics = searcher.searchForRecordMetrics(cl, true);
        assertFalse("Cross rank homonym should have been resolved",metrics.getErrors().contains(ErrorType.HOMONYM));
        //test resolved based on rank being determined
        cl.setRank(null);
        cl.setPhylum("Arthropoda");
        metrics = searcher.searchForRecordMetrics(cl, true);
        assertFalse("Cross rank homonym should have been resolved",metrics.getErrors().contains(ErrorType.HOMONYM));
    }

    // @Test
    public void testTibicentibicen() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName("Tibicen tibicen");
            //don't want Tibicen tibicen to match to Tibicen (?) blah
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertNull("Result should be null: " + metrics.getResult(), metrics.getResult());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSPNovName() {
        try {
            String name = "Eremophila sp.nov.";
            String genus = "Eremophila";
            String family = "Myoporaceae";
            String spEp = "sp.nov.";
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName(name);
            cl.setGenus(genus);
            cl.setFamily(family);
            cl.setSpecificEpithet(spEp);
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            //System.out.println(metrics.getResult());
            assertEquals("http://id.biodiversity.org.au/instance/apni/884433", metrics.getResult().getLsid());
            assertTrue(metrics.getErrors().contains(ErrorType.HOMONYM));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAlternatePhraseName() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName("Senna form taxon 'petiolaris'");
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            System.out.println(metrics);
        } catch (Exception e) {

        }
    }

    @Test
    public void genericIssueTest() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setAuthorship("L.");
            cl.setScientificName("Echium vulgare L.");
            cl.setKlass("Equisetopsida");
            cl.setPhylum("Streptophyta");
            cl.setKingdom("Plantae");
            cl.setGenus("EcHium");
            cl.setOrder("[Boraginales]");
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertEquals("http://id.biodiversity.org.au/node/apni/2895788", metrics.getResult().getLsid());
            //System.out.println(metrics);
            //System.out.println(metrics.getLastException());
            //System.out.println(metrics.getErrors());
            //System.out.println(metrics.getResult());
            //metrics.getLastException().printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testHomonym() {
        try {
            NameSearchResult nsr = searcher.searchForRecord("Agathis", null);
            fail("Expected homonym exception, got" + nsr);
        } catch (Exception e) {
            assertTrue(e instanceof HomonymException);
        }
    }

    @Test
    public void testMisappliedNames1() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName("Tephrosia savannicola");
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertTrue(metrics.getErrors().contains(ErrorType.MATCH_MISAPPLIED));
        assertEquals("http://id.biodiversity.org.au/node/apni/2894621", metrics.getResult().getLsid());
    }

    // Should go to higher taxon because there is no accepted match
    @Test
    public void testMisappliedNames2() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName("Myosurus minimus");
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertTrue(metrics.getErrors().contains(ErrorType.MISAPPLIED));
        assertEquals("http://id.biodiversity.org.au/node/apni/2896644", metrics.getResult().getLsid());
    }

    @Test
    public void testAuthorsProvidedInName() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName("Acanthastrea bowerbanki Edwards & Haime, 1857");
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertEquals(NameType.SCIENTIFIC, metrics.getNameType());
    }
    //test this one for a bunchof homonyms that are synonyms of another concept...
    //Acacia retinodes

    // Case where we have what should be a parent-child synonym but the spelling of the
    // parent name and the synonym name are different (usually because of a subgenus name)
    @Test
    public void testParentChildWithDifferentSpelling1() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();

        cl.setScientificName("Climacteris affinis");
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:b78847ca-c2dc-4fb5-9322-0dcd60e87196", metrics.getResult().getLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType()); // Dereferenced synonym
        assertTrue(metrics.getErrors().contains(ErrorType.PARENT_CHILD_SYNONYM));
    }

    @Test
    public void testParentChildWithDifferentSpelling2() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();

        cl.setScientificName("Limnodynastes dumerilii");
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:2c50c2f6-7a0d-44e1-b549-458427b420c4", metrics.getResult().getLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType()); // Dereferenced synonym
        assertTrue(metrics.getErrors().contains(ErrorType.PARENT_CHILD_SYNONYM));
    }

    @Test
    public void testAffCfSpecies1() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();

        // No issues
        cl.setScientificName("Zabidius novemaculeatus");
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:58e06bba-de3b-4c8c-b165-d75bbeb21a36", metrics.getResult().getLsid());
        assertTrue(metrics.getErrors().contains(ErrorType.NONE));

        cl = new LinnaeanRankClassification();
        cl.setScientificName("Acacia aff. retinodes");
        metrics = searcher.searchForRecordMetrics(cl, true);
        //aff. species need to match to the genus
        assertTrue(metrics.getErrors().contains(ErrorType.AFFINITY_SPECIES));
        assertEquals(RankType.GENUS, metrics.getResult().getRank());
    }

    @Test
    public void testAffCfSpecies2() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName("Acacia aff.");
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        //aff. species need to match to the genus
        assertTrue(metrics.getErrors().contains(ErrorType.AFFINITY_SPECIES));
    }

    @Test
    public void testAffCfSpecies3() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName("Acanthastrea cf. bowerbanki Edwards & Haime, 1857");
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertTrue(metrics.getErrors().contains(ErrorType.CONFER_SPECIES));
        assertEquals(RankType.GENUS, metrics.getResult().getRank());
    }

    @Test
    public void testQuestionSpecies() {
        String name = "Lepidosperma ? sp. Mt Short TESTS (S. Kern et al. LCH 17510)";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        try {
            //test a one where the species does not exists so that the higher level match can be tested
            //ensures that higher matches work in this case
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertEquals(NameType.DOUBTFUL, metrics.getNameType());
            assertTrue(metrics.getErrors().contains(ErrorType.QUESTION_SPECIES));
            assertEquals(MatchType.RECURSIVE, metrics.getResult().getMatchType());
            //Cardium media ?
            cl = new LinnaeanRankClassification();
            cl.setScientificName("Macropus rufus ?");
            metrics = searcher.searchForRecordMetrics(cl, true);
            assertEquals(NameType.DOUBTFUL, metrics.getNameType());
            assertTrue(metrics.getErrors().contains(ErrorType.QUESTION_SPECIES));
            assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
            cl = new LinnaeanRankClassification();
            cl.setScientificName("Macropus ?");

            //testing an empty result with errors on mname
            metrics = searcher.searchForRecordMetrics(cl, true);
            assertTrue(metrics.getErrors().contains(ErrorType.HOMONYM));
            assertTrue(metrics.getErrors().contains(ErrorType.QUESTION_SPECIES));
            assertTrue(metrics.getResult() == null);

            //System.out.println(metrics);
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should occur");
        }
    }


    // See https://github.com/AtlasOfLivingAustralia/ala-name-matching/issues/1
    @Test
    public void testSubSpeciesMarker1()  {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            String name = "Asparagus asparagoides (NC)";
            cl.setScientificName(name);
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertEquals("http://id.biodiversity.org.au/node/apni/2895458", metrics.getResult().getLsid());
            assertEquals(MatchType.CANONICAL, metrics.getResult().getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }
    // See https://github.com/AtlasOfLivingAustralia/ala-name-matching/issues/1
    @Test
    public void testSubSpeciesMarker2()  {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            String name = "Asparagus asparagoides f. asparagoides";
            cl.setScientificName(name);
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertEquals("http://id.biodiversity.org.au/node/apni/2895458", metrics.getResult().getLsid());
            assertEquals(MatchType.RECURSIVE, metrics.getResult().getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    // See https://github.com/AtlasOfLivingAustralia/ala-name-matching/issues/1
    // At the moment, not able to correctly parse this out
    @Ignore
    @Test
    public void testSubSpeciesMarker3()  {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            String name = "Asparagus asparagoides f. Western Cape (R.Taplin 1133)";
            cl.setScientificName(name);
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertEquals("http://id.biodiversity.org.au/node/apni/2895458", metrics.getResult().getLsid());
            assertEquals(MatchType.CANONICAL, metrics.getResult().getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }
    // See https://github.com/AtlasOfLivingAustralia/ala-name-matching/issues/1
    @Test
    public void testSubSpeciesMarker4()  {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            String name = "Asparagus asparagoides f.";
            cl.setScientificName(name);
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertEquals("http://id.biodiversity.org.au/node/apni/2895458", metrics.getResult().getLsid());
            assertEquals(MatchType.RECURSIVE, metrics.getResult().getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    // See https://github.com/AtlasOfLivingAustralia/ala-name-matching/issues/1
    @Test
    public void testSubSpeciesMarker5()  {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            String name = "Asparagus asparagoides (L.) Druce f. asparagoides";
            cl.setScientificName(name);
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertEquals("http://id.biodiversity.org.au/node/apni/2895458", metrics.getResult().getLsid());
            assertEquals(MatchType.RECURSIVE, metrics.getResult().getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    // See https://github.com/AtlasOfLivingAustralia/ala-name-matching/issues/10
    @Test
    public void testSubSpeciesMarker6()  {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            String name = "Salvia verbenaca var.";
            cl.setScientificName(name);
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertEquals("http://id.biodiversity.org.au/node/apni/2887555", metrics.getResult().getLsid());
            assertEquals(MatchType.RECURSIVE, metrics.getResult().getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    // See https://github.com/AtlasOfLivingAustralia/ala-name-matching/issues/10
    @Test
    public void testSubSpeciesMarker7()  {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            String name = "Eucalyptus leucoxylon ssp.";
            cl.setScientificName(name);
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertEquals("http://id.biodiversity.org.au/node/apni/2909698", metrics.getResult().getLsid());
            assertEquals(MatchType.RECURSIVE, metrics.getResult().getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    // See https://github.com/AtlasOfLivingAustralia/ala-name-matching/issues/5
    @Test
    public void testHybrid1()  {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            String name = "Correa reflexa (Labill.) Vent. hybrid";
            cl.setScientificName(name);
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertEquals("http://id.biodiversity.org.au/node/apni/2893483", metrics.getResult().getLsid());
            assertEquals("Correa reflexa", metrics.getResult().getRankClassification().getScientificName());
            assertEquals(MatchType.RECURSIVE, metrics.getResult().getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    @Test
    public void testDingo1()  {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            String name = "Canis lupus dingo";
            cl.setScientificName(name);
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertNotNull(metrics);
            assertEquals("urn:lsid:biodiversity.org.au:afd.name:3064f20b-f6de-4375-8377-904cbd6cf9fa", metrics.getResult().getLsid());
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:c2056f1b-fcde-45b9-904b-1cab280368d1", metrics.getResult().getAcceptedLsid());
            assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

}
