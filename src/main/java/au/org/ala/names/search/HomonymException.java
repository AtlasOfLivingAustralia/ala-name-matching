

package au.org.ala.names.search;

import java.util.List;

import au.org.ala.names.model.NameSearchResult;

/**
 *  Exception that is thrown when the result is an unresolved
 * homonym
 * @author Natasha
 */
public class HomonymException extends SearchResultException {
    public HomonymException(String msg, List<NameSearchResult> results){
        this(msg);
        this.results = results;
    }
    public HomonymException(String message){
        super("Warning an unresolved homonym has been detected. "+message);
        errorType = au.org.ala.names.model.ErrorType.HOMONYM;
    }
    public HomonymException(){
        this("Warning an unresolved homonym has been detected. ");
    }
    public HomonymException(List<NameSearchResult> results){
        this();
        this.results = results;
    }
    public void setResults(List<NameSearchResult> results){
        this.results = results;
    }
}
