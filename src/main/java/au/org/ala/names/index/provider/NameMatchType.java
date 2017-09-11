package au.org.ala.names.index.provider;

/**
 * How to match a name or author
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public enum NameMatchType {
    /** Exact match */
    EXACT,
    /** Case and space insensitive */
    INSENSITIVE,
    /** Normalised by GBIF name analysis @see org.gbif.checklistbank.utils.SciNameNormalizer @see org.gbif.checklistbank.authorship.AuthorComparator */
    NORMALISED,
    /** Reguilar expression match */
    REGEX
}
