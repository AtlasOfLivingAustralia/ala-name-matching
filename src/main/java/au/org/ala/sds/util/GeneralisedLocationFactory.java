/**
 *
 */
package au.org.ala.sds.util;

import java.util.Set;

import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.model.SensitivityZone;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class GeneralisedLocationFactory {

    public static GeneralisedLocation getGeneralisedLocation(String latitude, String longitude, SensitiveTaxon ss, Set<SensitivityZone> zones) {
        return new GeneralisedLocation(latitude, longitude, ss, zones);
    }

}
