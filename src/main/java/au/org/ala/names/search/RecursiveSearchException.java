/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.ala.names.search;

import au.org.ala.names.model.ErrorType;
import java.util.Set;
import org.gbif.ecat.voc.NameType;

/**
 * Represents an exception that occurs during a recursive match.  Where there
 * are one or  more issues with the supplied name that prevented a match.
 * @author Natasha Carter
 */
public class RecursiveSearchException extends SearchResultException{
    private Set<ErrorType> errors;
    private NameType nameType;
    public RecursiveSearchException(Set<ErrorType> errors, NameType nameType){
        super("There are one or more issues with the name to prevent a match");
        this.errors = errors;
        this.nameType = nameType;
    }

    public Set<ErrorType> getErrors() {
        return errors;
    }

    public NameType getNameType(){
        return nameType;
    }

}
