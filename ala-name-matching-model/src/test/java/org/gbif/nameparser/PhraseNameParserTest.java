

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

package org.gbif.nameparser;

import au.org.ala.names.model.ALAParsedName;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test cases for the phrase name parser.
 * <p>
 * Apart from a few tests to make sure that the parser is not screwing up "normal" parsing,
 * these tests concentrate on
 * </p>
 */
public class PhraseNameParserTest {
    private PhraseNameParser parser;

    @Before
    public void setup() throws Exception {
        this.parser = new PhraseNameParser();
    }

    @Test
    public void testSimpleParse1() throws Exception {
        ParsedName pn = parser.parse("Ozothamnus diosmifolius");
        assertFalse(pn instanceof ALAParsedName);
        assertEquals("Ozothamnus", pn.getGenusOrAbove());
        assertEquals("diosmifolius", pn.getSpecificEpithet());
        assertEquals(NameType.SCIENTIFIC, pn.getType());
        assertNull(pn.getAuthorship());
        assertEquals("Ozothamnus diosmifolius", pn.canonicalName());
    }

    @Test
    public void testSimpleParse2() throws Exception {
        ParsedName pn = parser.parse("Ozothamnus");
        assertFalse(pn instanceof ALAParsedName);
        assertEquals("Ozothamnus", pn.getGenusOrAbove());
        assertNull(pn.getSpecificEpithet());
        assertEquals(NameType.SCIENTIFIC, pn.getType());
        assertNull(pn.getAuthorship());
        assertEquals("Ozothamnus", pn.canonicalName());
    }

    @Test
    public void testSpeciesPlaceholder1() throws Exception {
        ParsedName pn = parser.parse("Diaporthe species1");
        assertFalse(pn instanceof ALAParsedName);
        assertEquals("Diaporthe", pn.getGenusOrAbove());
        assertEquals("species1", pn.getSpecificEpithet());
        assertEquals(NameType.PLACEHOLDER, pn.getType());
        assertNull(pn.getAuthorship());
        assertEquals("Diaporthe species1", pn.canonicalName());
    }

    @Test
    public void testSpeciesPlaceholder2() throws Exception {
        ParsedName pn = parser.parse("Diaporthe species 1");
        assertFalse(pn instanceof ALAParsedName);
        assertEquals("Diaporthe", pn.getGenusOrAbove());
        assertEquals("species-1", pn.getSpecificEpithet());
        assertEquals(NameType.PLACEHOLDER, pn.getType());
        assertNull(pn.getAuthorship());
        assertEquals("Diaporthe species-1", pn.canonicalName());
    }

    @Test
    public void testSpeciesPlaceholder3() throws Exception {
        ParsedName pn = parser.parse("Diaporthe species-1");
        assertFalse(pn instanceof ALAParsedName);
        assertEquals("Diaporthe", pn.getGenusOrAbove());
        assertEquals("species-1", pn.getSpecificEpithet());
        assertEquals(NameType.PLACEHOLDER, pn.getType());
        assertNull(pn.getAuthorship());
        assertEquals("Diaporthe species-1", pn.canonicalName());
    }

}
