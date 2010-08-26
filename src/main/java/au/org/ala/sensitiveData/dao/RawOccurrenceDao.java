package au.org.ala.sensitiveData.dao;

import java.util.List;

import au.org.ala.sensitiveData.model.RawOccurrenceRecord;

public interface RawOccurrenceDao {

	List<RawOccurrenceRecord> getOccurrences();
	
	void updateLocation(int id, String rawLatitude, String rawLongitude, int generalisedMetres, String generalisedLatitude, String generalisedLongitude);
	
	void updateLocation(int id, String generalisedLatitude, String generalisedLongitude, int generalisedMetres);
}
