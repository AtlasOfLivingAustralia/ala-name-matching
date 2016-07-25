package au.org.ala.names.search;

import au.org.ala.names.model.*;
import org.gbif.ecat.voc.NameType;
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
@Ignore
public class VernacularMatchTest {

    private static ALANameSearcher searcher;

    @org.junit.BeforeClass
    public static void init() {
        try {
            searcher = new ALANameSearcher("/data/lucene/test");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testVernacular1() throws Exception {
        String name = "Casuarina cunninghamiana";
        String expectedLsid = "http://id.biodiversity.org.au/node/apni/2904436";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        MetricsResultDTO result = null;

        cl.setScientificName(name);
        result = searcher.searchForRecordMetrics(cl, true, true);
        assertNotNull(result);
        assertNull(result.getLastException());
        assertEquals(MatchType.EXACT, result.getResult().getMatchType());
        assertEquals(expectedLsid, result.getResult().getLsid());
    }

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
        String name = "River Sheoak";
        String expectedLsid = "http://id.biodiversity.org.au/node/apni/2904436";
        NameSearchResult result = null;

        result = searcher.searchForCommonName(name);
        assertNotNull(result);
        assertEquals(MatchType.VERNACULAR, result.getMatchType());
        assertEquals(expectedLsid, result.getLsid());
    }

    @Test
    public void testVernacular4() throws Exception {
        String name = "River Oak";
        String expectedLsid = "http://id.biodiversity.org.au/node/apni/2904436";
        NameSearchResult result = null;

        result = searcher.searchForCommonName(name);
        assertNotNull(result);
        assertEquals(MatchType.VERNACULAR, result.getMatchType());
        assertEquals(expectedLsid, result.getLsid());
    }

    @Test
    public void testVernacular5() throws Exception {
        String name = "Microseris lanceolata";
        String expectedLsid = "http://id.biodiversity.org.au/node/apni/2886278";
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        MetricsResultDTO result = null;

        cl.setScientificName(name);
        result = searcher.searchForRecordMetrics(cl, true, true);
        assertNotNull(result);
        assertNull(result.getLastException());
        assertEquals(MatchType.EXACT, result.getResult().getMatchType());
        assertEquals(expectedLsid, result.getResult().getLsid());
    }


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
}
