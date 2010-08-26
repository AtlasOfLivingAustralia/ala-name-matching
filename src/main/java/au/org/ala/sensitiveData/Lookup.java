package au.org.ala.sensitiveData;

import au.org.ala.sensitiveData.model.SensitiveSpecies;

public interface Lookup {

	SensitiveSpecies findSensitiveSpecies(String scientificName);
	
	boolean isSensitive(String scientificName);
}
