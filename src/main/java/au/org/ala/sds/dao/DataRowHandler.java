/**
 *
 */
package au.org.ala.sds.dao;

import java.util.Map;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public interface DataRowHandler {

        void handleRow(Map<String, String> facts);
}
