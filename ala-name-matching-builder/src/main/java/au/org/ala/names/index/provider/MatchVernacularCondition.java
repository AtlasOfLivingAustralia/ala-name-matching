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

package au.org.ala.names.index.provider;

import au.org.ala.names.index.*;
import au.org.ala.names.model.VernacularType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.gbif.utils.text.StringUtils;

import java.util.regex.Pattern;

/**
 * Match against possible criteria in a vernacular name
 * <p>
 * Any non-null elements will be compared.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2023 Atlas of Living Australia
 */
public class MatchVernacularCondition extends TaxonCondition {
    /** Source dataset */
    private String datasetID;
    /** Compare vernacular name */
    private String vernacularName;
    /** The match name */
    @JsonIgnore
    private String matchVernacularName;
    /** The match pattern */
    @JsonIgnore
    private Pattern patternVernacularName;
    /** Ignore case and spaces when comparing names and authors */
    private NameMatchType matchType;
    /** Compare vernacular status */
    private VernacularType status;
    /** COmpared preferred status */
    private Boolean isPreferredName;
    /** Compare language */
    private String language;
    /** The match language */
    @JsonIgnore
    private String matchLanguage;
    /** The match pattern */
    @JsonIgnore
    private Pattern patternLanguage;
    /** The locality (could be a locationID, locality, countryCode etc) */
    private String locality;

    /**
     * Default, empty constructor
     */
    public MatchVernacularCondition() {
    }


    public String getDatasetID() {
        return datasetID;
    }

    public void setDatasetID(String datasetID) {
        this.datasetID = datasetID;
    }

    public String getVernacularName() {
        return vernacularName;
    }

    public void setVernacularName(String vernacularName) {
        this.vernacularName = vernacularName;
        this.matchVernacularName = null;
        this.patternVernacularName = null;
    }

    public NameMatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(NameMatchType matchType) {
        this.matchType = matchType;
        this.matchVernacularName = null;
        this.patternVernacularName = null;
        this.matchLanguage = null;
        this.patternLanguage = null;
    }

    public VernacularType getStatus() {
        return status;
    }

    public void setStatus(VernacularType status) {
        this.status = status;
    }

    public Boolean getPreferredName() {
        return isPreferredName;
    }

    public void setPreferredName(Boolean preferredName) {
        isPreferredName = preferredName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
        this.matchLanguage = null;
        this.patternLanguage = null;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    /**
     * Does this condition match an instance?
     *
     * @param instance The instance to match
     * @param key      The associated name key
     * @param provider The provider for match context
     *
     * @return Atways false
     *
     * @see MatchTaxonCondition
     */
    @Override
    public boolean match(TaxonConceptInstance instance, NameKey key, NameProvider provider) {
        return false;
    }

    /**
     * Match the taxon instance against the supplied conditions.
     *
     * @param name The vernacular name to match
     * @param provider The provider for match context
     *
     * @return True if the name matches all the supplied conditions
     */
    @Override
    public boolean match(VernacularName name, NameProvider provider) {
        if (this.datasetID != null && (name.getProvider() == null || !this.datasetID.equals(name.getProvider().getId())))
            return false;
        if (!this.matchVernacularName(name.getVernacularName()))
            return false;
         if (this.status != null && this.status != name.getStatus())
            return false;
         if (this.isPreferredName != null && this.isPreferredName != name.isPreferredName())
             return false;
        if (!this.matchLanguage(name.getLanguage()))
            return false;
        if (!this.matchLocality(name.getLocality(), provider))
            return false;
        return true;
    }

    /**
     * Try to match a name against a supplied vernacular name, using the match type.
     *
     * @param name The name
     *
     * @return True if the name matches.
     */
    private boolean matchVernacularName(String name) {
        if (this.vernacularName == null)
            return true;
        if (name == null)
            return false;
        name = name.trim();
        NameMatchType type = this.matchType != null ? this.matchType : NameMatchType.EXACT;
        switch (type) {
            case INSENSITIVE:
                name = name.toUpperCase().replaceAll("\\s+", " ");
                if (this.matchVernacularName == null)
                    this.matchVernacularName = this.vernacularName.toUpperCase().replaceAll("\\s+", " ").trim();
                return this.matchVernacularName.equals(name);
            case NORMALISED:
                name = StringUtils.foldToAscii(name).toUpperCase().replaceAll("\\s+", " ");
                if (this.matchVernacularName == null)
                    this.matchVernacularName = StringUtils.foldToAscii(this.vernacularName).toUpperCase().replaceAll("\\s+", " ");;
                return this.matchVernacularName.equals(name);
            case REGEX:
                if (this.patternVernacularName == null)
                    this.patternVernacularName = Pattern.compile(this.vernacularName, Pattern.CASE_INSENSITIVE);
                return this.patternVernacularName.matcher(name).matches();
            default:
                if (this.matchVernacularName == null)
                    this.matchVernacularName = this.vernacularName.trim();
                return this.matchVernacularName.equals(name);

        }
    }


    /**
     * Try to match a language against a supplied language, using the match type.
     *
     * @param name The name
     *
     * @return True if the name matches.
     */
    private boolean matchLanguage(String name) {
        if (this.language == null)
            return true;
        if (name == null)
            return false;
        name = name.trim();
        NameMatchType type = this.matchType != null ? this.matchType : NameMatchType.EXACT;
        switch (type) {
            case INSENSITIVE:
            case NORMALISED:
                name = name.toUpperCase().replaceAll("\\s+", " ");
                if (this.matchLanguage == null)
                    this.matchLanguage = this.language.toUpperCase().replaceAll("\\s+", " ").trim();
                return this.matchLanguage.equals(name);
            case REGEX:
                if (this.patternLanguage == null)
                    this.patternLanguage = Pattern.compile(this.language, Pattern.CASE_INSENSITIVE);
                return this.patternLanguage.matcher(name).matches();
            default:
                if (this.matchLanguage == null)
                    this.matchLanguage = this.language.trim();
                return this.matchLanguage.equals(name);

        }
    }

    private boolean matchLocality(Location location, NameProvider provider) {
        if (this.locality == null)
            return true;
        if (location == null)
            return false;
        Location matchLocation = provider.findLocation(this.locality);
        if (matchLocation == null)
            throw new IllegalStateException("Can't find location for locality " + this.locality);
        return matchLocation.covers(location);
    }


    /**
     * Provide a string explanation of the condition
     *
     * @return The explanation
     */
    @Override
    public String explain() {
        StringBuilder builder = new StringBuilder();
        this.explain(builder, "datasetID", this.datasetID);
        this.explain(builder, "scientificName", this.vernacularName, this.matchType);
        this.explain(builder, "status", this.status);
        this.explain(builder, "isPreferredName", this.isPreferredName);
        this.explain(builder, "language", this.language, this.matchType);
        this.explain(builder, "locality", this.locality);
        return builder.toString();
    }
}
