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

        SensitiveSpeciesDao dao = null;
        SensitiveTaxonStore store = null;

        try {
            dao = new SensitiveSpeciesXmlDao(url);
        } catch (Exception e) {
            System.err.println("Exception occurred getting sensitivity-species.xml from webapp - trying to read file in /data/sds");
            try {
                dao = new SensitiveSpeciesXmlDao("file:///data/sds/sensitivity-species.xml");
            } catch (Exception e1) {
                System.err.println("Exception occurred getting sensitivity-species.xml from /data/sds");
                e1.printStackTrace();
            }
        }

        store = new SensitiveTaxonStore(dao, cbIndexSearcher);

        return new SensitiveSpeciesFinder(store);

    }

    public static SensitiveSpeciesFinder getSensitiveSpeciesFinder(CBIndexSearch cbIndexSearcher) throws Exception {

        return getSensitiveSpeciesFinder("http://sds.ala.org.au/sensitive-species-data.xml", cbIndexSearcher);

    }
}
