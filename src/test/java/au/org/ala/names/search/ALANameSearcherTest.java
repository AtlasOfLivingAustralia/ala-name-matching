

package au.org.ala.names.search;

import au.org.ala.names.model.LinnaeanRankClassification;
import au.org.ala.names.model.MatchType;
import au.org.ala.names.model.NameSearchResult;
import au.org.ala.names.model.RankType;
import org.gbif.ecat.model.ParsedName;
import org.gbif.ecat.parser.NameParser;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Natasha, Tommy
 */
public class ALANameSearcherTest {
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
    public void testMisappliedNames1() {
        try {
            //test to ensure that a misapplied name also .
            String lsid = searcher.searchForLSID("Corybas macranthus");
            fail("A misapplied exception should be thrown");
            //assertEquals("urn:lsid:biodiversity.org.au:apni.taxon:549612",lsid);
        } catch (MisappliedException ex) {
            assertEquals("http://id.biodiversity.org.au/node/apni/2915977", ex.getMatchedResult().getLsid());
            assertNull(ex.getMisappliedResult());
        } catch (Exception ex) {
            fail("No other exceptions should occur, got " + ex);
        }
        //test a misapplied name that does not have an accepted concept
        //No misapplied names in current dataset without accepted concepts
        /*
        try {
            String lsid = searcher.searchForLSID("Myosurus minimus");
            fail("a misapplied expcetption shoudl be thrown.");
        } catch (MisappliedException e) {
            assertEquals("urn:lsid:biodiversity.org.au:apni.taxon:303525", e.getMatchedResult().getLsid());
            assertTrue(e.getMisappliedResult() == null);
        } catch (Exception e) {
            fail("no other exceptions should occur.");
        }
        */
    }

    @Test
    public void testMisappliedNames2() {
        try {
            //test to ensure that the accepted name is returned when it also exists as a misapplied name.
            String lsid = searcher.searchForLSID("Bertya rosmarinifolia");
            fail("A misapplied exception should be thrown, got " + lsid);
        } catch (MisappliedException ex) {
            assertEquals("http://id.biodiversity.org.au/node/apni/2893214", ex.getMatchedResult().getLsid());
            assertEquals("http://id.biodiversity.org.au/node/apni/2898349", ex.getMisappliedResult().getLsid());
        } catch (Exception ex) {
            fail("No other exceptions should occur, got " + ex);
        }
    }

