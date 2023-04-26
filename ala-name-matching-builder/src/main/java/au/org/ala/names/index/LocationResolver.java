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

import java.util.HashMap;
import java.util.Map;

/**
 * A location resolver that can be used to look up locations by name, identifier etc.
 * <p>
 * Location resolvers are attached to providers, so that individual providers can, in theory,
 * have slightly different ways of looking up locations.
 * </p>
 */
public class LocationResolver {
    /** The canonical map from locationID to loction */
    private Map<String, Location> locations;
    /** Alternative names and identifiers for a location */
    private Map<String, Location> alternatives;

    /**
     * Construct an empry resolver
     */
    public LocationResolver() {
        this.locations = new HashMap<>();
        this.alternatives = new HashMap<>();
    }

    /**
     * Add a new location.
     * <p>
     * The location is added by locationID.
     * Any alternative identifiers or names that have a higher weight than the existing
     * location replace that location.
     * </p>
     * @param location
     * @throws IllegalArgumentException If the location identifier is already in use
     */
    synchronized public void add(Location location) throws IllegalArgumentException {
        String id = location.getLocationID();
        if (this.locations.containsKey(id))
            throw new IllegalArgumentException("Location identifier " + id + " is already in use");
        this.locations.put(id, location);
        Location existing = this.alternatives.get(location.getLocality());
        if (existing == null || existing.getWeight() < location.getWeight())
            this.alternatives.put(location.getLocality(), location);
        for (String identifier: location.getIdentifiers()) {
            existing = this.alternatives.get(identifier);
            if (existing == null || existing.getWeight() < location.getWeight())
                this.alternatives.put(identifier, location);
        }
        for (String name: location.getNames()) {
            existing = this.alternatives.get(name);
            if (existing == null || existing.getWeight() < location.getWeight())
                this.alternatives.put(name, location);
        }
    }

    /**
     * Find a location by identifier or name.
     *
     * @param key An identifier, alternative identifier, name etc.
     *
     * @return The matching location or null for none
     */
    public Location get(String key) {
        Location location = this.locations.get(key);
        if (location != null)
            return location;
        return this.alternatives.get(key);
    }


    /**
     * Resolve the location list.
     *
     * @return The number of locations resolved
     *
     * @see Location#resolve(LocationResolver)
     */
    public int resolve() {
        for (Location loc : this.locations.values()) {
            loc.resolve(this);
        }
        return this.locations.size();
    }

}
