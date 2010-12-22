package au.org.ala.sensitiveData.dao;

import au.org.ala.sensitiveData.model.SensitivityZone;

public interface GeoLocationDao {
    SensitivityZone getStateContainingPoint(String latitude, String longitude) throws Exception;
}
