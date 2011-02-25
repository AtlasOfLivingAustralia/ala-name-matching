/**
 *
 */
package au.org.ala.sds.util;

import au.org.ala.sds.model.SensitiveSpecies;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class GeneralisedLocationFactory {

    public static GeneralisedLocation getGeneralisedLocation(String latitude, String longitude, SensitiveSpecies ss) {
        return new GeneralisedLocation(latitude, longitude, ss);
    }

    public static GeneralisedLocation getGeneralisedLocation(String latitude, String longitude, SensitiveSpecies ss, String state) {
        return new GeneralisedLocation(latitude, longitude, ss, state);
    }
}
