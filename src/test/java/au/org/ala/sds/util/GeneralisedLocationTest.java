package au.org.ala.sds.util;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import au.org.ala.sds.model.SensitiveSpecies;
import au.org.ala.sds.model.SensitivityCategory;
import au.org.ala.sds.model.SensitivityInstance;
import au.org.ala.sds.model.SensitivityZone;


public class GeneralisedLocationTest {

    String NSW_LAT = "-35.12345",  NSW_LONG = "146.67890";
    String VIC_LAT = "-37.046409", VIC_LONG = "146.239014";
    String QLD_LAT = "-18.406655", QLD_LONG = "146.2541";
    String TAS_LAT = "-42.067518", TAS_LONG = "145.278568";
    String ACT_LAT = "-35.29187",  ACT_LONG = "149.100137";
    static SensitiveSpecies ss;

    static {
        ss = new SensitiveSpecies("Crex crex");
        List<SensitivityInstance> instances = ss.getInstances();
        instances.add(new SensitivityInstance(SensitivityCategory.CRITICALLY_ENDANGERED, "NSW DECCW", null, null, SensitivityZone.NSW, "WITHHOLD"));
        instances.add(new SensitivityInstance(SensitivityCategory.ENDANGERED, "Vic DSE", null, null, SensitivityZone.VIC, "10km"));
        instances.add(new SensitivityInstance(SensitivityCategory.VULNERABLE, "QLD DERM", null, null, SensitivityZone.QLD, "1km"));
        instances.add(new SensitivityInstance(SensitivityCategory.NEAR_THREATENED, "Tas DPIPWE", null, null, SensitivityZone.TAS, "100m"));
    }

    @Test
    public void TestLocationGeneralisation() {
        GeneralisedLocation extremeGeneralisation = GeneralisedLocationFactory.getGeneralisedLocation(NSW_LAT, NSW_LONG, ss);
        assertEquals("", extremeGeneralisation.getGeneralisedLatitude());
        assertEquals("", extremeGeneralisation.getGeneralisedLongitude());
        assertEquals("", extremeGeneralisation.getGeneralisationInMetres());

        GeneralisedLocation highGeneralisation = GeneralisedLocationFactory.getGeneralisedLocation(VIC_LAT, VIC_LONG, ss);
        assertEquals("-37.0", highGeneralisation.getGeneralisedLatitude());
        assertEquals("146.2", highGeneralisation.getGeneralisedLongitude());
        assertEquals("10000", highGeneralisation.getGeneralisationInMetres());

        GeneralisedLocation mediumGeneralisation = GeneralisedLocationFactory.getGeneralisedLocation(QLD_LAT, QLD_LONG, ss);
        assertEquals("-18.41", mediumGeneralisation.getGeneralisedLatitude());
        assertEquals("146.25", mediumGeneralisation.getGeneralisedLongitude());
        assertEquals("1000", mediumGeneralisation.getGeneralisationInMetres());

        GeneralisedLocation lowGeneralisation = GeneralisedLocationFactory.getGeneralisedLocation(TAS_LAT, TAS_LONG, ss);
        assertEquals("-42.068", lowGeneralisation.getGeneralisedLatitude());
        assertEquals("145.279", lowGeneralisation.getGeneralisedLongitude());
        assertEquals("100", lowGeneralisation.getGeneralisationInMetres());

        GeneralisedLocation defaultGeneralisation = GeneralisedLocationFactory.getGeneralisedLocation(ACT_LAT, ACT_LONG, ss);
        assertEquals(ACT_LAT, defaultGeneralisation.getGeneralisedLatitude());
        assertEquals(ACT_LONG, defaultGeneralisation.getGeneralisedLongitude());
        assertEquals("", defaultGeneralisation.getGeneralisationInMetres());
    }
}
