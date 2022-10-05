/*
 * Copyright (c) 2022 Atlas of Living Australia
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

import au.org.ala.names.util.DwcaWriter;
import org.gbif.api.vocabulary.LifeStage;
import org.gbif.api.vocabulary.OccurrenceStatus;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwca.record.Record;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dsitribution information associated with a taxon.
 *
 * @see org.gbif.dwc.terms.GbifTerm#Distribution
 */
public class Distribution {
    private static final Set<Term> PARSED_TERMS =  new HashSet<Term>(Arrays.asList(
            DwcTerm.datasetID,
                    DwcTerm.datasetName,
                    DwcTerm.locationID,
                    DwcTerm.locality,
                    DwcTerm.lifeStage,
                    DwcTerm.occurrenceStatus
    ));

    /** The provider of this distribution */
    private NameProvider provider;
    /** The location this distribution applies to */
    private Location location;
    /** The life stage this applies to */
    private LifeStage lifeStage;
    /** The occurrence status this applies to */
    private OccurrenceStatus occurrenceStatus;
    /** Other information */
    private Map<Term, String> additional;

    /**
     * Create a distribution
     *
     * @param provider The record provider
     * @param location The location
     * @param lifeStage The lifestage
     * @param occurrenceStatus The occurrence status
     * @param additional Any additional terms
     */
    public Distribution(NameProvider provider, Location location, LifeStage lifeStage, OccurrenceStatus occurrenceStatus, Map<Term, String> additional) {
        this.provider = provider;
        this.location = location;
        this.lifeStage = lifeStage;
        this.occurrenceStatus = occurrenceStatus;
        this.additional = additional;
    }

    /**
     * Construct from a DwCA record
     *
     * @param taxonomy The taxonomy to interpret locations etc.
     * @param record The record
     */
    public Distribution(Taxonomy taxonomy, Record record) {
        String datasetID = record.value(DwcTerm.datasetID);
        String datasetName = record.value(DwcTerm.datasetName);
        this.provider = taxonomy.resolveProvider(datasetID, datasetName);
        String locationID = record.value(DwcTerm.locationID);
        if (locationID != null) {
            this.location = taxonomy.resolveLocation(locationID);
        }
        String locality = record.value(DwcTerm.locality);
        if (this.location == null && locality != null) {
            this.location = taxonomy.resolveLocation(locality);
        }
        if (this.location == null)
            throw new IllegalArgumentException("Unrecognised locality " + locationID + "/" + locality);
        String lifeStage = record.value(DwcTerm.lifeStage);
        this.lifeStage = taxonomy.resolveLifeStage(lifeStage);
        String occurrenceStatus = record.value(DwcTerm.occurrenceStatus);
        this.occurrenceStatus = taxonomy.resolveOccurrenceStatus(occurrenceStatus);
        this.additional = record.terms().stream().filter(t -> !PARSED_TERMS.contains(t) && record.value(t) != null).collect(Collectors.toMap(t -> t, t -> record.value(t)));
    }

    public NameProvider getProvider() {
        return provider;
    }

    public Location getLocation() {
        return location;
    }

    public LifeStage getLifeStage() {
        return lifeStage;
    }

    public OccurrenceStatus getOccurrenceStatus() {
        return occurrenceStatus;
    }

    public Map<Term, String> getAdditional() {
        return additional;
    }

    /**
     * Write this distribution record to the writer.
     *
     * @param taxonomy
     * @param writer
     * @throws IOException
     */
    public void writeExtension(Taxonomy taxonomy, DwcaWriter writer) throws IOException {
        Map<Term, String> ext = new LinkedHashMap<>(); // Preserve term order
        ext.put(DwcTerm.locationID, this.location.getLocationID());
        ext.put(DwcTerm.locality, this.location.getLocality());
        ext.put(DwcTerm.lifeStage, this.lifeStage == null ? null : this.lifeStage.name());
        ext.put(DwcTerm.occurrenceStatus, this.occurrenceStatus == null ? null : this.occurrenceStatus.name());
        List<Term> terms = taxonomy.outputTerms(GbifTerm.Distribution);
        for (Term term: terms) {
            if (!ext.containsKey(term)) {
                ext.put(term, this.additional == null ? null : this.additional.get(term));
            }
        }
        if (!ext.isEmpty())
            writer.addExtensionRecord(GbifTerm.Distribution, ext);
    }

    /**
     * Does this distribution cover another distribution
     *
     * @param other The other distribution
     *
     * @return True if this distribution covers the same location and has a similar life stage and occurrence status
     */
    public boolean covers(Distribution other) {
        if (!this.location.covers(other.location))
            return false;
        if (this.lifeStage != null && other.lifeStage == null)
            return false;
        if (this.lifeStage != null && other.lifeStage != this.lifeStage)
            return false;
        if (this.occurrenceStatus != null && other.occurrenceStatus == null)
            return false;
        if (this.occurrenceStatus != null && other.occurrenceStatus != this.occurrenceStatus)
            return false;
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Distribution that = (Distribution) o;

        if (!provider.equals(that.provider)) return false;
        if (!location.equals(that.location)) return false;
        if (lifeStage != that.lifeStage) return false;
        return occurrenceStatus == that.occurrenceStatus;
    }

    @Override
    public int hashCode() {
        int result = provider.hashCode();
        result = 31 * result + location.hashCode();
        result = 31 * result + (lifeStage != null ? lifeStage.hashCode() : 0);
        result = 31 * result + (occurrenceStatus != null ? occurrenceStatus.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Distribution{" +
                provider.getId() +
                ", " + location +
                ", " + lifeStage +
                ", " + occurrenceStatus +
                '}';
    }
}
