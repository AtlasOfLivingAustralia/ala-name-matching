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
    public static void init() {
        try {
            searcher = new ALANameSearcher("/data/lucene/namematching");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        assertEquals("NZOR-4-120184", metrics.getResult().getLsid()); // Can't find Graphis since not APC placed so gets Graphidaceae
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
            assertEquals("NZOR-4-56674", metrics.getResult().getLsid());
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
            searcher.searchForRecord("Terebratella", null);
            fail("Expected homonym exception");
        } catch (Exception e) {
            assertTrue(e instanceof HomonymException);
        }
    }

    @Test
    public void commonName() {
        try {
            //System.out.println(searcher.searchForCommonName("Red Kangaroo"));
            // searcher.searchForLSID("Centropogon australis");
            searcher.searchForRecord("Dexillus muelleri", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMisappliedNames() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName("Tephrosia savannicola");
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertTrue(metrics.getErrors().contains(ErrorType.MATCH_MISAPPLIED));
            assertEquals("http://id.biodiversity.org.au/node/apni/2894621", metrics.getResult().getLsid());
            cl = new LinnaeanRankClassification();
            cl.setScientificName("Myosurus minimus");
            metrics = searcher.searchForRecordMetrics(cl, true);
            assertTrue(metrics.getErrors().contains(ErrorType.MATCH_MISAPPLIED));
            assertEquals("NZOR-4-91924", metrics.getResult().getLsid());
        } catch (Exception e) {
            fail("No exception shoudl occur");
            e.printStackTrace();
        }
    }

    @Test
    public void testAuthorsProvidedInName() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName("Acanthastrea bowerbanki Edwards & Haime, 1857");
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertEquals(NameType.SCIENTIFIC, metrics.getNameType());
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should  occur");
        }
    }
    //test this one for a bunchof homonyms that are synonyms of another concept...
    //Acacia retinodes

    @Test
    public void testAffCfSpecies() {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        try {

            cl.setScientificName("Zabidius novemaculeatus");
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            System.out.println(metrics);


            cl.setScientificName("Climacteris affinis");
            //this on should match with parent child synonym issue
            metrics = searcher.searchForRecordMetrics(cl, true);
            assertTrue(metrics.getErrors().contains(ErrorType.PARENT_CHILD_SYNONYM));
            cl = new LinnaeanRankClassification();
            cl.setScientificName("Acacia aff. retinodes");
            metrics = searcher.searchForRecordMetrics(cl, true);
            //aff. species need to match to the genus
            assertTrue(metrics.getErrors().contains(ErrorType.AFFINITY_SPECIES));
            assertEquals(RankType.GENUS, metrics.getResult().getRank());
            cl.setScientificName("Acacia aff.");
            metrics = searcher.searchForRecordMetrics(cl, true);
            //aff. species need to match to the genus
            assertTrue(metrics.getErrors().contains(ErrorType.AFFINITY_SPECIES));
            cl = new LinnaeanRankClassification();
            cl.setScientificName("Acanthastrea cf. bowerbanki Edwards & Haime, 1857");
            metrics = searcher.searchForRecordMetrics(cl, true);
            assertTrue(metrics.getErrors().contains(ErrorType.CONFER_SPECIES));
            assertEquals(RankType.GENUS, metrics.getResult().getRank());
        } catch (Exception e) {
            fail("No excpetion shoudl occur");
        }
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
    public void tesDingo1()  {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            String name = "Canis lupus dingo";
            cl.setScientificName(name);
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:c2056f1b-fcde-45b9-904b-1cab280368d1", metrics.getResult().getAcceptedLsid());
            assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

}
