/*
 * Copyright (c) 2021 Atlas of Living Australia
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
        searcher = new ALANameSearcher("/data/lucene/namematching-20210811-2");
    }

    @Test
    public void testVernacular1() throws Exception {
        String name = "Mary River Turtle";
        String expectedLsid = "https://biodiversity.org.au/afd/taxa/d315deea-822c-4f2c-b439-da33d6af5fd6";
        NameSearchResult result = null;

        result = searcher.searchForCommonName(name);
        assertNotNull(result);
        assertEquals(expectedLsid, result.getLsid());
    }

    //@Ignore // Requires indigenous names
    @Test
    public void testVernacular2() throws Exception {
        String name = "Dhulwa";
        String expectedLsid = "https://id.biodiversity.org.au/node/apni/2904436";
        NameSearchResult result = null;

        result = searcher.searchForCommonName(name);
        assertNull(result); // Multiple matches
    }

    @Test
    public void testVernacular3() throws Exception {
        String name = "Drain Mangrovegoby";
        String expectedLsid = "https://biodiversity.org.au/afd/taxa/19c60dcd-93a0-40a2-9ac1-3abe7119c505";
        NameSearchResult result = null;

        result = searcher.searchForCommonName(name);
        assertNotNull(result);
        assertEquals(MatchType.VERNACULAR, result.getMatchType());
        assertEquals(expectedLsid, result.getLsid());
    }

    @Test
    public void testVernacular4() throws Exception {
        String name = "Onespine Unicornfish";
        String expectedLsid = "https://biodiversity.org.au/afd/taxa/f7bfd383-5501-4196-9acb-d9d4d03cc45d";
        NameSearchResult result = null;

        result = searcher.searchForCommonName(name);
        assertNotNull(result);
        assertEquals(MatchType.VERNACULAR, result.getMatchType());
        assertEquals(expectedLsid, result.getLsid());
    }

    @Test
    public void testVernacular5() throws Exception {
        String name = "Thread-Leaved Goodenia";
        String expectedLsid = "https://id.biodiversity.org.au/node/apni/2898188";
        NameSearchResult result = null;

        result = searcher.searchForCommonName(name);
        assertNotNull(result);
        assertEquals(MatchType.VERNACULAR, result.getMatchType());
        assertEquals(expectedLsid, result.getLsid());
    }


    @Ignore // Requires additonal indidgenous names
    @Test
    public void testVernacular6() throws Exception {
        String name = "Dharaban";
        String expectedLsid = "https://id.biodiversity.org.au/node/apni/2886278";
        NameSearchResult result = null;

        result = searcher.searchForCommonName(name);
        assertNotNull(result);
        assertEquals(MatchType.VERNACULAR, result.getMatchType());
        assertEquals(expectedLsid, result.getLsid());
    }


    @Test
    public void testVernacular7() throws Exception {
        String name = "T\u014crori";
        String expectedLsid = "https://id.biodiversity.org.au/node/apni/2900660";
        NameSearchResult result = null;

        result = searcher.searchForCommonName(name);
        assertNotNull(result);
        assertEquals(MatchType.VERNACULAR, result.getMatchType());
        assertEquals(expectedLsid, result.getLsid());
    }


    // Multiple names
    @Test
    public void testVernacular8() {
        String name = "White-spotted Skate";
        NameSearchResult result = this.searcher.searchForCommonName(name);
        assertNotNull(result);
        String lsid = result.getLsid();
        assertEquals("https://biodiversity.org.au/afd/taxa/e8e7ae96-a352-438c-8893-045ab8d5ce97", lsid);
    }

    // Conservation NSW
    @Test
    public void testVernacular9() {
        String name = "Velvet Wattle";
        NameSearchResult result = this.searcher.searchForCommonName(name);
        assertNull(result); // Multiple possibilties
        name = "Yass Daisy";
        result = this.searcher.searchForCommonName(name);
        assertNotNull(result);
        String lsid = result.getLsid();
        assertEquals("https://id.biodiversity.org.au/node/apni/2910323", lsid);
    }

    // Conservation Qld
    @Test
    public void testVernacular10() {
        String name = "Red Wattlebird";
        NameSearchResult result = this.searcher.searchForCommonName(name);
        assertNotNull(result);
        String lsid = result.getLsid();
        assertEquals("https://biodiversity.org.au/afd/taxa/8204979f-5302-41ea-943f-01d3c420f7bb", lsid);
    }


    // Conservation Aus
    @Test
    public void testVernacular11() {
        String name = "Mary River Turtle";
        NameSearchResult result = this.searcher.searchForCommonName(name);
        assertNotNull(result);
        String lsid = result.getLsid();
        assertEquals("https://biodiversity.org.au/afd/taxa/d315deea-822c-4f2c-b439-da33d6af5fd6", lsid);
    }
    
    // Sensitive NT
    @Test
    public void testVernacular12() {
        String name = "Atlas Moth";
        NameSearchResult result = this.searcher.searchForCommonName(name);
        assertNotNull(result);
        String lsid = result.getLsid();
        assertEquals("https://biodiversity.org.au/afd/taxa/8a05008e-53e8-4773-855a-43ac29cc6056", lsid);
    }

    // Sensitive Qld
    @Test
    public void testVernacular13() {
        String name = "Ravine Orchid";
        NameSearchResult result = this.searcher.searchForCommonName(name);
        assertNotNull(result);
        String lsid = result.getLsid();
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51413233", lsid);
    }

    // Sensitive SA
    @Test
    public void testVernacular14() {
        String name = "Osprey";
        NameSearchResult result = this.searcher.searchForCommonName(name);
        assertNull(result); // Multiple possibilitie
        name = "Swamp Bunnies";
        result = this.searcher.searchForCommonName(name);
        assertNotNull(result);
        String lsid = result.getLsid();
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51290607", lsid);
    }

    // Sensitive WA
    @Test
    public void testVernacular15() {
        String name = "Rakali";
        NameSearchResult result = this.searcher.searchForCommonName(name);
        assertNotNull(result);
        String lsid = result.getLsid();
        assertEquals("https://biodiversity.org.au/afd/taxa/9baa55f2-69c1-486c-91f7-f96e58b7c945", lsid);
    }

    // Advisory Vic
    @Test
    public void testVernacular16() {
        String name = "Robust Greenhood";
        NameSearchResult result = this.searcher.searchForCommonName(name);
        assertNotNull(result);
        String lsid = result.getLsid();
        assertEquals("https://id.biodiversity.org.au/taxon/apni/51412430", lsid);
    }

    // Advisory Vic
    @Test
    public void testVernacular17() {
        String name = "Little Tern";
        NameSearchResult result = this.searcher.searchForCommonName(name);
        assertNull(result); // Multiple results
        name = "Victoria Range Stringybark";
        result = this.searcher.searchForCommonName(name);
        assertNotNull(result);
        String lsid = result.getLsid();
        assertEquals("https://id.biodiversity.org.au/node/apni/2905514", lsid);
    }

    // Invasive species
    @Test
    public void testVernacular18() {
        String name = "Onion Fly";
        NameSearchResult result = this.searcher.searchForCommonName(name);
        assertNotNull(result);
        String lsid = result.getLsid();
        assertEquals("ALA_3855965", lsid);
    }

}
