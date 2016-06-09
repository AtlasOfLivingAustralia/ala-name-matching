package au.org.ala.names.index;

import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Build a name index from a number of sources.
 * <p>
 * All data is loaded into a working index.
 * The
 * </p>
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 *
 *         Copyright (c) 2016 CSIRO
 */
public class IndexBuilder {
    static protected Logger log = Logger.getLogger(IndexBuilder.class);

    /** The sources for index construction */
    private Map<String, NameSource> sources;
}
