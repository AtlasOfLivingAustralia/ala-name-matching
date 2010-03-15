/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.checklist.lucene;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.ala.checklist.lucene.model.NameSearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.FSDirectory;
import org.gbif.ecat.model.ParsedName;
import org.gbif.ecat.parser.NameParser;
import org.gbif.portal.util.taxonomy.TaxonNameSoundEx;

/**
 *
 * The API used to perform a search on the Lucene Index.  It follows the following
 * algorithm when trying to find a match:
 *
 * 1. Search for a direct match for supplied name on the name field(with the optional rank provided).
 *
 * 2. Search for a match on the alternative name field (with optional rank)
 *
 * 3. Generate a searchable canonical name for the supplied name.  Search for a match on
 * the searchable canonical field using the generated name
 *
 * 4. Clean up the supplied name using the ECAT name parser. Repeat steps 1 to 3 on
 * the clean name until a match is found
 *
 * 5. No match is found
 *
 * When a match is found the existence of homonyms are checked.  When a
 * a homonym exists if the kingdom of the result does not match the supplied kingdom
 * a HomonymException is thrown.
 *
 * @author Natasha
 */
public class CBIndexSearch {
    protected Log log = LogFactory.getLog(CBIndexSearch.class);
    private IndexReader reader;
    private Searcher searcher;
    protected TaxonNameSoundEx tnse;
    private NameParser parser;
    protected Set<String> knownHomonyms;
    public CBIndexSearch(){

    }
    public void init(String indexDirectory) throws CorruptIndexException, IOException{
        reader = IndexReader.open(FSDirectory.open(new File(indexDirectory)), true);
        searcher = new IndexSearcher(reader);
        tnse = new TaxonNameSoundEx();
        parser = new NameParser();
        
    }
    /**
     * Searches the index for the supplied name.  Returns null when there is no result
     * or the LSID for the first result.
     * @param name
     * @return
     */
    public String searchForLSID(String name) throws SearchResultException{
        return searchForLSID(name, null);
    }
    /**
     * Searches the index for the supplied name of the specified rank.  Returns
     * null when there is no result or the LSID for the first result.
     * @param name
     * @param rank
     * @return
     */
    public String searchForLSID(String name, String rank)throws SearchResultException{
        return searchForLSID(name, null,  null, rank);

    }
    public String searchForLSID(String name, String kingdom,  String genus, String rank) throws SearchResultException{
        NameSearchResult result = searchForRecord(name, kingdom, genus, rank);
        if(result != null)
                return result.getLsid();
        return null;
    }
    /**
     * Searches the index for the supplied name of the specified rank.  Returns
     * null when there is no result or the result object for the first result.
     * @param name
     * @param rank
     * @return
     */
    public NameSearchResult searchForRecord(String name, String rank) throws SearchResultException{
        return searchForRecord(name, null,  null, rank);
    }
    public NameSearchResult searchForRecord(String name, String kingdom, String genus, String rank)throws SearchResultException{
        List<NameSearchResult> results = searchForRecords(name, rank, kingdom, genus, 1);
        if(results != null && results.size()>0)
            return results.get(0);
      
        return null;
    }
    /**
     * Returns the name that has the supplied checklist bank id
     * @param id
     * @return
     */
    public NameSearchResult searchForRecordByID(String id){
        try{
            List<NameSearchResult> results = performSearch(CBCreateLuceneIndex.IndexField.ID.toString(),id, null, null, null, null, 1, null, false);
            if(results.size()>0)
                return results.get(0);
        }
        catch(SearchResultException e){
            //this should not happen as we are  not checking for homonyms
            //homonyms should only be checked if a search is being performed by name
        }
        catch(IOException e){}
        return null;
    }
    /**
     *
     * @param name
     * @param rank
     * @return
     */
    public List<NameSearchResult> searchForRecords(String name, String rank) throws SearchResultException{
        return searchForRecords(name, rank, null, null, 10);
    }
    
    /**
     * Searches for the records that satisfy the given conditions using the algorithm
     * outlined in the class description.
     * @param name
     * @param rank
     * @param kingdom
     * @param genus
     * @param max
     * @return
     * @throws SearchResultException
     */
    public List<NameSearchResult> searchForRecords(String name, String rank, String kingdom, String genus, int max) throws SearchResultException{
        try{
            String phylum = null;
            //1. Direct Name hit
            List hits = performSearch(CBCreateLuceneIndex.IndexField.NAME.toString(), name, rank, kingdom, phylum, genus, max, NameSearchResult.MatchType.DIRECT, true);
            if(hits == null)//situation where searcher has not been initialised
                return null;
            if(hits.size()>0)
                return hits;
            //2. Hit on the alternative names
            hits = performSearch(CBCreateLuceneIndex.IndexField.NAMES.toString(), name, rank, kingdom, phylum, genus, max, NameSearchResult.MatchType.ALTERNATE, true);
            if(hits.size()>0)
                return hits;
            //3. searchable canonical name
            String searchable = tnse.soundEx(name);
            hits = performSearch(CBCreateLuceneIndex.IndexField.SEARCHABLE_NAME.toString(), searchable, rank, kingdom, phylum, genus, max, NameSearchResult.MatchType.SEARCHABLE, true);
            if(hits.size()>0)
                return hits;
            //4. clean the name and then search for the new version
            ParsedName cn = parser.parseIgnoreAuthors(name);
                    if (cn != null) {
                        String cleanName = cn.buildCanonicalName();
                        if (cleanName != null && !name.equals(cleanName)) {
                            List<NameSearchResult> results =searchForRecords(cleanName, rank);
                            if(results != null){
                                for(NameSearchResult result : results)
                                    result.setCleanName(cleanName);
                            }
                            return results;
                        }
                    }
        }
        catch(IOException e){
            log.warn(e.getMessage());
            return null;
        }
        return null;
    }