    @Test
    public void testMisappliedNames3()  {
        try {
            String name = "Scleroderma aurantium (L. : Pers.) Pers.";
            NameSearchResult nsr = searcher.searchForRecord(name);
            fail("Expecting misapplied exception");
            assertNotNull(nsr);
        } catch (MisappliedException ex) {
            assertEquals("690bcf05-149f-4ab9-82c0-a2746e4d0bcb", ex.getMatchedResult().getLsid());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    @Test
    public void testSynonymAsHomonym1() {
        try {
            String lsid = searcher.searchForLSID("Terebratella");
            fail("This test should throw a homonym for a matched synonym");
        } catch (HomonymException ex) {
            // Correct behaviour
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    @Test
    public void testSynonymAsHomonym2() {
        try {
            String lsid = searcher.searchForLSID("ISOPTERA", RankType.ORDER);
            assertTrue(lsid != null);
        } catch (HomonymException ex) {
            fail("When supplied with a higher order rank no homonym exception should be thrown");
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }


    @Test
    public void catchAllSpeciesTest() {

        String name = "sp";
        try {
            String lsid = searcher.searchForLSID(name);
            fail("A rank marker should not match to a name");
        } catch (Exception e) {
            assertEquals("Supplied scientific name is a rank marker.", e.getMessage());
        }
    }

    @Ignore
    public void nullPointerExceptionInAuthorTest() {
        String name = "Sphacelaria Lynbye";
        try {
            String lsid = searcher.searchForLSID(name);
            assertNull("Whoops found lsid " + lsid, lsid);
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception shoul occur");
        }
    }

    @Test
    public void parserBlackList() {
        //Bettongia lesueur unnamed subsp. - this name should NOT throw a NPE
        String name = "Bettongia lesueur unnamed subsp.";
        try {
            String lsid = searcher.searchForLSID(name, true);
            assertNotNull(lsid);
            assertEquals("ALA_Bettongia_lesueur_unnamed_subsp.", lsid);
        } catch (Exception e) {
            e.printStackTrace();
            fail("No Exception should occur.");
        }
    }

    @Test
    public void testRecursiveSearch() {
        String name = "Varanus timorensis";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        cl.setGenus("Varanus");
        cl.setFamily("Varanidae");
        cl.setSpecificEpithet("timorensis");
        try {
            NameSearchResult nsr = searcher.searchForRecord(cl, true, true);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:3309bb2e-5b3f-4664-977b-147e60b66109", nsr.getLsid());
            System.out.println(nsr);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testSpeciesSplitSynonym() {
        String name = "Corvus orru'";
        try {
            List<NameSearchResult> results = searcher.searchForRecords(name, null, true);
            fail("An exception should have been thrown");
        } catch (Exception e) {
            assertTrue(e instanceof ParentSynonymChildException);
            ParentSynonymChildException psce = (ParentSynonymChildException) e;
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:32216e12-b8e8-48d9-8a2c-193633745762", psce.getParentResult().getLsid());
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:f8efc550-d7c1-44c9-9d16-787d50c8e857", psce.getChildResult().getLsid());
        }

    }

    @Test
    public void testEmbeddedRankMarker() {
        String name = "Flueggea virosa subsp. melanthesoides";
        try {
            String lsid = searcher.searchForLSID(name, true);
            assertEquals("http://id.biodiversity.org.au/node/apni/2893899", lsid);
        } catch (Exception e) {
            fail("An exception shoudl not occur in embedded rank marker");
            e.printStackTrace();
        }
    }

    @Test
    public void testExcludedNames() {
        String afdExcludedName = "Cyrtodactylus louisiadensis";
        try {
            searcher.searchForLSID(afdExcludedName);
        } catch (Exception e) {
            assertTrue(e instanceof ExcludedNameException);
            ExcludedNameException ene = (ExcludedNameException) e;
            assertEquals("urn:lsid:biodiversity.org.au:afd.name:450112", ene.getExcludedName().getLsid());
        }

        String apcExcludedName = "Parestia elegans";
        try {
            searcher.searchForLSID(apcExcludedName);
        } catch (Exception e) {
            assertTrue(e instanceof ExcludedNameException);
            ExcludedNameException ene = (ExcludedNameException) e;
            assertTrue(ene.getNonExcludedName() == null);
        }
        //Test to ensure that Exception identifies a non excluded version of a name (if one exists).
        apcExcludedName = "Callistemon pungens";
        try {
            searcher.searchForLSID(apcExcludedName);
        } catch (Exception e) {
            assertTrue(e instanceof ExcludedNameException);
            ExcludedNameException ene = (ExcludedNameException) e;
            assertEquals("urn:lsid:biodiversity.org.au:apni.taxon:445680", ene.getNonExcludedName().getLsid());
        }
    }

    @Test
    public void testHomonymsWithAuthorResolution() {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        NameSearchResult nsr = null;
        cl.setScientificName("Thalia");
        try {
            nsr = searcher.searchForRecord("Thalia", null, true);
            fail("Thalia should throw a homonym without kingdom or author");
        } catch (Exception e) {
            assertTrue(e instanceof HomonymException);
        }
        cl.setKingdom("Animalia");
        cl.setPhylum("Chordata");
        try {
            nsr = searcher.searchForRecord(cl, false);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:52c68649-47d5-4f2e-9730-417fc54fb080", nsr.getLsid());
        } catch (Exception e) {
            fail("Homonym should be resolved via the Kingdom");
        }
        cl.setKingdom(null);
        cl.setPhylum(null);
        cl.setAuthorship("Blumenbach, 1798");
        try {
            nsr = searcher.searchForRecord(cl, false);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:52c68649-47d5-4f2e-9730-417fc54fb080", nsr.getLsid());
        } catch (Exception e) {
            fail("Author should identify homonym value to use");
        }

    }

    @Test
    public void testBiocacheName() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName("Darwinia acerosa?");
            cl.setKingdom("Plantae");
            NameSearchResult nsr = searcher.searchForRecord(cl, true, true);
            System.out.println(nsr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testsStrMarker1(){
        try {
            NameSearchResult nsr;
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setKingdom("Plantae");
            cl.setGenus("Test");
            cl.setScientificName("Macropus rufus");
            nsr = searcher.searchForRecord(cl, true);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:31a9b8b8-4e8f-4343-a15f-2ed24e0bf1ae", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }

    @Test
    public void testsStrMarker2()  {
        try {
            String name = "Pterodroma arminjoniana s. str.";
            NameSearchResult nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("ALA_Pterodroma_arminjoniana_s._str.", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
     }

    @Test
    public void testsStrMarker3() {
        try {
            String name = "Stennella longirostris longirostris";
            NameSearchResult nsr = searcher.searchForRecord(name, null, true);
            assertNotNull(nsr);
            assertEquals("ALA_Stennella_longirostris_longirostris", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }

    @Test
    public void testsStrMarker4() {
        try {
            String name = "Aplonis fusca hulliana";
            NameSearchResult nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:76925a5b-be3b-4e5d-b800-9e3a555b8800", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }

    @Test
    public void testsStrMarker5() {
        try {
            String name = "Cryphaea tenella";
            NameSearchResult nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("e7f84546-9a40-4270-84b2-347d56e47642", nsr.getLsid());
            assertEquals(name, nsr.getRankClassification().getScientificName());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }

    @Test
    public void testsStrMarker6() {
        try {
            String name = "Grevillea 'White Wings'";
            NameSearchResult nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("http://id.biodiversity.org.au/name/apni/163801", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }
    @Test
    public void testsStrMarker7() {
        try {
            // This is 'blacklisted' but the blacklist is ignored by the DwCA loader
            String name = "Siganus nebulosus";
            NameSearchResult nsr = searcher.searchForRecord(name, null, true);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.name:595794", nsr.getLsid());
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:0aa9653f-00c7-42b9-896b-f399103703b8", nsr.getAcceptedLsid());

        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }
    @Test
    public void testsStrMarker8() {
        try {
            String name = "Anabathron contabulatum";
            NameSearchResult nsr = searcher.searchForRecord(name, null, true);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:1672efcd-fb3c-44c8-a124-0df5aade86be", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }

    @Test
    public void testSpeciesConstructFromClassification() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();

        } catch (Exception e) {

        }
    }

    @Test
    public void testQuestionSpeciesMatch() {
        try {
            String name = "Corymbia ?hendersonii K.D.Hill & L.A.S.Johnson";
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setKingdom("Plantae");
            cl.setScientificName(name);
            NameSearchResult nsr = searcher.searchForRecord(cl, true);
            assertEquals("http://id.biodiversity.org.au/node/apni/2891261", nsr.getLsid());
            //assertEquals(ErrorType.QUESTION_SPECIES, nsr.getErrorType());
            System.out.println(nsr);
            name = "Cacatua leadbeateri";
            //name = "Acacia bartleana ms";

            //test the "name based" synonym "has generic combination"
            nsr = searcher.searchForRecord("Cacatua leadbeateri", null);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:1365807d-927b-4219-97bf-7e619afa5f72", nsr.getAcceptedLsid());

            name = "Zieria smithii";
            nsr = searcher.searchForRecord(name, null);
            //Cycas media subsp. banksii - C.media subsp. media
            //Boronia crenulata subsp. crenulata var. angustifolia
            //Dendrobium kingianum subsp. kingianum
            //Dendrobium speciosum subsp. capricornicum
            //Dendrobium speciosum subsp. grandiflorum
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSpMarker1()  {
        try {
            String name = "Thelymitra sp. adorata";
            NameSearchResult nsr = null;
            nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            // Either one can match
            assertTrue("http://id.biodiversity.org.au/name/apni/190511".equals(nsr.getLsid()) || "http://id.biodiversity.org.au/name/apni/233691".equals(nsr.getLsid()));
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testSpMarker2()  {
        try {
            String name = "Grevillea brachystylis subsp. Busselton (G.J.Keighery s.n. 28/8/1985)";
            NameSearchResult nsr = null;
            nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("http://id.biodiversity.org.au/instance/apni/897499", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testSpMarker3()  {
        try {
            String name = "Pterodroma arminjoniana s. str.";
            NameSearchResult nsr = null;
            nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("ALA_Pterodroma_arminjoniana_s._str.", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testSpMarker4()  {
        try {
            String name = "Acacia dealbata subsp. subalpina";
            NameSearchResult nsr = null;
            nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("http://id.biodiversity.org.au/node/apni/2911757", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testSpMarker5()  {
        try {
            String name = "Grevillea brachystylis subsp. Busselton";
            NameSearchResult nsr = null;
            nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("http://id.biodiversity.org.au/instance/apni/897499", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testPhraseMatch1() {
        try {
            String name = "Elaeocarpus sp. Rocky Creek";
            NameSearchResult nsr = null;
            nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("http://id.biodiversity.org.au/node/apni/2916168", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testPhraseMatch2() {
        try {
            String name = "Elaeocarpus sp. Rocky Creek (Hunter s.n., 16 Sep 1993)";
            NameSearchResult nsr = null;
            nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("http://id.biodiversity.org.au/instance/apni/871103", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testPhraseMatch3() {
        try {
            String name = "Pultenaea sp. Olinda (R.Coveny 6616)";
            NameSearchResult nsr = null;
            nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("http://id.biodiversity.org.au/node/apni/2886985", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testPhraseMatch4() {
        try {
            String name = "Thelymitra sp. adorata";
            NameSearchResult nsr = null;
            nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("http://id.biodiversity.org.au/name/apni/233691", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testPhraseMatch5() {
        try {
            String name = "Asterolasia sp. \"Dungowan Creek\"";
            NameSearchResult nsr = null;
            nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("http://id.biodiversity.org.au/node/apni/2898916", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }


    @Test
    public void testSynonymWithoutRank() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setKingdom("Animalia");
            cl.setScientificName("Gymnorhina tibicen");
            NameSearchResult nsr = searcher.searchForRecord(cl, true, true);
            assertEquals("Gymnorhina tibicen", nsr.getRankClassification().getScientificName());
            assertEquals("(Latham, 1801)", nsr.getRankClassification().getAuthorship());
            nsr = searcher.searchForRecord("Cracticus tibicen", RankType.SPECIES);
            assertEquals("Cracticus tibicen", nsr.getRankClassification().getScientificName());
            nsr = searcher.searchForRecord("Cracticus tibicen", RankType.GENUS);
            assertEquals(null, nsr);
        } catch (Exception e) {

        }
    }

    @Test
    public void testRecordSearchWithoutScientificName() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification(null, null, null, "Hemiptera", "Pentatomidae", null, null);
            System.out.println(searcher.searchForRecord(cl, true));
            System.out.println("Lilianae::: " + searcher.searchForRecord("Lilianae", null));
            System.out.println("Leptospermum: " + searcher.searchForRecord("Leptospermum", null));
            //searcher.searchForLSID("Pulex (Pulex)");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInfragenricAndSoundEx1() {
        String nameDifferentEnding = "Phylidonyris pyrrhopterus";
        try {
            NameSearchResult nsr = searcher.searchForRecord(nameDifferentEnding, null, true);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:28fe15f4-ea77-482e-9bfc-cbd14ade35cf", nsr.getLsid());
            assertEquals(MatchType.SOUNDEX, nsr.getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testInfragenricAndSoundEx2() {
        String nameWithInfraGenric = "Phylidonyris (Phylidonyris) pyrrhoptera (Latham, 1801)";
        try {
            NameSearchResult nsr = searcher.searchForRecord(nameWithInfraGenric, null, true);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:28fe15f4-ea77-482e-9bfc-cbd14ade35cf", nsr.getLsid());
            assertEquals(MatchType.EXACT, nsr.getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testInfragenricAndSoundEx3() {
        String nameDiffEndInfraGeneric = "Phylidonyris (Phylidonyris) pyrrhopterus";
        try {
            NameSearchResult nsr = searcher.searchForRecord(nameDiffEndInfraGeneric, null, true);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:28fe15f4-ea77-482e-9bfc-cbd14ade35cf", nsr.getLsid());
            assertEquals(MatchType.SOUNDEX, nsr.getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testInfragenricAndSoundEx4() {
        String name = "Latrodectus hasseltii";
        try {
            NameSearchResult nsr = searcher.searchForRecord(name, null, true);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:10fdfc12-a6d2-42ea-b9fa-2492cf78b0dc", nsr.getLsid());
            assertEquals(MatchType.EXACT, nsr.getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testInfragenricAndSoundEx5() {
        String name = "Latrodectus hasselti";
        try {
            NameSearchResult nsr = searcher.searchForRecord(name, null, true);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:10fdfc12-a6d2-42ea-b9fa-2492cf78b0dc", nsr.getLsid());
            assertEquals(MatchType.SOUNDEX, nsr.getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testInfragenricAndSoundEx6() {
        String name = "Elseya belli";
        try {
            NameSearchResult nsr = searcher.searchForRecord(name, null, true);
            assertNotNull(nsr);
            assertEquals("ALA_Elseya_belli", nsr.getLsid());
            assertEquals(MatchType.EXACT, nsr.getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testInfragenricAndSoundEx7() {
        String name = "Grevillea brachystyliss";
        try {
            NameSearchResult nsr = searcher.searchForRecord(name, null, true);
            assertNotNull(nsr);
            assertEquals("http://id.biodiversity.org.au/node/apni/2901342", nsr.getLsid());
            assertEquals(MatchType.SOUNDEX, nsr.getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }
    @Test
    public void testInfragenricAndSoundEx8() {
        String name = "Prostanthera sp. Bundjalung Nat. Pk.";
        try {
            NameSearchResult nsr = searcher.searchForRecord(name, null, true);
            assertNotNull(nsr);
            assertEquals("http://id.biodiversity.org.au/instance/apni/913279", nsr.getLsid());
            assertEquals(MatchType.PHRASE, nsr.getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSoundExMatch1() {
        String name = "Argyrotegium nitidulus";
        try {
            NameSearchResult nsr = searcher.searchForRecord(name, null, true);
            assertNotNull(nsr);
            assertEquals("http://id.biodiversity.org.au/node/apni/2918399", nsr.getLsid());
            assertEquals(MatchType.SOUNDEX, nsr.getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSoundExMatch2() {
        String name = "Globigerinoides tennellus";
        try {
            NameSearchResult nsr = searcher.searchForRecord(name, null, true);
            assertNotNull(nsr);
            assertEquals("CoL:25498086", nsr.getLsid());
            assertEquals(MatchType.SOUNDEX, nsr.getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    //@Test
    public void testSubspeciesSynonym() {
        try {
            String name = "Turnix castanota magnifica";
            System.out.println(searcher.searchForRecord(name, null));
            System.out.println(searcher.searchForRecord("Baeckea sp. Baladjie (PJ Spencer 24)", null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //@Test
    public void testPhraseNames() {
        //All the names below need to map to the same concept
        try {
            String name1 = "Goodenia sp. Bachsten Creek (M.D. Barrett 685) WA Herbarium";
            String name2 = "Goodenia sp. Bachsten Creek (M.D.Barrett 685) WA Herbarium";
            String name3 = "Goodenia sp. Bachsten Creek";
            String name4 = "Goodenia sp. Bachsten Creek (M.D. Barrett 685)";
            String name5 = "Goodenia sp. Bachsten Creek M.D. Barrett 685";
            NameParser parser = new NameParser();
            ParsedName cn = parser.parse(name1);
            System.out.println(cn + "##" + cn.canonicalName());
            cn = parser.parse(name1);
            System.out.println(cn);
            System.out.println(parser.parse("Macropus sp. rufus"));
            System.out.println(parser.parse("Macropus rufus subsp. rufus"));
            System.out.println(parser.parse("Allocasuarina spinosissima subsp. Short spine (D.L.Serventy & A.R.Main s.n., 25 Aug. 1960) WA Herbarium"));

            System.out.println(searcher.searchForRecord(name1, null));

            System.out.println(searcher.searchForRecord("Baeckea sp. Bungalbin Hill (BJ Lepschi, LA Craven 4586)", null));

            System.out.println(searcher.searchForRecord("Baeckea sp. Calingiri (F Hort 1710)", null));

            System.out.println(searcher.searchForRecord("Baeckea sp. Calingiri (F Hort 1710)", null));

            System.out.println(searcher.searchForRecord("Acacia sp. Goodlands (BR Maslin 7761) [aff. resinosa]", null));

            System.out.println(searcher.searchForRecord("Acacia sp. Manmanning (BR Maslin 7711) [aff. multispicata]", null));

            //System.out.println(parser.parseExtended(name1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNoRank() {
        try {
            String lsid = searcher.searchForLSID("Animalia");
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:4647863b-760d-4b59-aaa1-502c8cdf8d3c", lsid);
            lsid = searcher.searchForLSID("Bacteria");
            assertEquals("CAAB:72000000", lsid);
        } catch (SearchResultException e) {
            e.printStackTrace();
            fail("testNoRank failed");
        }
    }

    //@Test // TODO No additional reallsid currently used
    public void testGetPrimaryLsid() {
        String primaryLsid = searcher.getPrimaryLsid("http://id.biodiversity.org.au/node/apni/2889838");
        assertEquals("http://id.biodiversity.org.au/node/apni/2889838", primaryLsid);
        primaryLsid = searcher.getPrimaryLsid("http://id.biodiversity.org.au/instance/apni/887198");
        assertEquals("http://id.biodiversity.org.au/node/apni/5487102", primaryLsid);
    }

    @Test
    public void testSearchForRecordByLsid() {
        String lsid = "http://id.biodiversity.org.au/instance/apni/885617";
        NameSearchResult nsr = searcher.searchForRecordByLsid(lsid);
        assertNotNull(nsr);
        assertEquals(lsid, nsr.getLsid());
    }

    private void printAllResults(String prefix, List<NameSearchResult> results) {
        System.out.println("## " + prefix + " ##");
        if (results != null && results.size() != 0) {
            for (NameSearchResult result : results)
                System.out.println(result);
        }
        System.out.println("###################################");
    }

    @Test
    public void testIgnoredHomonyms1() {
        //test that Macropus throws an exception in normal situations
        try {
            searcher.searchForLSID("Macropus");
        } catch (SearchResultException e) {
            assertTrue(e instanceof HomonymException);
            assertEquals(1, e.getResults().size());
        }
     }

    @Test
    public void testIgnoredHomonyms2() {
        //test that Macropus doesn't throw and exception when "ignoreHomonyms" is set
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName("Macropus");
            cl.setGenus("Macropus");
            //NameSearchResult nsr =searcher.searchForRecord(cl.getScientificName(), cl, null, true,true);
            String lsid = searcher.searchForLSID("Macropus", false, true);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:9e6a0bba-de5b-4465-8544-aa8fe3943fab", lsid);
        } catch (Exception e) {
            fail("ignored homonyms should not throw exception " + e.getMessage());
        }
    }

    @Test
    public void testIgnoredHomonyms3() {
        //test that Agathis still throws a homonym exception when "ignoreHomonyms" are set (because we have 2 different versions of this.
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName("Agathis");
            cl.setGenus("Agathis");
            NameSearchResult nsr = searcher.searchForRecord(cl.getScientificName(), cl, null, true, true);
            fail("A Homonym should have been detected. Not result returned: " + nsr);
        } catch (Exception e) {
            assertTrue(e instanceof HomonymException);
        }
    }

    @Test
    public void testIgnoredHomonyms4() {
        //test that Agathis is resolvable with a kingdom
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName("Agathis");
            cl.setGenus("Agathis");
            cl.setKingdom("Animalia");
            NameSearchResult nsr = searcher.searchForRecord(cl.getScientificName(), cl, null, true, true);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:d12e8168-51ae-4883-8c93-3ce60af18f15", nsr.getLsid());
        } catch (Exception e) {
            fail("A kingdom was supplied and should be resolvable. " + e.getMessage());
        }
    }


    @Test
    public void testHomonym() {
        try {

            LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Simsia");
            List<NameSearchResult> results = searcher.searchForRecords(
                    "Simsia", RankType.getForId(6000), cl, 10);
            printAllResults("hymonyms test 1", results);
            //test to ensure that kingdoms that almost match are being will not report homonym exceptions
            cl.setGenus("Silene");
            cl.setKingdom("Plantae");
            results = searcher.searchForRecords("Silene", RankType.getForId(6000), cl, 10);
            printAllResults("hymonyms test (Silene)", results);

            cl.setGenus("Serpula");
            cl.setKingdom("Animalia");
            cl.setPhylum("ANNELIDA");
            results = searcher.searchForRecords("Serpula", RankType.getForId(6000), cl, 10);
            printAllResults("hymonyms test (Serpula)", results);

            cl.setGenus("Gaillardia");
            cl.setKingdom("Plantae");
            results = searcher.searchForRecords("Gaillardia", RankType.getForId(6000), cl, 10);
            printAllResults("hymonyms test (Gaillardia)", results);

        } catch (SearchResultException e) {
            //			System.err.println(e.getMessage());
            e.printStackTrace();
            printAllResults("HOMONYM EXCEPTION", e.getResults());
            fail("testHomonym failed");
        }
    }

    @Test
    public void testIDLookup() {
        NameSearchResult result = searcher.searchForRecordByID("216346");
        System.out.println("testIDLookup: " + result);
    }

    @Test
    public void testSearchForRecord1() {
        NameSearchResult result = null;
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification(null, "Rhinotia");
            result = searcher.searchForRecord("Rhinotia", cl, RankType.GENUS);
            assertNotNull(result);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:73986806-7e29-407c-b0f4-e40868f2ea93", result.getLsid());
        } catch (SearchResultException e) {
            e.printStackTrace();
            fail("testSearchForRecord failed");
        }
    }

    @Test
    public void testSearchForRecord2() {
        NameSearchResult result = null;
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setAuthorship("Meisn.");
            cl.setFamily("Celastraceae");
            cl.setGenus("Denhamia");
            cl.setScientificName("Denhamia Meisn.");
            result = searcher.searchForRecord(cl, true);
            assertNotNull(result);
            assertEquals("http://id.biodiversity.org.au/node/apni/2887827", result.getLsid());
        } catch (SearchResultException e) {
            e.printStackTrace();
            fail("testSearchForRecord failed");
        }
    }

    @Test
    public void testCommonNames1() {
        String name = "Red Kangaroo";
        String lsid = getCommonNameLSID(name);
        String sciName = getCommonName(name);
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:31a9b8b8-4e8f-4343-a15f-2ed24e0bf1ae", lsid);
        assertEquals("Macropus rufus", sciName);
    }

    @Test
    public void testCommonNames2() {
        String name = "Yellow-tailed Black-Cockatoo";
        String lsid = getCommonNameLSID(name);
        String sciName = getCommonName(name);
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:e7873288-a90c-4f20-8be1-e8ec69a074a5", lsid);
        assertEquals("Calyptorhynchus (Zanda) funereus", sciName);
    }

    @Test
    public void testCommonNames3() {
        String name = "Scarlet Robin";
        String lsid = getCommonNameLSID(name);
        String sciName = getCommonName(name);
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:557046f1-5345-44e4-b08b-a1a55cacefa6", lsid);
        assertEquals("Petroica (Petroica) boodang", sciName);
    }

    @Test
    public void testCommonNames4() {
        String name = "Pacific Bluefin Tuna";
        String lsid = getCommonNameLSID(name);
        String sciName = getCommonName(name);
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:b35bf6d6-3b67-4d4c-b81e-b7ca7a64d341", lsid);
        assertEquals("Thunnus orientalis", sciName);
    }

    @Test
    public void testCommonNames5() {
        String name = "Pacific Black Duck";
        String lsid = getCommonNameLSID(name);
        String sciName = getCommonName(name);
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:da8a156f-95e2-4fcb-a6e7-52721705a70c", lsid);
        assertEquals("Anas (Anas) superciliosa", sciName);
    }

    @Test
    public void testCommonNames6() {
        String name = "European Carp";
        String lsid = getCommonNameLSID(name);
        String sciName = getCommonName(name);
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:16171fac-8d6c-4327-9fab-f2db864d71bf", lsid);
        assertEquals("Cyprinus carpio", sciName);
    }

    @Test
    public void testCommonNames7() {
        String name = "Sulphur-crested Cockatoo";
        String lsid = getCommonNameLSID(name);
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:8f8d2e99-0135-4574-bd0b-8ba5604118e6", lsid);
        name = "Sulphur crested Cockatoo";
        lsid = getCommonNameLSID(name);
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:8f8d2e99-0135-4574-bd0b-8ba5604118e6", lsid);
        name = "SULPHUR CRESTED COCKATOO";
        lsid = getCommonNameLSID(name);
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:8f8d2e99-0135-4574-bd0b-8ba5604118e6", lsid);
        String sciName = getCommonName(name);
        assertEquals("Cacatua (Cacatua) galerita", sciName);
    }

    private String getCommonNameLSID(String name) {
        return searcher.searchForLSIDCommonName(name);
    }

    private String getCommonName(String name) {
        NameSearchResult sciName = searcher.searchForCommonName(name);

        return (sciName == null ? null : sciName.getRankClassification().getScientificName());
    }

    @Test
    public void testIRMNGHomonymReconcile() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Chordata", null, null, "Macropodidae", "Macropus", null);
            RankType rank = searcher.resolveIRMNGHomonym(cl, RankType.GENUS);
            System.out.println("IRMNG Homonym resolved at " + rank + " rank");

            assertEquals("FAMILY", rank.toString());
            //now cause a homonym exception by removing the family
            cl.setFamily(null);
            searcher.resolveIRMNGHomonym(cl, RankType.GENUS);
        } catch (HomonymException e) {
            System.out.println("Expected HomonymException: " + e.getMessage());
            //			fail("testIRMNGHomonymReconcile failed");
        } catch (Exception e) {
            e.printStackTrace();
            fail("testIRMNGHomonymReconcile failed");
        }
    }

    @Test
    public void newHomonynmTest() {
        try {
            //Abelia grandiflora
        } catch (Exception e) {

        }
    }

    @Test
    public void testCultivars() {
        try {
            //species level concept
            System.out.println("Hypoestes phyllostachya: " + searcher.searchForLSID("Hypoestes phyllostachya"));
            //cultivar level concept
            System.out.println("Hypoestes phyllostachya 'Splash': " + searcher.searchForRecord("Hypoestes phyllostachya 'Splash'", null));

        } catch (Exception e) {
            e.printStackTrace();
            fail("testCultivars failed");
        }
    }

    @Test
    public void testMyrmecia() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Arthropoda", "Insecta", "Hymenoptera", "Formicidae", "Myrmecia", null);
            String output = null;
            NameSearchResult nsr = searcher.searchForRecord("Myrmecia", cl, RankType.GENUS);
            if (nsr != null) {
                output = nsr.toString();
            }
            System.out.println("testMyrmecia: " + output);
        } catch (Exception e) {
            e.printStackTrace();
            fail("testMyrmecia failed");
        }
    }

    @Test
    public void testSearchForLSID1() {
        try {
            String lsid = searcher.searchForLSID("Anochetus");
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:a5b62e5c-cbe7-4784-b9f1-0ab0157b8cf4", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSearchForLSID2() {
        try {
            String lsid = searcher.searchForLSID("Anochetus", true);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:a5b62e5c-cbe7-4784-b9f1-0ab0157b8cf4", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSearchForLSID3() {
        try {
            String lsid = searcher.searchForLSID("Anochetus", true);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:a5b62e5c-cbe7-4784-b9f1-0ab0157b8cf4", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSearchForLSID4() {
        try {
            String lsid = searcher.searchForLSID("Anochetus", RankType.GENUS);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:a5b62e5c-cbe7-4784-b9f1-0ab0157b8cf4", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSearchForLSID5() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Arthropoda", "Insecta", "Hymenoptera", "Formicidae", "Anochetus", null);
            String lsid = searcher.searchForLSID("Anochetus", cl, RankType.GENUS);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:a5b62e5c-cbe7-4784-b9f1-0ab0157b8cf4", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSearchForLSID6() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Arthropoda", "Insecta", "Hymenoptera", "Formicidae", "Anochetus", null);
            String lsid = searcher.searchForLSID(cl, true);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:a5b62e5c-cbe7-4784-b9f1-0ab0157b8cf4", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSearchForLSID7() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Arthropoda", "Insecta", "Hymenoptera", "Formicidae", "Anochetus", null);
            String lsid = searcher.searchForLSID(cl, true);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:a5b62e5c-cbe7-4784-b9f1-0ab0157b8cf4", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testFuzzyMatches() {
        try {
            //Eolophus roseicapillus - non fuzzy match
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:8d061243-c39f-4b81-92a9-c81f4419e93c", searcher.searchForLSID("Eolophus roseicapillus"));

            //Eolophus roseicapilla - fuzzy match
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:8d061243-c39f-4b81-92a9-c81f4419e93c", searcher.searchForLSID("Eolophus roseicapilla", true));
        } catch (Exception e) {
            e.printStackTrace();
            fail("testFuzzyMatches failed");
        }
    }

    @Test
    public void testCrossRankHomonyms() {
        try {
            //Patellina is an order and genus
            searcher.searchForLSID("Patellina");
            fail("Cross Homonym Patellina test 1 failed");
        } catch (SearchResultException e) {
            System.out.println(e.getResults());
            assertEquals("Cross Homonysm Patellina test 1 failed to throw correct exception", e.getClass(), HomonymException.class);
        }
    }

    @Test
    /**
     * Test that the spp. does not match.
     */
    public void testGenusNotAllSpecies() {
        try {
            //System.out.println(searcher.searchForLSID("Stackhousia sp. (McIvor River J.R.Clarkson 5201)"));
            String lsid = searcher.searchForLSID("Opuntia spp.");
            fail("Genus spp. test failed to throw exception.");
        } catch (Exception e) {
            assertEquals("Genus spp. test failed", "Unable to perform search. Can not match to a subset of species within a genus.", e.getMessage());
        }
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification(null, "Opuntia");
            cl.setScientificName("Opuntia spp.");
            searcher.searchForLSID(cl, true);
            fail("SPP2 failed to throw a homonym exception");
        } catch (Exception e) {
            assertEquals(e.getClass(), HomonymException.class);
        }
    }

    @Test
    public void testSinglePhraseName() {
        try {

            String name = "Astroloma sp. Cataby (EA Griffin 1022)";
            assertEquals("http://id.biodiversity.org.au/node/apni/2886191", searcher.searchForLSID(name, null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test that the an infraspecific rank searches for the specified rank and
     * RankType.INFRASPECIFICNAME
     */
    @Test
    public void testInfraSpecificRank() {
        try {
            String name = "Acacia acanthoclada subsp. glaucescens";
            assertEquals("http://id.biodiversity.org.au/node/apni/2905993", searcher.searchForLSID(name));
            assertNull(searcher.searchForLSID("Macropus rufus", RankType.GENUS));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRankMarker() {
        try {
            String lsid = searcher.searchForLSID("Macropus sp. rufus");
            System.out.println("SP.:" + lsid);
            lsid = searcher.searchForLSID("Macropus ssp. rufus");
            System.out.println("ssp: " + lsid);
        } catch (Exception e) {
            fail("rank marker test failed");
        }
    }

    @Test
    public void testSimpleLookup1()  {
        try {
            String name = "Megalurus gramineus";
            NameSearchResult nsr = searcher.searchForRecord(name);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:b88430ed-f7d7-482e-a586-f0a02d8e11ce", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }


    @Test
    public void testSimpleLookup2()  {
        try {
            String name = "Synemon plana";
            NameSearchResult nsr = searcher.searchForRecord(name);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:fb552f98-f304-47d5-a332-f3868fbf8556", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testSimpleLookup3()  {
        try {
            String name = "Sargassum podacanthum";
            NameSearchResult nsr = searcher.searchForRecord(name);
            assertNotNull(nsr);
            assertEquals("CAAB:54105060", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }


    @Test
    public void testSimpleLookup4()  {
        try {
            String name = "Chenopodium x bontei nothovar. submelanocarpum";
            NameSearchResult nsr = searcher.searchForRecord(name);
            assertNotNull(nsr);
            assertEquals("http://id.biodiversity.org.au/node/apni/2902250", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testSimpleLookup5()  {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setKingdom("Fungi");
            cl.setScientificName("Favolus princeps");
            NameSearchResult nsr = searcher.searchForRecord(cl, true);
            assertNotNull(nsr); // Been removed
            assertEquals("f9db9740-3ce1-4f18-b8f3-9484cadfec5e", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }


    @Test
    public void testSimpleLookup6()  {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setKingdom("Plantae");
            cl.setScientificName("Andreaea");
            NameSearchResult nsr = searcher.searchForRecord(cl, true);
            assertNotNull(nsr);
            assertEquals("0404cb28-4189-435d-8d7c-e7d600ba04e5", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    @Test
    public void testSimpleLookup7()  {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setKingdom("Plantae");
            String name = "Astomum";
            NameSearchResult nsr = searcher.searchForRecord(name, cl, RankType.GENUS);
            assertNotNull(nsr);
            assertEquals("78b20c8e-564b-4a05-838b-69cd5c2801ee", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    @Test
    public void testSimpleLookup8()  {
        try {
            String name = "Carbo ater";
            NameSearchResult nsr = searcher.searchForRecord(name);
            fail("Expecting ecxluded name exception");
        } catch (ExcludedNameException ex) {
            assertNull(ex.getNonExcludedName()); // Two types both excluded
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    @Test
    public void testSimpleLookup9()  {
        try {
            String name = "Neobatrachus sudellae";
            NameSearchResult nsr = searcher.searchForRecord(name);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:31753086-def1-48dd-b22e-946937979653", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    @Test
    public void testSimpleLookup10()  {
        try {
            String name = "Neobatrachus sudelli";
            NameSearchResult nsr = searcher.searchForRecord(name);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:31753086-def1-48dd-b22e-946937979653", nsr.getAcceptedLsid());
          } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }


    @Test
    public void testSimpleLookup11()  {
        try {
            String name = "Cereopsis novaehollandiae";
            NameSearchResult nsr = searcher.searchForRecord(name);
            fail("Expecting parent-child synonym exception");
        } catch (ParentSynonymChildException ex) {
            NameSearchResult nsr = ex.getChildResult();
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:6667178a-4c9c-475d-938b-7f9733707588", nsr.getLsid());
            assertEquals(RankType.SUBSPECIES, nsr.getRank());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    @Test
    public void testStigmoderaAurifera()  {
        try {
            String name = "Stigmodera aurifera Carter";
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName(name);
            NameSearchResult nsr = searcher.searchForRecord(cl, true);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:b43ac4f1-94ff-4a00-96c3-ed027aa44941", nsr.getAcceptedLsid());
            assertEquals("Stigmodera aurifera", nsr.getRankClassification().getScientificName());
            assertEquals(MatchType.CANONICAL, nsr.getMatchType());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testCorreaReflexaHybrid()  {
        try {
            String name = "Correa reflexa (Labill.) Vent. hybrid";
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName(name);
            NameSearchResult nsr = searcher.searchForRecord(cl, true);
            assertNotNull(nsr);
            assertEquals("Correa reflexa", nsr.getRankClassification().getSpecies());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testHigherTaxonMatch1()  {
        try {
            String name = "Breutelia scoparia";
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName(name);
            NameSearchResult nsr = searcher.searchForRecord(cl, true);
            assertNotNull(nsr);
            assertEquals("10f4ed8a-d572-46c3-83a6-69e083ac68a6", nsr.getLsid());
            assertEquals("Breutelia", nsr.getRankClassification().getGenus());
            assertEquals(MatchType.RECURSIVE, nsr.getMatchType());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testHigherTaxonMatch2()  {
        try {
            String name = "Ramalina aspera";
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName(name);
            NameSearchResult nsr = searcher.searchForRecord(cl, true);
            assertNotNull(nsr);
            assertEquals("NZOR-3-54382", nsr.getLsid());
            assertEquals("Ramalina", nsr.getRankClassification().getGenus());
            assertEquals(MatchType.RECURSIVE, nsr.getMatchType());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testHomonymWithOrderResolution1()  {
        try {
            String name = "Tyto";
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName(name);
            NameSearchResult nsr = searcher.searchForRecord(cl, true);
            fail("Expecting homonym exception");
        } catch (HomonymException ex) {
            assertEquals(2, ex.getResults().size());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
        try {
            String name = "Tyto";
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName(name);
            cl.setOrder("Strigiformes");
            NameSearchResult nsr = searcher.searchForRecord(cl, true);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:97bc55da-8870-45ac-a828-97787740ad61", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

}
