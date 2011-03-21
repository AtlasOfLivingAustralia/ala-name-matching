/**
 *
 */
package au.org.ala.sds.validation;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class FactCollection {

    public static final String LATITUDE_KEY = "latitude";
    public static final String LONGITUDE_KEY = "longitude";
    public static final String STATE_KEY = "state";
    public static final String DATE_KEY = "date";
    public static final String YEAR_KEY = "year";
    public static final String COUNTRY_KEY = "country";
    public static final String ROW_KEY = "row";
    public static final String ZONES_KEY = "zone";

    private final Map<String, String> facts;

    public FactCollection() {
        this.facts = new HashMap<String, String>();
    }

    public void add(String key, String value) {
        facts.put(key, value);
    }

    public String get(String key) {
        return facts.get(key);
    }

    public String remove(String key) {
        return facts.remove(key);
    }

}
