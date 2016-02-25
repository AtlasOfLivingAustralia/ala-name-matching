package au.org.ala.names.util;

import java.text.CharacterIterator;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cleaned scientific names to varying degrees,
 * <p>
 * Scientific names may contain non-breaking spaces.
 * They may also contain punctuation that is a bit weird and
 * they may also contain accented characters and ligatures.
 * This class provides various levels of cleanliness for the names.
 * </p>
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 *
 * @copyright Copyright (c) 2016 ALA
 */
public class CleanedScientificName {
    /** The non-ascii only pattern */
    private static final Pattern NON_ASCII = Pattern.compile("^[\\p{ASCII}]");
    /** The multiple space pattern */
    private static final Pattern SPACES = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    /** The punctuation translation table */
    private static final Substitute[] PUNCT_TRANSLATE = {
            new Substitute('\u00a0', ' '), // Non breaking space
            new Substitute('\u00ad', '-'), // Soft hyphen
            new Substitute('\u2010', '-'), // Hyphen
            new Substitute('\u2011', '-'), // Non-breaking hyphen
            new Substitute('\u2012', '-'), // Figure dash
            new Substitute('\u2013', '-'), // En-dash
            new Substitute('\u2014', '-'), // Em-dash
            new Substitute('\u2015', '-'), // Horizontal bar
            new Substitute('\u2018', '\''), // Single left quotation
            new Substitute('\u2019', '\''), // Single right quotation
            new Substitute('\u201a', '\''), // Single low quotation
            new Substitute('\u201b', '\''), // Single high reversed quotation
            new Substitute('\u201c', '"'), // Left quote
            new Substitute('\u201d', '"'), // Right quote
            new Substitute('\u201e', '"'), // Low quote
            new Substitute('\u201f', '"'), // Reversed high quote
            new Substitute('\u2027', ""), // Hyphenation point
            new Substitute('\u2028', ' '), // Line seperator
            new Substitute('\u2029', ' '), // Paragraph seperator
            new Substitute('\u202a', ""), // Left to right embedding
            new Substitute('\u202b', ""), // Right to left embeddiong
            new Substitute('\u202c', ""), // Pop directional formatting
            new Substitute('\u202d', ""), // Left to right override
            new Substitute('\u202e', ""), // Right to left override
            new Substitute('\u202f', ' '), // Narrow no break space
    };

