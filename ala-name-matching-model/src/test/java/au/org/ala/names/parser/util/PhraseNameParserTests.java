package au.org.ala.names.parser.util;

import au.org.ala.names.model.ALAParsedName;
import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.nameparser.PhraseNameParser;
import org.junit.Ignore;
import org.junit.Test;

import java.io.FileReader;

import static org.junit.Assert.*;

/**
 * Tests for the extra ala name parsing
 *
 * @author Natasha Carter
 */
public class PhraseNameParserTests {
    @Test
    public void testNameWithAuthor1() throws Exception {
        String name = "Trachymene incisa Rudge subsp. incisa";
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse(name);
        assertEquals(pn.getType(), NameType.SCIENTIFIC);
        assertEquals("Trachymene", pn.getGenusOrAbove());
        assertEquals("incisa", pn.getSpecificEpithet());
        assertEquals("incisa", pn.getInfraSpecificEpithet());
        //assertEquals("Rudge", pn.getAuthorship());
        assertEquals(Rank.SUBSPECIES, pn.getRank());
    }
    @Test
    public void testNameWithAuthor2() throws Exception {
        String name = "Trachymene incisa subsp. incisa Rudge ";
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse(name);
        assertEquals(pn.getType(), NameType.SCIENTIFIC);
        assertEquals("Trachymene", pn.getGenusOrAbove());
        assertEquals("incisa", pn.getSpecificEpithet());
        assertEquals("incisa", pn.getInfraSpecificEpithet());
        assertEquals("Rudge", pn.getAuthorship());
        assertEquals(Rank.SUBSPECIES, pn.getRank());
    }

    @Test
    public void testNameWithoutAuthor() throws Exception {
        String name = "Pterodroma arminjoniana s. str.";
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse(name);
        assertEquals(pn.getType(), NameType.SCIENTIFIC);
        assertEquals("Pterodroma", pn.getGenusOrAbove());
        assertEquals("arminjoniana", pn.getSpecificEpithet());
        assertEquals(Rank.SPECIES, pn.getRank());
    }

    @Test
    public void testNameWithSubgenus() throws Exception {
        String name = "Serpula (hydroides) multispinosa";
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse(name);
        pn = parser.parse(name);
        assertEquals(pn.getType(), NameType.SCIENTIFIC);
        assertEquals("Serpula", pn.getGenusOrAbove());
        assertEquals("multispinosa", pn.getSpecificEpithet());
        assertEquals("Hydroides", pn.getInfraGeneric());
        assertNull(pn.getAuthorship());
        assertEquals(Rank.SPECIES, pn.getRank());
    }

