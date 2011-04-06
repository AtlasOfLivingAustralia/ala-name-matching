/**
 *
 */
package au.org.ala.sds.dao;

import au.org.ala.sds.validation.FactCollection;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public interface DataFileRowHandler {

        void handleRow(FactCollection facts);
}
