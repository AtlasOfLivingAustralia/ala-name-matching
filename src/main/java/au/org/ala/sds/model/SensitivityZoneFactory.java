/**
 *
 */
package au.org.ala.sds.model;

import java.util.Map;

import org.apache.log4j.Logger;

import au.org.ala.sds.dao.SensitivityZonesXmlDao;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitivityZoneFactory {

    protected static final Logger logger = Logger.getLogger(SensitivityZoneFactory.class);

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
        try {
            zones = dao.getMap();
        } catch (Exception e) {
            logger.warn("Exception occurred getting sensitivity-categories.xml from webapp - trying to read file in /data/sds", e);
            dao = new SensitivityZonesXmlDao("file:///data/sds/sensitivity-zones.xml");
            try {
                zones = dao.getMap();
            } catch (Exception e1) {
                logger.warn("Exception occurred getting sensitivity-zones.xml from /data/sds", e1);
            }
        }
    }
}
