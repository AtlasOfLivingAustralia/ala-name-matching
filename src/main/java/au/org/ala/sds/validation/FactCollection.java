/**
 *
 */
package au.org.ala.sds.validation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class FactCollection {

    public static final String FAMILY_KEY = "family";
    public static final String GENUS_KEY = "genus";
    public static final String SPECIFIC_EPITHET_KEY = "specificEpithet";
    public static final String INTRA_SPECIFIC_EPITHET_KEY = "intraspecificEpithet";
    public static final String SCIENTIFIC_NAME_KEY = "scientificName";
    public static final String DECIMAL_LATITUDE_KEY = "decimalLatitude";
    public static final String DECIMAL_LONGITUDE_KEY = "decimalLongitude";
    public static final String STATE_PROVINCE_KEY = "stateProvince";
    public static final String MUNICIPALITY_KEY = "municipality";
    public static final String EVENT_DATE_KEY = "eventDate";
    public static final String YEAR_KEY = "year";
    public static final String MONTH_KEY = "month";
    public static final String DAY_KEY = "day";
    public static final String COUNTRY_KEY = "country";
    public static final String ROW_KEY = "row";
    public static final String ZONES_KEY = "zone";

    public static final String[] FACT_NAMES = { SCIENTIFIC_NAME_KEY,
            FAMILY_KEY, GENUS_KEY, SPECIFIC_EPITHET_KEY,
            INTRA_SPECIFIC_EPITHET_KEY, DECIMAL_LATITUDE_KEY,
            DECIMAL_LONGITUDE_KEY, MUNICIPALITY_KEY, STATE_PROVINCE_KEY,
            COUNTRY_KEY, EVENT_DATE_KEY, YEAR_KEY };

    public static final String[] FACT_NAMES1 = { SCIENTIFIC_NAME_KEY,
            FAMILY_KEY, GENUS_KEY, SPECIFIC_EPITHET_KEY,
            INTRA_SPECIFIC_EPITHET_KEY, DECIMAL_LATITUDE_KEY,
            DECIMAL_LONGITUDE_KEY };

    public static final String[] FACT_NAMES2 = { MUNICIPALITY_KEY,
            STATE_PROVINCE_KEY, COUNTRY_KEY, EVENT_DATE_KEY, YEAR_KEY };

    private final Map<String, String> facts;

    public FactCollection() {
        this.facts = new HashMap<String, String>();
    }

    public FactCollection(Map<String, String> map) {
        this.facts = new HashMap<String, String>(map);
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

    public boolean isNotEmpty() {
        return !facts.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[FactCollection: ");
        for (Entry<String, String> entry : facts.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
        }
        sb.replace(sb.length() - 2, sb.length() - 1, "]");

        return sb.toString();
    }

}
