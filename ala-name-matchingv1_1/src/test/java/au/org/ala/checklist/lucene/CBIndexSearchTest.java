

package au.org.ala.checklist.lucene;

import au.org.ala.checklist.lucene.model.MatchType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import org.junit.Test;


import au.org.ala.checklist.lucene.model.NameSearchResult;
import au.org.ala.data.util.RankType;
import au.org.ala.data.model.LinnaeanRankClassification;
import org.gbif.ecat.model.ParsedName;
import org.gbif.ecat.parser.NameParser;

/**
 *
 * @author Natasha, Tommy
 */
public class CBIndexSearchTest {
	private static CBIndexSearch searcher;

	@org.junit.BeforeClass
	public static void init() {
		try {
			searcher = new CBIndexSearch("/data/lucene/namematchingv1_1");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

        @Test
        public void testSpeciesConstructFromClassification(){
            try{
                LinnaeanRankClassification cl = new LinnaeanRankClassification();

            }
            catch(Exception e){

            }
        }

        @Test
        public void testQuestionSpeciesMatch(){
            try{
            String name = "Corymbia ?hendersonii K.D.Hill & L.A.S.Johnson";
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setKingdom("Plantae");
            cl.setScientificName(name);
            NameSearchResult nsr =searcher.searchForRecord(cl, true);
            assertEquals("urn:lsid:biodiversity.org.au:apni.taxon:251854",nsr.getLsid());
            //assertEquals(ErrorType.QUESTION_SPECIES, nsr.getErrorType());
            System.out.println(nsr);
            name ="Cacatua leadbeateri";
            //name = "Acacia bartleana ms";
            nsr =searcher.searchForRecord("Cacatua leadbeateri", null);
            assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:fb2de285-c58c-4c63-9268-9beef7c61c16", nsr.getAcceptedLsid());
//            name = "Dendrobium speciosum subsp. hillii";
//            try{
//                searcher.searchForLSID(name);
//                fail("Homonym should be detected.");
//            }
//            catch(HomonymException he){
//
//            }
            name = "Zieria smithii";
            nsr = searcher.searchForRecord(name, null);
            //Cycas media subsp. banksii - C.media subsp. media
            //Boronia crenulata subsp. crenulata var. angustifolia
            //Dendrobium kingianum subsp. kingianum
            //Dendrobium speciosum subsp. capricornicum
            //Dendrobium speciosum subsp. grandiflorum
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

        @Test
        public void testSpMarker(){
            try{
                String name ="Thelymitra sp. adorata";
                NameSearchResult nsr = searcher.searchForRecord(name, null);
                assertEquals("urn:lsid:biodiversity.org.au:apni.taxon:572459", nsr.getLsid());
                name = "Grevillea brachystylis subsp. Busselton (G.J.Keighery s.n. 28/6/1985)";
                nsr = searcher.searchForRecord(name, null);
                System.out.println(nsr);
                name = "Pterodroma arminjoniana s. str.";
                nsr = searcher.searchForRecord(name, null,true);
                System.out.println(nsr);
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

        @Test
        public void testPhraseMatch(){
            try{
                String name ="Elaeocarpus sp. Rocky Creek";
                NameSearchResult nsr = searcher.searchForRecord(name, null);
                //System.out.println(nsr);
                assertEquals(nsr.getLsid(), "urn:lsid:biodiversity.org.au:apni.taxon:245132");
                name = " Pultenaea sp. Olinda";
                nsr = searcher.searchForRecord(name, null);
                System.out.println(nsr);
                name ="Thelymitra sp. adorata";
                nsr = searcher.searchForRecord(name, null);
                System.out.println(nsr);
                name ="Asterolasia sp. \"Dungowan Creek\"";
                nsr = searcher.searchForRecord(name, null);
                //System.out.println(nsr);
                assertEquals(nsr.getLsid(), "urn:lsid:biodiversity.org.au:apni.taxon:270800");


            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

//        @Test
//        public void testBrokenNames(){
//            try{
//                String scientificName ="Thorectandra  6713 \n".trim();
//                String phylum = "Porifera";
//                String order = "Dictyoceratida";
//                String genus = "Thorectandra";
//                String species ="";
//                LinnaeanRankClassification classification = new LinnaeanRankClassification(
//					null, phylum, null, order, null, genus,
//					scientificName);
//                String lsid = searcher.searchForLSID(scientificName, classification,
//					RankType.getForName(null), false);
//                System.out.println(lsid);
//            }
//            catch(Exception e){
//
//            }
//        }
//
        @Test
        public void testSynonymWithoutRank(){
            try{
                LinnaeanRankClassification cl = new LinnaeanRankClassification();
                cl.setKingdom("Animalia");
                cl.setScientificName("Gymnorhina tibicen");
                NameSearchResult nsr = searcher.searchForRecord(cl, true, true);
                assertEquals("Gymnorhina tibicen", nsr.getRankClassification().getScientificName());
                nsr = searcher.searchForRecord("Cracticus tibicen", RankType.SPECIES);
                assertEquals("Cracticus tibicen", nsr.getRankClassification().getScientificName());
                nsr = searcher.searchForRecord("Cracticus tibicen", RankType.GENUS);
                assertEquals(null, nsr);
            }
            catch(Exception e){

            }
        }

        @Test
        public void testRecordSearchWithoutScientificName(){
            try{
                LinnaeanRankClassification cl = new LinnaeanRankClassification(null, null, null, "Hemiptera", "Pentatomidae", null, null);
                System.out.println(searcher.searchForRecord(cl, true));
                System.out.println("Lilianae::: " +searcher.searchForRecord("Lilianae", null));
                System.out.println("Leptospermum: " + searcher.searchForRecord("Leptospermum", null));
                //searcher.searchForLSID("Pulex (Pulex)");

            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

//        @Test
//        public void testAmbiguousSoudEx(){
//            String scientificName = "Marillia fusca";
//            try{
//                searcher.searchForLSID(scientificName, true);
//                fail("Should be ambiguous sound ex");
//            }
//            catch(SearchResultException e){
//                e.printStackTrace();
//            }
//        }

        @Test
        public void testInfragenricAndSoundEx(){
            String nameDifferentEnding = "Phylidonyris pyrrhopterus";
            String nameWithInfraGenric = "Phylidonyris (Phylidonyris) pyrrhoptera (Latham, 1801)";
            String nameDiffEndInfraGeneric = "Phylidonyris (Phylidonyris) pyrrhopterus";
            try{
                NameSearchResult nsr = searcher.searchForRecord(nameDifferentEnding, null, true);
                assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:f3871d29-1201-49eb-bd23-70f2bbc616fe", nsr.getLsid());
                assertEquals(nsr.getMatchType(), MatchType.SOUNDEX);
                nsr = searcher.searchForRecord(nameWithInfraGenric, null, true);
                assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:f3871d29-1201-49eb-bd23-70f2bbc616fe", nsr.getLsid());
                assertEquals(nsr.getMatchType(), MatchType.CANONICAL);
                nsr = searcher.searchForRecord(nameDiffEndInfraGeneric, null, true);
                assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:f3871d29-1201-49eb-bd23-70f2bbc616fe", nsr.getLsid());
                assertEquals(nsr.getMatchType(), MatchType.SOUNDEX);

                System.out.println(searcher.searchForRecord("Latrodectus hasseltii", null,true));
                System.out.println(searcher.searchForRecord("Latrodectus hasselti", null,true));
                System.out.println(searcher.searchForRecord("Elseya belli", null, true));
                System.out.println(searcher.searchForRecord("Grevillea brachystylis subsp. Busselton (G.J.Keighery s.n. 28/6/1985)", null));
                System.out.println(searcher.searchForRecord("Prostanthera sp. Bundjalung Nat. Pk. (B.J.Conn 3471)", null));


//                System.out.println(nameDifferentEnding + " Matches To " + searcher.searchForRecord(nameDifferentEnding, null, true));
//                System.out.println(nameWithInfraGenric + " Matches To " + searcher.searchForRecord(nameWithInfraGenric, null, true));
//                System.out.println(nameDiffEndInfraGeneric + " Matches To " + searcher.searchForRecord(nameDiffEndInfraGeneric, null, true));
            }
            catch(Exception e){
                e.printStackTrace();
                fail("testInfragenericAndSoundEx failed");
            }
        }

       // @Test
        public void testSoundExMatch(){
            String name = "Argyrotegium nitidulus";
            try{
                System.out.println(searcher.searchForRecord(name, null,true));
            }
            catch(Exception e){

            }
        }

        //@Test
        public void testSubspeciesSynonym(){
            try{
            String name = "Turnix castanota magnifica";
            System.out.println(searcher.searchForRecord(name, null));
            System.out.println(searcher.searchForRecord("Baeckea sp. Baladjie (PJ Spencer 24)",null));
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        //@Test
        public void testVirusName(){
            try{
                String name="Cucumovirus cucumber mosaic";
                 NameParser parser = new NameParser();
             ParsedName cn = parser.parse(name);
             System.out.println(cn);
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

        //@Test
        public void testPhraseNames(){
            //All the names below need to map to the same concept
            try{
            String name1 ="Goodenia sp. Bachsten Creek (M.D. Barrett 685) WA Herbarium";
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

            System.out.println(searcher.searchForRecord("Baeckea sp. Bungalbin Hill (BJ Lepschi, LA Craven 4586)",null));

            System.out.println(searcher.searchForRecord("Baeckea sp. Calingiri (F Hort 1710)",null));

            System.out.println(searcher.searchForRecord("Baeckea sp. Calingiri (F Hort 1710)",null));

            System.out.println(searcher.searchForRecord("Acacia sp. Goodlands (BR Maslin 7761) [aff. resinosa]", null));

            System.out.println(searcher.searchForRecord("Acacia sp. Manmanning (BR Maslin 7711) [aff. multispicata]",null));

            //System.out.println(parser.parseExtended(name1));
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

        //@Test
        public void testBadCommonName(){
            try{
                System.out.println("Higher_sulfur_oxides: " +searcher.searchForCommonName("Higher sulfur oxides"));
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        //@Test
        public void testSpeciesSynonymOfSubspecies(){
            LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia","Chordata","Aves","Charadriiformes","Laridae","Larus", "Larus novaehollandiae");
            try{
                System.out.println(searcher.searchForRecord(cl, true));
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

	@Test
	public void testNoRank() {
		try {
			String lsid = searcher.searchForLSID("Animalia");
			System.out.println("testNoRank: " + lsid);

			assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:4647863b-760d-4b59-aaa1-502c8cdf8d3c", lsid);
			lsid = searcher.searchForLSID("Bacteria");
			System.out.println("testNoRank: " + lsid);
			assertEquals("urn:lsid:catalogueoflife.org:taxon:d755c2e0-29c1-102b-9a4a-00304854f820:col20110201", lsid);
		} catch (SearchResultException e) {
			e.printStackTrace();
			fail("testNoRank failed");
		}
	}

	@Test
	public void testGetPrimaryLsid() {
		try {
			String primaryLsid = searcher.getPrimaryLsid("urn:lsid:biodiversity.org.au:afd.taxon:00d9e076-b619-4a65-bd9e-8538d958817a");
			System.out.println("testGetPrimaryLsid: " + primaryLsid);
			assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:00d9e076-b619-4a65-bd9e-8538d958817a", primaryLsid);
		} catch (Exception e) {
			e.printStackTrace();
			fail("testGetPrimaryLsid failed");
		}
	}

	@Test
	public void testSearchForRecordByLsid() {
		try {
			NameSearchResult nsr = searcher.searchForRecordByLsid("urn:lsid:biodiversity.org.au:afd.taxon:00d9e076-b619-4a65-bd9e-8538d958817a");
			System.out.println("testSearchForRecordByLsid: " + nsr);
		} catch (Exception e) {
			e.printStackTrace();
			fail("testSearchForRecordByLsid failed");
		}
	}

	@Test
	public void testSpecies() {
		try {
			NameSearchResult nsr = searcher.searchForRecord(
					"Petaurus australis", RankType.getForId(7000));
			System.out.println("testSpecies: " + nsr.toString() + "!!");
			NameSearchResult expectedResult = new NameSearchResult(String.valueOf(84985), "urn:lsid:biodiversity.org.au:afd.taxon:ca722c6d-6d53-4de6-b296-310621eeffa8", MatchType.EXACT);
			assertTrue(nameSearchResultEqual(expectedResult, nsr));
			//			assertEquals("Match: DIRECT id: 101300 lsid: urn:lsid:biodiversity.org.au:afd.taxon:00d9e076-b619-4a65-bd9e-8538d958817a classification: au.org.ala.data.model.LinnaeanRankClassification@15e232b5[kingdom=Animalia,phylum=Arthropoda,klass=Arachnida,order=Araneae,family=Sparassidae,genus=Holconia,species=Holconia nigrigularis,specificEpithet=<null>,scientificName=Holconia nigrigularis] synonym: null", nsr.toString());
		} catch (SearchResultException e) {
			e.printStackTrace();
			fail("testSpecies failed");
		}
	}

	private void printAllResults(String prefix, List<NameSearchResult> results) {
		System.out.println("## " + prefix + " ##");
		if (results != null && results.size() != 0) {
			for (NameSearchResult result : results)
				System.out.println(result);
		}
		System.out.println("###################################");
	}

	private boolean nameSearchResultEqual(NameSearchResult nsr1, NameSearchResult nsr2) {
		boolean equals = true;

		try {
			if (nsr1.getMatchType() == null && nsr2.getMatchType() == null) {
				equals = true;
			} else if (!nsr1.getMatchType().equals(nsr2.getMatchType())) {
				equals = false;
			}

			if (!nsr1.getId().equals(nsr2.getId())) {
				equals = false;
			}

			if (nsr1.getLsid() == null && nsr2.getLsid() == null) {
				equals = true;
			} else if (!nsr1.getLsid().equals(nsr2.getLsid())) {
				equals = false;
			}

			//			if (nsr1.getRankClassification() == null && nsr2.getRankClassification() == null) {
			//				equals = true;
			//			} else if (!nsr1.getRankClassification().equals(nsr2.getRankClassification())) {
			//				equals = false;
			//			}
			//
			//			if (nsr1.getAcceptedLsid() == null && nsr2.getAcceptedLsid() == null) {
			//				equals = true;
			//			} else if (!nsr1.getAcceptedLsid().equals(nsr2.getAcceptedLsid())) {
			//				equals = false;
			//			}
		} catch (NullPointerException npe) {
			equals = false;
		}

		return equals;
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
			//			cl.setKingdom(null);
			//			results = searcher.searchForRecords("Simsia", RankType.getForId(6000), cl, 10);
			//			printAllResults("homonyms test 2", results);

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
	public void testCommonNames(){
		//ANBG source
		String lsid = getCommonNameLSID("Red Kangaroo");
		String sciName = getCommonName("Red Kangaroo");
		System.out.println("Red Kangaroo LSID: " + lsid + ", Common Name: " + sciName);
                assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:31a9b8b8-4e8f-4343-a15f-2ed24e0bf1ae", lsid);
		//OLD LSID: assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:aa745ff0-c776-4d0e-851d-369ba0e6f537", lsid);
		//COL source
		lsid = getCommonNameLSID("Yellow-tailed Black-Cockatoo");
		sciName = getCommonName("Yellow-tailed Black-Cockatoo");
		System.out.println("Yellow-tailed Black-Cockatoo LSID: " + lsid + ", sciName: " + sciName);
		assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:ecdb82f9-d894-4d91-9533-3f2f010c9b14", lsid);
		//not found
		lsid = getCommonNameLSID("Scarlet Robin");
		sciName = getCommonName("Scarlet Robin");
		System.out.println("Scarlet Robin LSID: " + lsid + ", sciName: " + sciName);
		assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:0f5fa076-fe30-4598-b90c-31f12121a4fc", lsid);
		//CoL source that maps to a ANBG lsid
		lsid = getCommonNameLSID("Australian tuna");
		sciName = getCommonName("Australian tuna");
		System.out.println("Australian tuna LSID: " + lsid + ", sciName: " + sciName);
		//		assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:0f5fa076-fe30-4598-b90c-31f12121a4fc", lsid);
		//ANBG and CoL have slightly different scientific name
		lsid = getCommonNameLSID("Pacific Black Duck");
		sciName = getCommonName("Pacific Black Duck");
		System.out.println("Pacific Black Duck LSID: " + lsid + ", sciName: " + sciName);
                assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:ce7507c4-eafc-411b-8b12-84b9e425018b", lsid);
		//assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:d09b3807-f8d8-4cfb-a951-70e614e2d546", lsid);
		//Maps to many different species thus should return no LSID
		lsid = getCommonNameLSID("Carp");
		sciName = getCommonName("Carp");
		System.out.println("Carp LSID: " + lsid + ", sciName: " + sciName);
	}



	private String getCommonNameLSID(String name){
		return searcher.searchForLSIDCommonName(name);
	}
	private String getCommonName(String name){
		NameSearchResult sciName = searcher.searchForCommonName(name);

		return (sciName == null ? null : sciName.toString());
	}
	@Test
	public void testIRMNGHomonymReconcile(){
		try{
			LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Chordata", null, null, "Macropodidae", "Macropus", null);
			RankType rank = searcher.resolveIRMNGHomonym(cl, RankType.GENUS);
			System.out.println("IRMNG Homonym resolved at " + rank + " rank");

			assertEquals("FAMILY", rank.toString());
			//now cause a homonym exception by removing the family
			cl.setFamily(null);
			searcher.resolveIRMNGHomonym(cl, RankType.GENUS);
		}
		catch(HomonymException e){
			System.out.println("Expected HomonymException: " + e.getMessage());
			//			fail("testIRMNGHomonymReconcile failed");
		} catch (Exception e) {
			e.printStackTrace();
			fail("testIRMNGHomonymReconcile failed");
            }
    }
    @Test
	public void newHomonynmTest(){
        try{
			//Abelia grandiflora
		}
		catch(Exception e){

        }
	}
	@Test
	public void testCultivars(){
		try{
			//species level concept
			System.out.println("Hypoestes phyllostachya: " +searcher.searchForLSID("Hypoestes phyllostachya"));
			//cultivar level concept
			System.out.println("Hypoestes phyllostachya 'Splash': " + searcher.searchForRecord("Hypoestes phyllostachya 'Splash'", null));

		}
        catch(Exception e){
            e.printStackTrace();
			fail("testCultivars failed");
        }
    }
	@Test
	public void testMyrmecia(){
		try{
			LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia","Arthropoda", "Insecta", "Hymenoptera", "Formicidae", "Myrmecia",null);
			String output = null;
			NameSearchResult nsr = searcher.searchForRecord("Myrmecia", cl, RankType.GENUS);
			if (nsr != null) {
				output = nsr.toString();
			}
			System.out.println("testMyrmecia: " + output);
		}
		catch(Exception e){
			e.printStackTrace();
			fail("testMyrmecia failed");
		}
	}
	@Test
	public void testSearchForLSID(){

		try{
			LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Arthropoda", "Insecta", "Hymenoptera", "Formicidae","Anochetus", null);
			String output = searcher.searchForLSID("Anochetus");
			System.out.println("LSID for Anochetus: " + output);
			assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:c20caa14-2e67-4919-a9b2-627e55c47833", output);
			output = searcher.searchForLSID("Anochetus", true);
			System.out.println("LSID for Anochetus fuzzy: " + output);
			assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:c20caa14-2e67-4919-a9b2-627e55c47833", output);
			output = searcher.searchForLSID("Anochetus", false);
			System.out.println("LSID for Anochetus NOT fuzzy: " + output);
			assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:c20caa14-2e67-4919-a9b2-627e55c47833", output);
			output = searcher.searchForLSID("Anochetus", RankType.GENUS);
			System.out.println("LSID for Anochetus RankType Species: " + output);
			assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:c20caa14-2e67-4919-a9b2-627e55c47833", output);
			output = searcher.searchForLSID("Anochetus", cl, RankType.GENUS);
			System.out.println("LSID for Anochetus with cl and rank: " + output);
			assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:c20caa14-2e67-4919-a9b2-627e55c47833", output);
			output = searcher.searchForLSID(cl, true);
			System.out.println("LSID for cl and recursive matching: " + output);
			assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:c20caa14-2e67-4919-a9b2-627e55c47833", output);
			output = searcher.searchForLSID(cl, false);
			System.out.println("LSID for cl and NOT recursive matching: " + output);
			assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:c20caa14-2e67-4919-a9b2-627e55c47833", output);

}
		catch(Exception e){
			e.printStackTrace();
			fail("testSearchForLSID failed");
		}
	}
	@Test
	public void testFuzzyMatches(){
		try{
                    //Eolophus roseicapillus - non fuzzy match
                    assertEquals(searcher.searchForLSID("Eolophus roseicapillus"), "urn:lsid:biodiversity.org.au:afd.taxon:53f876f0-2c4d-40c8-ae6c-f478db8b07af");

                    //Eolophus roseicapilla - fuzzy match
                    assertEquals(searcher.searchForLSID("Eolophus roseicapilla", true),"urn:lsid:biodiversity.org.au:afd.taxon:53f876f0-2c4d-40c8-ae6c-f478db8b07af");


//                    //Bullia
//			String output = searcher.searchForRecord("Bullia", null) == null ? null : searcher.searchForRecord("Bullia", null).toString();
//			System.out.println("Bullia NOT fuzzy: " + output);
//			NameSearchResult nsr = searcher.searchForRecord("Bullia", null, true);
//			System.out.println("Bullia fuzzy: " + nsr);
//			NameSearchResult expectedResult = new NameSearchResult(String.valueOf(103077301), "urn:lsid:catalogueoflife.org:taxon:d8ccac42-29c1-102b-9a4a-00304854f820:ac2010", MatchType.SEARCHABLE);
//			assertTrue(nameSearchResultEqual(expectedResult, nsr));
//			//			assertEquals("Match: SEARCHABLE id: 103077301 lsid: urn:lsid:catalogueoflife.org:taxon:d8ccac42-29c1-102b-9a4a-00304854f820:ac2010 classification: au.org.ala.data.model.LinnaeanRankClassification@709446e4[kingdom=Animalia,phylum=Arthropoda,klass=Insecta,order=Lepidoptera,family=Noctuidae,genus=Bulia,species=<null>,specificEpithet=<null>,scientificName=Bulia] synonym: null", output);
//			//Anochetus
//			LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Arthropoda", "Insecta", "Hymenoptera", "Formicidae","Anochetus", null);
//			output = searcher.searchForLSID("Anochetus",cl, null);
//			System.out.println("Anochetus NOT fuzzy: " + output);
//			assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:413c014e-cc6c-4c44-b6e1-ad53008d1fd9", output);
//
//			output = searcher.searchForLSID("Anochetus", null, true);
//			System.out.println("Anochetus fuzzy: " + output);
//			assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:413c014e-cc6c-4c44-b6e1-ad53008d1fd9", output);
//
//			LinnaeanRankClassification classification = new LinnaeanRankClassification("Plantae", null, null, null, null, null, "Sauropus sp. A Kimberley Flora (T.E.H. Aplin et al. 929)");
//			output = searcher.searchForLSID("Sauropus sp. A Kimberley Flora (T.E.H. Aplin et al. 929)", classification, null);
//			System.out.println("Sauropus sp. A Kimberley Flora (T.E.H. Aplin et al. 929) : " + output);

		}
		catch(Exception e){
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
    public void testSpeciesHomonyms() {
        //Lobophora variegata is a species homonym in Animalia and Chromista
        try {
            searcher.searchForLSID("Agathis montana");
            fail("Species Homonym Test 1 (Agathis montana)");
        } catch (SearchResultException e) {
            System.out.println();
            assertEquals("Species Homonym Test 1 (Agathis montana) failed to throw correct exception", e.getClass(), HomonymException.class);
        }
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Aganthis");
            cl.setScientificName("Agathis montana");
            String lsid = searcher.searchForLSID(cl, false);
            assertEquals("urn:lsid:catalogueoflife.org:taxon:22dc2df8-60a7-102d-be47-00304854f810:col20110201", lsid);
        } catch (SearchResultException e) {
            e.printStackTrace();
            fail("Species Homonym Test 2 (Agathis montana)");
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
        try{
            LinnaeanRankClassification cl = new LinnaeanRankClassification(null, "Opuntia");
            cl.setScientificName("Opuntia spp");
            searcher.searchForLSID(cl, true);
            fail("SPP2 failed to throw a homonym exception");
        }
        catch(Exception e){
            assertEquals(e.getClass(), HomonymException.class);
        }
    }

    @Test
    public void testSinglePhraseName(){
        try{

            String name = "Astroloma sp. Cataby (EA Griffin 1022)";
            assertEquals(searcher.searchForLSID(name, null), "urn:lsid:biodiversity.org.au:apni.taxon:273906");
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    /**
     * Test that the an infraspecific rank searches for the specified rank and
     * RankType.INFRASPECIFICNAME
     */
    @Test
    public void testInfraSpecificRank(){
        try{
            String name="Acacia acanthoclada subsp. glaucescens";
           assertEquals(searcher.searchForLSID(name), "urn:lsid:biodiversity.org.au:apni.taxon:295870");
           assertEquals(searcher.searchForLSID("Macropus rufus", RankType.GENUS), null);

        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    @Test
    public void testRankMarker(){
        try{
            String lsid = searcher.searchForLSID("Macropus sp. rufus");
            System.out.println("SP.:"+lsid);
            lsid = searcher.searchForLSID("Macropus ssp. rufus");
            System.out.println("ssp: " +lsid);
        }
        catch(Exception e){
            fail("rank marker test failed");
        }
    }

}
