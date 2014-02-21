
package au.org.ala.names.search;

import au.org.ala.names.model.ErrorType;
import java.util.List;

import au.org.ala.names.model.NameSearchResult;

/**
 *  @see HomonymException
 * @author Natasha
 */
public class SearchResultException extends Exception {
    protected List<NameSearchResult> results;
    protected ErrorType errorType;
    public SearchResultException(String msg){
        super(msg);
        errorType = ErrorType.GENERIC;
    }
    public SearchResultException(String msg,List<NameSearchResult> results){
        this(msg);
        this.results = results;
    }
    public List<NameSearchResult> getResults(){
        return results;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

}
