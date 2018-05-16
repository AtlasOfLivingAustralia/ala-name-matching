package au.org.ala.names.index;

/**
 * What do to with names that we are going to discard.
 * <p>
 * Forbidden names, and the like, may be gone from the taxonomy but things like the identifiers and names
 * might need to hang around.
 * This enum gives the various possible strategies.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2018 Atlas of Living Australia
 */
public enum DiscardStrategy {
    /** Ignore the taxon and discard all information */
    IGNORE,
    /** Preserve the identifier as an identitier for the parent taxon */
    IDENTIFIER_TO_PARENT,
    /** Make the taxon a synonym of the parent taxon */
    SYNONYMISE_TO_PARENT
}
