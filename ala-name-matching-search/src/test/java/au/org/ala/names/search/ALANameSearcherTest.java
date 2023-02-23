

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

package au.org.ala.names.search;

import au.org.ala.names.model.*;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.nameparser.PhraseNameParser;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Natasha, Tommy
 */
public class ALANameSearcherTest {
    private static ALANameSearcher searcher;

    @org.junit.BeforeClass
    public static void init() throws Exception {
        searcher = new ALANameSearcher("/data/lucene/namematching-20210811-5");
    }

    @Test
    public void testMisappliedNames1() throws Exception {
        try {
            //test to ensure that a misapplied name also .
            String lsid = searcher.searchForLSID("Abildgaardia fusca");
            fail("A misapplied exception should be thrown");
            //assertEquals("urn:lsid:biodiversity.org.au:apni.taxon:549612",lsid);
        } catch (MisappliedException ex) {
            assertEquals("https://id.biodiversity.org.au/instance/apni/943544", ex.getMatchedResult().getLsid());
            assertEquals("https://id.biodiversity.org.au/node/apni/2888570", ex.getMisappliedResult().getLsid());
        }
    }

    @Test
    public void testMisappliedNames2() {
        try {
            //test to ensure that the accepted name is returned when it also exists as a misapplied name.
            String lsid = searcher.searchForLSID("Bertya rosmarinifolia");
            fail("A misapplied exception should be thrown, got " + lsid);
        } catch (MisappliedException ex) {
            assertEquals("https://id.biodiversity.org.au/node/apni/2893214", ex.getMatchedResult().getLsid());
            assertEquals("https://id.biodiversity.org.au/node/apni/2898349", ex.getMisappliedResult().getLsid());
        } catch (Exception ex) {
            fail("No other exceptions should occur, got " + ex);
        }
    }

