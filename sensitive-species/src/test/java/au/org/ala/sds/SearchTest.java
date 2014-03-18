/***************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 ***************************************************************************/
package au.org.ala.sds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.BeforeClass;
import org.junit.Test;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.util.Configuration;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SearchTest {

//    static DataSource dataSource;
    static CBIndexSearch cbIndexSearch;
    static SensitiveSpeciesFinder finder;

    @BeforeClass
    public static void runOnce() throws Exception {
//        dataSource = new BasicDataSource();
//        ((BasicDataSource) dataSource).setDriverClassName("com.mysql.jdbc.Driver");
//        ((BasicDataSource) dataSource).setUrl("jdbc:mysql://localhost/portal");
//        ((BasicDataSource) dataSource).setUsername("root");
//        ((BasicDataSource) dataSource).setPassword("password");

        cbIndexSearch = new CBIndexSearch(Configuration.getInstance().getNameMatchingIndex());
//        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(dataSource, cbIndexSearch);
        //finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder("file:///data/sds/sensitive-species.xml", cbIndexSearch);
        String uri = cbIndexSearch.getClass().getClassLoader().getResource("sensitive-species.xml").toURI().toString();
        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(uri, cbIndexSearch, true);
    }

    @Test
    public void lookup() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Macropus rufus");
        assertNull(ss);

        ss = finder.findSensitiveSpecies("Crex crex");
        assertNotNull(ss);

        ss = finder.findSensitiveSpeciesByLsid("urn:lsid:biodiversity.org.au:afd.taxon:1365807d-927b-4219-97bf-7e619afa5f72");
        assertNotNull(ss);
        assertEquals(ss.getTaxonName(), "Lophochroa leadbeateri");
        //NC 2013-04-10: This species does not exist in the current SDS lists
        //ss = finder.findSensitiveSpecies("Anigozanthos humilis subsp. Badgingarra (SD Hopper 7114)");
        //assertNotNull(ss);

        ss = finder.findSensitiveSpecies("Cacatua leadbeateri");
        assertNotNull(ss);
        assertEquals(ss.getTaxonName(), "Lophochroa leadbeateri");

        //NC 2013-04-10: This species does not exist in the current SDS lists
        //ss = finder.findSensitiveSpecies("Dendrobium speciosum subsp. hillii");
        //assertNotNull(ss);

        //NC 2013-04-10: This species does not exist in the current SDS lists
        //ss = finder.findSensitiveSpecies("Thelymitra nuda");
        //assertNotNull(ss);
    }
}
