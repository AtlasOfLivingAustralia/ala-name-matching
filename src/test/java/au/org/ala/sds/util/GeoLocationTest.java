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
package au.org.ala.sds.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import au.org.ala.sds.model.SensitivityZone;
import au.org.ala.sds.model.SensitivityZoneFactory;

public class GeoLocationTest {

    @Test
    public void zonesContainingPoint() throws Exception {
        assertTrue(GeoLocationHelper.getZonesContainingPoint("-35.0", "145.0").contains(SensitivityZoneFactory.getZone(SensitivityZone.NSW)));
        assertTrue(GeoLocationHelper.getZonesContainingPoint("-35.276771", "149.112539").contains(SensitivityZoneFactory.getZone(SensitivityZone.ACT)));
        assertTrue(GeoLocationHelper.getZonesContainingPoint("-35.140266", "150.698433").contains(SensitivityZoneFactory.getZone(SensitivityZone.ACT)));
        assertTrue(GeoLocationHelper.getZonesContainingPoint("-11.268428", "132.14653").contains(SensitivityZoneFactory.getZone(SensitivityZone.NT)));
        assertTrue(GeoLocationHelper.getZonesContainingPoint("-41.538137", "173.968817").contains(SensitivityZoneFactory.getZone(SensitivityZone.NOTAUS)));

        // Cairns
        Set<SensitivityZone> ref = new HashSet<SensitivityZone>();
        ref.add(SensitivityZoneFactory.getZone(SensitivityZone.QLD));
        ref.add(SensitivityZoneFactory.getZone(SensitivityZone.PFFPQA1995));
        Set<SensitivityZone> zones = GeoLocationHelper.getZonesContainingPoint("-16.902785", "145.738106");

        assertTrue(zones.containsAll(ref));

        // Emerald
        zones = GeoLocationHelper.getZonesContainingPoint("-23.546678", "148.151751");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.ECCPQA2004)));
        assertFalse(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.PFFPQA1995)));

        // Rolleston
        zones = GeoLocationHelper.getZonesContainingPoint("-24.527447", "148.602448");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.ECCPQA2004)));

        // Capella
        zones = GeoLocationHelper.getZonesContainingPoint("-23.087233", "148.025537");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.ECCPQA2004)));

        // Blackwater
        zones = GeoLocationHelper.getZonesContainingPoint("-23.5774", "148.885775");
        assertFalse(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.ECCPQA2004)));

        // Torres Strait Protection Zone
        zones = GeoLocationHelper.getZonesContainingPoint("-9.80699", "142.64282");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.TSPZ)));

        // Torres Strait Special Quarantine Zone
        zones = GeoLocationHelper.getZonesContainingPoint("-10.62870", "142.19788");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.TSSQZ)));

        // Fruit Fly Exclusion Zone - Tri State NSW
        zones = GeoLocationHelper.getZonesContainingPoint("-33.53336", "142.08734");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.FFEZ)));
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.NSW)));

        // Fruit Fly Exclusion Zone - Tri State SA
        zones = GeoLocationHelper.getZonesContainingPoint("-34.38989", "140.52728");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.FFEZ)));
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.SA)));

        // Fruit Fly Exclusion Zone - Tri State Vic
        zones = GeoLocationHelper.getZonesContainingPoint("-36.17444", "144.29559");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.FFEZ)));
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.VIC)));

        // Potato Cyst Nematode - Wandin
        zones = GeoLocationHelper.getZonesContainingPoint("-37.82010", "145.45541");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.PCNCAVICWAN)));
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.VIC)));

        // Potato Cyst Nematode - Gembrook
        zones = GeoLocationHelper.getZonesContainingPoint("-37.93933", "145.53094");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.PCNCAVICGEM)));
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.VIC)));

        // Potato Cyst Nematode - Koo Wee Rup
        zones = GeoLocationHelper.getZonesContainingPoint("-38.13727", "145.59823");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.PCNCAVICKWR)));
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.VIC)));

        // Potato Cyst Nematode - Thorpedale
        zones = GeoLocationHelper.getZonesContainingPoint("-38.35190", "146.14480");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.PCNCAVICTHO)));
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.VIC)));

        // Phylloxera Infestation Zone Albury - Corowa
        zones = GeoLocationHelper.getZonesContainingPoint("-35.99533", "146.88358");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.PIZNSWAC)));
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.NSW)));

        // Phylloxera Infestation Zone Sydney
        zones = GeoLocationHelper.getZonesContainingPoint("-34.13686", "150.99214");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.PIZNSWSR)));
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.NSW)));

        // Phylloxera Infestation Zone Vic North East
        zones = GeoLocationHelper.getZonesContainingPoint("-36.35326", "146.68664");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.PIZVICNE)));
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.VIC)));

        // Phylloxera Infestation Zone Vic North East
        zones = GeoLocationHelper.getZonesContainingPoint("-36.35326", "146.68664");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.PIZVICNE)));
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.VIC)));

        // Phylloxera Infestation Zone Vic Mooroopna
        zones = GeoLocationHelper.getZonesContainingPoint("-36.41738", "145.29687");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.PIZVICMOR)));
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.VIC)));

        // Phylloxera Infestation Zone Vic Nagambie
        zones = GeoLocationHelper.getZonesContainingPoint("-36.79892", "145.14681");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.PIZVICNAG)));
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.VIC)));

        // Phylloxera Infestation Zone Vic Maroondah
        zones = GeoLocationHelper.getZonesContainingPoint("-37.65750", "145.37350");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.PIZVICMAR)));
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.VIC)));

        // Phylloxera Infestation Zone Vic Upton
        zones = GeoLocationHelper.getZonesContainingPoint("-36.91657", "145.36814");
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.PIZVICUPT)));
        assertTrue(zones.contains(SensitivityZoneFactory.getZone(SensitivityZone.VIC)));

    }
}
