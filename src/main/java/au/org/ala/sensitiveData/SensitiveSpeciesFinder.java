/***************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 ***************************************************************************/
package au.org.ala.sensitiveData;

import java.math.BigDecimal;

import au.org.ala.sensitiveData.dao.SensitiveSpeciesDao;
import au.org.ala.sensitiveData.model.SensitiveSpecies;
import au.org.ala.sensitiveData.model.SensitivityCategory;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitiveSpeciesFinder implements Lookup, Generalise {
	
	private SensitiveSpeciesDao dao;
	
	public void setDao(SensitiveSpeciesDao dao ) {
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