    /**
     *
     * Performs an index search based on the supplied field and name
     *
     * @param field
     * @param name
     * @param rank
     * @param kingdom
     * @param genus
     * @param max
     * @param type
     * @return
     * @throws IOException
     * @throws SearchResultException
     */
    private List<NameSearchResult> performSearch(String field, String value, String rank, String kingdom, String phylum, String genus, int max, NameSearchResult.MatchType type, boolean checkHomo)throws IOException, SearchResultException{
        if(searcher != null){
            Term term = new Term(field, value);
            Query query = new TermQuery(term);
            
            
                
            BooleanQuery boolQuery = new BooleanQuery();
            
            boolQuery.add(query, Occur.MUST);
            if(rank!=null){
                Query rankQuery =new TermQuery(new Term(CBCreateLuceneIndex.IndexField.RANK.toString(), rank));
                boolQuery.add(rankQuery, Occur.MUST);
            }
            if(kingdom != null){
                Query kingQuery = new TermQuery(new Term(CBCreateLuceneIndex.IndexField.KINGDOM.toString(), kingdom));
                boolQuery.add(kingQuery, Occur.SHOULD);
                
            }
//            if(phylum != null){
//                Query phylumQuery = new TermQuery(new Term(CBCreateLuceneIndex.IndexField.PHYLUM.toString(), phylum));
//                boolQuery.add(phylumQuery, Occur.SHOULD);
//
//            }
            if(genus!=null){
                Query genusQuery = new TermQuery(new Term(CBCreateLuceneIndex.IndexField.GENUS.toString(), genus));
                boolQuery.add(genusQuery, Occur.SHOULD);
                
            }
//            System.out.println(boolQuery);
            //limit the number of potential matches to max
            TopDocs hits = searcher.search(boolQuery, max);
            //now put the hits into the arrayof NameSearchResult
            List<NameSearchResult> results = new java.util.ArrayList<NameSearchResult>();

            for(ScoreDoc sdoc : hits.scoreDocs){
                results.add(new NameSearchResult(reader.document(sdoc.doc), type));
            }

            //check to see if the search criteria could represent an unresolved homonym

            if(checkHomo && results.size()>0&&results.get(0).isHomonym()){
                NameSearchResult result= validateHomonyms(results, kingdom);
                results.clear();
                results.add(result);
            }

            return results;
            
        }
        else
            log.error("The Index searcher has not been initialised. Try calling init().");
        return null;
    }
    /**
     * Uses the distance between 2 strings to determine whether or not the
     * 2 strings are a close match.
     *
     * @param s1
     * @param s2
     * @param maxLengthDif  The maximum differences in length that the 2 strings can  be
     * @param maxDist The maximum distance between the 2 strings
     * @return
     */
    private boolean isCloseMatch(String s1, String s2, int maxLengthDif, int maxDist){
        if (Math.abs(s1.length() - s2.length()) <= maxLengthDif) {
            //if the difference in the length of the 2 strings is at the most maxLengthDif characters compare the L distance
            //System.out.println("Difference ("+s1 + ", " + s2+") : " + StringUtils.getLevenshteinDistance(s1, s2));
            return StringUtils.getLevenshteinDistance(s1, s2)<=maxDist;

        }
        return false;
    }
    /**
     * Takes a result set that contains a homonym and then either throws a HomonymException
     * or returns the first result that matches the supplied taxa
     * @param results
     * @param k
     * @return
     * @throws HomonymException
     */
    private NameSearchResult validateHomonyms(List<NameSearchResult> results, String k) throws HomonymException{
        //WE are basing our unresolvable homonyms on having a known homonym that does not match at the kingdom level
        //The remaining levels are being ignored in this check
        //if a homonym exists but exact genus/species match exists and some of the higher classification match assume we have a match

        //the first result should be the one that most closely resembles the required classification
        
        if(k!= null){
            //check to see if the kingdom for the result matches, or has a close match
            for(NameSearchResult result : results){
                if(k.equals(result.getKingdom()) || isCloseMatch(k, result.getKingdom(), 3, 3))
                    return result;
            }
        }
        throw new HomonymException(results);
        
    }


}
