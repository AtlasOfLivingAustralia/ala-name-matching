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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import au.org.ala.sds.model.SensitivityZone;
import au.org.ala.sds.util.GeoLocationHelper;

public class GeoLocationTest {

    @Test
    public void getStateContainingPointTest() throws Exception {
        assertEquals(SensitivityZone.NSW, GeoLocationHelper.getStateContainingPoint("-35.0", "145.0"));
        assertNull(GeoLocationHelper.getStateContainingPoint("-41.538137", "173.968817"));
    }
}
