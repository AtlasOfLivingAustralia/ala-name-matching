
package au.org.ala.checklist.lucene;

/**
 * The exception to be thrown when a Genus spp. scientific name is supplied
 * @author Natasha Carter
 */
public class SPPException extends SearchResultException {
public SPPException(){
    super("Unable to perform search. Can not match to a subset of species within a genus.");
    errorType= au.org.ala.checklist.lucene.model.ErrorType.SPECIES_PLURAL;
}
}
