package au.org.ala.sds.util;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import au.org.ala.sds.model.SensitivityZone;

public class GeoLocationHelper {

    @SuppressWarnings("unchecked")
    public static SensitivityZone getStateContainingPoint(String latitude, String longitude) throws Exception {

        final Logger logger = Logger.getLogger(GeoLocationHelper.class);
        SensitivityZone state = null;
        
        //
        // Call geospatial web service
        //
        URL url = new URL("http://spatial.ala.org.au/gazetteer/state/latlon/" + latitude + "," + longitude);
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
        InputStream inStream = connection.getInputStream();
        GZIPInputStream gis = new GZIPInputStream(inStream);

//        String response = 
//                  "<search xmlns:xlink='http://www.w3.org/1999/xlink'>"
//                + " <results>"
//                + "     <result xlink:href='/geoserver/rest/gazetteer/aus1/New_South_Wales.json'>"
//                + "     <id>aus1/New South Wales</id>"
//                + "     <name>New South Wales</name>"
//                + "     <layerName>aus1</layerName>"
//                + "     <idAttribute1>New South Wales</idAttribute1>"
//                + "     <idAttribute2></idAttribute2>"
//                + "     <score>1.0</score>"
//                + "     </result>"
//                + " </results>"
//                + "</search>";
//        InputStream in = new ByteArrayInputStream(response.getBytes());
        
        //
        // Parse XML result
        //
        SAXBuilder parser = new SAXBuilder();
        Document doc = parser.build(gis);
        Element root = doc.getRootElement();
        List<Element> results = root.getChild("results").getChildren();
        for (Element result : results) {
            String name = result.getChildText("name");
            state = SensitivityZone.getZone(name);
        }
        
        if (state == null) {
            logger.warn("State could not be determined from location: Lat " + latitude + ", Long " + longitude);
        }
        
        return state;
    }

}
