/*
 * Copyright (c) 2023 Atlas of Living Australia
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

import au.org.ala.names.model.VernacularType;
import au.org.ala.names.util.DwcaWriter;
import au.org.ala.vocab.ALATerm;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A description of a vernacular name
 */
public class VernacularName {
    /**
     * The terms to use if writing a
     */
    public static final List<Term> REQUIRED_TERMS = Collections.unmodifiableList(Arrays.asList(
            ALATerm.nameID,
            DwcTerm.vernacularName,
            DwcTerm.datasetID,
            ALATerm.status,
            GbifTerm.isPreferredName,
            DcTerm.language,
            DwcTerm.locationID,
            DwcTerm.locality,
            DwcTerm.countryCode,
            DwcTerm.taxonRemarks,
            DcTerm.provenance,
            ALATerm.priority
    ));

    /** The unique identifier for the vernacular name */
    private String id;
    /** The vernacular name identifier */
    private String nameID;
    /** The vernacular name */
    private String vernacularName;
    /** The vernacular name type/status */
    private VernacularType status;
    /** The preferred flag */
    private boolean preferredName;
    /** The language code */
    private String language;
    /** The locality */
    private Location locality;
    /** The provider of the name */
    private NameProvider provider;
    /** Any taxon remarks. This may be added to as processing occurs */
    private List<String> taxonRemarks;
    /** * Any provenance information */
    private List<String> provenance;
    /** The associated taxon concept instance */
    private TaxonConceptInstance instance;
    /** The name score */
    private Integer score;
    /** Is this name forbidden for some reason? */
    private boolean forbidden;

    /**
     * Create a new vernacular name.
     *
     * @param nameID The name identifier
     * @param vernacularName The vernacular name
     * @param status The name status
     * @param preferredName Is this a preferred name
     * @param language The language code
     * @param locality The locality
     * @param provider The name provider
     * @param taxonRemarks Remakrs
     * @param provenance The provenance
     * @param instance Any associated taxon concept instance
     * @param score The score, if supplied
     * @param forbidden A forbdden flag
     */
    public VernacularName(String nameID, String vernacularName, VernacularType status, boolean preferredName, String language, Location locality, NameProvider provider, List<String> taxonRemarks, List<String> provenance, TaxonConceptInstance instance, Integer score, boolean forbidden) {
        this.id = UUID.randomUUID().toString();
        this.nameID = nameID;
        this.vernacularName = vernacularName;
        this.status = status;
        this.preferredName = preferredName;
        this.language = language;
        this.locality = locality;
        this.provider = provider;
        this.taxonRemarks = taxonRemarks;
        this.provenance = provenance;
        this.instance = instance;
        this.score = score;
        this.forbidden = forbidden;
    }

    /**
     * Create a copy of this name with a new, UUID identifier.
     *
     * @return The new vernacular name
     */
    public VernacularName withNewID() {
        String nameID = UUID.randomUUID().toString();
        return new VernacularName(
                nameID,
                this.vernacularName,
                this.status,
                this.preferredName,
                this.language,
                this.locality,
                this.provider,
                this.taxonRemarks == null ? null : new ArrayList<>(this.taxonRemarks),
                this.provenance == null ? null : new ArrayList<>(this.provenance),
                this.instance,
                this.score,
                this.forbidden
        );
    }

    /**
     * Get the unique identifier for the name.
     *
     * @return The unique identifier
     */
    public String getId() {
        return this.id;
    }

    /**
     * Get the name identifier
     *
     * @return The name identifier
     */
    public String getNameID() {
        return nameID;
    }

    /**
     * Get the vernacular name
     *
     * @return The vernacular name
     */
    public String getVernacularName() {
        return vernacularName;
    }

    /**
     * Get the vernacular name status
     *
     * @return The vernacular name status
     */
    public VernacularType getStatus() {
        return status;
    }

    /**
     * Is this a marked preferred name?
     *
     * @return True if the name is marked as preferred
     */
    public boolean isPreferredName() {
        return preferredName;
    }

