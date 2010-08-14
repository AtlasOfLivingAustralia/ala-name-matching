
package au.org.ala.checklist.lucene;


import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.FSDirectory;
import org.gbif.ecat.model.ParsedName;
import org.gbif.ecat.parser.NameParser;
import org.gbif.portal.util.taxonomy.TaxonNameSoundEx;

import au.org.ala.checklist.lucene.model.NameSearchResult;
import au.org.ala.data.util.RankType;
import au.org.ala.data.model.LinnaeanRankClassification;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 * The API used to perform a search on the CB Lucene Index.  It follows the following
 * algorithm when trying to find a match:
 *
 * 1. Search for a direct match for supplied name on the name field(with the optional rank provided).
 *
 * 2. Search for a match on the alternative name field (with optional rank)
 *
 *
 * 3. Generate a searchable canonical name for the supplied name.  Search for a match on
 * the searchable canonical field using the generated name
 *
 * 4. Clean up the supplied name using the ECAT name parser. Repeat steps 1 to 3 on
 * the clean name until a match is found
 *
 * 5. No match is found
 *
 * When a match is found the existence of homonyms are checked.  Where a homonym exists, 
 * if the kingdom of the result does not match the supplied kingdom a HomonymException is thrown.
 *
 
 * 
 * @author Natasha
 */
public class CBIndexSearch {
    protected Log log = LogFactory.getLog(CBIndexSearch.class);
    private IndexReader cbReader,irmngReader, vernReader;
    private Searcher cbSearcher, irmngSearcher, vernSearcher, idSearcher;
    protected TaxonNameSoundEx tnse;
    private NameParser parser;
    private static final String RANK_MARKER_ALL = "("+StringUtils.join(NameParser.ALL_RANKS.keySet(), "|")+")\\.";
    private static final Pattern RANK_MARKER = Pattern.compile(RANK_MARKER_ALL);
   
	public CBIndexSearch() {}

	/**
         * Creates a new name seacher. Using the indexDirectory 
         * as the source directory 
         * 
         * @param indexDirectory The directory that contains the CB and IRMNG index.
         * @throws CorruptIndexException
         * @throws IOException
         */
        public CBIndexSearch(String indexDirectory) throws CorruptIndexException, IOException {
                //Initialis CB index searching items
		cbReader = IndexReader.open(FSDirectory.open(createIfNotExist(indexDirectory+File.separator+"cb")), true);
		cbSearcher = new IndexSearcher(cbReader);
                //Initalise the IRMNG index searching items
                irmngReader = IndexReader.open(FSDirectory.open(createIfNotExist(indexDirectory+File.separator+"irmng")), true);
                irmngSearcher = new IndexSearcher(irmngReader);
                //initalise the Common name index searching items
                vernReader = IndexReader.open(FSDirectory.open(createIfNotExist(indexDirectory+File.separator+"vernacular")), true);
                vernSearcher = new IndexSearcher(vernReader);
                //initialise the identifier index
                idSearcher = new IndexSearcher(FSDirectory.open(createIfNotExist(indexDirectory + File.separator + "id")), true);
		tnse = new TaxonNameSoundEx();
		parser = new NameParser();
	}
        private File createIfNotExist(String indexDirectory) throws IOException{
           
            File idxFile = new File(indexDirectory);
		if(!idxFile.exists()){
			FileUtils.forceMkdir(idxFile);
			Analyzer analyzer = new StandardAnalyzer();
            IndexWriter iw = new IndexWriter(idxFile, analyzer, MaxFieldLength.UNLIMITED);
            iw.commit();
            iw.close();
		}
            return idxFile;
        }

