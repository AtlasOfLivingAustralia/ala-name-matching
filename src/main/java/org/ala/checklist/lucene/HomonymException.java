

package org.ala.checklist.lucene;

import java.util.List;
import org.ala.checklist.lucene.model.NameSearchResult;

/**
 *  Exception that is thrown when the result is an unresolved
 * homonym
 * @author Natasha
 */
public class HomonymException extends SearchResultException {
    public HomonymException(List<NameSearchResult> results){
        super("Warning an unresolved homonym has been detected.");
        this.results = results;
    }
    public void setResults(List<NameSearchResult> results){
        this.results = results;
    }
}
