/**
 *
 */
package au.org.ala.sds.dao;

import au.org.ala.sds.validation.FactCollection;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public interface DataRowHandler {

        void handleRow(FactCollection facts);
}
