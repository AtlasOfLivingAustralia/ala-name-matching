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

import au.org.ala.names.model.*;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.nameparser.PhraseNameParser;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;

public class AutocompleteTest {
    private static ALANameSearcher searcher;

    @org.junit.BeforeClass
    public static void init() throws Exception {
        searcher = new ALANameSearcher("/data/lucene/namematching-20210811-2");
    }

    @Test
    public void testAutocomplete1() throws Exception {
        List<Map> results = searcher.autocomplete("Elusor", 10, false);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        Map first = results.get(0);
        assertEquals("Elusor", first.get("name"));
    }

    @Test
    public void testAutocomplete2() throws Exception {
        List<Map> results = searcher.autocomplete("Mary riv", 10, false);
        assertNotNull(results);
        assertTrue(results.size() > 1);
        Map first = results.get(0);
        assertEquals("Samadera sp. Mary River", first.get("name"));
        Map second = results.get(1);
        assertEquals("Mary River Cod", second.get("commonname"));
        assertEquals("Maccullochella mariensis", second.get("name"));
        Map third = results.get(2);
        assertEquals("Mary River Turtle", third.get("commonname"));
        assertEquals("Elusor macrurus", third.get("name"));
    }

    @Test
    public void testAutocomplete3() throws Exception {
        List<Map> results = searcher.autocomplete("Mary river t", 10, false);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        Map first = results.get(0);
        assertEquals("Mary River Turtle", first.get("commonname"));
        assertEquals("Elusor macrurus", first.get("name"));
    }

    @Test
    public void testAutocomplete4() throws Exception {
        List<Map> results = searcher.autocomplete("Acacia", 10, true);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        Map first = results.get(0);
        assertEquals("Acacia", first.get("name"));
    }

    @Test
    public void testAutocomplete5() throws Exception {
        List<Map> results = searcher.autocomplete("Acacia d", 10, true);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        Map first = results.get(0);
        assertEquals("Acacia dampieri", first.get("name"));
    }

    @Test
    public void testAutocomplete6() throws Exception {
        List<Map> results = searcher.autocomplete("Mylitta pse", 10, true);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        Map first = results.get(0);
        assertEquals("Hysterangium pseudacaciae", first.get("name"));
        assertNotNull(first.get("synonymMatch"));
    }


    @Test
    public void testAutocomplete7() throws Exception {
        // No match with synonym
        List<Map> results = searcher.autocomplete("Mylitta pse", 10, false);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }


    @Test
    public void testAutocomplete8() throws Exception {
        // No match with garbage
        List<Map> results = searcher.autocomplete("Glurglefkluff11", 10, true);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testAutocomplete9() throws Exception {
        List<Map> results = searcher.autocomplete("Osphra", 10, true);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        Map first = results.get(0);
        assertEquals("Osphranter", first.get("name"));
    }

    @Test
    public void testAutocomplete10() throws Exception {
        List<Map> results = searcher.autocomplete("Rossi", 10, true);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertTrue(results.stream().anyMatch(r -> "Pleurotomella rossi".equals(r.get("name"))));
        assertTrue(results.stream().anyMatch(r -> "Metacineta rossica".equals(r.get("name"))));
    }


    @Test
    public void testAutocomplete11() throws Exception {
        List<Map> results = searcher.autocomplete("rush", 10, false);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        Map first = results.get(0);
        assertEquals("Juncus", first.get("name"));
        assertEquals("Rushes", first.get("commonname"));
    }


    @Test
    public void testAutocomplete12() throws Exception {
        List<Map> results = searcher.autocomplete("rush li", 10, true);
        assertNotNull(results);
        assertTrue(results.size() > 2);
        Optional<Map> syn = results.stream().filter(r -> "Sisyrinchium micranthum".equals(r.get("name"))).findFirst();
        assertTrue(syn.isPresent());
        assertEquals("Yellow Rush Lily", syn.get().get("commonname"));
    }

}