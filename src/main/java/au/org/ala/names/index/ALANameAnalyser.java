package au.org.ala.names.index;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.names.model.RankType;
import au.org.ala.names.model.SynonymType;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.model.Equality;
import org.gbif.checklistbank.utils.SciNameNormalizer;
import org.gbif.common.parsers.NomStatusParser;
import org.gbif.common.parsers.core.ParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

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
    /** The default set of code identifiers. TODO allow overrides */
    private static final String DEFAULT_NOMENCLATURAL_CODE_MAP = "nomenclatural_codes.csv";
    /** The default set of synonym identifiers. TODO allow overrides */
    private static final String DEFAULT_TAXONOMIC_STATUS_CODE_MAP = "taxonomic_status_codes.csv";
    /** The default set of synonym identifiers. TODO allow overrides */
    private static final String DEFAULT_SYNONYM_CODE_MAP = "synonym_codes.csv";
    /** The default set of synonym identifiers. TODO allow overrides */
    private static final String DEFAULT_RANK_CODE_MAP = "rank_codes.csv";
    /** The default set of synonym identifiers. TODO allow overrides */
    private static final String DEFAULT_NONEMCLATURAL_STATUS_CODE_MAP = "nomenclatural_status_codes.csv";

    private Map<String, NomenclaturalCode> codeMap;
    private Map<String, TaxonomicStatus> taxonomicStatusMap;
    private Map<String, SynonymType> synonymMap;
    private Map<String, RankType> rankMap;
    private Map<String, NomenclaturalStatus> nomenclaturalStatusMap;
    private NomStatusParser nomStatusParser;
    private SciNameNormalizer nameNormalizer;
    private AuthorComparator authorComparator;

    public ALANameAnalyser() {
        this.nomStatusParser = NomStatusParser.getInstance();
        this.nameNormalizer = new SciNameNormalizer();
        this.authorComparator = AuthorComparator.createWithAuthormap();
        this.buildCodeMap();
        this.buildTaxonomicStatusMap();
        this.buildSynonymMap();
        this.buildRankMap();
        this.buildNomenclaturalStatusMap();
    }

    /**
     * Load a set of additional terms for a controlled vocabulary.
     *
     * @param resource The resource path (resolved against the class)
     * @param map The map to load
     * @param clazz The vocabulary class
     * @param <T> The vocabulary class
     */
    protected <T extends Enum<T>> void loadCsv(String resource, Map<String, T> map, Class<T> clazz) {
        try {
            CSVReader reader = new CSVReader(new InputStreamReader(this.getClass().getResourceAsStream(resource), "UTF-8"), ',', '"', 1);
            String[] next;
            while ((next = reader.readNext()) != null) {
                String label = next[0];
                T value = Enum.valueOf(clazz, next[1]);
                map.put(label.toUpperCase().trim(), value);
            }
        } catch (Exception ex) {
            LOGGER.error("Unable to build map for " + clazz, ex);
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
    protected void buildTaxonomicStatusMap() {
        this.taxonomicStatusMap = new HashMap<>(64);
        for (TaxonomicStatus s: TaxonomicStatus.values())
            this.taxonomicStatusMap.put(s.name().toUpperCase().trim(), s);
        this.loadCsv(DEFAULT_TAXONOMIC_STATUS_CODE_MAP, this.taxonomicStatusMap, TaxonomicStatus.class);
    }

    /**
     * Build a synonym map.
     */
    protected void buildSynonymMap() {
        this.synonymMap = new HashMap<>(64);
        for (SynonymType s: SynonymType.values())
            for (String label: s.getLabels())
                this.synonymMap.put(label.toUpperCase().trim(), s);
        this.loadCsv(DEFAULT_SYNONYM_CODE_MAP, this.synonymMap, SynonymType.class);
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
        this.loadCsv(DEFAULT_SYNONYM_CODE_MAP, this.synonymMap, SynonymType.class);
    }

    /**
     * Canonicalise the nomenclatural code.
     *
     * @param code The code name
     *
     * @return The mapped code
     */
    @Override
    public NomenclaturalCode canonicaliseCode(String code) {
        code = code.toUpperCase().trim();
        NomenclaturalCode nc = this.codeMap.get(code);
        if (nc == null)
            throw new IllegalArgumentException("Inavlid nomenclatural code acronym " + code);
        return nc;
    }

    /**
     * Canonicalise the taxonomic status.
     *
     * @param taxonomicStatus The taxonomic status term
     *
     * @return The mapped status
     */
    @Override
    public TaxonomicStatus canonicaliseTaxonomicStatus(String taxonomicStatus) {
        taxonomicStatus = taxonomicStatus.toUpperCase().trim();
        TaxonomicStatus status = this.taxonomicStatusMap.get(taxonomicStatus);
        if ((taxonomicStatus != null && !taxonomicStatus.isEmpty()) && status == null)
            throw new IllegalArgumentException("Invalid taxonomicStatus string " + taxonomicStatus);
        return status;
    }

    /**
     * Canonicalise the synonym type.
     *
     * @param taxonomicStatus The taxonomic status term
     *
     * @return The mapped synonym type
     */
    @Override
    public SynonymType canonicaliseSynonymType(String taxonomicStatus) {
        taxonomicStatus = taxonomicStatus.toUpperCase().trim();
        TaxonomicStatus status = this.taxonomicStatusMap.get(taxonomicStatus);
        SynonymType type = this.synonymMap.get(taxonomicStatus);
        if ((taxonomicStatus != null && !taxonomicStatus.isEmpty()) && (status != null && status.isSynonym() && type == null))
            throw new IllegalArgumentException("Invalid taxonomicStatus string " + taxonomicStatus);
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
        rank = rank.toUpperCase().trim();
        RankType rankType = this.rankMap.get(rank);
        if ((rank != null && !rank.isEmpty()) && rankType == null)
            throw new IllegalArgumentException("Invalid rank string " + rank);
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
        ParseResult<NomenclaturalStatus> parsed = this.nomStatusParser.parse(nomenclaturalStatus);
        if (parsed.isSuccessful())
            return parsed.getPayload();
        nomenclaturalStatus = nomenclaturalStatus.toUpperCase().trim();
        NomenclaturalStatus status = this.nomenclaturalStatusMap.get(nomenclaturalStatus);
        if ((nomenclaturalStatus != null && !nomenclaturalStatus.isEmpty()) && status == null)
            throw new IllegalArgumentException("Invalid nomenclatural status string " + nomenclaturalStatus);
        return status;
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
        if (author == null) {
            if (parsedName.isRecombination()) {
                author = parsedName.getBracketAuthorship();
                if (parsedName.getBracketYear() != null)
                    author = author + ", " + parsedName.getBracketYear();
                author = "(" + author + ")";
            } else {
                author = parsedName.getAuthorship();
                if (parsedName.getYear() != null)
                    author = author + ", " + parsedName.getYear();
            }
        }
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

        if ((cmp = key1.getCode().compareTo(key2.getCode())) != 0)
            return cmp;
        if ((cmp = key1.getScientificName().compareTo(key2.getScientificName())) != 0)
            return cmp;
        if (key1.getScientificNameAuthorship() == null && key2.getScientificNameAuthorship() == null)
            return 0;
        if (key1.getScientificNameAuthorship() == null && key2.getScientificNameAuthorship() != null)
            return -1;
        if (key1.getScientificNameAuthorship() != null && key2.getScientificNameAuthorship() == null)
            return 1;
        if (authorComparator.compare(key1.getScientificNameAuthorship(), null, key2.getScientificNameAuthorship(), null) == Equality.EQUAL)
            return 0;
        return key1.getScientificNameAuthorship().compareTo(key2.getScientificNameAuthorship());
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
        int hash = key1.getCode().hashCode();
        hash = hash * 31 + key1.getScientificName().hashCode();
        hash = hash * 31 + (key1.getScientificNameAuthorship() == null ? 0 : 1181);
        return hash;
    }
}
