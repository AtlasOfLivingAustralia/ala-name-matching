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

import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonFlag;
import au.org.ala.names.model.TaxonomicType;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.model.Equality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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
    private AuthorComparator authorComparator;

    /**
     * Constructor.
     *
     * @param authorComparator The author comparator to use
     * @param reporter The reporter to use
     */
    public NameAnalyser(AuthorComparator authorComparator, Reporter reporter) {
        this.authorComparator = authorComparator;
        this.reporter = reporter;
    }

    /**
     * Convienience method for testing.
     */
    public NameKey analyse(TaxonConceptInstance instance) {
        return this.analyse(instance.getCode(), instance.getScientificName(), instance.getScientificNameAuthorship(), instance.getRank(), instance.getTaxonomicStatus(), instance.getFlags(), false);
    }

    /**
     * Convienience method for testing.
     */
    public NameKey analyse(String code, String scientificName, String scientificNameAuthorship, String rank) {
        NomenclaturalClassifier canonicalCode = this.canonicaliseCode(code);
        RankType rankType = this.canonicaliseRank(rank);
        return this.analyse(canonicalCode, scientificName, scientificNameAuthorship, rankType, null, null, false);
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
     * @param taxonomicStatus The taxonomic status
     * @param loose This is from a loose source that may have names and authors mixed up and the like
     *
     * @return A suitable name key
     */
    abstract public NameKey analyse(@Nullable NomenclaturalClassifier code, String scientificName, @Nullable String scientificNameAuthorship, @Nullable RankType rankType, @Nullable TaxonomicType taxonomicStatus, @Nullable Set<TaxonFlag> flags, boolean loose);

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
    abstract public NomenclaturalClassifier canonicaliseCode(String code);

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
     * Canonicalise a supplied taxonomic flag
     *
     * @param flag The flag name
     *
     * @return The matching flag
     */
    abstract public TaxonFlag canonicaliseFlag(String flag);

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
     * Compare two keys.
     * <p>
     *     Comparison is equal if the codes, names and authors are equal.
     *     Name keys with fuzzy nomenclatural codes do not compare codes.
     *     Authorship equality is decided by a {@link AuthorComparator}
     *     which can get a wee bit complicated.
     * </p>
     *
     * @param key1 The first key to compare
     * @param key2 The second key to compare
     *
     * @return less than zero if key1 is less than ket2, greater than zero if key1 is greater than key2 and 0 for equality
     */
    public int compare(NameKey key1, NameKey key2) {
        int cmp;

        if (key1.getCode() == null && key2.getCode() != null)
            return -1;
        if (key1.getCode() != null && key2.getCode() == null)
            return 1;
        if (key1.getCode() != null && key2.getCode() != null) {
            if ((cmp = key1.getCode().compareTo(key2.getCode())) != 0)
                return cmp;
        }
        if ((cmp = key1.getScientificName().compareTo(key2.getScientificName())) != 0)
            return cmp;
        if ((cmp = key1.getRank().compareTo(key2.getRank())) != 0)
            return cmp;
        return this.compareAuthor(key1.getScientificNameAuthorship(), key2.getScientificNameAuthorship());
    }

    /**
     * Compare two author strings.
     * <p>
     * Use the GBIF {@link AuthorComparator} for equality
     * </p>
     *
     * @return Less than zero, zero or greater than zero based on lexical ordering, unless the author comparated declares two abbreviated names to be equal
     */
    public int compareAuthor(String author1, String author2) {
        if (author1 == null && author2 == null)
            return 0;
        if (author1 == null && author2 != null)
            return -1;
        if (author1 != null && author2 == null)
            return 1;
        if (authorComparator.compare(author1, null, author2, null) == Equality.EQUAL)
            return 0;
        return author1.compareTo(author2);
    }

    /**
     * Compute a hash code for a key.
     * <p>
     *     Based on hashing for the code and name. Only author presence/absence is calculated.
     * </p>
     * @param key1 The key
     *
     * @return The hash code
     */
    public int hashCode(NameKey key1) {
        int hash = key1.getCode() != null ? key1.getCode().hashCode() : 1181;
        hash = hash * 31 + key1.getScientificName().hashCode();
        hash = hash * 31 + (key1.getScientificNameAuthorship() == null ? 0 : 5659);
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void report(IssueType type, String code, String taxonID, String name, String... args) {
        if (this.reporter != null)
            this.reporter.report(type, code, taxonID, name, args);
        else
            logger.warn("Report " + type.name() + " code=" + code + " args=" + Arrays.toString(args));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void report(IssueType type, String code, TaxonomicElement main, List<? extends TaxonomicElement> associated) {
        if (this.reporter != null)
            this.reporter.report(type, code, main, associated);
        else
            logger.warn("Report " + type.name() + " code=" + code + " main=" + main.toString() + " associated=" + associated);
    }
}