    @Test
    public void testMisappliedNames3()  {
        try {
            String name = "Acacia bivenosa DC.";
            NameSearchResult nsr = searcher.searchForRecord(name);
            fail("Expecting misapplied exception");
            assertNotNull(nsr);
        } catch (MisappliedException ex) {
            assertEquals("https://id.biodiversity.org.au/node/apni/2912987", ex.getMatchedResult().getLsid());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }


    @Test
    public void testMisappliedNames4()  {
        try {
            String name = "Caladenia concinna";
            NameSearchResult nsr = searcher.searchForRecord(name);
            fail("Expecting misapplied exception");
            assertNotNull(nsr);
        } catch (MisappliedException ex) {
            assertEquals("https://id.biodiversity.org.au/taxon/apni/51398909", ex.getMatchedResult().getLsid());
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
            String lsid = searcher.searchForLSID("Bracteolatae", RankType.SERIES_BOTANY);
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
        assertEquals("ALA_DR652_233", lsid);
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
            assertEquals("https://biodiversity.org.au/afd/taxa/3309bb2e-5b3f-4664-977b-147e60b66109", nsr.getLsid());
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
            assertEquals("https://biodiversity.org.au/afd/taxa/2c5fd509-d4d6-4adb-9566-96280ff9e6af", psce.getParentResult().getLsid());
            assertEquals("https://biodiversity.org.au/afd/taxa/b4f39a2b-cfaf-4c69-8ace-77f1664acd6b", psce.getChildResult().getLsid());
        }

    }

    @Test
    public void testEmbeddedRankMarker() {
        String name = "Flueggea virosa subsp. melanthesoides";
        try {
            String lsid = searcher.searchForLSID(name, true);
            assertEquals("https://id.biodiversity.org.au/node/apni/2893899", lsid);
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
            assertEquals("https://biodiversity.org.au/afd/taxa/74ac7082-6138-4eb0-86ba-95535deab180", ene.getExcludedName().getLsid());
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
            fail("Thalia should throw a homonym without kingdom or author, got " + nsr.getLsid());
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
            assertEquals("https://biodiversity.org.au/afd/taxa/52c68649-47d5-4f2e-9730-417fc54fb080", nsr.getLsid());
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
            assertEquals("https://id.biodiversity.org.au/node/apni/2908051", nsr.getLsid());
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
            assertEquals("https://biodiversity.org.au/afd/taxa/52c68649-47d5-4f2e-9730-417fc54fb080", nsr.getLsid());
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
            assertEquals("https://biodiversity.org.au/afd/taxa/52c68649-47d5-4f2e-9730-417fc54fb080", nsr.getLsid());
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
            assertEquals("https://biodiversity.org.au/afd/taxa/fbe09d8b-8cc2-444a-b8f7-d06730543781", nsr.getLsid());
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
            assertEquals("https://biodiversity.org.au/afd/taxa/e6aff6af-ff36-4ad5-95f2-2dfdcca8caff", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }


    @Test
    public void testsStrMarker3()  {
        try {
            String name = "Oenochrominae s. str."; // There's only one of these left
            NameSearchResult nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("https://biodiversity.org.au/afd/taxa/537ff8fb-b6c2-4536-9cb8-ad244832c1de", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
     }

    @Test
    public void testsStrMarker4()  {
        try {
            String name = "Pterodroma arminjoniana s. str.";
            NameSearchResult nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("ALA_DR656_1585", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }


    @Test
    public void testsStrMarker5() {
        try {
            String name = "Vespa velutina";
            NameSearchResult nsr = searcher.searchForRecord(name, null, true);
            assertNotNull(nsr);
            assertEquals("ALA_DR18234_743", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }

    @Test
    public void testsStrMarker6() {
        try {
            String name = "Aplonis fusca hulliana";
            NameSearchResult nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("https://biodiversity.org.au/afd/taxa/7b241ea8-07ab-4aa0-a2d7-c0b43767c3d4", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }

    @Test
    public void testsStrMarker7() {
        try {
            String name = "Cryphaea tenella";
            NameSearchResult nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("https://id.biodiversity.org.au/node/ausmoss/10068952", nsr.getLsid());
            assertEquals(name, nsr.getRankClassification().getScientificName());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }

    @Test
    public void testsStrMarker8() {
        try {
            String name = "Grevillea 'White Wings'";
            NameSearchResult nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("https://id.biodiversity.org.au/name/apni/163801", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }
    @Test
    public void testsStrMarker9() {
        try {
            // This is 'blacklisted' but the blacklist is ignored by the DwCA loader
            String name = "Siganus nebulosus";
            NameSearchResult nsr = searcher.searchForRecord(name, null, true);
            assertNotNull(nsr);
            assertEquals("https://biodiversity.org.au/afd/taxa/c2d406d8-1066-4fd3-8c95-31ee6343a1b8", nsr.getLsid());
            assertEquals("https://biodiversity.org.au/afd/taxa/0aa9653f-00c7-42b9-896b-f399103703b8", nsr.getAcceptedLsid());

        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }
    @Test
    public void testsStrMarker10() {
        try {
            String name = "Anabathron contabulatum";
            NameSearchResult nsr = searcher.searchForRecord(name, null, true);
            assertNotNull(nsr);
            assertEquals("https://biodiversity.org.au/afd/taxa/b64ec630-8835-4d42-887c-83aee5f417b8", nsr.getLsid());
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
            assertEquals("https://id.biodiversity.org.au/taxon/apni/51258122", nsr.getLsid());
            //assertEquals(ErrorType.QUESTION_SPECIES, nsr.getErrorType());
            System.out.println(nsr);
            name = "Cacatua leadbeateri";
            //name = "Acacia bartleana ms";

            //test the "name based" synonym "has generic combination"
            nsr = searcher.searchForRecord("Cacatua leadbeateri", null);
            assertEquals("https://biodiversity.org.au/afd/taxa/5815e99d-01cd-4a92-99ba-36f480c4834d", nsr.getAcceptedLsid());

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
            assertEquals("https://id.biodiversity.org.au/taxon/apni/51414212", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testSpMarker2()  {
        try {
            String name = "Grevillea brachystylis subsp. Busselton (G.J.Keighery s.n. 28/8/1985)";
            NameSearchResult nsr = null;
            nsr = searcher.searchForRecord(name);
            assertNotNull(nsr);
            assertEquals("https://id.biodiversity.org.au/instance/apni/897499", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testSpMarker3()  {
        try {
            String name = "Lindernia sp. Pilbara (M.N.Lyons & L.Lewis FV 1069)";
            NameSearchResult nsr = null;
            nsr = searcher.searchForRecord(name, RankType.SPECIES);
            assertNotNull(nsr);
            assertEquals("https://id.biodiversity.org.au/name/apni/51306553", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testSpMarker4()  {
        try {
            String name = "Pterodroma arminjoniana s. str.";
            NameSearchResult nsr = null;
            nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("ALA_DR656_1585", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testSpMarker5()  {
        try {
            String name = "Acacia dealbata subsp. subalpina";
            NameSearchResult nsr = null;
            nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("https://id.biodiversity.org.au/node/apni/2911757", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testSpMarker6()  {
        try {
            String name = "Grevillea brachystylis subsp. Busselton";
            NameSearchResult nsr = null;
            nsr = searcher.searchForRecord(name, null);
            assertNotNull(nsr);
            assertEquals("https://id.biodiversity.org.au/instance/apni/897499", nsr.getLsid());
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
            assertEquals("https://id.biodiversity.org.au/node/apni/2916168", nsr.getLsid());
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
            assertEquals("https://id.biodiversity.org.au/instance/apni/871103", nsr.getLsid());
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
            assertEquals("https://id.biodiversity.org.au/node/apni/2886985", nsr.getLsid());
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
            assertEquals("https://id.biodiversity.org.au/taxon/apni/51414212", nsr.getLsid());
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
            assertEquals("https://id.biodiversity.org.au/instance/apni/9302042", nsr.getLsid());
        } catch (SearchResultException e) {
            fail("Unexpected search exception " + e);
        }
    }

    @Test
    public void testSynonymWithoutRank1() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setKingdom("Animalia");
        cl.setScientificName("Gymnorhina tibicen");
        NameSearchResult nsr = searcher.searchForRecord(cl, true, true);
        assertEquals("Gymnorhina tibicen", nsr.getRankClassification().getScientificName());
        assertEquals("(Latham, 1801)", nsr.getRankClassification().getAuthorship());
        cl.setScientificName("Cracticus tibicen");
        cl.setRank(RankType.SPECIES.getRank());
        nsr = searcher.searchForRecord(cl, true, true);
        assertEquals("ALA_DR7933_3", nsr.getLsid());
        assertEquals("https://biodiversity.org.au/afd/taxa/5291343e-fdeb-4a65-8ba5-928f5b96acf5", nsr.getAcceptedLsid());
    }


    @Test
    public void testSynonymWithoutRank2() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName("Abantiades zonatriticum");
        NameSearchResult nsr = searcher.searchForRecord(cl, true, true);
        assertEquals("Abantiades", nsr.getRankClassification().getScientificName());
        assertEquals(MatchType.RECURSIVE, nsr.getMatchType());
        cl.setRank(RankType.SPECIES.getRank());
        nsr = searcher.searchForRecord(cl, true, true);
        assertEquals("Abantiades", nsr.getRankClassification().getScientificName());
        assertEquals(MatchType.RECURSIVE, nsr.getMatchType());
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
        String nameDifferentEnding = "Phylidonyris pyrrhoptera";
        try {
            NameSearchResult nsr = searcher.searchForRecord(nameDifferentEnding, null, true);
            assertNotNull(nsr);
            assertEquals("https://biodiversity.org.au/afd/taxa/61f2bc62-dd50-4ba2-82a0-0377d386e4d8", nsr.getLsid());
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
            assertEquals("https://biodiversity.org.au/afd/taxa/61f2bc62-dd50-4ba2-82a0-0377d386e4d8", nsr.getLsid());
            assertEquals(MatchType.SOUNDEX, nsr.getMatchType());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testInfragenricAndSoundEx3() {
        String nameDiffEndInfraGeneric = "Phylidonyris (Phylidonyris) pyrrhopteras";
        try {
            NameSearchResult nsr = searcher.searchForRecord(nameDiffEndInfraGeneric, null, true);
            assertNotNull(nsr);
            assertEquals("https://biodiversity.org.au/afd/taxa/61f2bc62-dd50-4ba2-82a0-0377d386e4d8", nsr.getLsid());
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
            assertEquals("https://biodiversity.org.au/afd/taxa/c7d8dbc8-dcde-4182-85ba-907182f95ea9", nsr.getLsid());
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
            assertEquals("https://biodiversity.org.au/afd/taxa/c7d8dbc8-dcde-4182-85ba-907182f95ea9", nsr.getLsid());
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
            assertEquals("SY_39006017_1", nsr.getLsid());
            assertEquals("ALA_DR650_737", nsr.getAcceptedLsid());
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
            assertEquals("https://id.biodiversity.org.au/node/apni/2901342", nsr.getLsid());
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
            assertEquals("https://id.biodiversity.org.au/instance/apni/913279", nsr.getLsid());
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
            assertEquals("https://id.biodiversity.org.au/node/apni/2918399", nsr.getLsid());
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
            assertEquals("09df32b7ccf5484f6f280ec74ea31caf", nsr.getLsid());
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
            assertEquals("ALA_DR7933_1", nsr.getLsid());
            assertEquals(MatchType.RECURSIVE, nsr.getMatchType());
            assertEquals(RankType.ORDER, nsr.getRank());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    // This is an African Butterfly, not a mollusc
    @Test
    public void testOutOfGeography2() {
        String name = "Myrina silenus ficedula";
        LinnaeanRankClassification classification = new LinnaeanRankClassification();
         classification.setScientificName(name);
        try {
            NameSearchResult nsr = searcher.searchForRecord(classification, true, true, true);
            assertNotNull(nsr);
            assertEquals("ALA_DR7933_2", nsr.getLsid());
            assertEquals(MatchType.RECURSIVE, nsr.getMatchType());
            assertEquals(RankType.GENUS, nsr.getRank());
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
            assertEquals("https://biodiversity.org.au/afd/taxa/4647863b-760d-4b59-aaa1-502c8cdf8d3c", lsid);
            lsid = searcher.searchForLSID("Bacteria");
            assertEquals("NZOR-6-73174", lsid);
        } catch (SearchResultException e) {
            e.printStackTrace();
            fail("testNoRank failed");
        }
    }

    @Test
    public void testGetPrimaryLsid1() {
        String primaryLsid = searcher.getPrimaryLsid("https://id.biodiversity.org.au/node/apni/2889838");
        assertEquals("https://id.biodiversity.org.au/node/apni/2889838", primaryLsid);
    }

    @Test
    public void testGetPrimaryLsid2() {
        String primaryLsid = searcher.getPrimaryLsid("http://id.biodiversity.org.au/node/apni/2890752");
        assertEquals("https://id.biodiversity.org.au/node/apni/2890752", primaryLsid);
    }

    @Test
    public void testGetPrimaryLsid3() {
        String primaryLsid = searcher.getPrimaryLsid("https://id.biodiversity.org.au/instance/apni/707711");
        assertEquals("https://id.biodiversity.org.au/node/apni/2890752", primaryLsid);
    }

    @Test
    public void testGetPrimaryLsid4() {
        String primaryLsid = searcher.getPrimaryLsid("ALA_DR655_36");
        assertEquals("https://id.biodiversity.org.au/node/apni/2917784", primaryLsid);
    }

    @Test
    public void testGetPrimaryLsid5() {
        String primaryLsid = searcher.getPrimaryLsid("urn:lsid:biodiversity.org.au:afd.taxon:f71a4c71-48e1-4a9f-840e-1bb189611fd4");
        assertEquals("https://biodiversity.org.au/afd/taxa/f71a4c71-48e1-4a9f-840e-1bb189611fd4", primaryLsid);
    }

    @Test
    public void testGetPrimaryLsid6() {
        String primaryLsid = searcher.getPrimaryLsid("http://biodiversity.org.au/afd/taxa/f71a4c71-48e1-4a9f-840e-1bb189611fd4");
        assertEquals("https://biodiversity.org.au/afd/taxa/f71a4c71-48e1-4a9f-840e-1bb189611fd4", primaryLsid);
    }

    @Test
    public void testSearchForRecordByLsid1() {
        String lsid = "https://id.biodiversity.org.au/instance/apni/885617";
        NameSearchResult nsr = searcher.searchForRecordByLsid(lsid);
        assertNotNull(nsr);
        assertEquals(lsid, nsr.getLsid());
    }

    @Test
    public void testSearchForRecordByLsid2() {
        String actual = "https://id.biodiversity.org.au/instance/apni/885617";
        String lsid = actual.replace("https:", "http:");
        NameSearchResult nsr = searcher.searchForRecordByLsid(lsid);
        assertNotNull(nsr);
        assertEquals(actual, nsr.getLsid());
    }

    @Test
    public void testSearchForRecordByLsid3() {
        String actual = "https://biodiversity.org.au/afd/taxa/f71a4c71-48e1-4a9f-840e-1bb189611fd4";
        String lsid = actual.replace("https://biodiversity.org.au/afd/taxa/", "urn:lsid:biodiversity.org.au:afd.taxon:");
        NameSearchResult nsr = searcher.searchForRecordByLsid(lsid);
        assertNotNull(nsr);
        assertEquals(actual, nsr.getLsid());
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
            assertEquals("https://biodiversity.org.au/afd/taxa/b1d9bf29-648f-47e6-8544-2c2fbdf632b1", lsid);
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
            assertEquals("https://biodiversity.org.au/afd/taxa/d02923bc-cf54-4d7f-ae74-aac1d6af1830", nsr.getLsid());
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
        String id = "https://id.biodiversity.org.au/node/apni/2893343";
        String name = "Allocasuarina huegeliana";
        NameSearchResult result = searcher.searchForRecordByID(id);
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals(id, result.getLsid());
        assertEquals(MatchType.TAXON_ID, result.getMatchType());
        assertEquals(name, result.getRankClassification().getScientificName());
    }

    @Test
    public void testLSIDLookup1() {
        String id = "https://id.biodiversity.org.au/node/apni/2893343";
        String name = "Allocasuarina huegeliana";
        NameSearchResult result = searcher.searchForRecordByLsid(id);
        assertNotNull(result);
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
        assertEquals("https://biodiversity.org.au/afd/taxa/e6aff6af-ff36-4ad5-95f2-2dfdcca8caff", lsid);
        assertEquals("Osphranter rufus", sciName);
    }

    @Test
    public void testCommonNames2() {
        String name = "Yellow-tailed Black-Cockatoo";
        String lsid = getCommonNameLSID(name);
        String sciName = getCommonName(name);
        assertEquals("https://biodiversity.org.au/afd/taxa/145b081d-eca7-4d9b-9171-b97e2d061536", lsid);
        assertEquals("Zanda funerea", sciName);
    }

    @Test
    public void testCommonNames3() {
        String name = "Scarlet Robin";
        String lsid = getCommonNameLSID(name);
        String sciName = getCommonName(name);
        assertEquals("https://biodiversity.org.au/afd/taxa/a3e5376b-f9e6-4bdf-adae-1e7add9f5c29", lsid);
        assertEquals("Petroica (Petroica) boodang", sciName);
    }

    @Test
    public void testCommonNames4() {
        String name = "Pacific Bluefin Tuna";
        String lsid = getCommonNameLSID(name);
        String sciName = getCommonName(name);
        assertEquals("https://biodiversity.org.au/afd/taxa/b35bf6d6-3b67-4d4c-b81e-b7ca7a64d341", lsid);
        assertEquals("Thunnus orientalis", sciName);
    }

    @Test
    public void testCommonNames5() {
        String name = "Pacific Black Duck";
        String lsid = getCommonNameLSID(name);
        String sciName = getCommonName(name);
        assertEquals("https://biodiversity.org.au/afd/taxa/81be58f5-caf7-4f3d-b1eb-d4f83eb0af5a", lsid);
        assertEquals("Anas (Anas) superciliosa", sciName);
    }

    @Test
    public void testCommonNames6() {
        String name = "European Carp";
        String lsid = getCommonNameLSID(name);
        String sciName = getCommonName(name);
        assertEquals("https://biodiversity.org.au/afd/taxa/16171fac-8d6c-4327-9fab-f2db864d71bf", lsid);
        assertEquals("Cyprinus carpio", sciName);
    }

    @Test
    public void testCommonNames7() {
        String name = "Sulphur-crested Cockatoo";
        String lsid = getCommonNameLSID(name);
        assertEquals("https://biodiversity.org.au/afd/taxa/2c33a1fd-34f4-48ec-9ae6-38b51f2aa7ea", lsid);
        name = "Sulphur crested Cockatoo";
        lsid = getCommonNameLSID(name);
        assertEquals("https://biodiversity.org.au/afd/taxa/2c33a1fd-34f4-48ec-9ae6-38b51f2aa7ea", lsid);
        name = "SULPHUR CRESTED COCKATOO";
        lsid = getCommonNameLSID(name);
        assertEquals("https://biodiversity.org.au/afd/taxa/2c33a1fd-34f4-48ec-9ae6-38b51f2aa7ea", lsid);
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
    public void testCultivar1() throws Exception {
        NameSearchResult result = this.searcher.searchForRecord("Xerochrysum bracteatum");
        assertNotNull(result);
        assertEquals("https://id.biodiversity.org.au/node/apni/2891029", result.getLsid());
        result = this.searcher.searchForRecord("Xerochrysum bracteatum 'Golden Beauty'");
        assertNotNull(result);
        assertEquals("https://id.biodiversity.org.au/name/apni/226061", result.getLsid());
    }

    @Test
    public void testCultivar2() throws Exception {
        NameSearchResult result = this.searcher.searchForRecord("Grevillea 'Exul'");
        assertNotNull(result);
        assertEquals("https://biodiversity.org.au/nsl/services/rest/name/apni/174076", result.getLsid());
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
            assertEquals("https://biodiversity.org.au/afd/taxa/f9d0d9dc-597d-4344-9e06-1704af36b9b1", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSearchForLSID2() {
        try {
            String lsid = searcher.searchForLSID("Anochetus", true);
            assertEquals("https://biodiversity.org.au/afd/taxa/f9d0d9dc-597d-4344-9e06-1704af36b9b1", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSearchForLSID3() {
        try {
            String lsid = searcher.searchForLSID("Anochetus", true);
            assertEquals("https://biodiversity.org.au/afd/taxa/f9d0d9dc-597d-4344-9e06-1704af36b9b1", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSearchForLSID4() {
        try {
            String lsid = searcher.searchForLSID("Anochetus", RankType.GENUS);
            assertEquals("https://biodiversity.org.au/afd/taxa/f9d0d9dc-597d-4344-9e06-1704af36b9b1", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSearchForLSID5() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Arthropoda", "Insecta", "Hymenoptera", "Formicidae", "Anochetus", null);
            String lsid = searcher.searchForLSID("Anochetus", cl, RankType.GENUS);
            assertEquals("https://biodiversity.org.au/afd/taxa/f9d0d9dc-597d-4344-9e06-1704af36b9b1", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSearchForLSID6() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Arthropoda", "Insecta", "Hymenoptera", "Formicidae", "Anochetus", null);
            String lsid = searcher.searchForLSID(cl, true);
            assertEquals("https://biodiversity.org.au/afd/taxa/f9d0d9dc-597d-4344-9e06-1704af36b9b1", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    @Test
    public void testSearchForLSID7() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Arthropoda", "Insecta", "Hymenoptera", "Formicidae", "Anochetus", null);
            String lsid = searcher.searchForLSID(cl, true);
            assertEquals("https://biodiversity.org.au/afd/taxa/f9d0d9dc-597d-4344-9e06-1704af36b9b1", lsid);
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex.getMessage());
        }
    }

    // Issue 171
    @Test
    public void testSearchForRecordByLSID1() throws Exception {
        NameSearchResult result = this.searcher.searchForRecordByLsid("https://id.biodiversity.org.au/name/apni/245363");
        assertNotNull(result);
        assertEquals("https://id.biodiversity.org.au/name/apni/245363", result.getLsid());
        assertEquals("Hibbertia ericifolia subsp. acutifolia", result.getRankClassification().getScientificName());
        assertEquals(MatchType.TAXON_ID, result.getMatchType());
    }


    @Test
    public void testFuzzyMatches() throws Exception {
        //Eolophus  roseicapilla - non fuzzy match
        assertEquals("https://biodiversity.org.au/afd/taxa/9b4ad548-8bb3-486a-ab0a-905506c463ea", searcher.searchForLSID("Eolophus roseicapilla"));

        //Eolophus roseicapillus - fuzzy match
        assertEquals("https://biodiversity.org.au/afd/taxa/9b4ad548-8bb3-486a-ab0a-905506c463ea", searcher.searchForLSID("Eolophus roseicapillus", true));
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
            assertEquals("https://id.biodiversity.org.au/node/apni/7178434", searcher.searchForLSID(name, null));
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
        assertEquals("https://id.biodiversity.org.au/node/apni/2905993", searcher.searchForLSID(name));
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
            String name = "Poodytes gramineus";
            NameSearchResult nsr = searcher.searchForRecord(name);
            assertNotNull(nsr);
            assertEquals("https://biodiversity.org.au/afd/taxa/061fef09-7c9d-4b6d-9827-4da13a350dc6", nsr.getLsid());
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
            assertEquals("https://biodiversity.org.au/afd/taxa/a51dca29-50e7-49b4-ae35-5c35a9c4f854", nsr.getLsid());
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
            assertEquals("https://id.biodiversity.org.au/node/apni/2902250", nsr.getLsid());
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
            assertEquals("https://id.biodiversity.org.au/instance/fungi/60071845", nsr.getLsid());
            assertEquals( "https://id.biodiversity.org.au/node/fungi/60098663", nsr.getAcceptedLsid());
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
            assertEquals("https://id.biodiversity.org.au/node/ausmoss/10057678", nsr.getLsid());
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
            assertEquals("NZOR-6-29460", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    @Test
    public void testSimpleLookup8()  {
        try {
            String name = "Carbo ater";
            NameSearchResult nsr = searcher.searchForRecord(name);
            fail("Expecting excluded name exception");
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
            assertEquals("https://biodiversity.org.au/afd/taxa/953a5af4-2932-4c8b-8f33-850b5f8f3fed", nsr.getLsid());
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
            assertEquals("https://id.biodiversity.org.au/node/apni/2889217", nsr.getAcceptedLsid());
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

    // Do not match nom illeg. taxonomic status
    @Test
    public void testSimpleLookup13() throws Exception {
        String name = "Banksia collina";
        NameSearchResult nsr = searcher.searchForRecord(name);
        assertNotNull(nsr);
        assertEquals("https://id.biodiversity.org.au/instance/apni/838699", nsr.getLsid());
    }

    @Test
    public void testSimpleLookup14() throws Exception {
        String name = "Stephanopis similis";
        NameSearchResult nsr = searcher.searchForRecord(name);
        assertNotNull(nsr);
        assertEquals("https://biodiversity.org.au/afd/taxa/24bc164a-85b2-4633-85c5-a3b399daec0a", nsr.getLsid());
    }

    @Test
    public void testSimpleLookup15() throws Exception {
        String name = "Fraus latistria";
        NameSearchResult nsr = searcher.searchForRecord(name);
        assertNotNull(nsr);
        assertEquals("https://biodiversity.org.au/afd/taxa/2358fcc0-8db2-475d-8da4-fd4bd5e711f2", nsr.getLsid());
    }

    @Test
    public void testSimpleLookup16() throws Exception {
        String name = "Metrosideros fulgens";
        NameSearchResult nsr = searcher.searchForRecord(name);
        assertNotNull(nsr);
        assertEquals("NZOR-6-117997", nsr.getLsid());
    }


    @Test
    public void testSimpleLookup17() throws Exception {
        String name = "Metrosideros scandens";
        NameSearchResult nsr = searcher.searchForRecord(name);
        assertNotNull(nsr);
        assertEquals("NZOR-6-86045", nsr.getLsid());
    }

    @Test
    public void testSimpleLookup18() throws Exception {
        String name = "Poaceae";
        NameSearchResult nsr = searcher.searchForRecord(name);
        assertNotNull(nsr);
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51352071", nsr.getLsid());
    }


    @Test
    public void testAffLookup1() throws Exception  {
        String name = "Pterostylis sp. aff. boormanii (Beechworth)";
        NameSearchResult nsr = searcher.searchForRecord(name);
        assertNotNull(nsr);
        assertEquals("ALA_DR655_403", nsr.getLsid());
        name = "Pterostylis sp. aff. boormanii";
        nsr = searcher.searchForRecord(name);
        assertNotNull(nsr);
        assertEquals("https://id.biodiversity.org.au/instance/apni/51411749", nsr.getLsid());
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51412340", nsr.getAcceptedLsid());
    }


    @Test
    public void testMetricsLookup1() throws Exception {
        String name = "Geopelia placida";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true, true);
        assertNotNull(metrics);
        assertEquals("https://biodiversity.org.au/afd/taxa/3d5c4e0d-5138-46e0-8e14-5acd8fd2c523", metrics.getResult().getLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
        assertTrue(metrics.getErrors().contains(ErrorType.PARENT_CHILD_SYNONYM));
    }

    @Ignore // Until sub-taxon synonymy decided
    @Test
    public void testMetricsLookup2() throws Exception {
        String name = "Trigonaphera vinnulum"; // Synonym of Trigonostoma vinnulum
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true, true);
        assertNotNull(metrics);
        assertEquals("https://biodiversity.org.au/afd/taxa/7e67e588-927e-48a9-8765-365ae9f25fcb", metrics.getResult().getLsid());
        assertEquals("https://biodiversity.org.au/afd/taxa/5855a347-eee2-47bb-8130-94d49602d232", metrics.getResult().getAcceptedLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
        assertTrue(metrics.getErrors().contains(ErrorType.PARENT_CHILD_SYNONYM));
    }

    // Ensure Eucalyptus de beuzevillei does not gum up the works
    @Test
    public void testMetricsLookup3() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName("Eucalyptus");
        cl.setFamily("Myrtaceae");
        cl.setGenus("Eucalyptus");
        cl.setRank("species");
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true, true);
        assertNotNull(metrics);
        assertEquals("https://id.biodiversity.org.au/instance/apni/854042", metrics.getResult().getLsid());
        assertEquals("https://id.biodiversity.org.au/node/apni/2896227", metrics.getResult().getAcceptedLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
        assertEquals(new HashSet<>(Arrays.asList(ErrorType.NONE)), metrics.getErrors());
    }

    // Location-specific populations of Koalas
    @Test
    public void testMetricsLookup4() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName("Phascolarctos cinereus (Koala)");
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true, true);
        assertNotNull(metrics);
        assertEquals("ALA_DR656_1402", metrics.getResult().getLsid());
        assertNull(metrics.getResult().getAcceptedLsid());
        assertEquals(MatchType.CANONICAL, metrics.getResult().getMatchType());
        assertEquals(new HashSet<>(Arrays.asList(ErrorType.NONE)), metrics.getErrors());

        cl = new LinnaeanRankClassification();
        cl.setScientificName("Phascolarctos cinereus (combined populations of Qld, NSW and the ACT)");
        metrics = searcher.searchForRecordMetrics(cl, true, true);
        assertNotNull(metrics);
        assertEquals("ALA_DR656_1402", metrics.getResult().getLsid());
        assertNull(metrics.getResult().getAcceptedLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
        assertEquals(new HashSet<>(Arrays.asList(ErrorType.NONE)), metrics.getErrors());

        cl = new LinnaeanRankClassification();
        cl.setScientificName("Phascolarctos cinereus (Koala, Guba)");
        metrics = searcher.searchForRecordMetrics(cl, true, true);
        assertNotNull(metrics);
        assertEquals("ALA_DR656_1402", metrics.getResult().getLsid());
        assertNull(metrics.getResult().getAcceptedLsid());
        assertEquals(MatchType.CANONICAL, metrics.getResult().getMatchType());
        assertEquals(new HashSet<>(Arrays.asList(ErrorType.NONE)), metrics.getErrors());

        cl = new LinnaeanRankClassification();
        cl.setScientificName("Phascolarctos cinereus (Koala, Guba)");
        metrics = searcher.searchForRecordMetrics(cl, true, true);
        assertNotNull(metrics);
        assertEquals("ALA_DR656_1402", metrics.getResult().getLsid());
        assertNull(metrics.getResult().getAcceptedLsid());
        assertEquals(MatchType.CANONICAL, metrics.getResult().getMatchType());
        assertEquals(new HashSet<>(Arrays.asList(ErrorType.NONE)), metrics.getErrors());

        cl = new LinnaeanRankClassification();
        cl.setScientificName("Phascolarctos cinereus ( Koala )");
        metrics = searcher.searchForRecordMetrics(cl, true, true);
        assertNotNull(metrics);
        assertEquals("ALA_DR656_1402", metrics.getResult().getLsid());
        assertNull(metrics.getResult().getAcceptedLsid());
        assertEquals(MatchType.CANONICAL, metrics.getResult().getMatchType());
        assertEquals(new HashSet<>(Arrays.asList(ErrorType.NONE)), metrics.getErrors());
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
            assertEquals("https://biodiversity.org.au/afd/taxa/3e062650-6ecb-43e7-a903-5487e3dbbbb5", nsr.getLsid());
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
            assertEquals("https://id.biodiversity.org.au/node/fungi/60083449", nsr.getLsid());
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
            assertEquals("https://biodiversity.org.au/afd/taxa/426ab801-0d5f-4b43-b1b4-55ce7ce7a44e", nsr.getLsid());
            assertEquals("https://biodiversity.org.au/afd/taxa/6c212123-fadc-4307-8dd8-ac501bb534ba", nsr.getAcceptedLsid());
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
            assertEquals("https://id.biodiversity.org.au/taxon/apni/51300001", nsr.getLsid());
            nsr = ex.getMisappliedResult();
            assertEquals("Correa eburnea", nsr.getRankClassification().getSpecies());
            assertEquals("https://id.biodiversity.org.au/node/apni/2910182", nsr.getLsid());
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
        assertEquals("https://id.biodiversity.org.au/instance/apni/859716", nsr.getLsid());
        assertEquals("https://id.biodiversity.org.au/node/apni/2906575", nsr.getAcceptedLsid());
    }


    @Test
    public void testHigherTaxonMatch1()  {
        try {
            String name = "Breutelia scoparia";
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName(name);
            NameSearchResult nsr = searcher.searchForRecord(cl, true);
            assertNotNull(nsr);
            assertEquals("https://id.biodiversity.org.au/node/ausmoss/10061546", nsr.getLsid());
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
    public void testHomonymWithOrderResolution1() throws Exception  {
        try {
            String name = "Abelia";
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName(name);
            NameSearchResult nsr = searcher.searchForRecord(cl, true);
            fail("Expecting homonym exception");
        } catch (HomonymException ex) {
            assertEquals(2, ex.getResults().size());
        }
        String name = "Abelia";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        cl.setOrder("Dipsacales");
        NameSearchResult nsr = searcher.searchForRecord(cl, true);
        assertNotNull(nsr);
        assertEquals("https://id.biodiversity.org.au/node/apni/2892114", nsr.getLsid());
     }

    @Test
    public void testMultipleMisappliedResolution1() throws Exception {
        String name = "Cressa cretica";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("https://id.biodiversity.org.au/node/apni/2887824", metrics.getResult().getLsid());
        assertEquals(MatchType.TAXON_ID, metrics.getResult().getMatchType());
        assertTrue(metrics.getErrors().contains(ErrorType.MISAPPLIED));
    }

    @Test
    public void testMultipleMisappliedResolution2() throws Exception {
        String name = "Acrotriche divaricata";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("https://id.biodiversity.org.au/node/apni/2905435", metrics.getResult().getLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
        assertTrue(metrics.getErrors().contains(ErrorType.MATCH_MISAPPLIED));
    }

    // Multiple misapplied, to different things.
    @Test
    public void testMultipleMisappliedResolution3() throws Exception {
        String name = "Potamogeton obtusifolius";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("https://id.biodiversity.org.au/node/apni/8770682", metrics.getResult().getLsid());
        assertEquals(MatchType.RECURSIVE, metrics.getResult().getMatchType());
        assertTrue(metrics.getErrors().contains(ErrorType.MISAPPLIED));
    }

    // Ensure misapplication is ignored
    @Test
    public void testMultipleMisappliedResolution4() throws Exception {
        String name = "Pterostylis bryophila";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51412050", metrics.getResult().getLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
        name = "Pterostylis obtusa";
        cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51412242", metrics.getResult().getLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
        assertTrue(metrics.getErrors().contains(ErrorType.MATCH_MISAPPLIED));
    }


    // Synonym and accepted
    @Test
    public void testSynonymAccepted1() throws Exception {
        String name = "Acacia longissima";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51286968", metrics.getResult().getLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
        assertTrue(metrics.getErrors().contains(ErrorType.NONE));
    }

    @Test
    public void testSynonymAccepted2() throws Exception {
        String name = "Acacia longissima H.L.Wendl.";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51286968", metrics.getResult().getLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
        assertTrue(metrics.getErrors().contains(ErrorType.NONE));
    }

    @Test
    public void testSynonymAccepted3() throws Exception {
        String name = "Acacia longissima Chopinet";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("https://id.biodiversity.org.au/instance/apni/857118", metrics.getResult().getLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
        assertTrue(metrics.getErrors().contains(ErrorType.NONE));
        assertEquals("https://id.biodiversity.org.au/node/apni/2911212", metrics.getResult().getAcceptedLsid());
    }


    @Test
    public void testSynonymAccepted4() throws Exception {
        String name = "Sugomel niger";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("ALA_DR7933_6", metrics.getResult().getLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
        assertTrue(metrics.getErrors().contains(ErrorType.NONE));
        assertEquals("https://biodiversity.org.au/afd/taxa/b32a2ec6-315c-48cf-84b3-4898e39f4b57", metrics.getResult().getAcceptedLsid());
    }

    // Available as a synonym but also misapplied.
    @Test
    public void testSynonymMisapplied1() throws Exception {
        String name = "Commersonia fraseri ";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals(SynonymType.OBJECTIVE_SYNONYM, metrics.getResult().getSynonymType());
        assertEquals("https://id.biodiversity.org.au/instance/apni/948382", metrics.getResult().getLsid());
        assertEquals("https://id.biodiversity.org.au/node/apni/2916208", metrics.getResult().getAcceptedLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
        assertTrue(metrics.getErrors().contains(ErrorType.MATCH_MISAPPLIED));
    }

    // Available as a synonym but also misapplied.
    @Test
    public void testSynonymMisapplied2() throws Exception {
        String name = "Rulingia rugosa";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals(SynonymType.OBJECTIVE_SYNONYM, metrics.getResult().getSynonymType());
        assertEquals("https://id.biodiversity.org.au/instance/apni/949024", metrics.getResult().getLsid());
        assertEquals("https://id.biodiversity.org.au/node/apni/2891679", metrics.getResult().getAcceptedLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
        assertTrue(metrics.getErrors().contains(ErrorType.MATCH_MISAPPLIED));
    }


    // Higher taxonomy only filled out
    @Test
    public void testHigherTaxonomy() throws Exception {
        String family = "Pterophoridae";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setFamily(family);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("https://biodiversity.org.au/afd/taxa/81da9a0d-ecb6-4040-a56d-12a44042b63b", metrics.getResult().getLsid());
        assertEquals(RankType.FAMILY, metrics.getResult().getRank());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
    }

    // Phrase name with rank marker
    @Test
    public void testPhraseName1() throws Exception {
        String name = "Tephrosia sp. Crowded pinnae (C.R.Dunlop 8202)";
        String kingdom = "Plantae";
        String phylum = "Streptophyta";
        String class_ = "Equisetopsida";
        String order = "Fabales";
        String genus = "Tephrosia";
        String specificEpithet = "sp. Crowded pinnae (C.R.Dunlop 8202)";
        String rank = "species";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setKingdom(kingdom);
        cl.setPhylum(phylum);
        cl.setKlass(class_);
        cl.setOrder(order);
        cl.setGenus(genus);
        cl.setSpecificEpithet(specificEpithet);
        cl.setRank(rank);
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("https://id.biodiversity.org.au/instance/apni/932722", metrics.getResult().getLsid());
        assertEquals("https://id.biodiversity.org.au/node/apni/2890778", metrics.getResult().getAcceptedLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
    }

    @Test
    public void testPhraseName2() throws Exception {
        String name = "Tephrosia sp. (Miriam Vale E.J.Thompson+ MIR33)";
        String kingdom = "Plantae";
        String class_ = "Equisetopsida";
        String genus = "Tephrosia";
         String rank = "species";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setKingdom(kingdom);
        cl.setKlass(class_);
        cl.setGenus(genus);
        //cl.setRank(rank);
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("https://id.biodiversity.org.au/instance/apni/51376249", metrics.getResult().getLsid());
        assertEquals("https://id.biodiversity.org.au/node/apni/2903953", metrics.getResult().getAcceptedLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
        assertEquals(SynonymType.SUBJECTIVE_SYNONYM, metrics.getResult().getSynonymType());
    }

    @Test
    public void testPhraseName3() throws Exception {
        String name = "Thryptomene sp. Leinster (B.J. Lepschi & L.A. Craven 4362) PN";
        String kingdom = "Plantae";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setKingdom(kingdom);
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("https://id.biodiversity.org.au/node/apni/2904210", metrics.getResult().getLsid());
        assertEquals(MatchType.PHRASE, metrics.getResult().getMatchType());
    }

    @Test
    public void testPhraseName4() throws Exception {
        String name = "Tephrosia sp. Miriam Vale (E.J.Thompson+ MIR33) WA Herbarium";
        String kingdom = "Plantae";
        String class_ = "Equisetopsida";
        String genus = "Tephrosia";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setKingdom(kingdom);
        cl.setKlass(class_);
        cl.setGenus(genus);
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("https://id.biodiversity.org.au/node/apni/2903953", metrics.getResult().getLsid());
        assertEquals(MatchType.PHRASE, metrics.getResult().getMatchType());
    }

    @Test
    public void testPhraseName5() throws Exception {
        String name = "Galaxias sp. 14";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("https://biodiversity.org.au/afd/taxa/dd9ccbd9-2b23-4e64-8a68-ec70e2146770", metrics.getResult().getLsid());
        assertEquals(MatchType.PHRASE, metrics.getResult().getMatchType());
    }

    // Ensure illegitimate names are excluded from the system and don't gum the works up
    @Test
    public void testIllegitimate1() throws Exception {
        String name = "Banksia collina";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("https://id.biodiversity.org.au/instance/apni/838699", metrics.getResult().getLsid());
        assertEquals("https://id.biodiversity.org.au/node/apni/2900678", metrics.getResult().getAcceptedLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
        assertEquals(SynonymType.OBJECTIVE_SYNONYM, metrics.getResult().getSynonymType());
    }

    // Ensure illegitimate names are excluded from the system and don't gum the works up
    @Test
    public void testIllegitimate2() throws Exception {
        String name = "Zieria fordii";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("ALA_DR652_1992", metrics.getResult().getLsid());
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51367864", metrics.getResult().getRankClassification().getGid());
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51367862", metrics.getResult().getRankClassification().getFid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
    }


    // Test vile examples where someone has put in something like Genus N27 or Genus B
    @Test
    public void testGenusMarker1() throws Exception {
        String name = "Genus NC72";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals(NameType.INFORMAL, metrics.getNameType());
        assertNull(metrics.getResult());
    }


    // Test vile examples where someone has put in something like Genus N27 or Genus B
    @Test
    public void testGenusMarker2() throws Exception {
        String name = "Genus B";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("https://biodiversity.org.au/afd/taxa/18997fe9-4fc7-4327-b962-e921cfee45c7", metrics.getResult().getLsid());
        assertEquals("https://biodiversity.org.au/afd/taxa/4c582775-3afe-4076-b919-3251f515e7c1", metrics.getResult().getAcceptedLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
    }


    // Test wierd virus names
    @Test
    public void testVirus1() throws Exception {
        String name = "Arbovirus: Exotic West Nile virus";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertNotNull(metrics);
        assertEquals("ALA_DR18234_49", metrics.getResult().getLsid());
        assertNull(metrics.getResult().getAcceptedLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
    }

    // Test looking for an accepted name with multiple misapplications (for lists tool)
    // Issue
    @Test
    public void testMisappliedStrict1() throws Exception {
        String name = "Caladenia dilatata";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName(name);
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, false);
        assertNotNull(metrics);
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51398946", metrics.getResult().getLsid());
        assertNull(metrics.getResult().getAcceptedLsid());
        assertEquals(MatchType.EXACT, metrics.getResult().getMatchType());
        assertTrue(metrics.getErrors().contains(ErrorType.MATCH_MISAPPLIED));
    }

}
