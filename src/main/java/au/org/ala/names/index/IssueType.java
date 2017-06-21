package au.org.ala.names.index;

/**
 * Vocabulary for reporting issues.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public enum IssueType {
    /** An invalid source taxonomy */
    VALIDATION,
    /** An error likely to make a taxonomy unusable */
    ERROR,
    /** A problem loading the taxonomy that needs to be addressed */
    PROBLEM,
    /** A note about processing */
    NOTE;
}
