package au.org.ala.vocab;

import au.org.ala.names.util.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.net.URI;

import static org.junit.Assert.assertEquals;

/**
 * Tests for a taxonomic rank.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class TaxonRankTest extends TestUtils {
    private Vocabulary<TaxonRank> vocabulary;

    @Before
    public void setup() throws Exception {
        this.vocabulary = new Vocabulary<>(URI.create("urm:x-ala:vocabulary:tr-1"), "tr-1", null);
    }

    @Test
    public void testWrite1() throws Exception {
        TaxonRank rank = new TaxonRank(this.vocabulary, "genus", 6000, true, true);
        StringWriter sw = new StringWriter();
        rank.write(sw);
        assertEquals(this.loadResource("taxon-rank-1.json"), sw.toString());
    }
}
