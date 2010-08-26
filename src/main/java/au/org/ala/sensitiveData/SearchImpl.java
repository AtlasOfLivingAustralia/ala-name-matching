package au.org.ala.sensitiveData;

import java.math.BigDecimal;

import au.org.ala.sensitiveData.dao.LookupDao;
import au.org.ala.sensitiveData.model.SensitiveSpecies;
import au.org.ala.sensitiveData.model.SensitivityCategory;

public class SearchImpl implements Lookup, Generalise {
	
	private LookupDao dao;
	
	public void setDao(LookupDao dao ) {
		this.dao = dao;
	}
	
	public SensitiveSpecies findSensitiveSpecies(String scientificName) {
		return dao.findByName(scientificName);
	}

	public boolean isSensitive(String scientificName) {
		return dao.findByName(scientificName) != null;
	}

	public String[] generaliseLocation(SensitiveSpecies sensitiveSpecies, String latitude, String longitude) {
		if (sensitiveSpecies != null) {
			return generaliseCoordinates(sensitiveSpecies.getSensitivityCategory(), latitude, longitude);
		} else {
			return new String[] { latitude, longitude };
		}
	}

	private String[] generaliseCoordinates(SensitivityCategory category, String latitude, String longitude) {
		return new String[] { round(latitude, category.getGeneralisationDecimalPlaces()), round(longitude, category.getGeneralisationDecimalPlaces()) };
	}

	private String round(String number, int decimalPlaces) {
		if (number == null || number.equals("")) {
			return "";
		} else {
			return String.format("%." + decimalPlaces + "f", new BigDecimal(number));
		}
	}

}
