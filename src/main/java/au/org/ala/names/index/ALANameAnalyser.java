package au.org.ala.names.index;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.model.Equality;
import org.gbif.checklistbank.utils.SciNameNormalizer;

/**
 * A name analyser for the ALA.
 * <p>
 * Codes are returned as trimmed uppercase.
 * Scientific names are returned as trimmed uppercase with normalised spaces and regularised  and
 * Scientific name authors are de-abbreviated, where possible and regularised.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2017 CSIRO
 */
public class ALANameAnalyser extends NameAnalyser {
    private SciNameNormalizer nameNormalizer;
    private AuthorComparator authorComparator;

    public ALANameAnalyser() {
        this.nameNormalizer = new SciNameNormalizer();
        this.authorComparator = AuthorComparator.createWithAuthormap();
    }

    /**
     * Canonicalise the code.
     *
     * @param code The code name
     *
     * @return Trimmed, uppercase code
     */
    @Override
    public String canonicaliseCode(String code) {
        return code.toUpperCase().trim();
    }

    /**
     * Canonicalise the scientific name.
     *
     * @param parsedName The parsed name
     *
     * @return The trimmed, uppercase canonical name, with endings regularised via a {@link SciNameNormalizer}
     */
    @Override
    public String canonicaliseName(ParsedName parsedName) {
        String canonicalName = parsedName.canonicalName().trim();
        canonicalName = SciNameNormalizer.normalizeAll(canonicalName);
        return canonicalName.toUpperCase();
    }

    @Override
    public String canonicaliseAuthor(ParsedName parsedName, String author) {
        author = author != null ? author : parsedName.getAuthorship();
        return author != null ? author.trim() : null;
    }

    /**
     * Compare two keys.
     * <p>
     *     Comparison is equal if the codes, names and authors are equal.
     *     Authorship equality is decided by a {@link AuthorComparator}
     *     which can get a wee bit complicated.
     * </p>
     *
     * @param key1 The first key to compare
     * @param key2 The second key to compare
     *
     * @return
     */
    @Override
    public int compare(NameKey key1, NameKey key2) {
        int cmp;

        if ((cmp = key1.code.compareTo(key2.code)) != 0)
            return cmp;
        if ((cmp = key1.scientificName.compareTo(key2.scientificName)) != 0)
            return cmp;
        if (key1.scientificNameAuthorship == null && key2.scientificName == null)
            return 0;
        if (key1.scientificNameAuthorship == null && key2.scientificName != null)
            return -1;
        if (key1.scientificNameAuthorship != null && key2.scientificName == null)
            return 1;
        if (authorComparator.compare(key1.scientificNameAuthorship, null, key2.scientificNameAuthorship, null) == Equality.EQUAL)
            return 0;
        return key1.scientificNameAuthorship.compareTo(key2.scientificNameAuthorship);
    }

    /**
     * Compute a hash code for a key.
     * <p>
     *     Based on hashing for the code and name. Only author presence/absence is calculated.
     * </p>
     * @param key1 The key
     *
     * @return
     */
    @Override
    public int hashCode(NameKey key1) {
        int hash = key1.code.hashCode();
        hash = hash * 31 + key1.scientificName.hashCode();
        hash = hash * 31 + (key1.scientificNameAuthorship == null ? 0 : 1181);
        return hash;
    }
}
