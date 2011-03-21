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

import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;

import au.org.ala.sds.model.SensitivityZone;

public class GeoLocationTest {

    @Test
    public void zonesContainingPoint() throws Exception {
        assertTrue(GeoLocationHelper.getZonesContainingPoint("-35.0", "145.0").contains(SensitivityZone.NSW));
        assertTrue(GeoLocationHelper.getZonesContainingPoint("-41.538137", "173.968817").contains(SensitivityZone.NOTAUS));

        // Cairns
        Set<SensitivityZone> zones = GeoLocationHelper.getZonesContainingPoint("-16.902785", "145.738106");
        assertTrue(zones.containsAll(EnumSet.of(SensitivityZone.QLD, SensitivityZone.PFFPQA1995)));

        // Emerald
        zones = GeoLocationHelper.getZonesContainingPoint("-23.546678", "148.151751");
        assertTrue(zones.contains(SensitivityZone.ECCPQA2004));
        assertFalse(zones.contains(SensitivityZone.PFFPQA1995));

        // Rolleston
        zones = GeoLocationHelper.getZonesContainingPoint("-24.527447", "148.602448");
        assertTrue(zones.contains(SensitivityZone.ECCPQA2004));

        // Capella
        zones = GeoLocationHelper.getZonesContainingPoint("-23.087233", "148.025537");
        assertTrue(zones.contains(SensitivityZone.ECCPQA2004));

        // Blackwater
        zones = GeoLocationHelper.getZonesContainingPoint("-23.5774", "148.885775");
        assertFalse(zones.contains(SensitivityZone.ECCPQA2004));

    }
}
