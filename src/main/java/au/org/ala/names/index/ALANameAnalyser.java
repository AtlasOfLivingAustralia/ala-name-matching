package au.org.ala.names.index;

import org.gbif.ecat.model.ParsedName;

import java.util.Map;
import java.util.Properties;

/**
 * A name analyser for the ALA.
 * <p>
 * Codes are returned as trimmed uppercase.
 * Scientific names are returned as trimmed uppercase with normalised spaces and
 * Scientific name authors are de-abbreviated, where possible and regularised.
 * </p>
 * <p>
 * The source of
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2017 CSIRO
 */
public class ALANameAnalyser extends NameAnalyser {
    private static String DEFAULT_ABBREVIATIONS = "author_abbreviations.properties";
    private static Map<String, String> AUTHOR_ABBREVIATIONS;

    static {
        Properties defaultAbbreviations = new Properties();
        defaultAbbreviations.load();
        }
    @Override
    public String canonicaliseCode(String code) {
        return code.toUpperCase().trim();
    }

    @Override
    public String canonicaliseName(ParsedName parsedName) {
        String canonicalName = parsedName.canonicalName();
        return canonicalName.toUpperCase().trim();
    }

    @Override
    public String canonicaliseAuthor(ParsedName parsedName, String author) {
        String authorship = parsedName.getAuthorship();
    }
}
