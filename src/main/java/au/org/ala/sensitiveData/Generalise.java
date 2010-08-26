package au.org.ala.sensitiveData;

import au.org.ala.sensitiveData.model.SensitiveSpecies;

public interface Generalise {

	String[] generaliseLocation(SensitiveSpecies sensitiveSpecies, String latitude, String longitude);
	
}
