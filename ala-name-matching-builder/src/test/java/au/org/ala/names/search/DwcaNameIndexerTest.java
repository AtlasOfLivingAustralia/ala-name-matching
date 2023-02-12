/*
 * Copyright (c) 2023 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 */

package au.org.ala.names.search;

import au.org.ala.names.util.FileUtils;
import au.org.ala.names.util.TestUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Test cases for the DWCA name indexer
 */
public class DwcaNameIndexerTest extends TestUtils {
    private DwcaNameIndexer indexer;
    private File work;
    private File output;

    @Before
    public void setUp() throws Exception {
        this.work = FileUtils.mkTempDir("work", null, null);
        this.output = FileUtils.mkTempDir("indexer", null, null);
        Properties priorities = new Properties();
        priorities.load(this.resourceReader("priorities.properties"));
        this.indexer = new DwcaNameIndexer(this.output, this.work, priorities, true, true);
    }

    @After
    public void tearDown() throws Exception {
        if (this.output != null) {
            FileUtils.clear(this.output, true);
        }
        if (this.work != null) {
            FileUtils.clear(this.work, true);
        }
    }

    @Test
    public void testBuildNameComplete1() throws Exception {
        assertEquals("Caladenia dilatata", this.indexer.buildNameComplete("Caladenia dilatata", null, null));
        assertEquals("Caladenia dilatata R.Br.", this.indexer.buildNameComplete("Caladenia dilatata", "R.Br.", null));
        assertEquals("Caladenia dilatata R. Br.", this.indexer.buildNameComplete("Caladenia dilatata", "R.Br.", "Caladenia dilatata R. Br."));
    }

    @Test
    public void testBuildNameComplete2() throws Exception {
        assertEquals("Caladenia dilatata R.Br.", this.indexer.buildNameComplete(null, "Caladenia dilatata R.Br.", null));
    }


    @Test
    public void testBuildNameComplete3() throws Exception {
        assertEquals("Caladenia dilatata R.Br.", this.indexer.buildNameComplete("Caladenia dilatata", "Caladenia dilatata R.Br.", null));
        assertEquals("Caladenia dilatata R.Br.", this.indexer.buildNameComplete("Caladenia dilatata R.Br.", "R.Br.", null));
    }

    // Test for name duplication
    @Test
    public void testNameDuplicate1() throws Exception {
        File source = this.resourceAsFile("dwca-1");
        this.indexer.begin();
        this.indexer.createLoadingIndex(source);
        this.indexer.commitLoadingIndexes();
        this.indexer.generateIndex();
        this.indexer.create(source);
        this.indexer.commit();
        assertTrue(new File(this.output, "cb").exists());
        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(new File(this.output, "cb").toPath()));//false
        IndexSearcher searcher = new IndexSearcher(reader);
        Query query = NameIndexField.LSID.search("https://id.biodiversity.org.au/taxon/apni/51398946");
        TopDocs docs = searcher.search(query, 1);
        assertEquals(1, docs.totalHits.value);
        Document doc = searcher.doc(docs.scoreDocs[0].doc);
        List<String> names = Arrays.asList(doc.getValues(NameIndexField.NAME.name));
        assertEquals(2, names.size());
        assertTrue(names.contains("Caladenia dilatata"));
        assertTrue(names.contains("Caladenia dilatata R.Br."));
        assertFalse(names.contains("Caladenia dilatata Caladenia dilatata R.Br."));
    }
}