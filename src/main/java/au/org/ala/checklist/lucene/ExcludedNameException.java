

package au.org.ala.checklist.lucene;

import au.org.ala.checklist.lucene.model.NameSearchResult;

/**
 *  An exception that is thrown when the match is to a name that has been excluded.
 *
 * In the BIE we would want to use the match anyway because all excluded names will have
 * a species page.
 *
 * In the biocache we would want to flag the record and match to the associated record?? OR maybe
 *
 * @author Natasha Carter
 */
public class ExcludedNameException extends SearchResultException{
    private NameSearchResult excludedName;    
    private NameSearchResult nonExcludedName;
    public ExcludedNameException(String message, NameSearchResult excludedName){
        super(message);
        this.excludedName= excludedName;
        errorType =au.org.ala.checklist.lucene.model.ErrorType.EXCLUDED;
        
    }

    public ExcludedNameException(String message, NameSearchResult nonExcludedName, NameSearchResult excludedName){
        this(message, excludedName);
        this.nonExcludedName = nonExcludedName;
        errorType=au.org.ala.checklist.lucene.model.ErrorType.ASSOCIATED_EXCLUDED;
    }

    public NameSearchResult getExcludedName() {
        return excludedName;
    }

    public void setExcludedName(NameSearchResult excludedName) {
        this.excludedName = excludedName;
    }

    public NameSearchResult getNonExcludedName() {
        return nonExcludedName;
    }

    public void setNonExcludedName(NameSearchResult nonExcludedName) {
        this.nonExcludedName = nonExcludedName;
    }
    
}
