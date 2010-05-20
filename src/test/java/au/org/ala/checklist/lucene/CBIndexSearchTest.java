

package au.org.ala.checklist.lucene;

import java.util.List;


import au.org.ala.checklist.lucene.model.NameSearchResult;
import au.org.ala.data.util.RankType;
import org.gbif.portal.model.LinnaeanRankClassification;

/**
 *
 * @author Natasha
 */
public class CBIndexSearchTest {
    CBIndexSearch searcher;

	@org.junit.Before
	public void init() {
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
			System.out.println(lsid);
			lsid = searcher.searchForLSID("Bacteria");
			System.out.println(lsid);
		} catch (SearchResultException e) {
			e.printStackTrace();
		}
	}

	@org.junit.Test
	public void testSpecies() {
		try {
			NameSearchResult nsr = searcher.searchForRecord(
					"Holconia nigrigularis", RankType.getForId(7000));
			System.out.println(nsr);
		} catch (SearchResultException e) {
			e.printStackTrace();
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
                System.out.println("synonym: " + result);
                System.out.println("LSID: " + searcher.searchForLSID("Atylus monoculoides"));
                //System.out.println("LSID: " +searcher.searchForLSID("Sira tricincta"));
            }
            catch(Exception e){
                //e.printStackTrace();//
                if(e instanceof HomonymException){
                    printAllResults("SYNONYM/HOMONYM: ", ((HomonymException)e).getResults());
                }
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
		}
	}

	@org.junit.Test
	public void testIDLookup() {
		NameSearchResult result = searcher.searchForRecordByID("216346");
		System.out.println(result);
	}
	
	@org.junit.Test
	public void testSearchForRecord() {
		NameSearchResult result = null;
		try {
                        LinnaeanRankClassification cl = new LinnaeanRankClassification(null, "Rhinotia");
			result = searcher.searchForRecord(null, cl, null);
		} catch (SearchResultException e) {
			e.printStackTrace();
		}
		System.out.println(result);
	}
        @org.junit.Test
        public void testCommonNames(){
            //ANBG source
            printCommonName("Red Kangaroo");
            //COL source
            printCommonName("Yellow-tailed Black-Cockatoo");
            //not found
            printCommonName("Scarlet Robin");
            //CoL source that maps to a ANBG lsid
            printCommonName("Australian tuna");
            //ANBG and CoL have slightly different scientific name
            printCommonName("Pacific Black Duck");
            //Maps to many different species thus should return no LSID
            printCommonName("Carp");
        }
        private void printCommonName(String name){
            System.out.println(name + " " + searcher.searchForLSIDCommonName(name));
        }
        @org.junit.Test
        public void testIRMNGHomonymReconcile(){
            try{
                LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Chordata", null, null, "Macropodidae", "Macropus", null);
                RankType rank = searcher.resolveIRMNGHomonym(cl);
                System.out.println("IRMNG Homonym resolved at " + rank + " rank");
                //now cause a homonym exception by removing the family
                cl.setFamily(null);
                searcher.resolveIRMNGHomonym(cl);
            }
            catch(HomonymException e){
                System.out.println(e.getMessage());
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
}
