package au.org.ala.sds.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import au.org.ala.sds.model.SensitivityCategory;


public class GeneralisedLocationTest {

    static String LAT = "-35.12345";
    static String LONG = "146.67890";

    @Test
    public void TestLocationGeneralisation() {
        GeneralisedLocation extremeGeneralisation = GeneralisedLocationFactory.getGeneralisedLocation(LAT, LONG, SensitivityCategory.CRITICALLY_ENDANGERED);
        assertEquals("", extremeGeneralisation.getGeneralisedLatitude());
        assertEquals("", extremeGeneralisation.getGeneralisedLongitude());
        assertEquals("", extremeGeneralisation.getGeneralisationInMetres());

        GeneralisedLocation highGeneralisation = GeneralisedLocationFactory.getGeneralisedLocation(LAT, LONG, SensitivityCategory.ENDANGERED);
        assertEquals("-35.1", highGeneralisation.getGeneralisedLatitude());
        assertEquals("146.7", highGeneralisation.getGeneralisedLongitude());
        assertEquals("10000", highGeneralisation.getGeneralisationInMetres());

        GeneralisedLocation mediumGeneralisation = GeneralisedLocationFactory.getGeneralisedLocation(LAT, LONG, SensitivityCategory.VULNERABLE);
        assertEquals("-35.12", mediumGeneralisation.getGeneralisedLatitude());
        assertEquals("146.68", mediumGeneralisation.getGeneralisedLongitude());
        assertEquals("1000", mediumGeneralisation.getGeneralisationInMetres());

        GeneralisedLocation lowGeneralisation = GeneralisedLocationFactory.getGeneralisedLocation(LAT, LONG, SensitivityCategory.NEAR_THREATENED);
        assertEquals("-35.123", lowGeneralisation.getGeneralisedLatitude());
        assertEquals("146.679", lowGeneralisation.getGeneralisedLongitude());
        assertEquals("100", lowGeneralisation.getGeneralisationInMetres());

        GeneralisedLocation defaultGeneralisation = GeneralisedLocationFactory.getGeneralisedLocation(LAT, LONG, SensitivityCategory.LEAST_CONCERN);
        assertEquals(LAT, defaultGeneralisation.getGeneralisedLatitude());
        assertEquals(LONG, defaultGeneralisation.getGeneralisedLongitude());
        assertEquals("", defaultGeneralisation.getGeneralisationInMetres());
    }
}
