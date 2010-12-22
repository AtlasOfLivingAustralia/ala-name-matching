package au.org.ala.sensitiveData.dao;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import au.org.ala.sensitiveData.model.SensitivityZone;

public class GeoLocationDaoImpl implements GeoLocationDao {

    @SuppressWarnings("unchecked")
    @Override
    public SensitivityZone getStateContainingPoint(String latitude, String longitude) throws Exception {
        SensitivityZone state = null;
        
        //
        // Call geospatial web service
        //
        URL url = new URL("http://spatial.ala.org.au/gazetteer/state/latlon/" + latitude + "," + longitude);
//        InputStream in = url.openStream();
        String response = 
                  "<search xmlns:xlink='http://www.w3.org/1999/xlink'>"
                + " <results>"
                + "     <result xlink:href='/geoserver/rest/gazetteer/aus1/New_South_Wales.json'>"
                + "     <id>aus1/New South Wales</id>"
                + "     <name>New South Wales</name>"
                + "     <layerName>aus1</layerName>"
                + "     <idAttribute1>New South Wales</idAttribute1>"
                + "     <idAttribute2></idAttribute2>"
                + "     <score>1.0</score>"
                + "     </result>"
                + " </results>"
                + "</search>";
        InputStream in = new ByteArrayInputStream(response.getBytes());
        
        //
        // Parse XML result
        //
        SAXBuilder parser = new SAXBuilder();
        Document doc = parser.build(in);
        Element root = doc.getRootElement();
        List<Element> results = root.getChild("results").getChildren();
        for (Element result : results) {
            String name = result.getChildText("name");
            state = SensitivityZone.getZone(name);
        }
        return state;
    }

}
