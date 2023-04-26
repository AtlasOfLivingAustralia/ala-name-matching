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

package au.org.ala.names.index.provider;

import au.org.ala.names.index.*;
import au.org.ala.names.model.VernacularType;
import au.org.ala.names.util.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Test cases for {@link MatchVernacularCondition}
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class MatchVernacularConditionTest extends TestUtils {
    private NameProvider provider;
    private ALANameAnalyser analyser;

    @Before
    public void setup() {
        this.analyser = new ALANameAnalyser();
        this.provider = new NameProvider();
        this.provider.addLocation(new Location("L-1", null, "Australia", "country", 1.0, Arrays.asList("C-1"), Arrays.asList("AU")));
        this.provider.addLocation(new Location("L-2", "L-1", "New South Wales", "stateProvince", 1.0, Arrays.asList("S-1"), Arrays.asList("NSW")));
        this.provider.addLocation(new Location("L-3", null, "France", "country", 1.0, Arrays.asList("C-2"), Arrays.asList("FR")));
       this.provider.postLocationLoad();
    }

    @Test
    public void testMatch1() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setVernacularName("Dingo");
        VernacularName name = this.createVernacular("ID-1", "Dingo", this.provider, VernacularType.PREFERRED, true, null, null);
        assertTrue(condition.match(name, this.provider));
    }


    @Test
    public void testMatch2() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setVernacularName("A Dingo");
        VernacularName name = this.createVernacular("ID-1", "Dingo", this.provider, VernacularType.PREFERRED, true, null, null);
        assertFalse(condition.match(name, this.provider));
    }

    @Test
    public void testMatch3() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setVernacularName("DINGO");
        condition.setMatchType(NameMatchType.INSENSITIVE);
        VernacularName name = this.createVernacular("ID-1", "Dingo", this.provider, VernacularType.PREFERRED, true, null, null);
        assertTrue(condition.match(name, this.provider));
    }

    @Test
    public void testMatch4() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setVernacularName("A fish");
        condition.setMatchType(NameMatchType.NORMALISED);
        VernacularName name = this.createVernacular("ID-1", "a  fish", this.provider, VernacularType.PREFERRED, true, null, null);
        assertTrue(condition.match(name, this.provider));
    }

    @Test
    public void testMatch5() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setVernacularName("A.*fish");
        condition.setMatchType(NameMatchType.REGEX);
        VernacularName name = this.createVernacular("ID-1", "Also the fish", this.provider, VernacularType.PREFERRED, true, null, null);
        assertTrue(condition.match(name, this.provider));
    }


    @Test
    public void testMatch6() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setVernacularName("A.*fish");
        condition.setMatchType(NameMatchType.REGEX);
        VernacularName name = this.createVernacular("ID-1", "Also a bit fishy", this.provider, VernacularType.PREFERRED, true, null, null);
        assertFalse(condition.match(name, this.provider));
    }


    @Test
    public void testMatch7() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setStatus(VernacularType.PREFERRED);
        VernacularName name = this.createVernacular("ID-1", "Kangaroo", this.provider, VernacularType.PREFERRED, true, null, null);
        assertTrue(condition.match(name, this.provider));
    }

    @Test
    public void testMatch8() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setStatus(VernacularType.STANDARD);
        VernacularName name = this.createVernacular("ID-1", "Kangaroo", this.provider, VernacularType.STANDARD, true, null, null);
        assertTrue(condition.match(name, this.provider));
    }


    @Test
    public void testMatch9() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setStatus(VernacularType.STANDARD);
        VernacularName name = this.createVernacular("ID-1", "Kangaroo", this.provider, VernacularType.PREFERRED, true, null, null);
        assertFalse(condition.match(name, this.provider));
    }

    @Test
    public void testMatch10() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setPreferredName(true);
        VernacularName name = this.createVernacular("ID-1", "Kangaroo", this.provider, VernacularType.PREFERRED, true, null, null);
        assertTrue(condition.match(name, this.provider));
    }

    @Test
    public void testMatch11() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setPreferredName(false);
        VernacularName name = this.createVernacular("ID-1", "Kangaroo", this.provider, VernacularType.PREFERRED, true, null, null);
        assertFalse(condition.match(name, this.provider));
    }

    @Test
    public void testMatch12() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setLanguage("en");
        VernacularName name = this.createVernacular("ID-1", "Kangaroo", this.provider, VernacularType.PREFERRED, true, null, null);
        assertFalse(condition.match(name, this.provider));
    }

    @Test
    public void testMatch13() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setLanguage("en");
        VernacularName name = this.createVernacular("ID-1", "Kangaroo", this.provider, VernacularType.PREFERRED, true, "en", null);
        assertTrue(condition.match(name, this.provider));
    }

    @Test
    public void testMatch14() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setLanguage("fr");
        VernacularName name = this.createVernacular("ID-1", "Kangaroo", this.provider, VernacularType.PREFERRED, true, "en", null);
        assertFalse(condition.match(name, this.provider));
    }

    @Test
    public void testMatch15() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setLanguage("en");
        condition.setMatchType(NameMatchType.INSENSITIVE);
        VernacularName name = this.createVernacular("ID-1", "Kangaroo", this.provider, VernacularType.PREFERRED, true, "EN", null);
        assertTrue(condition.match(name, this.provider));
    }

    @Test
    public void testMatch16() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setLanguage("en(-[A-Z]+)?");
        condition.setMatchType(NameMatchType.REGEX);
        VernacularName name = this.createVernacular("ID-1", "Kangaroo", this.provider, VernacularType.PREFERRED, true, "EN", null);
        assertTrue(condition.match(name, this.provider));
    }

    @Test
    public void testMatch17() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setLanguage("en(-[A-Z]+)?");
        condition.setMatchType(NameMatchType.REGEX);
        VernacularName name = this.createVernacular("ID-1", "Kangaroo", this.provider, VernacularType.PREFERRED, true, "EN-AU", null);
        assertTrue(condition.match(name, this.provider));
    }

    @Test
    public void testMatch18() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setLanguage("en(-[A-Z]+)?");
        condition.setMatchType(NameMatchType.REGEX);
        VernacularName name = this.createVernacular("ID-1", "Kangaroo", this.provider, VernacularType.PREFERRED, true, "EN-99", null);
        assertFalse(condition.match(name, this.provider));
    }

    @Test
    public void testMatch19() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setLocality("AU");
        VernacularName name = this.createVernacular("ID-1", "Kangaroo", this.provider, VernacularType.PREFERRED, true, "EN-99", null);
        assertFalse(condition.match(name, this.provider));
    }

    @Test
    public void testMatch20() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setLocality("Australia");
        VernacularName name = this.createVernacular("ID-1", "Kangaroo", this.provider, VernacularType.PREFERRED, true, "EN-99", "NSW");
        assertTrue(condition.match(name, this.provider));
    }

    @Test
    public void testMatch21() {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setLocality("Australia");
        VernacularName name = this.createVernacular("ID-1", "Kangaroo", this.provider, VernacularType.PREFERRED, true, "EN-99", "France");
        assertFalse(condition.match(name, this.provider));
    }

    @Test
    public void testWrite1() throws Exception {
        MatchVernacularCondition condition = new MatchVernacularCondition();
        condition.setVernacularName("Dingo");
        condition.setStatus(VernacularType.LOCAL);
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, condition);
        assertEquals(this.loadResource("match-vernacular-condition-1.json"), writer.toString());
    }

    @Test
    public void testRead1() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        MatchVernacularCondition condition = mapper.readValue(this.resourceReader("match-vernacular-condition-1.json"), MatchVernacularCondition.class);
        VernacularName name = this.createVernacular("ID-1", "Dingo", this.provider, VernacularType.LOCAL, true, null, null);
        assertTrue(condition.match(name, this.provider));
        name = this.createVernacular("ID-1", "Dingo", this.provider, VernacularType.COMMON, true, null, null);
        assertFalse(condition.match(name, this.provider));
    }


}
