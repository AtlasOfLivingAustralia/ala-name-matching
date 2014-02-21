
package au.org.ala.names.model;

import au.org.ala.names.search.SearchResultException;
import java.util.Set;
import org.gbif.ecat.voc.NameType;

/**
 * Stores the data the data that makes up a name match includes metrics such as name types and errors
 *
 * Allows a metrics to be returned even when a match is not found.
 *
 * @author Natasha Carter
 */
public class MetricsResultDTO {
    private NameSearchResult result;
    private NameType nameType;
    private Set<ErrorType> errors;
    private SearchResultException lastException;

    public Set<ErrorType> getErrors() {
        return errors;
    }

    public void setErrors(Set<ErrorType> errors) {
        this.errors = errors;
    }

    public NameType getNameType() {
        return nameType;
    }

    public void setNameType(NameType nameType) {
        this.nameType = nameType;
    }

    public NameSearchResult getResult() {
        return result;
    }

    public void setResult(NameSearchResult result) {
        this.result = result;
    }

    public SearchResultException getLastException() {
        return lastException;
    }

    public void setLastException(SearchResultException lastException) {
            this.lastException = lastException;
    }
    

}