    @Test
    public void phraseNameTest1() throws Exception {
        String name = "Elaeocarpus sp. Rocky Creek";
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse(name);
        assertTrue(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.INFORMAL);
        assertEquals("Elaeocarpus", pn.getGenusOrAbove());
        assertEquals("Rocky Creek", pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertEquals("Rocky Creek", ((ALAParsedName) pn).cleanPhrase);
        assertEquals(Rank.SPECIES, pn.getRank());
    }

    @Test
    public void phraseNameTest2() throws Exception {
        String name = "Thelymitra sp. adorata";
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse(name);
        assertFalse(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.SCIENTIFIC);
        assertEquals("Thelymitra", pn.getGenusOrAbove());
        assertEquals("adorata", pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertEquals(Rank.SPECIES, pn.getRank());

    }

    @Test
    public void phraseNameTest3() throws Exception {
        String name = "Asterolasia sp. \"Dungowan Creek\"";
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse(name);
        assertTrue(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.CULTIVAR);
        assertEquals("Asterolasia", pn.getGenusOrAbove());
        assertNull(pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertEquals("\"Dungowan Creek\"", pn.getCultivarEpithet());
        assertEquals(Rank.CULTIVAR, pn.getRank());
    }

    @Test
    public void phraseNameTest4() throws Exception {
        String name = "Pultenaea sp. 'Olinda' (Coveny 6616)";
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse(name);
        pn = parser.parse(name);
        assertTrue(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.CULTIVAR);
        assertEquals("Pultenaea", pn.getGenusOrAbove());
        assertNull(pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertEquals("'Olinda'", pn.getCultivarEpithet());
        assertEquals("(Coveny 6616)", ((ALAParsedName) pn).getPhraseVoucher());
        assertEquals("Olinda", ((ALAParsedName) pn).cleanPhrase);
        assertEquals(Rank.CULTIVAR, pn.getRank());
    }

    @Test
    public void phraseNameTest5() throws Exception {
        String name = "Thelymitra aff. pauciflora";
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse(name);
        assertFalse(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.INFORMAL);
        assertEquals("Thelymitra", pn.getGenusOrAbove());
        assertEquals("pauciflora", pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertEquals(Rank.SPECIES, pn.getRank());
    }

    @Test
    public void phraseNameTest6() throws Exception {
        String name = "Corymbia ?hendersonii K.D.Hill & L.A.S.Johnson";
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse(name);
        assertFalse(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.DOUBTFUL);
        assertEquals("Corymbia", pn.getGenusOrAbove());
        assertNull(pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertNull(pn.getAuthorship());
        assertNull(pn.getRank());
    }

    @Test
    public void testVoucherClean1() throws Exception {
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse("Marsilea sp. Neutral Junction (D.E.Albrecht 9192)");
        assertTrue(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.INFORMAL);
        assertEquals("Marsilea", pn.getGenusOrAbove());
        assertEquals("Neutral Junction", pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertNull(pn.getCultivarEpithet());
        assertEquals("(D.E.Albrecht 9192)", ((ALAParsedName) pn).getPhraseVoucher());
        assertEquals("Neutral Junction", ((ALAParsedName) pn).cleanPhrase);
        assertEquals("Albrecht9192", ((ALAParsedName) pn).cleanVoucher);
        assertEquals(Rank.SPECIES, pn.getRank());
    }

    @Test
    public void testVoucherClean2() throws Exception {
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse("Dampiera sp. Central Wheatbelt (L.W.Sage, F.Hort, C.A.Hollister LWS2321)");
        assertTrue(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.INFORMAL);
        assertEquals("Dampiera", pn.getGenusOrAbove());
        assertEquals("Central Wheatbelt", pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertNull(pn.getCultivarEpithet());
        assertEquals("(L.W.Sage, F.Hort, C.A.Hollister LWS2321)", ((ALAParsedName) pn).getPhraseVoucher());
        assertEquals("Central Wheatbelt", ((ALAParsedName) pn).cleanPhrase);
        assertEquals("SageHortHollisterLWS2321", ((ALAParsedName) pn).cleanVoucher);
        assertEquals(Rank.SPECIES, pn.getRank());
    }

    @Test
    public void testVoucherClean3() throws Exception {
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse("Baeckea sp. Bunney Road (S.Patrick 4059)");
        assertTrue(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.INFORMAL);
        assertEquals("Baeckea", pn.getGenusOrAbove());
        assertEquals("Bunney Road", pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertNull(pn.getCultivarEpithet());
        assertEquals("(S.Patrick 4059)", ((ALAParsedName) pn).getPhraseVoucher());
        assertEquals("Bunney Road", ((ALAParsedName) pn).cleanPhrase);
        assertEquals("Patrick4059", ((ALAParsedName) pn).cleanVoucher);
        assertEquals(Rank.SPECIES, pn.getRank());
    }

    @Test
    public void testVoucherClean4() throws Exception {
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse("Prostanthera sp. Bundjalung Nat. Pk. (B.J.Conn 3471)");
        assertTrue(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.INFORMAL);
        assertEquals("Prostanthera", pn.getGenusOrAbove());
        assertEquals("Bundjalung Nat. Pk.", pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertNull(pn.getCultivarEpithet());
        assertEquals("(B.J.Conn 3471)", ((ALAParsedName) pn).getPhraseVoucher());
        assertEquals("Bundjalung Nat. Pk.", ((ALAParsedName) pn).cleanPhrase);
        assertEquals("Conn3471", ((ALAParsedName) pn).cleanVoucher);
        assertEquals(Rank.SPECIES, pn.getRank());
    }

    @Test
    public void testVoucherClean5() throws Exception {
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse("Toechima sp. East Alligator (J.Russell-Smith 8418) NT Herbarium");
        assertTrue(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.INFORMAL);
        assertEquals("Toechima", pn.getGenusOrAbove());
        assertEquals("East Alligator", pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertNull(pn.getCultivarEpithet());
        assertEquals("(J.Russell-Smith 8418)", ((ALAParsedName) pn).getPhraseVoucher());
        assertEquals("East Alligator", ((ALAParsedName) pn).cleanPhrase);
        assertEquals("RussellSmith8418", ((ALAParsedName) pn).cleanVoucher);
        assertEquals("NT Herbarium", ((ALAParsedName) pn).getPhraseNominatingParty());
        assertEquals(Rank.SPECIES, pn.getRank());
    }

    @Test
    public void testSpeciesLevelPhraseName1() throws Exception {
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse("Goodenia sp. Bachsten Creek (M.D. Barrett 685) WA Herbarium");
        assertTrue(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.INFORMAL);
        assertEquals("Goodenia", pn.getGenusOrAbove());
        assertEquals("Bachsten Creek", pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertNull(pn.getCultivarEpithet());
        assertEquals("(M.D. Barrett 685)", ((ALAParsedName) pn).getPhraseVoucher());
        assertEquals("Bachsten Creek", ((ALAParsedName) pn).cleanPhrase);
        assertEquals("Barrett685", ((ALAParsedName) pn).cleanVoucher);
        assertEquals("WA Herbarium", ((ALAParsedName) pn).getPhraseNominatingParty());
        assertEquals(Rank.SPECIES, pn.getRank());
    }

    @Test
    public void testSpeciesLevelPhraseName2() throws Exception {
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse("Baeckea sp. Beringbooding (AR Main 11/9/1957)");
        assertTrue(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.INFORMAL);
        assertEquals("Baeckea", pn.getGenusOrAbove());
        assertEquals("Beringbooding", pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertNull(pn.getCultivarEpithet());
        assertEquals("(AR Main 11/9/1957)", ((ALAParsedName) pn).getPhraseVoucher());
        assertEquals("Beringbooding", ((ALAParsedName) pn).cleanPhrase);
        assertEquals("Main1191957", ((ALAParsedName) pn).cleanVoucher);
        assertEquals(Rank.SPECIES, pn.getRank());
    }

    @Test
    public void testSpeciesLevelPhraseName3() throws Exception {
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse("Baeckea sp. Calingiri (F.Hort 1710)");
        assertTrue(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.INFORMAL);
        assertEquals("Baeckea", pn.getGenusOrAbove());
        assertEquals("Calingiri", pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertNull(pn.getCultivarEpithet());
        assertEquals("(F.Hort 1710)", ((ALAParsedName) pn).getPhraseVoucher());
        assertEquals("Calingiri", ((ALAParsedName) pn).cleanPhrase);
        assertEquals("Hort1710", ((ALAParsedName) pn).cleanVoucher);
        assertEquals(Rank.SPECIES, pn.getRank());
    }

    @Test
    public void testSpeciesLevelPhraseName4() throws Exception {
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse("Baeckea sp. East Yuna (R Spjut & C Edson 7077)");
        assertTrue(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.INFORMAL);
        assertEquals("Baeckea", pn.getGenusOrAbove());
        assertEquals("East Yuna", pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertNull(pn.getCultivarEpithet());
        assertEquals("(R Spjut & C Edson 7077)", ((ALAParsedName) pn).getPhraseVoucher());
        assertEquals("East Yuna", ((ALAParsedName) pn).cleanPhrase);
        assertEquals("SpjutEdson7077", ((ALAParsedName) pn).cleanVoucher);
        assertEquals(Rank.SPECIES, pn.getRank());
    }

    @Test
    public void testSpeciesLevelPhraseName5() throws Exception {
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse("Acacia sp. Goodlands (BR Maslin 7761) [aff. resinosa]");
        assertTrue(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.INFORMAL);
        assertEquals("Acacia", pn.getGenusOrAbove());
        assertEquals("Goodlands", pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertNull(pn.getCultivarEpithet());
        assertEquals("(BR Maslin 7761)", ((ALAParsedName) pn).getPhraseVoucher());
        assertEquals("Goodlands", ((ALAParsedName) pn).cleanPhrase);
        assertEquals("Maslin7761", ((ALAParsedName) pn).cleanVoucher);
        assertEquals(Rank.SPECIES, pn.getRank());
    }

    @Test
    public void testSpeciesLevelPhraseName6() throws Exception {
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse("Acacia sp. Manmanning (BR Maslin 7711) [aff. multispicata]");
        assertTrue(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.INFORMAL);
        assertEquals("Acacia", pn.getGenusOrAbove());
        assertEquals("Manmanning", pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertNull(pn.getCultivarEpithet());
        assertEquals("(BR Maslin 7711)", ((ALAParsedName) pn).getPhraseVoucher());
        assertEquals("Manmanning", ((ALAParsedName) pn).cleanPhrase);
        assertEquals("Maslin7711", ((ALAParsedName) pn).cleanVoucher);
        assertEquals(Rank.SPECIES, pn.getRank());
    }

    @Test
    public void testNormalNamesThroughParser1() throws Exception {
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse("Atrichornis (rahcinta) clamosus");
        assertFalse(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.SCIENTIFIC);
        assertEquals("Atrichornis", pn.getGenusOrAbove());
        assertEquals("Rahcinta", pn.getInfraGeneric());
        assertEquals("clamosus", pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertNull(pn.getCultivarEpithet());
        assertEquals(Rank.SPECIES, pn.getRank());
    }

    @Test
    public void testNormalNamesThroughParser2() throws Exception {
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse("Sunesta setigera macroptera");
        assertFalse(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.SCIENTIFIC);
        assertEquals("Sunesta", pn.getGenusOrAbove());
         assertEquals("setigera", pn.getSpecificEpithet());
        assertEquals("macroptera", pn.getInfraSpecificEpithet());
        assertNull(pn.getCultivarEpithet());
        assertEquals(Rank.INFRASPECIFIC_NAME, pn.getRank());
    }

    @Test
    public void testNormalNamesThroughParser3() throws Exception {
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse("Cleome uncifera subsp. uncifera");
        assertFalse(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.SCIENTIFIC);
        assertEquals("Cleome", pn.getGenusOrAbove());
        assertEquals("uncifera", pn.getSpecificEpithet());
        assertEquals("uncifera", pn.getInfraSpecificEpithet());
        assertNull(pn.getCultivarEpithet());
        assertEquals(Rank.SUBSPECIES, pn.getRank());
    }

    @Test
    public void testNormalNamesThroughParser4() throws Exception {
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse("Plantae Haeckel");
        assertFalse(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.SCIENTIFIC);
        assertEquals("Plantae", pn.getGenusOrAbove());
        assertNull(pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertNull(pn.getCultivarEpithet());
        assertEquals("Haeckel", pn.getAuthorship());
        assertNull(pn.getRank());
    }

    @Test
    public void testNormalNamesThroughParser5() throws Exception {
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse("Luposicya lupus Smith, 1959");
        assertFalse(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.SCIENTIFIC);
        assertEquals("Luposicya", pn.getGenusOrAbove());
        assertEquals("lupus", pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertNull(pn.getCultivarEpithet());
        assertEquals("Smith", pn.getAuthorship());
        assertEquals("1959", pn.getYear());
        assertEquals(1959, pn.getYearInt().intValue());
        assertEquals(Rank.SPECIES, pn.getRank());
    }

    @Test
    public void testNormalNamesThroughParser6() throws Exception {
        PhraseNameParser parser = new PhraseNameParser();
        ParsedName pn = parser.parse("Corymbia hendersonii K.D.Hill & L.A.S.Johnson");
        assertFalse(pn instanceof ALAParsedName);
        assertEquals(pn.getType(), NameType.SCIENTIFIC);
        assertEquals("Corymbia", pn.getGenusOrAbove());
        assertEquals("hendersonii", pn.getSpecificEpithet());
        assertNull(pn.getInfraSpecificEpithet());
        assertNull(pn.getCultivarEpithet());
        assertEquals("K.D.Hill & L.A.S.Johnson", pn.getAuthorship());
        assertNull(pn.getYear());
        assertEquals(Rank.SPECIES, pn.getRank());
    }

    @Test
    public void testVirusName1() throws Exception {
        try {
            PhraseNameParser parser = new PhraseNameParser();
            ParsedName pn = parser.parse("Cucumovirus");
            fail("Expecting unparsable for virus");
        } catch (UnparsableException ex) {
        }
    }

    @Test
    public void testVirusName2() throws Exception {
        try {
            PhraseNameParser parser = new PhraseNameParser();
            ParsedName pn = parser.parse("Cucumber green mottle mosaic virus");
            fail("Expecting unparsable for virus");
        } catch (UnparsableException ex) {
        }
    }

    //@Test
    public void testAllNamesForType() {
        try {
            com.opencsv.CSVReader reader = new com.opencsv.CSVReader(new FileReader("/data/names/Version2011/ala_concepts_dump.txt"), '\t', '"', '\\', 1);
            PhraseNameParser parser = new PhraseNameParser();
            int i = 0;
            for (String[] values = reader.readNext(); values != null; values = reader.readNext()) {
                i++;
                if (values.length != 35)
                    System.out.println("Line " + i + " incorrect length");
                //scientific name only 6th
                String sciName = values[6];
                try {
                    ParsedName pn = parser.parse(sciName);
                    if (pn instanceof ALAParsedName) {
                        System.out.println(sciName);
                    }
                } catch (UnparsableException e) {

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // See https://github.com/AtlasOfLivingAustralia/ala-name-matching/issues/1
    @Test
    public void testRankMarkerPhraseName1() {
        try {
            PhraseNameParser parser = new PhraseNameParser();
            ParsedName pn = parser.parse("Marsilea sp. Neutral Junction (D.E.Albrecht 9192)");
            assertEquals(ALAParsedName.class, pn.getClass());
            assertEquals("Albrecht9192", ((ALAParsedName) pn).cleanVoucher);

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    // See https://github.com/AtlasOfLivingAustralia/ala-name-matching/issues/1
    // Form doesn't seem to work correctly as it is treating the voucher as an authort
    @Test
    @Ignore
    public void testRankMarkerPhraseName2() {
        try {
            PhraseNameParser parser = new PhraseNameParser();
            ParsedName pn = parser.parse("Asparagus asparagoides f. Western Cape (R.Taplin 1133)");
            assertEquals(ALAParsedName.class, pn.getClass());
            assertEquals("Albrecht9192", ((ALAParsedName) pn).cleanVoucher);

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
