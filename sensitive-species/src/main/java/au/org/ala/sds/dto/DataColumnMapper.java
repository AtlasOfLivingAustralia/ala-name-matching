/**
 *
 */
package au.org.ala.sds.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class DataColumnMapper {

    private final Map<String, String> map = new HashMap<String, String>();

    public void add(String key, String value) {
        map.put(key, value);
    }

    public String get(String key) {
        return map.get(key);
    }

    public Set<String> getKeySet() {
        return map.keySet();
    }
}
