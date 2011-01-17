

package au.org.ala.checklist.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;


import au.org.ala.checklist.lucene.model.NameSearchResult;
import au.org.ala.data.util.RankType;
import au.org.ala.data.model.LinnaeanRankClassification;

/**
 *
 * @author Natasha, Tommy
 */
public class CBIndexSearchTest {
	private static CBIndexSearch searcher;

	@org.junit.BeforeClass
	public static void init() {
		try {
			searcher = new CBIndexSearch("/data/lucene/namematching");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@org.junit.Test
	public void testNoRank() {
		try {
			String lsid = searcher.searchForLSID("Animalia");
			System.out.println("testNoRank: " + lsid);
			
			assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:4647863b-760d-4b59-aaa1-502c8cdf8d3c", lsid);
			lsid = searcher.searchForLSID("Bacteria");
			System.out.println("testNoRank: " + lsid);
			assertEquals("3", lsid);
		} catch (SearchResultException e) {
			e.printStackTrace();
			fail("testNoRank failed");
		}
	}
	
	@org.junit.Test
	public void testGetPrimaryLsid() {
		try {
			String primaryLsid = searcher.getPrimaryLsid("urn:lsid:biodiversity.org.au:afd.taxon:00d9e076-b619-4a65-bd9e-8538d958817a");
			System.out.println("testGetPrimaryLsid: " + primaryLsid);
		} catch (Exception e) {
			e.printStackTrace();
			fail("testGetPrimaryLsid failed");
		}
	}
	
	@org.junit.Test
	public void testSearchForRecordByLsid() {
		try {
			NameSearchResult nsr = searcher.searchForRecordByLsid("urn:lsid:biodiversity.org.au:afd.taxon:00d9e076-b619-4a65-bd9e-8538d958817a");
			System.out.println("testSearchForRecordByLsid: " + nsr == null ? null : nsr.toString());
		} catch (Exception e) {
			e.printStackTrace();
			fail("testSearchForRecordByLsid failed");
		}
	}

	@org.junit.Test
	public void testSpecies() {
		try {
			NameSearchResult nsr = searcher.searchForRecord(
					"Holconia nigrigularis", RankType.getForId(7000));
			System.out.println("testSpecies: " + nsr.toString() + "!!");
			assertEquals("Match: DIRECT id: 101300 lsid: urn:lsid:biodiversity.org.au:afd.taxon:00d9e076-b619-4a65-bd9e-8538d958817a classification: au.org.ala.data.model.LinnaeanRankClassification@356f144c[kingdom=Animalia,phylum=Arthropoda,klass=Arachnida,order=Araneae,family=Sparassidae,genus=Holconia,species=Holconia nigrigularis,specificEpithet=<null>,scientificName=Holconia nigrigularis] synonym: null", nsr.toString());
		} catch (SearchResultException e) {
			e.printStackTrace();
			fail("testSpecies failed");
		}
	}

	private void printAllResults(String prefix, List<NameSearchResult> results) {
		System.out.println("## " + prefix + " ##");
		for (NameSearchResult result : results)
			System.out.println(result);
		System.out.println("###################################");
	}
	@org.junit.Test
	public void testSynonymWithHomonym(){
		try{
			//               NameSearchResult result = searcher.searchForRecord("Macropus rufus", RankType.SPECIES);
			//               System.out.println("Macropus rufus: " + result);
			//               System.out.println("LSID: " + searcher.searchForLSID("Macropus rufus"));
			LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Atylus");
			NameSearchResult result = searcher.searchForRecord("Atylus monoculoides", cl, RankType.SPECIES);
			System.out.println("testSynonymWithHomonym Synonym: " + result);
			assertEquals("Match: DIRECT id: 223782 lsid: urn:lsid:biodiversity.org.au:afd.taxon:5005b407-1e87-4aa3-a2ff-88b89f0a2dc4 classification: au.org.ala.data.model.LinnaeanRankClassification@7aad7aad[kingdom=<null>,phylum=<null>,klass=<null>,order=<null>,family=<null>,genus=<null>,species=<null>,specificEpithet=<null>,scientificName=<null>] synonym: urn:lsid:biodiversity.org.au:afd.taxon:dcd396c3-afd4-498f-ab83-2605926f64f8", result.toString());
			String lsid = searcher.searchForLSID("Atylus monoculoides");			
			System.out.println("testSynonymWithHomonym LSID: " + lsid);
			assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:dcd396c3-afd4-498f-ab83-2605926f64f8", lsid);
			//System.out.println("LSID: " +searcher.searchForLSID("Sira tricincta"));
		}
		catch(Exception e){
			//e.printStackTrace();//
			if(e instanceof HomonymException){
				printAllResults("SYNONYM/HOMONYM: ", ((HomonymException)e).getResults());
			}
			fail("testSynonymWithHomonym failed");
		}
	}

	@org.junit.Test
	public void testHomonym() {
		try {

			LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Simsia");
			List<NameSearchResult> results = searcher.searchForRecords(
					"Simsia", RankType.getForId(6000), cl, 10);
			printAllResults("hymonyms test 1", results);
			//test to ensure that kingdoms that almost match are being will not report homonym exceptions
			cl.setKingdom("Anmalia");
			results = searcher.searchForRecords("Simsia", RankType.getForId(6000), cl, 10);
			printAllResults("hymonyms test (Anmalia)", results);
			cl.setKingdom(null);
			results = searcher.searchForRecords("Simsia", RankType.getForId(6000), cl, 10);
			printAllResults("homonyms test 2", results);

		} catch (SearchResultException e) {
			System.err.println(e.getMessage());
			printAllResults("HOMONYM EXCEPTION", e.getResults());
			fail("testHomonym failed");
		}
	}

	@org.junit.Test
	public void testIDLookup() {
		NameSearchResult result = searcher.searchForRecordByID("216346");
		System.out.println("testIDLookup: " + result);
	}

	@org.junit.Test
	public void testSearchForRecord() {
		NameSearchResult result = null;
		try {
			LinnaeanRankClassification cl = new LinnaeanRankClassification(null, "Rhinotia");
			result = searcher.searchForRecord(null, cl, null);
		} catch (SearchResultException e) {
			e.printStackTrace();
			fail("testSearchForRecord failed");
		}
		System.out.println("testSearchForRecord: " + result);
	}
	@org.junit.Test
	public void testCommonNames(){
		//ANBG source
		String lsid = getCommonNameLSID("Red Kangaroo");
		String sciName = getCommonName("Red Kangaroo");
		System.out.println("Red Kangaroo LSID: " + lsid + ", sciName: " + sciName);
		//COL source
		lsid = getCommonNameLSID("Yellow-tailed Black-Cockatoo");
		sciName = getCommonName("Yellow-tailed Black-Cockatoo");
		System.out.println("Yellow-tailed Black-Cockatoo LSID: " + lsid + ", sciName: " + sciName);
		//not found
		lsid = getCommonNameLSID("Scarlet Robin");
		sciName = getCommonName("Scarlet Robin");
		System.out.println("Scarlet Robin LSID: " + lsid + ", sciName: " + sciName);
		//CoL source that maps to a ANBG lsid
		lsid = getCommonNameLSID("Australian tuna");
		sciName = getCommonName("Australian tuna");
		System.out.println("Australian tuna LSID: " + lsid + ", sciName: " + sciName);
		//ANBG and CoL have slightly different scientific name
		lsid = getCommonNameLSID("Pacific Black Duck");
		sciName = getCommonName("Pacific Black Duck");
		System.out.println("Pacific Black Duck LSID: " + lsid + ", sciName: " + sciName);
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
	@org.junit.Test
	public void testIRMNGHomonymReconcile(){
		try{
			LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Chordata", null, null, "Macropodidae", "Macropus", null);
			RankType rank = searcher.resolveIRMNGHomonym(cl);
			System.out.println("IRMNG Homonym resolved at " + rank + " rank");
			
			assertEquals("FAMILY", rank.toString());
			//now cause a homonym exception by removing the family
			cl.setFamily(null);
			searcher.resolveIRMNGHomonym(cl);
		}
		catch(HomonymException e){
			System.out.println(e.getMessage());
			fail("testIRMNGHomonymReconcile failed");
		}
	}
	@org.junit.Test
	public void newHomonynmTest(){
		try{
			//Abelia grandiflora
		}
		catch(Exception e){

		}
	}
	@org.junit.Test
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
	@org.junit.Test
	public void testMyrmecia(){
		try{
			LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia","Arthropoda", "Insecta", "Hymenoptera", "Formicidae", "Myrmecia",null);
			String output = searcher.searchForRecord("Myrmecia", cl, null).toString();
			System.out.println(output);
		}
		catch(Exception e){
			e.printStackTrace();
			fail("testMyrmecia failed");
		}
	}
	@org.junit.Test
	public void testSearchForLSID(){
		
		try{
			LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia","Arthropoda", "Insecta", "Hymenoptera", "Formicidae", "Myrmecia",null);
			String output = searcher.searchForLSID("Myrmecia");
			System.out.println("LSID for Myrmecia: " + output);
			output = searcher.searchForLSID("Myrmecia", true);
			System.out.println("LSID for Myrmecia fuzzy: " + output);
			output = searcher.searchForLSID("Myrmecia", false);
			System.out.println("LSID for Myrmecia NOT fuzzy: " + output);
			output = searcher.searchForLSID("Myrmecia", RankType.GENUS);
			System.out.println("LSID for Myrmecia RankType Species: " + output);
			output = searcher.searchForLSID("Myrmecia", cl, RankType.GENUS);
			System.out.println("LSID for Myrmecia with cl and rank: " + output);
			output = searcher.searchForLSID(cl, true);
			System.out.println("LSID for cl and recursive matching: " + output);
			output = searcher.searchForLSID(cl, false);
			System.out.println("LSID for cl and NOT recursive matching: " + output);
			
		}
		catch(Exception e){
			e.printStackTrace();
			fail("testSearchForLSID failed");
		}
	}
	@org.junit.Test
	public void testFuzzyMatches(){
		try{
			//Bullia
			String output = searcher.searchForRecord("Bullia", null).toString();			
			System.out.println("Bullia NOT fuzzy: " + output);
			output = searcher.searchForRecord("Bullia", null, true).toString();
			System.out.println("Bullia fuzzy: " + output);
			assertEquals("Match: SEARCHABLE id: 103077301 lsid: urn:lsid:catalogueoflife.org:taxon:d8ccac42-29c1-102b-9a4a-00304854f820:ac2010 classification: au.org.ala.data.model.LinnaeanRankClassification@709446e4[kingdom=Animalia,phylum=Arthropoda,klass=Insecta,order=Lepidoptera,family=Noctuidae,genus=Bulia,species=<null>,specificEpithet=<null>,scientificName=Bulia] synonym: null", output);
			//Anochetus
			LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Arthropoda", "Insecta", "Hymenoptera", "Formicidae","Anochetus", null);
			output = searcher.searchForLSID("Anochetus",cl, null);
			System.out.println("Anochetus NOT fuzzy: " + output);
			assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:413c014e-cc6c-4c44-b6e1-ad53008d1fd9", output);
			
			output = searcher.searchForLSID("Anochetus", null, true);
			System.out.println("Anochetus fuzzy: " + output);
			assertEquals("urn:lsid:biodiversity.org.au:afd.taxon:413c014e-cc6c-4c44-b6e1-ad53008d1fd9", output);
			
			LinnaeanRankClassification classification = new LinnaeanRankClassification("Plantae", null, null, null, null, null, "Sauropus sp. A Kimberley Flora (T.E.H. Aplin et al. 929)");
			output = searcher.searchForLSID("Sauropus sp. A Kimberley Flora (T.E.H. Aplin et al. 929)", classification, null);
			System.out.println("Sauropus sp. A Kimberley Flora (T.E.H. Aplin et al. 929) : " + output);

		}
		catch(Exception e){
			e.printStackTrace();
			fail("testFuzzyMatches failed");
		}
	}
	
	public void testWaiIssues(){
		//            System.out.println("#### TEST 1 ####"); synonym
		//            testName("Cyrtanthus elatus");
		//            testName("Cyrtanthus purpureus");
		//            System.out.println("#### TEST 2 ####"); synonym
		//            testName("Lycoris africana");
		//            testName("Lycoris aurea");
		//            System.out.println("#### TEST 3 ####");
		//            testName("Monstera deliciosa");
		//            testName("Monstera deliciosa 'Albo-Variegata'");

	}
	public void testName(String name){
		try{
			String output = searcher.searchForRecord(name, null).toString();
			System.out.println(name+": " + output);

		}
		catch(Exception e){
			e.printStackTrace();
			fail("testWaiIssues failed");
		}
	}
}
