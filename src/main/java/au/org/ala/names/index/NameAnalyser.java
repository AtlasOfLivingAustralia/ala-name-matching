package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonomicType;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Arrays;
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
abstract public class NameAnalyser implements Comparator<NameKey>, Reporter {
    private static final Logger logger = LoggerFactory.getLogger(NameAnalyser.class);

    /** Report possible informal names; can be used for debugging source data */
    protected static final boolean REPORT_INFORMAL = true;

    private Reporter reporter;

    public NameAnalyser() {
    }

    /**
     * Convienience method for testing.
     */
    public NameKey analyse(TaxonConceptInstance instance) {
        return this.analyse(instance.getCode(), instance.getScientificName(), instance.getScientificNameAuthorship(), instance.getRank(), false);
    }

    /**
     * Convienience method for testing.
     */
    public NameKey analyse(String code, String scientificName, String scientificNameAuthorship, String rank) {
        NomenclaturalCode canonicalCode = this.canonicaliseCode(code);
        RankType rankType = this.canonicaliseRank(rank);
        return this.analyse(canonicalCode, scientificName, scientificNameAuthorship, rankType, false);
    }

    /**
     * Analyse a name and return a matching name key.
     * <p>
     * Name keys are expected to canonicalise the provided names so that
     * we can link like name with like name.
     * </p>
     *
     * @param code The nomenclatural code
     * @param scientificName The scientific name
     * @param scientificNameAuthorship The authorship
     * @param rankType The taxon rank
     * @param loose This is from a loose source that may have names and authors mixed up and the like
     *
     * @return A suitable name key
     */
    abstract public NameKey analyse(@Nullable NomenclaturalCode code, String scientificName, @Nullable String scientificNameAuthorship, @Nullable RankType rankType, boolean loose);

    /**
     * Set the issue reporter.
     *
     * @param reporter The issue reporter
     */
    public void setReporter(Reporter reporter) {
        this.reporter = reporter;
    }

    /**
     * Canonicalise the nomenclatural code
     *
     * @param code The code name
     *
     * @return The canonicalised code
     */
    abstract public NomenclaturalCode canonicaliseCode(String code);

    /**
     * Canonicalise the taxonomic status
     *
     * @param taxonomicStatus The taxonomic status
     *
     * @return The canonicalised taxonomic status
     */
    abstract public TaxonomicType canonicaliseTaxonomicType(String taxonomicStatus);

    /**
     * Canonicalise the rank
     *
     * @param rank The rank name
     *
     * @return The canonicalised rank, if present
     */
    abstract public RankType canonicaliseRank(String rank);

    /**
     * Canonicalise a nomenclatural status
     *
     * @param nomenclaturalStatus The nomenclatural status
     *
     * @return The canonicalised nomenclatural status, if present
     */
    abstract public NomenclaturalStatus canonicaliseNomenclaturalStatus(String nomenclaturalStatus);

    /**
     * Test for an informal name.
     * <p>
     *
     * </p>
     * @param name
     * @return
     */
    abstract public boolean isInformal(String name);

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
     * {@inheritDoc}
     */
    @Override
    public void report(IssueType type, String code, String taxonID, String scientificName, String scientificNameAuthorship, String... args) {
        if (this.reporter != null)
            this.reporter.report(type, code, taxonID, scientificName, scientificNameAuthorship, args);
        else
            logger.warn("Report " + type.name() + " code=" + code + " args=" + Arrays.toString(args));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void report(IssueType type, String code, TaxonomicElement... elements) {
        if (this.reporter != null)
            this.reporter.report(type, code, elements);
        else
            logger.warn("Report " + type.name() + " code=" + code + " elements=" + Arrays.toString(elements));
    }

    /**
     * Compare author strings.
     * <p>
     * This usually compared equality across author abbreviations.
     *
     * </p>
     * @param author1 The first author string (may be null)
     * @param author2 The second author string (may be null)
     *
     * @return A value less than, greater than or equal to 0, similar to compare
     */
    public abstract int compareAuthor(String author1, String author2);
}
