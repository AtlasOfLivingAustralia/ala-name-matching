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

import au.org.ala.names.util.TestUtils;
import org.gbif.api.model.registry.Contact;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.model.Equality;
import org.junit.Test;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Test cases for {@link TaxonomyConfiguration}
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class TaxonomyConfiugrationTest extends TestUtils {
    private static final String CID = "CID";
    private static final String CTITLE = "A Title";
    private static final String CDESC = "A description";
    private static final String CFN = "Arnold";
    private static final String CLN = "Sideways";
    private static final String COR = "Nowhere.org";
    private static final String ID_1 = "ID-1";
    private static final String ID_2 = "ID-2";
    private static final String NAME_1 = "Acacia dealbata";
    private static final int PRIORITY_1 = 150;


    @Test
    public void testWrite1() throws Exception {
        NameProvider provider1 = new NameProvider(ID_1, PRIORITY_1);
        TaxonomyConfiguration config = new TaxonomyConfiguration();
        config.id = CID;
        config.name = CTITLE;
        config.description = CDESC;
        config.providers = Arrays.asList(provider1);
        config.defaultProvider = provider1;
        config.nameAnalyserClass = ALANameAnalyser.class;
        config.acceptedCutoff = 50;
        StringWriter sw = new StringWriter();
        config.write(sw);
        assertEquals(this.loadResource("taxonomy-config-1.json"), sw.toString());
    }


    @Test
    public void testWrite2() throws Exception {
        Contact contact1 = new Contact();
        contact1.setFirstName(CFN);
        contact1.setLastName(CLN);
        contact1.setOrganization(COR);
        NameProvider provider1 = new NameProvider(ID_1, PRIORITY_1);
        TaxonomyConfiguration config = new TaxonomyConfiguration();
        config.id = CID;
        config.contact = contact1;
        config.providers = Arrays.asList(provider1);
        config.defaultProvider = provider1;
        config.nameAnalyserClass = ALANameAnalyser.class;
        config.acceptedCutoff = 50;
        StringWriter sw = new StringWriter();
        config.write(sw);
        assertEquals(this.loadResource("taxonomy-config-3.json"), sw.toString());
    }

    @Test
    public void testGetContact1() throws Exception {
        Contact contact1 = new Contact();
        contact1.setFirstName(CFN);
        contact1.setLastName(CLN);
        contact1.setOrganization(COR);
        TaxonomyConfiguration config = new TaxonomyConfiguration();
        config.id = CID;
        config.contact = contact1;
        assertEquals(CFN + " " + CLN + ", " + COR, config.getContactName());
    }

    @Test
    public void testGetContact2() throws Exception {
        Contact contact1 = new Contact();
        contact1.setFirstName(CFN);
        contact1.setLastName(CLN);
        TaxonomyConfiguration config = new TaxonomyConfiguration();
        config.id = CID;
        config.contact = contact1;
        assertEquals(CFN + " " + CLN, config.getContactName());
    }

    @Test
    public void testGetContact3() throws Exception {
        Contact contact1 = new Contact();
        contact1.setLastName(CLN);
        TaxonomyConfiguration config = new TaxonomyConfiguration();
        config.id = CID;
        config.contact = contact1;
        assertEquals(CLN, config.getContactName());
    }

    @Test
    public void testRead1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-1.json"));
        assertEquals(CID, config.id);
        assertEquals(CTITLE, config.name);
        assertEquals(CDESC, config.description);
        assertNotNull(config.providers);
        assertEquals(1, config.providers.size());
        NameProvider provider = config.providers.get(0);
        assertEquals(ID_1, provider.getId());
        assertEquals(ALANameAnalyser.class, config.nameAnalyserClass);
        TaxonConceptInstance instance = this.createInstance(ID_2, NomenclaturalClassifier.BOTANICAL, NAME_1, provider);
        assertEquals(PRIORITY_1, provider.computeScore(instance));
        assertEquals(provider, config.defaultProvider);
    }

    @Test
    public void testRead2() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-3.json"));
        assertEquals(CID, config.id);
        assertNotNull(config.providers);
        assertNotNull(config.contact);
        assertEquals(CFN, config.contact.getFirstName());
        assertEquals(CLN, config.contact.getLastName());
        assertEquals(COR, config.contact.getOrganization());
    }

    @Test
    public void testPriorities1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        Properties properties = config.getPriorities();
        assertNotNull(properties.getProperty("dr100"));
        assertEquals(1.5, Double.parseDouble(properties.getProperty("dr100")), 0.01);
        assertNotNull(properties.getProperty("dr100"));
        assertEquals(1.5 * 125 / 150, Double.parseDouble(properties.getProperty("dr101")), 0.01);
        assertNull(properties.getProperty("drNotADataset"));
    }

    @Test
    public void testNewAuthorMap1() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-2.json"));
        AuthorComparator comparator = config.newAuthorComparator();

        assertEquals(Equality.EQUAL, comparator.compare("Linnaeus", null, "Linnaeus", null));
        assertEquals(Equality.EQUAL, comparator.compare("L.", null, "Linnaeus", null));
        assertEquals(Equality.DIFFERENT, comparator.compare("Linnaeus", null, "Fred", null));
        assertEquals(Equality.DIFFERENT, comparator.compare("Sweet", null, "Sw.", null));
        assertEquals(Equality.DIFFERENT, comparator.compare("SZ", null, "S Zhu", null));
    }

    @Test
    public void testNewAuthorMap2() throws Exception {
        TaxonomyConfiguration config = TaxonomyConfiguration.read(this.resourceReader("taxonomy-config-4.json"));
        AuthorComparator comparator = config.newAuthorComparator();

        assertEquals(Equality.EQUAL, comparator.compare("Linnaeus", null, "Linnaeus", null));
        assertEquals(Equality.EQUAL, comparator.compare("L.", null, "Linnaeus", null));
        assertEquals(Equality.DIFFERENT, comparator.compare("Linnaeus", null, "Fred", null));
        assertEquals(Equality.EQUAL, comparator.compare("Sweet", null, "Sw.", null));
        assertEquals(Equality.EQUAL, comparator.compare("SZ", null, "S Zhu", null));
    }

}
