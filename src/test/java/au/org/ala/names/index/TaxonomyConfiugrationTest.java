package au.org.ala.names.index;

import au.org.ala.names.util.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Test cases for {@link TaxonomyConfiguration}
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class TaxonomyConfiugrationTest extends TestUtils {
    private static final String ID_1 = "ID-1";
    private static final float PRIORITY_1 = 1.5f;

    @Test
    public void testWrite1() throws Exception {
        NameProvider provider1 = new NameProvider(ID_1, PRIORITY_1);
        TaxonomyConfiguration config = new TaxonomyConfiguration();
        config.providers = Arrays.asList(provider1);
        config.defaultProvider = ID_1;
        config.nameAnalyserClass = ALANameAnalyser.class;
        StringWriter sw = new StringWriter();
        config.write(sw);
        assertEquals(this.loadResource("taxonomy-config-1.json"), sw.toString());
    }


    @Test
    public void testRead1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-1.json"));
        assertEquals(ID_1, config.defaultProvider);
        assertNotNull(config.providers);
        assertEquals(1, config.providers.size());
        assertEquals(ID_1, config.providers.get(0).getId());
        assertEquals(PRIORITY_1, config.providers.get(0).getPriority(), 0.001);
        assertEquals(ALANameAnalyser.class, config.nameAnalyserClass);
    }

}
