package au.org.ala.names.search;

import au.org.ala.names.model.LinnaeanRankClassification;
import au.org.ala.names.model.MatchType;
import au.org.ala.names.model.MetricsResultDTO;
import au.org.ala.names.model.NameSearchResult;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * The set of test associated with performing correct matches of biocache names
 * - includes tests for error/issues types
 * - matches based on higher classification etc...
 * <p/>
 * TODO Need to add more test cases to this class
 *
 * @author Natasha Carter
 */
public class VernacularMatchTest {

    private static ALANameSearcher searcher;

    @org.junit.BeforeClass
    public static void init() throws Exception {
        searcher = new ALANameSearcher("/data/lucene/namematching-20190213");
     }

    @Test
    public void testVernacular1() throws Exception {
        String name = "Giant Rush";
        String expectedLsid = "http://id.biodiversity.org.au/node/apni/2902770";
        NameSearchResult result = null;

        result = searcher.searchForCommonName(name);
        assertNotNull(result);
        assertEquals(expectedLsid, result.getLsid());
    }

    @Ignore // Requires indidgenous names
    @Test
    public void testVernacular2() throws Exception {
        String name = "Dhulwa";
        String expectedLsid = "http://id.biodiversity.org.au/node/apni/2904436";
        NameSearchResult result = null;

        result = searcher.searchForCommonName(name);
        assertNull(result); // Multiple matches
    }

    @Test
    public void testVernacular3() throws Exception {
        String name = "Rock Sheoak";
        String expectedLsid = "http://id.biodiversity.org.au/node/apni/2893343";
        NameSearchResult result = null;

        result = searcher.searchForCommonName(name);
        assertNotNull(result);
        assertEquals(MatchType.VERNACULAR, result.getMatchType());
        assertEquals(expectedLsid, result.getLsid());
    }

    @Test
    public void testVernacular4() throws Exception {
        String name = "Onespine Unicornfish";
        String expectedLsid = "urn:lsid:biodiversity.org.au:afd.taxon:f7bfd383-5501-4196-9acb-d9d4d03cc45d";
        NameSearchResult result = null;

        result = searcher.searchForCommonName(name);
        assertNotNull(result);
        assertEquals(MatchType.VERNACULAR, result.getMatchType());
        assertEquals(expectedLsid, result.getLsid());
    }

    @Test
    public void testVernacular5() throws Exception {
        String name = "River Saltbush";
        String expectedLsid = "http://id.biodiversity.org.au/node/apni/2887659";
        NameSearchResult result = null;

        result = searcher.searchForCommonName(name);
        assertNotNull(result);
        assertEquals(MatchType.VERNACULAR, result.getMatchType());
        assertEquals(expectedLsid, result.getLsid());
    }


    @Ignore // Requires indidgenous names
    @Test
    public void testVernacular6() throws Exception {
        String name = "Dharaban";
        String expectedLsid = "http://id.biodiversity.org.au/node/apni/2886278";
        NameSearchResult result = null;

        result = searcher.searchForCommonName(name);
        assertNotNull(result);
        assertEquals(MatchType.VERNACULAR, result.getMatchType());
        assertEquals(expectedLsid, result.getLsid());
    }


    @Test
    public void testVernacular7() throws Exception {
        String name = "T\u014crori";
        String expectedLsid = "http://id.biodiversity.org.au/node/apni/2900660";
        NameSearchResult result = null;

        result = searcher.searchForCommonName(name);
        assertNotNull(result);
        assertEquals(MatchType.VERNACULAR, result.getMatchType());
        assertEquals(expectedLsid, result.getLsid());
    }

}
