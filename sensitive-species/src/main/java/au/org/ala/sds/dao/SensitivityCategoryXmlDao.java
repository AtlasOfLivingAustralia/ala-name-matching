/**
 *
 */
package au.org.ala.sds.dao;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import au.org.ala.sds.model.SensitivityCategory;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitivityCategoryXmlDao {

    protected static final Logger logger = Logger.getLogger(SensitivityCategoryXmlDao.class);

    private final InputStream stream;

    public SensitivityCategoryXmlDao(InputStream inputStream) {
        this.stream = inputStream;
    }

    /**
     * @throws IOException
     * @throws JDOMException
     * @see au.org.ala.sds.dao.SensitiveSpeciesDao#getAll()
     */
    @SuppressWarnings("unchecked")
    public Map<String, SensitivityCategory> getMap() throws Exception {
        Map<String, SensitivityCategory> zones = new HashMap<String, SensitivityCategory>();
        SAXBuilder builder = new SAXBuilder();
        Document doc = null;

        doc = builder.build(this.stream);

        Element root = doc.getRootElement();
        List zonesList = root.getChildren();

        for (Iterator sli = zonesList.iterator(); sli.hasNext(); ) {
            Element sze = (Element) sli.next();
            String id = sze.getAttributeValue("id");
            String name = sze.getAttributeValue("name");
            SensitivityCategory.CategoryType type = SensitivityCategory.CategoryType.valueOf(sze.getAttributeValue("type").toUpperCase());
            SensitivityCategory sz = new SensitivityCategory(id, name, type);
            zones.put(id, sz);
        }
        return zones;
    }

}