    /**
     * Searches the index for the supplied name.  Returns null when there is no result
     * or the LSID for the first result. Where no LSID exist for the record the
     * CB ID is returned instead
     * @param name
     * @return
     */
    public String searchForLSID(String name) throws SearchResultException{
        return searchForLSID(name, null);
    }
    /**
     * Searches the index for the supplied name of the specified rank.  Returns
     * null when there is no result or the LSID for the first result.
     * Where no LSID exist for the record the
     * CB ID is returned instead
     *
     * When the result is a synonym the "accepted" taxons's LSID is returned.
     *
     * @param name
     * @param rank
     * @return
     */
    public String searchForLSID(String name, RankType rank)throws SearchResultException{
        return searchForLSID(name, null, rank);

    }
    /**
     * A wrapper method for the method below. Allows searching to occur without a classification.
     * @param name
     * @param kingdom
     * @param genus
     * @param rank
     * @return
     * @throws SearchResultException
     */
    @Deprecated
    public String searchForLSID(String name, String kingdom, String genus, RankType rank) throws SearchResultException{
        LinnaeanRankClassification cl = new LinnaeanRankClassification(kingdom, genus);
        return searchForLSID(name, cl, rank);
    }
    /**
     * Search for an LSID based on the supplied name.  When the kingdom and genus
     * are provided they are used to try and resolve homonyms. If they are not
     * provided and a homonym is detected in the result a HomonymException is thrown.
     * 
     * 
     * @param name
     * @param cl The high taxa that form the classification for the search item
     * @param rank
     * @return
     * @throws SearchResultException
     */
    public String searchForLSID(String name, LinnaeanRankClassification cl, RankType rank) throws SearchResultException{
		String lsid = null;
    	NameSearchResult result = searchForRecord(name, cl, rank);
		if (result != null) {
			if (result.getAcceptedLsid()==null && result.getLsid()==null) {
				log.warn("LSID missing for [name=" + name + ", id=" + result.getId() + "]");
			} else {
				lsid = result.getAcceptedLsid()!= null ? result.getAcceptedLsid() :result.getLsid();
			}
		}
		return lsid;
    }
    /**
     * Searches the index for the supplied name of the specified rank.  Returns
     * null when there is no result or the result object for the first result.
     *
     * @param name
     * @param rank
     * @return
     */
    public NameSearchResult searchForRecord(String name, RankType rank) throws SearchResultException{
        return searchForRecord(name, null,  rank);
    }
    /**
     * A wrapper method for the method below. Allows searching to occur without a classification.
     *
     * @param name
     * @param kingdom
     * @param genus
     * @param rank
     * @return
     * @throws SearchResultException
     */
    @Deprecated
    public NameSearchResult searchForRecord(String name, String kingdom, String genus, RankType rank)throws SearchResultException{
        LinnaeanRankClassification cl = new LinnaeanRankClassification(kingdom, genus);
        return searchForRecord(name,cl, rank);
    }

