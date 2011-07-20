/**
 *
 */
package au.org.ala.sds.model;

import java.util.Map;

import au.org.ala.sds.dao.SensitivityZonesXmlDao;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitivityZoneFactory {

    private static Map<String, SensitivityZone> zones;

    public static SensitivityZone getZone(String key) {
        if (zones == null) {
            initZones();
        }
        return zones.get(key);
    }

    public static SensitivityZone getZoneByName(String name) {
        if (zones == null) {
            initZones();
        }
        for (SensitivityZone sz : zones.values()) {
            if (sz.getName().equalsIgnoreCase(name)) {
                return sz;
            }
        }
        return null;
    }

    private static void initZones() {
        SensitivityZonesXmlDao dao = new SensitivityZonesXmlDao("http://sds.ala.org.au/sensitivity-zones.xml");
        zones = dao.getMap();
    }
}
