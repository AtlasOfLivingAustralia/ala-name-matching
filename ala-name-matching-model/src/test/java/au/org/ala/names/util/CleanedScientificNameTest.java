

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

package au.org.ala.names.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test cases for cleaned scientific names
 */
public class CleanedScientificNameTest {

    @Test
    public void testBasicName1() {
        CleanedScientificName name = new CleanedScientificName("Ozothamnus diosmifolius");
        assertEquals("Ozothamnus diosmifolius", name.getName());
        assertEquals("Ozothamnus diosmifolius", name.getNormalised());
        assertEquals("Ozothamnus diosmifolius", name.getBasic());
        assertFalse(name.hasNormalised());
        assertFalse(name.hasBasic());
    }

    @Test
    public void testBasicName2() {
        CleanedScientificName name = new CleanedScientificName(" Ozothamnus     diosmifolius   ");
        assertEquals("Ozothamnus diosmifolius", name.getName());
        assertEquals("Ozothamnus diosmifolius", name.getNormalised());
        assertEquals("Ozothamnus diosmifolius", name.getBasic());
        assertFalse(name.hasNormalised());
        assertFalse(name.hasBasic());
    }

    @Test
    public void testBasicName3() {
        CleanedScientificName name = new CleanedScientificName(" Ozothamnus     diosmifolius  'Adelaide White' ");
        assertEquals("Ozothamnus diosmifolius 'Adelaide White'", name.getName());
        assertEquals("Ozothamnus diosmifolius 'Adelaide White'", name.getNormalised());
        assertEquals("Ozothamnus diosmifolius 'Adelaide White'", name.getBasic());
        assertFalse(name.hasNormalised());
        assertFalse(name.hasBasic());
    }

    @Test
    public void testBasicName4() {
        CleanedScientificName name = new CleanedScientificName("Ozothamnus\u00a0diosmifolius");
        assertEquals("Ozothamnus diosmifolius", name.getName());
        assertEquals("Ozothamnus diosmifolius", name.getNormalised());
        assertEquals("Ozothamnus diosmifolius", name.getBasic());
        assertFalse(name.hasNormalised());
        assertFalse(name.hasBasic());
    }

    @Test
    public void testPunctName1() {
        CleanedScientificName name = new CleanedScientificName("Ozothamnus diosmifolius \u2018Adelaide White\u2019");
        assertEquals("Ozothamnus diosmifolius \u2018Adelaide White\u2019", name.getName());
        assertEquals("Ozothamnus diosmifolius 'Adelaide White'", name.getNormalised());
        assertEquals("Ozothamnus diosmifolius 'Adelaide White'", name.getBasic());
        assertTrue(name.hasNormalised());
        assertFalse(name.hasBasic());
    }

    @Test
    public void testPunctName2() {
        CleanedScientificName name = new CleanedScientificName("Ozothamnus diosmifolius \u201cAdelaide White\u201d");
        assertEquals("Ozothamnus diosmifolius \u201cAdelaide White\u201d", name.getName());
        assertEquals("Ozothamnus diosmifolius \"Adelaide White\"", name.getNormalised());
        assertEquals("Ozothamnus diosmifolius \"Adelaide White\"", name.getBasic());
        assertTrue(name.hasNormalised());
        assertFalse(name.hasBasic());
    }

    @Test
    public void testPunctName3() {
        CleanedScientificName name = new CleanedScientificName("Bernhardia novae\u2010hollandiae");
        assertEquals("Bernhardia novae\u2010hollandiae", name.getName());
        assertEquals("Bernhardia novae\u002dhollandiae", name.getNormalised());
        assertEquals("Bernhardia novae\u002dhollandiae", name.getBasic());
        assertTrue(name.hasNormalised());
        assertFalse(name.hasBasic());
    }

    @Test
    public void testPunctName4() {
        CleanedScientificName name = new CleanedScientificName("Oribatida \u2013 astigmata");
        assertEquals("Oribatida \u2013 astigmata", name.getName());
        assertEquals("Oribatida - astigmata", name.getNormalised());
        assertEquals("Oribatida - astigmata", name.getBasic());
        assertTrue(name.hasNormalised());
        assertFalse(name.hasBasic());
    }

    @Test
    public void testPunctName5() {
        CleanedScientificName name = new CleanedScientificName("Olearia\u00a0\u00d7matthewsii");
        assertEquals("Olearia \u00d7matthewsii", name.getName());
        assertEquals("Olearia \u00d7matthewsii", name.getNormalised());
        assertEquals("Olearia x matthewsii", name.getBasic());
        assertFalse(name.hasNormalised());
        assertTrue(name.hasBasic());
    }

    @Test
    public void testAccentedName1() {
        CleanedScientificName name = new CleanedScientificName("Aleochara haemorrho\u00efdalis");
        assertEquals("Aleochara haemorrho\u00efdalis", name.getName());
        assertEquals("Aleochara haemorrho\u00efdalis", name.getNormalised());
        assertEquals("Aleochara haemorrhoidalis", name.getBasic());
        assertFalse(name.hasNormalised());
        assertTrue(name.hasBasic());
    }

    @Test
    public void testAccentedName2() {
        CleanedScientificName name = new CleanedScientificName("Staurastrum subbr\u00e9bissonii");
        assertEquals("Staurastrum subbr\u00e9bissonii", name.getName());
        assertEquals("Staurastrum subbr\u00e9bissonii", name.getNormalised());
        assertEquals("Staurastrum subbrebissonii", name.getBasic());
        assertFalse(name.hasNormalised());
        assertTrue(name.hasBasic());
    }

    @Test
    public void testGreekName1() {
        CleanedScientificName name = new CleanedScientificName("Senecio banksii var. \u03b2 velleia");
        assertEquals("Senecio banksii var. \u03b2 velleia", name.getName());
        assertEquals("Senecio banksii var. \u03b2 velleia", name.getNormalised());
        assertEquals("Senecio banksii var. beta velleia", name.getBasic());
        assertFalse(name.hasNormalised());
        assertTrue(name.hasBasic());
    }

}