    /** The special character translation table for spelling some things out and replacing interesting punctuation with basic latin versions */
    private static final Substitute[] BASIC_TRANSLATE = {
            new Substitute('\u00a1', '!'), // Inverted exclamation
            new Substitute('\u00a2', 'c'), // Cent sign
            new Substitute('\u00a3', '#'), // Pound sign
            new Substitute('\u00a4', '#'), // Currency sign
            new Substitute('\u00a5', 'Y'), // Yen
            new Substitute('\u00a6', '|'), // Borken bar
            new Substitute('\u00a7', '$'), // Section sign
            new Substitute('\u00a8', ""), // Diaresis
            new Substitute('\u00a9', 'c'), // Copyright
            new Substitute('\u00aa', ""), // Feminine ordinal
            new Substitute('\u00ab', "<<"), // Left angle quotation
            new Substitute('\u00ac', '~'), // Not sign
            new Substitute('\u00d7', " x "), // Multiplication sign
            new Substitute('\u00ae', 'r'), // Registerd
            new Substitute('\u00af', ' '), // Macron
            new Substitute('\u00b0', 'o'), // Degree
            new Substitute('\u00b1', "+-"), // Plus-minus
            new Substitute('\u00b2', '2'), // Superscipt 2
            new Substitute('\u00b3', '3'), // Superscript 3
            new Substitute('\u00b4', ""), // Acute accent
            new Substitute('\u00b5', 'u'), // Micro
            new Substitute('\u00b6', '@'), // Pilcrow
            new Substitute('\u00b7', '.'), // Middle dot
            new Substitute('\u00b8', ""), // Cedilla
            new Substitute('\u00b9', '1'), // Superscript 1
            new Substitute('\u00bb', ">>"), // Right angle quotation
            new Substitute('\u00bf', '?'), // Inverted question mark
            new Substitute('\u00df', "ss"), // Small sharp s
            new Substitute('\u03b1', " alpha "),
            new Substitute('\u03b2', " beta "),
            new Substitute('\u03b3', " gamma "),
            new Substitute('\u03b4', " delta "),
            new Substitute('\u03b5', " epsilon "),
            new Substitute('\u03b6', " zeta "),
            new Substitute('\u03b7', " eta"),
            new Substitute('\u03b8', " theta "),
            new Substitute('\u03ba', " kappa "),
            new Substitute('\u03bb', " lambda "),
            new Substitute('\u03bc', " mu "),
            new Substitute('\u03bd', " nu "),
            new Substitute('\u03be', " xi "),
            new Substitute('\u03bf', " omicron "),
            new Substitute('\u03c0', " pi "),
            new Substitute('\u03c1', " rho "),
            new Substitute('\u03c2', " sigma "),
            new Substitute('\u03c3', " sigma"),
            new Substitute('\u03c4', " tau "),
            new Substitute('\u03c5', " upsilon "),
            new Substitute('\u03c6', " phi "),
            new Substitute('\u03c7', " chi "),
            new Substitute('\u03c8', " psi "),
            new Substitute('\u03c9', " omega "),
            new Substitute('\u1e9e', "SS"), // Capital sharp s
            new Substitute('\u2016', '|'), // Double vertical line
            new Substitute('\u2017', '-'), // Double low line
            new Substitute('\u2020', '*'), // Dagger
            new Substitute('\u2021', '*'), // Double dagger
            new Substitute('\u2022', '*'), // Bullet
            new Substitute('\u2023', '*'), // Triangular bullet
            new Substitute('\u2024', '.'), // One dot leader
            new Substitute('\u2025', '.'), // Two dot leader
            new Substitute('\u2026', '.'), // Three dot leader
            new Substitute('\u2030', '%'), // Per mille
            new Substitute('\u2031', '%'), // Per ten thousand
            new Substitute('\u2032', '\''), // Prime
            new Substitute('\u2033', '"'), // Double prime
            new Substitute('\u2034', '"'), // Triple prime
            new Substitute('\u2035', '\''), // Reversed prime
            new Substitute('\u2036', '"'), // Reversed double prime
            new Substitute('\u2037', '"'), // Reversed triple prime
            new Substitute('\u2038', '^'), // Caret
            new Substitute('\u2039', '<'), // Left angle quote
            new Substitute('\u203a', '>'), // Right angle quote
            new Substitute('\u203b', '*'), // Reference mark
            new Substitute('\u203c', "!!"), // Double exclamation
            new Substitute('\u203d', "?!"), // Interrobang
            new Substitute('\u203e', '-'), // Overline
            new Substitute('\u203f', '_'), // Undertie
            new Substitute('\u2040', '-'), // Character tie
            new Substitute('\u2041', '^'), // Caret insertion point
            new Substitute('\u2042', '*'), // Asterism
            new Substitute('\u2043', '*'), // Hyphen bullet
            new Substitute('\u2044', '/'), // Fraction slash
            new Substitute('\u2045', '['), // Left bracket with quill
            new Substitute('\u2046', ']'), // Right bracket with quill
            new Substitute('\u2047', "??"), // Double question mark
            new Substitute('\u2715', " x "), // Multiplication x
            new Substitute('\u2a09', " x "), // n-ary cross
            new Substitute('\u2a7f', " x ") // Cross product
    };

    /** The source name */
    private String source;
    /** The basic name, with spaces reduced */
    private String name;
    /** The name with normalised punctuation and ligatures */
    private String normalised;
    /** The name with basic latin characters (ie. no accents and funny stuff */
    private String basic;

    private static Map<Character, String> PUNCT_MAP = null;
    private static Map<Character, String> BASIC_MAP = null;

    /**
     * Get the map for translating unicode punctuation to ASCII punctuation
     *
     * @return The punctuation map
     */
    protected static synchronized Map<Character, String> getPunctuationMap() {
        if (PUNCT_MAP == null) {
            PUNCT_MAP = new HashMap<Character, String>(100);
            for (Substitute sub: PUNCT_TRANSLATE)
                PUNCT_MAP.put(sub.ch, sub.sub);
        }
        return PUNCT_MAP;
    }

