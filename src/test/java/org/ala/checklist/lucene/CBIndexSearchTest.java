

package org.ala.checklist.lucene;

import java.util.List;
import org.ala.checklist.lucene.model.NameSearchResult;

/**
 *
 * @author Natasha
 */
public class CBIndexSearchTest {
    CBIndexSearch searcher;

     @org.junit.Before
    public void init() {
        try {
            searcher = new CBIndexSearch();
            searcher.init("/data/lucene/cb/classification");
        }
        catch(Exception e){

        }

    }
     @org.junit.Test
     public void testNoRank(){
         try{
         String lsid = searcher.searchForLSID("Animalia");
         System.out.println(lsid);
         lsid = searcher.searchForLSID("Bacteria");
         System.out.println(lsid);
         }
         catch(SearchResultException e){}
     }
     @org.junit.Test
     public void testSpecies(){
         try{
         NameSearchResult nsr = searcher.searchForRecord("Holconia nigrigularis", "7000");
         System.out.println(nsr);
         }
         catch(SearchResultException e){}
     }
     private void printAllResults(String prefix, List<NameSearchResult> results){
         System.out.println("## "+prefix+" ##");
         for(NameSearchResult result : results)
             System.out.println(result);
         System.out.println("###################################");
     }
     @org.junit.Test
     public void testHomonym(){
         try{
             
             List<NameSearchResult> results =searcher.searchForRecords("Simsia", "6000", "Animalia",  "Simsia", 10);
             printAllResults("hymonyms test 1", results);

             results =searcher.searchForRecords("Simsia", "6000", "Anmalia",  "Simsia", 10);
             printAllResults("hymonyms test (Anmalia)", results);
             
             results = searcher.searchForRecords("Simsia", "6000", null, "Simsia", 10);
             printAllResults("homonyms test 2", results);
             
         }
         catch(SearchResultException e){
             System.err.println(e.getMessage());
             printAllResults("HOMONYM EXCEPTION", e.getResults());
             
         }
     }
     @org.junit.Test
     public void testIDLookup(){
         NameSearchResult result = searcher.searchForRecordByID("216346");
         System.out.println(result);
     }
}
