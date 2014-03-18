/**
 *
 */
package au.org.ala.sds.model;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.apache.log4j.Logger;

import au.org.ala.sds.dao.SensitivityZonesXmlDao;
import au.org.ala.sds.util.Configuration;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitivityZoneFactory {

    protected static final Logger logger = Logger.getLogger(SensitivityZoneFactory.class);

    private static final String ZONES_RESOURCE = "sensitivity-zones.xml";

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

        URL url = null;
        InputStream is = null;

        try {
            url = new URL(Configuration.getInstance().getZoneUrl());
            is = url.openStream();
        } catch (Exception e) {
            logger.warn("Exception occurred getting zones list from " + url, e);
            is = SensitivityZoneFactory.class.getClassLoader().getResourceAsStream(ZONES_RESOURCE);
            if (is == null) {
                logger.error("Unable to read " + ZONES_RESOURCE + " from jar file");
            } else {
                logger.info("Reading bundled resource " + ZONES_RESOURCE + " from jar file");
            }
        }

        SensitivityZonesXmlDao dao = new SensitivityZonesXmlDao(is);
        try {
            zones = dao.getMap();
        } catch (Exception e) {
            logger.error("Exception occurred parsing zones list from " + is, e);
        }
    }
}
