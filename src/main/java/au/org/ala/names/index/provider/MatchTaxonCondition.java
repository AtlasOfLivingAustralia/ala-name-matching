package au.org.ala.names.index.provider;

import au.org.ala.names.index.NameProvider;
import au.org.ala.names.index.TaxonConceptInstance;
import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonomicType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.model.Equality;
import org.gbif.checklistbank.utils.SciNameNormalizer;

import java.util.regex.Pattern;

/**
 * Match against possible criteria in a taxon instance.
 * <p>
 * Any non-null elements will be compared.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class MatchTaxonCondition extends TaxonCondition {
    /** Shared author comparator */
    private static AuthorComparator AUTHOR_COMPARATOR = AuthorComparator.createWithAuthormap();

    /** Compare nomenclatural code */
    private NomenclaturalCode nomenclaturalCode;
    /** Source dataset */
    private String datasetID;
    /** Compare scientific name */
    private String scientificName;
    /** The match name */
    @JsonIgnore
    private String matchScientificName;
    /** The match pattern */
    @JsonIgnore
    private Pattern patternScientificName;
    /** Compare scientific name authorship */
    private String scientificNameAuthorship;
    /** The match name */
    @JsonIgnore
    private String matchScientificNameAuthorship;
    /** The match pattern */
    @JsonIgnore
    private Pattern patternScientificNameAuthorship;
    /** Ignore case and spaces when comparing names and authors */
    private NameMatchType matchType;
    /** Compare taxonomic status */
    private TaxonomicType taxonomicStatus;
    /** Compare nomenclatural status */
    private NomenclaturalStatus nomenclaturalStatus;
    /** Compare name type */
    private NameType nameType;
    /** Compare rank */
    private RankType taxonRank;
    /** Compare year */
    private String year;

    /**
     * Default, empty constructor
     */
    public MatchTaxonCondition() {
    }

    public NomenclaturalCode getNomenclaturalCode() {
        return nomenclaturalCode;
    }

    public void setNomenclaturalCode(NomenclaturalCode nomenclaturalCode) {
        this.nomenclaturalCode = nomenclaturalCode;
    }

    public String getDatasetID() {
        return datasetID;
    }

    public void setDatasetID(String datasetID) {
        this.datasetID = datasetID;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
        this.matchScientificName = null;
        this.patternScientificName = null;
    }

    public String getScientificNameAuthorship() {
        return scientificNameAuthorship;
    }

    public void setScientificNameAuthorship(String scientificNameAuthorship) {
        this.scientificNameAuthorship = scientificNameAuthorship;
        this.matchScientificNameAuthorship = null;
        this.patternScientificNameAuthorship = null;
    }

    public NameMatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(NameMatchType matchType) {
        this.matchType = matchType;
        this.matchScientificName = null;
        this.patternScientificName = null;
        this.matchScientificNameAuthorship = null;
        this.patternScientificNameAuthorship = null;
    }

    public TaxonomicType getTaxonomicStatus() {
        return taxonomicStatus;
    }

    public void setTaxonomicStatus(TaxonomicType taxonomicStatus) {
        this.taxonomicStatus = taxonomicStatus;
    }

    public NomenclaturalStatus getNomenclaturalStatus() {
        return nomenclaturalStatus;
    }

    public void setNomenclaturalStatus(NomenclaturalStatus nomenclaturalStatus) {
        this.nomenclaturalStatus = nomenclaturalStatus;
    }

    public NameType getNameType() {
        return nameType;
    }

    public void setNameType(NameType nameType) {
        this.nameType = nameType;
    }

    public RankType getTaxonRank() {
        return taxonRank;
    }

    public void setTaxonRank(RankType taxonRank) {
        this.taxonRank = taxonRank;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    /**
     * Match the taxon instance against the supplied conditions.
     *
     * @param instance The instance to match
     *
     * @return True if the instance matches all the supplied conditions
     */
    @Override
    public boolean match(TaxonConceptInstance instance) {
        if (this.nomenclaturalCode != null && this.nomenclaturalCode != instance.getCode())
            return false;
        if (this.datasetID != null && (instance.getProvider() == null || !this.datasetID.equals(instance.getProvider().getId())))
            return false;
        if (!this.matchScientificName(instance.getScientificName()))
            return false;
        if (!this.matchScientificNameAuthorship(instance.getScientificNameAuthorship()))
            return false;
        if (this.taxonomicStatus != null && this.taxonomicStatus != instance.getTaxonomicStatus())
            return false;
        if (this.nomenclaturalStatus != null && (instance.getStatus() == null || !instance.getStatus().contains(this.nomenclaturalStatus)))
            return false;
        if (this.nameType != null && (instance.getTaxonConcept() == null || this.nameType != instance.getTaxonConcept().getKey().getType()))
            return false;
        if (this.taxonRank != null && this.taxonRank != instance.getRank())
            return false;
        if (this.year != null && !this.year.equals(instance.getYear()))
            return false;
        return true;
    }

    /**
     * Try to match a name against a supplied scientific name, using the match type.
     *
     * @param name The name
     *
     * @return True if the name matches.
     */
    private boolean matchScientificName(String name) {
        if (this.scientificName == null)
            return true;
        if (name == null)
            return false;
        name = name.trim();
        NameMatchType type = this.matchType != null ? this.matchType : NameMatchType.EXACT;
        switch (type) {
            case INSENSITIVE:
                name = name.toUpperCase().replaceAll("\\s+", " ");
                if (this.matchScientificName == null)
                    this.matchScientificName = this.scientificName.toUpperCase().replaceAll("\\s+", " ").trim();
                return this.matchScientificName.equals(name);
            case NORMALISED:
                name = SciNameNormalizer.normalizeAll(name).toUpperCase();
                if (this.matchScientificName == null)
                    this.matchScientificName = SciNameNormalizer.normalizeAll(this.scientificName).toUpperCase();
                return this.matchScientificName.equals(name);
            case REGEX:
                if (this.patternScientificName == null)
                    this.patternScientificName = Pattern.compile(this.scientificName);
                return this.patternScientificName.matcher(name).matches();
            default:
                if (this.matchScientificName == null)
                    this.matchScientificName = this.scientificName.trim();
                return this.matchScientificName.equals(name);

        }
    }

    /**
     * Try to match a name against a supplied scientific name, using the match type.
     *
     * @param author The author
     *
     * @return True if the name matches.
     */
    private boolean matchScientificNameAuthorship(String author) {
        if (this.scientificNameAuthorship == null)
            return true;
        if (author == null)
            return false;
        author = author.trim();
        NameMatchType type = this.matchType != null ? this.matchType : NameMatchType.EXACT;
        switch (type) {
            case INSENSITIVE:
                author = author.toUpperCase().replaceAll("\\s+", " ");
                if (this.matchScientificNameAuthorship == null)
                    this.matchScientificNameAuthorship = this.scientificNameAuthorship.toUpperCase().replaceAll("\\s+", " ").trim();
                return this.matchScientificNameAuthorship.equals(author);
            case NORMALISED:
                synchronized (AUTHOR_COMPARATOR) {
                    if (this.matchScientificNameAuthorship == null)
                        this.matchScientificNameAuthorship = this.scientificNameAuthorship.trim();
                    return AUTHOR_COMPARATOR.compare(this.matchScientificNameAuthorship, null, author, null) == Equality.EQUAL;
                }
            case REGEX:
                if (this.patternScientificNameAuthorship == null)
                    this.patternScientificNameAuthorship = Pattern.compile(this.scientificNameAuthorship);
                return this.patternScientificNameAuthorship.matcher(author).matches();
            default:
                if (this.matchScientificNameAuthorship == null)
                    this.matchScientificNameAuthorship = this.scientificNameAuthorship.trim();
                return this.matchScientificNameAuthorship.equals(author);

        }
    }

}
