/**
 *
 */
package au.org.ala.sds.util;

import au.org.ala.sds.model.SensitiveSpecies;
import au.org.ala.sds.model.SensitivityCategory;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class GeneralisedLocationFactory {

    public static GeneralisedLocation getGeneralisedLocation(String latitude, String longitude, SensitivityCategory category) {
        return new GeneralisedLocation(latitude, longitude, category);
    }

    public static GeneralisedLocation getGeneralisedLocation(String latitude, String longitude, SensitiveSpecies ss, String state) {
        return new GeneralisedLocation(latitude, longitude, ss, state);
    }
}
