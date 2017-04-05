

package au.org.ala.names.index;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test cases for cleaned scientific names
 */
public class ALANameAnalyserTest {
    private ALANameAnalyser analyser;

    @Before
    public void setup() {
        this.analyser = new ALANameAnalyser();
    }

    @Test
    public void testKey1() throws Exception {
        NameAnalyser.NameKey key = this.analyser.analyse("ICNAFP", "Hemigenia brachyphylla F.Muell.", null);
        assertEquals("ICNAFP", key.code);
        assertEquals("HEMIGENIA BRACHYPHYLLA", key.scientificName);
        assertEquals("F.Muell.", key.scientificNameAuthorship);

    }
}
