/**
 *
 */
package au.org.ala.sds;

import java.io.IOException;

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

        SensitiveSpeciesDao dao = null;
        SensitiveTaxonStore store = null;

        try {
            dao = new SensitiveSpeciesXmlDao(url);
            store = new SensitiveTaxonStore(dao, cbIndexSearcher);
        } catch (Exception e) {
            if (e instanceof IOException) {
                try {

                } catch (Exception e1) {

                }
            }
        }
        return new SensitiveSpeciesFinder(store);

    }

    public static SensitiveSpeciesFinder getSensitiveSpeciesFinder(CBIndexSearch cbIndexSearcher) throws Exception {

        return getSensitiveSpeciesFinder("http://sds.ala.org.au/sensitive-species-data.xml", cbIndexSearcher);

    }
}
