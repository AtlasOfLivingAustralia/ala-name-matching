package au.org.ala.names.index;

import au.org.ala.names.parser.PhraseNameParser;
import org.gbif.ecat.model.ParsedName;
import org.gbif.ecat.parser.UnparsableException;

/**
 * Work out the indexable details of a name.
 * The key information produced are <em>canonical</em> versons of
 * nomenclatural code (which should ensure names are unique)
 * scientific name and scientific name authorship.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2017 CSIRO
 */
abstract public class NameAnalyser {
    private PhraseNameParser parser;

    public NameAnalyser() {
        this.parser = new PhraseNameParser();
    }

    public NameKey analyse(String code, String scientificName, String scientificNameAuthorship) throws UnparsableException {
        ParsedName parsedName = this.parser.parse(scientificName);
        String canonicalCode = this.canonicaliseCode(code);
        String canonicalName = this.canonicaliseName(parsedName);
        String canonicalAuthor = this.canonicaliseAuthor(parsedName, scientificNameAuthorship);
        return new NameKey(canonicalCode, canonicalName, canonicalAuthor);
    }

    /**
     * Canonicalise the nomenclatural code
     *
     * @param code The code name
     *
     * @return The canonicalised code
     */
    abstract public String canonicaliseCode(String code);

    /**
     * Canonicalise the scientfic name
     *
     * @param parsedName The parsed name
     *
     * @return The canonicalised scientfic name (no author)
     */
    abstract public String canonicaliseName(ParsedName parsedName);

    /**
     * Canonicalised the scientific name authorship
     *
     * @param parsedName The parsed name
     * @param author The separately supplied author
     *
     * @return The canonicalised author
     */
    abstract public String canonicaliseAuthor(ParsedName parsedName, String author);

    /**
     * A name key is a unique identifier for either a scientific name (code + name) or
     * a taxonomic concept (code + name + authorship)
     */
    public static class NameKey {
        /** The nomenclatural code for homonyms */
        public String code;
        /** The scientific name */
        public String scientificName;
        /** The authorship */
        public String scientificNameAuthorship;

        public NameKey(String code, String scientificName, String scientificNameAuthorship) {
            this.code = code;
            this.scientificName = scientificName;
            this.scientificNameAuthorship = scientificNameAuthorship;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NameKey nameKey = (NameKey) o;

            if (!code.equals(nameKey.code)) return false;
            if (!scientificName.equals(nameKey.scientificName)) return false;
            return scientificNameAuthorship != null ? scientificNameAuthorship.equals(nameKey.scientificNameAuthorship) : nameKey.scientificNameAuthorship == null;
        }

        @Override
        public int hashCode() {
            int result = code.hashCode();
            result = 31 * result + scientificName.hashCode();
            result = 31 * result + (scientificNameAuthorship != null ? scientificNameAuthorship.hashCode() : 0);
            return result;
        }
    }
}
