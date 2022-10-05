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

import au.org.ala.vocab.ALATerm;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.*;

/**
 * Location information
 */
public class Location {
    /** The location identifier */
    private String locationID;
    /** The parent location identifier */
    private String parentLocationID;
    /** The parent location */
    private Location parent;
    /** The name of the locality */
    private String locality;
    /** The type of geography this represents */
    private String geographyType;

    /**
     * Empty constructor
     */
    protected Location() {
    }

    /**
     * Construct a location
     *
     * @param locationID The location identifier
     * @param parentLocationID The parent location identifier
     * @param locality The locality (name)
     * @param geographyType The type of geography
     */
    public Location(String locationID, String parentLocationID, String locality, String geographyType) {
        this.locationID = locationID;
        this.parentLocationID = parentLocationID;
        this.locality = locality;
        this.geographyType = geographyType;
    }

    /**
     * Get the location identifier
     *
     * @return The location identifier
     */
    public String getLocationID() {
        return locationID;
    }

    /**
     * Get the parent location identifier
     *
     * @return The parent location identifier
     */
    public String getParentLocationID() {
        return parentLocationID;
    }

    /**
     * Get the parent location
     *
     * @return The parent location or null for none
     *
     * @see #resolve(Map)
     */
    public Location getParent() {
        return parent;
    }

    /**
     * Get the location name
     *
     * @return The location name
     */
    public String getLocality() {
        return locality;
    }

    /**
     * Get the geography type
     *
     * @return The geography type
     */
    public String getGeographyType() {
        return geographyType;
    }

    /**
     * Resolve parent locations
     *
     * @param locations The location map
     *
     * @throws IllegalStateException if the parent location is not found
     */
    public void resolve(Map<String, Location> locations) {
        if (this.parentLocationID == null) {
            this.parent = null;
            return;
        }
        this.parent = locations.get(this.parentLocationID);
        if (this.parent == null)
            throw new IllegalStateException("Can't find parent location " + this.parentLocationID);
    }

    /**
     * Equality test.
     * <p>
     * Locations are equal if thet have the same location ID
     * </p>
     *
     * @param o The other object to test
     *
     * @return True if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Location location = (Location) o;

        return locationID.equals(location.locationID);
    }

    /**
     * Hash code derived from location ID
     *
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return locationID.hashCode();
    }

    @Override
    public String toString() {
        return "Location{" + this.locationID + ", " + locality + "}";
    }

    /**
     * Does this location cover another location?
     *
     * @param other The other location
     *
     * @return True if this location represents a parent or equal geographic unit
     */
    public boolean covers(Location other) {
        while (other != null) {
            if (this == other)
                return true;
            other = other.parent;
        }
        return false;
    }
}
