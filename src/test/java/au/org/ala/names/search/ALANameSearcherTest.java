

package au.org.ala.names.search;

import au.org.ala.names.model.*;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.nameparser.PhraseNameParser;
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
    public static void init() throws Exception {
        searcher = new ALANameSearcher("/data/lucene/namematching-20181120");
    }

    @Test
    public void testMisappliedNames1() throws Exception {
        try {
            //test to ensure that a misapplied name also .
            String lsid = searcher.searchForLSID("Corybas macranthus");
            fail("A misapplied exception should be thrown");
            //assertEquals("urn:lsid:biodiversity.org.au:apni.taxon:549612",lsid);
        } catch (MisappliedException ex) {
            assertEquals("http://id.biodiversity.org.au/node/apni/2915977", ex.getMatchedResult().getLsid());
            //assertNull(ex.getMisappliedResult());
        }
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
            assertEquals("92a4e5c4-32c1-44c6-a9f7-410659692dfa", ex.getMatchedResult().getLsid());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    @Test
    public void testSynonymAsHomonym1() {
        try {
            String lsid = searcher.searchForLSID("Abelia");
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

    //@Test TODO What does NPE mean? Sphacelaria is in CAAB
    public void npeInAuthorTest() {
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
    public void parserBlackList() throws Exception {
        //Petaurus australis unnamed subsp. - this name should NOT throw a NPE (although it generates an unhappiness in the parser)
        String name = "Petaurus australis unnamed subsp.";
        String lsid = searcher.searchForLSID(name, true);
        assertNotNull(lsid);
        assertEquals("ALA_Petaurus_australis_unnamed_subsp", lsid);
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
            assertEquals("urn:lsid:biodiversity.org.au:afd.name:4b4fb3db-2bc4-4005-8660-7531a86b8786", ene.getExcludedName().getLsid());
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
    public void testHomonymsWithResolution1() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        NameSearchResult nsr = null;
        cl.setScientificName("Thalia");
        try {
            nsr = searcher.searchForRecord("Thalia", null, true);
            fail("Thalia should throw a homonym without kingdom or author");
        } catch (HomonymException e) {
        }
    }

    @Test
    public void testHomonymsWithResolution2() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        NameSearchResult nsr = null;
        cl.setScientificName("Thalia");
        cl.setKingdom("Animalia");
        cl.setPhylum("Chordata");
        try {
            nsr = searcher.searchForRecord(cl, false);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:52c68649-47d5-4f2e-9730-417fc54fb080", nsr.getLsid());
        } catch (HomonymException e) {
            fail("Homonym should be resolved via the Kingdom");
        }
    }

    @Test
    public void testHomonymsWithResolution3() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        NameSearchResult nsr = null;
        cl.setScientificName("Thalia");
        cl.setKingdom("Plantae");
        try {
            nsr = searcher.searchForRecord(cl, false);
            assertNotNull(nsr);
            assertEquals("http://id.biodiversity.org.au/node/apni/2908051", nsr.getLsid());
        } catch (HomonymException e) {
            fail("Homonym should be resolved via the Kingdom");
        }
    }

    @Test
    public void testHomonymsWithResolution4() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        NameSearchResult nsr = null;
        cl.setScientificName("Thalia");
        cl.setAuthorship("Blumenbach, 1798");
        try {
            nsr = searcher.searchForRecord(cl, false);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:52c68649-47d5-4f2e-9730-417fc54fb080", nsr.getLsid());
        } catch (HomonymException e) {
            fail("Author should identify homonym value to use");
        }
    }

    @Test
    public void testHomonymsWithResolution5() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        NameSearchResult nsr = null;
        cl.setScientificName("Thalia");
        cl.setAuthorship("Blumenbach");
        try {
            nsr = searcher.searchForRecord(cl, false);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:52c68649-47d5-4f2e-9730-417fc54fb080", nsr.getLsid());
        } catch (HomonymException e) {
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
            assertEquals("urn:lsid:biodiversity.org.au:afd.name:fbe09d8b-8cc2-444a-b8f7-d06730543781", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }


    @Test
    public void testsStrMarker2(){
        try {
            NameSearchResult nsr;
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setKingdom("Plantae");
            cl.setGenus("Test");
            cl.setScientificName("Osphranter rufus");
            nsr = searcher.searchForRecord(cl, true);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:e6aff6af-ff36-4ad5-95f2-2dfdcca8caff", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }


    @Test
    public void testsStrMarker3()  {
        try {
            String name = "Pterodroma arminjoniana s. str.";
            NameSearchResult nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("ALA_Pterodroma_arminjoniana_s_str", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
     }

    @Test
    public void testsStrMarker4() {
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
    public void testsStrMarker5() {
        try {
            String name = "Aplonis fusca hulliana";
            NameSearchResult nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:d1674a33-af14-4592-be4d-2ededc1b53cd", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }

    @Test
    public void testsStrMarker6() {
        try {
            String name = "Cryphaea tenella";
            NameSearchResult nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("http://id.biodiversity.org.au/node/ausmoss/10068952", nsr.getLsid());
            assertEquals(name, nsr.getRankClassification().getScientificName());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }

    @Test
    public void testsStrMarker7() {
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
    public void testsStrMarker8() {
        try {
            // This is 'blacklisted' but the blacklist is ignored by the DwCA loader
            String name = "Siganus nebulosus";
            NameSearchResult nsr = searcher.searchForRecord(name, null, true);
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.name:c2d406d8-1066-4fd3-8c95-31ee6343a1b8", nsr.getLsid());
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:0aa9653f-00c7-42b9-896b-f399103703b8", nsr.getAcceptedLsid());

        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }
    @Test
    public void testsStrMarker9() {
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
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:0217f06f-664c-4c64-bc59-1b54650fa23d", nsr.getAcceptedLsid());

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
            nsr = searcher.searchForRecord(name, RankType.SUBSPECIES);
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
            assertEquals("ALA_Pterodroma_arminjoniana_s_str", nsr.getLsid());
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
            // There are two Thelymitra adorata one with nom. inval. return the other one
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
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:b1ba5449-a68e-4c3b-ae90-8e667617945b", nsr.getLsid());
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
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:b1ba5449-a68e-4c3b-ae90-8e667617945b", nsr.getLsid());
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
            assertEquals("urn:lsid:biodiversity.org.au:afd.name:b64aac6e-d2c4-40d0-b3fd-2b037a6e4d07", nsr.getLsid());
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:3fb329bb-89d6-4589-b98a-3b1a6a284021", nsr.getAcceptedLsid());
            assertEquals(MatchType.SOUNDEX, nsr.getMatchType());
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
            assertEquals("CoL:29273814", nsr.getLsid());
            assertEquals(MatchType.SOUNDEX, nsr.getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }


    // This is an African Elephant, not a giant land whelk
    @Test
    public void testOutOfGeography1() {
        String name = "Loxodonta africana";
        LinnaeanRankClassification classification = new LinnaeanRankClassification();
        classification.setPhylum("Chordata");
        classification.setKlass("Mammalia");
        classification.setOrder("Proboscidea");
        classification.setFamily("Elephantidae");
        classification.setGenus("Loxodonta");
        classification.setScientificName(name);
        try {
            NameSearchResult nsr = searcher.searchForRecord(classification, true, true, true);
            assertNotNull(nsr);
            assertEquals("ALA_Proboscidea", nsr.getLsid());
            assertEquals(MatchType.RECURSIVE, nsr.getMatchType());
            assertEquals(RankType.ORDER, nsr.getRank());
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
            PhraseNameParser parser = new PhraseNameParser();
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
            assertEquals("NZOR-6-73174", lsid);
        } catch (SearchResultException e) {
            e.printStackTrace();
            fail("testNoRank failed");
        }
    }

    @Ignore // TODO No additional reallsid currently used
    @Test
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
            //NameSearchResult nsr =searcher.searchForRecord(cl.getId(), cl, null, true,true);
            String lsid = searcher.searchForLSID("Macropus", false, true);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:b1d9bf29-648f-47e6-8544-2c2fbdf632b1", lsid);
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
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:fa8687aa-2d6f-4d86-b9b7-175cb32136d3", nsr.getLsid());
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
    public void testIDLookup1() {
        String id = "http://id.biodiversity.org.au/node/apni/2893343";
        String name = "Allocasuarina huegeliana";
        NameSearchResult result = searcher.searchForRecordByID(id);
        assertEquals(id, result.getId());
        assertEquals(id, result.getLsid());
        assertEquals(MatchType.TAXON_ID, result.getMatchType());
        assertEquals(name, result.getRankClassification().getScientificName());
    }

    @Test
    public void testLSIDLookup1() {
        String id = "http://id.biodiversity.org.au/node/apni/2893343";
        String name = "Allocasuarina huegeliana";
        NameSearchResult result = searcher.searchForRecordByLsid(id);
        assertEquals(id, result.getId());
        assertEquals(id, result.getLsid());
        assertEquals(MatchType.TAXON_ID, result.getMatchType());
        assertEquals(name, result.getRankClassification().getScientificName());
    }

    @Test
    public void testSearchForRecord() {
        NameSearchResult result = null;
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification(null, "Rhinotia");
            result = searcher.searchForRecord("Rhinotia", cl, RankType.GENUS);
        } catch (SearchResultException e) {
            e.printStackTrace();
            fail("testSearchForRecord failed");
        }
        System.out.println("testSearchForRecord: " + result);
    }

    @Test
    public void testCommonNames1() {
        String name = "Red Kangaroo";
        String lsid = getCommonNameLSID(name);
        String sciName = getCommonName(name);
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:e6aff6af-ff36-4ad5-95f2-2dfdcca8caff", lsid);
        assertEquals("Osphranter rufus", sciName);
    }

    @Test
    public void testCommonNames2() {
        String name = "Yellow-tailed Black-Cockatoo";
        String lsid = getCommonNameLSID(name);
        String sciName = getCommonName(name);
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:72ca8d75-71da-4751-a5cf-aa07ac3869f7", lsid);
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
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:f9eb417b-2de3-48ac-ba4e-1d438f0cb323", lsid);
        name = "Sulphur crested Cockatoo";
        lsid = getCommonNameLSID(name);
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:f9eb417b-2de3-48ac-ba4e-1d438f0cb323", lsid);
        name = "SULPHUR CRESTED COCKATOO";
        lsid = getCommonNameLSID(name);
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:f9eb417b-2de3-48ac-ba4e-1d438f0cb323", lsid);
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
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:bea6dc0a-1e16-424d-9256-4b01e5a56b05", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSearchForLSID2() {
        try {
            String lsid = searcher.searchForLSID("Anochetus", true);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:bea6dc0a-1e16-424d-9256-4b01e5a56b05", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSearchForLSID3() {
        try {
            String lsid = searcher.searchForLSID("Anochetus", true);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:bea6dc0a-1e16-424d-9256-4b01e5a56b05", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSearchForLSID4() {
        try {
            String lsid = searcher.searchForLSID("Anochetus", RankType.GENUS);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:bea6dc0a-1e16-424d-9256-4b01e5a56b05", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSearchForLSID5() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Arthropoda", "Insecta", "Hymenoptera", "Formicidae", "Anochetus", null);
            String lsid = searcher.searchForLSID("Anochetus", cl, RankType.GENUS);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:bea6dc0a-1e16-424d-9256-4b01e5a56b05", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSearchForLSID6() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Arthropoda", "Insecta", "Hymenoptera", "Formicidae", "Anochetus", null);
            String lsid = searcher.searchForLSID(cl, true);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:bea6dc0a-1e16-424d-9256-4b01e5a56b05", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSearchForLSID7() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Arthropoda", "Insecta", "Hymenoptera", "Formicidae", "Anochetus", null);
            String lsid = searcher.searchForLSID(cl, true);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:bea6dc0a-1e16-424d-9256-4b01e5a56b05", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testFuzzyMatches() throws Exception {
        //Eolophus roseicapillus - non fuzzy match
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:577ff059-a2a7-48b0-976c-fdd6a345f878", searcher.searchForLSID("Eolophus roseicapilla"));

        //Eolophus roseicapilla - fuzzy match
        assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:577ff059-a2a7-48b0-976c-fdd6a345f878", searcher.searchForLSID("Eolophus roseicapillus", true));
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
            fail("SPP2 failed to throw a SPP exception");
        } catch (Exception e) {
            assertEquals(SPPException.class, e.getClass());
        }
    }

    @Test
    public void testSinglePhraseName() {
        try {

            String name = "Astroloma sp. Cataby (E.A.Griffin 1022)";
            assertEquals("http://id.biodiversity.org.au/node/apni/7178434", searcher.searchForLSID(name, null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test that the an infraspecific rank searches for the specified rank and
     * RankType.INFRASPECIFICNAME
     */
    @Test
    public void testInfraSpecificRank() throws Exception {
        String name = "Acacia acanthoclada subsp. glaucescens";
        assertEquals("http://id.biodiversity.org.au/node/apni/2905993", searcher.searchForLSID(name));
        assertNull(searcher.searchForLSID("Osphranter rufus", RankType.GENUS));
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
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:a51dca29-50e7-49b4-ae35-5c35a9c4f854", nsr.getLsid());
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
            assertEquals("54105060", nsr.getLsid());
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
            assertEquals("http://id.biodiversity.org.au/instance/apni/769095", nsr.getLsid());
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
            assertEquals("43e1bc65-3580-47db-b269-cdb066ed49e9", nsr.getLsid());
            assertEquals( "10911fd1-a2dd-41f1-9c4d-8dff7f118670", nsr.getAcceptedLsid());
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
            assertEquals("http://id.biodiversity.org.au/node/ausmoss/10057678", nsr.getLsid());
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
            assertEquals("http://id.biodiversity.org.au/name/ausmoss/10001613", nsr.getLsid());
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
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:953a5af4-2932-4c8b-8f33-850b5f8f3fed", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    @Test
    public void testSimpleLookup10()  {
        try {
            String name = "Eucalyptus acaciaeformis";
            NameSearchResult nsr = searcher.searchForRecord(name);
            assertNotNull(nsr);
            assertNotNull(nsr.getAcceptedLsid());
            assertEquals("http://id.biodiversity.org.au/node/apni/2889217", nsr.getAcceptedLsid());
          } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }


    @Test
    public void testSimpleLookup12()  {
        try {
            String name = "Acaena";
            NameSearchResult nsr = searcher.searchForRecord(name);
            fail("Expecting homonym exception");
        } catch (HomonymException e) {
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }
    @Test
    public void testParentChildSynonym1()  {
        try {
            String name = "Pitta versicolor";
            NameSearchResult nsr = searcher.searchForRecord(name);
            fail("Expecting parent-child synonym exception");
        } catch (ParentSynonymChildException ex) {
            NameSearchResult nsr = ex.getChildResult();
            assertNotNull(nsr);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:d0e66526-1cdd-4b03-85b2-71b7e7d8b84a", nsr.getLsid());
            assertEquals(RankType.SUBSPECIES, nsr.getRank());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    @Test
    public void testParentChildSynonym2()  {
        try {
            String name = "Phoma lobeliae";
            NameSearchResult nsr = searcher.searchForRecord(name);
            fail("Expecting parent-child synonym exception");
        } catch (ParentSynonymChildException ex) {
            NameSearchResult nsr = ex.getChildResult();
            assertNotNull(nsr);
            assertEquals("8e64942a-f300-46c8-ba97-76492d25d985", nsr.getLsid());
            assertEquals(RankType.FORM, nsr.getRank());
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
            assertEquals("urn:lsid:biodiversity.org.au:afd.name:efbe20e3-e69d-4f2c-80ec-79051dee1174", nsr.getLsid());
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:91d85a17-66d9-4879-b761-e19089e4710c", nsr.getAcceptedLsid());
            assertEquals("Stigmodera aurifera", nsr.getRankClassification().getScientificName());
            assertEquals(MatchType.CANONICAL, nsr.getMatchType());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testCorreaReflexaHybrid() throws Exception {
        String name = "Correa reflexa (Labill.) Vent. hybrid";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        try {
            searcher.searchForRecord(cl, true);
            fail("Expecting misapplied exception");
        } catch (MisappliedException ex) {
            NameSearchResult nsr = ex.getMatchedResult();
            assertEquals("Correa reflexa", nsr.getRankClassification().getSpecies());
            assertEquals("http://id.biodiversity.org.au/node/apni/2893483", nsr.getLsid());
            nsr = ex.getMisappliedResult();
            assertEquals("Correa eburnea", nsr.getRankClassification().getSpecies());
            assertEquals("http://id.biodiversity.org.au/node/apni/2910182", nsr.getLsid());
        }
    }


    @Test
    public void testIpomoea() throws Exception {
        String name = "Ipomoea";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        cl.setFamily("Convolvulaceae");
        NameSearchResult nsr = searcher.searchForRecord(cl, true);
        assertNotNull(nsr);
        assertEquals(RankType.GENUS, nsr.getRank());
        assertEquals("Ipomoea", nsr.getRankClassification().getScientificName());
    }


    @Test
    public void testSpecificEpithetWithoutName1() throws Exception {
         LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setGenus("Oxypetalum");
        cl.setSpecificEpithet("caeruleum");
        NameSearchResult nsr = searcher.searchForRecord(cl, true);
        assertNotNull(nsr);
        assertEquals(SynonymType.SYNONYM, nsr.getSynonymType());
        assertEquals("http://id.biodiversity.org.au/instance/apni/859716", nsr.getLsid());
        assertEquals("http://id.biodiversity.org.au/node/apni/2906575", nsr.getAcceptedLsid());
    }


    @Test
    public void testHigherTaxonMatch1()  {
        try {
            String name = "Breutelia scoparia";
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName(name);
            NameSearchResult nsr = searcher.searchForRecord(cl, true);
            assertNotNull(nsr);
            assertEquals("http://id.biodiversity.org.au/node/ausmoss/10061546", nsr.getLsid());
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
            assertEquals("NZOR-6-1843", nsr.getLsid());
            assertEquals("Ramalina", nsr.getRankClassification().getGenus());
            assertEquals(MatchType.RECURSIVE, nsr.getMatchType());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testHomonymWithOrderResolution1()  {
        try {
            String name = "Abelia";
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
            String name = "Abelia";
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName(name);
            cl.setOrder("Dipsacales");
            NameSearchResult nsr = searcher.searchForRecord(cl, true);
            assertNotNull(nsr);
            assertEquals("http://id.biodiversity.org.au/node/apni/2892114", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

}
