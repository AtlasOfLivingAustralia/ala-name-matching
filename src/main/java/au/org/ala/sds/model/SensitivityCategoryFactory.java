/**
 *
 */
package au.org.ala.sds.model;

import java.util.Map;

import au.org.ala.sds.dao.SensitivityCategoryXmlDao;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitivityCategoryFactory {

    private static Map<String, SensitivityCategory> categories;

    public static SensitivityCategory getCategory(String key) {
        if (categories == null) {
            initCategories();
        }
        return categories.get(key);
    }

    private static void initCategories() {
        SensitivityCategoryXmlDao dao = new SensitivityCategoryXmlDao("http://sds.ala.org.au/sensitivity-categories.xml");
        categories = dao.getMap();
    }
}
