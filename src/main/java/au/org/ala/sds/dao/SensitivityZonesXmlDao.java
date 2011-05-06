/**
 *
 */
package au.org.ala.sds.dao;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import au.org.ala.sds.model.SensitivityZone;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitivityZonesXmlDao {

    protected static final Logger logger = Logger.getLogger(SensitivityZonesXmlDao.class);

    private final String url;

    public SensitivityZonesXmlDao(String url) {
        this.url = url;
    }
    /**
     * @see au.org.ala.sds.dao.SensitiveSpeciesDao#getAll()
     */
    @SuppressWarnings("unchecked")
    public Map<String, SensitivityZone> getMap() {
        Map<String, SensitivityZone> zones = new HashMap<String, SensitivityZone>();
        SAXBuilder builder = new SAXBuilder();
        Document doc = null;

        try {
            doc = builder.build(this.url);
        } catch (JDOMException e) {
            logger.error("Error parsing species list xml", e);
        } catch (IOException e) {
            logger.error("Error reading species list xml", e);
        }

        Element root = doc.getRootElement();
        List zonesList = root.getChildren();

        for (Iterator sli = zonesList.iterator(); sli.hasNext(); ) {
            Element sze = (Element) sli.next();
            String id = sze.getAttributeValue("id");
            String name = sze.getAttributeValue("name");
            SensitivityZone.ZoneType type = SensitivityZone.ZoneType.valueOf(sze.getAttributeValue("type").toUpperCase());
            SensitivityZone sz = new SensitivityZone(id, name, type);
            zones.put(id, sz);
        }
        return zones;
    }

}
