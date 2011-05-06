/**
 *
 */
package au.org.ala.sds.util;

import java.util.List;

import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.model.SensitivityZone;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class GeneralisedLocationFactory {

    public static GeneralisedLocation getGeneralisedLocation(String latitude, String longitude, SensitiveTaxon st, List<SensitivityZone> zones) {
        return new GeneralisedLocation(latitude, longitude, st, zones);
    }

}
