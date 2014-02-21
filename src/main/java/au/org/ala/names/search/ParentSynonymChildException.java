
package au.org.ala.names.search;

import au.org.ala.names.model.NameSearchResult;

/**
 *
 * An exception that is thrown when the result represents a situation 
 * where the parent is a synonym of the child concept.
 *
 * @author Natasha Carter
 */
public class ParentSynonymChildException extends SearchResultException{
    private NameSearchResult parentResult;
    private NameSearchResult childResult;
    public ParentSynonymChildException(NameSearchResult parentResult, NameSearchResult childResult){
        super("The parent name is a synonym to the child name");
        this.parentResult = parentResult;
        this.childResult = childResult;
        errorType=au.org.ala.names.model.ErrorType.PARENT_CHILD_SYNONYM;

    }

    public NameSearchResult getChildResult() {
        return childResult;
    }

    public NameSearchResult getParentResult() {
        return parentResult;
    }

}
