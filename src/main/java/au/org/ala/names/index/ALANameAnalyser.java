package au.org.ala.names.index;

import au.org.ala.names.model.*;
import au.org.ala.names.util.CleanedScientificName;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.model.Equality;
import org.gbif.checklistbank.utils.SciNameNormalizer;
import org.gbif.common.parsers.NomStatusParser;
import org.gbif.common.parsers.core.ParseResult;
import org.gbif.nameparser.PhraseNameParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(ALANameAnalyser.class);
    /**
     * The default set of code identifiers. TODO allow overrides
     */
    private static final String DEFAULT_NOMENCLATURAL_CODE_MAP = "nomenclatural_codes.csv";
    /**
     * The default set of synonym identifiers. TODO allow overrides
     */
    private static final String DEFAULT_TAXONOMIC_TYPE_CODE_MAP = "taxonomic_type_codes.csv";
    /**
     * The default set of synonym identifiers. TODO allow overrides
     */
    private static final String DEFAULT_RANK_CODE_MAP = "rank_codes.csv";
    /**
     * The default set of synonym identifiers. TODO allow overrides
     */
    private static final String DEFAULT_NONEMCLATURAL_STATUS_CODE_MAP = "nomenclatural_status_codes.csv";
    /**
     * The default set of informal patterns. TODO allow overrides
     */
    private static final String DEFAULT_INFORMAL_PATTERN_LIST = "informal_names.csv";

    /**
     * Pattern for a bracketed sub-species name
     */
    protected static final Pattern BRACKETED = Pattern.compile("\\s\\(\\s*\\p{Alpha}+\\s*\\)\\s");


    private static final String RANK_MARKERS = Arrays.stream(Rank.values()).filter(r -> r.getMarker() != null).map(r -> r.getMarker().replaceAll("\\.", "\\.")).collect(Collectors.joining("|"));
    private static final String RANK_PLACEHOLDER_MARKERS = "\\p{Alpha}\\.";
    /**
     * Pattern for rank markers
     */
    protected static final Pattern MARKERS = Pattern.compile("\\s+(?:" + RANK_MARKERS + "|" + RANK_PLACEHOLDER_MARKERS + ")\\s+");
    /**
     * Pattern for non-name characters
     */
    protected static final Pattern NON_NAME = Pattern.compile("[^A-Za-z0-9'\\- ]+");

    /**
     * Pattern for repeated spaces
     */
    protected static final Pattern SPACES = Pattern.compile("\\s+");

    /**
     * Pattern for doubtful markers
     */
    protected static final Pattern DOUBTFUL = Pattern.compile("((^| )(undet|indet|aff|cf)[#!?\\.]?)+(?![a-z])");


    /**
     * Something that looks like an unplaced name
     */
    protected static final Predicate<String> PLACEHOLDER_TEST = Pattern.compile("(?i:species inquirenda|incertae sedis|unplaced)").asPredicate();
    /**
     * Something that looks like a hybrid
     */
    protected static final Predicate<String> HYBRID_TEST = Pattern.compile(" x ").asPredicate();
    /**
     * Something that looks like a cultivar
     */
    protected static final Predicate<String> CULTIVAR_TEST = Pattern.compile("\\p{Upper}\\p{Lower}+\\s+(?:\\p{Lower}+\\s+)?'[\\w\\s]+'").asPredicate();
    /**
     * Something that looks like an invalid name
     */
    protected static final Predicate<String> INVALID_TEST = Pattern.compile("^[^\\p{Alpha}]*$").asPredicate();
    /**
     * Something that looks like an doubtful name
     */
    protected static final Predicate<String> DOUBTFUL_TEST = DOUBTFUL.asPredicate();
    /**
     * Something that looks like a higher order scientific name
     */
    protected static final Predicate<String> HIGHER_SCIENTIFIC_TEST = Pattern.compile("^\\p{Upper}[\\p{Alpha}\\-]+(\\s+\\p{Alpha}[\\p{Alpha}\\-]+)?$").asPredicate();
    /**
     * Something that looks like a species level scientific name
     */
    protected static final Predicate<String> LOWER_SCIENTIFIC_TEST = Pattern.compile("^\\p{Upper}[\\p{Alpha}\\-]+\\s+\\p{Lower}[\\p{Lower}\\-]+(\\s+\\p{Lower}[\\p{Lower}\\-]+)?$").asPredicate();

    private Map<String, NomenclaturalCode> codeMap;
    private Map<String, TaxonomicType> taxonomicTypeMap;
    private Map<String, SynonymType> synonymMap;
    private Map<String, RankType> rankMap;
    private Map<String, NomenclaturalStatus> nomenclaturalStatusMap;
    private List<Pattern> informalPatterns;
    private NameParser nameParser;
    private NomStatusParser nomStatusParser;

    public ALANameAnalyser() {
        this.nameParser = new PhraseNameParser();
        this.nomStatusParser = NomStatusParser.getInstance();
        this.buildCodeMap();
        this.buildTaxonomicTypeMap();
        this.buildRankMap();
        this.buildNomenclaturalStatusMap();
        this.buildInformalPatternList();
    }

    /**
     * Analyze a name and turn it into a parsable form.
     *
     * @param code                     The nomenclatural code
     * @param scientificName           The scientific name
     * @param scientificNameAuthorship The authorship
     * @param rankType                 The taxon rank
     * @param taxonomicStatus          The taxonomic status
     * @param loose                    This is from a loose source that may have authors mixed up with names
     *
     * @return The analyzed name
     */
    @Override
    public NameKey analyse(@Nullable NomenclaturalCode code, String scientificName, @Nullable String scientificNameAuthorship, RankType rankType, TaxonomicType taxonomicStatus, boolean loose) {
        NameType nameType = NameType.INFORMAL;
        Set<NameFlag> flags = null;
        ParsedName name = null;

        if (scientificNameAuthorship != null && scientificName.endsWith(scientificNameAuthorship)) {
            scientificName = scientificName.substring(0, scientificName.length() - scientificNameAuthorship.length()).trim();
        }
        try {
            name = this.nameParser.parse(scientificName, (rankType == null || rankType == RankType.UNRANKED) ? null : rankType.getCbRank());
            if (name != null) {
                nameType = name.getType();
                if (rankType == null && name.getRank() != null)
                    rankType = RankType.getForCBRank(name.getRank());
            }
        } catch (UnparsableException ex) {
            // Oh well, worth a try
        }
        if (loose) {
            if (scientificNameAuthorship == null && name != null) {
                String ac = name.authorshipComplete();
                if (ac != null && !ac.isEmpty() && !(name instanceof ALAParsedName)) { // ALAParsedName indicates a phrase name; leave as-is
                    scientificName = name.buildName(true, true, false, true, true, false, true, false, true, false, false, false, true, true);
                    scientificNameAuthorship = ac;
                }
            }
        }
        CleanedScientificName cleaned = new CleanedScientificName(scientificName);
        scientificName = cleaned.getBasic();

        // Remove bracketed names
        scientificName = BRACKETED.matcher(scientificName).replaceAll(" ");

        // Remove markers
        scientificName = MARKERS.matcher(scientificName).replaceAll(" ");


        // Categorize
        if (PLACEHOLDER_TEST.test(scientificName) || (taxonomicStatus != null && taxonomicStatus.isPlaceholder())) {
            scientificName = scientificName + " " + UUID.randomUUID().toString();
            nameType = NameType.PLACEHOLDER;
        } else if (code == NomenclaturalCode.VIRUS) {
            nameType = NameType.VIRUS;
        } else if (code == NomenclaturalCode.BACTERIAL) {
            nameType = NameType.SCIENTIFIC;
        } else if (HYBRID_TEST.test(scientificName)) {
            nameType = NameType.HYBRID;
        } else if (code == NomenclaturalCode.CULTIVARS || CULTIVAR_TEST.test(scientificName)) {
            nameType = NameType.CULTIVAR;
        } else if (INVALID_TEST.test(scientificName)) {
            scientificName = UUID.randomUUID().toString();
            nameType = NameType.NO_NAME;
        } else if (DOUBTFUL_TEST.test(scientificName)) {
            nameType = NameType.DOUBTFUL;
        } else if (rankType == null && (HIGHER_SCIENTIFIC_TEST.test(scientificName) || LOWER_SCIENTIFIC_TEST.test(scientificName))) {
            nameType = NameType.SCIENTIFIC;
        } else if (rankType != null && rankType.isHigherThan(RankType.SPECIES) && HIGHER_SCIENTIFIC_TEST.test(scientificName)) {
            nameType = NameType.SCIENTIFIC;
        } else if(LOWER_SCIENTIFIC_TEST.test(scientificName)) {
            nameType = NameType.SCIENTIFIC;
            scientificName = SciNameNormalizer.normalize(scientificName);
        }
        if (rankType == null)
            rankType = RankType.UNRANKED;

        // Remove non-name characters
        scientificName = NON_NAME.matcher(scientificName).replaceAll(" ");
        scientificName = SPACES.matcher(scientificName).replaceAll(" ");

        scientificName = scientificName.trim().toUpperCase();

        // Normalise authorship
        if (scientificNameAuthorship != null) {
            CleanedScientificName cleanedAuthor = new CleanedScientificName(scientificNameAuthorship);
            scientificNameAuthorship = cleanedAuthor.getBasic();
        }
        scientificNameAuthorship = scientificNameAuthorship == null || scientificNameAuthorship.isEmpty() ? null : scientificNameAuthorship;

        // Flags
        if (name != null && name.isAutonym()) {
            scientificName = name.canonicalName();
            flags = Collections.singleton(NameFlag.AUTONYM);
            scientificNameAuthorship = null;
        }

        return new NameKey(this, code, scientificName.toUpperCase(), scientificNameAuthorship, rankType, nameType, flags);
    }

    /**
     * Load a set of additional terms for a controlled vocabulary.
     * <p>
     * The standard format of the CSV file is:
     * </p>
     * <ol>
     *     <li>Vocabulary label, used to look up the term in the vocabulary</li>
     *     <li>The enum name to map this label onto</li>
     *     <li>A description of the label</li>
     *     <li>A reference URL, if available</li>
     * </ol>
     *
     * @param resource The resource path (resolved against the class)
     * @param map The map to load
     * @param clazz The vocabulary class
     * @param <T> The vocabulary class
     */
    protected <T extends Enum<T>> void loadCsv(String resource, Map<String, T> map, Class<T> clazz) {
        try {
            CSVReader reader = new CSVReaderBuilder(new InputStreamReader(this.getClass().getResourceAsStream(resource), "UTF-8")).withSkipLines(1).build();
            String[] next;
            while ((next = reader.readNext()) != null) {
                String label = next[0];
                String val = next[1];
                T value = null;
                if (val != null ) {
                    val = val.trim();
                    value = val.isEmpty() ? null : Enum.valueOf(clazz, val);
                }
                map.put(label.toUpperCase().trim(), value);
            }
        } catch (Exception ex) {
            LOGGER.error("Unable to build map for " + clazz, ex);
        }

    }

    /**
     * Load a list of additional items.
     * <p>
     * The standard format of the CSV file is:
     * </p>
     * <ol>
     *     <li>A label for the item (not used)</li>
     *     <li>The item constructor string</li>
     *     <li>A description of the item</li>
     *     <li>A reference URL, if available</li>
     * </ol>
     *
     * @param resource The resource path (resolved against the class)
     * @param list The list to load
     */
    protected void loadPatternCsv(String resource, List<Pattern> list) {
        try {
            CSVReader reader = new CSVReader(new InputStreamReader(this.getClass().getResourceAsStream(resource), "UTF-8"), ',', '"', 1);
            String[] next;
            while ((next = reader.readNext()) != null) {
                String label = next[0];
                String val = next[1];
                if (val != null) {
                    val = val.trim();
                    val = val.isEmpty() ? null : val;
                }
                Pattern value = Pattern.compile(val);
                list.add(value);
            }
        } catch (Exception ex) {
            LOGGER.error("Unable to build pattern list", ex);
        }

    }

    /**
     * Build a code map.
     */
    protected void buildCodeMap() {
        this.codeMap = new HashMap<>(64);
        for (NomenclaturalCode c: NomenclaturalCode.values())
            this.codeMap.put(c.getAcronym().toUpperCase().trim(), c);
        this.loadCsv(DEFAULT_NOMENCLATURAL_CODE_MAP, this.codeMap, NomenclaturalCode.class);
    }

    /**
     * Build a taxonomic status map.
     */
    protected void buildTaxonomicTypeMap() {
        this.taxonomicTypeMap = new HashMap<String, TaxonomicType>(64);
        for (TaxonomicType s: TaxonomicType.values()) {
            this.taxonomicTypeMap.put(s.getTerm().toUpperCase().trim(), s);
            if (s.getLabels() != null) {
                for (String l : s.getLabels())
                    this.taxonomicTypeMap.put(l.toUpperCase().trim(), s);
            }
        }
        this.loadCsv(DEFAULT_TAXONOMIC_TYPE_CODE_MAP, this.taxonomicTypeMap, TaxonomicType.class);
    }

    /**
     * Build a rank map.
     */
    protected void buildRankMap() {
        this.rankMap = new HashMap<>(64);
        for (RankType r: RankType.values()) {
            this.rankMap.put(r.getRank().toUpperCase().trim(), r);
            if (r.getStrRanks() != null) {
                for (String label : r.getStrRanks())
                    this.rankMap.put(label.toUpperCase().trim(), r);
            }
        }
        this.loadCsv(DEFAULT_RANK_CODE_MAP, this.rankMap, RankType.class);
    }
    /**
     * Build a synonym map.
     */
    protected void buildNomenclaturalStatusMap() {
        this.nomenclaturalStatusMap = new HashMap<>(64);
        // Use NomStatusParser for default values
        this.loadCsv(DEFAULT_NONEMCLATURAL_STATUS_CODE_MAP, this.nomenclaturalStatusMap, NomenclaturalStatus.class);
    }

    protected void buildInformalPatternList() {
        this.informalPatterns = new ArrayList<>(32);
        this.loadPatternCsv(DEFAULT_INFORMAL_PATTERN_LIST, this.informalPatterns);
    }

    /**
     * Canonicalise the nomenclatural code.
     *
     * @param code The code name
     *
     * @return The mapped code or null for not found
     */
    @Override
    public NomenclaturalCode canonicaliseCode(String code) {
        if (code == null)
            return null;
        code = code.toUpperCase().trim();
        NomenclaturalCode nc = this.codeMap.get(code);
        if (nc == null)
            this.report(IssueType.PROBLEM, "nomenclaturalCode.notFound", null, null, code);
        return nc;
    }

    /**
     * Canonicalise the taxonomic status.
     * <p>
     * If the term cannot be parsed, {@link TaxonomicType#INFERRED_UNPLACED} is used.
     * </p>
     *
     * @param taxonomicStatus The taxonomic status term
     *
     * @return The mapped status
     */
    @Override
    public TaxonomicType canonicaliseTaxonomicType(String taxonomicStatus) {
        if (taxonomicStatus == null)
            return TaxonomicType.INFERRED_UNPLACED;
        taxonomicStatus = taxonomicStatus.toUpperCase().trim();
        if (taxonomicStatus.isEmpty())
            return TaxonomicType.INFERRED_UNPLACED;
        TaxonomicType type = this.taxonomicTypeMap.get(taxonomicStatus);
        if (type == null) {
            this.report(IssueType.PROBLEM, "taxonomicStatus.notFound", null, null, taxonomicStatus);
            type = TaxonomicType.INFERRED_UNPLACED;
            synchronized (this) {
                this.taxonomicTypeMap.put(taxonomicStatus, type); // Report once
            }
        }
        return type;
    }

    /**
     * Canonicalise the rank.
     *
     * @param rank The rank term
     *
     * @return The mapped rank
     */
    @Override
    public RankType canonicaliseRank(String rank) {
        if (rank == null)
            return RankType.UNRANKED;
        rank = rank.toUpperCase().trim();
        if (rank.isEmpty())
            return RankType.UNRANKED;
        RankType rankType = this.rankMap.get(rank);
        if (rankType == null) {
            this.report(IssueType.PROBLEM, "rank.notFound", null, null, rank);
            rankType = RankType.UNRANKED;
            synchronized (this) {
                this.rankMap.put(rank, rankType); // Report once
            }
        }
        return rankType;
    }

    /**
     * Canonicalise the nomenclatural status.
     *
     * @param nomenclaturalStatus The nomenclatural status term
     *
     * @return The mapped nomenclatural status
     */
    @Override
    public NomenclaturalStatus canonicaliseNomenclaturalStatus(String nomenclaturalStatus) {
        if (nomenclaturalStatus == null)
            return null;
        ParseResult<NomenclaturalStatus> parsed = this.nomStatusParser.parse(nomenclaturalStatus);
        if (parsed.isSuccessful())
            return parsed.getPayload();
        nomenclaturalStatus = nomenclaturalStatus.toUpperCase().trim();
        if (nomenclaturalStatus.isEmpty())
            return null;
        NomenclaturalStatus status = this.nomenclaturalStatusMap.get(nomenclaturalStatus);
        if (status == null && !this.nomenclaturalStatusMap.containsKey(nomenclaturalStatus)) {
            this.report(IssueType.PROBLEM, "nomenclaturalStatus.notFound", null, null, nomenclaturalStatus);
            synchronized (this) {
                this.nomenclaturalStatusMap.put(nomenclaturalStatus, null); // Report once
            }
        }
        return status;
    }

    /**
     * Test for a known informal name.
     * <p>
     * The name is compared against the list of known informal patterns in the informal pattern list
     * </p>
     *
     * @param name The name
     *
     * @return True if a known informal name type
     */
    @Override
    public boolean isInformal(String name) {
        for (Pattern p: this.informalPatterns)
            if (p.matcher(name).matches())
                return true;
        return false;
    }
}