    /**
     * Get the name language
     *
     * @return The language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Get the name locality
     *
     * @return The locality
     */
    public Location getLocality() {
        return locality;
    }

    /**
     * Get the name provider
     *
     * @return The proivider
     */
    public NameProvider getProvider() {
        return provider;
    }

    public void setProvider(NameProvider provider) {
        this.provider = provider;
    }

    /**
     * Get any taxon remarks
     *
     * @return The taxon remarks
     */
    public List<String> getTaxonRemarks() {
        return taxonRemarks;
    }

    /**
     * Add a note top the taxon remarks
     *
     * @param remark The remark
     */
    public void addTaxonRemark(String remark) {
        if (this.taxonRemarks == null)
            this.taxonRemarks = new ArrayList<>();
        this.taxonRemarks.add(remark);
    }

    /**
     * Get the provenance statement
     *
     * @return The provenance statement
     */
    public List<String> getProvenance() {
        return provenance;
    }

    /**
     * Add a note top the provenance
     *
     * @param remark The remark
     */
    public void addProvenance(String remark) {
        if (this.provenance == null)
            this.provenance = new ArrayList<>();
        this.provenance.add(remark);
    }

    /**
     * Get the attached taxon concept instance
     *
     * @return The taxon concept instance this vernacular name is attached to
     */
    public TaxonConceptInstance getInstance() {
        return instance;
    }

    /**
     * Set the attached taxon concept instance.
     *
     * @param instance The new taxon concept instance
     */
    public void setInstance(TaxonConceptInstance instance) {
        this.instance = instance;
    }

    /**
     * Has this name been assigned?
     *
     * @return True if the name has an attached instance and the instance knows about the name
     */
    public boolean isAssigned() {
        return this.instance != null
                && this.instance.getVernacularNames() != null
                && this.instance.getVernacularNames().contains(this);
    }


    /**
     * Get the base score for this name.
     * <p>
     * The score is derived from the provider
     * </p>
     *
     * @return The base score
     */
    public int getBaseScore() {
       return this.provider.computeBaseScore(this);
    }

    /**
     * Get the vernacular name score.
     * <p>
     * This is computed lazily from
     * </p>
     *
     * @return The score
     */
    public Integer getScore() {
        if (this.score == null)
            this.score = this.provider.computeScore(this);
        return score;
    }

    /**
     * Is this vernacular name forbidden (not to be used)?
     *
     * @return True if the name is forbidden
     */
    public boolean isForbidden() {
        return forbidden;
    }

    /**
     * Set the forbidden flag.
     *
     * @param forbidden
     */
    public void setForbidden(boolean forbidden) {
        this.forbidden = forbidden;
    }

    public Map<Term, String> asMap(Taxonomy taxonomy) throws IOException {
        Map<Term, String> map = taxonomy.getIndexValue(GbifTerm.VernacularName, "id", this.getId());
        map.put(ALATerm.nameID, this.nameID);
        map.put(DwcTerm.datasetID, this.provider == null ? null : this.provider.getId());
        map.put(DwcTerm.vernacularName, this.vernacularName);
        map.put(ALATerm.status, this.status == null ? null : this.status.getTerm());
        map.put(GbifTerm.isPreferredName, Boolean.toString(this.preferredName));
        map.put(DcTerm.language, this.language);
        map.put(DwcTerm.locationID, this.locality == null ? null : this.locality.getLocationID());
        map.put(DwcTerm.locality, this.locality == null ? null : this.locality.getLocality());
        map.put(DwcTerm.taxonRemarks, this.taxonRemarks == null ? null : this.taxonRemarks.stream().collect(Collectors.joining("|")));
        map.put(DcTerm.provenance, this.provenance == null ? null : this.provenance.stream().collect(Collectors.joining("|")));
        map.put(ALATerm.priority, this.score == null ? null : Integer.toString(this.score));
        return map;
    }
}
