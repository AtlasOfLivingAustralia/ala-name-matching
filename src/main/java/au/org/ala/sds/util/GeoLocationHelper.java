package au.org.ala.sds.util;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import au.org.ala.sds.model.SensitivityZone;

public class GeoLocationHelper {

    @SuppressWarnings("unchecked")
    public static Set<SensitivityZone> getZonesContainingPoint(String latitude, String longitude) throws Exception {

        final Logger logger = Logger.getLogger(GeoLocationHelper.class);
        Set<SensitivityZone> zones = new HashSet<SensitivityZone>();;

        //
        // Call geospatial web service
        //
        URL url = new URL("http://spatial.ala.org.au/gazetteer/latlon/" + latitude + "," + longitude);
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
        logger.debug("Looking up location using " + url);
        InputStream inStream = connection.getInputStream();
        GZIPInputStream gis = new GZIPInputStream(inStream);

        //
        // Parse XML result
        //
        logger.debug("Parsing location results");
        SAXBuilder parser = new SAXBuilder();
        Document doc = parser.build(gis);
        Element root = doc.getRootElement();
        List<Element> results = root.getChild("results").getChildren();
        for (Element result : results) {
            String name = result.getChildText("name");
            String layer = result.getChildText("layerName");
            if (layer.equalsIgnoreCase("state")) {
                SensitivityZone zone;
                if ((zone = SensitivityZone.getZone(name)) != null) {
                    zones.add(zone);
                }

                // TODO PFF PQA work around - remove when implemented in Gazetteer
                if (name.equalsIgnoreCase("Queensland") &&
                    layer.equalsIgnoreCase("state") &&
                    NumberUtils.toFloat(latitude) >= -19.0 &&
                    NumberUtils.toFloat(longitude) >= 144.25) {
                    zones.add(SensitivityZone.PFFPQA1995);
                }
            }

            if (layer.equalsIgnoreCase("lga")) {
                // TODO Special zones work around - remove when implemented in Gazetteer
                if (name.equalsIgnoreCase("Bauhinia (Queensland)") ||
                    name.equalsIgnoreCase("Emerald (Queensland)") ||
                    name.equalsIgnoreCase("Peak Downs (Queensland)")) {
                    // Emerald Citrus Canker PQA
                    zones.add(SensitivityZone.ECCPQA2004);
                } else if (name.equalsIgnoreCase("Wacol (Queensland)")) {
                    // Red Imported Fire Ant
                    zones.add(SensitivityZone.RIFARA);
                } else if (name.equalsIgnoreCase("Kubin (Queensland)")) {
                    // Torres Strait Protected Zone
                    zones.add(SensitivityZone.TSPZ);
                } else if (name.equalsIgnoreCase("Hammond (Queensland)")) {
                    // Special Quarantine Zone
                    zones.add(SensitivityZone.TSSQZ);
                }
            }
        }

        if (zones.isEmpty()) {
            logger.warn("Zone could not be determined from location: Lat " + latitude + ", Long " + longitude);
            zones.add(SensitivityZone.NOTAUS);
        }

        return zones;
    }

}
