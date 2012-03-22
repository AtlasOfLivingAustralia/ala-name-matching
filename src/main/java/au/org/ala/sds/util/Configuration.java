package au.org.ala.sds.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Configuration {

    private static final Configuration instance = new Configuration();

    private final Properties config;

    private Configuration() {
        config = new Properties();
        File configFile = new File("/data/sds/sds-config.properties");
        try {
            config.load(new FileInputStream(configFile));
        } catch(IOException e) { /* Getters will provide defaults */ }
    }

    public static Configuration getInstance() {
        return instance;
    }

    public String getSpeciesUrl() {
        return config.getProperty("species-data", "http://sds.ala.org.au/sensitive-species-data.xml");
    }

    public String getCategoryUrl() {
        return config.getProperty("category-data", "http://sds.ala.org.au/sensitivity-categories.xml");
    }

    public String getZoneUrl() {
        return config.getProperty("zone-data", "http://sds.ala.org.au/sensitivity-zones.xml");
    }

    public boolean isCached() {
        return config.getProperty("species-cache", "false").equalsIgnoreCase("true");
    }

    public String getCacheUrl() {
        return config.getProperty("cache-data", "/data/sds/species-cache.ser");
    }

    public String getSpatialUrl( ) {
        return config.getProperty("spatial-layer-ws", "http://spatial.ala.org.au/layers-service/intersect/");
    }
}
