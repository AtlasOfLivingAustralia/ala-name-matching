package au.org.ala.names.search;

import au.org.ala.names.model.LinnaeanRankClassification;
import au.org.ala.names.model.MatchMetrics;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the rank classification
 */
public class MatchMetricsTest {
    private static final float MATCH_TOLERANCE = 0.01f;
    private static final LinnaeanRankClassification CLASS1 = new LinnaeanRankClassification("Animalia", "Arthropoda", "Insecta", "Hymenoptra", "Formicidae", "Huberria", "Huberia striata", "(Smith, 1876)");
    private static final LinnaeanRankClassification CLASS2 = new LinnaeanRankClassification("Charophyta", "Arthropoda", "Equisetopsida", "Gentianales", "Apocynaceae", "Oxypetalum", "Oxypetalum caeruleum", "(D.Don) Decne.");
    private MatchMetrics metrics;

    @Before
    public void setup() {
        this.metrics = new MatchMetrics();
    }

    @Test
    public void testComputeMatch1() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        this.metrics.computeMatch(query, result, false);
        assertEquals(1.0, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch2() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        query.setKingdom(null);
        this.metrics.computeMatch(query, result, false);
        assertEquals(1.0, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch3() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        query.setPhylum(null);
        this.metrics.computeMatch(query, result, false);
        assertEquals(1.0, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch4() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        query.setKlass(null);
        this.metrics.computeMatch(query, result, false);
        assertEquals(1.0, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch5() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        query.setOrder(null);
        this.metrics.computeMatch(query, result, false);
        assertEquals(1.0, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch6() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        query.setFamily(null);
        this.metrics.computeMatch(query, result, false);
        assertEquals(1.0, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch7() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        query.setGenus(null);
        this.metrics.computeMatch(query, result, false);
        assertEquals(1.0, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch8() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        query.setScientificName(null);
        this.metrics.computeMatch(query, result, false);
        assertEquals(1.0, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch9() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        query.setAuthorship(null);
        this.metrics.computeMatch(query, result, false);
        assertEquals(1.0, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch10() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        result.setKingdom(null);
        this.metrics.computeMatch(query, result, false);
        assertEquals(0.746, metrics.getMatch(), MATCH_TOLERANCE);
    }


    @Test
    public void testComputeMatch11() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        result.setKingdom("Plantae");
        this.metrics.computeMatch(query, result, false);
        assertEquals(0.816, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch12() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        result.setPhylum(null);
        this.metrics.computeMatch(query, result, false);
        assertEquals(0.976, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch13() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        result.setPhylum("Chordata");
        this.metrics.computeMatch(query, result, false);
        assertEquals(0.958, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch14() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        result.setPhylum("ARTHROPODA");
        this.metrics.computeMatch(query, result, false);
        assertEquals(1.0, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch15() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        result.setKlass("Hexapodia");
        this.metrics.computeMatch(query, result, false);
        assertEquals(0.947, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch16() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        result.setFamily("PERIPATOPSIDAE");
        this.metrics.computeMatch(query, result, false);
        assertEquals(0.942, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch17() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        result.setGenus("Vescerro");
        this.metrics.computeMatch(query, result, false);
        assertEquals(0.929, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch18() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        query.setSpecificEpithet("striata");
        result.setSpecificEpithet("trigona");
        this.metrics.computeMatch(query, result, false);
        assertEquals(0.975, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch19() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        result.setAuthorship("Smith, 1876");
        this.metrics.computeMatch(query, result, false);
        assertEquals(1.0, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch20() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        result.setAuthorship("Smith");
        this.metrics.computeMatch(query, result, false);
        assertEquals(1.0, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch21() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        result.setAuthorship("Jones, 1876");
        this.metrics.computeMatch(query, result, false);
        assertEquals(0.854, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch22() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        result.setAuthorship("Jones");
        this.metrics.computeMatch(query, result, false);
        assertEquals(0.756, metrics.getMatch(), MATCH_TOLERANCE);
    }

    /**
     * Testing bad authors without much context, should result in a lowered match.
     *
     * @throws Exception
     */
    @Test
    public void testComputeMatch23() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        query.setKingdom(null);
        query.setPhylum(null);
        query.setKlass(null);
        query.setOrder(null);
        query.setFamily(null);
        query.setGenus(null);
        result.setAuthorship("Smith, 1876");
        this.metrics.computeMatch(query, result, false);
        assertEquals(1.0, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch24() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        query.setKingdom(null);
        query.setPhylum(null);
        query.setKlass(null);
        query.setOrder(null);
        query.setFamily(null);
        query.setGenus(null);
        result.setAuthorship("Smith");
        this.metrics.computeMatch(query, result, false);
        assertEquals(1.0, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch25() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        query.setKingdom(null);
        query.setPhylum(null);
        query.setKlass(null);
        query.setOrder(null);
        query.setFamily(null);
        query.setGenus(null);
        result.setAuthorship("Jones, 1876");
        this.metrics.computeMatch(query, result, false);
        assertEquals(0.554, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatch26() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        query.setKingdom(null);
        query.setPhylum(null);
        query.setKlass(null);
        query.setOrder(null);
        query.setFamily(null);
        query.setGenus(null);
        result.setAuthorship("Jones");
        this.metrics.computeMatch(query, result, false);
        assertEquals(0.255, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatchSynonym1() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS2);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS2);
        result.setKingdom(null);
        result.setPhylum(null);
        result.setKlass(null);
        result.setOrder(null);
        result.setFamily(null);
        result.setGenus(null);
        this.metrics.computeMatch(query, result, true);
        assertEquals(0.655, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatchSynonym2() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS2);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS2);
        result.setKingdom(null);
        result.setPhylum(null);
        result.setKlass(null);
        result.setOrder(null);
        result.setFamily(null);
        this.metrics.computeMatch(query, result, true);
        assertEquals(0.675, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatchSynonym3() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS2);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS2);
        result.setKingdom(null);
        result.setPhylum(null);
        result.setKlass(null);
        result.setOrder(null);
        result.setFamily(null);
        result.setAuthorship(null);
        this.metrics.computeMatch(query, result, true);
        assertEquals(0.482, metrics.getMatch(), MATCH_TOLERANCE);
    }

    @Test
    public void testComputeMatchSynonym4() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS2);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS2);
        result.setKingdom(null);
        result.setPhylum(null);
        result.setKlass(null);
        result.setOrder(null);
        result.setFamily(null);
        query.setAuthorship(null);
        result.setAuthorship(null);
        this.metrics.computeMatch(query, result, true);
        assertEquals(0.471, metrics.getMatch(), MATCH_TOLERANCE);
    }


    /**
     * Test match computation takes less than 1us per match for simple cases
     *
     * @throws Exception
     */
    @Test
    public void testComputeMatchTiming1() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++)
            this.metrics.computeMatch(query, result, false);
        assertEquals(1.0, metrics.getMatch(), MATCH_TOLERANCE);
        long time = System.currentTimeMillis() - start;
        assertTrue("Took " + time + "ms. Required to be less than 1000ms", time < 1000);
    }

    /**
     * Test match computation takes less than 1us per match for simple cases
     *
     * @throws Exception
     */
    @Test
    public void testComputeMatchTiming2() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        query.setKingdom(null);
        query.setPhylum(null);
        query.setKlass(null);
        query.setOrder(null);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++)
            this.metrics.computeMatch(query, result, false);
        assertEquals(1.0, metrics.getMatch(), MATCH_TOLERANCE);
        long time = System.currentTimeMillis() - start;
        assertTrue("Took " + time + "ms. Required to be less than 1000ms", time < 1000);
    }

    /**
     * Test match computation takes less than 10us per match for one bodgy result
     *
     * @throws Exception
     */
    @Test
    public void testComputeMatchTiming3() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        result.setPhylum("Chordata");
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++)
            this.metrics.computeMatch(query, result, false);
        assertEquals(0.958, metrics.getMatch(), MATCH_TOLERANCE);
        long time = System.currentTimeMillis() - start;
        assertTrue("Took " + time + "ms. Required to be less than 1000ms", time < 1000);
    }

    /**
     * Test match computation takes less than 10us per match for two bodgy results
     *
     * @throws Exception
     */
    @Test
    public void testComputeMatchTiming4() throws Exception {
        LinnaeanRankClassification query = new LinnaeanRankClassification(CLASS1);
        LinnaeanRankClassification result = new LinnaeanRankClassification(CLASS1);
        result.setPhylum("Chordata");
        result.setGenus("Acacia");
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++)
            this.metrics.computeMatch(query, result, false);
        assertEquals(0.873, metrics.getMatch(), MATCH_TOLERANCE);
        long time = System.currentTimeMillis() - start;
        assertTrue("Took " + time + "ms. Required to be less than 1000ms", time < 1000);
    }


}
