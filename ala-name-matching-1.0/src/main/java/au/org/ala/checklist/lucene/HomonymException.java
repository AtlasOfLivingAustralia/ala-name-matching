

package au.org.ala.checklist.lucene;

import java.util.List;

import au.org.ala.checklist.lucene.model.NameSearchResult;

/**
 *  Exception that is thrown when the result is an unresolved
 * homonym
 * @author Natasha
 */
public class HomonymException extends SearchResultException {
    public HomonymException(String msg, List<NameSearchResult> results){
        super(msg);
        this.results = results;
    }
    public HomonymException(String message){
        super("Warning an unresolved homonym has been detected. "+message);
    }
    public HomonymException(){
        super("Warning an unresolved homonym has been detected. ");
    }
    public HomonymException(List<NameSearchResult> results){
        this();
        this.results = results;
    }
    public void setResults(List<NameSearchResult> results){
        this.results = results;
    }
}
