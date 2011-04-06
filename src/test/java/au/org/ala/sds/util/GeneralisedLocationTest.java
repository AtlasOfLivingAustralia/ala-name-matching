package au.org.ala.sds.util;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import au.org.ala.sds.model.ConservationInstance;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.model.SensitivityCategory;
import au.org.ala.sds.model.SensitivityInstance;
import au.org.ala.sds.model.SensitivityZone;


public class GeneralisedLocationTest {

    String NSW_LAT = "-35.12345",  NSW_LONG = "146.67890";
    String VIC_LAT = "-37.046409", VIC_LONG = "146.239014";
    String QLD_LAT = "-18.406655", QLD_LONG = "146.2541";
    String TAS_LAT = "-42.067518", TAS_LONG = "145.278568";
    String ACT_LAT = "-35.29187",  ACT_LONG = "149.100137";
    static SensitiveTaxon ss;
    static Set<SensitivityZone> zones;

    static {
        ss = new SensitiveTaxon("Crex crex", SensitiveTaxon.Rank.SPECIES);
        List<SensitivityInstance> instances = ss.getInstances();
        instances.add(new ConservationInstance(SensitivityCategory.CRITICALLY_ENDANGERED, "NSW DECCW", SensitivityZone.NSW, "WITHHOLD"));
        instances.add(new ConservationInstance(SensitivityCategory.ENDANGERED, "Vic DSE", SensitivityZone.VIC, "10km"));
        instances.add(new ConservationInstance(SensitivityCategory.VULNERABLE, "QLD DERM", SensitivityZone.QLD, "1km"));
        instances.add(new ConservationInstance(SensitivityCategory.NEAR_THREATENED, "Tas DPIPWE", SensitivityZone.TAS, "100m"));
    }

    @Test
    public void locationGeneralisation() {
        zones = new HashSet<SensitivityZone>(Collections.singleton(SensitivityZone.NSW));
        GeneralisedLocation extremeGeneralisation = GeneralisedLocationFactory.getGeneralisedLocation(NSW_LAT, NSW_LONG, ss, zones);
        assertEquals("", extremeGeneralisation.getGeneralisedLatitude());
        assertEquals("", extremeGeneralisation.getGeneralisedLongitude());
        assertEquals("", extremeGeneralisation.getGeneralisationInMetres());

        zones = new HashSet<SensitivityZone>(Collections.singleton(SensitivityZone.VIC));
        GeneralisedLocation highGeneralisation = GeneralisedLocationFactory.getGeneralisedLocation(VIC_LAT, VIC_LONG, ss, zones);
        assertEquals("-37.0", highGeneralisation.getGeneralisedLatitude());
        assertEquals("146.2", highGeneralisation.getGeneralisedLongitude());
        assertEquals("10000", highGeneralisation.getGeneralisationInMetres());

        zones = new HashSet<SensitivityZone>(Collections.singleton(SensitivityZone.QLD));
        GeneralisedLocation mediumGeneralisation = GeneralisedLocationFactory.getGeneralisedLocation(QLD_LAT, QLD_LONG, ss, zones);
        assertEquals("-18.41", mediumGeneralisation.getGeneralisedLatitude());
        assertEquals("146.25", mediumGeneralisation.getGeneralisedLongitude());
        assertEquals("1000", mediumGeneralisation.getGeneralisationInMetres());

        zones = new HashSet<SensitivityZone>(Collections.singleton(SensitivityZone.TAS));
        GeneralisedLocation lowGeneralisation = GeneralisedLocationFactory.getGeneralisedLocation(TAS_LAT, TAS_LONG, ss, zones);
        assertEquals("-42.068", lowGeneralisation.getGeneralisedLatitude());
        assertEquals("145.279", lowGeneralisation.getGeneralisedLongitude());
        assertEquals("100", lowGeneralisation.getGeneralisationInMetres());

        zones = new HashSet<SensitivityZone>(Collections.singleton(SensitivityZone.ACT));
        GeneralisedLocation defaultGeneralisation = GeneralisedLocationFactory.getGeneralisedLocation(ACT_LAT, ACT_LONG, ss, zones);
        assertEquals(ACT_LAT, defaultGeneralisation.getGeneralisedLatitude());
        assertEquals(ACT_LONG, defaultGeneralisation.getGeneralisedLongitude());
        assertEquals("", defaultGeneralisation.getGeneralisationInMetres());
    }
}
