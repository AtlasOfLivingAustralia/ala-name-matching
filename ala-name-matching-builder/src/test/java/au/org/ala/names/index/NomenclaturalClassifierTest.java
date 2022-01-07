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

package au.org.ala.names.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.*;

public class NomenclaturalClassifierTest {

    @Test
    public void testFind1() {
        NomenclaturalClassifier classifier = NomenclaturalClassifier.find("ICBN");
        assertEquals("ICBN", classifier.getAcronym());
        assertEquals("International Code of Botanical Nomenclature", classifier.getTitle());
        assertNotNull(classifier.getSource());
        assertEquals("http://ibot.sav.sk/icbn/main.htm", classifier.getSource().toString());
        assertNotNull(classifier.getAliases());
        assertEquals(2, classifier.getAliases().size());
        assertTrue(classifier.getAliases().contains("ICN"));
        assertNotNull(classifier.getParent());
        assertEquals("EUK", classifier.getParent().getAcronym());
        assertEquals(NomenclaturalCode.BOTANICAL, classifier.getCode());
    }

    @Test
    public void testFind2() {
        NomenclaturalClassifier classifier = NomenclaturalClassifier.find("ICN");
        assertEquals("ICBN", classifier.getAcronym());
    }

    @Test
    public void testFind3() {
        NomenclaturalClassifier classifier = NomenclaturalClassifier.find("ICNB");
        assertEquals("ICNB", classifier.getAcronym());
        assertEquals("International Code of Nomenclature of Bacteria", classifier.getTitle());
        assertNotNull(classifier.getSource());
        assertEquals("http://www.ncbi.nlm.nih.gov/books/NBK8808/", classifier.getSource().toString());
        assertNotNull(classifier.getAliases());
        assertEquals(1, classifier.getAliases().size());
        assertTrue(classifier.getAliases().contains("NCGN"));
        assertNull(classifier.getParent());
        assertEquals(NomenclaturalCode.BACTERIAL, classifier.getCode());
    }

    @Test
    public void testFind4() {
        NomenclaturalClassifier classifier = NomenclaturalClassifier.find("XXXX");
        assertNull(classifier);
    }


    @Test
    public void testFind5() {
        NomenclaturalClassifier classifier = NomenclaturalClassifier.find("BOTANICAL");
        assertNotNull(classifier);
        assertEquals("ICBN", classifier.getAcronym());
    }

    @Test
    public void testFind6() {
        NomenclaturalClassifier classifier = NomenclaturalClassifier.find(NomenclaturalCode.ZOOLOGICAL);
        assertNotNull(classifier);
        assertEquals("ICZN", classifier.getAcronym());
    }

    @Test
    public void testToString1() {
        NomenclaturalClassifier classifier = NomenclaturalClassifier.find("ICBN");
        assertEquals("ICBN", classifier.toString());
    }

    @Test
    public void testToJson1() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        NomenclaturalClassifier classifier = NomenclaturalClassifier.ZOOLOGICAL;
        mapper.writeValue(sw, classifier);
        assertEquals("\"ICZN\"", sw.toString());
    }

    @Test
    public void testFromJson1() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        NomenclaturalClassifier classifier = mapper.readValue("\"ICZN\"", NomenclaturalClassifier.class);
        assertNotNull(classifier);
        assertEquals("ICZN", classifier.getAcronym());
    }


    @Test
    public void testFromJson2() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        NomenclaturalClassifier classifier = mapper.readValue("null", NomenclaturalClassifier.class);
        assertNull(classifier);
    }


}