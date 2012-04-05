/**
 *
 */
package au.org.ala.sds;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.sds.dao.SensitiveSpeciesDao;
import au.org.ala.sds.dao.SensitiveSpeciesMySqlDao;
import au.org.ala.sds.dao.SensitiveSpeciesXmlDao;
import au.org.ala.sds.model.SensitiveTaxonStore;
import au.org.ala.sds.util.Configuration;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitiveSpeciesFinderFactory {

    protected static final Logger logger = Logger.getLogger(SensitiveSpeciesFinderFactory.class);

    private static final String SPECIES_RESOURCE = "sensitive-species.xml";

    public static SensitiveSpeciesFinder getSensitiveSpeciesFinder(DataSource dataSource, CBIndexSearch cbIndexSearcher) throws Exception {

        SensitiveSpeciesDao dao = new SensitiveSpeciesMySqlDao(dataSource);
        SensitiveTaxonStore store = new SensitiveTaxonStore(dao, cbIndexSearcher);
        return new SensitiveSpeciesFinder(store);

    }

    public static SensitiveSpeciesFinder getSensitiveSpeciesFinder(String dataUrl, CBIndexSearch cbIndexSearcher) throws Exception {

        SensitiveTaxonStore store = null;

        if (Configuration.getInstance().isCached()) {
            File cache = new File(Configuration.getInstance().getCacheUrl());
            if (cache.exists()) {
                logger.info("Reading SensitveTaxonStore from serialized cache file " + cache.getPath());
                store = getStoreFromCache(cache);
            } else {
                store = getStoreFromUrl(dataUrl, cbIndexSearcher);
                writeStoreCache(cache, store);
            }
        } else {
            store = getStoreFromUrl(dataUrl, cbIndexSearcher);
        }

        return new SensitiveSpeciesFinder(store);

    }

    public static SensitiveSpeciesFinder getSensitiveSpeciesFinder(CBIndexSearch cbIndexSearcher) throws Exception {

        return getSensitiveSpeciesFinder(Configuration.getInstance().getSpeciesUrl(), cbIndexSearcher);

    }

    private static SensitiveTaxonStore getStoreFromUrl(String dataUrl, CBIndexSearch cbIndexSearcher) throws Exception {

        URL url = null;
        InputStream is = null;

        try {
            url = new URL(dataUrl);
            is = url.openStream();
        } catch (Exception e) {
            logger.warn("Exception occurred getting species list from " + dataUrl, e);
            is = SensitiveSpeciesFinderFactory.class.getClassLoader().getResourceAsStream(SPECIES_RESOURCE);
            if (is == null) {
                logger.error("Unable to read " + SPECIES_RESOURCE + " from jar file");
            } else {
                logger.info("Reading bundled resource " + SPECIES_RESOURCE + " from jar file");
            }
        }

        SensitiveSpeciesDao dao = null;
        try {
            dao = new SensitiveSpeciesXmlDao(is);
        } catch (Exception e) {
            logger.error("Exception occurred parsing species list from " + is.toString(), e);
        }

        return new SensitiveTaxonStore(dao, cbIndexSearcher);

    }

    private static SensitiveTaxonStore getStoreFromCache(File cache) {
        SensitiveTaxonStore store = null;

        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cache));
            store = (SensitiveTaxonStore) ois.readObject();
            ois.close();
        } catch (Exception e) {
            logger.error("Error deserializing SensitiveTaxonStore", e);
        }

        return store;
    }

    private static void writeStoreCache(File cache, SensitiveTaxonStore store) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cache));
            oos.writeObject(store);
            oos.close();
        } catch (Exception e) {
            logger.error("Error serializing SensitiveTaxonStore", e);
        }

    }
}