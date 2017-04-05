package au.org.ala.names.index;

import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.nameparser.PhraseNameParser;

import java.util.Comparator;

/**
 * Work out the indexable details of a name.
 * The key information produced are <em>canonical</em> versons of
 * nomenclatural code (which should ensure names are unique)
 * scientific name and scientific name authorship.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2017 CSIRO
 */
abstract public class NameAnalyser implements Comparator<NameAnalyser.NameKey> {
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
     * Canonicalise the scientfic authorship
     *
     * @param parsedName The parsed name
     * @param scientificNameAuthorship The supplied author
     *
     * @return The canonicalised authorship
     */
    abstract public String canonicaliseAuthor(ParsedName parsedName, String scientificNameAuthorship);

    /**
     * Compare two name keys.
     * <p>
     * What constitutes "equal" depends on the implementation of the analyser.
     * </p>
     *
     * @param key1 The first key to compare
     * @param key2 The second key to compare
     *
     * @return Less than, equal to or greater than zero depending on whether key1 is less than, equal to or greater than key2
     */
    abstract public int compare(NameKey key1, NameKey key2);

    /**
     * Compute a hash code for a name key.
     * <p>
     * The hash code computation has to be within
     * </p>
     *
     * @param key1 The key
     *
     * @return The resulting hash code
     */
    abstract public int hashCode(NameKey key1);

    /**
     * A name key is a unique identifier for either a scientific name (code + name) or
     * a taxonomic concept (code + name + authorship)
     */
    public class NameKey {
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
            return NameAnalyser.this.compare(this, nameKey) == 0;
        }

        @Override
        public int hashCode() {
            return NameAnalyser.this.hashCode(this);
        }
    }
}
