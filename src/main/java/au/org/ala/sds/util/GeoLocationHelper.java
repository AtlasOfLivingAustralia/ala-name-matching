package au.org.ala.sds.util;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import au.org.ala.sds.model.SensitivityZone;
import au.org.ala.sds.model.SensitivityZoneFactory;

public class GeoLocationHelper {

    public static Set<SensitivityZone> getZonesContainingPoint(String latitude, String longitude) throws Exception {

        final Logger logger = Logger.getLogger(GeoLocationHelper.class);

        final String COASTAL_WATERS_LAYER = "cl927";
        final String LGA_BOUNDARIES_LAYER = "cl23";

        Set<SensitivityZone> zones = new HashSet<SensitivityZone>();;

        //
        // Call geospatial web service
        //
        URL url = new URL(Configuration.getInstance().getSpatialUrl() + latitude + "/" + longitude);
        URLConnection connection = url.openConnection();
        logger.debug("Looking up location using " + url);
        InputStream inStream = connection.getInputStream();

        //
        // Parse JSON result
        //
        logger.debug("Parsing location results");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(inStream, JsonNode.class);
        for (JsonNode node : rootNode) {
            String field = node.get("field").getTextValue();
            String value = node.get("value").getTextValue();

            if (field.equalsIgnoreCase(COASTAL_WATERS_LAYER)) {
                String state = value.replace(" (including Coastal Waters)", "");
                state = state.replace("Captial", "Capital");
                state = state.replace("Jervis Bay Territory", "Australian Capital Territory");
                SensitivityZone zone;
                if ((zone = SensitivityZoneFactory.getZoneByName(state)) != null) {
                    zones.add(zone);
                }

                // TODO PFF PQA work around - remove when implemented in Gazetteer
                if (state.equalsIgnoreCase("Queensland") &&
                    NumberUtils.toFloat(latitude) >= -19.0 &&
                    NumberUtils.toFloat(longitude) >= 144.25) {
                    zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PFFPQA1995));
                }

            } else if (field.equalsIgnoreCase(LGA_BOUNDARIES_LAYER)) {
                // TODO Special zones work around - remove when implemented in Gazetteer
                if (value.equalsIgnoreCase("Bauhinia") ||
                    value.equalsIgnoreCase("Emerald") ||
                    value.equalsIgnoreCase("Peak Downs")) {
                    // Emerald Citrus Canker PQA
                    zones.add(SensitivityZoneFactory.getZone(SensitivityZone.ECCPQA2004));
                } else if (value.equalsIgnoreCase("Wacol")) {
                    // Red Imported Fire Ant
                    zones.add(SensitivityZoneFactory.getZone(SensitivityZone.RIFARA));
                } else if (value.equalsIgnoreCase("Kubin") || value.equalsIgnoreCase("Badu")) {
                    // Torres Strait Protected Zone
                    zones.add(SensitivityZoneFactory.getZone(SensitivityZone.TSPZ));
                } else if (value.equalsIgnoreCase("Hammond") || value.equalsIgnoreCase("Torres")) {
                    // Special Quarantine Zone
                    zones.add(SensitivityZoneFactory.getZone(SensitivityZone.TSSQZ));
                } else if (value.equalsIgnoreCase("Albury")) {
                    // Phylloxera Infested Zone
                    zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PIZNSWAC));
                }
            }
        }

        if (zones.isEmpty()) {
            logger.debug("Zone could not be determined from location: Lat " + latitude + ", Long " + longitude);
            zones.add(SensitivityZoneFactory.getZone(SensitivityZone.NOTAUS));
        }
        return zones;
    }

}
