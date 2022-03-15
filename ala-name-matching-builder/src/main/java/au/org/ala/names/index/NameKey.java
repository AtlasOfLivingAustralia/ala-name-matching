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

/**
 * A name key is a unique identifier for either a scientific name (code + name + rank) or
 * a taxonomic concept (code + name + authorship + rank)
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2017 CSIRO
 */

import au.org.ala.names.model.TaxonFlag;
import au.org.ala.names.model.RankType;
import org.gbif.api.vocabulary.NameType;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class NameKey implements Comparable<NameKey> {
    /** The analyser to use for comparisons */
    private NameAnalyser analyser;
    /** The nomenclatural code for scientific names */
    private NomenclaturalClassifier code;
    /** The scientific name */
    private String scientificName;
    /** The authorship */
    private String scientificNameAuthorship;
    /** The taxon rank */
    private RankType rank;
    /** The name type */
    private NameType type;
    /** Special case flags for a name */
    @Nullable
    private Set<TaxonFlag> flags;

    /**
     * Construct a name key
     *
     * @param analyser The source analyser
     * @param code The nomenclatural code
     * @param scientificName The normalised scientific name
     * @param scientificNameAuthorship The normalised authorship, for taxon concepts (null for names)
     * @param rank The rank
     * @param type The type of name
     * @param flags Any name flags (null for none)
     */
    public NameKey(NameAnalyser analyser, NomenclaturalClassifier code, String scientificName, String scientificNameAuthorship, RankType rank, NameType type, @Nullable Set<TaxonFlag> flags) {
        this.analyser = analyser;
        this.code = code;
        this.scientificName = scientificName;
        this.scientificNameAuthorship = scientificNameAuthorship;
        this.rank = rank;
        this.type = type;
        this.flags = flags;
    }

    /**
     * Get the name analyser for this key.
     * <p>
     * This is, essentially the analyser used to build the key
     * </p>
     *
     * @return The name analyser
     */
    public NameAnalyser getAnalyser() {
        return analyser;
    }

    /**
     * Get the normenclatural code for this key.
     *
     * @return The normenclatural code
     */
    public NomenclaturalClassifier getCode() {
        return code;
    }

    /**
     * Get the normalised scientific name
     *
     * @return The scientific name
     */
    public String getScientificName() {
        return scientificName;
    }

    /**
     * Get the normalised scientific name authorship
     */
    public String getScientificNameAuthorship() {
        return scientificNameAuthorship;
    }

    /**
     * Get the rank of the name
     */
    public RankType getRank() {
        return rank;
    }

    /**
     * Get the type of name
     *
     * @return
     */
    public NameType getType() {
        return type;
    }

    /**
     * Get the name flags
     *
     * @return Any name flags for this name, null for none
     */
    @Nullable
    public Set<TaxonFlag> getFlags() {
        return this.flags;
    }

    /**
     * Check to see if this key has a flag.
     * @param flag
     * @return
     */
    public boolean hasFlag(TaxonFlag flag) {
        return this.flags != null && this.flags.contains(flag);
    }

    /**
     * Is this name an autonym?
     *
     * @return True if the name is an autonym (and therefore doesn't have an author
     */
    public boolean isAutonym() {
        return this.hasFlag(TaxonFlag.AUTONYM);
    }

    /**
     * Is this an unauthored name, meaning that it does not have a scientific name author?
     *
     * @return True if the name key is unauthored
     *
     * @see #isAutonym()
     */
    public boolean isUnauthored() {
        return this.scientificNameAuthorship == null;
    }

    /**
     * Is this an uncoded name, meaning that it does not have a nomenclatural code to accurately
     * distinguish between homonyms?
     *
     * @return True if the name key is uncoded
     */
    public boolean isUncoded() {
        return this.code == null;
    }


    /**
     * Is this an unranked name, either because there is no rank or the rank is explicitly {@link RankType#UNRANKED}
     *
     * @return True if the name key is unranked
     */
    public boolean isUnranked() {
        return this.rank == null || this.rank == RankType.UNRANKED;
    }

    /**
     * Is this a formaal name key, meaning that the name type represents something like a scientific name.
     *
     * @return True if this represents a formal name
     */
    public boolean isFormal() {
        return this.code != null && this.type != null && (this.type == NameType.SCIENTIFIC || this.type == NameType.HYBRID || this.type == NameType.CULTIVAR || this.type == NameType.VIRUS);
    }


    /**
     * Compare for equality with another name key
     * <p>
     * Name keys are equal if the analyser says they are
     * </p>
     *
     * @param o The other object
     *
     * @return True if the name keys are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NameKey nameKey = (NameKey) o;
        return this.analyser.compare(this, nameKey) == 0;
    }

    /**
     * Compute a has code for the name key
     *
     * @return A hash code from the analyser
     */
    @Override
    public int hashCode() {
        return this.analyser.hashCode(this);
    }

    /**
     * Convert a full taxon key into a name-only key.
     *
     * @return A name key without authorship
     */
    public NameKey toNameKey() {
        if (this.scientificNameAuthorship == null)
            return this;
        Set<TaxonFlag> fl = this.flags;
        if (fl != null && fl.contains(TaxonFlag.AUTONYM)) {
            fl = new HashSet<>(this.flags);
            fl.remove(TaxonFlag.AUTONYM);
            fl = fl.isEmpty() ? null : fl;

        }
        return new NameKey(this.analyser, this.code, this.scientificName, null, this.rank, this.type, fl);
    }

    /**
     * Convert a full taxon key into an unranked key.
     *
     * @return An unranked name key without authorship
     */
    public NameKey toUnrankedNameKey() {
        if (this.scientificNameAuthorship == null && this.rank == RankType.UNRANKED)
            return this;
        Set<TaxonFlag> fl = this.flags;
        if (fl != null && fl.contains(TaxonFlag.AUTONYM)) {
            fl = new HashSet<>(this.flags);
            fl.remove(TaxonFlag.AUTONYM);
            fl = fl.isEmpty() ? null : fl;

        }
        return new NameKey(this.analyser, this.code, this.scientificName, null, RankType.UNRANKED, this.type, fl);
    }

    /**
     * Convert a taxon key into an ranked key.
     *
     * @param rank The new rank
     *
     * @return An unranked name key without authorship
     */
    public NameKey toRankedNameKey(RankType rank) {
        if (this.rank == rank)
            return this;
        return new NameKey(this.analyser, this.code, this.scientificName, this.scientificNameAuthorship, rank, this.type, this.flags);
    }


    /**
     * Convert a full taxon key into an unranked and un-nomenclatural coded key.
     *
     * @return An unranked/uncoded name key without authorship
     */
    public NameKey toUncodedNameKey() {
        if (this.scientificNameAuthorship == null && this.rank == RankType.UNRANKED && this.code == null)
            return this;
        Set<TaxonFlag> fl = this.flags;
        if (fl != null && fl.contains(TaxonFlag.AUTONYM)) {
            fl = new HashSet<>(this.flags);
            fl.remove(TaxonFlag.AUTONYM);
            fl = fl.isEmpty() ? null : fl;

        }
        return new NameKey(this.analyser, null, this.scientificName, null, RankType.UNRANKED, this.type, fl);
    }

    @Override
    public String toString() {
        return "[" + (code != null ? code.getAcronym() : "") + ":" + scientificName + (scientificNameAuthorship != null ? "," + scientificNameAuthorship : "") + ":" + this.rank.getRank() + (this.flags != null ? ":" + this.flags : "") +  ']';
    }

    /**
     * Compare name keys.
     *
     * @param o The other key
     *
     * @return Less than, equal to or grater than zero for the key.
     */
    @Override
    public int compareTo(NameKey o) {
        return this.analyser.compare(this, o);
    }

    /**
     * Compare authorship
     *
     * @param author The author to compare with
     *
     * @return
     */
    public int compareAuthor(String author) {
        return this.analyser.compareAuthor(this.scientificNameAuthorship, author);
    }
}