    /**
     * Get the map for translating unicode characters to ASCII spellings
     *
     * @return The punctuation map
     */
    protected static synchronized Map<Character, String> getBasicMap() {
        if (BASIC_MAP == null) {
            BASIC_MAP = new HashMap<Character, String>(100);
            for (Substitute sub: BASIC_TRANSLATE)
                BASIC_MAP.put(sub.ch, sub.sub);
        }
        return BASIC_MAP;
    }

    /**
     * Normalise spaces.
     * <p>
     * Replace all sequences of whitespace with a single space.
     * Remove fancy whitespace.
     * </p>
     *
     * @param name The name to translate
     *
     * @return The normalised name
     */
    protected String normaliseSpaces(String name) {
        Matcher matcher = this.SPACES.matcher(name);

        return matcher.replaceAll(" ").trim();
    }

    /**
     * Translate a string according to a translation map.
     *
     * @param name The string
     * @param map The translation map
     *
     * @return The translated string
     */
    protected String translate(String name, Map<Character, String> map) {
        StringBuilder builder = new StringBuilder(name.length());
        int i, len = name.length();

        for (i = 0; i < len; i++) {
            char ch = name.charAt(i);
            String r = map.get(ch);

            if (r == null)
                builder.append(ch);
            else
                builder.append(r);
        }
        return builder.toString();
    }

    /**
     * Construct for a source name.
     * @param source
     */
    public CleanedScientificName(String source) {
        assert source != null;
        this.source = source;
        this.name = null;
        this.normalised = null;
        this.basic = null;
    }

    /**
     * Build a name from a source string.
     * <p>
     * By default, all sequences of whitespace are replaced by a single space character.
     * </p>
     *
     * @param source The reduced source
     *
     * @return The norm
     */
    protected String buildName(String source) {
        return this.normaliseSpaces(source);
    }

    /**
     * Get the name.
     * <p>
     * The name is the source name, with spaces reduced by {@link #buildName(String)}
     * </p>
     *
     * @return The basic name
     */
    public String getName() {
        if (this.name == null)
            this.name = this.buildName(this.source);
        return this.name;
    }

    /**
     * Build a normalised name.
     * <p>
     * By default, most unicode punctuation is replaced by ASCII equivalents.
     * The string is also decomposed so that ligatures and the like are replaced by character sequences.
     * </p>
     *
     * @param name The name to normalise
     *
     * @return The built name
     */
    protected String buildNormalised(String name) {
        name = Normalizer.normalize(name, Normalizer.Form.NFKC);
        name = this.translate(name, this.getPunctuationMap());
        return this.normaliseSpaces(name);
    }

    /**
     * Get the normalised name.
     * <p>
     * The normalised name is the name from {@link #getName()} normaised by {@link #buildNormalised(String)}
     *
     * @return The
     */
    public String getNormalised() {
        if (this.normalised == null)
            this.normalised = this.buildNormalised(this.getName());
        return this.normalised;
    }

    /**
     * Has this got a normalised version that is different from the basic name?
     *
     * @return True if the normalised version is different
     */
    public boolean hasNormalised() {
        return !this.getName().equals(this.getNormalised());
    }

    /**
     * Build a basic latin version of a name.
     * <p>
     * Any accented characters are replaced by non-accented equivalents.
     * Any non-basic lating characters are removed.
     * </p>
     * @param name The name to make basic
     *
     * @return The basic name
     */
    protected String buildBasic(String name) {
        name = Normalizer.normalize(name, Normalizer.Form.NFD);
        name = this.translate(name, this.getBasicMap());
        name = this.normaliseSpaces(name);

        int i, len = name.length();
        StringBuilder builder = new StringBuilder();

        for (i = 0; i < len; i++) {
            char ch = name.charAt(i);
            if (ch < 128)
                builder.append(ch);
        }
        return builder.toString();
    }

    public String getBasic() {
        if (this.basic == null)
            this.basic = this.buildBasic(this.getNormalised());
        return this.basic;
    }


    /**
     * Has this got a normalised version that is different from the basic name?
     *
     * @return True if the normalised version is different
     */
    public boolean hasBasic() {
        return !this.getNormalised().equals(this.getBasic());
    }

    protected static class Substitute {
        public Character ch;
        public String sub;

        public Substitute(Character ch, String sub) {
            this.ch = ch;
            this.sub = sub;
        }

        public Substitute(char ch, char sub) {
            this.ch = ch;
            this.sub = new String(new char[] { sub });
        }
    }

}

