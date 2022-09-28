/*
 * Copyright (c) 2021 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 */

package au.org.ala.names.index;

import au.org.ala.names.model.*;
import au.org.ala.names.util.CleanedScientificName;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.commons.lang3.StringUtils;
import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.api.vocabulary.*;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.utils.SciNameNormalizer;
import org.gbif.common.parsers.LifeStageParser;
import org.gbif.common.parsers.NomStatusParser;
import org.gbif.common.parsers.OccurrenceStatusParser;
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
     * Pattern for a sub-species name in parentheses
     */
    protected static final Pattern PARENTHESIS = Pattern.compile("\\s\\(\\s*\\p{Alpha}+\\s*\\)\\s");
    /**
     * Pattern for annotations in brackets or braces
     */
    protected static final Pattern BRACKETED = Pattern.compile("\\s(\\[.+\\]|\\{.+\\})");

    /**
     * Detect a rank name where a nomrmal name should go
     */
    private static final String RANK_NAMES = Arrays.stream(Rank.values()).map(Rank::name).collect(Collectors.joining("|"));
    private static final Pattern RANK_AS_NAME = Pattern.compile("(?i:" + RANK_NAMES + ")");

    private static final String RANK_MARKERS_STRICT = Arrays.stream(Rank.values()).filter(r -> r.getMarker() != null).map(r -> r.getMarker().replaceAll("\\.", "\\.")).collect(Collectors.joining("|"));
    private static final String RANK_MARKERS_LOOSE = Arrays.stream(Rank.values()).filter(r -> r.getMarker() != null).map(r -> r.getMarker().replaceAll("\\.", "\\.?")).collect(Collectors.joining("|"));
    private static final String RANK_PLACEHOLDER_MARKERS = "\\p{Alpha}\\.";
    /**
     * Pattern for rank markers
     */
    protected static final Pattern STRICT_MARKERS = Pattern.compile("\\s+(?:" + RANK_MARKERS_STRICT + "|" + RANK_PLACEHOLDER_MARKERS + ")\\s+");
    /**
     * Pattern for bare (no proper period) rank markers
     */
    protected static final Pattern LOOSE_MARKERS = Pattern.compile("\\s+(?:" + RANK_MARKERS_LOOSE + "|" + RANK_PLACEHOLDER_MARKERS + ")\\.?\\s+");
    /**
     * Pattern for unsure markers (cf, aff etc)
     */
    protected static final Pattern UNSURE_MARKER = Pattern.compile("\\s+(?:cf|cfr|conf|aff)\\.?\\s+" );

    /**
     * Pattern for non-name characters
     */
    protected static final Pattern NON_NAME = Pattern.compile("[^A-Za-z0-9'\"\\- ]+");

    /**
     * Pattern for repeated spaces
     */
    protected static final Pattern SPACES = Pattern.compile("\\s+");

    /**
     * Pattern for escaped characters
     */
    protected static final Pattern ESCAPES = Pattern.compile("\\\\(.)");

    /**
     * Pattern for actual escapes
     */
    protected static final Pattern ESCAPE = Pattern.compile("\\\\");

    /**
     * Pattern for and in authors
     */
    protected static final Pattern AUTHOR_AND = Pattern.compile("\\s+and\\s+");

    /**
     * Pattern for an unconsumed year marker immediately after an author
     */
    protected static final Pattern YEAR_MARKER = Pattern.compile("^\\s*,\\s*\\d{4}($|\\s)");

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
    /**
     * Pattern for initial quotes around a genus name (who does this? brown moth people)
     */
    protected static final Predicate<String> INITIAL_QUOTES_TEST = Pattern.compile("^['\"]\\s*(\\p{Upper}\\p{Lower}+)]\\s*['\"]'").asPredicate();

    private Map<String, TaxonomicType> taxonomicTypeMap;
    private Map<String, SynonymType> synonymMap;
    private Map<String, RankType> rankMap;
    private Map<String, NomenclaturalStatus> nomenclaturalStatusMap;
    private List<Pattern> informalPatterns;
    private NameParser nameParser;
    private NomStatusParser nomStatusParser;
    private LifeStageParser lifeStageParser;
    private OccurrenceStatusParser occurrenceStatusParser;

    public ALANameAnalyser(AuthorComparator authorComparator, Reporter reporter) {
        super(authorComparator, reporter);
        this.nameParser = new PhraseNameParser();
        this.nomStatusParser = NomStatusParser.getInstance();
        this.lifeStageParser = LifeStageParser.getInstance();
        this.occurrenceStatusParser = OccurrenceStatusParser.getInstance();
        this.buildTaxonomicTypeMap();
        this.buildRankMap();
        this.buildNomenclaturalStatusMap();
        this.buildInformalPatternList();
    }

    public ALANameAnalyser() {
        this(AuthorComparator.createWithAuthormap(), null);
    }

    /**
     * Analyze a name and turn it into a parsable form.
     *
     * @param code                     The nomenclatural code
     * @param scientificName           The scientific name
     * @param scientificNameAuthorship The authorship
     * @param rankType                 The taxon rank
     * @param taxonomicStatus          The taxonomic status
     * @param flags                    Taxonomic flags from the instance
     * @param loose                    This is from a loose source that may have authors mixed up with names
     *
     * @return The analyzed name
     */
    @Override
    public AnalysisResult analyse(@Nullable NomenclaturalClassifier code, String scientificName, @Nullable String scientificNameAuthorship, RankType rankType, TaxonomicType taxonomicStatus, Set<TaxonFlag> flags, boolean loose) {
        NameType nameType = NameType.INFORMAL;
        ParsedName name = null;
        ALAParsedName phraseName = null;

        scientificName = this.normalise(scientificName);
        scientificNameAuthorship = this.normalise(scientificNameAuthorship);
        // Remove embedded author, if any, taking care of a trailing year marker if there is one
        if (scientificNameAuthorship != null) {
            int p = scientificName.indexOf(scientificNameAuthorship);
            if (p >= 0) {
                String left = scientificName.substring(0, p);
                String right = scientificName.substring(p + scientificNameAuthorship.length());
                right = YEAR_MARKER.matcher(right).replaceFirst(" ");
                scientificName = (left + " " + right).trim();
            }
        }
        try {
            name = this.nameParser.parse(scientificName, (rankType == null || rankType == RankType.UNRANKED) ? null : rankType.getCbRank());
            if (name != null) {
                if (name instanceof ALAParsedName && ((ALAParsedName) name).cleanPhrase != null) {
                    phraseName = (ALAParsedName) name;
                }
                nameType = name.getType();
                if (rankType == null && name.getRank() != null)
                    rankType = RankType.getForCBRank(name.getRank());
            }
        } catch (UnparsableException ex) {
            LOGGER.info("Unable to parse " + name + ": " + ex.getMessage());
        }
        if (UNSURE_MARKER.matcher(scientificName).find()) {
            // Leave this well alone but indicate that it is doubtful
            nameType = NameType.DOUBTFUL;
        } else {
            if (loose) {
                if (scientificNameAuthorship == null && name != null) {
                    String ac = this.normalise(name.authorshipComplete());
                    if (ac != null && !ac.isEmpty() && !(name instanceof ALAParsedName)) { // ALAParsedName indicates a phrase name; leave as-is
                        scientificName = name.buildName(true, true, false, true, true, false, true, false, true, false, false, false, true, true);
                        scientificNameAuthorship = ac;
                    }
                }
            }
        }

        // Remove parenthesis names
        scientificName = PARENTHESIS.matcher(scientificName).replaceAll(" ");

        // Remove markers (loose markers for the win, since there appears to be no consistency)
        scientificName = LOOSE_MARKERS.matcher(scientificName).replaceAll(" ");


        // Categorize
        if (phraseName != null) {
            nameType = NameType.PLACEHOLDER;
            scientificName = phraseName.getGenusOrAbove();
            if (phraseName.getRank() != null) {
                scientificName = scientificName + " " + phraseName.getRank().getMarker();
            }
            scientificName = scientificName + " " + phraseName.cleanPhrase;
            if (phraseName.cleanVoucher != null) {
                scientificName = scientificName + " " + phraseName.cleanVoucher;
            }
        } if (taxonomicStatus == TaxonomicType.MISCELLANEOUS_LITERATURE) {
            nameType = NameType.INFORMAL;
        } else if (PLACEHOLDER_TEST.test(scientificName) || (taxonomicStatus != null && taxonomicStatus.isPlaceholder())) {
            scientificName = scientificName + " " + UUID.randomUUID().toString();
            nameType = NameType.PLACEHOLDER;
        } else if (code == NomenclaturalClassifier.VIRUS) {
            nameType = NameType.VIRUS;
        } else if (code == NomenclaturalClassifier.BACTERIAL) {
            nameType = NameType.SCIENTIFIC;
        } else if (HYBRID_TEST.test(scientificName)) {
            nameType = NameType.HYBRID;
        } else if (code == NomenclaturalClassifier.CULTIVARS || CULTIVAR_TEST.test(scientificName)) {
            nameType = NameType.CULTIVAR;
        } else if (INVALID_TEST.test(scientificName)) {
            scientificName = UUID.randomUUID().toString();
            nameType = NameType.NO_NAME;
        } else if (DOUBTFUL_TEST.test(scientificName)) {
            nameType = NameType.DOUBTFUL;
        } else if (INITIAL_QUOTES_TEST.test(scientificName)) {
            nameType = NameType.DOUBTFUL;
        } else if (rankType == null && (HIGHER_SCIENTIFIC_TEST.test(scientificName) || LOWER_SCIENTIFIC_TEST.test(scientificName))) {
            nameType = NameType.SCIENTIFIC;
        } else if (rankType != null && rankType.isHigherThan(RankType.SPECIES) && HIGHER_SCIENTIFIC_TEST.test(scientificName)) {
            nameType = NameType.SCIENTIFIC;
            scientificName = SciNameNormalizer.normalize(scientificName);
        } else if(LOWER_SCIENTIFIC_TEST.test(scientificName)) {
            nameType = NameType.SCIENTIFIC;
            scientificName = SciNameNormalizer.normalize(scientificName);
        }

        // Default rank
        if (rankType == null)
            rankType = RankType.UNRANKED;

        // Flags
        if (name != null && name.isAutonym()) {
            scientificName = name.canonicalName();
            flags = flags == null ? new HashSet<>() : new HashSet<>(flags);
            flags.add(TaxonFlag.AUTONYM);
            scientificNameAuthorship = null;
        }

        // Remove non-name characters
        scientificName = NON_NAME.matcher(scientificName).replaceAll(" ");
        scientificName = SPACES.matcher(scientificName).replaceAll(" ");

        scientificName = scientificName.trim().toUpperCase();


        NameKey key = new NameKey(this, code, scientificName, scientificNameAuthorship, rankType, nameType, flags);
        String mononomial = null;
        String genus = null;
        String specificEpithet = null;
        String infraspecificEpithet = null;
        String cultivarEpithet = null;
        if (name != null) {
            mononomial = name.getGenusOrAbove();
            if (mononomial != null && RANK_AS_NAME.matcher(mononomial).matches()) {
                mononomial = null;
            } else {
                genus = rankType != null && !rankType.isHigherThan(RankType.GENUS) ? mononomial : null;
                if (phraseName == null) {
                    specificEpithet = name.getSpecificEpithet();
                    infraspecificEpithet = name.getInfraSpecificEpithet();
                    cultivarEpithet = name.getCultivarEpithet();
                }
            }
        }
        return new AnalysisResult(key, mononomial, genus, specificEpithet, infraspecificEpithet, cultivarEpithet);
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
            CSVParser csvParser = new CSVParserBuilder()
                    .withSeparator(',')
                    .withQuoteChar('"')
                    .withEscapeChar('\\')
                    .build();
            CSVReader reader = new CSVReaderBuilder(new InputStreamReader(this.getClass().getResourceAsStream(resource), "UTF-8"))
                    .withCSVParser(csvParser)
                    .withSkipLines(1)
                    .build();
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
    public NomenclaturalClassifier canonicaliseCode(String code) {
        if (code == null)
            return null;
        code = code.toUpperCase().trim();
        NomenclaturalClassifier nc = NomenclaturalClassifier.find(code);
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
     * Canonicalise a supplied taxonomic flag
     *
     * @param flag The flag name
     * @return The matching flag
     */
    @Override
    public TaxonFlag canonicaliseFlag(String flag) {
        if (StringUtils.isBlank(flag))
            return null;
        flag = flag.trim();
        if (flag.equalsIgnoreCase("autonym"))
            return TaxonFlag.AUTONYM;
        if (flag.equalsIgnoreCase("ambiguousNomenclaturalCode"))
            return TaxonFlag.AMBIGUOUS_NOMENCLATURAL_CODE;
        if (flag.equalsIgnoreCase("synthetic"))
            return TaxonFlag.SYNTHETIC;
        throw new IllegalArgumentException("Unrecognised flag " + flag);
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
     * Canonicalise the life stage
     *
     * @param lifeStage the life stage string
     *
     * @return The matching life stage or null for non-matched
     */
    @Override
    public LifeStage canonicaliseLifeStage(String lifeStage) {
        if (lifeStage == null || lifeStage.isEmpty())
            return null;
        ParseResult<LifeStage> result = this.lifeStageParser.parse(lifeStage);
        if (!result.isSuccessful()) {
            this.report(IssueType.VALIDATION, "taxonomy.load.lifeStage.invalid", lifeStage, "");
            return null;
        }
        return result.getPayload();
    }

    /**
     * Canonicalise the occurrence status
     *
     * @param occurrenceStatus the occurrence status string
     *
     * @return The occurrence status or null for non-matched
     */
    @Override
    public OccurrenceStatus canonicaliseOccurrenceStatus(String occurrenceStatus) {
        if (occurrenceStatus == null || occurrenceStatus.isEmpty())
            return null;
        ParseResult<OccurrenceStatus> result = this.occurrenceStatusParser.parse(occurrenceStatus);
        if (!result.isSuccessful()) {
            this.report(IssueType.VALIDATION, "taxonomy.load.occurrenceStatus.invalid", occurrenceStatus, "");
            return null;
        }
        return result.getPayload();
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

    /**
     * Normalise a name or an author with simple
     *
     * @return The resulting name, or null for no identifiable name
     */
    protected String normalise(String name) {
        if (name == null)
            return null;
        CleanedScientificName cleaned = new CleanedScientificName(name);
        name = cleaned.getBasic();
        if (name.isEmpty())
            return null;
        name = ESCAPES.matcher(name).replaceAll("$1"); // Remove escaped letters
        name = ESCAPE.matcher(name).replaceAll(""); // Remove left-over escapes
        name = BRACKETED.matcher(name).replaceAll(" "); // Remove bracheted annotations
        name = AUTHOR_AND.matcher(name).replaceAll(" & "); // Replace and with &
        name = SPACES.matcher(name).replaceAll(" "); // Re-normaliose spaces
        return name.isEmpty() ? null : name;
    }
}
