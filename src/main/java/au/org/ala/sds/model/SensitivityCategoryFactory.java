/**
 *
 */
package au.org.ala.sds.model;

import java.util.Map;

import org.apache.log4j.Logger;

import au.org.ala.sds.dao.SensitivityCategoryXmlDao;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitivityCategoryFactory {

    protected static final Logger logger = Logger.getLogger(SensitivityCategoryFactory.class);

    private static Map<String, SensitivityCategory> categories;

    public static SensitivityCategory getCategory(String key) {
        if (categories == null) {
            initCategories();
        }
        return categories.get(key);
    }

    private static void initCategories() {
        SensitivityCategoryXmlDao dao = new SensitivityCategoryXmlDao("http://sds.ala.org.au/sensitivity-categories.xml");
        try {
            categories = dao.getMap();
        } catch (Exception e) {
            logger.warn("Exception occurred getting sensitivity-categories.xml from webapp - trying to read file in /data/sds", e);
            dao = new SensitivityCategoryXmlDao("file:///data/sds/sensitivity-categories.xml");
            try {
                categories = dao.getMap();
            } catch (Exception e1) {
                logger.warn("Exception occurred getting sensitivity-categories.xml from /data/sds", e1);
            }
        }
    }
}
