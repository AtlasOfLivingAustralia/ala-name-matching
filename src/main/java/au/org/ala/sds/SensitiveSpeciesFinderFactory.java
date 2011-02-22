/**
 *
 */
package au.org.ala.sds;

import javax.sql.DataSource;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.sds.dao.SensitiveSpeciesDao;
import au.org.ala.sds.dao.SensitiveSpeciesDaoImpl;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitiveSpeciesFinderFactory {

    public static SensitiveSpeciesFinder getSensitiveSpeciesFinder(DataSource dataSource, CBIndexSearch cbIndexSearch) throws Exception {

        SensitiveSpeciesDao dao = new SensitiveSpeciesDaoImpl(dataSource, cbIndexSearch);
        return new SensitiveSpeciesFinder(dao);
    }
}
