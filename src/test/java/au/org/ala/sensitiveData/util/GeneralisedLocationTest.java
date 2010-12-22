package au.org.ala.sensitiveData.util;

import static org.junit.Assert.*;

import org.junit.Test;

import au.org.ala.sensitiveData.model.SensitivityCategory;


public class GeneralisedLocationTest {
    
    static String LAT = "-35.12345";
    static String LONG = "146.67890";

    @Test
    public void TestLocationGeneralisation() {
        GeneralisedLocation extremeGeneralisation = new GeneralisedLocation(LAT, LONG, SensitivityCategory.CONSERVATION_EXTREME);
        assertEquals("", extremeGeneralisation.getGeneralisedLatitude());
        assertEquals("", extremeGeneralisation.getGeneralisedLongitude());
        assertEquals("", extremeGeneralisation.getGeneralisationInMetres());
        
        GeneralisedLocation highGeneralisation = new GeneralisedLocation(LAT, LONG, SensitivityCategory.CONSERVATION_HIGH);
        assertEquals("-35.1", highGeneralisation.getGeneralisedLatitude());
        assertEquals("146.7", highGeneralisation.getGeneralisedLongitude());
        assertEquals("10000", highGeneralisation.getGeneralisationInMetres());
        
        GeneralisedLocation mediumGeneralisation = new GeneralisedLocation(LAT, LONG, SensitivityCategory.CONSERVATION_MEDIUM);
        assertEquals("-35.12", mediumGeneralisation.getGeneralisedLatitude());
        assertEquals("146.68", mediumGeneralisation.getGeneralisedLongitude());
        assertEquals("1000", mediumGeneralisation.getGeneralisationInMetres());

        GeneralisedLocation lowGeneralisation = new GeneralisedLocation(LAT, LONG, SensitivityCategory.CONSERVATION_LOW);
        assertEquals("-35.123", lowGeneralisation.getGeneralisedLatitude());
        assertEquals("146.679", lowGeneralisation.getGeneralisedLongitude());
        assertEquals("100", lowGeneralisation.getGeneralisationInMetres());
        
        GeneralisedLocation defaultGeneralisation = new GeneralisedLocation(LAT, LONG, SensitivityCategory.NOT_SENSITIVE);
        assertEquals(LAT, defaultGeneralisation.getGeneralisedLatitude());
        assertEquals(LONG, defaultGeneralisation.getGeneralisedLongitude());
        assertEquals("", defaultGeneralisation.getGeneralisationInMetres());
        
    }
}
