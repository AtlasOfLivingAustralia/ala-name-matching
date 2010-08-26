package au.org.ala.sensitiveData.dao;

import au.org.ala.sensitiveData.model.SensitiveSpecies;

public interface LookupDao {

	SensitiveSpecies findByName(String scientificName);
	
}
