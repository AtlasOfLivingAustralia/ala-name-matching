/**
 *
 */
package au.org.ala.sds;

import javax.sql.DataSource;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.sds.dao.SensitiveSpeciesDao;
import au.org.ala.sds.dao.SensitiveSpeciesMySqlDao;
import au.org.ala.sds.dao.SensitiveSpeciesXmlDao;
import au.org.ala.sds.model.SensitiveTaxonStore;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitiveSpeciesFinderFactory {

    public static SensitiveSpeciesFinder getSensitiveSpeciesFinder(DataSource dataSource, CBIndexSearch cbIndexSearcher) throws Exception {

        SensitiveSpeciesDao dao = new SensitiveSpeciesMySqlDao(dataSource);
        SensitiveTaxonStore store = new SensitiveTaxonStore(dao, cbIndexSearcher);
        return new SensitiveSpeciesFinder(store);

    }

    public static SensitiveSpeciesFinder getSensitiveSpeciesFinder(String url, CBIndexSearch cbIndexSearcher) throws Exception {

        SensitiveSpeciesDao dao = new SensitiveSpeciesXmlDao(url);
        SensitiveTaxonStore store = new SensitiveTaxonStore(dao, cbIndexSearcher);
        return new SensitiveSpeciesFinder(store);

    }

}