    public NameSearchResult searchForRecord(String name, LinnaeanRankClassification cl, RankType rank)throws SearchResultException{
        List<NameSearchResult> results = searchForRecords(name, rank, cl, 1);
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
            List<NameSearchResult> results = performSearch(CBCreateLuceneIndex.IndexField.ID.toString(),id, null,  null, 1, null, false);
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
     * Searches for a name of the specified rank.
     *
     * @param name
     * @param rank
     * @return
     */
    public List<NameSearchResult> searchForRecords(String name, RankType rank) throws SearchResultException{
        return searchForRecords(name, rank, null, 10);
    }
    
    /**
     * Searches for the records that satisfy the given conditions using the algorithm
     * outlined in the class description.
     *
     * @param name
     * @param rank
     * @param kingdom 
     * @param genus
     * @param max The maximum number of results to return
     * @return
     * @throws SearchResultException
     */
    public List<NameSearchResult> searchForRecords(String name, RankType rank, LinnaeanRankClassification cl, int max) throws SearchResultException{
        //The name is not allowed to be null
        if(name == null)
            throw new SearchResultException("Unable to perform search. Null value supplied for the name."); 
        try{
            
            //1. Direct Name hit
            List<NameSearchResult> hits = performSearch(CBCreateLuceneIndex.IndexField.NAME.toString(), name, rank, cl, max, NameSearchResult.MatchType.DIRECT, true);
			if (hits == null) // situation where searcher has not been initialised
				return null;
			if (hits.size() > 0)
				return hits;

			//2. Hit on the alternative names
            //check to see if the name needs a different rank associated with it
            rank = getUpdatedRank(name, rank);

            hits = performSearch(CBCreateLuceneIndex.IndexField.NAMES.toString(), name, rank, cl, max, NameSearchResult.MatchType.ALTERNATE, true);
            if(hits.size()>0)
                return hits;


            //3. searchable canonical name
            String searchable = tnse.soundEx(name);
            //searchable canonical should not check for homonyms due to the more erratic nature of the result
            hits = performSearch(CBCreateLuceneIndex.IndexField.SEARCHABLE_NAME.toString(), searchable, rank, cl, max, NameSearchResult.MatchType.SEARCHABLE, false);
            if(hits.size()>0)
                return hits;

            //4. clean the name and then search for the new version
            ParsedName<?> cn = parser.parseIgnoreAuthors(name);
			if (cn != null) {
				String cleanName = cn.buildCanonicalName();
				if (StringUtils.trimToNull(cleanName) != null && !name.equals(cleanName)) {
					List<NameSearchResult> results = searchForRecords(
							cleanName, rank, cl, max);
					if (results != null) {
						for (NameSearchResult result : results)
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
     * Update the rank for the name based on it containing rank strings.
     * Provides a bit of a sanity check on the name matching.  If we expect a
     * species we don't want to match on a genus
     * 
     * @param name
     * @param rank
     * @return
     */
    private RankType getUpdatedRank(String name, RankType rank){
        Matcher matcher = RANK_MARKER.matcher(name);
       
        if(matcher.find()){
            String value = name.substring(matcher.start(), matcher.end()) ;
            log.debug("Changing rank to : " + value);
            if(value.endsWith("."))
                rank = RankType.getForCBRank(NameParser.ALL_RANKS.get(value.substring(0, value.length()-1)));
            //System.out.println("Using the new rank " + rank);
        }
        return rank;
    }

    /**
     * Checks to see if the supplied name is a synonym. A synonym will not have
     * an associated kingdom and genus in the index.
     *
     *
     * @param name
     * @param rank
     * @param kingdom
     * @param genus
     * @param max
     * @throws SearchResultException
     */
    private void checkForSynonym(String name, RankType rank, String kingdom, String genus, int max) throws SearchResultException{
       //search on name field with name and empty kingdom and genus
       //search on the alternative names field with name and empty kingdom and genus
       //if we get a match that is a synonym verify match against IRMNG
    }

    private boolean doesSynonymMatch(String name, RankType rank, String kingdom, String genus){
        return false;
    }

    /**
     *
     * Performs an index search based on the supplied field and name
     *
     * @param field Index field on which to perform the search
     * @param value The value of which to search
     * @param rank Optional rank of the value
     * @param kingdom Optional kingdom for value
     * @param genus Optional genus for value
     * @param max The maximum number of results to return
     * @param type The type of search that is being performed
     * @param checkHomo Whether or not the result should check for homonyms
     * @return
     * @throws IOException
     * @throws SearchResultException
     */
    private List<NameSearchResult> performSearch(String field, String value, RankType rank, LinnaeanRankClassification cl, int max, NameSearchResult.MatchType type, boolean checkHomo)throws IOException, SearchResultException{
        if(cbSearcher != null){
            Term term = new Term(field, value);
            Query query = new TermQuery(term);
            
            
                
            BooleanQuery boolQuery = new BooleanQuery();
            
            boolQuery.add(query, Occur.MUST);
            if(rank!=null){
                Query rankQuery =new TermQuery(new Term(CBCreateLuceneIndex.IndexField.RANK.toString(), rank.getRank()));
                boolQuery.add(rankQuery, Occur.MUST);
            }
            if(cl != null &&cl.getKingdom() != null){
                Query kingQuery = new TermQuery(new Term(RankType.KINGDOM.getRank(), cl.getKingdom()));
                boolQuery.add(kingQuery, Occur.SHOULD);
                
            }
//            if(phylum != null){
//                Query phylumQuery = new TermQuery(new Term(CBCreateLuceneIndex.IndexField.PHYLUM.toString(), phylum));
//                boolQuery.add(phylumQuery, Occur.SHOULD);
//
//            }
            if(cl != null &&cl.getGenus()!=null){
                Query genusQuery = new TermQuery(new Term(RankType.GENUS.getRank(), cl.getGenus()));
                boolQuery.add(genusQuery, Occur.SHOULD);
                
            }
//            System.out.println(boolQuery);
            //limit the number of potential matches to max
            TopDocs hits = cbSearcher.search(boolQuery, max);
            //now put the hits into the arrayof NameSearchResult
            List<NameSearchResult> results = new java.util.ArrayList<NameSearchResult>();

            for(ScoreDoc sdoc : hits.scoreDocs){
                results.add(new NameSearchResult(cbReader.document(sdoc.doc), type));
            }

            //check to see if the search criteria could represent an unresolved homonym

            //if(checkHomo && results.size()>0&&results.get(0).isHomonym()){
            if(checkHomo && results.size() > 0 && results.get(0).getRank() == RankType.GENUS){
                NameSearchResult result= validateHomonyms(results,value, cl);
                results.clear();
                results.add(result);
            }

            return results;
            
        }
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
        if (s1 != null && s2!=null &&Math.abs(s1.length() - s2.length()) <= maxLengthDif) {
            //if the difference in the length of the 2 strings is at the most maxLengthDif characters compare the L distance
            //log.debug("Difference ("+s1 + ", " + s2+") : " + StringUtils.getLevenshteinDistance(s1, s2));
            return StringUtils.getLevenshteinDistance(s1, s2)<=maxDist;

        }
        return false;
    }
    /**
     * Takes a result set that contains a homonym and then either throws a HomonymException
     * or returns the first result that matches the supplied taxa.
     *
     * AS OF 22/07/2010:
     * Homonyms are ONLY being tested if the result was a genus. According to Tony it is
     * very rare for a species to be a homonym with another species that belongs to a homonym
     * of the same genus.  Eventually we should get a list of the known cases to
     * test against.  
     *
     * This should provide overall better name matcing.
     *
     * @param results
     * @param k
     * @return
     * @throws HomonymException
     */
    public NameSearchResult validateHomonyms(List<NameSearchResult> results, String name, LinnaeanRankClassification cl) throws HomonymException{
        //WE are basing our unresolvable homonyms on having a known homonym that does not match at the kingdom level
        //The remaining levels are being ignored in this check
        //if a homonym exists but exact genus/species match exists and some of the higher classification match assume we have a match

        //check to see if the homonym is resolvable given the details provide
        try{
            RankType resolveLevel = resolveIRMNGHomonym(cl);
            //result must match at the kingdom level and resolveLevel of the taxonomy (TODO)
            log.debug("resolve the homonym at " + resolveLevel +" rank");

            //the first result should be the one that most closely resembles the required classification
            String k = cl != null ?cl.getKingdom(): null;
            if(k!= null){
                //check to see if the kingdom for the result matches, or has a close match
                for(NameSearchResult result : results){
                    //check to see if the result is a synonym
                    if(result.isSynonym()){
                        //get the kingdom for the synonym using the IRMNG search
                        String kingdom = getValueForSynonym(name);
                        if(kingdom != null &&(kingdom.equals(k) || isCloseMatch(k, kingdom, 3, 3)))
                            return result;
                    }
                    else if(k.equals(result.getKingdom()) || isCloseMatch(k, result.getKingdom(), 3, 3))
                        return result;
                }
            }
            throw new HomonymException(results);
        }
        catch(HomonymException e){
            e.setResults(results);
            throw e;
        }
        
    }
    /**
     * Uses the IRMNG index to determine whether or not a homonym can be resolved
     * with the supplied details.
     * @return
     */
    private boolean isHomonymResolvable(LinnaeanRankClassification cl){
        TopDocs results = getIRMNGGenus(cl);
        if(results != null)
            return results.totalHits <=1;
        return false;
    }

    /**
     * Multiple genus indicate that an unresolved homonym exists for the supplied
     * search details.
     * @param k
     * @param p
     * @param c
     * @param o
     * @param f
     * @param g
     */
    private TopDocs getIRMNGGenus(LinnaeanRankClassification cl){
        if(cl != null && cl.getGenus() != null){
            Term term = new Term(RankType.GENUS.getRank(), cl.getGenus());
            Query query = new TermQuery(term);
            BooleanQuery boolQuery = new BooleanQuery();
            boolQuery.add(query, Occur.MUST);
            //optionally add the remaining ranks if they were supplied (performing the search at each level)
            //result will be returned if count =1
            if(cl.getKingdom() != null)
                boolQuery.add(new TermQuery(new Term(RankType.KINGDOM.getRank(), cl.getKingdom())), Occur.MUST);

            if(cl.getPhylum() != null)
                boolQuery.add(new TermQuery(new Term(RankType.PHYLUM.getRank(), cl.getPhylum())), Occur.MUST);
            if(cl.getKlass() != null)
                boolQuery.add(new TermQuery(new Term(RankType.CLASS.getRank(), cl.getKlass())), Occur.MUST);
            if(cl.getOrder() != null)
                boolQuery.add(new TermQuery(new Term(RankType.ORDER.getRank(), cl.getOrder())), Occur.MUST);
            if(cl.getFamily() != null)
                boolQuery.add(new TermQuery(new Term(RankType.FAMILY.getRank(), cl.getFamily())), Occur.MUST);
            //now perform the search
            try{
            return irmngSearcher.search(boolQuery, 10);

            }
            catch(IOException e){
                log.warn("Error searching IRMNG index." , e);
            }
        }
        return null;
    }

    /**
     * Attempt to resolve the homonym using the IRMNG index. 
     * 
     * The ability to resolve the homonym is dependent on the quality and quantity 
     * of the higher taxa provided in the search via cl. 
     * @param cl
     * @return
     * @throws HomonymException
     */
    public RankType resolveIRMNGHomonym(LinnaeanRankClassification cl) throws HomonymException{
        if(cl != null && cl.getGenus() != null){
            //create a local classification to work with we will only add a taxon when we are ready to try and resolve with it
            LinnaeanRankClassification newcl = new LinnaeanRankClassification(cl.getKingdom(), cl.getGenus());
            //Step 1 search for kingdom and genus
            TopDocs results = getIRMNGGenus(newcl);
            if(results.totalHits == 1)
                return RankType.KINGDOM;
            //Step 2 add the phylum
            if(cl.getPhylum() != null && results.totalHits>1){
                    newcl.setPhylum(cl.getPhylum());
                    results = getIRMNGGenus(newcl);
                    if(results.totalHits == 1)
                        return RankType.PHYLUM;
                    //This may not be a good idea 
                    else if(results.totalHits ==0)
                        newcl.setPhylum(null);//just in case the phylum was specified incorrectly
            }
            //Step 3 try the class
            if(cl.getKlass() != null){// && results.totalHits>1){
                newcl.setKlass(cl.getKlass());
                results = getIRMNGGenus(newcl);
                if(results.totalHits == 1)
                    return RankType.CLASS;

            }
            //step 4 try order
            if(cl.getOrder() != null && results.totalHits>1){
                newcl.setOrder(cl.getOrder());
                results = getIRMNGGenus(newcl);
                if(results.totalHits == 1)
                    return RankType.ORDER;
            }
            //step 5 try  the family
            if(cl.getFamily() != null && results.totalHits>1){
                newcl.setFamily(cl.getFamily());
                results = getIRMNGGenus(newcl);
                if(results.totalHits == 1)
                    return RankType.FAMILY;
            }
        }
        throw new HomonymException("Problem resolving the classification: "+cl.toString());
    }

    private String getValueForSynonym(String name){
        //get the genus for the name
        ParsedName<?> pn =  parser.parse(name);
        if(pn!= null){
            String genus = pn.getGenusOrAbove();
            LinnaeanRankClassification cl = new LinnaeanRankClassification(null, genus);
            TopDocs docs = getIRMNGGenus(cl);
            try{
            if(docs.totalHits>0)
                return irmngSearcher.doc(docs.scoreDocs[0].doc).get(RankType.KINGDOM.getRank());
            }
            catch(IOException e){
                log.warn("Unable to get value for synonym. " ,e);
            }
            //seach for the genus in irmng
            //return simpleIndexLookup(irmngSearcher, RankType.GENUS.getRank(), genus, RankType.KINGDOM.getRank());
        }
        return null;
    }
    
    /**
     * Performs a search on the common name index for the supplied name.
     * @param commonName
     * @return
     */
    public String searchForLSIDCommonName(String commonName){

        return getLSIDForUniqueCommonName(commonName);
    }
    /**
     * Returns the LSID for the CB name usage for the supplied common name.
     *
     * When the common name returns more than 1 hit a result is only returned if all the scientific names match
     * @param name
     * @return
     */
    private String getLSIDForUniqueCommonName(String name){
        if(name != null){
            TermQuery query = new TermQuery(new Term(CBCreateLuceneIndex.IndexField.COMMON_NAME.toString(), name.toUpperCase().replaceAll("[^A-Z0-9ÏËÖÜÄÉÈČÁÀÆŒ]", "")));
            try{
                TopDocs results = vernSearcher.search(query, 10);
                //if all the results have the same scientific name result the LSID for the first
                String firstLsid = null;
                String firstName = null;
                log.debug("Number of matches for " + name + " " + results.totalHits);
                for(ScoreDoc sdoc: results.scoreDocs){
                    org.apache.lucene.document.Document doc =vernSearcher.doc(sdoc.doc);
                    if(firstLsid == null){
                        firstLsid = doc.get(CBCreateLuceneIndex.IndexField.LSID.toString());
                        firstName = doc.get(CBCreateLuceneIndex.IndexField.NAME.toString());
                    }
                    else{
                        if(!doSciNamesMatch(firstName, doc.get(CBCreateLuceneIndex.IndexField.NAME.toString())))
                            return null;
                    }
                }
                return firstLsid;
            }
            catch(IOException e){
                //
                log.debug("Unable to access document for common name." , e);
            }
        }
        return null;
    }
    /**
     * Returns true when the parsed names match.
     * @param n1
     * @param n2
     * @return
     */
    private boolean doSciNamesMatch(String n1, String n2){
        ParsedName<?> pn1 =  parser.parse(n1);
        ParsedName<?> pn2 =  parser.parse(n2);
        if(pn1 != null && pn2!= null)
            return pn1.buildCanonicalName().equals(pn2.buildCanonicalName());
        return false;
    }
    /**
     * Performs a search on the supplied common name returning a NameSearchResult.
     * Useful if you required CB ID's etc.
     * @param name
     * @return
     */
    public NameSearchResult searchForCommonName(String name){
        NameSearchResult result = null;
        String lsid = getLSIDForUniqueCommonName(name);
        if(lsid != null){
            //we need to get the CB ID for the supplied LSID
            result =searchForRecordByLsid(lsid);
        }
        return result;
    }
    /**
     * Returns the primary LSID for the supplied lsid.
     * 
     * This is useful in the situation where multiple LSIDs are associated with
     * a scientific name and there is a reference to the non-primary LSID.
     * 
     * @param lsid
     * @return
     */
    public String getPrimaryLsid(String lsid){

        TermQuery tq = new TermQuery(new Term("lsid", lsid));
        try{
        org.apache.lucene.search.TopDocs results = idSearcher.search(tq, 1);
        if(results.totalHits>0)
            return idSearcher.doc(results.scoreDocs[0].doc).get("reallsid");
        }catch(IOException e){}

        return lsid;
    }
    public NameSearchResult searchForRecordByLsid(String lsid){
        NameSearchResult result = null;
        try{
            List<NameSearchResult> results= performSearch(CBCreateLuceneIndex.IndexField.LSID.toString(), lsid, null, null, 1, NameSearchResult.MatchType.DIRECT, false);
                if(results.size()>0)
                    result = results.get(0);
        }
        catch(Exception e){
            //we are not checking for homonyms so this should never happen
            log.error("Unable to search for record by LSID");
        }
        return result;
    }
}
