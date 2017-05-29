package au.org.ala.names.index;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Build a working index.
 * <p>
 * The working index contains all relevant information
 * </p>
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 *         <p/>
 *         Copyright (c) 2016 CSIRO
 */
public class WorkingIndexBuilder {
    static protected Logger log = Logger.getLogger(WorkingIndexBuilder.class);

    /** The source index builder */
    private Collection<NameProvider> sources;
    /** The target directory */
    private File target;
    /** The index writer */
    private IndexWriter writer;

    /**
     * Construct for an index builder and target location.
     *
     * @param sources The name sources
     * @param target The target directory to build the index in
     */
    public WorkingIndexBuilder(Collection<NameProvider> sources, File target) {
        this.sources = sources;
        this.target = target;
        this.writer = null;
    }

    /**
     * Add a source to the working index
     *
     * @param source The source
     */
    protected void build(NameProvider source) {

    }

    public void begin() throws Exception {
        this.writer = this.createIndexWriter(this.target, new KeywordAnalyzer(), true);
    }

    public void commit() {
        try {
            if (this.writer != null) {
                this.writer.commit();
                this.writer.close();
            }
        } catch (IOException ex) {
            log.error("Unable to close writer", ex);
        } finally {
            this.writer = null;
        }
    }

    public void build() throws Exception {
        log.info("Building working index at " + this.target);
        try {
            this.begin();
            for (NameProvider source : this.sources)
                this.build(source);
        } finally {
            this.commit();
        }
        log.info("Finished building working index");
    }

    /**
     * Creates an index writer in the specified directory.  It will create/recreate
     * the target directory
     *
     * @param directory The directory
     * @param analyzer The term analyzer
     * @param replace If true, any existing index is
     *
     * @return The opened index writer
     *
     * @throws IOException if unable to open the index
     */
    protected IndexWriter createIndexWriter(File directory, Analyzer analyzer, boolean replace) throws IOException {
        IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_4_10_4, analyzer);
        if (replace)
            conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        else
            conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        if (directory.exists() && replace) {
            FileUtils.forceDelete(directory);
        }
        FileUtils.forceMkdir(directory);
        IndexWriter iw = new IndexWriter(FSDirectory.open(directory), conf);
        return iw;
    }

}
