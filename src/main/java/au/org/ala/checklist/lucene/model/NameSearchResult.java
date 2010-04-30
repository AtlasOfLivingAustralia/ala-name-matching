
package au.org.ala.checklist.lucene.model;

import org.apache.lucene.document.Document;

import au.org.ala.checklist.lucene.CBCreateLuceneIndex.IndexField;
import org.apache.commons.lang.StringUtils;

/**
 * A model to store the required information in a search result
 *
 * This includes the type of match that was used to get the result
 * 
 * @author Natasha
 */
public class NameSearchResult {
    private long id;
    private String lsid;
    private String classification;
    private String cleanName;
    private boolean isHomonym;
    private String synonymLsid;
    private String kingdom;
    public enum MatchType{
        DIRECT,
        ALTERNATE,
        SEARCHABLE
    }
    private MatchType matchType;
    public NameSearchResult(String id, String lsid, String classification, MatchType type){
        this.id = Long.parseLong(id);
        this.lsid = StringUtils.trimToNull(lsid)==null ? id : StringUtils.trimToNull(lsid);
        this.classification = classification;
        matchType = type;
        isHomonym = false;
    }
    public NameSearchResult(Document doc, MatchType type){
        this(doc.get(IndexField.ID.toString()), doc.get(IndexField.LSID.toString()),doc.get(IndexField.CLASS.toString()), type);
        isHomonym = doc.get(IndexField.HOMONYM.toString())!=null;
        kingdom = doc.get(IndexField.KINGDOM.toString());
        String syn = doc.get(IndexField.SYNONYM.toString());
        if(syn != null){
            String[] synDetails = syn.split("\t",2);
            synonymLsid = StringUtils.trimToNull(synDetails[1]) == null?synDetails[0]:synDetails[1];

        }
    }
    public String getKingdom(){
        return kingdom;
    }
    public String getLsid(){

        return lsid;
    }
    public long getId(){
        return id;
    }
    public String getClassification(){
        return classification;
    }
    public MatchType getMatchType(){
        return matchType;
    }
    public void setMatchType(MatchType type){
        matchType = type;
    }
    public String getCleanName(){
        return cleanName;
    }
    public void setCleanName(String name){
        cleanName = name;
    }
    public boolean hasBeenCleaned(){
        return cleanName != null;
    }
    public boolean isHomonym(){
        return isHomonym;
    }
    public boolean isSynonym(){
        return synonymLsid != null;
    }
    public String getSynonymLsid(){
        return synonymLsid;
    }
    @Override
    public String toString(){
        return "Match: " + matchType + " id: " + id+ " lsid: "+ lsid+ " classification: " +classification +" synonym: "+ synonymLsid;
    }
}
